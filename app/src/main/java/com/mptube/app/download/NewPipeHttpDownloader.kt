package com.mptube.app.download

import okhttp3.RequestBody.Companion.toRequestBody
import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.downloader.Request
import org.schabi.newpipe.extractor.downloader.Response
import org.schabi.newpipe.extractor.exceptions.ReCaptchaException

class NewPipeHttpDownloader private constructor() : Downloader()
{
    private val client = DownloadNetwork.extractor

    override fun execute(request: Request): Response
    {
        val method = request.httpMethod()
        val data = request.dataToSend()
        val requiresBody = method.equals("POST", ignoreCase = true) ||
            method.equals("PUT", ignoreCase = true) ||
            method.equals("PATCH", ignoreCase = true)
        val body = when
        {
            data != null -> data.toRequestBody(null)
            requiresBody -> ByteArray(0).toRequestBody(null)
            else -> null
        }
        val builder = okhttp3.Request.Builder()
            .url(request.url())
            .method(method, body)

        var hasUserAgent = false
        request.headers().forEach { (name, values) ->
            builder.removeHeader(name)
            values.forEach { value ->
                builder.addHeader(name, value)
            }
            if (name.equals("User-Agent", ignoreCase = true)) hasUserAgent = true
        }

        if (!hasUserAgent)
        {
            builder.header("User-Agent", USER_AGENT)
        }

        client.newCall(builder.build()).execute().use { response ->
            if (response.code == 429)
            {
                throw ReCaptchaException("YouTube yêu cầu xác minh", request.url())
            }

            val text = response.body?.string().orEmpty()
            return Response(
                response.code,
                response.message,
                response.headers.toMultimap(),
                text,
                response.request.url.toString()
            )
        }
    }

    companion object
    {
        const val USER_AGENT = "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/140.0 Mobile Safari/537.36"
        val instance: NewPipeHttpDownloader by lazy { NewPipeHttpDownloader() }
    }
}
