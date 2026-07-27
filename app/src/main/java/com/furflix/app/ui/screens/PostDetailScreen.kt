package com.furflix.app.ui.screens

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.res.stringResource

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.text.HtmlCompat
import androidx.core.graphics.toColorInt
import android.text.method.LinkMovementMethod
import android.widget.TextView
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.furflix.app.R
import com.furflix.app.data.model.Comment
import com.furflix.app.data.model.Submission
import com.furflix.app.ui.components.CustomTabRow
import com.furflix.app.ui.components.DefaultHazeStyle
import com.furflix.app.ui.screens.GlassActionBar
import com.furflix.app.ui.screens.GlassActionItem
import com.furflix.app.ui.components.Particle
import com.furflix.app.ui.components.HazeTopAppBar
import com.furflix.app.ui.components.ImageLoaderFactory
import com.furflix.app.data.local.DownloadHelper
import android.widget.Toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.furflix.app.ui.components.ZoomableImageViewer
import com.furflix.app.ui.theme.*
import com.furflix.app.viewmodel.MainViewModel
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostDetailScreen(
    initialPostId: String,
    submissions: List<Submission>,
    onLoadMore: () -> Unit,
    onBack: () -> Unit,
    onAuthorClick: (String) -> Unit = {},
    onNavigateToPost: (String) -> Unit = {},
    onTagClick: (String) -> Unit = {},
    viewModel: MainViewModel
) {
    val submissionDetailsMap by viewModel.submissionDetails.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    val foundInList = submissions.any { it.id == initialPostId }
    val effectiveSubmissions = remember(submissions, initialPostId) {
        if (foundInList) submissions else {
            listOf(Submission(id = initialPostId, title = "", author = "", thumbnailUrl = "", fullImageUrl = "", description = "", tags = emptyList()))
        }
    }

    val initialIndex = remember(effectiveSubmissions) {
        effectiveSubmissions.indexOfFirst { it.id == initialPostId }.takeIf { it >= 0 } ?: 0
    }

    val pagerState = rememberPagerState(
        initialPage = initialIndex,
        pageCount = { effectiveSubmissions.size.coerceAtLeast(1) }
    )

    LaunchedEffect(pagerState.currentPage) {
        if (pagerState.currentPage >= effectiveSubmissions.size - 3) {
            onLoadMore()
        }
    }

    BackHandler { onBack() }

    HorizontalPager(state = pagerState) { page ->
        val currentPostId = effectiveSubmissions.getOrNull(page)?.id ?: initialPostId

        LaunchedEffect(currentPostId) {
            viewModel.loadSubmission(currentPostId)
        }

        val submission = submissionDetailsMap[currentPostId]
        val hazeState = remember { HazeState() }
        val scrollState = rememberScrollState()
        var showZoom by remember { mutableStateOf(false) }
        var downloadDone by remember { mutableStateOf(false) }

        LaunchedEffect(downloadDone) {
            if (downloadDone) {
                kotlinx.coroutines.delay(4000)
                downloadDone = false
            }
        }

        Box(modifier = Modifier.fillMaxSize().navigationBarsPadding()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .hazeSource(state = hazeState)
            ) {

                if (isLoading && submission == null) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                } else if (submission != null) {
                    val sub = submission
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(scrollState)
                    ) {
                        // ── Hero image ──────────────────────────────────────
                        HeroImage(sub, onImageClick = { showZoom = true })

                        Column(
                            modifier = Modifier.padding(horizontal = 20.dp)
                        ) {
                            // Tab state lives up here so the stats bar can jump
                            // straight to the Comments tab.
                            var selectedTab by remember { mutableIntStateOf(0) }

                            Spacer(modifier = Modifier.height(16.dp))

                            // ── Title ───────────────────────────────────────
                            Text(
                                text = sub.title,
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )

                            // Accent underline
                            Spacer(modifier = Modifier.height(8.dp))
                            Box(
                                modifier = Modifier
                                    .width(48.dp)
                                    .height(3.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(
                                        Brush.horizontalGradient(
                                            listOf(Cyan, Pink)
                                        )
                                    )
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            // ── Author card ─────────────────────────────────
                            if (sub.author.isNotEmpty()) {
                                AuthorCard(
                                    sub = sub,
                                    onAuthorClick = onAuthorClick
                                )
                            }

                            Spacer(modifier = Modifier.height(20.dp))

                            // ── Info chips ──────────────────────────────────
                            InfoChipsRow(sub)

                            Spacer(modifier = Modifier.height(16.dp))

                            // ── Stats bar ───────────────────────────────────
                            StatsRow(
                                sub = sub,
                                onToggleFavorite = { viewModel.toggleFavorite(currentPostId) },
                                onCommentsClick = { selectedTab = 2 }
                            )

                            // ── Date ────────────────────────────────────────
                            if (sub.date.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(14.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.Schedule,
                                        contentDescription = null,
                                        tint = MutedText,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Text(
                                        text = sub.date,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MutedText
                                    )
                                }
                            }

                            // ── Tags ────────────────────────────────────────
                            if (sub.tags.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(24.dp))
                                TagsSection(tags = sub.tags, onTagClick = onTagClick)
                            }

                            Spacer(modifier = Modifier.height(24.dp))

                            // ── Tabs: Description / Meta / Comments ─────────
                            val tabs = listOf(
                                stringResource(R.string.post_description),
                                stringResource(R.string.post_meta_info),
                                "${stringResource(R.string.post_comments)} (${sub.commentList.size})"
                            )

                            CustomTabRow(
                                tabs = tabs,
                                selectedTab = selectedTab,
                                onTabSelected = { selectedTab = it }
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            when (selectedTab) {
                                0 -> DescriptionContent(
                                    sub = sub,
                                    onAuthorClick = onAuthorClick,
                                    onNavigateToPost = onNavigateToPost
                                )
                                1 -> MetaInfoContent(sub)
                                2 -> CommentsContent(
                                    comments = sub.commentList,
                                    onAuthorClick = onAuthorClick
                                )
                            }

                            Spacer(modifier = Modifier.height(150.dp))
                        }
                    }
                }
            }

            // Back arrow top-left
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(top = 48.dp, start = 16.dp)
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.5f))
                    .clickable { onBack() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }

            // Bottom fade
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(140.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color(0xFF0A0A0C).copy(alpha = 0.7f),
                                Color(0xFF0A0A0C).copy(alpha = 0.95f)
                            )
                        )
                    )
            )

            // Glass action bar
            val downloadIcon = if (downloadDone) Icons.Default.Check else Icons.Default.Download
            val bottomBarItems = listOf(
                GlassActionItem(androidx.compose.ui.res.stringResource(id = R.string.action_back), Icons.AutoMirrored.Filled.ArrowBack, Color(0xFF00E5FF)),
                GlassActionItem(androidx.compose.ui.res.stringResource(id = R.string.cd_share), Icons.Default.Share, Color(0xFFFFD700)),
                GlassActionItem(
                    title = androidx.compose.ui.res.stringResource(id = R.string.action_favorite),
                    icon = if (submission?.isFavorited == true) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    color = Color(0xFFFF007F),
                    isActive = submission?.isFavorited == true
                ),
                GlassActionItem(
                    title = androidx.compose.ui.res.stringResource(id = R.string.cd_download),
                    icon = downloadIcon,
                    color = Color(0xFF4CD964),
                    isActive = downloadDone
                )
            )

            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 24.dp, vertical = 24.dp)
                    .fillMaxWidth()
                    .height(64.dp)
            ) {
                GlassActionBar(
                    items = bottomBarItems,
                    hazeState = hazeState,
                    onItemClick = { index ->
                        val sub = submission ?: return@GlassActionBar
                        when (index) {
                            0 -> onBack()
                            1 -> {
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, "https://www.furaffinity.net/view/${currentPostId}/")
                                    putExtra(Intent.EXTRA_SUBJECT, sub.title)
                                }
                                context.startActivity(Intent.createChooser(shareIntent, "Share via"))
                            }
                            2 -> {
                                viewModel.toggleFavorite(currentPostId)
                            }
                            3 -> {
                                if (sub.fullImageUrl.isNotEmpty()) {
                                    viewModel.downloadImage(
                                        sub.fullImageUrl,
                                        "${sub.title.replace(" ", "_")}_${sub.id}.jpg"
                                    )
                                    downloadDone = true
                                }
                            }
                        }
                    }
                )
            }

            // Full-screen zoomable artwork viewer
            if (showZoom && submission?.fullImageUrl?.isNotEmpty() == true) {
                ZoomableImageViewer(
                    imageUrl = submission?.fullImageUrl ?: "",
                    onClose = { showZoom = false }
                )
            }
        }
    }
}

// -----------------------------------------------------------------------------------------
// Hero image with gradient fade + rating badge
// -----------------------------------------------------------------------------------------

@Composable
private fun HeroImage(sub: Submission, onImageClick: () -> Unit = {}) {
    if (sub.fullImageUrl.isNotEmpty()) {
        Box(modifier = Modifier.fillMaxWidth()) {
            SubcomposeAsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(sub.fullImageUrl)
                    .crossfade(300)
                    .build(),
                imageLoader = ImageLoaderFactory.getInstance(LocalContext.current),
                contentDescription = sub.title,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp))
                    .clickable(onClick = onImageClick),
                contentScale = ContentScale.FillWidth,
                loading = {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(4f / 3f)
                            .background(DarkSurfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(28.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                            strokeWidth = 2.dp
                        )
                    }
                },
                error = {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(4f / 3f)
                            .background(DarkSurfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.BrokenImage,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            )

            // Gradient fade into background at the bottom
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                DarkBackground.copy(alpha = 0.4f),
                                DarkBackground
                            )
                        )
                    )
            )

            // Rating badge overlaid on image
            if (sub.rating.isNotEmpty()) {
                Surface(
                    modifier = Modifier
                        .padding(14.dp)
                        .align(Alignment.TopStart),
                    shape = RoundedCornerShape(8.dp),
                    color = when {
                        sub.rating.contains("General", true) -> Rating_General.copy(alpha = 0.9f)
                        sub.rating.contains("Mature", true) -> Rating_Mature.copy(alpha = 0.9f)
                        sub.rating.contains("Adult", true) -> Rating_Adult.copy(alpha = 0.9f)
                        else -> MaterialTheme.colorScheme.primary.copy(alpha = 0.9f)
                    }
                ) {
                    Text(
                        text = sub.rating,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            // Zoom hint — translucent chip bottom-end, signals the image opens fullscreen
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(14.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.45f))
                    .clickable(onClick = onImageClick)
                    .padding(8.dp)
            ) {
                Icon(
                    Icons.Default.ZoomIn,
                    contentDescription = "Zoom artwork",
                    tint = Color.White.copy(alpha = 0.9f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

// -----------------------------------------------------------------------------------------
// Author card with neon ring avatar
// -----------------------------------------------------------------------------------------

@Composable
private fun AuthorCard(
    sub: Submission,
    onAuthorClick: (String) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(AuthorCardBackground)
            .border(1.dp, AuthorCardBorder, RoundedCornerShape(18.dp))
            .clickable { onAuthorClick(sub.authorUsername.ifEmpty { sub.author }) }
            .padding(14.dp)
    ) {
        // Avatar with gradient ring
        Box(
            modifier = Modifier.size(56.dp),
            contentAlignment = Alignment.Center
        ) {
            // Neon gradient ring
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .border(
                        width = 1.5.dp,
                        brush = Brush.linearGradient(listOf(Cyan, Pink)),
                        shape = CircleShape
                    )
            )
            if (sub.authorAvatar.isNotEmpty()) {
                SubcomposeAsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(sub.authorAvatar)
                        .crossfade(true)
                        .build(),
                    imageLoader = ImageLoaderFactory.getInstance(LocalContext.current),
                    contentDescription = sub.author,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(DarkSurfaceVariant),
                    error = {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(DarkSurfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Person,
                                contentDescription = null,
                                tint = SubtleGray,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(DarkSurfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = null,
                        tint = SubtleGray,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = sub.author,
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = stringResource(R.string.post_view_profile),
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
            )
        }

        Icon(
            Icons.Default.ChevronRight,
            contentDescription = stringResource(R.string.post_view_profile),
            tint = SubtleGray,
            modifier = Modifier.size(22.dp)
        )
    }
}

// -----------------------------------------------------------------------------------------
// Info chips: rating, category, species, gender — icon-based with colored accents
// -----------------------------------------------------------------------------------------

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun InfoChipsRow(sub: Submission) {
    val hasInfo = sub.rating.isNotEmpty() || sub.category.isNotEmpty() ||
            sub.species.isNotEmpty() || sub.gender.isNotEmpty()
    if (!hasInfo) return

    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (sub.rating.isNotEmpty()) {
            val ratingColor = when {
                sub.rating.contains("General", true) -> Rating_General
                sub.rating.contains("Mature", true) -> Rating_Mature
                sub.rating.contains("Adult", true) -> Rating_Adult
                else -> MaterialTheme.colorScheme.primary
            }
            InfoChipCard(Icons.Default.Shield, stringResource(R.string.filter_rating), sub.rating, ratingColor)
        }
        if (sub.category.isNotEmpty()) InfoChipCard(Icons.Default.Folder, "Category", sub.category, Cyan)
        if (sub.species.isNotEmpty()) InfoChipCard(Icons.Default.Pets, "Species", sub.species, Green)
        if (sub.gender.isNotEmpty()) InfoChipCard(Icons.Default.Person, "Gender", sub.gender, Gold)
    }
}

@Composable
private fun InfoChipCard(
    icon: ImageVector,
    label: String,
    value: String,
    accentColor: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(ChipBackground)
            .border(1.dp, ChipBorder, RoundedCornerShape(14.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(accentColor.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(16.dp)
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Text(
                text = label,
                fontSize = 10.sp,
                color = SubtleGray
            )
            Text(
                text = value,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// -----------------------------------------------------------------------------------------
// Stats bar: views / favorites / comments — colored, tappable favorite
// -----------------------------------------------------------------------------------------

@Composable
private fun StatsRow(
    sub: Submission,
    onToggleFavorite: () -> Unit,
    onCommentsClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CardBackground)
            .border(1.dp, BorderDark, RoundedCornerShape(16.dp))
            .padding(vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        StatBarItem(
            icon = Icons.Default.Visibility,
            value = formatCount(sub.views),
            color = Cyan
        )
        StatDivider()
        StatBarItem(
            icon = if (sub.isFavorited) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
            value = formatCount(sub.favorites),
            color = if (sub.isFavorited) Pink else SubtleGray,
            onClick = onToggleFavorite
        )
        StatDivider()
        StatBarItem(
            icon = Icons.Default.ChatBubbleOutline,
            value = formatCount(sub.comments),
            color = Gold,
            onClick = onCommentsClick
        )
    }
}

@Composable
private fun StatBarItem(
    icon: ImageVector,
    value: String,
    color: Color,
    onClick: (() -> Unit)? = null
) {
    val modifier = if (onClick != null) {
        Modifier.clickable(onClick = onClick)
    } else Modifier

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = color
        )
        Spacer(modifier = Modifier.width(7.dp))
        Text(
            text = value,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    }
}

@Composable
private fun StatDivider() {
    Box(
        modifier = Modifier
            .width(1.dp)
            .height(24.dp)
            .background(StatsDivider)
    )
}

// -----------------------------------------------------------------------------------------
// Tags section
// -----------------------------------------------------------------------------------------

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TagsSection(tags: List<String>, onTagClick: (String) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            Icons.Default.LocalOffer,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(15.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = "Теги",
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.White
        )
    }
    Spacer(modifier = Modifier.height(10.dp))
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        tags.forEach { tag ->
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(ChipBackground)
                    .border(1.dp, ChipBorder, RoundedCornerShape(10.dp))
                    .clickable { onTagClick(tag) }
                    .padding(horizontal = 12.dp, vertical = 7.dp)
            ) {
                Text(
                    text = "#$tag",
                    fontSize = 12.sp,
                    color = DarkOnSurface
                )
            }
        }
    }
}

// -----------------------------------------------------------------------------------------
// Tab content: Description
// -----------------------------------------------------------------------------------------

@Composable
private fun DescriptionContent(
    sub: Submission,
    onAuthorClick: (String) -> Unit,
    onNavigateToPost: (String) -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    if (sub.description.isNotEmpty()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(CardBackground)
                .border(1.dp, BorderDark, RoundedCornerShape(16.dp))
                .padding(16.dp)
        ) {
            val textColor = "#E0E0E0".toColorInt()
            val linkColor = LinkColor.hashCode()
            AndroidView(
                factory = { ctx ->
                    TextView(ctx).apply {
                        setTextColor(textColor)
                        setLinkTextColor(linkColor)
                        textSize = 15f
                        movementMethod = LinkMovementMethod.getInstance()
                    }
                },
                update = { textView ->
                    val htmlSpannable = android.text.SpannableStringBuilder(
                        HtmlCompat.fromHtml(sub.description, HtmlCompat.FROM_HTML_MODE_COMPACT)
                    )
                    val spans = htmlSpannable.getSpans(0, htmlSpannable.length, android.text.style.URLSpan::class.java)
                    for (span in spans) {
                        val start = htmlSpannable.getSpanStart(span)
                        val end = htmlSpannable.getSpanEnd(span)
                        val flags = htmlSpannable.getSpanFlags(span)
                        val url = span.url
                        htmlSpannable.removeSpan(span)
                        htmlSpannable.setSpan(object : android.text.style.ClickableSpan() {
                            override fun onClick(widget: android.view.View) {
                                if (url.contains("/user/")) {
                                    val username = url.substringAfter("/user/").substringBefore("/")
                                    if (username.isNotEmpty()) onAuthorClick(username)
                                } else if (url.contains("/view/")) {
                                    val id = url.substringAfter("/view/").substringBefore("/")
                                    if (id.isNotEmpty()) onNavigateToPost(id)
                                } else {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                    context.startActivity(intent)
                                }
                            }
                        }, start, end, flags)
                    }
                    textView.text = htmlSpannable
                }
            )
        }
    } else {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(CardBackground)
                .border(1.dp, BorderDark, RoundedCornerShape(16.dp))
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("No description", color = MutedText, fontSize = 14.sp)
        }
    }
}

// -----------------------------------------------------------------------------------------
// Tab content: Meta Info
// -----------------------------------------------------------------------------------------

@Composable
private fun MetaInfoContent(sub: Submission) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        MetaInfoCard("Category", sub.category)
        MetaInfoCard("Species", sub.species)
        MetaInfoCard("Gender", sub.gender)
        MetaInfoCard("Rating", sub.rating)
        MetaInfoCard(stringResource(R.string.meta_resolution), sub.resolution)
        MetaInfoCard(stringResource(R.string.meta_file_size), sub.fileSize)
        MetaInfoCard(stringResource(R.string.meta_file_type), sub.fileType)
        MetaInfoCard(stringResource(R.string.meta_date), sub.date)
    }
}

@Composable
private fun MetaInfoCard(label: String, value: String) {
    if (value.isEmpty()) return
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(CardBackground)
            .border(1.dp, BorderDark, RoundedCornerShape(12.dp))
            .padding(horizontal = 16.dp, vertical = 13.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, fontSize = 13.sp, color = SubtleGray)
        Text(
            text = value,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

// -----------------------------------------------------------------------------------------
// Tab content: Comments
// -----------------------------------------------------------------------------------------

@Composable
private fun CommentsContent(
    comments: List<Comment>,
    onAuthorClick: (String) -> Unit
) {
    if (comments.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(CardBackground)
                .border(1.dp, BorderDark, RoundedCornerShape(16.dp))
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Default.ChatBubbleOutline,
                    contentDescription = null,
                    tint = MutedText,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text("No comments yet", color = MutedText, fontSize = 14.sp)
            }
        }
    } else {
        Column {
            comments.forEachIndexed { index, comment ->
                CommentCard(comment = comment, onAuthorClick = onAuthorClick)
                if (index < comments.size - 1) {
                    Spacer(modifier = Modifier.height(10.dp))
                }
            }
        }
    }
}

@Composable
private fun CommentCard(comment: Comment, onAuthorClick: (String) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(CardBackground)
            .border(1.dp, BorderDark, RoundedCornerShape(14.dp))
            .padding(14.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Avatar with subtle ring
            Box(
                modifier = Modifier.size(36.dp),
                contentAlignment = Alignment.Center
            ) {
                if (comment.authorAvatar.isNotEmpty()) {
                    SubcomposeAsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(comment.authorAvatar)
                            .crossfade(true)
                            .build(),
                        imageLoader = ImageLoaderFactory.getInstance(LocalContext.current),
                        contentDescription = comment.author,
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), CircleShape)
                            .background(DarkSurfaceVariant)
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), CircleShape)
                            .background(DarkSurfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = null,
                            tint = SubtleGray,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = comment.author.ifEmpty { comment.authorUsername },
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.clickable { onAuthorClick(comment.authorUsername) }
                )
                if (comment.title.isNotEmpty()) {
                    Text(
                        text = comment.title,
                        fontSize = 11.sp,
                        color = SubtleGray
                    )
                }
            }

            if (comment.timestamp > 0) {
                val dateStr = remember(comment.timestamp) {
                    try {
                        val sdf = java.text.SimpleDateFormat("MMM d, yyyy", java.util.Locale.getDefault())
                        sdf.format(java.util.Date(comment.timestamp * 1000))
                    } catch (_: Exception) { "" }
                }
                if (dateStr.isNotEmpty()) {
                    Text(text = dateStr, fontSize = 11.sp, color = MutedText)
                }
            }
        }

        if (comment.text.isNotEmpty()) {
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = comment.text,
                fontSize = 13.sp,
                color = CommentBodyColor,
                lineHeight = 18.sp
            )
        }
    }
}

// -----------------------------------------------------------------------------------------
// Helpers
// -----------------------------------------------------------------------------------------

