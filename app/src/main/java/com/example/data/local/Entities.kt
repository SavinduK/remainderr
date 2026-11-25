package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String,
    val completed: Boolean,
    val date: String,
    val startTime: String? = null,
    val endTime: String? = null,
    val createdDate: String? = null,
)

@Serializable
@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey val id: String,
    val title: String,
    val amount: Double,
    val category: String,
    val date: String,
)

@Serializable
@Entity(tableName = "credit_cards")
data class CreditCardEntity(
    @PrimaryKey val id: String,
    val bankName: String,
    val cardNumber: String,
    val expiry: String,
    val type: String, // "Visa" or "Mastercard"
    val cardHolder: String,
    val color: String,
)

@Serializable
@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey val id: String,
    val title: String,
    val content: String,
    val createdAt: String,
    val isFavorite: Boolean,
    val isCompleted: Boolean = false,
    val category: String? = null,
)

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey val name: String,
    val sortOrder: Int = 0,
)

@Serializable
@Entity(tableName = "passwords")
data class PasswordEntity(
    @PrimaryKey val id: String,
    val site: String,
    val username: String,
    val password: String,
)

@Entity(tableName = "checklist_items")
data class ChecklistItemEntity(
    @PrimaryKey val id: String,
    val category: String,
    val title: String,
    val checked: Boolean,
    val sortOrder: Int = 0,
)
