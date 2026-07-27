package com.furflix.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.furflix.app.R
import androidx.lifecycle.viewmodel.compose.viewModel
import com.furflix.app.data.model.Submission
import com.furflix.app.ui.components.HazeTopAppBar
import com.furflix.app.ui.components.SubmissionCard
import com.furflix.app.ui.theme.SubtleGray
import com.furflix.app.viewmodel.FiltersViewModel
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FiltersScreen(
    onBack: () -> Unit,
    viewModel: FiltersViewModel = viewModel()
) {
    val mutedWords by viewModel.mutedWords.collectAsState()
    val hazeState = remember { HazeState() }
    var newWord by remember { mutableStateOf("") }

    // Dummy data for live preview
    val dummyPosts = remember {
        listOf(
            Submission(
                id = "dummy1",
                title = "Cute YCH",
                author = "FurryArtist",
                rating = "General",
                thumbnailUrl = "https://placehold.co/400x400/png?text=YCH"
            ),
            Submission(
                id = "dummy2",
                title = "Fox Adopt",
                author = "Spammer",
                rating = "General",
                thumbnailUrl = "https://placehold.co/400x400/png?text=Adopt"
            ),
            Submission(
                id = "dummy3",
                title = "Wolf Commission",
                author = "CoolWolf",
                rating = "General",
                thumbnailUrl = "https://placehold.co/400x400/png?text=Wolf"
            )
        )
    }

    Scaffold(
        topBar = {
            HazeTopAppBar(
                title = { Text(stringResource(R.string.settings_filters_content)) },
                hazeState = hazeState,
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .hazeSource(state = hazeState)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {

            // Live Preview Section
            Column {
                Text(
                    text = stringResource(R.string.settings_filters_live_preview),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.settings_filters_live_preview_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    dummyPosts.forEach { post ->
                        val isFiltered = mutedWords.any { post.title.lowercase().contains(it) }

                        AnimatedVisibility(
                            visible = !isFiltered,
                            enter = fadeIn() + expandHorizontally(),
                            exit = fadeOut(animationSpec = tween(400)) + shrinkHorizontally(animationSpec = tween(400)),
                            modifier = Modifier.weight(1f)
                        ) {
                            SubmissionCard(
                                submission = post,
                                onClick = {},
                                showTitle = true,
                                compact = true,
                                onAuthorClick = {}
                            )
                        }
                    }
                    
                    // Show a placeholder if all are filtered
                    val allFiltered = dummyPosts.all { post ->
                        mutedWords.any { post.title.lowercase().contains(it) }
                    }
                    AnimatedVisibility(visible = allFiltered) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.Visibility, contentDescription = null, tint = SubtleGray)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(stringResource(R.string.settings_filters_all_hidden), color = SubtleGray, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

            // Muted Words
            Column {
                Text(
                    text = stringResource(R.string.settings_filters_muted_words),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.settings_filters_muted_words_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
                
                OutlinedTextField(
                    value = newWord,
                    onValueChange = { newWord = it },
                    placeholder = { Text(stringResource(R.string.settings_filters_muted_words_placeholder)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    trailingIcon = {
                        IconButton(onClick = {
                            if (newWord.isNotBlank()) {
                                viewModel.addMutedWord(newWord)
                                newWord = ""
                            }
                        }) {
                            Icon(Icons.Default.Add, contentDescription = "Add Word", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    mutedWords.forEach { word ->
                        FilterChip(
                            label = word,
                            onRemove = { viewModel.removeMutedWord(word) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterChip(label: String, onRemove: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
        contentColor = MaterialTheme.colorScheme.primary,
        modifier = Modifier.clickable(onClick = onRemove)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = label, style = MaterialTheme.typography.labelLarge)
            Spacer(modifier = Modifier.width(6.dp))
            Icon(Icons.Default.Close, contentDescription = "Remove", modifier = Modifier.size(14.dp))
        }
    }
}
