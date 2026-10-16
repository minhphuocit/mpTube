package com.mptube.app.download

import android.content.ContentValues
import android.content.Context
import android.media.AudioFormat
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.mptube.app.model.Video
import com.naman14.androidlame.AndroidLame
import com.naman14.androidlame.LameBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.stream.AudioStream
import org.schabi.newpipe.extractor.stream.StreamInfo
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.TimeUnit
import kotlin.math.max

object Mp3DownloadManager
{
    data class Result(
        val displayName: String,
        val location: String
    )

    private val httpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .build()
    }

    @Volatile
    private var extractorInitialized = false

    suspend fun download(
        context: Context,
        video: Video,
        onProgress: (Int) -> Unit = {}
    ): Result = withContext(Dispatchers.IO)
    {
        ensureExtractor()
        onProgress(2)

        val watchUrl = "https://www.youtube.com/watch?v=${video.videoId}"
        val info = StreamInfo.getInfo(ServiceList.YouTube, watchUrl)
        var audioStream = chooseAudioStream(info.audioStreams)
            ?: error("Không tìm thấy audio stream có thể tải.")

        onProgress(8)

        val tempAudio = File.createTempFile("mptube_audio_", ".media", context.cacheDir)
        val tempMp3 = File.createTempFile("mptube_mp3_", ".mp3", context.cacheDir)

        try
        {
            try
            {
                downloadStream(audioStream.content, tempAudio) { percent ->
                    onProgress(8 + (percent * 47 / 100))
                }
            }
            catch (firstError: Exception)
            {
                val retryable = firstError.message?.contains("HTTP 403") == true ||
                    firstError.message?.contains("HTTP 410") == true
                if (!retryable) throw firstError

                tempAudio.delete()
                val refreshedInfo = StreamInfo.getInfo(ServiceList.YouTube, watchUrl)
                audioStream = chooseAudioStream(refreshedInfo.audioStreams)
                    ?: throw firstError
                downloadStream(audioStream.content, tempAudio) { percent ->
                    onProgress(8 + (percent * 47 / 100))
                }
            }

            onProgress(58)
            transcodeToMp3(tempAudio, tempMp3) { percent ->
                onProgress(58 + (percent * 37 / 100))
            }

            onProgress(96)
            val result = saveMp3(context, tempMp3, video)
            onProgress(100)
            result
        }
        finally
        {
            tempAudio.delete()
            tempMp3.delete()
        }
    }

    @Synchronized
    private fun ensureExtractor()
    {
        if (extractorInitialized) return
        NewPipe.init(NewPipeHttpDownloader.instance)
        extractorInitialized = true
    }

    private fun chooseAudioStream(streams: List<AudioStream>): AudioStream?
    {
        return streams
            .asSequence()
            .filter { it.isUrl && it.content.startsWith("http") }
            .maxByOrNull { stream ->
                max(stream.averageBitrate, stream.bitrate)
            }
    }

    private fun downloadStream(
        url: String,
        output: File,
        onProgress: (Int) -> Unit
    )
    {
        val request = okhttp3.Request.Builder()
            .url(url)
            .header("User-Agent", NewPipeHttpDownloader.USER_AGENT)
            .build()

        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful)
            {
                error("Không tải được audio: HTTP ${response.code}")
            }

            val body = response.body ?: error("Audio stream rỗng.")
            val total = body.contentLength()
            var copied = 0L
            var lastProgress = -1

            body.byteStream().use { input ->
                FileOutputStream(output).buffered().use { out ->
                    val buffer = ByteArray(64 * 1024)
                    while (true)
                    {
                        val count = input.read(buffer)
                        if (count < 0) break
                        out.write(buffer, 0, count)
                        copied += count

                        if (total > 0)
                        {
                            val progress = ((copied * 100L) / total).toInt().coerceIn(0, 100)
                            if (progress != lastProgress)
                            {
                                lastProgress = progress
                                onProgress(progress)
                            }
                        }
                    }
                    out.flush()
                }
            }
        }
    }

    private fun transcodeToMp3(
        inputFile: File,
        outputFile: File,
        onProgress: (Int) -> Unit
    )
    {
        val extractor = MediaExtractor()
        extractor.setDataSource(inputFile.absolutePath)

        var trackIndex = -1
        var inputFormat: MediaFormat? = null
        for (i in 0 until extractor.trackCount)
        {
            val format = extractor.getTrackFormat(i)
            val mime = format.getString(MediaFormat.KEY_MIME).orEmpty()
            if (mime.startsWith("audio/"))
            {
                trackIndex = i
                inputFormat = format
                break
            }
        }

        if (trackIndex < 0 || inputFormat == null)
        {
            extractor.release()
            error("Định dạng audio không được Android hỗ trợ.")
        }

        extractor.selectTrack(trackIndex)
        val mime = inputFormat.getString(MediaFormat.KEY_MIME)
            ?: error("Không xác định được codec audio.")
        val decoder = MediaCodec.createDecoderByType(mime)
        decoder.configure(inputFormat, null, null, 0)
        decoder.start()

        val durationUs = if (inputFormat.containsKey(MediaFormat.KEY_DURATION))
        {
            inputFormat.getLong(MediaFormat.KEY_DURATION)
        }
        else 0L

        val info = MediaCodec.BufferInfo()
        var inputEnded = false
        var outputEnded = false
        var outputFormat = inputFormat
        var lame: AndroidLame? = null
        var outputStream: FileOutputStream? = null
        var channels = inputFormat.getIntOrDefault(MediaFormat.KEY_CHANNEL_COUNT, 2).coerceIn(1, 2)
        var sampleRate = inputFormat.getIntOrDefault(MediaFormat.KEY_SAMPLE_RATE, 44100)
        var pcmEncoding = AudioFormat.ENCODING_PCM_16BIT

        fun prepareLame()
        {
            if (lame != null) return
            lame = LameBuilder()
                .setInSampleRate(sampleRate)
                .setOutSampleRate(sampleRate)
                .setOutChannels(channels)
                .setOutBitrate(192)
                .setQuality(5)
                .build()
            outputStream = FileOutputStream(outputFile)
        }

        try
        {
            while (!outputEnded)
            {
                if (!inputEnded)
                {
                    val inputIndex = decoder.dequeueInputBuffer(10_000)
                    if (inputIndex >= 0)
                    {
                        val buffer = decoder.getInputBuffer(inputIndex)
                            ?: error("Không lấy được decoder input buffer.")
                        buffer.clear()
                        val size = extractor.readSampleData(buffer, 0)
                        if (size < 0)
                        {
                            decoder.queueInputBuffer(
                                inputIndex,
                                0,
                                0,
                                0,
                                MediaCodec.BUFFER_FLAG_END_OF_STREAM
                            )
                            inputEnded = true
                        }
                        else
                        {
                            decoder.queueInputBuffer(
                                inputIndex,
                                0,
                                size,
                                extractor.sampleTime.coerceAtLeast(0L),
                                0
                            )
                            extractor.advance()
                        }
                    }
                }

                when (val outputIndex = decoder.dequeueOutputBuffer(info, 10_000))
                {
                    MediaCodec.INFO_OUTPUT_FORMAT_CHANGED ->
                    {
                        outputFormat = decoder.outputFormat
                        channels = outputFormat.getIntOrDefault(MediaFormat.KEY_CHANNEL_COUNT, channels).coerceIn(1, 2)
                        sampleRate = outputFormat.getIntOrDefault(MediaFormat.KEY_SAMPLE_RATE, sampleRate)
                        pcmEncoding = if (
                            Build.VERSION.SDK_INT >= Build.VERSION_CODES.N &&
                            outputFormat.containsKey(MediaFormat.KEY_PCM_ENCODING)
                        )
                        {
                            outputFormat.getInteger(MediaFormat.KEY_PCM_ENCODING)
                        }
                        else AudioFormat.ENCODING_PCM_16BIT
                        prepareLame()
                    }

                    MediaCodec.INFO_TRY_AGAIN_LATER -> Unit

                    else -> if (outputIndex >= 0)
                    {
                        if (lame == null) prepareLame()

                        val outputBuffer = decoder.getOutputBuffer(outputIndex)
                        if (outputBuffer != null && info.size > 0)
                        {
                            outputBuffer.position(info.offset)
                            outputBuffer.limit(info.offset + info.size)
                            val pcm = pcmToShortArray(outputBuffer, pcmEncoding)
                            encodePcm(lame!!, outputStream!!, pcm, channels)
                        }

                        if (durationUs > 0)
                        {
                            val progress = ((info.presentationTimeUs * 100L) / durationUs)
                                .toInt()
                                .coerceIn(0, 100)
                            onProgress(progress)
                        }

                        outputEnded = info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0
                        decoder.releaseOutputBuffer(outputIndex, false)
                    }
                }
            }

            if (lame == null) prepareLame()
            val flushBuffer = ByteArray(16 * 1024)
            val flushSize = lame!!.flush(flushBuffer)
            if (flushSize > 0)
            {
                outputStream!!.write(flushBuffer, 0, flushSize)
            }
            outputStream!!.flush()
            onProgress(100)
        }
        finally
        {
            try { outputStream?.close() } catch (_: Exception) { }
            try { lame?.close() } catch (_: Exception) { }
            try { decoder.stop() } catch (_: Exception) { }
            decoder.release()
            extractor.release()
        }
    }

    private fun MediaFormat.getIntOrDefault(key: String, fallback: Int): Int
    {
        return if (containsKey(key)) getInteger(key) else fallback
    }

    private fun pcmToShortArray(buffer: ByteBuffer, pcmEncoding: Int): ShortArray
    {
        val view = buffer.slice().order(ByteOrder.LITTLE_ENDIAN)
        return if (pcmEncoding == AudioFormat.ENCODING_PCM_FLOAT)
        {
            val count = view.remaining() / 4
            ShortArray(count) { index ->
                val value = view.getFloat(index * 4).coerceIn(-1f, 1f)
                (value * Short.MAX_VALUE).toInt().toShort()
            }
        }
        else
        {
            val shortBuffer = view.asShortBuffer()
            ShortArray(shortBuffer.remaining()).also { shortBuffer.get(it) }
        }
    }

    private fun encodePcm(
        lame: AndroidLame,
        output: FileOutputStream,
        pcm: ShortArray,
        channels: Int
    )
    {
        if (pcm.isEmpty()) return

        val encoded = if (channels == 1)
        {
            val mp3Buffer = ByteArray((7200 + pcm.size * 1.25).toInt())

            lame.encode(
                pcm,
                pcm,
                pcm.size,
                mp3Buffer
            ).also { size ->
                if (size > 0)
                {
                    output.write(mp3Buffer, 0, size)
                }
            }
        }
        else
        {
            val sampleCount = pcm.size / 2
            if (sampleCount == 0) return

            val left = ShortArray(sampleCount)
            val right = ShortArray(sampleCount)

            var sourceIndex = 0

            for (i in 0 until sampleCount)
            {
                left[i] = pcm[sourceIndex]
                right[i] = pcm[sourceIndex + 1]
                sourceIndex += 2
            }

            val mp3Buffer =
                ByteArray((7200 + sampleCount * 1.25).toInt())

            lame.encode(
                left,
                right,
                sampleCount,
                mp3Buffer
            ).also { size ->
                if (size > 0)
                {
                    output.write(mp3Buffer, 0, size)
                }
            }
        }

        if (encoded < 0)
        {
            error("LAME encode lỗi: $encoded")
        }
    }

    private fun saveMp3(context: Context, source: File, video: Video): Result
    {
        val cleanTitle = sanitizeFileName(video.title).ifBlank { "mpTube_${video.videoId}" }
        val displayName = "$cleanTitle.mp3"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
        {
            val resolver = context.contentResolver
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
                put(MediaStore.MediaColumns.MIME_TYPE, "audio/mpeg")
                put(MediaStore.MediaColumns.RELATIVE_PATH, "${Environment.DIRECTORY_MUSIC}/mpTube")
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }

            val uri = resolver.insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, values)
                ?: error("Không tạo được file MP3 trong Music/mpTube.")

            try
            {
                resolver.openOutputStream(uri, "w")?.use { output ->
                    source.inputStream().buffered().use { input ->
                        input.copyTo(output, 128 * 1024)
                    }
                } ?: error("Không mở được file MP3 đích.")

                values.clear()
                values.put(MediaStore.MediaColumns.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
                return Result(displayName, "Music/mpTube/$displayName")
            }
            catch (e: Exception)
            {
                resolver.delete(uri, null, null)
                throw e
            }
        }

        val musicRoot = context.getExternalFilesDir(Environment.DIRECTORY_MUSIC)
            ?: File(context.filesDir, Environment.DIRECTORY_MUSIC)
        val folder = File(musicRoot, "mpTube")
        if (!folder.exists() && !folder.mkdirs())
        {
            error("Không tạo được thư mục lưu MP3.")
        }
        val target = uniqueFile(folder, cleanTitle, "mp3")
        source.copyTo(target, overwrite = true)
        return Result(target.name, target.absolutePath)
    }

    private fun uniqueFile(folder: File, baseName: String, extension: String): File
    {
        var file = File(folder, "$baseName.$extension")
        var index = 2
        while (file.exists())
        {
            file = File(folder, "$baseName ($index).$extension")
            index++
        }
        return file
    }

    private fun sanitizeFileName(value: String): String
    {
        return value
            .replace(Regex("[\\\\/:*?\"<>|]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
            .take(120)
    }
}
