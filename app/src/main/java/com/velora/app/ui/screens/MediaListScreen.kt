package com.velora.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.velora.app.FilterTab
import com.velora.app.PlayerState
import com.velora.app.model.MediaItem
import com.velora.app.model.Playlist
import com.velora.app.ui.components.LiquidGlassSurface
import com.velora.app.util.MediaRepository
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MediaListScreen(
    state: PlayerState,
    filteredItems: List<MediaItem>,
    onItemClick: (MediaItem) -> Unit,
    onFilterChange: (FilterTab) -> Unit,
    onImportZip: () -> Unit,
    onCreatePlaylist: (String) -> Unit,
    onPlayPlaylist: (Playlist) -> Unit,
    onAddToFavourites: (MediaItem) -> Unit,
    isFavourite: (MediaItem) -> Boolean,
    onMergePlaylists: (List<Long>, String, Boolean) -> Unit,
    onDeletePlaylist: (Long) -> Unit,
    onRenamePlaylist: (Long, String) -> Unit,
    onAddToPlaylist: (Long, MediaItem) -> Unit,
    onRemoveImported: (Long) -> Unit,
    onMultiDeleteMedia: (Set<Long>, Boolean) -> Unit,
    onMultiDeletePlaylists: (Set<Long>, Boolean) -> Unit,
    nonFavPlaylists: List<Playlist>,
    modifier: Modifier = Modifier
) {
    var showNewPlaylistDialog by remember { mutableStateOf(false) }
    var showMergeDialog by remember { mutableStateOf(false) }
    var addToPlaylistItem by remember { mutableStateOf<MediaItem?>(null) }
    var multiSelectMode by remember { mutableStateOf(false) }
    var selectedMediaIds by remember { mutableStateOf(setOf<Long>()) }
    var selectedPlaylistIds by remember { mutableStateOf(setOf<Long>()) }
    var showMultiDeleteMediaDialog by remember { mutableStateOf(false) }
    var showMultiDeletePlaylistDialog by remember { mutableStateOf(false) }

    // Multi-delete dialogs
    if (showMultiDeleteMediaDialog) {
        MultiDeleteDialog(
            count = selectedMediaIds.size,
            entityName = "file",
            onDeleteWithFiles = { onMultiDeleteMedia(selectedMediaIds, true); selectedMediaIds = emptySet(); multiSelectMode = false; showMultiDeleteMediaDialog = false },
            onDeleteWithoutFiles = { onMultiDeleteMedia(selectedMediaIds, false); selectedMediaIds = emptySet(); multiSelectMode = false; showMultiDeleteMediaDialog = false },
            onDismiss = { showMultiDeleteMediaDialog = false }
        )
    }
    if (showMultiDeletePlaylistDialog) {
        MultiDeleteDialog(
            count = selectedPlaylistIds.size,
            entityName = "playlist",
            onDeleteWithFiles = { onMultiDeletePlaylists(selectedPlaylistIds, true); selectedPlaylistIds = emptySet(); multiSelectMode = false; showMultiDeletePlaylistDialog = false },
            onDeleteWithoutFiles = { onMultiDeletePlaylists(selectedPlaylistIds, false); selectedPlaylistIds = emptySet(); multiSelectMode = false; showMultiDeletePlaylistDialog = false },
            onDismiss = { showMultiDeletePlaylistDialog = false }
        )
    }

    Column(modifier = modifier.fillMaxSize()) {
        // ── Header — hugs status bar ───────────────────────────────────────────
        Box(modifier = Modifier.fillMaxWidth()
            .background(Brush.verticalGradient(listOf(
                MaterialTheme.colorScheme.background,
                MaterialTheme.colorScheme.background.copy(alpha = 0f))))
            .statusBarsPadding()
            .padding(bottom = 8.dp, start = 24.dp, end = 24.dp, top = 8.dp)
        ) {
            Column {
                Row(modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Library", style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (state.filterTab == FilterTab.PLAYLISTS) {
                            LiquidGlassSurface(cornerRadius = 999.dp, alpha = 0.15f) {
                                IconButton(onClick = { showMergeDialog = true }) {
                                    Icon(Icons.Rounded.MergeType, "Merge", tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(20.dp))
                                }
                            }
                        }
                        LiquidGlassSurface(cornerRadius = 999.dp, alpha = 0.15f) {
                            IconButton(onClick = { showNewPlaylistDialog = true }) {
                                Icon(Icons.Rounded.PlaylistAdd, "New Playlist", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
                            }
                        }
                        LiquidGlassSurface(cornerRadius = 999.dp, alpha = 0.15f) {
                            IconButton(onClick = onImportZip) {
                                Icon(Icons.Rounded.FolderZip, "Import ZIP", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
                            }
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterTab.values().forEach { tab -> FilterChip(tab, state.filterTab == tab) { onFilterChange(tab) } }
                }
            }
        }

        when {
            state.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
            state.filterTab == FilterTab.PLAYLISTS -> PlaylistsView(
                playlists = state.playlists, mediaList = state.mediaList + state.extraMediaList,
                onPlayPlaylist = onPlayPlaylist, onDeletePlaylist = onDeletePlaylist,
                onRenamePlaylist = onRenamePlaylist, modifier = Modifier.fillMaxSize())
            filteredItems.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Rounded.LibraryMusic, null, modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f))
                    Spacer(Modifier.height(12.dp))
                    Text("No media found", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                }
            }
            else -> LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 160.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)) {
                items(filteredItems, key = { it.id }) { item ->
                    MediaRow(item = item,
                        isPlaying = state.currentItem?.id == item.id && state.isPlaying,
                        isFavourite = isFavourite(item),
                        isImported = state.extraMediaList.any { it.id == item.id },
                        onClick = { onItemClick(item) },
                        onFavourite = { onAddToFavourites(item) },
                        onLongPress = { addToPlaylistItem = item })
                }
            }
        }
    }

    // ── New playlist dialog ────────────────────────────────────────────────────
    if (showNewPlaylistDialog) {
        var name by remember { mutableStateOf("") }
        AlertDialog(onDismissRequest = { showNewPlaylistDialog = false },
            title = { Text("New Playlist") },
            text = { OutlinedTextField(value = name, onValueChange = { name = it },
                label = { Text("Playlist name") }, singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = {
                    if (name.isNotBlank()) { onCreatePlaylist(name.trim()); showNewPlaylistDialog = false }
                })) },
            confirmButton = { TextButton(onClick = { if (name.isNotBlank()) { onCreatePlaylist(name.trim()); showNewPlaylistDialog = false } }) { Text("Create") } },
            dismissButton = { TextButton(onClick = { showNewPlaylistDialog = false }) { Text("Cancel") } })
    }

    // ── Merge playlists ────────────────────────────────────────────────────────
    if (showMergeDialog) {
        MergePlaylistsDialog(playlists = state.playlists,
            onMerge = { ids, name, keep -> onMergePlaylists(ids, name, keep); showMergeDialog = false },
            onDismiss = { showMergeDialog = false })
    }

    // ── Long-press: add to playlist + remove imported ──────────────────────────
    addToPlaylistItem?.let { item ->
        val isImported = state.extraMediaList.any { it.id == item.id }
        AddToPlaylistDialog(item = item, playlists = nonFavPlaylists, isImported = isImported,
            onAdd = { playlistId -> onAddToPlaylist(playlistId, item); addToPlaylistItem = null },
            onRemoveImported = { onRemoveImported(item.id); addToPlaylistItem = null },
            onDismiss = { addToPlaylistItem = null })
    }
}

// ── Add-to-playlist dialog ─────────────────────────────────────────────────────
@Composable
private fun AddToPlaylistDialog(
    item: MediaItem, playlists: List<Playlist>, isImported: Boolean,
    onAdd: (Long) -> Unit, onRemoveImported: () -> Unit, onDismiss: () -> Unit
) {
    AlertDialog(onDismissRequest = onDismiss,
        title = { Text("Options") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("\"${item.title}\"", style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(0.6f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(4.dp))
                // Add to playlist section
                if (playlists.isEmpty()) {
                    Text("No playlists yet. Create one from the Library header.",
                        style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface.copy(0.6f))
                } else {
                    Text("Add to playlist", style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(0.45f),
                        modifier = Modifier.padding(start = 4.dp, bottom = 2.dp))
                    playlists.forEach { playlist ->
                        Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                            .clickable { onAdd(playlist.id) }.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Box(modifier = Modifier.size(36.dp).clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer.copy(0.3f)),
                                contentAlignment = Alignment.Center) {
                                Icon(Icons.Rounded.PlaylistPlay, null, modifier = Modifier.size(18.dp),
                                    tint = MaterialTheme.colorScheme.primary.copy(0.7f))
                            }
                            Column(Modifier.weight(1f)) {
                                Text(playlist.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                                Text("${playlist.itemIds.size} songs", style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(0.5f))
                            }
                            Icon(Icons.Rounded.Add, null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary.copy(0.6f))
                        }
                    }
                }
                // Remove imported option (only for ZIP-imported files)
                if (isImported) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), thickness = 0.5.dp,
                        color = MaterialTheme.colorScheme.onSurface.copy(0.1f))
                    Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                        .clickable { onRemoveImported() }.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(modifier = Modifier.size(36.dp).clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFFFF3B3B).copy(0.15f)), contentAlignment = Alignment.Center) {
                            Icon(Icons.Rounded.DeleteOutline, null, modifier = Modifier.size(18.dp), tint = Color(0xFFFF3B3B))
                        }
                        Text("Remove imported file", style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFFFF3B3B), fontWeight = FontWeight.Medium)
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } })
}

// ── Merge dialog ───────────────────────────────────────────────────────────────
@Composable
private fun MergePlaylistsDialog(
    playlists: List<Playlist>,
    onMerge: (List<Long>, String, Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    val selected = remember { mutableStateListOf<Long>() }
    var newName by remember { mutableStateOf("") }
    var keepOriginals by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Merge Playlists") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Select 2 or more playlists:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(0.7f))
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    playlists.forEach { playlist ->
                        val checked = selected.contains(playlist.id)
                        Row(modifier = Modifier.fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (checked) MaterialTheme.colorScheme.primaryContainer.copy(0.3f) else Color.Transparent)
                            .clickable { if (checked) selected.remove(playlist.id) else selected.add(playlist.id) }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Checkbox(checked = checked, onCheckedChange = {
                                if (it) selected.add(playlist.id) else selected.remove(playlist.id)
                            })
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    if (playlist.isFavourites) Icon(Icons.Rounded.Favorite, null,
                                        modifier = Modifier.size(14.dp), tint = Color(0xFFFF3B6B))
                                    Text(playlist.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium)
                                }
                                Text("${playlist.itemIds.size} songs",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(0.5f))
                            }
                        }
                    }
                }
                OutlinedTextField(
                    value = newName, onValueChange = { newName = it },
                    label = { Text("New playlist name") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (selected.size >= 2) {
                    val total = playlists.filter { selected.contains(it.id) }
                        .flatMap { it.itemIds }.distinct().size
                    Text("Will contain $total unique songs",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary)
                }
                // Keep originals toggle
                Row(
                    modifier = Modifier.fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (keepOriginals) MaterialTheme.colorScheme.primaryContainer.copy(0.25f) else Color.Transparent)
                        .clickable { keepOriginals = !keepOriginals }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Checkbox(checked = keepOriginals, onCheckedChange = { keepOriginals = it })
                    Column {
                        Text("Keep original playlists",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface)
                        Text(
                            if (keepOriginals) "Originals will stay alongside the merged playlist"
                            else "Originals will be deleted after merging",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(0.55f)
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val finalName = newName.trim().ifBlank {
                        playlists.filter { selected.contains(it.id) }.take(2).joinToString(" + ") { it.name }
                    }
                    if (selected.size >= 2) onMerge(selected.toList(), finalName, keepOriginals)
                },
                enabled = selected.size >= 2
            ) { Text("Merge") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

// ── Rename playlist dialog ─────────────────────────────────────────────────────
@Composable
private fun RenamePlaylistDialog(currentName: String, onRename: (String) -> Unit, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf(currentName) }
    AlertDialog(onDismissRequest = onDismiss,
        title = { Text("Rename Playlist") },
        text = {
            OutlinedTextField(value = name, onValueChange = { name = it },
                label = { Text("Playlist name") }, singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = {
                    if (name.isNotBlank()) onRename(name.trim())
                }))
        },
        confirmButton = {
            TextButton(onClick = { if (name.isNotBlank()) onRename(name.trim()) },
                enabled = name.isNotBlank() && name.trim() != currentName) { Text("Rename") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } })
}

// ── Playlists view ─────────────────────────────────────────────────────────────
@Composable
private fun PlaylistsView(playlists: List<Playlist>, mediaList: List<MediaItem>,
    onPlayPlaylist: (Playlist) -> Unit, onDeletePlaylist: (Long) -> Unit,
    onRenamePlaylist: (Long, String) -> Unit, modifier: Modifier = Modifier) {
    LazyColumn(modifier = modifier, contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 160.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(playlists, key = { it.id }) { playlist ->
            PlaylistRow(playlist = playlist, mediaList = mediaList, onPlay = { onPlayPlaylist(playlist) },
                onDelete = if (!playlist.isFavourites) {{ onDeletePlaylist(playlist.id) }} else null,
                onRename = if (!playlist.isFavourites) { newName -> onRenamePlaylist(playlist.id, newName) } else null)
        }
    }
}

@Composable
private fun PlaylistRow(playlist: Playlist, mediaList: List<MediaItem>, onPlay: () -> Unit, onDelete: (() -> Unit)?, onRename: ((String) -> Unit)?) {
    var showRenameDialog by remember { mutableStateOf(false) }
    val artItems = if (playlist.isFavourites) emptyList()
                   else playlist.itemIds.take(4).mapNotNull { id -> mediaList.firstOrNull { it.id == id } }
    LiquidGlassSurface(cornerRadius = 16.dp, alpha = if (playlist.isFavourites) 0.25f else 0.12f,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onPlay)) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Box(modifier = Modifier.size(56.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.primaryContainer.copy(0.35f))) {
                // Always show playlist icon — never pull in song cover art for a playlist
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Icon(
                        if (playlist.isFavourites) Icons.Rounded.Favorite else Icons.Rounded.PlaylistPlay,
                        null,
                        modifier = Modifier.size(28.dp),
                        tint = if (playlist.isFavourites) Color(0xFFFF3B6B) else MaterialTheme.colorScheme.primary.copy(0.8f)
                    )
                }
            }
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    if (playlist.isFavourites) Icon(Icons.Rounded.Favorite, null, modifier = Modifier.size(13.dp), tint = Color(0xFFFF3B6B))
                    Text(playlist.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                }
                Text("${playlist.itemIds.size} song${if (playlist.itemIds.size != 1) "s" else ""}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(0.55f))
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (onRename != null) {
                    IconButton(onClick = { showRenameDialog = true }, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Rounded.Edit, "Rename", modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurface.copy(0.4f))
                    }
                }
                if (onDelete != null) {
                    IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Rounded.DeleteOutline, "Delete", modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurface.copy(0.4f))
                    }
                }
                Icon(Icons.Rounded.PlayArrow, null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary.copy(0.6f))
            }
        }
    }

    if (showRenameDialog && onRename != null) {
        RenamePlaylistDialog(
            currentName = playlist.name,
            onRename = { newName -> onRename(newName); showRenameDialog = false },
            onDismiss = { showRenameDialog = false }
        )
    }
}

// ── Filter chip ────────────────────────────────────────────────────────────────
@Composable
private fun FilterChip(tab: FilterTab, selected: Boolean, onClick: () -> Unit) {
    val label = when (tab) { FilterTab.ALL -> "All"; FilterTab.AUDIO -> "Audio"; FilterTab.VIDEO -> "Video"; FilterTab.PLAYLISTS -> "Playlists" }
    val icon  = when (tab) { FilterTab.ALL -> Icons.Rounded.GridView; FilterTab.AUDIO -> Icons.Rounded.MusicNote; FilterTab.VIDEO -> Icons.Rounded.Videocam; FilterTab.PLAYLISTS -> Icons.Rounded.PlaylistPlay }
    LiquidGlassSurface(cornerRadius = 999.dp, alpha = if (selected) 0.3f else 0.1f, modifier = Modifier.clickable(onClick = onClick)) {
        Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            Icon(icon, null, modifier = Modifier.size(15.dp), tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(0.6f))
            Text(label, fontSize = 12.sp, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(0.7f))
        }
    }
}

// ── Media row ──────────────────────────────────────────────────────────────────
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MediaRow(
    item: MediaItem, isPlaying: Boolean, isFavourite: Boolean, isImported: Boolean,
    onClick: () -> Unit, onFavourite: () -> Unit, onLongPress: () -> Unit
) {
    LiquidGlassSurface(cornerRadius = 16.dp, alpha = if (isPlaying) 0.25f else 0.1f,
        modifier = Modifier.fillMaxWidth().combinedClickable(onClick = onClick, onLongClick = onLongPress)) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(modifier = Modifier.size(52.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.primaryContainer.copy(0.4f)),
                contentAlignment = Alignment.Center) {
                val art = item.artUri
                if (art != null) AsyncImage(model = art, contentDescription = null, contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(12.dp)))
                else Icon(if (item.isVideo) Icons.Rounded.Videocam else Icons.Rounded.MusicNote, null,
                    tint = MaterialTheme.colorScheme.primary.copy(0.7f), modifier = Modifier.size(26.dp))
                if (isPlaying) Box(Modifier.fillMaxSize().background(Color.Black.copy(0.4f), RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                    Icon(Icons.AutoMirrored.Rounded.VolumeUp, null, tint = Color.White, modifier = Modifier.size(20.dp))
                }
            }
            Column(Modifier.weight(1f)) {
                Text(item.title, style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (isPlaying) FontWeight.SemiBold else FontWeight.Normal,
                    maxLines = 2,  // 2 lines instead of 1
                    overflow = TextOverflow.Ellipsis,
                    color = if (isPlaying) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.height(2.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (item.artist.isNotBlank() && item.artist != "Unknown") {
                        Text(item.artist, style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(0.55f), maxLines = 1, overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false))
                        Text("·", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(0.3f))
                    }
                    // Duration + unit label
                    val dur = MediaRepository.formatDuration(item.duration)
                    val unit = MediaRepository.durationUnit(item.duration)
                    Text("$dur $unit", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(0.45f))
                }
            }
            LibraryHeartButton(isFavourite = isFavourite, onToggle = onFavourite)
        }
    }
}

// ── Library heart button with red splash animation ───────────────────────────
@Composable
private fun LibraryHeartButton(isFavourite: Boolean, onToggle: () -> Unit) {
    val scope = rememberCoroutineScope()
    val heartScale = remember { Animatable(1f) }
    val splashScale = remember { Animatable(0f) }
    val splashAlpha = remember { Animatable(0f) }
    val tint by animateColorAsState(
        targetValue = if (isFavourite) Color(0xFFFF3B6B)
                      else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f),
        animationSpec = tween(260), label = "heartColor"
    )
    Box(modifier = Modifier.size(36.dp), contentAlignment = Alignment.Center) {
        // Red splash ripple
        Box(
            modifier = Modifier
                .size(36.dp)
                .graphicsLayer {
                    scaleX = splashScale.value; scaleY = splashScale.value; alpha = splashAlpha.value
                }
                .background(Color(0xFFFF3B6B).copy(alpha = 0.35f), CircleShape)
        )
        IconButton(
            onClick = {
                onToggle()
                scope.launch {
                    heartScale.animateTo(1.45f, tween(110, easing = FastOutSlowInEasing))
                    heartScale.animateTo(1f, spring(stiffness = Spring.StiffnessHigh))
                }
                if (!isFavourite) {
                    scope.launch {
                        splashScale.snapTo(0f); splashAlpha.snapTo(0.9f)
                        splashScale.animateTo(1.8f, tween(350, easing = FastOutSlowInEasing))
                        splashAlpha.animateTo(0f, tween(300))
                        splashScale.snapTo(0f)
                    }
                }
            },
            modifier = Modifier.fillMaxSize()
        ) {
            Icon(
                if (isFavourite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                "Favorite",
                modifier = Modifier.size(18.dp).scale(heartScale.value),
                tint = tint
            )
        }
    }
}

// ── Multi-delete dialog ───────────────────────────────────────────────────────
@Composable
private fun MultiDeleteDialog(
    count: Int,
    entityName: String,
    onDeleteWithFiles: () -> Unit,
    onDeleteWithoutFiles: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete $count ${entityName}${if (count != 1) "s" else ""}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Would you like to also delete the actual files from storage?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(0.7f))
                Spacer(Modifier.height(4.dp))
                Button(onClick = onDeleteWithFiles, modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) {
                    Icon(Icons.Rounded.DeleteForever, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Delete + remove files")
                }
                OutlinedButton(onClick = onDeleteWithoutFiles, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Rounded.Delete, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Delete (keep files)")
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
