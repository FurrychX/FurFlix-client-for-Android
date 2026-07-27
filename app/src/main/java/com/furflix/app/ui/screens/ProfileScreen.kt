package com.furflix.app.ui.screens

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.res.stringResource
import com.furflix.app.R

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import kotlinx.coroutines.flow.distinctUntilChanged
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.furflix.app.ui.components.CustomTabRow
import com.furflix.app.ui.components.EmptyState
import com.furflix.app.ui.components.LoadingMoreFooter
import com.furflix.app.ui.components.SubmissionCard
import com.furflix.app.ui.components.WatchlistGridItem
import com.furflix.app.ui.components.WatchlistItem
import com.furflix.app.ui.theme.*
import com.furflix.app.viewmodel.WatchlistViewModel
import kotlinx.coroutines.launch

@Composable
fun ProfileScreen(
    isGrid: Boolean,
    onNavigateToUser: (String) -> Unit,
    onNavigateToPost: (String) -> Unit,
    onNavigateToFavoritesPost: (String) -> Unit = onNavigateToPost,
    scrollToTopTrigger: Int = 0,
    viewModel: WatchlistViewModel = viewModel()
) {
    val followingList by viewModel.followingList.collectAsState()
    val followersList by viewModel.followersList.collectAsState()
    val favoritesList by viewModel.favoritesList.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isLoadingMore by viewModel.isLoadingMore.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val currentTab by viewModel.currentTab.collectAsState()
    val followingHasMore by viewModel.followingHasMore.collectAsState()
    val followersHasMore by viewModel.followersHasMore.collectAsState()

    val topTabs = listOf(stringResource(R.string.profile_favorites), stringResource(R.string.profile_watchlist))
    val pagerState = rememberPagerState(pageCount = { topTabs.size })
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(pagerState.currentPage) {
        when (pagerState.currentPage) {
            0 -> viewModel.setTab(WatchlistViewModel.TAB_FAVORITES)
            1 -> viewModel.setTab(WatchlistViewModel.TAB_FOLLOWING)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        CustomTabRow(
            tabs = topTabs,
            selectedTab = pagerState.currentPage,
            onTabSelected = { index ->
                coroutineScope.launch { pagerState.animateScrollToPage(index) }
            },
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
        )

        HorizontalPager(
            state = pagerState,
            userScrollEnabled = false,
            modifier = Modifier.weight(1f)
        ) { page ->
            when (page) {
                0 -> FavoritesContent(
                    submissions = favoritesList,
                    isLoading = isLoading,
                    isLoadingMore = isLoadingMore,
                    errorMessage = errorMessage,
                    isGrid = isGrid,
                    onNavigateToPost = onNavigateToFavoritesPost,
                    onNavigateToUser = onNavigateToUser,
                    onLoadMore = { viewModel.loadMore() },
                    scrollToTopTrigger = scrollToTopTrigger
                )
                1 -> WatchlistContent(
                    followingList = followingList,
                    followersList = followersList,
                    followingHasMore = followingHasMore,
                    followersHasMore = followersHasMore,
                    isLoading = isLoading,
                    isLoadingMore = isLoadingMore,
                    errorMessage = errorMessage,
                    currentTab = currentTab,
                    isGrid = isGrid,
                    onSetTab = { viewModel.setTab(it) },
                    onLoadMore = { viewModel.loadMore() },
                    onNavigateToUser = onNavigateToUser,
                    scrollToTopTrigger = scrollToTopTrigger
                )
            }
        }
    }
}

@Composable
private fun FavoritesContent(
    submissions: List<com.furflix.app.data.model.Submission>,
    isLoading: Boolean,
    isLoadingMore: Boolean,
    errorMessage: String?,
    isGrid: Boolean,
    onNavigateToPost: (String) -> Unit,
    onNavigateToUser: (String) -> Unit,
    onLoadMore: () -> Unit = {},
    scrollToTopTrigger: Int = 0
) {
    when {
        isLoading && submissions.isEmpty() -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        }
        errorMessage != null && submissions.isEmpty() -> {
            EmptyState(message = errorMessage, icon = Icons.Default.ErrorOutline)
        }
        submissions.isEmpty() && !isLoadingMore -> {
            EmptyState(message = "No favorites yet.", icon = Icons.Default.FavoriteBorder)
        }
        else -> {
            Crossfade(targetState = isGrid, animationSpec = tween(300), label = "favGridList") { grid ->
                if (grid) {
                    val gridState = rememberLazyGridState()
                    LaunchedEffect(scrollToTopTrigger) {
                        if (scrollToTopTrigger > 0) gridState.animateScrollToItem(0)
                    }
                    LaunchedEffect(gridState) {
                        snapshotFlow {
                            val lastVisible = gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                            val total = gridState.layoutInfo.totalItemsCount
                            lastVisible >= total - 4
                        }.distinctUntilChanged().collect { shouldLoadMore ->
                            if (shouldLoadMore && !isLoadingMore && submissions.isNotEmpty()) {
                                onLoadMore()
                            }
                        }
                    }
                    LazyVerticalGrid(
                        state = gridState,
                        modifier = Modifier.fillMaxSize(),
                        columns = GridCells.Fixed(2),
                        contentPadding = PaddingValues(start = 8.dp, end = 8.dp, top = 8.dp, bottom = 120.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(submissions, key = { it.id }) { submission ->
                            SubmissionCard(
                                submission = submission,
                                onClick = { onNavigateToPost(submission.id) },
                                showTitle = false,
                                compact = true,
                                onAuthorClick = { onNavigateToUser(it) }
                            )
                        }
                        if (isLoadingMore) {
                            item { LoadingMoreFooter() }
                        }
                    }
                } else {
                    val listState = rememberLazyListState()
                    LaunchedEffect(scrollToTopTrigger) {
                        if (scrollToTopTrigger > 0) listState.animateScrollToItem(0)
                    }
                    LaunchedEffect(listState) {
                        snapshotFlow {
                            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                            val total = listState.layoutInfo.totalItemsCount
                            lastVisible >= total - 4
                        }.distinctUntilChanged().collect { shouldLoadMore ->
                            if (shouldLoadMore && !isLoadingMore && submissions.isNotEmpty()) {
                                onLoadMore()
                            }
                        }
                    }
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 8.dp, end = 8.dp, top = 8.dp, bottom = 120.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(submissions, key = { it.id }) { submission ->
                            SubmissionCard(
                                submission = submission,
                                onClick = { onNavigateToPost(submission.id) },
                                onAuthorClick = { onNavigateToUser(it) }
                            )
                        }
                        if (isLoadingMore) {
                            item { LoadingMoreFooter() }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WatchlistContent(
    followingList: List<com.furflix.app.data.model.WatchlistUser>,
    followersList: List<com.furflix.app.data.model.WatchlistUser>,
    followingHasMore: Boolean,
    followersHasMore: Boolean,
    isLoading: Boolean,
    isLoadingMore: Boolean,
    errorMessage: String?,
    currentTab: Int,
    isGrid: Boolean,
    onSetTab: (Int) -> Unit,
    onLoadMore: () -> Unit,
    onNavigateToUser: (String) -> Unit,
    scrollToTopTrigger: Int = 0
) {
    val currentData = if (currentTab == WatchlistViewModel.TAB_FOLLOWING) followingList else followersList
    val gridState = rememberLazyGridState()
    val listState = rememberLazyListState()

    LaunchedEffect(scrollToTopTrigger) {
        if (scrollToTopTrigger > 0) {
            try { gridState.animateScrollToItem(0) } catch (_: Exception) {}
            try { listState.animateScrollToItem(0) } catch (_: Exception) {}
        }
    }

    val followingCount = followingList.size.let { if (followingHasMore) "${it}+" else "$it" }
    val followersCount = followersList.size.let { if (followersHasMore) "${it}+" else "$it" }

    Column(Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val isFollowing = currentTab == WatchlistViewModel.TAB_FOLLOWING

            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onSetTab(WatchlistViewModel.TAB_FOLLOWING) }
                    .background(if (isFollowing) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else Color.Transparent)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    stringResource(R.string.profile_following_list).uppercase(),
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    letterSpacing = 1.sp,
                    color = if (isFollowing) MaterialTheme.colorScheme.primary else SubtleGray
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (isFollowing) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .padding(horizontal = 4.dp, vertical = 1.dp)
                ) {
                    Text(
                        followingCount,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isFollowing) MaterialTheme.colorScheme.onPrimary else SubtleGray
                    )
                }
            }

            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onSetTab(WatchlistViewModel.TAB_FOLLOWERS) }
                    .background(if (!isFollowing) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else Color.Transparent)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    stringResource(R.string.profile_followers_list).uppercase(),
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    letterSpacing = 1.sp,
                    color = if (!isFollowing) MaterialTheme.colorScheme.primary else SubtleGray
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (!isFollowing) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .padding(horizontal = 4.dp, vertical = 1.dp)
                ) {
                    Text(
                        followersCount,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (!isFollowing) MaterialTheme.colorScheme.onPrimary else SubtleGray
                    )
                }
            }

            Spacer(Modifier.weight(1f))
        }

        when {
            isLoading && currentData.isEmpty() -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }
            errorMessage != null && currentData.isEmpty() -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.ErrorOutline, null, Modifier.size(64.dp), tint = SubtleGray)
                        Spacer(Modifier.height(16.dp))
                        Text(errorMessage, style = MaterialTheme.typography.bodyLarge, color = SubtleGray, textAlign = TextAlign.Center, modifier = Modifier.padding(32.dp))
                    }
                }
            }
            else -> {
                Crossfade(targetState = isGrid, animationSpec = tween(300), label = "watchlistGridList") { grid ->
                    if (grid) {
                        LazyVerticalGrid(
                            state = gridState,
                            modifier = Modifier.fillMaxSize(),
                            columns = GridCells.Fixed(2),
                            contentPadding = PaddingValues(start = 8.dp, end = 8.dp, top = 4.dp, bottom = 120.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(currentData, key = { it.username }) { user ->
                                WatchlistGridItem(user = user, onClick = { onNavigateToUser(user.username) })
                            }
                            if (isLoadingMore) {
                                item { LoadingMoreFooter() }
                            }
                        }
                    } else {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(start = 8.dp, end = 8.dp, top = 4.dp, bottom = 120.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(currentData, key = { it.username }) { user ->
                                WatchlistItem(user = user, onClick = { onNavigateToUser(user.username) })
                            }
                            if (isLoadingMore) {
                                item { LoadingMoreFooter() }
                            }
                        }
                    }
                }

                LaunchedEffect(gridState, listState, currentTab) {
                    snapshotFlow {
                        if (isGrid) {
                            val lastVisible = gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                            val total = gridState.layoutInfo.totalItemsCount
                            lastVisible >= total - 5
                        } else {
                            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                            val total = listState.layoutInfo.totalItemsCount
                            lastVisible >= total - 5
                        }
                    }.collect { shouldLoadMore ->
                        if (shouldLoadMore && !isLoadingMore && currentData.isNotEmpty()) {
                            onLoadMore()
                        }
                    }
                }
            }
        }
    }
}
