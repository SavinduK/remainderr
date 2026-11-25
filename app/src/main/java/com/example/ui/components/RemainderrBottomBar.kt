package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.StickyNote2
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.RemainderrTheme

enum class MainTab {
    TASKS,
    DIARY,
    CASH,
    NOTES,
    SETTINGS,
}

private data class TabItem(
    val tab: MainTab,
    val labelRes: Int,
    val icon: ImageVector,
    val testTag: String,
)

@Composable
fun RemainderrBottomBar(
    currentTab: MainTab?,
    onNavigateTab: (MainTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = RemainderrTheme.palette
    val items = listOf(
        TabItem(MainTab.TASKS, R.string.nav_tasks, Icons.AutoMirrored.Filled.FormatListBulleted, "nav_tasks"),
        TabItem(MainTab.DIARY, R.string.nav_diary, Icons.AutoMirrored.Filled.MenuBook, "nav_diary"),
        TabItem(MainTab.CASH, R.string.nav_cash, Icons.Filled.AttachMoney, "nav_cash"),
        TabItem(MainTab.NOTES, R.string.nav_notes, Icons.AutoMirrored.Filled.StickyNote2, "nav_notes"),
        TabItem(MainTab.SETTINGS, R.string.nav_settings, Icons.Filled.Settings, "nav_settings"),
    )

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = palette.card,
        shadowElevation = 6.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(palette.card)
                .navigationBarsPadding(),
        ) {
            HorizontalDivider(thickness = 1.dp, color = palette.border)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp, bottom = 10.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                items.forEach { item ->
                    val isSelected = currentTab == item.tab
                    val tint = if (isSelected) palette.accent else palette.subtext
                    val label = stringResource(item.labelRes)
                    Column(
                        modifier = Modifier
                            .defaultMinSize(minWidth = 64.dp, minHeight = 48.dp)
                            .testTag(item.testTag)
                            .clickable { onNavigateTab(item.tab) }
                            .padding(vertical = 4.dp, horizontal = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = label,
                            tint = tint,
                            modifier = Modifier.size(22.dp),
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = label,
                            color = tint,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }
            }
        }
    }
}
