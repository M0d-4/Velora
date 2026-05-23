package com.velora.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
    onMergePlaylists: (List<Long>, String) -> Unit,
    onDeletePlaylist: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var showNewPlaylistDialog by remember { mutableStateOf(false) }
    var showMergeDialog by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxSize()) {
        // Header
        Box(
            modifier = Modifier.fillMaxWidth()
                .background(Brush.verticalGradient(listOf(
                    MaterialTheme.colorScheme.background,
                    MaterialTheme.colorScheme.background.copy(alpha = 0f)
                )))
                .padding(top = 56.dp, bottom = 8.dp, start = 24.dp, end = 24.dp)
        ) {
            Column {
                Row(modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Library", style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (state.filterTab == FilterTab.PLAYLISTS) {
                            // Merge button only visible in playlists tab
                            LiquidGlassSurface(cornerRadius = 999.dp, alpha = 0.15f) {
                                IconButton(onClick = { showMergeDialog = true }) {
                                    Icon(Icons.Rounded.MergeType, "Merge Playlists",
                                        tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(20.dp))
                                }
                            }
                        }
                        LiquidGlassSurface(cornerRadius = 999.dp, alpha = 0.15f) {
                            IconButton(onClick = { showNewPlaylistDialog = true }) {
                                Icon(Icons.Rounded.PlaylistAdd, "New Playlist",
                                    tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
                            }
                        }
                        LiquidGlassSurface(cornerRadius = 999.dp, alpha = 0.15f) {
                            IconButton(onClick = onImportZip) {
                                Icon(Icons.Rounded.FolderZip, "Import ZIP",
                                    tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
                            }
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterTab.values().forEach { tab ->
                        FilterChip(tab, state.filterTab == tab) { onFilterChange(tab) }
                    }
                }
            }
        }

        when {
            state.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
            state.filterTab == FilterTab.PLAYLISTS -> PlaylistsView(
                playlists = state.playlists, mediaList = state.mediaList,
                onPlayPlaylist = onPlayPlaylist, onDeletePlaylist = onDeletePlaylist,
                modifier = Modifier.fillMaxSize()
            )
            filteredItems.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Rounded.LibraryMusic, null, modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f))
                    Spacer(Modifier.height(12.dp))
                    Text("No media found", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                }
            }
            else -> LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 200.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(filteredItems, key = { it.id }) { item ->
                    MediaRow(
                        item = item,
                        isPlaying = state.currentItem?.id == item.id && state.isPlaying,
                        isFavourite = isFavourite(item),
                        onClick = { onItemClick(item) },
                        onFavourite = { onAddToFavourites(item) }
                    )
                }
            }
        }
    }

    // ── New playlist dialog ───────────────────────────────────────────────────
    if (showNewPlaylistDialog) {
        var name by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showNewPlaylistDialog = false },
            title = { Text("New Playlist") },
            text = {
                OutlinedTextField(value = name, onValueChange = { name = it },
                    label = { Text("Playlist name") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = {
                        if (name.isNotBlank()) { onCreatePlaylist(name.trim()); showNewPlaylistDialog = false }
                    }))
            },
            confirmButton = {
                TextButton(onClick = {
                    if (name.isNotBlank()) { onCreatePlaylist(name.trim()); showNewPlaylistDialog = false }
                }) { Text("Create") }
            },
            dismissButton = { TextButton(onClick = { showNewPlaylistDialog = false }) { Text("Cancel") } }
        )
    }

    // ── Merge playlists dialog ────────────────────────────────────────────────
    if (showMergeDialog) {
        MergePlaylistsDialog(
            playlists = state.playlists,
            onMerge = { ids, name -> onMergePlaylists(ids, name); showMergeDialog = false },
            onDismiss = { showMergeDialog = false }
        )
    }
}

// ── Merge playlists dialog ────────────────────────────────────────────────────
@Composable
private fun MergePlaylistsDialog(
    playlists: List<Playlist>,
    onMerge: (List<Long>, String) -> Unit,
    onDismiss: () -> Unit
) {
    val selected = remember { mutableStateListOf<Long>() }
    var newName by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Merge Playlists") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Select 2 or more playlists to merge:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))

                // Scrollable list of checkboxes
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    playlists.forEach { playlist ->
                        val checked = selected.contains(playlist.id)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (checked) MaterialTheme.colorScheme.primaryContainer.copy(0.3f)
                                    else Color.Transparent
                                )
                                .clickable {
                                    if (checked) selected.remove(playlist.id)
                                    else selected.add(playlist.id)
                                }
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Checkbox(
                                checked = checked,
                                onCheckedChange = {
                                    if (it) selected.add(playlist.id) else selected.remove(playlist.id)
                                }
                            )
                            Column {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    if (playlist.isFavourites) {
                                        Icon(Icons.Rounded.Favorite, null,
                                            modifier = Modifier.size(14.dp), tint = Color(0xFFFF3B6B))
                                    }
                                    Text(playlist.name, style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium)
                                }
                                Text("${playlist.itemIds.size} songs",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(0.5f))
                            }
                        }
                    }
                }

                Spacer(Modifier.height(4.dp))
                OutlinedTextField(
                    value = newName, onValueChange = { newName = it },
                    label = { Text("New playlist name") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = {
                        // Auto-suggest name from selected playlists
                        val suggestion = if (selected.size >= 2) {
                            val names = playlists.filter { selected.contains(it.id) }.map { it.name }
                            names.take(2).joinToString(" + ")
                        } else "Name for merged playlist"
                        Text(suggestion, color = MaterialTheme.colorScheme.onSurface.copy(0.35f))
                    }
                )

                if (selected.size >= 2) {
                    val totalSongs = playlists
                        .filter { selected.contains(it.id) }
                        .flatMap { it.itemIds }
                        .distinct().size
                    Text("Will contain $totalSongs unique songs",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary)
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val finalName = newName.trim().ifBlank {
                        playlists.filter { selected.contains(it.id) }
                            .take(2).joinToString(" + ") { it.name }
                    }
                    if (selected.size >= 2) onMerge(selected.toList(), finalName)
                },
                enabled = selected.size >= 2
            ) { Text("Merge") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

// ── Playlist list view ────────────────────────────────────────────────────────
@Composable
private fun PlaylistsView(
    playlists: List<Playlist>,
    mediaList: List<MediaItem>,
    onPlayPlaylist: (Playlist) -> Unit,
    onDeletePlaylist: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(modifier = modifier,
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 200.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(playlists, key = { it.id }) { playlist ->
            PlaylistRow(
                playlist = playlist, mediaList = mediaList,
                onPlay = { onPlayPlaylist(playlist) },
                onDelete = if (!playlist.isFavourites) {{ onDeletePlaylist(playlist.id) }} else null
            )
        }
    }
}

@Composable
private fun PlaylistRow(
    playlist: Playlist,
    mediaList: List<MediaItem>,
    onPlay: () -> Unit,
    onDelete: (() -> Unit)?
) {
    // Grab first 4 items' art for a mini grid preview
    val artItems = playlist.itemIds.take(4)
        .mapNotNull { id -> mediaList.firstOrNull { it.id == id } }

    LiquidGlassSurface(
        cornerRadius = 16.dp,
        alpha = if (playlist.isFavourites) 0.25f else 0.12f,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onPlay)
    ) {
        Row(modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)) {

            // Cover art grid or icon
            Box(modifier = Modifier.size(56.dp).clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.primaryContainer.copy(0.35f))) {
                if (artItems.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Icon(if (playlist.isFavourites) Icons.Rounded.Favorite else Icons.Rounded.PlaylistPlay,
                            null, modifier = Modifier.size(28.dp),
                            tint = if (playlist.isFavourites) Color(0xFFFF3B6B)
                                   else MaterialTheme.colorScheme.primary.copy(0.8f))
                    }
                } else if (artItems.size < 4) {
                    // Single art
                    val art = artItems.firstOrNull()?.artUri
                    if (art != null) {
                        AsyncImage(model = art, contentDescription = null,
                            contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                    } else {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Icon(Icons.Rounded.PlaylistPlay, null, modifier = Modifier.size(28.dp),
                                tint = MaterialTheme.colorScheme.primary.copy(0.8f))
                        }
                    }
                } else {
                    // 2×2 art grid
                    Column(modifier = Modifier.fillMaxSize()) {
                        Row(modifier = Modifier.weight(1f)) {
                            artItems.take(2).forEach { it2 ->
                                val a = it2.artUri
                                Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                                    if (a != null) AsyncImage(model = a, contentDescription = null,
                                        contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                                    else Box(Modifier.fillMaxSize().background(
                                        MaterialTheme.colorScheme.primaryContainer.copy(0.3f)))
                                }
                            }
                        }
                        Row(modifier = Modifier.weight(1f)) {
                            artItems.drop(2).forEach { it2 ->
                                val a = it2.artUri
                                Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                                    if (a != null) AsyncImage(model = a, contentDescription = null,
                                        contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                                    else Box(Modifier.fillMaxSize().background(
                                        MaterialTheme.colorScheme.primaryContainer.copy(0.3f)))
                                }
                            }
                        }
                    }
                }
            }

            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    if (playlist.isFavourites) {
                        Icon(Icons.Rounded.Favorite, null, modifier = Modifier.size(13.dp),
                            tint = Color(0xFFFF3B6B))
                    }
                    Text(playlist.name, style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                }
                Text("${playlist.itemIds.size} song${if (playlist.itemIds.size != 1) "s" else ""}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(0.55f))
            }

            Row(horizontalArrangement = Arrangement.spacedBy(0.dp)) {
                if (onDelete != null) {
                    IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Rounded.DeleteOutline, "Delete", modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.onSurface.copy(0.4f))
                    }
                }
                Icon(Icons.Rounded.PlayArrow, null, modifier = Modifier.size(20.dp).align(Alignment.CenterVertically),
                    tint = MaterialTheme.colorScheme.primary.copy(0.6f))
            }
        }
    }
}

// ── Filter chip ───────────────────────────────────────────────────────────────
@Composable
private fun FilterChip(tab: FilterTab, selected: Boolean, onClick: () -> Unit) {
    val label = when (tab) {
        FilterTab.ALL -> "All"; FilterTab.AUDIO -> "Audio"
        FilterTab.VIDEO -> "Video"; FilterTab.PLAYLISTS -> "Playlists"
    }
    val icon = when (tab) {
        FilterTab.ALL -> Icons.Rounded.GridView; FilterTab.AUDIO -> Icons.Rounded.MusicNote
        FilterTab.VIDEO -> Icons.Rounded.Videocam; FilterTab.PLAYLISTS -> Icons.Rounded.PlaylistPlay
    }
    LiquidGlassSurface(cornerRadius = 999.dp, alpha = if (selected) 0.3f else 0.1f,
        modifier = Modifier.clickable(onClick = onClick)) {
        Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            Icon(icon, null, modifier = Modifier.size(15.dp),
                tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(0.6f))
            Text(label, fontSize = 12.sp, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(0.7f))
        }
    }
}

// ── Media row ─────────────────────────────────────────────────────────────────
@Composable
private fun MediaRow(
    item: MediaItem,
    isPlaying: Boolean,
    isFavourite: Boolean,
    onClick: () -> Unit,
    onFavourite: () -> Unit
) {
    LiquidGlassSurface(cornerRadius = 16.dp, alpha = if (isPlaying) 0.25f else 0.1f,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {

            // Thumbnail with cover art (audio + video)
            Box(modifier = Modifier.size(52.dp).clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.primaryContainer.copy(0.4f)),
                contentAlignment = Alignment.Center) {
                val art = item.artUri
                if (art != null) {
                    AsyncImage(model = art, contentDescription = null, contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(12.dp)),
                        onError = {})   // fallback to icon handled below via overlay
                }
                // Icon overlay when no art or as fallback
                if (art == null) {
                    Icon(if (item.isVideo) Icons.Rounded.Videocam else Icons.Rounded.MusicNote,
                        null, tint = MaterialTheme.colorScheme.primary.copy(0.7f),
                        modifier = Modifier.size(26.dp))
                }
                // Playing indicator overlay
                if (isPlaying) {
                    Box(Modifier.fillMaxSize().background(Color.Black.copy(0.4f), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center) {
                        Icon(Icons.AutoMirrored.Rounded.VolumeUp, null, tint = Color.White,
                            modifier = Modifier.size(20.dp))
                    }
                }
            }

            Column(Modifier.weight(1f)) {
                Text(item.title, style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (isPlaying) FontWeight.SemiBold else FontWeight.Normal,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                    color = if (isPlaying) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.height(2.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    if (item.artist.isNotBlank() && item.artist != "Unknown") {
                        Text(item.artist, style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(0.55f),
                            maxLines = 1, overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false))
                        Text("·", style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(0.3f))
                    }
                    Text(MediaRepository.formatDuration(item.duration),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(0.45f))
                }
            }

            IconButton(onClick = onFavourite, modifier = Modifier.size(36.dp)) {
                Icon(if (isFavourite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                    "Favourite", modifier = Modifier.size(18.dp),
                    tint = if (isFavourite) Color(0xFFFF3B6B) else MaterialTheme.colorScheme.onSurface.copy(0.35f))
            }
        }
    }
}
