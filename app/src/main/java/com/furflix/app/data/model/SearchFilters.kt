package com.furflix.app.data.model

data class SearchFilters(
    val orderBy: String = "date",
    val orderDirection: String = "desc",
    val ratingGeneral: Boolean = true,
    val ratingMature: Boolean = true,
    val ratingAdult: Boolean = true,
    val typeArt: Boolean = true,
    val typeFlash: Boolean = true,
    val typePhoto: Boolean = true,
    val typeMusic: Boolean = true,
    val typeStory: Boolean = true,
    val typePoetry: Boolean = true,
    val category: Int = 0,
    val artType: Int = 0,
    val species: Int = 0,
    val gender: String = "",
    val keywords: List<String> = emptyList(),
    val range: String = "all",
    val mode: String = "extended"
) {
    fun toSearchQueryMap(): Map<String, String> {
        val map = mutableMapOf<String, String>()
        map["order-by"] = orderBy
        map["order-direction"] = orderDirection
        if (ratingGeneral) map["rating-general"] = "1"
        if (ratingMature) map["rating-mature"] = "1"
        if (ratingAdult) map["rating-adult"] = "1"
        if (typeArt) map["type-art"] = "1"
        if (typeFlash) map["type-flash"] = "1"
        if (typePhoto) map["type-photo"] = "1"
        if (typeMusic) map["type-music"] = "1"
        if (typeStory) map["type-story"] = "1"
        if (typePoetry) map["type-poetry"] = "1"
        map["range"] = range
        map["mode"] = mode
        if (category != 0) map["category"] = category.toString()
        if (artType != 0) map["arttype"] = artType.toString()
        if (species != 0) map["species"] = species.toString()
        if (gender.isNotEmpty()) map["gender"] = gender
        return map
    }

    fun toBrowseQueryMap(): Map<String, String> {
        val map = mutableMapOf<String, String>()
        if (category != 0) map["cat"] = category.toString()
        if (artType != 0) map["atype"] = artType.toString()
        if (species != 0) map["species"] = species.toString()
        if (gender.isNotEmpty()) map["gender"] = gender
        map["rating_general"] = if (ratingGeneral) "1" else "0"
        map["rating_mature"] = if (ratingMature) "1" else "0"
        map["rating_adult"] = if (ratingAdult) "1" else "0"
        return map
    }

    fun buildKeywordQuery(baseQuery: String): String {
        if (keywords.isEmpty()) return baseQuery
        val kw = if (mode == "extended") {
            keywords.joinToString(" ") { "@keywords $it" }
        } else {
            keywords.joinToString(" ")
        }
        return if (baseQuery.isBlank()) kw else "$baseQuery $kw"
    }

    val hasBrowseFilters: Boolean
        get() = category != 0 || artType != 0 || species != 0 || gender.isNotEmpty()

    val hasKeywords: Boolean
        get() = keywords.isNotEmpty()

    val advancedFilterCount: Int
        get() {
            var count = 0
            if (keywords.isNotEmpty()) count++
            if (species != 0) count++
            if (!ratingGeneral || !ratingMature || !ratingAdult) count++
            if (!typeArt || !typeFlash || !typePhoto || !typeMusic || !typeStory || !typePoetry) count++
            if (range != "all") count++
            if (mode != "extended") count++
            if (orderBy != "date") count++
            return count
        }
}
