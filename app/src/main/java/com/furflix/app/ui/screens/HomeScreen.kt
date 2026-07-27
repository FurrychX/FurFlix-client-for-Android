package com.furflix.app.ui.screens

import androidx.compose.material3.MaterialTheme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.runtime.*
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import com.furflix.app.R
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.distinctUntilChanged
import com.furflix.app.ui.components.SubmissionCard
import com.furflix.app.ui.components.GlassmorphicBottomBar
import com.furflix.app.ui.components.GlassBarTab
import com.furflix.app.ui.components.FurryTabIcons
import dev.chrisbanes.haze.HazeState
import com.furflix.app.ui.components.HazeTopAppBar
import com.furflix.app.ui.components.LoadingMoreFooter
import com.furflix.app.ui.components.PawPullIndicator
import dev.chrisbanes.haze.hazeSource

import com.furflix.app.ui.theme.*
import com.furflix.app.viewmodel.MainViewModel
import com.furflix.app.data.FA_CATEGORIES
import com.furflix.app.data.FA_ART_TYPES
import com.furflix.app.data.FA_GENDER_OPTIONS
import com.furflix.app.data.repository.FurRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToPost: (String) -> Unit,
    onNavigateToUser: (String) -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToDownloads: () -> Unit,
    onNavigateToFavoritesPost: (String) -> Unit = onNavigateToPost,
    viewModel: MainViewModel = viewModel(),
    watchlistViewModel: com.furflix.app.viewmodel.WatchlistViewModel = viewModel()
) {
    val browseSubmissions by viewModel.browseSubmissions.collectAsState()
    val latestSubmissions by viewModel.latestSubmissions.collectAsState()
    val searchSubmissions by viewModel.searchSubmissions.collectAsState()
    val currentSearchQuery by viewModel.searchQuery.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isLoadingMore by viewModel.isLoadingMore.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val isLoggedIn by viewModel.isLoggedIn.collectAsState()
    val nsfwEnabled by viewModel.nsfwEnabled.collectAsState()

    val selectedTab by viewModel.selectedTab.collectAsState()
    val hasSearched by viewModel.hasSearched.collectAsState()
    val recentSearches by viewModel.recentSearches.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    // Resolve tab labels as plain strings first — stringResource() is @Composable,
    // so it must not be called inside `remember { ... }`.
    val tabBrowse = stringResource(R.string.nav_tab_browse)
    val tabLatest = stringResource(R.string.nav_tab_latest)
    val tabSearch = stringResource(R.string.nav_tab_search)
    val tabProfile = stringResource(R.string.nav_tab_profile)

    val hazeState = remember { HazeState() }
    val bottomBarItems = remember(tabBrowse, tabLatest, tabSearch, tabProfile) {
        listOf(
            GlassBarTab(tabBrowse, FurryTabIcons.PawOutline, TabBrowseAccent),
            GlassBarTab(tabLatest, FurryTabIcons.TailSpark, TabLatestAccent),
            GlassBarTab(tabSearch, FurryTabIcons.SharkSearch, TabSearchAccent),
            GlassBarTab(tabProfile, FurryTabIcons.MuzzleProfile, TabProfileAccent)
        )
    }

    // View options
    val context = LocalContext.current
    val repository = remember { FurRepository.getInstance(context) }
    val isGrid by repository.isGridFlow.collectAsState(initial = true)
    
    var showTitles by rememberSaveable { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        viewModel.tryAutoLogin()
    }

    var backPressedTime by remember { mutableLongStateOf(0L) }
    val activity = context as? android.app.Activity
    androidx.activity.compose.BackHandler {
        if (System.currentTimeMillis() - backPressedTime < 2000) {
            activity?.finish()
        } else {
            backPressedTime = System.currentTimeMillis()
            android.widget.Toast.makeText(context, "Press back again to exit", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    val gridStates = remember { mutableMapOf<Int, LazyGridState>() }
    var profileScrollToTopTrigger by remember { mutableIntStateOf(0) }

    val focusManager = LocalFocusManager.current
    Scaffold(
        modifier = Modifier.pointerInput(Unit) {
            detectTapGestures(onTap = {
                focusManager.clearFocus()
            })
        },
        topBar = {
            HazeTopAppBar(
                title = {
                    Text(
                        text = "FurFlix",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                },
                hazeState = hazeState,
                actions = {
                    IconButton(onClick = onNavigateToDownloads) {
                        Icon(Icons.Default.Download, contentDescription = stringResource(R.string.nav_tab_downloads), tint = MaterialTheme.colorScheme.primary)
                    }
                    // NSFW toggle pill
                    NsfwTogglePill(
                        nsfwEnabled = nsfwEnabled,
                        onToggle = { 
                            viewModel.toggleNsfw() 
                            coroutineScope.launch {
                                try {
                                    val gs = gridStates[selectedTab]
                                    gs?.animateScrollToItem(0)
                                } catch (_: Exception) {}
                            }
                        },
                        modifier = Modifier.padding(end = 8.dp)
                    )

                    // Show titles toggle
                    IconButton(onClick = { showTitles = !showTitles }, modifier = Modifier.size(36.dp)) {
                        Icon(
                            imageVector = Icons.Default.Title,
                            contentDescription = "Toggle titles",
                            tint = if (showTitles) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Settings
                    IconButton(onClick = onNavigateToSettings, modifier = Modifier.size(36.dp)) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            val pagerState = rememberPagerState(
                initialPage = selectedTab,
                pageCount = { 4 }
            )

            val loadedTabs = remember { mutableSetOf<Int>() }

            // Sync: user swipe → update selectedTab
            LaunchedEffect(pagerState) {
                snapshotFlow { pagerState.currentPage }
                    .distinctUntilChanged()
                    .collect { page ->
                        if (page != selectedTab) {
                            viewModel.setSelectedTab(page)
                            if (page !in loadedTabs) {
                                when (page) {
                                    0 -> viewModel.loadBrowse()
                                    1 -> viewModel.loadLatest()
                                    2 -> {}
                                    3 -> {}
                                }
                                loadedTabs.add(page)
                            }
                        }
                    }
            }

            // Sync: selectedTab change → scroll pager
            LaunchedEffect(selectedTab) {
                if (selectedTab != pagerState.currentPage) {
                    pagerState.scrollToPage(selectedTab)
                }
            }

            // Pull-to-refresh
            var isRefreshing by remember { mutableStateOf(false) }
            var pullProgress by remember { mutableFloatStateOf(0f) }
            val pullThreshold = 330f
            var hasTriggered by remember { mutableStateOf(false) }

            LaunchedEffect(isLoading, isRefreshing) {
                if (!isLoading && isRefreshing) {
                    isRefreshing = false
                    pullProgress = 0f
                    hasTriggered = false
                }
            }

            val nestedScrollConnection = remember {
                object : NestedScrollConnection {
                    override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset = Offset.Zero

                    override fun onPostScroll(
                        consumed: Offset,
                        available: Offset,
                        source: NestedScrollSource
                    ): Offset {
                        if (available.y > 0 && source == NestedScrollSource.UserInput && !isRefreshing && pagerState.currentPage != 3) {
                            pullProgress = (pullProgress + available.y).coerceIn(0f, pullThreshold * 1.5f)
                            if (pullProgress >= pullThreshold && !hasTriggered) {
                                hasTriggered = true
                                isRefreshing = true
                                when (pagerState.currentPage) {
                                    0 -> viewModel.loadBrowse()
                                    1 -> viewModel.loadLatest()
                                    2 -> viewModel.loadSearch(currentSearchQuery)
                                    3 -> {}
                                }
                            }
                            return Offset(0f, available.y)
                        }
                        return Offset.Zero
                    }

                    override suspend fun onPreFling(available: Velocity): Velocity {
                        if (pullProgress > 0 && !isRefreshing) {
                            pullProgress = 0f
                        }
                        return Velocity.Zero
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .nestedScroll(nestedScrollConnection)
            ) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize().hazeSource(state = hazeState),
                    beyondViewportPageCount = 1
            ) { page ->
                when (page) {
                    0, 1 -> {
                        val gridPadding = if (isGrid) 6.dp else 12.dp
                        val itemSpacing = if (isGrid) 4.dp else 12.dp
                        val compact = isGrid
                        val bottomBarClearance = 120.dp
                        val gridState = rememberLazyGridState()
                        SideEffect { gridStates[page] = gridState }
                        DisposableEffect(Unit) { onDispose { gridStates.keys.remove(page) } }
                        val submissions = if (page == 0) browseSubmissions else latestSubmissions

                        if (page == 1 && !isLoggedIn) {
                            LoginContent(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(bottom = bottomBarClearance),
                                onLoginSuccess = { viewModel.loadLatest() },
                                viewModel = viewModel
                            )
                        } else if (isLoading && submissions.isEmpty()) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                                    if (!isLoggedIn) {
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Text(
                                            text = stringResource(R.string.home_not_logged_in),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        } else if (errorMessage != null && submissions.isEmpty()) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        Icons.Default.ErrorOutline,
                                        contentDescription = null,
                                        modifier = Modifier.size(64.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        text = errorMessage ?: "",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(horizontal = 32.dp)
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Button(
                                        onClick = { viewModel.clearError() },
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                    ) {
                                        Text("OK")
                                    }
                                }
                            }
                        } else {
                            LazyVerticalGrid(
                                modifier = Modifier.fillMaxSize(),
                                columns = GridCells.Fixed(if (isGrid) 2 else 1),
                                state = gridState,
                                contentPadding = PaddingValues(
                                    start = gridPadding,
                                    end = gridPadding,
                                    top = gridPadding,
                                    bottom = gridPadding + bottomBarClearance
                                ),
                                horizontalArrangement = Arrangement.spacedBy(itemSpacing),
                                verticalArrangement = Arrangement.spacedBy(itemSpacing)
                            ) {
                                items(
                                    items = submissions,
                                    key = { it.id }
                                ) { submission ->
                                    SubmissionCard(
                                        submission = submission,
                                        onClick = { onNavigateToPost(submission.id) },
                                        showTitle = showTitles,
                                        compact = compact,
                                        onAuthorClick = { onNavigateToUser(it) }
                                    )
                                }

                                if (isLoadingMore) {
                                    item {
                                        LoadingMoreFooter()
                                    }
                                }
                            }

                            LaunchedEffect(gridState) {
                                snapshotFlow {
                                    val lastVisibleItem = gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                                    val totalItems = gridState.layoutInfo.totalItemsCount
                                    lastVisibleItem >= totalItems - (if (isGrid) 6 else 3)
                                }.collect { shouldLoadMore ->
                                    if (shouldLoadMore && !isLoadingMore && submissions.isNotEmpty()) {
                                        viewModel.loadMore()
                                    }
                                }
                            }
                        }
                    }
                    2 -> {
                        val searchGridState = rememberLazyGridState()
                        var isHeaderVisible by remember { mutableStateOf(true) }
                        
                        LaunchedEffect(searchGridState) {
                            var prevOffset = searchGridState.firstVisibleItemScrollOffset
                            var prevIndex = searchGridState.firstVisibleItemIndex
                            snapshotFlow { searchGridState.firstVisibleItemScrollOffset to searchGridState.firstVisibleItemIndex }
                                .collect { (offset, index) ->
                                    if (index == 0 && offset < 50) {
                                        isHeaderVisible = true
                                    } else {
                                        val dy = if (index == prevIndex) offset - prevOffset else if (index > prevIndex) 100 else -100
                                        if (dy > 15) isHeaderVisible = false
                                        else if (dy < -15) isHeaderVisible = true
                                    }
                                    prevOffset = offset
                                    prevIndex = index
                                }
                        }

                        var showFilters by remember { mutableStateOf(false) }
                        val searchFilters by viewModel.searchFilters.collectAsState()
                        var draftFilters by remember(searchFilters) { mutableStateOf(searchFilters) }
                        val searchQuery = viewModel.searchQuery.collectAsState().value

                        // Scroll to top whenever a new search query is fired
                        LaunchedEffect(searchQuery) {
                            if (searchQuery.isNotEmpty()) {
                                searchGridState.scrollToItem(0)
                            }
                        }
                        
                        androidx.activity.compose.BackHandler(enabled = (pagerState.currentPage == 2 && hasSearched)) {
                            if (showFilters) {
                                showFilters = false
                            } else {
                                viewModel.resetSearch()
                            }
                        }

                        
                        Column(modifier = Modifier.fillMaxSize()) {
                        
                        val tagSuggestions by viewModel.tagSuggestions.collectAsState()

                        androidx.compose.animation.AnimatedVisibility(
                            visible = isHeaderVisible || showFilters || !hasSearched,
                            enter = expandVertically() + fadeIn(),
                            exit = shrinkVertically() + fadeOut()
                        ) {
                            Column {
                                SearchBar(
                                    query = searchQuery,
                                    tagSuggestions = tagSuggestions,
                                    onTypingWordChanged = { viewModel.fetchTagSuggestions(it) },
                                    onSearch = { query -> 
                                        viewModel.updateSearchFilters(draftFilters)
                                        viewModel.loadSearch(query)
                                        showFilters = false
                                    },
                                    onFilterClick = { 
                                        if (showFilters) {
                                            viewModel.updateSearchFilters(draftFilters)
                                            showFilters = false
                                            if (searchQuery.isNotEmpty()) {
                                                viewModel.loadSearch(searchQuery)
                                            }
                                        } else {
                                            showFilters = true 
                                        }
                                    },
                                    activeFilterCount = draftFilters.advancedFilterCount,
                                    isFilterOpen = showFilters
                                )
                                
                                InlineFiltersRow(
                                    filters = draftFilters,
                                    onFilterChange = { newFilters -> 
                                        draftFilters = newFilters
                                        viewModel.updateSearchFilters(newFilters)
                                    }
                                )
                            }
                        }
                        
                        androidx.compose.animation.AnimatedVisibility(
                            visible = showFilters,
                            enter = expandVertically() + fadeIn(),
                            exit = shrinkVertically() + fadeOut()
                        ) {
                            InlineSearchFilters(
                                filters = draftFilters,
                                onFiltersChanged = { draftFilters = it },
                                onClear = { draftFilters = com.furflix.app.data.model.SearchFilters() }
                            )
                        }

                        if (!hasSearched) {
                            if (recentSearches.isNotEmpty()) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f, fill = false)
                                        .verticalScroll(rememberScrollState())
                                        .padding(horizontal = 20.dp, vertical = 12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = stringResource(R.string.search_recent),
                                            style = MaterialTheme.typography.labelLarge,
                                            color = SubtleGray
                                        )
                                        TextButton(onClick = { viewModel.clearRecentSearches() }) {
                                            Text(stringResource(R.string.search_clear_recent), color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    @OptIn(ExperimentalLayoutApi::class)
                                    androidx.compose.foundation.layout.FlowRow(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        recentSearches.forEach { query ->
                                            val displayQuery = query.replace("@keywords ", "#")

                                            Surface(
                                                shape = RoundedCornerShape(16.dp),
                                                color = DarkSurfaceVariant,
                                                onClick = { viewModel.loadSearch(query) }
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    // Removed Icon here
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text(
                                                        text = displayQuery,
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = DarkOnSurface
                                                    )
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    IconButton(
                                                        onClick = { viewModel.removeRecentSearch(query) },
                                                        modifier = Modifier.size(16.dp)
                                                    ) {
                                                        Icon(
                                                            Icons.Default.Close,
                                                            contentDescription = "Remove",
                                                            modifier = Modifier.size(14.dp),
                                                            tint = SubtleGray
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            } else {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(
                                            Icons.Default.Search,
                                            contentDescription = null,
                                            modifier = Modifier.size(64.dp),
                                            tint = DividerDark
                                        )
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Text(
                                            text = stringResource(R.string.search_furaffinity),
                                            style = MaterialTheme.typography.titleMedium,
                                            color = SubtleGray
                                        )
                                    }
                                }
                            }
                        } else if (isLoading && searchSubmissions.isEmpty()) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                                }
                            }
                        } else if (errorMessage != null && searchSubmissions.isEmpty()) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        Icons.Default.ErrorOutline,
                                        contentDescription = null,
                                        modifier = Modifier.size(64.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        text = errorMessage ?: "",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(horizontal = 32.dp)
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Button(
                                        onClick = { viewModel.clearError() },
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                    ) {
                                        Text("OK")
                                    }
                                }
                            }
                        } else if (searchSubmissions.isEmpty()) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        Icons.Default.SearchOff,
                                        contentDescription = null,
                                        modifier = Modifier.size(64.dp),
                                        tint = DividerDark
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        text = "No results found",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = SubtleGray
                                    )
                                }
                            }
                        } else {
                            SideEffect { gridStates[2] = searchGridState }
                            DisposableEffect(Unit) { onDispose { gridStates.keys.remove(2) } }
                            LazyVerticalGrid(
                                modifier = Modifier.fillMaxSize(),
                                columns = GridCells.Fixed(if (isGrid) 2 else 1),
                                state = searchGridState,
                                contentPadding = PaddingValues(
                                    start = if (isGrid) 6.dp else 12.dp,
                                    end = if (isGrid) 6.dp else 12.dp,
                                    top = if (isGrid) 6.dp else 12.dp,
                                    bottom = (if (isGrid) 6.dp else 12.dp) + 120.dp
                                ),
                                horizontalArrangement = Arrangement.spacedBy(if (isGrid) 4.dp else 12.dp),
                                verticalArrangement = Arrangement.spacedBy(if (isGrid) 4.dp else 12.dp)
                            ) {
                                items(
                                    items = searchSubmissions,
                                    key = { it.id }
                                ) { submission ->
                                    SubmissionCard(
                                        submission = submission,
                                        onClick = { onNavigateToPost(submission.id) },
                                        showTitle = showTitles,
                                        compact = isGrid,
                                        onAuthorClick = { onNavigateToUser(it) }
                                    )
                                }
                                if (isLoadingMore) {
                                    item { LoadingMoreFooter() }
                                }
                            }
                            LaunchedEffect(searchGridState) {
                                snapshotFlow {
                                    val lastVisibleItem = searchGridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                                    val totalItems = searchGridState.layoutInfo.totalItemsCount
                                    lastVisibleItem >= totalItems - (if (isGrid) 6 else 3)
                                }.collect { shouldLoadMore ->
                                    if (shouldLoadMore && !isLoadingMore && searchSubmissions.isNotEmpty()) {
                                        viewModel.loadMore()
                                    }
                                }
                            }
                        }
                        }
                    }
                    3 -> {
                        LaunchedEffect(isLoggedIn) {
                            watchlistViewModel.refreshIfNeeded()
                        }
                        ProfileScreen(
                            isGrid = isGrid,
                            onNavigateToUser = onNavigateToUser,
                            onNavigateToPost = onNavigateToPost,
                            onNavigateToFavoritesPost = onNavigateToFavoritesPost,
                            scrollToTopTrigger = profileScrollToTopTrigger,
                            viewModel = watchlistViewModel
                        )
                    }
                }
            }
            } // close Box(nestedScroll)

            // Pull indicator overlay — drawn on top of everything
            if (pullProgress > 0f || isRefreshing) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 12.dp)
                        .size(56.dp)
                        .clip(RoundedCornerShape(28.dp))
                        .background(Color.Black.copy(alpha = 0.6f)),
                    contentAlignment = Alignment.Center
                ) {
                    PawPullIndicator(
                        isRefreshing = isRefreshing,
                        pullProgress = pullProgress / pullThreshold
                    )
                }
            }

            // Floating panel sits on top of the content (true overlay, not a Scaffold slot).
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
                    onTabSelected = { index ->
                        if (selectedTab == index) {
                            when (index) {
                                2 -> {
                                    coroutineScope.launch {
                                        val gs: LazyGridState? = gridStates[2]
                                        gs?.animateScrollToItem(0)
                                    }
                                }
                                3 -> profileScrollToTopTrigger++
                                else -> {
                                    coroutineScope.launch {
                                        val gs: LazyGridState? = gridStates[index]
                                        gs?.animateScrollToItem(0)
                                    }
                                }
                            }
                        } else {
                            viewModel.setSelectedTab(index)
                            if (index !in loadedTabs) {
                                when (index) {
                                    0 -> viewModel.loadBrowse()
                                    1 -> viewModel.loadLatest()
                                    2 -> {}
                                    3 -> {}
                                }
                                loadedTabs.add(index)
                            }
                        }
                    }
                )
            }
        }
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
fun SearchBar(
    query: String = "",
    onSearch: (String) -> Unit,
    onFilterClick: () -> Unit = {},
    activeFilterCount: Int = 0,
    isFilterOpen: Boolean = false,
    tagSuggestions: List<String> = emptyList(),
    onTypingWordChanged: (String) -> Unit = {}
) {
    var tags by remember(query) { 
        mutableStateOf(
            Regex("@keywords\\s+(\\S+)").findAll(query).map { it.groupValues[1] }.toList()
        )
    }
    var text by remember(query) { 
        mutableStateOf(
            "\u200B" + query.replace(Regex("@keywords\\s+\\S+"), "").trim().replace(Regex("\\s+"), " ")
        )
    }
    val keyboardController = LocalSoftwareKeyboardController.current
    val scrollState = rememberScrollState()
    val focusRequester = remember { FocusRequester() }
    
    androidx.compose.runtime.LaunchedEffect(text, tags.size) {
        scrollState.animateScrollTo(scrollState.maxValue)
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 8.dp),
            shape = RoundedCornerShape(16.dp),
            color = CardBackground,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
            shadowElevation = 8.dp,
            tonalElevation = 4.dp
        ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            
            Row(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 8.dp)
                    .horizontalScroll(scrollState)
                    .clickable(
                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                        indication = null
                    ) {
                        focusRequester.requestFocus()
                        keyboardController?.show()
                    },
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                tags.forEach { tag ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primary,
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(tag, color = DarkSurface, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Remove",
                                modifier = Modifier
                                    .size(14.dp)
                                    .clickable { tags = tags - tag },
                                tint = DarkSurface
                            )
                        }
                    }
                }
                
                androidx.compose.foundation.text.BasicTextField(
                    value = text,
                    onValueChange = { newText ->
                        if (!newText.startsWith("\u200B")) {
                            // Backspace was pressed on the empty field
                            if (tags.isNotEmpty()) {
                                tags = tags.dropLast(1)
                            }
                            text = "\u200B"
                            return@BasicTextField
                        }
                        
                        if (newText.endsWith(" ")) {
                            val words = newText.removePrefix("\u200B").split(" ")
                            val lastWord = words.dropLast(1).lastOrNull()
                            if (lastWord != null && lastWord.startsWith("#") && lastWord.length > 1) {
                                val newTag = lastWord.removePrefix("#")
                                if (newTag !in tags) {
                                    tags = tags + newTag
                                }
                                text = "\u200B" + newText.removePrefix("\u200B").substring(0, newText.removePrefix("\u200B").lastIndexOf(lastWord))
                                return@BasicTextField
                            }
                        }
                        text = newText
                        
                        val words = newText.removePrefix("\u200B").split(" ")
                        val currentWord = words.lastOrNull() ?: ""
                        if (currentWord.startsWith("#")) {
                            onTypingWordChanged(currentWord.removePrefix("#"))
                        } else {
                            onTypingWordChanged(currentWord)
                        }
                    },
                    modifier = Modifier
                        .focusRequester(focusRequester)
                        .defaultMinSize(minWidth = 60.dp)
                        .padding(vertical = 4.dp),
                    textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(
                        onSearch = {
                            var finalText = text.removePrefix("\u200B").trim()
                            val words = finalText.split(" ")
                            val lastWord = words.lastOrNull()
                            if (lastWord != null && lastWord.startsWith("#") && lastWord.length > 1) {
                                val newTag = lastWord.removePrefix("#")
                                if (newTag !in tags) {
                                    tags = tags + newTag
                                }
                                finalText = finalText.substring(0, finalText.lastIndexOf(lastWord)).trim()
                                text = "\u200B" + finalText
                            }
                            
                            val finalQuery = buildString {
                                append(finalText)
                                if (tags.isNotEmpty()) {
                                    if (isNotEmpty()) append(" ")
                                    append(tags.joinToString(" ") { "@keywords $it" })
                                }
                            }
                            if (finalQuery.isNotBlank()) {
                                onSearch(finalQuery.trim())
                                keyboardController?.hide()
                            }
                        }
                    ),
                    cursorBrush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.primary),
                    decorationBox = { innerTextField ->
                        if (text == "\u200B" && tags.isEmpty()) {
                            Text(stringResource(R.string.search_hint), color = MutedText, style = MaterialTheme.typography.bodyMedium)
                        }
                        innerTextField()
                    }
                )
                
                DropdownMenu(
                    expanded = tagSuggestions.isNotEmpty(),
                    onDismissRequest = { onTypingWordChanged("") },
                    properties = androidx.compose.ui.window.PopupProperties(focusable = false),
                    modifier = Modifier.background(DarkSurface).heightIn(max = 250.dp)
                ) {
                    tagSuggestions.forEach { suggestion ->
                        DropdownMenuItem(
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            text = { 
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.primary,
                                ) {
                                    Text(
                                        "#$suggestion", 
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        color = DarkSurface, 
                                        style = MaterialTheme.typography.bodyMedium, 
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            },
                            onClick = {
                                if (suggestion !in tags) {
                                    tags = tags + suggestion
                                }
                                val words = text.removePrefix("\u200B").split(" ").toMutableList()
                                if (words.isNotEmpty()) {
                                    words.removeAt(words.lastIndex)
                                }
                                text = "\u200B" + words.joinToString(" ") + (if (words.isNotEmpty()) " " else "")
                                onTypingWordChanged("")
                            }
                        )
                    }
                }
            }


            
            Box {
                IconButton(onClick = onFilterClick, modifier = Modifier.size(36.dp)) {
                    Icon(
                        if (isFilterOpen) Icons.Default.KeyboardArrowUp else Icons.Default.FilterList,
                        contentDescription = "Filters",
                        tint = if (activeFilterCount > 0 || isFilterOpen) MaterialTheme.colorScheme.primary else MutedText
                    )
                }
                if (activeFilterCount > 0) {
                    Surface(
                        modifier = Modifier
                            .size(18.dp)
                            .align(Alignment.TopEnd)
                            .offset(x = 2.dp, y = (-2).dp),
                        shape = RoundedCornerShape(9.dp),
                        color = MaterialTheme.colorScheme.primary
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = activeFilterCount.toString(),
                                style = MaterialTheme.typography.labelSmall,
                                color = DarkSurface,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }


}
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InlineFiltersRow(
    filters: com.furflix.app.data.model.SearchFilters,
    onFilterChange: (com.furflix.app.data.model.SearchFilters) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        InlineDropdown(
            label = FA_CATEGORIES.firstOrNull { it.first == filters.category }?.second ?: "All",
            isDefault = filters.category == 0,
            options = FA_CATEGORIES,
            onSelect = { onFilterChange(filters.copy(category = it)) },
            modifier = Modifier.weight(1f)
        )
        InlineDropdown(
            label = FA_ART_TYPES.firstOrNull { it.first == filters.artType }?.second ?: "All",
            isDefault = filters.artType == 0,
            options = FA_ART_TYPES,
            onSelect = { onFilterChange(filters.copy(artType = it)) },
            modifier = Modifier.weight(1f)
        )
        InlineGenderDropdown(
            label = FA_GENDER_OPTIONS.firstOrNull { it.first == filters.gender }?.second ?: "Any",
            isDefault = filters.gender.isEmpty(),
            options = FA_GENDER_OPTIONS,
            onSelect = { onFilterChange(filters.copy(gender = it)) },
            modifier = Modifier.weight(1f)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun InlineDropdown(
    label: String,
    isDefault: Boolean,
    options: List<Pair<Int, String>>,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        Surface(
            onClick = { expanded = true },
            shape = RoundedCornerShape(8.dp),
            color = DarkSurfaceVariant,
            border = BorderStroke(
                1.dp,
                if (isDefault) BorderDark else MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isDefault) MutedText else MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                )
                Spacer(modifier = Modifier.width(2.dp))
                Icon(
                    Icons.Default.ArrowDropDown,
                    contentDescription = null,
                    tint = if (isDefault) MutedText else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            containerColor = DarkSurfaceVariant,
            border = BorderStroke(1.dp, BorderDark)
        ) {
            options.forEach { (value, name) ->
                DropdownMenuItem(
                    text = {
                        Text(
                            name,
                            color = if (value == 0 && options[0].first == 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    },
                    onClick = { onSelect(value); expanded = false }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun InlineGenderDropdown(
    label: String,
    isDefault: Boolean,
    options: List<Pair<String, String>>,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        Surface(
            onClick = { expanded = true },
            shape = RoundedCornerShape(8.dp),
            color = DarkSurfaceVariant,
            border = BorderStroke(
                1.dp,
                if (isDefault) BorderDark else MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isDefault) MutedText else MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                )
                Spacer(modifier = Modifier.width(2.dp))
                Icon(
                    Icons.Default.ArrowDropDown,
                    contentDescription = null,
                    tint = if (isDefault) MutedText else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            containerColor = DarkSurfaceVariant,
            border = BorderStroke(1.dp, BorderDark)
        ) {
            options.forEach { (value, name) ->
                DropdownMenuItem(
                    text = {
                        Text(
                            name,
                            color = if (value.isEmpty()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    },
                    onClick = { onSelect(value); expanded = false }
                )
            }
        }
    }
}

@Composable
fun FilterDropdownRow(
    title: String,
    currentValueLabel: String,
    options: List<Pair<String, String>>,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = true }
                .padding(vertical = 12.dp, horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(currentValueLabel, style = MaterialTheme.typography.bodyMedium, color = MutedText)
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = MutedText,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            containerColor = DarkSurfaceVariant,
            border = BorderStroke(1.dp, BorderDark),
            modifier = Modifier.fillMaxWidth(0.6f)
        ) {
            options.forEach { (value, label) ->
                DropdownMenuItem(
                    text = {
                        Text(
                            label,
                            color = if (label == currentValueLabel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    },
                    onClick = {
                        onSelect(value)
                        expanded = false
                    }
                )
            }
        }
        HorizontalDivider(color = BorderDark, thickness = 1.dp)
    }
}

@Composable
fun ExpandableFilterRow(
    title: String,
    content: @Composable () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Column(modifier = Modifier.animateContentSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(vertical = 12.dp, horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
            Icon(
                imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                tint = MutedText,
                modifier = Modifier.size(18.dp)
            )
        }
        if (expanded) {
            Box(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp, start = 4.dp, end = 4.dp)) {
                content()
            }
        }
        HorizontalDivider(color = BorderDark, thickness = 1.dp)
    }
}

@Composable
fun InlineSearchFilters(
    filters: com.furflix.app.data.model.SearchFilters,
    onFiltersChanged: (com.furflix.app.data.model.SearchFilters) -> Unit,
    onClear: () -> Unit
) {

    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp).padding(bottom = 8.dp),
        shape = RoundedCornerShape(16.dp),
        color = CardBackground,
        border = BorderStroke(1.dp, BorderDark)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 550.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(R.string.search_filters),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                TextButton(onClick = onClear, modifier = Modifier.height(24.dp), contentPadding = PaddingValues(0.dp)) {
                    Text(stringResource(R.string.search_clear_all), color = SubtleGray, style = MaterialTheme.typography.labelMedium)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            // 1. Rating
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                FAFilterChip(modifier = Modifier.padding(horizontal = 4.dp), label = stringResource(R.string.filter_general), selected = filters.ratingGeneral) { onFiltersChanged(filters.copy(ratingGeneral = !filters.ratingGeneral)) }
                FAFilterChip(modifier = Modifier.padding(horizontal = 4.dp), label = stringResource(R.string.filter_mature), selected = filters.ratingMature) { onFiltersChanged(filters.copy(ratingMature = !filters.ratingMature)) }
                FAFilterChip(modifier = Modifier.padding(horizontal = 4.dp), label = stringResource(R.string.filter_adult), selected = filters.ratingAdult) { onFiltersChanged(filters.copy(ratingAdult = !filters.ratingAdult)) }
            }
            Spacer(modifier = Modifier.height(16.dp))

            // 2. Match Mode
            val modes = listOf("all" to stringResource(R.string.filter_require_all), "any" to stringResource(R.string.filter_require_any), "extended" to stringResource(R.string.filter_advanced))
            FilterDropdownRow(
                title = stringResource(R.string.filter_match_mode),
                currentValueLabel = modes.firstOrNull { it.first == filters.mode }?.second ?: stringResource(R.string.filter_advanced),
                options = modes,
                onSelect = { onFiltersChanged(filters.copy(mode = it)) }
            )

            // 3. Sort By
            val sortOptions = listOf("date" to stringResource(R.string.filter_sort_date), "relevancy" to stringResource(R.string.filter_sort_relevancy), "popularity" to stringResource(R.string.filter_sort_popularity))
            FilterDropdownRow(
                title = stringResource(R.string.filter_order_by),
                currentValueLabel = sortOptions.firstOrNull { it.first == filters.orderBy }?.second ?: stringResource(R.string.filter_sort_date),
                options = sortOptions,
                onSelect = { onFiltersChanged(filters.copy(orderBy = it)) }
            )

            // 4. Order Direction
            val orderDirs = listOf("desc" to stringResource(R.string.filter_dir_desc), "asc" to stringResource(R.string.filter_dir_asc))
            FilterDropdownRow(
                title = stringResource(R.string.filter_sort_direction),
                currentValueLabel = orderDirs.firstOrNull { it.first == filters.orderDirection }?.second ?: stringResource(R.string.filter_dir_desc),
                options = orderDirs,
                onSelect = { onFiltersChanged(filters.copy(orderDirection = it)) }
            )

            // 5. Time Range
            val timeRanges = listOf(
                "1day" to stringResource(R.string.filter_time_1d),
                "3days" to stringResource(R.string.filter_time_3d),
                "1week" to stringResource(R.string.filter_time_1w),
                "1month" to stringResource(R.string.filter_time_1m),
                "1year" to stringResource(R.string.filter_time_1y),
                "3years" to stringResource(R.string.filter_time_3y),
                "5years" to stringResource(R.string.filter_time_5y),
                "all" to stringResource(R.string.filter_time_all)
            )
            FilterDropdownRow(
                title = stringResource(R.string.filter_time_range),
                currentValueLabel = timeRanges.firstOrNull { it.first == filters.range }?.second ?: stringResource(R.string.filter_time_all),
                options = timeRanges,
                onSelect = { onFiltersChanged(filters.copy(range = it)) }
            )

            // 6. Content Type
            ExpandableFilterRow(title = stringResource(R.string.filter_content_type)) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    FAFilterChip(label = stringResource(R.string.filter_type_art), selected = filters.typeArt) { onFiltersChanged(filters.copy(typeArt = !filters.typeArt)) }
                    FAFilterChip(label = stringResource(R.string.filter_type_flash), selected = filters.typeFlash) { onFiltersChanged(filters.copy(typeFlash = !filters.typeFlash)) }
                    FAFilterChip(label = stringResource(R.string.filter_type_photo), selected = filters.typePhoto) { onFiltersChanged(filters.copy(typePhoto = !filters.typePhoto)) }
                    FAFilterChip(label = stringResource(R.string.filter_type_music), selected = filters.typeMusic) { onFiltersChanged(filters.copy(typeMusic = !filters.typeMusic)) }
                    FAFilterChip(label = stringResource(R.string.filter_type_story), selected = filters.typeStory) { onFiltersChanged(filters.copy(typeStory = !filters.typeStory)) }
                    FAFilterChip(label = stringResource(R.string.filter_type_poetry), selected = filters.typePoetry) { onFiltersChanged(filters.copy(typePoetry = !filters.typePoetry)) }
                }
            }
        }
    }
}

@Composable
private fun FAFilterChip(
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        modifier = modifier,
        label = { Text(label) },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primary,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
            containerColor = Color.White.copy(alpha = 0.05f),
            labelColor = MutedText
        ),
        border = FilterChipDefaults.filterChipBorder(
            borderColor = Color.White.copy(alpha = 0.06f),
            selectedBorderColor = MaterialTheme.colorScheme.primary,
            enabled = true,
            selected = selected
        )
    )
}

@Composable
fun NsfwTogglePill(
    nsfwEnabled: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (nsfwEnabled) Color(0xFFFF007F).copy(alpha = 0.15f) else Rating_General.copy(alpha = 0.15f),
        label = "bg_color"
    )
    val contentColor by animateColorAsState(
        targetValue = if (nsfwEnabled) Color(0xFFFF007F) else Rating_General,
        label = "content_color"
    )
    val text = if (nsfwEnabled) "NSFW" else "SFW"
    val icon = if (nsfwEnabled) Icons.Default.LocalFireDepartment else Icons.Default.Shield

    Surface(
        color = backgroundColor,
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
            .height(32.dp)
            .clickable(onClick = onToggle)
            .border(
                1.dp,
                contentColor.copy(alpha = 0.3f),
                RoundedCornerShape(16.dp)
            )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 10.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            AnimatedContent(
                targetState = text,
                transitionSpec = {
                    fadeIn(animationSpec = tween(150)) togetherWith fadeOut(animationSpec = tween(150))
                },
                label = "text_anim"
            ) { targetText ->
                Text(
                    text = targetText,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = contentColor
                )
            }
        }
    }
}

