package com.example.ui.passwords

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
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
import androidx.compose.foundation.layout.defaultMinSize
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PersonOutline
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.local.PasswordEntity
import com.example.ui.theme.RemainderrTheme

private data class SiteBrandVisual(
    val color: Color,
    val badgeText: String?,
    val icon: ImageVector?,
)

private fun getSiteBrandVisual(siteName: String): SiteBrandVisual {
    val name = siteName.lowercase().trim()
    return when {
        name.contains("paypal") -> SiteBrandVisual(Color(0xFF003087), "P", null)
        name.contains("facebook") -> SiteBrandVisual(Color(0xFF1877F2), "f", null)
        name.contains("instagram") -> SiteBrandVisual(Color(0xFFE4405F), "IG", null)
        name.contains("debit") || name.contains("credit") || name.contains("visa") ->
            SiteBrandVisual(Color(0xFF1A1F71), null, Icons.Filled.CreditCard)
        name.contains("mastercard") -> SiteBrandVisual(Color(0xFFEB001B), null, Icons.Filled.CreditCard)
        name.contains("google") -> SiteBrandVisual(Color(0xFF4285F4), "G", null)
        name.contains("twitter") || name.contains("x") -> SiteBrandVisual(Color(0xFF1DA1F2), "X", null)
        name.contains("github") -> SiteBrandVisual(Color(0xFF7E7B7B), "GH", null)
        name.contains("apple") -> SiteBrandVisual(Color(0xFF000000), "A", null)
        else -> SiteBrandVisual(Color(0xFF6C757D), null, Icons.Filled.Key)
    }
}

@Composable
fun PasswordsScreen(
    viewModel: PasswordsViewModel,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val palette = RemainderrTheme.palette
    val context = LocalContext.current

    var addDialogVisible by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(palette.background)
            .statusBarsPadding(),
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(top = 15.dp, bottom = 25.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.passwords_title),
                color = palette.title,
                fontSize = 26.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = (-0.5).sp,
            )
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(palette.accent)
                    .clickable { addDialogVisible = true }
                    .testTag("add_password_button"),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = stringResource(R.string.new_password_entry),
                    tint = Color.White,
                    modifier = Modifier.size(20.dp),
                )
            }
        }

        if (state.passwords.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 80.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(
                    imageVector = Icons.Filled.Key,
                    contentDescription = null,
                    tint = palette.border,
                    modifier = Modifier.size(48.dp),
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "No passwords saved yet.",
                    color = palette.subtext,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                items(state.passwords, key = { it.id }) { item ->
                    PasswordCard(
                        item = item,
                        isPasswordShown = item.id in state.visiblePasswordIds,
                        onToggleVisibility = { viewModel.togglePasswordVisibility(item.id) },
                        onCopyUsername = {
                            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            cm.setPrimaryClip(ClipData.newPlainText("Username", item.username))
                        },
                        onCopyPassword = {
                            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            cm.setPrimaryClip(ClipData.newPlainText("Password", item.password))
                        },
                        onDelete = { viewModel.requestDelete(item.id) },
                    )
                }
                item { Spacer(modifier = Modifier.height(24.dp)) }
            }
        }
    }

    if (addDialogVisible) {
        AddPasswordDialog(
            onDismiss = { addDialogVisible = false },
            onSave = { site, user, pass ->
                viewModel.addPassword(site, user, pass)
                addDialogVisible = false
            },
        )
    }

    if (state.deleteTargetId != null) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissDelete() },
            containerColor = palette.card,
            title = {
                Text(
                    text = stringResource(R.string.delete_credential_title),
                    color = palette.title,
                    fontWeight = FontWeight.ExtraBold,
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.delete_credential_subtext),
                    color = palette.subtext,
                )
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissDelete() }) {
                    Text(stringResource(R.string.action_cancel), color = palette.subtext, fontWeight = FontWeight.SemiBold)
                }
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.confirmDelete() },
                    colors = ButtonDefaults.buttonColors(containerColor = palette.delete.copy(alpha = 0.14f)),
                ) {
                    Text(stringResource(R.string.action_delete), color = palette.delete, fontWeight = FontWeight.Bold)
                }
            },
        )
    }
}

@Composable
private fun PasswordCard(
    item: PasswordEntity,
    isPasswordShown: Boolean,
    onToggleVisibility: () -> Unit,
    onCopyUsername: () -> Unit,
    onCopyPassword: () -> Unit,
    onDelete: () -> Unit,
) {
    val palette = RemainderrTheme.palette
    val brand = remember(item.site) { getSiteBrandVisual(item.site) }
    var menuOpen by remember { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = palette.card),
        border = BorderStroke(1.dp, palette.border),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(brand.color.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center,
            ) {
                if (brand.icon != null) {
                    Icon(
                        imageVector = brand.icon,
                        contentDescription = item.site,
                        tint = brand.color,
                        modifier = Modifier.size(22.dp),
                    )
                } else {
                    Text(
                        text = brand.badgeText.orEmpty(),
                        color = if (brand.color == Color(0xFF000000) && palette.isDark) Color.White else brand.color,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold,
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.site,
                    color = palette.title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (isPasswordShown) item.password else "●●●●●●●●",
                    color = palette.title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.5.sp,
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onToggleVisibility,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("toggle_password_${item.id}"),
                ) {
                    Icon(
                        imageVector = if (isPasswordShown) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                        contentDescription = if (isPasswordShown) "Hide password" else "Show password",
                        tint = palette.title,
                        modifier = Modifier.size(22.dp),
                    )
                }

                Box {
                    IconButton(
                        onClick = { menuOpen = true },
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("password_menu_${item.id}"),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.MoreVert,
                            contentDescription = "Credential actions",
                            tint = palette.title,
                            modifier = Modifier.size(20.dp),
                        )
                    }

                    DropdownMenu(
                        expanded = menuOpen,
                        onDismissRequest = { menuOpen = false },
                        containerColor = palette.card,
                    ) {
                        DropdownMenuItem(
                            text = { Text("Copy Username", color = palette.title, fontSize = 13.sp, fontWeight = FontWeight.SemiBold) },
                            leadingIcon = { Icon(Icons.Filled.PersonOutline, contentDescription = null, tint = palette.title, modifier = Modifier.size(16.dp)) },
                            onClick = { menuOpen = false; onCopyUsername() },
                        )
                        DropdownMenuItem(
                            text = { Text("Copy Password", color = palette.title, fontSize = 13.sp, fontWeight = FontWeight.SemiBold) },
                            leadingIcon = { Icon(Icons.Filled.ContentCopy, contentDescription = null, tint = palette.title, modifier = Modifier.size(16.dp)) },
                            onClick = { menuOpen = false; onCopyPassword() },
                        )
                        HorizontalDivider(color = palette.border)
                        DropdownMenuItem(
                            text = { Text("Delete", color = palette.delete, fontSize = 13.sp, fontWeight = FontWeight.SemiBold) },
                            leadingIcon = { Icon(Icons.Filled.DeleteOutline, contentDescription = null, tint = palette.delete, modifier = Modifier.size(16.dp)) },
                            onClick = { menuOpen = false; onDelete() },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AddPasswordDialog(
    onDismiss: () -> Unit,
    onSave: (site: String, username: String, password: String) -> Unit,
) {
    val palette = RemainderrTheme.palette
    var site by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = palette.background,
            border = BorderStroke(1.dp, palette.border),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.new_password_entry),
                        color = palette.title,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(48.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = stringResource(R.string.action_cancel),
                            tint = palette.subtext,
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(15.dp))
                        .background(palette.card)
                        .border(1.dp, palette.border, RoundedCornerShape(15.dp))
                        .padding(horizontal = 15.dp),
                ) {
                    VaultField(
                        value = site,
                        onValueChange = { site = it },
                        placeholder = "Site / App",
                        visualTransformation = VisualTransformation.None,
                        testTag = "password_site_input",
                    )
                    HorizontalDivider(color = palette.border)
                    VaultField(
                        value = username,
                        onValueChange = { username = it },
                        placeholder = "Username",
                        visualTransformation = VisualTransformation.None,
                        testTag = "password_user_input",
                    )
                    HorizontalDivider(color = palette.border)
                    VaultField(
                        value = password,
                        onValueChange = { password = it },
                        placeholder = "Password",
                        visualTransformation = PasswordVisualTransformation(),
                        testTag = "password_pass_input",
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 50.dp)
                        .clip(RoundedCornerShape(15.dp))
                        .background(palette.accentLight)
                        .border(2.dp, palette.accent, RoundedCornerShape(15.dp))
                        .clickable {
                            if (site.isNotBlank() && username.isNotBlank() && password.isNotBlank()) {
                                onSave(site, username, password)
                            }
                        }
                        .testTag("save_password_button"),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(R.string.action_save),
                        color = palette.accent,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold,
                    )
                }
            }
        }
    }
}

@Composable
private fun VaultField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    visualTransformation: VisualTransformation,
    testTag: String,
) {
    val palette = RemainderrTheme.palette
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        if (value.isEmpty()) {
            Text(
                text = placeholder,
                color = palette.subtext,
                fontSize = 16.sp,
            )
        }
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            visualTransformation = visualTransformation,
            textStyle = TextStyle(color = palette.text, fontSize = 16.sp),
            cursorBrush = SolidColor(palette.accent),
            modifier = Modifier
                .fillMaxWidth()
                .testTag(testTag),
        )
    }
}
