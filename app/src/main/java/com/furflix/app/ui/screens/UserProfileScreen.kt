package com.furflix.app.ui.screens

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.res.stringResource
import com.furflix.app.R

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.furflix.app.data.model.ContactLink
import com.furflix.app.data.model.UserProfile
import com.furflix.app.data.repository.FurRepository
import com.furflix.app.ui.components.*
import com.furflix.app.ui.theme.*
import com.furflix.app.viewmodel.UserViewModel
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun UserProfileScreen(
    username: String,
    viewModel: UserViewModel,
    onBack: () -> Unit,
    onNavigateToPost: (String) -> Unit,
    onNavigateToUser: (String) -> Unit
) {
    val context = LocalContext.current

    val submissions by viewModel.submissions.collectAsState()
    val userProfile by viewModel.userProfile.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isLoadingMore by viewModel.isLoadingMore.collectAsState()
    val isTogglingWatch by viewModel.isTogglingWatch.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val watchingList by viewModel.watchingList.collectAsState()
    val watchersList by viewModel.watchersList.collectAsState()

    val selectedTab by viewModel.currentTab.collectAsState()
    var isGrid by remember { mutableStateOf(true) }
    val repository = remember { FurRepository.getInstance(context) }
    LaunchedEffect(Unit) { isGrid = repository.loadIsGrid() }
    val hazeState = remember { HazeState() }

    val tabGallery = stringResource(R.string.user_gallery)
    val tabFavorites = stringResource(R.string.profile_favorites)
    val tabWatching = stringResource(R.string.user_watching)
    val tabWatchers = stringResource(R.string.user_watchers)

    val bottomBarItems = remember(tabGallery, tabFavorites, tabWatching, tabWatchers) {
        listOf(
            GlassBarTab(tabGallery, Icons.Default.PhotoLibrary, Color(0xFF00E5FF)),
            GlassBarTab(tabFavorites, Icons.Default.Favorite, Color(0xFFFF007F)),
            GlassBarTab(tabWatching, Icons.Default.Group, Color(0xFF00FF7F)),
            GlassBarTab(tabWatchers, Icons.Default.Group, Color(0xFFFFD700))
        )
    }

    val listState = rememberLazyListState()
    var scrollTarget by remember { mutableIntStateOf(0) }
    var scrollKey by remember { mutableIntStateOf(0) }

    fun contentFirstIndex(p: UserProfile?): Int {
        if (p == null) return 2
        var idx = 2
        if (p.galleryCount > 0 || p.favoritesCount > 0 ||
            p.watchersCount > 0 || p.watchingCount > 0) idx++
        if (p.profileText.isNotBlank()) idx++
        if (p.species.isNotBlank() || p.acceptingTrades.isNotBlank() ||
            p.acceptingCommissions.isNotBlank() || p.views > 0 || p.journalsCount > 0) idx++
        if (p.contactLinks.isNotEmpty()) idx++
        return idx
    }

    fun selectTab(index: Int, fromBottom: Boolean) {
        val isSameTab = selectedTab == index
        if (!isSameTab) {
            when (index) {
                UserViewModel.TAB_GALLERY -> viewModel.loadGallery()
                UserViewModel.TAB_FAVORITES -> viewModel.loadFavorites()
                UserViewModel.TAB_WATCHING -> viewModel.loadWatching()
                UserViewModel.TAB_WATCHERS -> viewModel.loadWatchers()
            }
        }
        scrollTarget = when {
            isSameTab && fromBottom -> contentFirstIndex(userProfile)
            fromBottom -> 0
            else -> contentFirstIndex(userProfile)
        }
        scrollKey++
    }

    // Scroll to content section when target changes
    LaunchedEffect(scrollKey) {
        if (scrollKey == 0) return@LaunchedEffect
        kotlinx.coroutines.delay(100)
        val total = listState.layoutInfo.totalItemsCount
        if (total > 0) {
            listState.animateScrollToItem(scrollTarget.coerceIn(0, total - 1))
        }
    }

    // Show the artist's name in the top bar once the hero header is scrolled away
    val showBarTitle by remember { derivedStateOf { listState.firstVisibleItemIndex >= 2 } }

    // Infinite scroll
    val hasContent by rememberUpdatedState(
        when (selectedTab) {
            UserViewModel.TAB_GALLERY, UserViewModel.TAB_FAVORITES -> submissions.isNotEmpty()
            UserViewModel.TAB_WATCHING -> watchingList.isNotEmpty()
            else -> watchersList.isNotEmpty()
        }
    )
    LaunchedEffect(listState) {
        snapshotFlow {
            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            lastVisible >= listState.layoutInfo.totalItemsCount - 5
        }.collect { shouldLoadMore ->
            if (shouldLoadMore && hasContent) viewModel.loadMore()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding()
            .pointerInput(selectedTab) {
                var accumulatedDrag = 0f
                detectHorizontalDragGestures(
                    onDragEnd = {
                        if (accumulatedDrag > 60f) {
                            selectTab((selectedTab - 1).coerceAtLeast(0), fromBottom = false)
                        } else if (accumulatedDrag < -60f) {
                            selectTab((selectedTab + 1).coerceAtMost(3), fromBottom = false)
                        }
                        accumulatedDrag = 0f
                    },
                    onDragCancel = { accumulatedDrag = 0f }
                ) { _, dragAmount ->
                    accumulatedDrag += dragAmount
                }
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .hazeSource(state = hazeState)
        ) {
            HazeTopAppBar(
                title = {
                    AnimatedVisibility(
                        visible = showBarTitle && userProfile != null,
                        enter = fadeIn(tween(200)),
                        exit = fadeOut(tween(200))
                    ) {
                        Text(
                            text = userProfile?.displayName ?: "",
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                hazeState = hazeState,
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        openUrl(context, "https://www.furaffinity.net/user/$username/")
                    }) {
                        Icon(Icons.Default.OpenInBrowser, contentDescription = "Open in browser")
                    }
                }
            )

            Crossfade(
                targetState = isGrid,
                animationSpec = tween(300),
                label = "gridListCrossfade"
            ) { grid ->
                LazyColumn(
                    state = listState,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(bottom = 120.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item(key = "header") {
                        val profile = userProfile
                        if (profile != null) ProfileHeader(profile) else HeaderSkeleton()
                    }
                    userProfile?.let { profile ->
                        item(key = "actions") {
                            ProfileActions(
                                profile = profile,
                                isToggling = isTogglingWatch,
                                onToggleWatch = { viewModel.toggleWatch() },
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }
                        if (profile.galleryCount > 0 || profile.favoritesCount > 0 ||
                            profile.watchersCount > 0 || profile.watchingCount > 0
                        ) {
                            item(key = "stats") {
                                StatsCard(
                                    profile = profile,
                                    selectedTab = selectedTab,
                                    onSelect = { selectTab(it, fromBottom = false) },
                                    modifier = Modifier.padding(horizontal = 16.dp)
                                )
                            }
                        }
                        if (profile.profileText.isNotBlank()) {
                            item(key = "bio") {
                                BioCard(text = profile.profileText, modifier = Modifier.padding(horizontal = 16.dp))
                            }
                        }
                        val hasChips = profile.species.isNotBlank() ||
                                profile.acceptingTrades.isNotBlank() ||
                                profile.acceptingCommissions.isNotBlank() ||
                                profile.views > 0 || profile.journalsCount > 0
                        if (hasChips) {
                            item(key = "chips") {
                                InfoChips(profile = profile, modifier = Modifier.padding(horizontal = 16.dp))
                            }
                        }
                        if (profile.contactLinks.isNotEmpty()) {
                            item(key = "contacts") {
                                ContactLinksRow(links = profile.contactLinks, onOpen = { openUrl(context, it) })
                            }
                        }
                    }

                    when (selectedTab) {
                        UserViewModel.TAB_GALLERY, UserViewModel.TAB_FAVORITES -> {
                            if (errorMessage != null && submissions.isEmpty()) {
                                item(key = "error") { ErrorState(message = errorMessage ?: "") }
                            }
                            if (submissions.isEmpty() && !isLoading && errorMessage == null) {
                                item(key = "empty") { EmptyState(message = "No submissions.", icon = Icons.Default.PhotoLibrary) }
                            }
                            if (submissions.isNotEmpty()) {
                                if (grid) {
                                    items(submissions.chunked(2), key = { chunk -> chunk.joinToString { it.id } }) { row ->
                                        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                            row.forEach { submission ->
                                                Box(modifier = Modifier.weight(1f)) {
                                                    SubmissionCard(submission = submission, onClick = { onNavigateToPost(submission.id) }, compact = true, onAuthorClick = { onNavigateToUser(it) })
                                                }
                                            }
                                            if (row.size < 2) Spacer(modifier = Modifier.weight(1f))
                                        }
                                    }
                                } else {
                                    items(submissions, key = { it.id }) { submission ->
                                        Box(modifier = Modifier.padding(horizontal = 12.dp)) {
                                            SubmissionCard(submission = submission, onClick = { onNavigateToPost(submission.id) }, onAuthorClick = { onNavigateToUser(it) })
                                        }
                                    }
                                }
                            }
                            if (isLoadingMore) {
                                item(key = "footer") { LoadingMoreFooter() }
                            }
                        }
                        UserViewModel.TAB_WATCHING -> {
                            if (errorMessage != null && watchingList.isEmpty()) {
                                item(key = "error") { ErrorState(message = errorMessage ?: "") }
                            }
                            if (watchingList.isEmpty() && !isLoading && errorMessage == null) {
                                item(key = "empty") { EmptyState(message = "Not watching anyone.", icon = Icons.Default.Group) }
                            }
                            if (watchingList.isNotEmpty()) {
                                if (grid) {
                                    items(watchingList.chunked(2), key = { chunk -> chunk.joinToString { it.username } }) { row ->
                                        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                            row.forEach { user -> Box(modifier = Modifier.weight(1f)) { WatchlistGridItem(user = user, onClick = { onNavigateToUser(user.username) }) } }
                                            if (row.size < 2) Spacer(modifier = Modifier.weight(1f))
                                        }
                                    }
                                } else {
                                    items(watchingList, key = { it.username }) { user ->
                                        Box(modifier = Modifier.padding(horizontal = 12.dp)) {
                                            WatchlistItem(user = user, onClick = { onNavigateToUser(user.username) })
                                        }
                                    }
                                }
                            }
                            if (isLoadingMore) {
                                item(key = "footer") { LoadingMoreFooter() }
                            }
                        }
                        UserViewModel.TAB_WATCHERS -> {
                            if (errorMessage != null && watchersList.isEmpty()) {
                                item(key = "error") { ErrorState(message = errorMessage ?: "") }
                            }
                            if (watchersList.isEmpty() && !isLoading && errorMessage == null) {
                                item(key = "empty") { EmptyState(message = "No watchers.", icon = Icons.Default.Group) }
                            }
                            if (watchersList.isNotEmpty()) {
                                if (grid) {
                                    items(watchersList.chunked(2), key = { chunk -> chunk.joinToString { it.username } }) { row ->
                                        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                            row.forEach { user -> Box(modifier = Modifier.weight(1f)) { WatchlistGridItem(user = user, onClick = { onNavigateToUser(user.username) }) } }
                                            if (row.size < 2) Spacer(modifier = Modifier.weight(1f))
                                        }
                                    }
                                } else {
                                    items(watchersList, key = { it.username }) { user ->
                                        Box(modifier = Modifier.padding(horizontal = 12.dp)) {
                                            WatchlistItem(user = user, onClick = { onNavigateToUser(user.username) })
                                        }
                                    }
                                }
                            }
                            if (isLoadingMore) {
                                item(key = "footer") { LoadingMoreFooter() }
                            }
                        }
                    }
                }

                // Infinite scroll
                val hasContent = when (selectedTab) {
                    UserViewModel.TAB_GALLERY, UserViewModel.TAB_FAVORITES -> submissions.isNotEmpty()
                    UserViewModel.TAB_WATCHING -> watchingList.isNotEmpty()
                    else -> watchersList.isNotEmpty()
                }
                LaunchedEffect(listState, hasContent) {
                    snapshotFlow {
                        val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                        val total = listState.layoutInfo.totalItemsCount
                        lastVisible >= total - 5
                    }.collect { shouldLoadMore ->
                        if (shouldLoadMore && hasContent && !isLoadingMore) viewModel.loadMore()
                    }
                }
            }
        } // close Column

            // Glass bottom bar
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 24.dp, vertical = 24.dp)
                .fillMaxWidth()
                .height(64.dp)
        ) {
            GlassmorphicBottomBar(
                tabs = bottomBarItems,
                selectedIndex = selectedTab,
                hazeState = hazeState,
                onTabSelected = { selectTab(it, fromBottom = true) }
            )
        }
    }
}

// -----------------------------------------------------------------------------------------
// Hero header: banner + avatar + name + title chips
// -----------------------------------------------------------------------------------------

@Composable
private fun ProfileHeader(profile: UserProfile) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Box(modifier = Modifier.fillMaxWidth()) {
            // Banner
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
            ) {
                if (profile.bannerUrl.isNotEmpty()) {
                    AsyncImage(
                        model = profile.bannerUrl,
                        contentDescription = "Banner",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        Cyan.copy(alpha = 0.25f),
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
                                        Pink.copy(alpha = 0.20f)
                                    )
                                )
                            )
                    )
                }
                // Fade into the background
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    DarkBackground.copy(alpha = 0.55f),
                                    DarkBackground
                                ),
                                startY = 120f
                            )
                        )
                )
            }

            // Avatar overlapping the banner bottom
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 16.dp)
                    .offset(y = 36.dp)
                    .size(96.dp),
                contentAlignment = Alignment.Center
            ) {
                // Neon accent ring
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .border(
                            width = 2.dp,
                            brush = Brush.linearGradient(
                                colors = listOf(Cyan, Pink)
                            ),
                            shape = CircleShape
                        )
                )
                AsyncImage(
                    model = profile.avatarUrl,
                    contentDescription = "Avatar",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(86.dp)
                        .clip(CircleShape)
                        .border(3.dp, DarkBackground, CircleShape)
                        .background(DarkSurfaceVariant)
                )
            }
        }

        // Name block (below avatar overlap)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 44.dp)
        ) {
            Text(
                text = profile.displayName,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = "@${profile.username}",
                style = MaterialTheme.typography.bodyMedium,
                color = SubtleGray
            )

            if (profile.userTitle.isNotBlank() || profile.registeredText.isNotBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (profile.userTitle.isNotBlank()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                                .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f), RoundedCornerShape(20.dp))
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Icon(
                                Icons.Default.Badge,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = profile.userTitle,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    if (profile.registeredText.isNotBlank()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(ChipBackground)
                                .border(1.dp, ChipBorder, RoundedCornerShape(20.dp))
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Icon(
                                Icons.Default.Schedule,
                                contentDescription = null,
                                tint = SubtleGray,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = "Joined ${profile.registeredText}",
                                fontSize = 12.sp,
                                color = SubtleGray
                            )
                        }
                    }
                }
            }
        }
    }
}

// -----------------------------------------------------------------------------------------
// Action row: big Watch button + PM + Share
// -----------------------------------------------------------------------------------------

@Composable
private fun ProfileActions(
    profile: UserProfile,
    isToggling: Boolean,
    onToggleWatch: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showUnwatchDialog by remember { mutableStateOf(false) }

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (profile.watchUrl.isNotEmpty()) {
            WatchActionButton(
                isWatching = profile.isWatching,
                isToggling = isToggling,
                onClick = {
                    if (profile.isWatching) showUnwatchDialog = true
                    else onToggleWatch()
                },
                modifier = Modifier.weight(1f)
            )
        }
        if (profile.noteUrl.isNotEmpty()) {
            ActionIconButton(
                icon = Icons.Default.Mail,
                contentDescription = "Send note",
                onClick = { openUrl(context, profile.noteUrl) }
            )
        }
        ActionIconButton(
            icon = Icons.Default.Share,
            contentDescription = "Share profile",
            onClick = {
                val sendIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(
                        Intent.EXTRA_TEXT,
                        "https://www.furaffinity.net/user/${profile.username}/"
                    )
                }
                context.startActivity(Intent.createChooser(sendIntent, null))
            }
        )
    }

    if (showUnwatchDialog) {
        AlertDialog(
            onDismissRequest = { showUnwatchDialog = false },
            containerColor = DarkSurface,
            titleContentColor = Color.White,
            textContentColor = SubtleGray,
            title = { Text("Отписаться") },
            text = { Text("Перестать читать ${profile.displayName}?") },
            confirmButton = {
                TextButton(onClick = {
                    showUnwatchDialog = false
                    onToggleWatch()
                }) {
                    Text("Отписаться", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showUnwatchDialog = false }) {
                    Text("Отмена", color = MaterialTheme.colorScheme.primary)
                }
            }
        )
    }
}

/**
 * Big prominent watch/unwatch button with a particle burst when subscribing.
 */
@Composable
private fun WatchActionButton(
    isWatching: Boolean,
    isToggling: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val explosionAnim = remember { Animatable(0f) }
    var wasWatching by remember { mutableStateOf(isWatching) }
    val primaryColor = MaterialTheme.colorScheme.primary

    val particles = remember(primaryColor) {
        List(18) { i ->
            val angle = (i * 20 + Random.nextInt(-8, 8)) * (Math.PI / 180f).toFloat()
            Particle(
                id = i,
                angle = angle,
                distance = Random.nextFloat() * 70f + 50f,
                size = Random.nextFloat() * 4f + 2f,
                color = listOf(primaryColor, Cyan, Pink, Color.White).random()
            )
        }
    }

    LaunchedEffect(isWatching) {
        if (!wasWatching && isWatching) {
            explosionAnim.snapTo(0f)
            explosionAnim.animateTo(1f, tween(650, easing = FastOutSlowInEasing))
        }
        wasWatching = isWatching
    }

    val buttonScale by animateFloatAsState(
        targetValue = if (isToggling) 0.96f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "watchScale"
    )
    val containerColor by animateColorAsState(
        targetValue = if (isWatching) DarkSurfaceVariant else MaterialTheme.colorScheme.primary,
        animationSpec = tween(300),
        label = "watchContainer"
    )
    val contentColor by animateColorAsState(
        targetValue = if (isWatching) MaterialTheme.colorScheme.primary else Color(0xFF0A0A0A),
        animationSpec = tween(300),
        label = "watchContent"
    )

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        // Particle burst behind the button
        if (explosionAnim.value > 0f && explosionAnim.value < 1f) {
            Canvas(modifier = Modifier.matchParentSize()) {
                val center = Offset(size.width / 2f, size.height / 2f)
                val progress = explosionAnim.value
                val alpha = if (progress > 0.5f) (1f - progress) * 2f else 1f
                particles.forEach { particle ->
                    val currentDist = particle.distance * (1f - (1f - progress) * (1f - progress))
                    drawCircle(
                        color = particle.color.copy(alpha = alpha),
                        radius = particle.size * (1f - progress * 0.3f),
                        center = Offset(
                            x = center.x + cos(particle.angle) * currentDist,
                            y = center.y + sin(particle.angle) * currentDist
                        ),
                        style = Fill
                    )
                }
            }
        }

        Button(
            onClick = onClick,
            enabled = !isToggling,
            colors = ButtonDefaults.buttonColors(
                containerColor = containerColor,
                contentColor = contentColor,
                disabledContainerColor = containerColor.copy(alpha = 0.6f),
                disabledContentColor = contentColor.copy(alpha = 0.6f)
            ),
            shape = RoundedCornerShape(24.dp),
            contentPadding = PaddingValues(horizontal = 20.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(46.dp)
                .scale(buttonScale)
                .then(
                    if (isWatching) Modifier.border(
                        1.5.dp,
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                        RoundedCornerShape(24.dp)
                    ) else Modifier
                )
        ) {
            if (isToggling) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    color = contentColor,
                    strokeWidth = 2.dp
                )
            } else {
                Icon(
                    if (isWatching) Icons.Default.Check else Icons.Default.PersonAdd,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isWatching) stringResource(R.string.user_watching) else stringResource(R.string.user_watch),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun ActionIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(46.dp)
            .clip(RoundedCornerShape(23.dp))
            .background(DarkSurfaceVariant)
            .border(1.dp, BorderSubtleDark, RoundedCornerShape(23.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            icon,
            contentDescription = contentDescription,
            tint = Color.White,
            modifier = Modifier.size(20.dp)
        )
    }
}

// -----------------------------------------------------------------------------------------
// Stats card — each block is tappable and switches the corresponding tab
// -----------------------------------------------------------------------------------------

@Composable
private fun StatsCard(
    profile: UserProfile,
    selectedTab: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(DarkSurface)
            .border(1.dp, BorderDark, RoundedCornerShape(20.dp))
            .padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        StatBlock(
            count = profile.galleryCount,
            label = stringResource(R.string.user_gallery),
            selected = selectedTab == UserViewModel.TAB_GALLERY,
            onClick = { onSelect(UserViewModel.TAB_GALLERY) },
            modifier = Modifier.weight(1f)
        )
        StatDivider()
        StatBlock(
            count = profile.favoritesCount,
            label = stringResource(R.string.profile_favorites),
            selected = selectedTab == UserViewModel.TAB_FAVORITES,
            onClick = { onSelect(UserViewModel.TAB_FAVORITES) },
            modifier = Modifier.weight(1f)
        )
        StatDivider()
        StatBlock(
            count = profile.watchingCount,
            label = stringResource(R.string.user_watching),
            selected = selectedTab == UserViewModel.TAB_WATCHING,
            onClick = { onSelect(UserViewModel.TAB_WATCHING) },
            modifier = Modifier.weight(1f)
        )
        StatDivider()
        StatBlock(
            count = profile.watchersCount,
            label = stringResource(R.string.user_watchers),
            selected = selectedTab == UserViewModel.TAB_WATCHERS,
            onClick = { onSelect(UserViewModel.TAB_WATCHERS) },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun StatBlock(
    count: Int,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val valueColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primary else Color.White,
        animationSpec = tween(250),
        label = "statValueColor"
    )
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 2.dp)
    ) {
        Text(
            text = formatCount(count),
            fontWeight = FontWeight.Bold,
            fontSize = 17.sp,
            color = valueColor
        )
        Text(
            text = label,
            fontSize = 11.sp,
            color = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.8f) else SubtleGray
        )
    }
}

@Composable
private fun StatDivider() {
    Box(
        modifier = Modifier
            .width(1.dp)
            .height(30.dp)
            .background(DividerDark)
    )
}

// -----------------------------------------------------------------------------------------
// Bio card — expandable
// -----------------------------------------------------------------------------------------

@Composable
private fun BioCard(text: String, modifier: Modifier = Modifier) {
    var expanded by remember { mutableStateOf(false) }
    var hasOverflow by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(DarkSurface)
            .border(1.dp, BorderDark, RoundedCornerShape(20.dp))
            .clickable { if (hasOverflow || expanded) expanded = !expanded }
            .animateContentSize()
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Default.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(15.dp)
            )
            Spacer(modifier = Modifier.width(7.dp))
            Text(
                text = stringResource(R.string.profile_about),
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                color = Color.White
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = text,
            fontSize = 13.sp,
            lineHeight = 19.sp,
            color = DarkOnSurface,
            maxLines = if (expanded) Int.MAX_VALUE else 4,
            overflow = TextOverflow.Ellipsis,
            onTextLayout = { result ->
                if (!expanded) hasOverflow = result.hasVisualOverflow
            }
        )
        if (hasOverflow || expanded) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = if (expanded) stringResource(R.string.profile_show_less) else stringResource(R.string.profile_show_more),
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

// -----------------------------------------------------------------------------------------
// Info chips: species, trades/commissions status, views, journals
// -----------------------------------------------------------------------------------------

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun InfoChips(profile: UserProfile, modifier: Modifier = Modifier) {
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (profile.species.isNotBlank()) {
            InfoChip(icon = Icons.Default.Pets, text = profile.species)
        }
        if (profile.acceptingTrades.isNotBlank()) {
            StatusChip(label = "Trades", value = profile.acceptingTrades)
        }
        if (profile.acceptingCommissions.isNotBlank()) {
            StatusChip(label = "Commissions", value = profile.acceptingCommissions)
        }
        if (profile.views > 0) {
            InfoChip(icon = Icons.Default.Visibility, text = "${formatCount(profile.views)} views")
        }
        if (profile.journalsCount > 0) {
            InfoChip(icon = Icons.AutoMirrored.Filled.MenuBook, text = "${formatCount(profile.journalsCount)} journals")
        }
    }
}

@Composable
private fun InfoChip(icon: ImageVector, text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(ChipBackground)
            .border(1.dp, ChipBorder, RoundedCornerShape(20.dp))
            .padding(horizontal = 12.dp, vertical = 7.dp)
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(14.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(text = text, fontSize = 12.sp, color = DarkOnSurface)
    }
}

@Composable
private fun StatusChip(label: String, value: String) {
    val isOpen = value.equals("yes", ignoreCase = true)
    val isClosed = value.equals("no", ignoreCase = true)
    val dotColor = when {
        isOpen -> Rating_General
        isClosed -> Rating_Adult
        else -> SubtleGray
    }
    val stateText = when {
        isOpen -> "Open"
        isClosed -> "Closed"
        else -> value
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(ChipBackground)
            .border(1.dp, ChipBorder, RoundedCornerShape(20.dp))
            .padding(horizontal = 12.dp, vertical = 7.dp)
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(dotColor)
        )
        Spacer(modifier = Modifier.width(7.dp))
        Text(text = "$label: $stateText", fontSize = 12.sp, color = DarkOnSurface)
    }
}

// -----------------------------------------------------------------------------------------
// Contact links — horizontally scrollable chips
// -----------------------------------------------------------------------------------------

@Composable
private fun ContactLinksRow(
    links: List<ContactLink>,
    onOpen: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Links",
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
            color = Color.White,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            links.forEach { link ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(ChipBackground)
                        .border(1.dp, ChipBorder, RoundedCornerShape(20.dp))
                        .clickable { onOpen(link.url) }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Icon(
                        contactIcon(link.type),
                        contentDescription = link.type,
                        tint = Cyan,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = link.label,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = DarkOnSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

private fun contactIcon(type: String): ImageVector = when (type.lowercase()) {
    "website" -> Icons.Default.Language
    "youtube" -> Icons.Default.PlayArrow
    "twitter" -> Icons.Default.AlternateEmail
    "twitch" -> Icons.Default.VideogameAsset
    "facebook" -> Icons.Default.Public
    "telegram" -> Icons.AutoMirrored.Filled.Send
    "instagram" -> Icons.Default.PhotoCamera
    else -> Icons.Default.Link
}

// -----------------------------------------------------------------------------------------
// Loading skeleton for the hero header
// -----------------------------------------------------------------------------------------

@Composable
private fun HeaderSkeleton() {
    val transition = rememberInfiniteTransition(label = "skeleton")
    val alpha by transition.animateFloat(
        initialValue = 0.30f,
        targetValue = 0.65f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "skeletonAlpha"
    )

    Column(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .background(DarkSurfaceVariant.copy(alpha = alpha))
        )
        Row(
            modifier = Modifier
                .padding(start = 16.dp, top = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(DarkSurfaceVariant.copy(alpha = alpha))
            )
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Box(
                    modifier = Modifier
                        .width(150.dp)
                        .height(18.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(DarkSurfaceVariant.copy(alpha = alpha))
                )
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .width(100.dp)
                        .height(13.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(DarkSurfaceVariant.copy(alpha = alpha))
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Box(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .fillMaxWidth()
                .height(46.dp)
                .clip(RoundedCornerShape(23.dp))
                .background(DarkSurfaceVariant.copy(alpha = alpha))
        )
    }
}

// -----------------------------------------------------------------------------------------
// Helpers
// -----------------------------------------------------------------------------------------

private fun openUrl(context: Context, url: String) {
    runCatching {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }
}
