package com.example.projetopdm4.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.util.Base64
import com.example.projetopdm4.BuildConfig
import com.example.projetopdm4.data.firebase.awaitResult
import com.example.projetopdm4.domain.model.UserProfile
import com.example.projetopdm4.domain.repository.ProfileRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.io.ByteArrayOutputStream
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class FirebaseProfileRepository(context: Context) : ProfileRepository {
    private val preferences = context.applicationContext.getSharedPreferences("profile", Context.MODE_PRIVATE)
    private val auth = FirebaseAuth.getInstance()
    private val databaseUrl = BuildConfig.FIREBASE_DATABASE_URL.trim()
    private val mutableProfile = MutableStateFlow(UserProfile(null))
    private val profileMutex = Mutex()
    private var profileReference: DatabaseReference? = null
    private var profileListener: ValueEventListener? = null
    override val profile: StateFlow<UserProfile> = mutableProfile.asStateFlow()

    @Synchronized
    override fun selectAccount(uid: String?) {
        profileReference?.let { reference ->
            profileListener?.let(reference::removeEventListener)
        }
        profileReference = null
        profileListener = null
        if (uid == null || auth.currentUser?.isAnonymous != false) {
            mutableProfile.value = UserProfile(uid)
            return
        }

        val accountProfile = localProfile(uid)
        mutableProfile.value = accountProfile
        if (databaseUrl.isEmpty()) return

        val reference = FirebaseDatabase.getInstance(databaseUrl)
            .getReference("users")
            .child(uid)
            .child("profile")
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (auth.currentUser?.uid != uid) return
                val remoteName = snapshot.child("name").getValue(String::class.java).orEmpty()
                val remoteColor = snapshot.child("color").getValue(Int::class.java) ?: accountProfile.color
                val photo = snapshot.child("photoBase64").getValue(String::class.java)
                    ?: accountProfile.photoBase64
                val merged = accountProfile.copy(
                    name = remoteName.ifBlank { accountProfile.name },
                    color = remoteColor,
                    photoBase64 = photo
                )
                mutableProfile.value = merged
                preferences.edit()
                    .putString("name_$uid", merged.name)
                    .putInt("color_$uid", merged.color)
                    .apply()
            }

            override fun onCancelled(error: com.google.firebase.database.DatabaseError) {
                if (auth.currentUser?.uid == uid) {
                    mutableProfile.value = mutableProfile.value.copy(
                        error = "Não foi possível carregar o perfil: ${error.message}"
                    )
                }
            }
        }
        profileReference = reference
        profileListener = listener
        reference.addValueEventListener(listener)
    }

    override suspend fun save(name: String, color: Int) = profileMutex.withLock {
        val uid = authenticatedUid()
        val current = mutableProfile.value.copy(uid = uid, name = name, color = color)
        preferences.edit().putString("name_$uid", name).putInt("color_$uid", color).apply()
        mutableProfile.value = current
        if (databaseUrl.isNotEmpty()) {
            FirebaseDatabase.getInstance(databaseUrl)
                .getReference("users")
                .child(uid)
                .child("profile")
                .updateChildren(mapOf("name" to name, "color" to color))
                .awaitResult()
        }
    }

    override suspend fun savePhoto(imageBytes: ByteArray) = profileMutex.withLock {
        val uid = authenticatedUid()
        check(databaseUrl.isNotEmpty()) { "Configure a URL do Firebase Realtime Database para sincronizar a foto." }
        val encodedPhoto = Base64.encodeToString(
            withContext(Dispatchers.Default) { compressImage(imageBytes) },
            Base64.NO_WRAP
        )
        FirebaseDatabase.getInstance(databaseUrl)
            .getReference("users")
            .child(uid)
            .child("profile")
            .child("photoBase64")
            .setValue(encodedPhoto)
            .awaitResult()
        mutableProfile.value = mutableProfile.value.copy(uid = uid, photoBase64 = encodedPhoto)
    }

    override fun clearError() {
        mutableProfile.value = mutableProfile.value.copy(error = null)
    }

    private fun authenticatedUid(): String {
        val user = auth.currentUser
        check(user != null && !user.isAnonymous) { "Entre na sua conta para editar o perfil." }
        return user.uid
    }

    private fun localProfile(uid: String) = UserProfile(
        uid = uid,
        name = preferences.getString("name_$uid", "").orEmpty(),
        color = preferences.getInt("color_$uid", 0)
    )

    private fun compressImage(bytes: ByteArray): ByteArray {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        check(bounds.outWidth > 0 && bounds.outHeight > 0) { "O arquivo selecionado não é uma imagem válida." }
        var sampleSize = 1
        while (bounds.outWidth / sampleSize > MAX_IMAGE_SIZE ||
            bounds.outHeight / sampleSize > MAX_IMAGE_SIZE
        ) {
            sampleSize *= 2
        }
        val decoded = BitmapFactory.decodeByteArray(
            bytes,
            0,
            bytes.size,
            BitmapFactory.Options().apply { inSampleSize = sampleSize }
        ) ?: error("Não foi possível abrir a imagem selecionada.")
        val scale = minOf(
            MAX_IMAGE_SIZE.toFloat() / decoded.width,
            MAX_IMAGE_SIZE.toFloat() / decoded.height,
            1f
        )
        val resized = if (scale < 1f) {
            Bitmap.createBitmap(
                decoded,
                0,
                0,
                decoded.width,
                decoded.height,
                Matrix().apply { setScale(scale, scale) },
                true
            ).also { decoded.recycle() }
        } else {
            decoded
        }
        return ByteArrayOutputStream().use { output ->
            check(resized.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, output)) {
                "Não foi possível compactar a foto."
            }
            resized.recycle()
            output.toByteArray()
        }
    }

    private companion object {
        const val MAX_IMAGE_SIZE = 512
        const val JPEG_QUALITY = 75
    }
}
