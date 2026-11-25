package com.example.ui.transactions

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.RemainderrTheme
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddTransactionScreen(
    onBack: () -> Unit,
    onSave: (isExpense: Boolean, amount: Double, category: String, description: String) -> Unit,
) {
    val palette = RemainderrTheme.palette

    var isExpense by remember { mutableStateOf(true) }
    var amountText by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Food") }
    var errorDialog by remember { mutableStateOf<Pair<String, String>?>(null) }

    val categories = listOf(
        "Food" to Icons.Filled.Restaurant,
        "Travel" to Icons.Filled.DirectionsCar,
        "Shopping" to Icons.Filled.ShoppingCart,
        "Health" to Icons.Filled.Favorite,
        "Other" to Icons.Filled.MoreHoriz,
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
            .verticalScroll(rememberScrollState()),
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.size(48.dp),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.action_back),
                    tint = palette.title,
                )
            }
            Text(
                text = stringResource(R.string.new_transaction_title),
                color = palette.title,
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = (-0.5).sp,
            )
            Spacer(modifier = Modifier.size(48.dp))
        }

        Column(modifier = Modifier.padding(horizontal = 25.dp)) {
            // Income / Expense Switch
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(palette.card)
                    .border(1.dp, palette.border, RoundedCornerShape(18.dp))
                    .padding(6.dp),
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (!isExpense) palette.success else Color.Transparent)
                        .clickable { isExpense = false }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "Income",
                        color = if (!isExpense) Color.White else palette.subtext,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                    )
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (isExpense) palette.delete else Color.Transparent)
                        .clickable { isExpense = true }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "Expense",
                        color = if (isExpense) Color.White else palette.subtext,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                    )
                }
            }

            // Amount Input
            Text(
                text = "AMOUNT (LKR)",
                color = palette.accent,
                fontSize = 12.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(bottom = 6.dp),
            )
            TextField(
                value = amountText,
                onValueChange = { amountText = it },
                placeholder = { Text("0.00", color = palette.border, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                textStyle = TextStyle(color = palette.title, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = palette.accent,
                    unfocusedIndicatorColor = palette.border,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("tx_amount_input"),
            )

            // Description Input
            Text(
                text = "DESCRIPTION",
                color = palette.accent,
                fontSize = 12.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(top = 28.dp, bottom = 10.dp),
            )
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                placeholder = { Text("What was this for?", color = palette.subtext) },
                singleLine = true,
                shape = RoundedCornerShape(20.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = palette.card,
                    unfocusedContainerColor = palette.card,
                    focusedBorderColor = palette.accent,
                    unfocusedBorderColor = palette.border,
                    focusedTextColor = palette.title,
                    unfocusedTextColor = palette.title,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("tx_description_input"),
            )

            // Category Grid
            Text(
                text = "CATEGORY",
                color = palette.accent,
                fontSize = 12.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(top = 28.dp, bottom = 10.dp),
            )
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                maxItemsInEachRow = 3,
            ) {
                categories.forEach { (label, icon) ->
                    val selected = category == label
                    Column(
                        modifier = Modifier
                            .fillMaxWidth(0.30f)
                            .clip(RoundedCornerShape(20.dp))
                            .background(palette.card)
                            .border(
                                width = 2.dp,
                                color = if (selected) palette.accent else palette.border,
                                shape = RoundedCornerShape(20.dp),
                            )
                            .clickable { category = label }
                            .padding(vertical = 18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = label,
                            tint = if (selected) palette.accent else palette.icon,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = label,
                            color = if (selected) palette.title else palette.subtext,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(36.dp))
            Button(
                onClick = {
                    val parsed = amountText.toDoubleOrNull()
                    when {
                        parsed == null || parsed <= 0.0 -> {
                            errorDialog = "Invalid Amount" to "Please enter a valid numeric value."
                        }
                        description.isBlank() -> {
                            errorDialog = "Missing Detail" to "Please provide a description."
                        }
                        else -> {
                            onSave(isExpense, parsed, category, description)
                            onBack()
                        }
                    }
                },
                shape = RoundedCornerShape(22.dp),
                colors = ButtonDefaults.buttonColors(containerColor = palette.accent),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 5.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp)
                    .testTag("save_transaction_button"),
            ) {
                Text(
                    text = "Save Transaction",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold,
                )
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    errorDialog?.let { (errTitle, errMsg) ->
        AlertDialog(
            onDismissRequest = { errorDialog = null },
            containerColor = palette.card,
            title = { Text(errTitle, color = palette.title, fontWeight = FontWeight.Bold) },
            text = { Text(errMsg, color = palette.subtext) },
            confirmButton = {
                TextButton(onClick = { errorDialog = null }) {
                    Text("OK", color = palette.accent, fontWeight = FontWeight.Bold)
                }
            },
        )
    }
}
