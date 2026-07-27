package com.furflix.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.furflix.app.data.repository.FurRepository
import com.furflix.app.ui.components.LoadingMoreFooter
import com.furflix.app.ui.screens.InlineFiltersRow
import com.furflix.app.ui.screens.InlineSearchFilters
import com.furflix.app.ui.screens.SearchBar
import com.furflix.app.ui.components.SubmissionCard
import com.furflix.app.viewmodel.TagSearchViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TagSearchScreen(
    tag: String,
    viewModel: TagSearchViewModel,
    onBack: () -> Unit,
    onNavigateToPost: (String) -> Unit,
    onNavigateToUser: (String) -> Unit
) {
    val submissions by viewModel.submissions.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isLoadingMore by viewModel.isLoadingMore.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    val context = LocalContext.current
    val repository = remember { FurRepository.getInstance(context) }
    val isGrid by repository.isGridFlow.collectAsState(initial = true)

    Scaffold { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            var showFilters by remember { mutableStateOf(false) }
            val searchFilters by viewModel.searchFilters.collectAsState()
            var draftFilters by remember(searchFilters) { mutableStateOf(searchFilters) }
            val searchQuery by viewModel.searchQuery.collectAsState()
            val tagSuggestions by viewModel.tagSuggestions.collectAsState()

            Column(modifier = Modifier.fillMaxSize()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                    Box(modifier = Modifier.weight(1f)) {
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
                    }
                }

                InlineFiltersRow(
                    filters = draftFilters,
                    onFilterChange = { newFilters -> 
                        draftFilters = newFilters
                        viewModel.updateSearchFilters(newFilters)
                    }
                )

                androidx.compose.animation.AnimatedVisibility(
                    visible = showFilters,
                    enter = androidx.compose.animation.expandVertically() + androidx.compose.animation.fadeIn(),
                    exit = androidx.compose.animation.shrinkVertically() + androidx.compose.animation.fadeOut()
                ) {
                    InlineSearchFilters(
                        filters = draftFilters,
                        onFiltersChanged = { draftFilters = it },
                        onClear = { draftFilters = com.furflix.app.data.model.SearchFilters() }
                    )
                }

                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            if (isLoading && submissions.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
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
                val gridState = rememberLazyGridState()
                
                LazyVerticalGrid(
                    modifier = Modifier.fillMaxSize(),
                    columns = GridCells.Fixed(if (isGrid) 2 else 1),
                    state = gridState,
                    contentPadding = PaddingValues(
                        start = if (isGrid) 6.dp else 12.dp,
                        end = if (isGrid) 6.dp else 12.dp,
                        top = if (isGrid) 6.dp else 12.dp,
                        bottom = (if (isGrid) 6.dp else 12.dp) + 80.dp
                    ),
                    horizontalArrangement = Arrangement.spacedBy(if (isGrid) 4.dp else 12.dp),
                    verticalArrangement = Arrangement.spacedBy(if (isGrid) 4.dp else 12.dp)
                ) {
                    items(
                        items = submissions,
                        key = { it.id }
                    ) { submission ->
                        SubmissionCard(
                            submission = submission,
                            onClick = { onNavigateToPost(submission.id) },
                            showTitle = true,
                            compact = isGrid,
                            onAuthorClick = { onNavigateToUser(it) }
                        )
                    }
                    if (isLoadingMore) {
                        item { LoadingMoreFooter() }
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
        }
    }
}
}
