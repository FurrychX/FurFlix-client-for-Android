package com.furflix.app.data.model

import kotlinx.serialization.Serializable

@Serializable
data class Submission(
    val id: String = "",
    val title: String = "",
    val author: String = "",
    val authorUsername: String = "",
    val authorAvatar: String = "",
    val thumbnailUrl: String = "",
    val fullImageUrl: String = "",
    val description: String = "",
    val rating: String = "",
    val category: String = "",
    val species: String = "",
    val gender: String = "",
    val views: Int = 0,
    val favorites: Int = 0,
    val comments: Int = 0,
    val isFavorited: Boolean = false,
    val favUrl: String = "",
    val date: String = "",
    val tags: List<String> = emptyList(),
    val fileUrl: String = "",
    val fileName: String = "",
    val resolution: String = "",
    val fileSize: String = "",
    val fileType: String = "",
    val commentList: List<Comment> = emptyList()
)

@Serializable
data class Comment(
    val id: String = "",
    val author: String = "",
    val authorUsername: String = "",
    val authorAvatar: String = "",
    val title: String = "",
    val text: String = "",
    val date: String = "",
    val timestamp: Long = 0
)
