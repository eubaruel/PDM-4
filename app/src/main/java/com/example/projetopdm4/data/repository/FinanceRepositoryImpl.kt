package com.example.projetopdm4.data.repository

import com.example.projetopdm4.data.local.FinanceDao
import com.example.projetopdm4.data.local.toDomain
import com.example.projetopdm4.data.local.toEntity
import com.example.projetopdm4.data.firebase.awaitResult
import com.example.projetopdm4.domain.model.AuthSession
import com.example.projetopdm4.domain.model.CategoryBudget
import com.example.projetopdm4.domain.model.FinanceCategory
import com.example.projetopdm4.domain.model.FinanceData
import com.example.projetopdm4.domain.model.FinanceTransaction
import com.example.projetopdm4.domain.model.SavingsGoal
import com.example.projetopdm4.domain.repository.FinanceRepository as FinanceRepositoryContract

import android.content.Context
import com.example.projetopdm4.BuildConfig
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.Job
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import com.google.firebase.auth.FirebaseUser
import java.util.UUID

class FinanceRepositoryImpl(
    private val dao: FinanceDao,
    context: Context
) : FinanceRepositoryContract {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val syncPreferences = context.applicationContext.getSharedPreferences("finance_sync", Context.MODE_PRIVATE)
    private val mutableSyncStatus = MutableStateFlow("Conectando ao Firebase…")
    override val syncStatus = mutableSyncStatus
    private val auth = FirebaseAuth.getInstance()
    private val mutableAuthState = MutableStateFlow(auth.currentUser.toAuthSession())
    override val authState = mutableAuthState
    private val localData = combine(
        dao.observeTransactions(),
        dao.observeCategories(),
        dao.observeBudgets(),
        dao.observeGoals()
    ) { transactions, categories, budgets, goals ->
        FinanceData(
            transactions.map { it.toDomain() },
            categories.map { it.toDomain() },
            budgets.map { it.toDomain() },
            goals.map { it.toDomain() }
        )
    }
    override val data = localData.stateIn(scope, SharingStarted.Eagerly, FinanceData())

    @Volatile
    private var started = false
    private var syncJob: Job? = null

    override fun startSync() {
        synchronized(this) {
            if (started) return
            started = true
        }
        scope.launch {
            val databaseUrl = BuildConfig.FIREBASE_DATABASE_URL.trim()
            if (databaseUrl.isEmpty()) {
                mutableSyncStatus.value =
                    "Firebase sem URL: configure firebaseDatabaseUrl no local.properties."
            }
            auth.addAuthStateListener { firebaseAuth ->
                val user = firebaseAuth.currentUser
                mutableAuthState.value = user.toAuthSession()
                if (user == null) {
                    syncJob?.cancel()
                    syncJob = null
                    mutableSyncStatus.value = "Entre na sua conta para sincronizar os dados."
                } else if (user.isAnonymous) {
                    auth.signOut()
                } else {
                    if (databaseUrl.isNotEmpty()) {
                        val previousSync = syncJob
                        previousSync?.cancel()
                        syncJob = scope.launch {
                            previousSync?.join()
                            syncForUser(user.uid, databaseUrl)
                        }
                    }
                }
            }
        }
    }

    override suspend fun signIn(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password).awaitResult()
    }

    override suspend fun createAccount(email: String, password: String) {
        if (auth.currentUser?.isAnonymous == true) auth.signOut()
        auth.createUserWithEmailAndPassword(email, password).awaitResult()
    }

    override fun signOut() {
        auth.signOut()
    }

    override suspend fun updateDisplayName(name: String) {
        val user = auth.currentUser ?: error("Entre em uma conta para editar o perfil.")
        user.updateProfile(
            com.google.firebase.auth.UserProfileChangeRequest.Builder()
                .setDisplayName(name)
                .build()
        ).awaitResult()
    }

    private suspend fun syncForUser(uid: String, databaseUrl: String) = coroutineScope {
        try {
            val previousUid = syncPreferences.getString("owner_uid", null)
            if (previousUid != null && previousUid != uid) {
                dao.replaceData(FinanceData())
            }
            syncPreferences.edit().putString("owner_uid", uid).apply()
            mutableSyncStatus.value = "Sincronizando…"
            val reference = FirebaseDatabase.getInstance(databaseUrl)
                .getReference("users")
                .child(uid)
                .child("finance")
            var lastFingerprint: String? = null
            var initialLoad = true
            var waitingForInitialRemoteData = false
            val mutex = Mutex()

            val listener = object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    launch {
                        mutex.withLock {
                            try {
                                val localAtStart = if (initialLoad && snapshot.exists()) {
                                    localData.first()
                                } else {
                                    null
                                }
                                val keepLocalData = localAtStart?.hasData() == true
                                val remote = if (snapshot.exists()) readSnapshot(snapshot) else FinanceData()
                                val remoteFingerprint = fingerprint(remote)
                                if (snapshot.exists() && !keepLocalData && remoteFingerprint != lastFingerprint) {
                                    dao.replaceData(remote)
                                    lastFingerprint = remoteFingerprint
                                }
                                if (initialLoad) {
                                    initialLoad = false
                                    if (!snapshot.exists() || keepLocalData) {
                                        val local = localAtStart ?: localData.first()
                                        lastFingerprint = fingerprint(local)
                                        reference.setValue(writeSnapshot(local))
                                            .addOnFailureListener {
                                                mutableSyncStatus.value = "Falha na sincronização: ${it.localizedMessage}"
                                            }
                                    }
                                    waitingForInitialRemoteData = snapshot.exists() && !keepLocalData
                                }
                                mutableSyncStatus.value = "Sincronizado"
                            } catch (error: Exception) {
                                mutableSyncStatus.value =
                                    "Falha ao ler dados sincronizados: ${error.localizedMessage}"
                            }
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    mutableSyncStatus.value = "Falha na sincronização: ${error.message}"
                }
            }
            reference.addValueEventListener(listener)
            try {
                launch {
                    localData.collect { local ->
                        mutex.withLock {
                            val nextFingerprint = fingerprint(local)
                            if (waitingForInitialRemoteData) {
                                if (nextFingerprint == lastFingerprint) waitingForInitialRemoteData = false
                            } else if (nextFingerprint != lastFingerprint) {
                                lastFingerprint = nextFingerprint
                                reference.setValue(writeSnapshot(local))
                                    .addOnSuccessListener { mutableSyncStatus.value = "Sincronizado" }
                                    .addOnFailureListener {
                                        mutableSyncStatus.value = "Falha na sincronização: ${it.localizedMessage}"
                                    }
                            }
                        }
                    }
                }
                awaitCancellation()
            } finally {
                reference.removeEventListener(listener)
            }
        } catch (error: CancellationException) {
            throw error
        } catch (error: FirebaseAuthException) {
            mutableSyncStatus.value =
                "Erro de autenticação Firebase (${error.errorCode}): ${error.localizedMessage}"
        } catch (error: Exception) {
            mutableSyncStatus.value =
                "Firebase indisponível: ${error.localizedMessage ?: "verifique a conexão e a configuração"}"
        }
    }

    private fun FirebaseUser?.toAuthSession() = AuthSession(
        uid = this?.uid,
        email = this?.email,
        displayName = this?.displayName,
        isAnonymous = this?.isAnonymous ?: true
    )

    override suspend fun saveTransaction(transaction: FinanceTransaction) {
        dao.putTransaction(transaction.toEntity())
    }

    override suspend fun deleteTransaction(id: Long) = dao.deleteTransaction(id)

    override suspend fun saveCategory(name: String, type: String) {
        dao.putCategory(FinanceCategory(UUID.randomUUID().toString(), name, type).toEntity())
    }

    override suspend fun deleteCategory(id: String) = dao.deleteCategory(id)

    override suspend fun saveBudget(budget: CategoryBudget) = dao.putBudget(budget.toEntity())

    override suspend fun deleteBudget(categoryId: String, month: String) = dao.deleteBudget(categoryId, month)

    override suspend fun saveGoal(goal: SavingsGoal) = dao.putGoal(goal.toEntity())

    override suspend fun deleteGoal(id: String) = dao.deleteGoal(id)

    private suspend fun readSnapshot(snapshot: DataSnapshot): FinanceData {
        val transactions = snapshot.child("transactions").children.mapNotNull { item ->
            val id = item.key?.toLongOrNull() ?: return@mapNotNull null
            FinanceTransaction(
                id = id,
                type = item.child("type").getValue(String::class.java) ?: return@mapNotNull null,
                amountCents = item.child("amountCents").getValue(Long::class.java) ?: return@mapNotNull null,
                categoryId = item.child("categoryId").getValue(String::class.java).orEmpty(),
                categoryName = item.child("categoryName").getValue(String::class.java).orEmpty(),
                description = item.child("description").getValue(String::class.java).orEmpty(),
                occurredAt = item.child("occurredAt").getValue(Long::class.java) ?: return@mapNotNull null
            )
        }
        val categories = snapshot.child("categories").children.mapNotNull { item ->
            val id = item.key ?: return@mapNotNull null
            FinanceCategory(
                id,
                item.child("name").getValue(String::class.java) ?: return@mapNotNull null,
                item.child("type").getValue(String::class.java) ?: return@mapNotNull null
            )
        }
        val budgets = snapshot.child("budgets").children.mapNotNull { item ->
            val categoryId = item.child("categoryId").getValue(String::class.java) ?: return@mapNotNull null
            CategoryBudget(
                categoryId,
                item.child("categoryName").getValue(String::class.java).orEmpty(),
                item.child("month").getValue(String::class.java) ?: return@mapNotNull null,
                item.child("limitCents").getValue(Long::class.java) ?: return@mapNotNull null
            )
        }
        val goals = snapshot.child("goals").children.mapNotNull { item ->
            val id = item.key ?: return@mapNotNull null
            SavingsGoal(
                id,
                item.child("name").getValue(String::class.java) ?: return@mapNotNull null,
                item.child("targetCents").getValue(Long::class.java) ?: return@mapNotNull null,
                item.child("savedCents").getValue(Long::class.java) ?: return@mapNotNull null,
                item.child("targetDate").getValue(Long::class.java)
            )
        }
        return FinanceData(transactions, categories, budgets, goals)
    }

    private fun writeSnapshot(data: FinanceData): Map<String, Any?> = buildMap {
        if (data.transactions.isNotEmpty()) put("transactions", data.transactions.associate { item ->
            item.id.toString() to mapOf(
                "type" to item.type,
                "amountCents" to item.amountCents,
                "categoryId" to item.categoryId,
                "categoryName" to item.categoryName,
                "description" to item.description,
                "occurredAt" to item.occurredAt
            )
        })
        if (data.categories.isNotEmpty()) put("categories", data.categories.associate { item ->
            item.id to mapOf("name" to item.name, "type" to item.type)
        })
        if (data.budgets.isNotEmpty()) put("budgets", data.budgets.associate { item ->
            "${item.categoryId}_${item.month}" to mapOf(
                "categoryId" to item.categoryId,
                "categoryName" to item.categoryName,
                "month" to item.month,
                "limitCents" to item.limitCents
            )
        })
        if (data.goals.isNotEmpty()) put("goals", data.goals.associate { item ->
            item.id to mutableMapOf<String, Any>(
                "name" to item.name,
                "targetCents" to item.targetCents,
                "savedCents" to item.savedCents
            ).apply {
                item.targetDate?.let { put("targetDate", it) }
            }
        })
    }

    private fun fingerprint(data: FinanceData): String = buildString {
        data.transactions.sortedBy { it.id }.forEach { append(it).append('|') }
        data.categories.sortedBy { it.id }.forEach { append(it).append('|') }
        data.budgets.sortedWith(compareBy({ it.categoryId }, { it.month })).forEach { append(it).append('|') }
        data.goals.sortedBy { it.id }.forEach { append(it).append('|') }
    }

    private fun FinanceData.hasData(): Boolean =
        transactions.isNotEmpty() || categories.isNotEmpty() || budgets.isNotEmpty() || goals.isNotEmpty()

}
