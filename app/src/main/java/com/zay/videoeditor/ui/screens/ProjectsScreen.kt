package com.zay.videoeditor.ui.screens

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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import android.widget.Toast
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import com.zay.videoeditor.ui.theme.NavyCardHigh
import com.zay.videoeditor.ui.theme.NeonPink
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zay.videoeditor.data.ProjectItem
import com.zay.videoeditor.data.ProjectStore
import com.zay.videoeditor.ui.components.GradientButton
import com.zay.videoeditor.ui.components.IconCircleButton
import com.zay.videoeditor.ui.components.NeonChip
import com.zay.videoeditor.ui.components.PanelTabs
import com.zay.videoeditor.ui.components.ProjectCard
import com.zay.videoeditor.ui.components.zayFieldColors
import com.zay.videoeditor.ui.theme.TextPrimary
import com.zay.videoeditor.ui.theme.TextSecondary

@Composable
fun ProjectsScreen(
    onNewProject: () -> Unit,
    onOpenProject: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var query by rememberSaveable { mutableStateOf("") }
    var showSearch by rememberSaveable { mutableStateOf(false) }
    var filter by rememberSaveable { mutableStateOf(0) }
    val filters = listOf("Semua", "Video", "Draf", "Favorit")

    var menuFor by remember { mutableStateOf<ProjectItem?>(null) }
    var confirmDelete by remember { mutableStateOf<ProjectItem?>(null) }
    var renameFor by remember { mutableStateOf<ProjectItem?>(null) }
    var renameText by remember { mutableStateOf("") }

    val shown = ProjectStore.projects.filter { p ->
        val byFilter = when (filter) {
            1 -> p.kind == "Video"
            2 -> p.kind == "Draf"
            3 -> p.favorite
            else -> true
        }
        byFilter && p.name.contains(query, ignoreCase = true)
    }

    Column(
        modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
            .padding(top = 12.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Proyek Saya",
                color = TextPrimary,
                fontFamily = androidx.compose.ui.text.font.FontFamily.Serif,
                fontSize = 26.sp,
                modifier = Modifier.weight(1f),
            )
            IconCircleButton(
                if (showSearch) Icons.Rounded.Close else Icons.Rounded.Search,
                onClick = {
                    showSearch = !showSearch
                    if (!showSearch) query = ""
                },
            )
            Spacer(Modifier.padding(horizontal = 4.dp))
            IconCircleButton(
                Icons.Rounded.MoreVert,
                onClick = {
                    Toast.makeText(context, "Ketuk titik tiga di kartu proyek untuk hapus atau favorit", Toast.LENGTH_SHORT).show()
                },
            )
        }

        if (showSearch) {
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                placeholder = { Text("Cari proyek") },
                leadingIcon = { Icon(Icons.Rounded.Search, null) },
                shape = RoundedCornerShape(18.dp),
                colors = zayFieldColors(),
            )
        }

        Spacer(Modifier.height(12.dp))
        PanelTabs(filters, filter) { filter = it }
        Spacer(Modifier.height(12.dp))

        Box(Modifier.weight(1f)) {
            if (shown.isEmpty()) {
                Column(
                    Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text("Tidak ada proyek", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text("Ketuk Proyek Baru untuk mulai mengedit.", color = TextSecondary, fontSize = 13.sp)
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 8.dp),
                ) {
                    items(shown, key = { it.id }) { item ->
                        ProjectCard(item, onClick = { onOpenProject(item.id) }, onMore = { menuFor = item })
                    }
                }
            }
        }

        GradientButton(
            text = "Proyek Baru",
            icon = Icons.Rounded.Add,
            onClick = onNewProject,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
        )
    }

    menuFor?.let { item ->
        AlertDialog(
            onDismissRequest = { menuFor = null },
            containerColor = NavyCardHigh,
            titleContentColor = TextPrimary,
            title = { Text(item.name) },
            text = {
                Column {
                    Text("${item.duration} • ${item.resolution}", color = TextSecondary)
                    TextButton(onClick = {
                        renameText = item.name
                        renameFor = item
                        menuFor = null
                    }) { Text("Ganti nama") }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = item
                    menuFor = null
                }) { Text("Hapus", color = NeonPink) }
            },
            dismissButton = {
                TextButton(onClick = {
                    ProjectStore.setFavorite(context, item.id, !item.favorite)
                    menuFor = null
                }) { Text(if (item.favorite) "Hapus favorit" else "Jadikan favorit") }
            },
        )
    }

    renameFor?.let { item ->
        AlertDialog(
            onDismissRequest = { renameFor = null },
            containerColor = NavyCardHigh,
            titleContentColor = TextPrimary,
            title = { Text("Ganti nama proyek") },
            text = {
                OutlinedTextField(
                    value = renameText,
                    onValueChange = { renameText = it },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = zayFieldColors(),
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    ProjectStore.rename(context, item.id, renameText)
                    renameFor = null
                }) { Text("Simpan") }
            },
            dismissButton = {
                TextButton(onClick = { renameFor = null }) { Text("Batal") }
            },
        )
    }

    confirmDelete?.let { item ->
        AlertDialog(
            onDismissRequest = { confirmDelete = null },
            containerColor = NavyCardHigh,
            titleContentColor = TextPrimary,
            textContentColor = TextSecondary,
            title = { Text("Hapus proyek?") },
            text = { Text("\"${item.name}\" dan salinan videonya akan dihapus dari aplikasi. Video asli di galeri tidak ikut terhapus.") },
            confirmButton = {
                TextButton(onClick = {
                    ProjectStore.delete(context, item.id)
                    confirmDelete = null
                }) { Text("Hapus", color = NeonPink) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = null }) { Text("Batal") }
            },
        )
    }
}
