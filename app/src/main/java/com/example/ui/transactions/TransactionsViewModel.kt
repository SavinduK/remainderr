package com.example.ui.transactions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.CreditCardEntity
import com.example.data.local.TransactionEntity
import com.example.data.repository.RemainderrRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

data class GroupedTransactions(
    val dateKey: String,
    val formattedDate: String,
    val dailyTotal: Double,
    val items: List<TransactionEntity>,
)

data class TransactionsUiState(
    val activeTab: Int = 0, // 0 = transactions, 1 = cards
    val groupedTransactions: List<GroupedTransactions> = emptyList(),
    val netBalance: Double = 0.0,
    val cards: List<CreditCardEntity> = emptyList(),
    val deleteTransactionTarget: TransactionEntity? = null,
    val deleteCardTarget: CreditCardEntity? = null,
)

class TransactionsViewModel(
    private val repository: RemainderrRepository,
) : ViewModel() {

    private val activeTab = MutableStateFlow(0)
    private val deleteTxTarget = MutableStateFlow<TransactionEntity?>(null)
    private val deleteCardTarget = MutableStateFlow<CreditCardEntity?>(null)

    val uiState: StateFlow<TransactionsUiState> = combine(
        repository.transactions,
        repository.creditCards,
        activeTab,
        deleteTxTarget,
        deleteCardTarget,
    ) { txs, cards, tab, delTx, delCard ->
        val zone = ZoneId.systemDefault()
        val displayFmt = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH)

        val groups = txs.groupBy { tx ->
            runCatching {
                Instant.parse(tx.date).atZone(zone).toLocalDate()
            }.getOrElse {
                runCatching { LocalDate.parse(tx.date.take(10)) }.getOrDefault(LocalDate.now())
            }
        }.entries
            .sortedByDescending { it.key }
            .map { (localDate, list) ->
                GroupedTransactions(
                    dateKey = localDate.toString(),
                    formattedDate = localDate.format(displayFmt),
                    dailyTotal = list.sumOf { it.amount },
                    items = list,
                )
            }

        TransactionsUiState(
            activeTab = tab,
            groupedTransactions = groups,
            netBalance = txs.sumOf { it.amount },
            cards = cards,
            deleteTransactionTarget = delTx,
            deleteCardTarget = delCard,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = TransactionsUiState(),
    )

    fun setActiveTab(tab: Int) {
        activeTab.value = tab
    }

    fun addTransaction(isExpense: Boolean, amount: Double, category: String, description: String) {
        val signedAmount = if (isExpense) -kotlin.math.abs(amount) else kotlin.math.abs(amount)
        val entry = TransactionEntity(
            id = System.currentTimeMillis().toString(),
            title = description.trim(),
            amount = signedAmount,
            category = category,
            date = Instant.now().toString(),
        )
        viewModelScope.launch {
            repository.addTransaction(entry)
        }
    }

    fun requestDeleteTransaction(tx: TransactionEntity) {
        deleteTxTarget.value = tx
    }

    fun dismissDeleteTransaction() {
        deleteTxTarget.value = null
    }

    fun confirmDeleteTransaction() {
        val target = deleteTxTarget.value ?: return
        deleteTxTarget.value = null
        viewModelScope.launch {
            repository.deleteTransaction(target.id)
        }
    }

    fun addCreditCard(
        bankName: String,
        cardNumber: String,
        expiry: String,
        type: String,
        cardHolder: String,
        colorHex: String,
    ) {
        val card = CreditCardEntity(
            id = System.currentTimeMillis().toString(),
            bankName = bankName.trim(),
            cardNumber = cardNumber.replace(" ", ""),
            expiry = expiry.trim(),
            type = type,
            cardHolder = cardHolder.trim(),
            color = colorHex,
        )
        viewModelScope.launch {
            repository.addCreditCard(card)
        }
    }

    fun requestDeleteCard(card: CreditCardEntity) {
        deleteCardTarget.value = card
    }

    fun dismissDeleteCard() {
        deleteCardTarget.value = null
    }

    fun confirmDeleteCard() {
        val target = deleteCardTarget.value ?: return
        deleteCardTarget.value = null
        viewModelScope.launch {
            repository.deleteCreditCard(target.id)
        }
    }

    class Factory(private val repository: RemainderrRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return TransactionsViewModel(repository) as T
        }
    }
}
