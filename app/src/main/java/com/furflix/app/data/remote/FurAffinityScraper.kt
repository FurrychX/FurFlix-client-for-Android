package com.furflix.app.data.remote

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import android.webkit.CookieManager
import com.furflix.app.data.model.Submission
import com.furflix.app.data.model.Comment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

object FurAffinityScraper {

    // Session cookies — keyed by name. Real FA auth cookies are named "a" and "b".
    private var sessionCookies: MutableMap<String, String> = mutableMapOf()
    private var loggedIn = false
    private var username = ""

    private val cookieManager = CookieManager.getInstance()

    private const val USER_AGENT =
        "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Mobile Safari/537.36"

    const val FA_BASE = "https://www.furaffinity.net"

    var latestNextUrl: String? = null
    var watchlistNextUrl: String? = null
    var browseNextUrl: String? = null
    var searchNextUrl: String? = null
    
    private val galleryNextUrls = mutableMapOf<String, String>()
    private val favoritesNextUrls = mutableMapOf<String, String>()

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    @SuppressLint("SetJavaScriptEnabled")
    fun init(context: Context) {
        CookieManager.getInstance().setAcceptCookie(true)
        if (sessionCookies.isNotEmpty()) {
            applySessionCookiesToCookieManager()
        }
    }

    fun isLoggedIn(): Boolean = loggedIn

    fun getUsername(): String = username

    fun setUsername(name: String) {
        username = name
    }

    /**
     * Returns the Cookie header string from session cookies.
     * Only uses cookies that are actual auth/session cookies - not consent junk.
     * @param forceNsfw if true, injects sfw=0; if false, injects sfw=1; if null, leaves it alone.
     */
    fun getCookiesString(forceNsfw: Boolean? = null): String {
        val cookies = sessionCookies.toMutableMap()
        if (forceNsfw == true) {
            cookies.remove("sfw")
        } else if (forceNsfw == false) {
            cookies["sfw"] = "1"
        }
        
        if (cookies.isNotEmpty()) {
            return cookies.entries.joinToString("; ") { "${it.key}=${it.value}" }
        }
        val c1 = cookieManager.getCookie("https://furaffinity.net") ?: ""
        val c2 = cookieManager.getCookie("https://www.furaffinity.net") ?: ""
        
        var combined = listOf(c1, c2).filter { it.isNotEmpty() }.joinToString("; ")
        if (forceNsfw == true) {
            combined = combined.replace(Regex("""sfw=\d+"""), "")
            combined = combined.replace(Regex(""";\s*;"""), ";").trim(';', ' ')
        } else if (forceNsfw == false) {
            combined = combined.replace(Regex("""sfw=\d+"""), "") + "; sfw=1"
        }
        return combined
    }

    /**
     * Called after WebView login. Parses and stores cookies.
     * Key insight from iOS: only cookie named "a" (and optionally "b") signals real login.
     * Cookies like FCCDCF, FCNEC are just CloudFlare/consent cookies — NOT auth.
     */
    fun setCookiesFromWebView(cookiesString: String) {
        sessionCookies.clear()
        cookiesString.split(";").forEach { cookie ->
            val parts = cookie.trim().split("=", limit = 2)
            if (parts.size == 2) {
                val name = parts[0].trim()
                val value = parts[1].trim()
                if (name.isNotEmpty()) {
                    sessionCookies[name] = value
                }
            }
        }
        applySessionCookiesToCookieManager()

        // Real FA session requires the "a" cookie — just like the iOS app checks:
        // guard cookies.map(\.name).contains("a") else { return nil }
        val hasAuthCookie = sessionCookies.containsKey("a")
        loggedIn = hasAuthCookie

        Log.d(
            "FurScraper",
            "Cookies set: ${sessionCookies.keys}, hasAuthCookie(a)=$hasAuthCookie, loggedIn=$loggedIn"
        )
    }

    private fun applySessionCookiesToCookieManager() {
        sessionCookies.forEach { (name, value) ->
            cookieManager.setCookie("https://furaffinity.net", "$name=$value")
            cookieManager.setCookie("https://www.furaffinity.net", "$name=$value")
        }
        cookieManager.flush()
    }

    fun clearSession() {
        sessionCookies.clear()
        loggedIn = false
        username = ""
        cookieManager.removeAllCookies(null)
        cookieManager.flush()
    }

        private suspend fun postDocument(url: String, formBody: okhttp3.FormBody, forceNsfw: Boolean? = null): org.jsoup.nodes.Document =
        withContext(Dispatchers.IO) {
            Log.d("FurScraper", "POSTing to: $url")
            val cookieHeader = getCookiesString(forceNsfw)
            val reqBuilder = Request.Builder()
                .url(url)
                .post(formBody)
                .header("User-Agent", USER_AGENT)
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8")
                .header("Accept-Language", "en-US,en;q=0.5")
                .header("Referer", "$FA_BASE/")
                .header("Connection", "keep-alive")

            if (cookieHeader.isNotEmpty()) {
                reqBuilder.header("Cookie", cookieHeader)
            }

            val response = httpClient.newCall(reqBuilder.build()).execute()
            val html = response.body?.string() ?: ""
            val finalUrl = response.request.url.toString()

            Log.d("FurScraper", "POST Status=${response.code}, finalUrl=$finalUrl, len=${html.length}")
            Jsoup.parse(html, finalUrl)
        }

    private suspend fun fetchDocument(url: String, forceNsfw: Boolean? = null): org.jsoup.nodes.Document =
        withContext(Dispatchers.IO) {
            Log.d("FurScraper", "Fetching: $url")

            val cookieHeader = getCookiesString(forceNsfw)
            val reqBuilder = Request.Builder()
                .url(url)
                .header("User-Agent", USER_AGENT)
                .header(
                    "Accept",
                    "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8"
                )
                .header("Accept-Language", "en-US,en;q=0.5")
                .header("Referer", "$FA_BASE/")
                .header("Connection", "keep-alive")

            if (cookieHeader.isNotEmpty()) {
                reqBuilder.header("Cookie", cookieHeader)
            }

            val response = httpClient.newCall(reqBuilder.build()).execute()
            val html = response.body?.string() ?: ""
            val finalUrl = response.request.url.toString()

            Log.d("FurScraper", "Status=${response.code}, finalUrl=$finalUrl, len=${html.length}")
            if (html.length < 1000) Log.d("FurScraper", "Short HTML: ${html.take(500)}")

            if (finalUrl.contains("/login")) {
                Log.w("FurScraper", "Redirected to login — session cookie 'a' missing or expired")
            }

            Jsoup.parse(html, FA_BASE)
        }

    /**
     * After successful WebView login, verify the session by fetching the home page
     * and extracting the logged-in username — same as iOS does in OnlineFASession.init().
     */
    suspend fun verifyLoginAndGetUsername(): String? = withContext(Dispatchers.IO) {
        try {
            val doc = fetchDocument("$FA_BASE/")

            // Most reliable source: the logged-in avatar link, whose href is always
            // "/user/{canonical-lowercase-username}/" — its <img> has no useful text,
            // so we must read the href, not .text(). This avoids accidentally picking up
            // a menu link whose visible text is something generic like "Logged in".
            val avatarLink = doc.selectFirst("a.loggedin_user_avatar, a:has(img.loggedin_user_avatar)")
            var name = avatarLink?.attr("href")?.trim('/')?.removePrefix("user/")?.substringBefore("/")

            if (name.isNullOrEmpty()) {
                // Fallback: nav-user-menu / navhideonmobile links, but again prefer href over text
                // since these can also wrap icons or generic labels.
                val usernameEl = doc.selectFirst(
                    ".nav-user-menu a[href^=\"/user/\"], ul.navhideonmobile a[href^=\"/user/\"]"
                )
                name = usernameEl?.attr("href")?.trim('/')?.removePrefix("user/")?.substringBefore("/")
                    ?.takeIf { it.isNotEmpty() }
                    ?: usernameEl?.text()?.trim()
            }

            if (!name.isNullOrEmpty() && name != "Logged in") {
                Log.d("FurScraper", "Verified login, username=$name")
                username = name
                loggedIn = true
            } else {
                // Try alternate selector
                val alt = doc.selectFirst("a[href^=\"/user/\"]")
                val altHref = alt?.attr("href")?.trim('/')?.removePrefix("user/")?.substringBefore("/")
                val altName = altHref?.takeIf { it.isNotEmpty() } ?: alt?.text()?.trim()
                if (!altName.isNullOrEmpty() && !altName.contains(" ") && altName.length < 40) {
                    username = altName
                    loggedIn = true
                    Log.d("FurScraper", "Verified login via alt selector, username=$altName")
                } else {
                    Log.w("FurScraper", "Could not find username on home page")
                }
            }
            username.ifEmpty { null }
        } catch (e: Exception) {
            Log.e("FurScraper", "verifyLogin error: ${e.message}", e)
            null
        }
    }

    suspend fun login(user: String, pass: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val loginClient = OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .followRedirects(true)
                .build()

            val getResp = loginClient.newCall(
                Request.Builder().url("$FA_BASE/login/").header("User-Agent", USER_AGENT).build()
            ).execute()
            val html = getResp.body?.string()
                ?: return@withContext Result.failure(Exception("Empty response"))

            val doc = Jsoup.parse(html)
            val csrfToken = doc.selectFirst("input[name=\"csrf_token\"]")?.attr("value") ?: ""
            if (csrfToken.isEmpty()) {
                return@withContext Result.failure(Exception("Could not find login form."))
            }

            val postResp = loginClient.newCall(
                Request.Builder()
                    .url("$FA_BASE/login/")
                    .post(
                        FormBody.Builder()
                            .add("action", "login")
                            .add("retard_language", "en")
                            .add("username", user)
                            .add("password", pass)
                            .add("csrf_token", csrfToken)
                            .build()
                    )
                    .header("User-Agent", USER_AGENT)
                    .header("Referer", "$FA_BASE/login/")
                    .header("Origin", FA_BASE)
                    .build()
            ).execute()

            val respHtml = postResp.body?.string() ?: ""
            val respUrl = postResp.request.url.toString()

            if (respUrl.contains("login") &&
                (respHtml.contains("Login to access") || respHtml.contains("Invalid username"))
            ) {
                val msg = when {
                    respHtml.contains("Incorrect Password") -> "Incorrect password"
                    respHtml.contains("Invalid username") -> "Invalid username"
                    else -> "Login failed. Check your credentials."
                }
                return@withContext Result.failure(Exception(msg))
            }

            loggedIn = true
            username = user
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(Exception("Login error: ${e.message}"))
        }
    }

    // ------------------------------------------------------------------------------------------
    // Content fetching — URL structure taken directly from iOS FAURLs.swift
    // ------------------------------------------------------------------------------------------

    /**
     * New submissions feed (logged-in watchlist).
     * iOS uses: https://www.furaffinity.net/msg/submissions/new@72
     */
    suspend fun getLatestPage(page: Int = 1): List<Submission> = withContext(Dispatchers.IO) {
        try {
            // For page 1: /msg/submissions/new@72
            // For page 2+: FA uses SID-based navigation, so we use latestNextUrl
            val url = if (page == 1) "$FA_BASE/msg/submissions/new@72" else latestNextUrl ?: return@withContext emptyList()
            val doc = fetchDocument(url)

            val nextButton = doc.select("a").firstOrNull { it.text().contains("Next", ignoreCase = true) && it.attr("href").contains("~") }
            latestNextUrl = nextButton?.attr("href")?.let { "$FA_BASE$it" }

            parseMessageCenterSubmissions(doc)
        } catch (e: Exception) {
            Log.e("FurScraper", "getLatestPage error: ${e.message}", e)
            emptyList()
        }
    }

    /**
     * Browse / recent art page.
     * iOS doesn't have a "browse" feed — FA's public browse is at /browse/
     * but it returns a different HTML structure. We try it and fall back to search.
     */
    suspend fun getBrowsePage(page: Int = 1, filters: com.furflix.app.data.model.SearchFilters = com.furflix.app.data.model.SearchFilters()): List<Submission> = withContext(Dispatchers.IO) {
        try {
            val browseParams = filters.toBrowseQueryMap().map { "${it.key}=${it.value}" }.joinToString("&")
            val url = "$FA_BASE/browse/?$browseParams&page=$page&perpage=72"
            
            val forceNsfw = filters.ratingAdult || filters.ratingMature
            val doc = fetchDocument(url, forceNsfw)
            parseSearchResults(doc)
        } catch (e: Exception) {
            Log.e("FurScraper", "getBrowsePage error: ${e.message}", e)
            emptyList()
        }
    }

    suspend fun search(query: String, page: Int = 1, filters: com.furflix.app.data.model.SearchFilters = com.furflix.app.data.model.SearchFilters()): List<Submission> =
        withContext(Dispatchers.IO) {
            try {
                val finalQuery = filters.buildKeywordQuery(query)
                val formBuilder = okhttp3.FormBody.Builder()
                formBuilder.add("q", finalQuery)
                formBuilder.add("page", page.toString())
                
                // Add all filters
                filters.toSearchQueryMap().forEach { (key, value) ->
                    formBuilder.add(key, value)
                }
                
                val forceNsfw = filters.ratingAdult || filters.ratingMature
                val doc = postDocument("$FA_BASE/search/", formBuilder.build(), forceNsfw)
                
                // Pagination is handled by passing the correct page number,
                // but we can check if a "Next" button exists to know if there are more results.
                val hasNext = doc.select("button[name=page]").any { it.text().contains("Next", ignoreCase = true) }
                if (!hasNext && doc.select("form[action^=/search/]").isNotEmpty()) {
                    searchNextUrl = null
                } else {
                    searchNextUrl = "has_next" // dummy value so the ViewModel knows there's more
                }
                
                parseSearchResults(doc)
            } catch (e: Exception) {
                Log.e("FurScraper", "search error: ${e.message}", e)
                emptyList()
            }
        }

    suspend fun getSubmissionDetails(submissionId: String): Submission? =
        withContext(Dispatchers.IO) {
            try {
                val doc = fetchDocument("$FA_BASE/view/$submissionId/")
                parseSubmissionDetail(doc, submissionId)
            } catch (e: Exception) {
                Log.e("FurScraper", "getSubmissionDetails error: ${e.message}", e)
                null
            }
        }

    suspend fun getWatchlistPage(page: Int = 1): List<Submission> = withContext(Dispatchers.IO) {
        try {
            val url = if (page == 1) "$FA_BASE/msg/submissions/new@72" else watchlistNextUrl ?: return@withContext emptyList()
            val doc = fetchDocument(url)

            val nextButton = doc.select("a").firstOrNull { it.text().contains("Next", ignoreCase = true) && it.attr("href").contains("~") }
            watchlistNextUrl = nextButton?.attr("href")?.let { "$FA_BASE$it" }

            parseMessageCenterSubmissions(doc)
        } catch (e: Exception) {
            Log.e("FurScraper", "getWatchlistPage error: ${e.message}", e)
            emptyList()
        }
    }

    suspend fun getUserGallery(username: String, page: Int = 1, forceNsfw: Boolean? = null): List<Submission> = withContext(Dispatchers.IO) {
        try {
            val url = if (page == 1) {
                "$FA_BASE/gallery/$username/"
            } else {
                galleryNextUrls[username] ?: return@withContext emptyList()
            }
            val doc = fetchDocument(url, forceNsfw)
            
            val nextA = doc.select("a").firstOrNull { it.text().contains("Next", ignoreCase = true) && it.attr("href").contains("/gallery/", ignoreCase = true) }
            if (nextA != null) {
                galleryNextUrls[username] = "$FA_BASE${nextA.attr("href")}"
            } else {
                val nextBtn = doc.select("form[action^=/gallery/] button, form[action^=/gallery/] input").firstOrNull { (it.tagName() == "button" && it.text().contains("Next", ignoreCase = true)) || (it.tagName() == "input" && it.attr("value").contains("Next", ignoreCase = true)) }
                val form = nextBtn?.closest("form")
                val actionUrl = form?.attr("action")
                if (!actionUrl.isNullOrEmpty()) {
                    galleryNextUrls[username] = "$FA_BASE$actionUrl"
                } else {
                    galleryNextUrls.remove(username)
                }
            }
            
            parseSearchResults(doc)
        } catch (e: Exception) {
            Log.e("FurScraper", "getUserGallery error: ${e.message}", e)
            emptyList()
        }
    }

    suspend fun getUserFavorites(username: String, page: Int = 1, forceNsfw: Boolean? = null): List<Submission> = withContext(Dispatchers.IO) {
        try {
            val url = if (page == 1) {
                "$FA_BASE/favorites/$username/"
            } else {
                // Use cached next URL if available, otherwise fall back to page number
                favoritesNextUrls[username] ?: "$FA_BASE/favorites/$username/$page/"
            }
            Log.d("FurScraper", "getUserFavorites: page=$page url=$url username=$username")
            val doc = fetchDocument(url, forceNsfw)

            // Try anchor link with href containing /favorites/
            val nextA = doc.select("a").firstOrNull {
                it.text().contains("Next", ignoreCase = true) && it.attr("href").contains("/favorites/", ignoreCase = true)
            }
            if (nextA != null) {
                val href = nextA.attr("href")
                val nextUrl = if (href.startsWith("http")) href else "$FA_BASE$href"
                favoritesNextUrls[username] = nextUrl
                Log.d("FurScraper", "getUserFavorites: next via <a>: $nextUrl")
            } else {
                // Try form button — check both form action starts with /favorites/ and absolute URLs
                val nextBtn = doc.select("form button, form input[type=submit]").firstOrNull { btn ->
                    val parentForm = btn.closest("form")
                    val action = parentForm?.attr("action") ?: ""
                    (action.contains("/favorites/", ignoreCase = true)) &&
                    ((btn.tagName() == "button" && btn.text().contains("Next", ignoreCase = true)) ||
                     (btn.tagName() == "input" && btn.attr("value").contains("Next", ignoreCase = true))) &&
                    !btn.hasAttr("disabled")
                }
                val form = nextBtn?.closest("form")
                val actionUrl = form?.attr("action")
                if (!actionUrl.isNullOrEmpty()) {
                    val nextUrl = if (actionUrl.startsWith("http")) actionUrl else "$FA_BASE$actionUrl"
                    favoritesNextUrls[username] = nextUrl
                    Log.d("FurScraper", "getUserFavorites: next via form: $nextUrl")
                } else {
                    // No "Next" button found — clear cached URL, but don't block further loads
                    // The fallback page-number URL above will still be tried on next scroll
                    Log.d("FurScraper", "getUserFavorites: no next button found, will use page number fallback")
                    favoritesNextUrls.remove(username)
                }
            }

            val results = parseSearchResults(doc)
            Log.d("FurScraper", "getUserFavorites: page=$page parsed ${results.size} items")
            results
        } catch (e: Exception) {
            Log.e("FurScraper", "getUserFavorites error: ${e.message}", e)
            emptyList()
        }
    }


    fun getImageUrl(url: String): String {
        if (url.startsWith("http")) return url
        return "$FA_BASE$url"
    }

    // ------------------------------------------------------------------------------------------
    // Parsers — ported from iOS FASubmissionsPage.swift and FASearchPage.swift
    // ------------------------------------------------------------------------------------------

    /**
     * Parses the /msg/submissions/ page (logged-in submission feed).
     * iOS query: "figure" inside "#messagecenter-submissions section"
     * Figure id format: "sid-XXXXXX" (not "id-XXXXXX"!)
     */
    private fun parseMessageCenterSubmissions(doc: org.jsoup.nodes.Document): List<Submission> {
        val figures = doc.select(
            "#messagecenter-submissions section figure, " +
                    "#messagecenter-new-submissions figure, " +
                    "div#messagecenter-submissions figure"
        )
        Log.d("FurScraper", "parseMessageCenter: found ${figures.size} figures")

        if (figures.isEmpty()) {
            // Fallback: any figure with sid- prefix
            val fallback = doc.select("figure[id^=\"sid-\"]")
            Log.d("FurScraper", "parseMessageCenter fallback: ${fallback.size} sid- figures")
            return fallback.mapNotNull { parseFigure(it, sidPrefix = "sid-") }
        }

        return figures.mapNotNull { parseFigure(it, sidPrefix = "sid-") }
    }

    /**
     * Parses search results page.
     * iOS: doc.select("section#gallery-search-results figure")
     */
    private fun parseSearchResults(doc: org.jsoup.nodes.Document): List<Submission> {
        val figures = doc.select("section#gallery-search-results figure")
        Log.d("FurScraper", "parseSearch: found ${figures.size} figures")

        if (figures.isEmpty()) {
            // Fallback to any figure with id pattern
            val fallback = doc.select("figure[id^=\"sid-\"], figure[id^=\"id-\"]")
            Log.d("FurScraper", "parseSearch fallback: ${fallback.size} figures")
            return fallback.mapNotNull {
                val prefix = if (it.attr("id").startsWith("sid-")) "sid-" else "id-"
                parseFigure(it, sidPrefix = prefix)
            }
        }

        return figures.mapNotNull { parseFigure(it, sidPrefix = "sid-") }
    }

    /**
     * Parse a single figure element.
     * iOS: figure b u a img for thumbnail; figcaption p a for title/author.
     * Figure ID is "sid-XXXXXX".
     */
    private fun parseFigure(figure: Element, sidPrefix: String): Submission? {
        return try {
            val rawId = figure.attr("id")
            val id = rawId.removePrefix(sidPrefix)
            if (id.isEmpty() || id == rawId) return null

            // Thumbnail — iOS: "figure b u a img"
            val imgEl = figure.selectFirst("b u a img, a img, img")
            var thumbnailUrl = imgEl?.attr("src") ?: ""
            if (thumbnailUrl.isEmpty()) thumbnailUrl = imgEl?.attr("data-src") ?: ""
            if (thumbnailUrl.isNotEmpty() && !thumbnailUrl.startsWith("http")) {
                thumbnailUrl = "https:$thumbnailUrl"
            }

            // Title and author — iOS: "figcaption p a" (index 0 = title, 1 = author link)
            val captionLinks = figure.select("figcaption p a")
            val title = captionLinks.getOrNull(0)?.text()?.trim()
                ?: figure.selectFirst("a[href*=\"/view/\"]")?.attr("title")?.trim()
                ?: "Untitled"
            val author = captionLinks.getOrNull(1)?.text()?.trim()
                ?: figure.selectFirst("a[href*=\"/user/\"]")?.text()?.trim()
                ?: ""

            val authorUrl = captionLinks.getOrNull(1)?.attr("href")
                ?: figure.selectFirst("a[href*=\"/user/\"]")?.attr("href") ?: ""
            val authorUsername = if (authorUrl.contains("/user/")) {
                authorUrl.substringAfter("/user/").substringBefore("/")
            } else ""

            // Rating from figure class — iOS: Rating(submissionFigureClass: node.attr("class"))
            val figClass = figure.attr("class")
            val rating = when {
                figClass.contains("r-adult") || figClass.contains("rating-adult") -> "Adult"
                figClass.contains("r-mature") || figClass.contains("rating-mature") -> "Mature"
                else -> "General"
            }

            Submission(
                id = id,
                title = title,
                author = author,
                authorUsername = authorUsername,
                thumbnailUrl = thumbnailUrl,
                rating = rating,
                favorites = 0,
                comments = 0,
                views = 0
            )
        } catch (e: Exception) {
            null
        }
    }

    private fun parseSubmissionDetail(
        doc: org.jsoup.nodes.Document,
        submissionId: String
    ): Submission {
        val title =
            doc.selectFirst(".submission-title h2, #page-submission .submission-title h2, #page-submission h2")
                ?.text()?.trim() ?: "Untitled"

        val authorNodes = doc.select(".submission-id-sub-container a[href*=\"/user/\"], .submission-title a[href*=\"/user/\"], .submission-description a[href*=\"/user/\"], .c-usernameBlockSimple a[href*=\"/user/\"], #page-submission a[href*=\"/user/\"]")
        val authorEl = authorNodes.firstOrNull { it.text().trim().isNotEmpty() && it.parents().none { p -> p.id() == "header" } }
        val author = authorEl?.text()?.trim() ?: ""

        val authorUrl = authorEl?.attr("href") ?: ""
        val authorUsername = if (authorUrl.contains("/user/")) {
            authorUrl.substringAfter("/user/").substringBefore("/")
        } else ""

        var authorAvatar =
            doc.selectFirst("div.artist-profile-avatar img, .submission-content-header img, .sidebar-section img.avatar")
                ?.attr("src") ?: ""
        if (authorAvatar.isNotEmpty() && !authorAvatar.startsWith("http")) {
            authorAvatar = "https:$authorAvatar"
        }
        // iOS approach: avatar is always at a.furaffinity.net/{username}.gif
        if (authorAvatar.isEmpty() && authorUsername.isNotEmpty()) {
            authorAvatar = "https://a.furaffinity.net/$authorUsername.gif"
        }

        // Favorite button — "+Fav" / "-Fav" in #submission-options, href carries the CSRF key
        val favButton = doc.select("div#submission-options a.button, div.submission-content-inner a.button")
            .firstOrNull { it.text().trim() in listOf("+Fav", "-Fav") }
        val isFavorited = favButton?.text()?.trim() == "-Fav"
        val favUrlAttr = favButton?.attr("href") ?: ""
        val favUrl = if (favUrlAttr.isNotEmpty() && !favUrlAttr.startsWith("http")) {
            "$FA_BASE$favUrlAttr"
        } else favUrlAttr

        var fullImageUrl = ""
        val mainImg =
            doc.selectFirst("#page-submission img, .submission-image img, .submission-body img")
        if (mainImg != null) {
            fullImageUrl = mainImg.attr("src").ifEmpty { mainImg.attr("data-src") }
            if (fullImageUrl.isNotEmpty() && !fullImageUrl.startsWith("http")) {
                fullImageUrl = "https:$fullImageUrl"
            }
        }
        if (fullImageUrl.isEmpty() || fullImageUrl.startsWith("data:")) {
            val dl = doc.selectFirst("a[href*=\"/download/\"], a.download")
            if (dl != null) {
                fullImageUrl = dl.attr("href")
                if (fullImageUrl.isNotEmpty() && !fullImageUrl.startsWith("http")) {
                    fullImageUrl = "$FA_BASE$fullImageUrl"
                }
            }
        }

        val descNode = doc.selectFirst(".submission-description-text")
            ?: doc.selectFirst(".submission-description")
            ?: doc.selectFirst(".text")

        // Remove the 'by User on Date' header if it's included in the description container
        descNode?.select(".submission-id-sub-container")?.remove()
        descNode?.select(".submission-content-header")?.remove()
        descNode?.select("a[href^=\"/\"]")?.forEach {
            it.attr("href", "$FA_BASE${it.attr("href")}")
        }
        val description = descNode?.html()?.trim() ?: ""

        val sidebar = doc.selectFirst(".submission-sidebar, .sidebar, .stats")
        var rating = ""
        var category = ""
        var species = ""
        var gender = ""
        var views = 0
        var favorites = 0
        var comments = 0
        var date = doc.selectFirst("span.popup_date")?.attr("title")?.trim() ?: ""

        if (sidebar != null) {
            for (section in sidebar.select("section, div.stats-row, .mobile-section, .info-container div")) {
                val text = section.text().lowercase()
                val header = section.selectFirst("header, h5, h3, h4, strong")?.text()?.trim()?.lowercase() ?: ""
                val value = section.selectFirst("div, span, p")?.text()?.trim() ?: section.text().trim()

                when {
                    header.contains("category") || text.contains("category:") -> category = value.substringAfter(":")
                    header.contains("species") || text.contains("species:") -> species = value.substringAfter(":")
                    header.contains("gender") || text.contains("gender:") -> gender = value.substringAfter(":")
                    header.contains("views") || text.contains("views:") -> views = value.replace(Regex("[^0-9]"), "").toIntOrNull() ?: views
                    header.contains("favorites") || text.contains("favorites:") -> favorites = value.replace(Regex("[^0-9]"), "").toIntOrNull() ?: favorites
                    header.contains("comments") || text.contains("comments:") -> comments = value.replace(Regex("[^0-9]"), "").toIntOrNull() ?: comments
                    header.contains("date") || header.contains("posted") -> if (date.isEmpty()) date = value
                    header.contains("rating") || text.contains("rating:") -> rating = value.substringAfter(":")
                }
            }
        }

        // Direct selectors for beta theme stats container
        doc.select("div.stats-container div.views, .views, .submission-page-stats div[title=\"Views\"], div[title=\"Views\"]").firstOrNull()?.text()?.replace(Regex("[^0-9]"), "")?.toIntOrNull()?.let { views = it }
        doc.select("div.stats-container div.favorites, .favorites, .submission-page-stats div[title=\"Favorites\"], div[title=\"Favorites\"]").firstOrNull()?.text()?.replace(Regex("[^0-9]"), "")?.toIntOrNull()?.let { favorites = it }
        doc.select("div.stats-container div.comments, .comments, .submission-page-stats div[title=\"Comments\"], div[title=\"Comments\"]").firstOrNull()?.text()?.replace(Regex("[^0-9]"), "")?.toIntOrNull()?.let { comments = it }

        val tags = doc.select("a[href*=\"/search/?q=\"], .tag a, .tags a")
            .map { it.text().trim() }
            .filter { it.isNotEmpty() && it.length < 50 }

        var fileUrl = ""
        val dlLink = doc.selectFirst("a[href*=\"/download/\"], a.download")
        if (dlLink != null) {
            fileUrl = dlLink.attr("href")
            if (fileUrl.isNotEmpty() && !fileUrl.startsWith("http")) fileUrl = "$FA_BASE$fileUrl"
        }

        var resolution = ""
        var fileSize = ""
        var fileType = ""

        val statsLabels = doc.select("div.submission-content-stats span.highlight span")
        val statsValues = doc.select("div.submission-content-stats > span:not(.highlight) span")
        if (statsLabels.size == statsValues.size) {
            for (i in statsLabels.indices) {
                val label = statsLabels[i].text().trim().lowercase()
                val value = statsValues[i].text().trim()
                when {
                    label.contains("resolution") -> resolution = value
                    label.contains("file size") -> fileSize = value
                    label.contains("type") || label.contains("category") -> if (fileType.isEmpty()) fileType = value
                }
            }
        }

        val commentList = mutableListOf<Comment>()
        for (commentEl in doc.select("div.comment_container")) {
            try {
                val commentId = commentEl.selectFirst("a.comment_anchor")?.id()?.removePrefix("cid:") ?: ""
                val timestamp = commentEl.attr("data-timestamp").toLongOrNull() ?: 0

                val avatarEl = commentEl.selectFirst("img.comment_useravatar")
                var commentAvatar = avatarEl?.attr("src") ?: ""
                if (commentAvatar.isNotEmpty() && !commentAvatar.startsWith("http")) commentAvatar = "https:$commentAvatar"
                val commentUsername = avatarEl?.attr("alt") ?: ""
                // Fallback — same iOS trick as the author avatar
                if (commentAvatar.isEmpty() && commentUsername.isNotEmpty()) {
                    commentAvatar = "https://a.furaffinity.net/$commentUsername.gif"
                }

                val displayNameEl = commentEl.selectFirst("a.c-usernameBlock__displayName span.js-displayName")
                val commentAuthor = displayNameEl?.text()?.trim() ?: commentUsername

                val commentTitle = commentEl.selectFirst("comment-title")?.text()?.trim() ?: ""

                val commentTextEl = commentEl.selectFirst("comment-user-text")
                commentTextEl?.select("a[href^=\"/\"]")?.forEach {
                    it.attr("href", "$FA_BASE${it.attr("href")}")
                }
                val commentText = commentTextEl?.text()?.trim() ?: ""

                commentList.add(Comment(
                    id = commentId,
                    author = commentAuthor,
                    authorUsername = commentUsername,
                    authorAvatar = commentAvatar,
                    title = commentTitle,
                    text = commentText,
                    timestamp = timestamp
                ))
            } catch (_: Exception) {}
        }

        return Submission(
            id = submissionId,
            title = title,
            author = author,
            authorUsername = authorUsername,
            authorAvatar = authorAvatar,
            thumbnailUrl = fullImageUrl.ifEmpty { authorAvatar },
            fullImageUrl = fullImageUrl,
            description = description,
            rating = rating,
            category = category,
            species = species,
            gender = gender,
            views = views,
            favorites = favorites,
            comments = commentList.size,
            date = date,
            tags = tags,
            fileUrl = fileUrl,
            resolution = resolution,
            fileSize = fileSize,
            fileType = fileType,
            commentList = commentList,
            isFavorited = isFavorited,
            favUrl = favUrl
        )
    }

    suspend fun getUserProfile(username: String): com.furflix.app.data.model.UserProfile = withContext(Dispatchers.IO) {
        val url = "$FA_BASE/user/$username/"
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", USER_AGENT)
            .header("Cookie", getCookiesString())
            .build()
        val response = httpClient.newCall(request).execute()
        if (!response.isSuccessful) throw Exception("Failed to load profile: ${response.code}")
        val html = response.body?.string() ?: ""
        val doc = org.jsoup.Jsoup.parse(html)

        // iOS approach: look for the watch/unwatch button in userpage-nav-interface-buttons
        val watchBtn = doc.selectFirst("userpage-nav-interface-buttons a.button")
        val watchUrlAttr = watchBtn?.attr("href") ?: ""
        val watchUrl = if (watchUrlAttr.isNotEmpty() && !watchUrlAttr.startsWith("http")) {
            "$FA_BASE$watchUrlAttr"
        } else watchUrlAttr

        // iOS approach: watching = URL path starts with /unwatch/
        val isWatching = watchUrlAttr.startsWith("/unwatch/")

        // iOS approach: avatar is always at a.furaffinity.net/{username}.gif — no HTML parsing needed
        val avatarUrl = "https://a.furaffinity.net/$username.gif"

        // Banner
        var bannerUrl = doc.selectFirst("#header a img")?.attr("src") ?: ""
        if (bannerUrl.isNotEmpty() && !bannerUrl.startsWith("http")) bannerUrl = "https:$bannerUrl"

        // Display name: iOS uses c-usernameBlock a.c-usernameBlock__displayName
        val displayName = doc
            .selectFirst("a.c-usernameBlock__displayName, userpage-nav-header .c-usernameBlock__displayName")
            ?.text()?.trim()
            ?.trimStart('~', '!')
            ?: username

        // User title ("Fursuiter | Registered: ...") — own text of span.user-title before the pipe
        val userTitleEl = doc.selectFirst("span.user-title")
        val userTitle = userTitleEl?.ownText()
            ?.substringBefore('|')?.trim() ?: ""

        // Registration date — the popup_date inside user-title carries title="19 years ago"
        val registeredText = userTitleEl?.selectFirst("span.popup_date")?.attr("title")?.trim() ?: ""

        // PM (note) link sits next to the watch button
        val noteUrlAttr = doc.selectFirst("userpage-nav-interface-buttons a[href*=\"/newpm/\"]")?.attr("href") ?: ""
        val noteUrl = if (noteUrlAttr.isNotEmpty() && !noteUrlAttr.startsWith("http")) {
            "$FA_BASE$noteUrlAttr"
        } else noteUrlAttr

        // Profile bio text — .userpage-profile, keeping <br> line breaks via marker chars
        val profileEl = doc.selectFirst(".userpage-profile")
        profileEl?.select("br")?.forEach { it.after(org.jsoup.nodes.TextNode("")) }
        val profileText = (profileEl?.text() ?: "")
            .replace("", "\n")
            .lines().joinToString("\n") { it.trim() }
            .replace(Regex("\n{3,}"), "\n\n")
            .trim()

        // Stats block: <h2>Stats</h2> section with "Views: 78297 Submissions: 498 ..." cells
        var views = 0
        var galleryCount = 0
        var favoritesCount = 0
        var commentsCount = 0
        var journalsCount = 0
        val statsH2 = doc.select("h2").firstOrNull { it.text().trim().equals("Stats", ignoreCase = true) }
        val statsText = statsH2?.closest("section")?.text() ?: ""
        fun statValue(label: String): Int =
            Regex("$label:\\s*([\\d,]+)").find(statsText)?.groupValues?.get(1)
                ?.replace(",", "")?.toIntOrNull() ?: 0
        if (statsText.isNotEmpty()) {
            views = statValue("Views")
            galleryCount = statValue("Submissions")
            favoritesCount = statValue("Favs")
            commentsCount = statValue("Comments Earned")
            journalsCount = statValue("Journals")
        }

        // Watchers / Watching counts live in "View List (Watched by 2944)" links
        var watchersCount = 0
        var watchingCount = 0
        doc.selectFirst("a[href*=\"/watchlist/to/\"]")?.text()?.let { text ->
            watchersCount = Regex("Watched by\\s*([\\d,]+)").find(text)
                ?.groupValues?.get(1)?.replace(",", "")?.toIntOrNull() ?: 0
        }
        doc.selectFirst("a[href*=\"/watchlist/by/\"]")?.text()?.let { text ->
            watchingCount = Regex("Watching\\s*([\\d,]+)").find(text)
                ?.groupValues?.get(1)?.replace(",", "")?.toIntOrNull() ?: 0
        }

        // Legacy fallback for stats if the new selectors found nothing
        if (galleryCount == 0 && favoritesCount == 0) {
            val statElements = doc.select("userpage-nav-stat")
            for (el in statElements) {
                val label = el.selectFirst("span")?.text()?.trim()?.lowercase() ?: ""
                val value = el.selectFirst("b")?.text()?.trim()?.replace(",", "")?.toIntOrNull() ?: 0
                when {
                    label.contains("gallery") -> galleryCount = value
                    label.contains("favorite") -> favoritesCount = value
                    label.contains("watcher") -> watchersCount = value
                    label.contains("watching") -> watchingCount = value
                    label.contains("comment") -> commentsCount = value
                }
            }
        }

        // Profile questions: "Accepting Trades", "Accepting Commissions", "Character Species"
        var acceptingTrades = ""
        var acceptingCommissions = ""
        var species = ""
        for (row in doc.select("#userpage-contact-item .table-row")) {
            val question = row.selectFirst(".userpage-profile-question")?.text()?.trim() ?: continue
            val value = row.text().removePrefix(question).trim()
            when {
                question.contains("Accepting Trades", true) -> acceptingTrades = value
                question.contains("Accepting Commissions", true) -> acceptingCommissions = value
                question.contains("Character Species", true) -> species = value
            }
        }

        // Contact links — icon class carries the type (contact-icon-website etc.)
        val contactLinks = doc.select(".user-contact-item").mapNotNull { item ->
            val type = item.selectFirst("img[class*=\"contact-icon-\"]")?.classNames()
                ?.firstOrNull { it.startsWith("contact-icon-") }
                ?.removePrefix("contact-icon-") ?: "link"
            val info = item.selectFirst(".user-contact-user-info") ?: return@mapNotNull null
            val label = info.selectFirst("strong")?.text()?.trim()
                ?.ifEmpty { null } ?: type.replaceFirstChar { it.uppercase() }
            val anchor = info.selectFirst("a") ?: return@mapNotNull null
            val href = anchor.attr("href").trim()
            val text = anchor.text().trim()
            // Some links (e.g. Website) use javascript:void(0) — keep the visible text instead
            val linkUrl = when {
                href.startsWith("http") -> href
                text.startsWith("http") -> text
                text.contains(".") && !text.contains(" ") -> "https://$text"
                else -> return@mapNotNull null
            }
            com.furflix.app.data.model.ContactLink(type = type, label = label, url = linkUrl)
        }

        com.furflix.app.data.model.UserProfile(
            username = username,
            displayName = displayName,
            avatarUrl = avatarUrl,
            bannerUrl = bannerUrl,
            isWatching = isWatching,
            watchUrl = watchUrl,
            profileText = profileText,
            galleryCount = galleryCount,
            favoritesCount = favoritesCount,
            watchersCount = watchersCount,
            watchingCount = watchingCount,
            commentsCount = commentsCount,
            userTitle = userTitle,
            registeredText = registeredText,
            noteUrl = noteUrl,
            views = views,
            journalsCount = journalsCount,
            acceptingTrades = acceptingTrades,
            acceptingCommissions = acceptingCommissions,
            species = species,
            contactLinks = contactLinks
        )
    }

    suspend fun toggleWatch(watchUrl: String): Boolean = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(watchUrl)
            .header("User-Agent", USER_AGENT)
            .header("Cookie", getCookiesString())
            .header("Referer", "https://www.furaffinity.net/")
            .build()
        val response = httpClient.newCall(request).execute()
        response.isSuccessful
    }

    suspend fun toggleFavorite(favUrl: String): Boolean = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(favUrl)
            .header("User-Agent", USER_AGENT)
            .header("Cookie", getCookiesString())
            .header("Referer", "$FA_BASE/")
            .build()
        val response = httpClient.newCall(request).execute()
        response.isSuccessful
    }

    /**
     * Returns (users, hasNextPage) for "users I am watching".
     * Endpoint: https://www.furaffinity.net/watchlist/by/{username}?page=N
     * This is a public endpoint that works whether or not the viewer is logged in.
     * Mirrors the iOS implementation in FAWatchlistPage.swift.
     */
    suspend fun getWatchlistBy(
        username: String,
        page: Int = 1
    ): Pair<List<com.furflix.app.data.model.WatchlistUser>, Boolean> = withContext(Dispatchers.IO) {
        val url = "$FA_BASE/watchlist/by/$username?page=$page"
        val cookieHeader = getCookiesString()
        val requestBuilder = Request.Builder()
            .url(url)
            .header("User-Agent", USER_AGENT)
            .header("Referer", "$FA_BASE/")
        if (cookieHeader.isNotEmpty()) {
            requestBuilder.header("Cookie", cookieHeader)
        }

        Log.d("FurScraper", "getWatchlistBy: requesting $url (cookies=${cookieHeader.isNotEmpty()})")
        val response = httpClient.newCall(requestBuilder.build()).execute()
        val finalUrl = response.request.url.toString()
        Log.d("FurScraper", "getWatchlistBy: status=${response.code}, finalUrl=$finalUrl")
        if (!response.isSuccessful) throw Exception("Failed to load watchlist/by: ${response.code}")
        val html = response.body?.string() ?: ""
        Log.d("FurScraper", "getWatchlistBy: htmlLen=${html.length}")
        if (finalUrl.contains("/login")) {
            Log.w("FurScraper", "getWatchlistBy: redirected to login page, session likely expired")
        }
        val doc = org.jsoup.Jsoup.parse(html)

        val (users, hasNext) = parseWatchlistPage(doc, "by")
        Log.d("FurScraper", "getWatchlistBy: parsed ${users.size} users, hasNext=$hasNext, first=${users.firstOrNull()?.username}")
        Pair(users, hasNext)
    }

    /**
     * Returns (users, hasNextPage) for "users watching me" (Followers tab).
     * Endpoint: https://www.furaffinity.net/watchlist/to/{username}?page=N
     */
    suspend fun getWatchlistTo(
        username: String,
        page: Int = 1
    ): Pair<List<com.furflix.app.data.model.WatchlistUser>, Boolean> = withContext(Dispatchers.IO) {
        val url = "$FA_BASE/watchlist/to/$username?page=$page"
        val cookieHeader = getCookiesString()
        val requestBuilder = Request.Builder()
            .url(url)
            .header("User-Agent", USER_AGENT)
            .header("Referer", "$FA_BASE/")
        if (cookieHeader.isNotEmpty()) {
            requestBuilder.header("Cookie", cookieHeader)
        }

        Log.d("FurScraper", "getWatchlistTo: requesting $url (cookies=${cookieHeader.isNotEmpty()})")
        val response = httpClient.newCall(requestBuilder.build()).execute()
        val finalUrl = response.request.url.toString()
        Log.d("FurScraper", "getWatchlistTo: status=${response.code}, finalUrl=$finalUrl")
        if (!response.isSuccessful) throw Exception("Failed to load watchlist/to: ${response.code}")
        val html = response.body?.string() ?: ""
        Log.d("FurScraper", "getWatchlistTo: htmlLen=${html.length}")
        if (finalUrl.contains("/login")) {
            Log.w("FurScraper", "getWatchlistTo: redirected to login page, session likely expired")
        }
        val doc = org.jsoup.Jsoup.parse(html)

        val (users, hasNext) = parseWatchlistPage(doc, "to")
        Log.d("FurScraper", "getWatchlistTo: parsed ${users.size} users, hasNext=$hasNext, first=${users.firstOrNull()?.username}")
        Pair(users, hasNext)
    }

    /**
     * Parse the /watchlist/by and /watchlist/to pages.
     * Each user lives in <div class="watch-list-items watch-row ..."> with an
     * <a href="/user/{name}/"> inside a <span class="c-usernameBlockSimple__displayName">.
     * The `title` attribute on the displayName span holds the canonical lowercase username.
     * Pagination is form-based: the "Next" button has `disabled=""` when there is no next page.
     *
     * Returns (unique users, hasNextPage).
     */
    private fun parseWatchlistPage(doc: org.jsoup.nodes.Document, label: String = ""): Pair<List<com.furflix.app.data.model.WatchlistUser>, Boolean> {
        val rawItems = doc.select("div.watch-list-items")
        Log.d("FurScraper", "parseWatchlistPage[$label]: found ${rawItems.size} raw 'div.watch-list-items' blocks")

        if (rawItems.isEmpty()) {
            // The selector matched nothing at all — FA likely changed its markup, or we're on
            // an error/login page. Dump a slice of the HTML so it shows up in Logcat for diagnosis.
            val bodySnippet = doc.body()?.html()?.take(1500) ?: doc.html().take(1500)
            Log.w("FurScraper", "parseWatchlistPage[$label]: 0 raw items — dumping page title='${doc.title()}'")
            Log.w("FurScraper", "parseWatchlistPage[$label]: body snippet: $bodySnippet")
        }

        var skippedFlexHidden = 0
        var skippedNoLink = 0
        var skippedEmptyUsername = 0

        val users = rawItems.mapNotNull { item ->
            // Skip the empty placeholder rows FA uses for flex layout
            if (item.classNames().contains("flex-hidden-item-watches")) {
                skippedFlexHidden++
                return@mapNotNull null
            }
            val link = item.selectFirst("a[href*=\"/user/\"]")
            if (link == null) {
                skippedNoLink++
                return@mapNotNull null
            }

            val href = link.attr("href")
            val extractedUsername = href.substringAfter("/user/").substringBefore("/")
            if (extractedUsername.isEmpty()) {
                skippedEmptyUsername++
                return@mapNotNull null
            }

            // Prefer the displayName span (text = display name, title = canonical lowercase)
            val displayNameEl = item.selectFirst(".c-usernameBlockSimple__displayName")
            val displayName = displayNameEl?.text()?.trim()?.takeIf { it.isNotEmpty() }
                ?: link.text().trim().takeIf { it.isNotEmpty() }
                ?: extractedUsername

            com.furflix.app.data.model.WatchlistUser(
                username = extractedUsername,
                displayName = displayName,
                avatarUrl = "" // /watchlist/by and /watchlist/to do not expose avatars; WatchlistScreen falls back to a.furaffinity.net/{user}.gif
            )
        }.distinctBy { it.username.lowercase() }

        Log.d(
            "FurScraper",
            "parseWatchlistPage[$label]: parsed=${users.size}, skippedFlexHidden=$skippedFlexHidden, " +
                    "skippedNoLink=$skippedNoLink, skippedEmptyUsername=$skippedEmptyUsername"
        )
        if (users.isNotEmpty()) {
            Log.d("FurScraper", "parseWatchlistPage[$label]: usernames=${users.take(10).map { it.username }}")
        }

        // Form-based pagination: the "Next 200" button is disabled when there is no next page.
        // Section structure:
        //   <div class="section-footer watchlist-navigation">
        //     <div class="floatright">
        //       <form method="get" action="/watchlist/by/{user}">
        //         <input type="hidden" name="page" value="N">
        //         <button class="button" type="submit" disabled>Next 200</button>
        //       </form>
        //     </div>
        //   </div>
        val nextButton = doc.selectFirst("div.section-footer div.floatright form button[type=\"submit\"]")
        val hasNext = nextButton?.let { !it.hasAttr("disabled") } ?: false
        Log.d("FurScraper", "parseWatchlistPage[$label]: nextButtonFound=${nextButton != null}, hasNext=$hasNext")

        return Pair(users, hasNext)
    }
}