package com.furflix.app.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.furflix.app.data.model.Submission
import com.furflix.app.data.repository.FurRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class TagSearchViewModel(
    application: Application,
    initialQuery: String
) : AndroidViewModel(application) {

    private val repository = FurRepository.getInstance(application)

    private val _submissions = MutableStateFlow<List<Submission>>(emptyList())
    val submissions: StateFlow<List<Submission>> = _submissions.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isLoadingMore = MutableStateFlow(false)
    val isLoadingMore: StateFlow<Boolean> = _isLoadingMore.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _searchFilters = MutableStateFlow(com.furflix.app.data.model.SearchFilters())
    val searchFilters: StateFlow<com.furflix.app.data.model.SearchFilters> = _searchFilters.asStateFlow()

    private val _searchQuery = MutableStateFlow(initialQuery)
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _tagSuggestions = MutableStateFlow<List<String>>(emptyList())
    val tagSuggestions: StateFlow<List<String>> = _tagSuggestions.asStateFlow()

    private var tagSearchJob: kotlinx.coroutines.Job? = null

    private var currentPage = 1
    private var hasMore = true

    init {
        loadSearch()
    }

    fun loadSearch(newQuery: String = _searchQuery.value) {
        if (_isLoading.value) return
        val trimmed = newQuery.trim()
        if (trimmed.isNotEmpty()) {
            viewModelScope.launch {
                repository.addRecentSearch(trimmed)
            }
        }
        _searchQuery.value = newQuery
        currentPage = 1
        hasMore = true
        viewModelScope.launch {
            _submissions.value = emptyList()
            _isLoading.value = true
            _errorMessage.value = null
            try {
                val result = repository.search(newQuery, currentPage, _searchFilters.value)
                _submissions.value = result
                if (result.isEmpty()) {
                    _errorMessage.value = "No search results found."
                }
            } catch (e: Exception) {
                Log.e("TagSearchVM", "loadSearch error", e)
                _errorMessage.value = "Error: ${e.message}"
            }
            _isLoading.value = false
        }
    }

    fun loadMore() {
        if (!hasMore || _isLoadingMore.value || _isLoading.value) return
        viewModelScope.launch {
            _isLoadingMore.value = true
            try {
                currentPage++
                val result = repository.search(_searchQuery.value, currentPage, _searchFilters.value)
                if (result.isEmpty()) {
                    hasMore = false
                } else {
                    val currentIds = _submissions.value.map { it.id }.toSet()
                    val newItems = result.filter { it.id !in currentIds }
                    if (newItems.isEmpty()) {
                        hasMore = false
                    } else {
                        _submissions.value = _submissions.value + newItems
                    }
                }
            } catch (e: Exception) {
                Log.e("TagSearchVM", "loadMore error", e)
            }
            _isLoadingMore.value = false
        }
    }

    fun updateSearchFilters(filters: com.furflix.app.data.model.SearchFilters) {
        _searchFilters.value = filters
    }

    fun fetchTagSuggestions(prefix: String) {
        tagSearchJob?.cancel()
        if (prefix.isBlank()) {
            _tagSuggestions.value = emptyList()
            return
        }
        tagSearchJob = viewModelScope.launch {
            _tagSuggestions.value = repository.searchTags(prefix)
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }

    class Factory(
        private val application: Application,
        private val query: String
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(TagSearchViewModel::class.java)) {
                return TagSearchViewModel(application, query) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
