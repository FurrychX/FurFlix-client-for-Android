package com.furflix.app.ui.screens

import androidx.compose.ui.res.stringResource
import com.furflix.app.R
import android.content.Context
import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.furflix.app.data.local.DownloadHelper
import com.furflix.app.data.local.DownloadedItem
import com.furflix.app.data.repository.FurRepository
import com.furflix.app.ui.components.HazeTopAppBar
import com.furflix.app.ui.components.ImageLoaderFactory
import com.furflix.app.ui.theme.*
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import kotlinx.coroutines.launch

data class DownloadMeta(val author: String, val title: String, val postId: String)

private fun parseMeta(context: Context, filename: String): DownloadMeta? {
    val idStr = filename.substringAfterLast("_", "")
    val id = idStr.takeWhile { it.isDigit() }
    if (id.isEmpty()) return null

    val prefs = context.getSharedPreferences("downloads_meta", Context.MODE_PRIVATE)
    val jsonStr = prefs.getString(id, null)

    if (jsonStr == null) {
        val parts = filename.substringBeforeLast(".").split("_")
        if (parts.size >= 4 && parts[0] == "FA") {
            return DownloadMeta(
                author = parts[1],
                title = parts.subList(2, parts.size - 1).joinToString("_"),
                postId = id
            )
        }
        return DownloadMeta(
            author = "Unknown",
            title = filename.substringBeforeLast("_", filename.substringBeforeLast(".")),
            postId = id
        )
    }

    return try {
        val json = org.json.JSONObject(jsonStr)
        DownloadMeta(
            author = json.optString("author", "Unknown"),
            title = json.optString("title", "Unknown"),
            postId = json.optString("postId", id)
        )
    } catch (_: Exception) { null }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadsScreen(
    onBack: () -> Unit,
    onNavigateToUser: (String) -> Unit = {},
    onNavigateToPost: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val repository = remember { FurRepository.getInstance(context) }
    val hazeState = remember { HazeState() }

    var images by remember { mutableStateOf<List<DownloadedItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var isGrid by remember { mutableStateOf(true) }

    val gridState = rememberLazyGridState()

    LaunchedEffect(Unit) {
        isGrid = repository.loadIsGrid()
    }
    LaunchedEffect(Unit) {
        images = DownloadHelper.getDownloadedImages(context)
        isLoading = false
    }

    Scaffold(
        topBar = {
            HazeTopAppBar(
                title = {
                    Text(
                        stringResource(R.string.nav_tab_downloads),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = FA_Accent
                    )
                },
                hazeState = hazeState,
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = MaterialTheme.colorScheme.onSurface)
                    }
                },
                actions = {
                    IconButton(onClick = {
                        isGrid = !isGrid
                        coroutineScope.launch { repository.saveIsGrid(isGrid) }
                    }, modifier = Modifier.size(36.dp)) {
                        Icon(
                            imageVector = if (isGrid) Icons.Default.GridView else Icons.AutoMirrored.Filled.ViewList,
                            contentDescription = "Toggle layout",
                            tint = FA_Accent,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            )
        },
        containerColor = DarkBackground
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .hazeSource(state = hazeState)
        ) {
            if (isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = FA_Accent)
                }
            } else if (images.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Default.Download, null, modifier = Modifier.size(72.dp), tint = DividerDark)
                    Spacer(Modifier.height(20.dp))
                    Text(stringResource(R.string.downloads_empty_title), style = MaterialTheme.typography.titleMedium, color = SubtleGray)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(stringResource(R.string.downloads_empty_desc), style = MaterialTheme.typography.bodyMedium, color = DividerDark)
                }
            } else {
                Crossfade(targetState = isGrid, animationSpec = tween(300), label = "gridList") { grid ->
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(if (grid) 2 else 1),
                        modifier = Modifier.fillMaxSize(),
                        state = gridState,
                        contentPadding = PaddingValues(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(if (grid) 8.dp else 16.dp),
                        verticalArrangement = Arrangement.spacedBy(if (grid) 8.dp else 16.dp)
                    ) {
                        items(images) { item ->
                            DownloadCard(
                                item = item,
                                onOpen = {
                                    val intent = Intent(Intent.ACTION_VIEW).apply {
                                        setDataAndType(item.uri, "image/*")
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(intent)
                                },
                                onViewPost = {
                                    parseMeta(context, item.filename)?.let { onNavigateToPost(it.postId) }
                                },
                                onViewAuthor = {
                                    parseMeta(context, item.filename)?.let {
                                        if (it.author != "Unknown") onNavigateToUser(it.author)
                                    }
                                },
                                onShare = {
                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "image/*"
                                        putExtra(Intent.EXTRA_STREAM, item.uri)
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(Intent.createChooser(shareIntent, "Share"))
                                },
                                onDelete = {
                                    coroutineScope.launch {
                                        DownloadHelper.deleteImage(context, item.uri)
                                        images = images.filter { it.uri != item.uri }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DownloadCard(
    item: DownloadedItem,
    onOpen: () -> Unit,
    onViewPost: () -> Unit,
    onViewAuthor: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    val meta = remember(item.filename) { parseMeta(context, item.filename) }
    var deleteConfirm by remember { mutableStateOf(false) }

    val displayAuthor = if (meta != null && meta.author != "Unknown" && meta.author.isNotBlank()) "@${meta.author}" else null

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column {
            // Image
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp))
                    .clickable(onClick = onOpen)
            ) {
                SubcomposeAsyncImage(
                    model = ImageRequest.Builder(context).data(item.uri).crossfade(300).build(),
                    imageLoader = ImageLoaderFactory.getInstance(context),
                    contentDescription = "Downloaded image",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Actions row (No text labels)
            Column(modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ActionPill(
                        icon = Icons.Default.Description,
                        onClick = onViewPost,
                        modifier = Modifier
                    )
                    if (displayAuthor != null) {
                        ActionPill(
                        icon = Icons.Default.Person,
                        onClick = onViewAuthor,
                        modifier = Modifier
                    )
                    }
                    ActionPill(
                        icon = Icons.Default.Share,
                        onClick = onShare,
                        modifier = Modifier
                    )
                    ActionPill(
                        icon = Icons.Default.Delete,
                        onClick = { deleteConfirm = true },
                        modifier = Modifier
                    )
                }

                // Delete confirmation inside card
                AnimatedVisibility(
                    visible = deleteConfirm,
                    enter = fadeIn(tween(200)) + slideInVertically(tween(200)) { it },
                    exit = fadeOut(tween(200)) + slideOutVertically(tween(200)) { it }
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(DarkSurface)
                            .padding(12.dp)
                    ) {
                        Text("Удалить это изображение?", style = MaterialTheme.typography.bodySmall, color = Color.White)
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            ActionPill(
                        icon = Icons.Default.Close,
                        onClick = { deleteConfirm = false },
                        modifier = Modifier
                    )
                            ActionPill(
                        icon = Icons.Default.Delete,
                        onClick = {
                                    onDelete()
                                    deleteConfirm = false
                                },
                        modifier = Modifier
                    )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ActionPill(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    IconButton(
        onClick = onClick,
        modifier = modifier
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.7f),
            modifier = Modifier.size(24.dp)
        )
    }
}
