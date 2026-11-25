package com.example.ui.transactions

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.local.CreditCardEntity
import com.example.data.local.TransactionEntity
import com.example.ui.theme.RemainderrTheme
import java.text.NumberFormat
import java.util.Locale

private data class CategoryVisual(val icon: ImageVector, val color: Color)

private fun categoryVisual(category: String, fallbackColor: Color): CategoryVisual {
    return when (category) {
        "Food" -> CategoryVisual(Icons.Filled.Restaurant, Color(0xFFFF9500))
        "Travel" -> CategoryVisual(Icons.Filled.DirectionsCar, Color(0xFF5AC8FA))
        "Shopping" -> CategoryVisual(Icons.Filled.ShoppingCart, Color(0xFFFF2D55))
        "Health" -> CategoryVisual(Icons.Filled.Favorite, Color(0xFF4CD964))
        "Other" -> CategoryVisual(Icons.Filled.MoreHoriz, Color(0xFF8E8E93))
        else -> CategoryVisual(Icons.Filled.MoreHoriz, fallbackColor)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionsScreen(
    viewModel: TransactionsViewModel,
    onAddTransactionClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val palette = RemainderrTheme.palette

    var addCardSheetVisible by remember { mutableStateOf(false) }
    var bankName by remember { mutableStateOf("") }
    var cardHolder by remember { mutableStateOf("") }
    var cardNumber by remember { mutableStateOf("") }
    var expiry by remember { mutableStateOf("") }
    var cardType by remember { mutableStateOf("Visa") }
    var cardError by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(palette.background)
            .statusBarsPadding(),
    ) {
        // Dynamic Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(top = 15.dp, bottom = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = if (state.activeTab == 0) {
                    stringResource(R.string.transactions_title)
                } else {
                    stringResource(R.string.credit_cards_title)
                },
                color = palette.title,
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = (-0.5).sp,
            )
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(palette.accent)
                    .testTag("add_cash_button")
                    .clickable {
                        if (state.activeTab == 0) onAddTransactionClick()
                        else addCardSheetVisible = true
                    },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = "Add",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp),
                )
            }
        }

        // Segmented Control Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 10.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(palette.card)
                .border(1.dp, palette.border, RoundedCornerShape(16.dp))
                .padding(4.dp),
        ) {
            listOf("Transactions", "Cards").forEachIndexed { index, label ->
                val selected = state.activeTab == index
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (selected) palette.accent else Color.Transparent)
                        .clickable { viewModel.setActiveTab(index) }
                        .padding(vertical = 10.dp)
                        .testTag("cash_tab_$index"),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = label,
                        color = if (selected) Color.White else palette.subtext,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }

        if (state.activeTab == 0) {
            // Transactions View
            val balanceFmt = remember(state.netBalance) {
                NumberFormat.getNumberInstance(Locale.US).apply {
                    minimumFractionDigits = 2
                    maximumFractionDigits = 2
                }.format(state.netBalance)
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                item {
                    // Net Balance Card
                    Card(
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = palette.card),
                        border = BorderStroke(1.dp, palette.border),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Filled.AccountBalanceWallet,
                                        contentDescription = null,
                                        tint = palette.accent,
                                        modifier = Modifier.size(14.dp),
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = stringResource(R.string.net_balance).uppercase(Locale.US),
                                        color = palette.subtext,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.5.sp,
                                    )
                                }
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                                    contentDescription = null,
                                    tint = palette.accent,
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                            Text(
                                text = "LKR $balanceFmt",
                                color = palette.title,
                                fontSize = 25.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = (-1).sp,
                            )
                        }
                    }
                }

                if (state.groupedTransactions.isEmpty()) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 60.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Icon(
                                imageVector = Icons.Filled.AccountBalanceWallet,
                                contentDescription = null,
                                tint = palette.border,
                                modifier = Modifier.size(48.dp),
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = stringResource(R.string.no_transactions_yet),
                                color = palette.subtext,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium,
                            )
                        }
                    }
                } else {
                    items(state.groupedTransactions, key = { it.dateKey }) { group ->
                        Card(
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = palette.card),
                            border = BorderStroke(1.dp, palette.border),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Column {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        text = group.formattedDate,
                                        color = palette.subtext,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                    )
                                    val prefix = if (group.dailyTotal >= 0) "+" else ""
                                    Text(
                                        text = "${prefix}LKR ${String.format(Locale.US, "%.2f", group.dailyTotal)}",
                                        color = palette.title,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                    )
                                }
                                HorizontalDivider(thickness = 0.5.dp, color = palette.border)

                                group.items.forEachIndexed { idx, tx ->
                                    SwipeableTransactionRow(
                                        tx = tx,
                                        isLast = idx == group.items.lastIndex,
                                        onRequestDelete = { viewModel.requestDeleteTransaction(tx) },
                                    )
                                }
                            }
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(16.dp)) }
            }
        } else {
            // Cards View
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                if (state.cards.isEmpty()) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 80.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Icon(
                                imageVector = Icons.Filled.CreditCard,
                                contentDescription = null,
                                tint = palette.border,
                                modifier = Modifier.size(48.dp),
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = stringResource(R.string.no_cards_yet),
                                color = palette.subtext,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium,
                            )
                        }
                    }
                } else {
                    items(state.cards, key = { it.id }) { card ->
                        CreditCardItem(
                            card = card,
                            onDelete = { viewModel.requestDeleteCard(card) },
                        )
                    }
                }
            }
        }
    }

    // Delete Transaction Confirmation
    state.deleteTransactionTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { viewModel.dismissDeleteTransaction() },
            containerColor = palette.card,
            title = { Text("Delete Transaction", color = palette.title, fontWeight = FontWeight.ExtraBold) },
            text = {
                Text(
                    text = "Are you sure you want to remove \"${target.title}\"?",
                    color = palette.subtext,
                )
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissDeleteTransaction() }) {
                    Text(stringResource(R.string.action_cancel), color = palette.subtext, fontWeight = FontWeight.SemiBold)
                }
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.confirmDeleteTransaction() },
                    colors = ButtonDefaults.buttonColors(containerColor = palette.delete),
                ) {
                    Text(stringResource(R.string.action_delete), color = Color.White, fontWeight = FontWeight.SemiBold)
                }
            },
        )
    }

    // Delete Card Confirmation
    state.deleteCardTarget?.let {
        AlertDialog(
            onDismissRequest = { viewModel.dismissDeleteCard() },
            containerColor = palette.card,
            title = { Text("Delete Card", color = palette.title, fontWeight = FontWeight.ExtraBold) },
            text = { Text("Are you sure you want to remove this card?", color = palette.subtext) },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissDeleteCard() }) {
                    Text(stringResource(R.string.action_cancel), color = palette.subtext)
                }
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.confirmDeleteCard() },
                    colors = ButtonDefaults.buttonColors(containerColor = palette.delete),
                ) {
                    Text(stringResource(R.string.action_delete), color = Color.White)
                }
            },
        )
    }

    // Add Card Modal Sheet
    if (addCardSheetVisible) {
        ModalBottomSheet(
            onDismissRequest = { addCardSheetVisible = false },
            containerColor = palette.card,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 28.dp),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 18.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Add New Card", color = palette.title, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                    IconButton(onClick = { addCardSheetVisible = false }) {
                        Icon(Icons.Filled.Close, contentDescription = "Close", tint = palette.subtext)
                    }
                }

                // Visa / Mastercard Selector
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    listOf("Visa", "Mastercard").forEach { typeOption ->
                        val selected = cardType == typeOption
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (selected) palette.accent else Color.Transparent)
                                .border(
                                    1.dp,
                                    if (selected) palette.accent else palette.border,
                                    RoundedCornerShape(14.dp),
                                )
                                .clickable { cardType = typeOption }
                                .padding(vertical = 12.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                imageVector = Icons.Filled.CreditCard,
                                contentDescription = null,
                                tint = if (selected) Color.White else palette.subtext,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = typeOption,
                                color = if (selected) Color.White else palette.subtext,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                            )
                        }
                    }
                }

                CardInputField(value = bankName, onValueChange = { bankName = it }, placeholder = "Bank Name")
                CardInputField(value = cardHolder, onValueChange = { cardHolder = it }, placeholder = "Card Holder")
                CardInputField(
                    value = cardNumber,
                    onValueChange = { input ->
                        val filtered = input.filter { it.isDigit() }.take(16)
                        cardNumber = filtered
                        if (filtered.startsWith("4")) cardType = "Visa"
                        else if (filtered.startsWith("5")) cardType = "Mastercard"
                    },
                    placeholder = "Card Number",
                    keyboardType = KeyboardType.Number,
                )
                CardInputField(value = expiry, onValueChange = { expiry = it }, placeholder = "Expiry (MM/YY)")

                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = {
                        if (bankName.isBlank() || cardNumber.length < 4) {
                            cardError = true
                        } else {
                            viewModel.addCreditCard(
                                bankName = bankName,
                                cardNumber = cardNumber,
                                expiry = expiry,
                                type = cardType,
                                cardHolder = cardHolder,
                                colorHex = if (palette.isDark) "#0A84FF" else "#007AFF",
                            )
                            bankName = ""
                            cardHolder = ""
                            cardNumber = ""
                            expiry = ""
                            cardType = "Visa"
                            addCardSheetVisible = false
                        }
                    },
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = palette.accent),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                ) {
                    Text("Save Card", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    if (cardError) {
        AlertDialog(
            onDismissRequest = { cardError = false },
            containerColor = palette.card,
            title = { Text("Error", color = palette.title, fontWeight = FontWeight.Bold) },
            text = { Text("Please fill in all details", color = palette.subtext) },
            confirmButton = {
                TextButton(onClick = { cardError = false }) {
                    Text("OK", color = palette.accent, fontWeight = FontWeight.Bold)
                }
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeableTransactionRow(
    tx: TransactionEntity,
    isLast: Boolean,
    onRequestDelete: () -> Unit,
) {
    val palette = RemainderrTheme.palette
    val dismissState = rememberSwipeToDismissBoxState()

    LaunchedEffect(dismissState.currentValue) {
        if (dismissState.currentValue == SwipeToDismissBoxValue.StartToEnd) {
            onRequestDelete()
            dismissState.snapTo(SwipeToDismissBoxValue.Settled)
        }
    }

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromEndToStart = false,
        backgroundContent = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(palette.delete)
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                Icon(
                    imageVector = Icons.Filled.DeleteOutline,
                    contentDescription = "Delete",
                    tint = Color.White,
                )
            }
        },
    ) {
        val visual = categoryVisual(tx.category, palette.subtext)
        val isIncome = tx.amount > 0

        Column(modifier = Modifier.background(palette.card)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(visual.color.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = visual.icon,
                        contentDescription = tx.category,
                        tint = visual.color,
                        modifier = Modifier.size(18.dp),
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                    Text(
                        text = tx.title,
                        color = palette.title,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = tx.category,
                        color = palette.subtext,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (isIncome) Icons.Filled.ArrowUpward else Icons.Filled.ArrowDownward,
                        contentDescription = null,
                        tint = palette.subtext,
                        modifier = Modifier.size(14.dp),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "LKR ${String.format(Locale.US, "%.2f", kotlin.math.abs(tx.amount))}",
                        color = palette.subtext,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold,
                    )
                }
            }
            if (!isLast) {
                HorizontalDivider(thickness = 0.5.dp, color = palette.border)
            }
        }
    }
}

@Composable
private fun CreditCardItem(
    card: CreditCardEntity,
    onDelete: () -> Unit,
) {
    val palette = RemainderrTheme.palette
    val formattedNumber = remember(card.cardNumber) {
        card.cardNumber.replace(" ", "").chunked(4).joinToString(" ")
    }

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = palette.accent),
        elevation = CardDefaults.cardElevation(defaultElevation = 5.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(22.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = card.bankName,
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.5.sp,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = card.type.uppercase(Locale.US),
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.padding(end = 12.dp),
                    )
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.Filled.Delete,
                            contentDescription = "Delete card",
                            tint = Color.White.copy(alpha = 0.85f),
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }

            Text(
                text = formattedNumber,
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.5.sp,
                modifier = Modifier.padding(bottom = 24.dp),
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom,
            ) {
                Column {
                    Text(
                        text = "CARD HOLDER",
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 4.dp),
                    )
                    Text(
                        text = card.cardHolder.ifBlank { "---" },
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.sp,
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "EXPIRY",
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 4.dp),
                    )
                    Text(
                        text = card.expiry.ifBlank { "MM/YY" },
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}

@Composable
private fun CardInputField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    keyboardType: KeyboardType = KeyboardType.Text,
) {
    val palette = RemainderrTheme.palette
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(placeholder, color = palette.subtext.copy(alpha = 0.6f)) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        shape = RoundedCornerShape(18.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = palette.background,
            unfocusedContainerColor = palette.background,
            focusedBorderColor = palette.accent,
            unfocusedBorderColor = palette.border,
            focusedTextColor = palette.title,
            unfocusedTextColor = palette.title,
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp),
    )
}
