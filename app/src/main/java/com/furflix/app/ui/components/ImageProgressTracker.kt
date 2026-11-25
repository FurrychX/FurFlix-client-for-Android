package com.furflix.app.ui.components

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import okhttp3.Interceptor
import okhttp3.MediaType
import okhttp3.Response
import okhttp3.ResponseBody
import okio.Buffer
import okio.BufferedSource
import okio.ForwardingSource
import okio.Source
import okio.buffer

object ImageProgressTracker {
    private val _progressMap = MutableStateFlow<Map<String, Float>>(emptyMap())
    val progressMap: StateFlow<Map<String, Float>> = _progressMap

    fun updateProgress(url: String, progress: Float) {
        _progressMap.update { currentMap ->
            val mutableMap = currentMap.toMutableMap()
            mutableMap[url] = progress
            mutableMap
        }
    }
}

class ProgressInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val response = chain.proceed(request)
        val url = request.url.toString()

        return response.newBuilder()
            .body(ProgressResponseBody(url, response.body ?: return response))
            .build()
    }
}

private class ProgressResponseBody(
    private val url: String,
    private val responseBody: ResponseBody
) : ResponseBody() {

    private var bufferedSource: BufferedSource? = null

    override fun contentType(): MediaType? = responseBody.contentType()

    override fun contentLength(): Long = responseBody.contentLength()

    override fun source(): BufferedSource {
        if (bufferedSource == null) {
            bufferedSource = source(responseBody.source()).buffer()
        }
        return bufferedSource!!
    }

    private fun source(source: Source): Source {
        return object : ForwardingSource(source) {
            var totalBytesRead = 0L

            override fun read(sink: Buffer, byteCount: Long): Long {
                val bytesRead = super.read(sink, byteCount)

                // Read operation is complete, bytesRead is -1
                totalBytesRead += if (bytesRead != -1L) bytesRead else 0

                val contentLength = responseBody.contentLength()
                if (contentLength > 0) {
                    val progress = (totalBytesRead.toFloat() / contentLength.toFloat()).coerceIn(0f, 1f)
                    ImageProgressTracker.updateProgress(url, progress)
                }

                return bytesRead
            }
        }
    }
}
