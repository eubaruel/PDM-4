package com.example.projetopdm4.data.local

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Transaction
import com.example.projetopdm4.domain.model.FinanceData
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "transactions")
data class FinanceTransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String,
    val amountCents: Long,
    val categoryId: String,
    val categoryName: String,
    val description: String,
    val occurredAt: Long
)

@Entity(tableName = "categories")
data class FinanceCategoryEntity(
    @PrimaryKey val id: String,
    val name: String,
    val type: String
)

@Entity(tableName = "budgets", primaryKeys = ["categoryId", "month"])
data class CategoryBudgetEntity(
    val categoryId: String,
    val categoryName: String,
    val month: String,
    val limitCents: Long
)

@Entity(tableName = "goals")
data class SavingsGoalEntity(
    @PrimaryKey val id: String,
    val name: String,
    val targetCents: Long,
    val savedCents: Long,
    val targetDate: Long?
)

@Dao
interface FinanceDao {
    @Query("SELECT * FROM transactions ORDER BY occurredAt DESC")
    fun observeTransactions(): Flow<List<FinanceTransactionEntity>>

    @Query("SELECT * FROM categories ORDER BY name COLLATE NOCASE")
    fun observeCategories(): Flow<List<FinanceCategoryEntity>>

    @Query("SELECT * FROM budgets ORDER BY month DESC, categoryName COLLATE NOCASE")
    fun observeBudgets(): Flow<List<CategoryBudgetEntity>>

    @Query("SELECT * FROM goals ORDER BY name COLLATE NOCASE")
    fun observeGoals(): Flow<List<SavingsGoalEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun putTransaction(transaction: FinanceTransactionEntity): Long

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteTransaction(id: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun putCategory(category: FinanceCategoryEntity)

    @Query("DELETE FROM categories WHERE id = :id")
    suspend fun deleteCategoryRow(id: String)

    @Query("DELETE FROM budgets WHERE categoryId = :id")
    suspend fun deleteCategoryBudgets(id: String)

    @Transaction
    suspend fun deleteCategory(id: String) {
        deleteCategoryRow(id)
        deleteCategoryBudgets(id)
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun putBudget(budget: CategoryBudgetEntity)

    @Query("DELETE FROM budgets WHERE categoryId = :categoryId AND month = :month")
    suspend fun deleteBudget(categoryId: String, month: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun putGoal(goal: SavingsGoalEntity)

    @Query("DELETE FROM goals WHERE id = :id")
    suspend fun deleteGoal(id: String)

    @Query("DELETE FROM transactions")
    suspend fun clearTransactions()

    @Query("DELETE FROM categories")
    suspend fun clearCategories()

    @Query("DELETE FROM budgets")
    suspend fun clearBudgets()

    @Query("DELETE FROM goals")
    suspend fun clearGoals()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun putTransactions(items: List<FinanceTransactionEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun putCategories(items: List<FinanceCategoryEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun putBudgets(items: List<CategoryBudgetEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun putGoals(items: List<SavingsGoalEntity>)

    @Transaction
    suspend fun replaceData(data: FinanceData) {
        clearTransactions()
        clearCategories()
        clearBudgets()
        clearGoals()
        putTransactions(data.transactions.map { it.toEntity() })
        putCategories(data.categories.map { it.toEntity() })
        putBudgets(data.budgets.map { it.toEntity() })
        putGoals(data.goals.map { it.toEntity() })
    }
}

@Database(
    entities = [FinanceTransactionEntity::class, FinanceCategoryEntity::class, CategoryBudgetEntity::class, SavingsGoalEntity::class],
    version = 1,
    exportSchema = false
)
abstract class FinanceDatabase : RoomDatabase() {
    abstract fun financeDao(): FinanceDao

    companion object {
        @Volatile
        private var instance: FinanceDatabase? = null

        fun get(context: Context): FinanceDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                FinanceDatabase::class.java,
                "finance.db"
            ).build().also { instance = it }
        }
    }
}
