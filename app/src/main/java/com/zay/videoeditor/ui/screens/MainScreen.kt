package com.zay.videoeditor.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import com.zay.videoeditor.data.ProjectStore
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zay.videoeditor.ui.components.ZayBackground
import com.zay.videoeditor.ui.theme.Platinum

private data class NavItem(val label: String, val icon: ImageVector)

private val navItems = listOf(
    NavItem("Beranda", Icons.Rounded.Home),
    NavItem("Proyek", Icons.Rounded.Folder),
    NavItem("Template", Icons.Rounded.Dashboard),
    NavItem("Profil", Icons.Rounded.Person),
)

@Composable
fun MainScreen(onNewProject: () -> Unit, onOpenProject: (Int) -> Unit) {
    var tab by rememberSaveable { mutableStateOf(0) }
    val context = LocalContext.current
    LaunchedEffect(Unit) { ProjectStore.refresh(context) }

    ZayBackground {
        Column(Modifier.fillMaxSize()) {
            Box(Modifier.weight(1f)) {
                when (tab) {
                    0 -> HomeScreen(
                        onNewProject = onNewProject,
                        onOpenProject = onOpenProject,
                        onSeeAll = { tab = 1 },
                        onSettings = { tab = 3 },
                        onTemplate = { tab = 2 },
                    )
                    1 -> ProjectsScreen(onNewProject = onNewProject, onOpenProject = onOpenProject)
                    2 -> TemplateScreen()
                    else -> ProfileScreen()
                }
            }
            ZayBottomBar(selected = tab, onSelect = { tab = it })
        }
    }
}

@Composable
private fun ZayBottomBar(selected: Int, onSelect: (Int) -> Unit) {
    val shape = RoundedCornerShape(35.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .clip(shape)
            .background(Color(0xE612121A))
            .border(1.dp, Platinum.copy(alpha = 0.25f), shape)
            .padding(horizontal = 6.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        navItems.forEachIndexed { index, item ->
            val isSelected = index == selected
            val tint = if (isSelected) Platinum else Color(0xFF7D8499)
            Column(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(20.dp))
                    .clickable { onSelect(index) }
                    .padding(vertical = 2.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Icon(item.icon, item.label, tint = tint, modifier = Modifier.size(22.dp))
                Text(
                    item.label,
                    color = tint,
                    fontSize = 10.sp,
                    letterSpacing = 0.6.sp,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                )
                Box(
                    Modifier
                        .size(4.dp)
                        .clip(CircleShape)
                        .background(if (isSelected) Platinum else Color.Transparent),
                )
            }
        }
    }
}
