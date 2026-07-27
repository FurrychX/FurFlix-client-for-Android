package com.furflix.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.furflix.app.data.repository.FurRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class FiltersViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = FurRepository.getInstance(application)

    private val _mutedWords = MutableStateFlow<List<String>>(emptyList())
    val mutedWords: StateFlow<List<String>> = _mutedWords.asStateFlow()

    init {
        viewModelScope.launch {
            repository.mutedWordsFlow.collect {
                _mutedWords.value = it
            }
        }
    }

    fun addMutedWord(word: String) {
        val trimmed = word.trim().lowercase()
        if (trimmed.isNotEmpty() && !_mutedWords.value.contains(trimmed)) {
            val newList = _mutedWords.value + trimmed
            viewModelScope.launch {
                repository.saveMutedWords(newList)
            }
        }
    }

    fun removeMutedWord(word: String) {
        val newList = _mutedWords.value - word
        viewModelScope.launch {
            repository.saveMutedWords(newList)
        }
    }
}
