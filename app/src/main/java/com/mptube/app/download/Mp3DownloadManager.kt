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
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.stream.AudioStream
import org.schabi.newpipe.extractor.stream.StreamInfo
import java.io.File
import java.io.BufferedOutputStream
import java.io.FileOutputStream
import java.io.OutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.max

object Mp3DownloadManager
{
    data class Result(
        val displayName: String,
        val location: String
    )

    private val httpClient by lazy { DownloadNetwork.stream }

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
            withBackgroundThreadPriority {
                transcodeToMp3(tempAudio, tempMp3) { percent ->
                    onProgress(58 + (percent * 37 / 100))
                }
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
            .minByOrNull { stream ->
                val bitrate = max(stream.averageBitrate, stream.bitrate)
                if (bitrate > 0) bitrate else Int.MAX_VALUE
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
                    val buffer = ByteArray(256 * 1024)
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
        var outputStream: OutputStream? = null
        var channels = inputFormat.getIntOrDefault(MediaFormat.KEY_CHANNEL_COUNT, 2).coerceIn(1, 2)
        var sampleRate = inputFormat.getIntOrDefault(MediaFormat.KEY_SAMPLE_RATE, 44100)
        var pcmEncoding = AudioFormat.ENCODING_PCM_16BIT
        var lastProgress = -1
        val encodeBuffers = ReusableEncodeBuffers()

        fun prepareLame()
        {
            if (lame != null) return
            lame = LameBuilder()
                .setInSampleRate(sampleRate)
                .setOutSampleRate(sampleRate)
                .setOutChannels(channels)
                .setOutBitrate(128)
                .setQuality(9)
                .build()
            outputStream = BufferedOutputStream(FileOutputStream(outputFile), 128 * 1024)
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
                            encodePcmBuffer(
                                lame = lame!!,
                                output = outputStream!!,
                                buffer = outputBuffer,
                                pcmEncoding = pcmEncoding,
                                channels = channels,
                                reusable = encodeBuffers
                            )
                        }

                        if (durationUs > 0)
                        {
                            val progress = ((info.presentationTimeUs * 100L) / durationUs)
                                .toInt()
                                .coerceIn(0, 100)
                            if (progress != lastProgress)
                            {
                                lastProgress = progress
                                onProgress(progress)
                            }
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

    private class ReusableEncodeBuffers
    {
        var left = ShortArray(0)
        var right = ShortArray(0)
        var mp3 = ByteArray(0)

        fun ensure(sampleCount: Int, mp3Size: Int)
        {
            if (left.size < sampleCount) left = ShortArray(sampleCount)
            if (right.size < sampleCount) right = ShortArray(sampleCount)
            if (mp3.size < mp3Size) mp3 = ByteArray(mp3Size)
        }
    }

    private fun encodePcmBuffer(
        lame: AndroidLame,
        output: OutputStream,
        buffer: ByteBuffer,
        pcmEncoding: Int,
        channels: Int,
        reusable: ReusableEncodeBuffers
    )
    {
        val view = buffer.slice().order(ByteOrder.LITTLE_ENDIAN)
        val bytesPerSample = if (pcmEncoding == AudioFormat.ENCODING_PCM_FLOAT) 4 else 2
        val totalSamples = view.remaining() / bytesPerSample
        val sampleCount = if (channels == 1) totalSamples else totalSamples / 2
        if (sampleCount <= 0) return

        val mp3Size = (7200 + sampleCount * 1.25).toInt()
        reusable.ensure(sampleCount, mp3Size)

        if (pcmEncoding == AudioFormat.ENCODING_PCM_FLOAT)
        {
            if (channels == 1)
            {
                for (i in 0 until sampleCount)
                {
                    val value = view.getFloat().coerceIn(-1f, 1f)
                    reusable.left[i] = (value * Short.MAX_VALUE).toInt().toShort()
                }
            }
            else
            {
                for (i in 0 until sampleCount)
                {
                    val left = view.getFloat().coerceIn(-1f, 1f)
                    val right = view.getFloat().coerceIn(-1f, 1f)
                    reusable.left[i] = (left * Short.MAX_VALUE).toInt().toShort()
                    reusable.right[i] = (right * Short.MAX_VALUE).toInt().toShort()
                }
            }
        }
        else
        {
            if (channels == 1)
            {
                for (i in 0 until sampleCount)
                {
                    reusable.left[i] = view.getShort()
                }
            }
            else
            {
                for (i in 0 until sampleCount)
                {
                    reusable.left[i] = view.getShort()
                    reusable.right[i] = view.getShort()
                }
            }
        }

        val encoded = lame.encode(
            reusable.left,
            if (channels == 1) reusable.left else reusable.right,
            sampleCount,
            reusable.mp3
        )

        if (encoded < 0) error("LAME encode lỗi: $encoded")
        if (encoded > 0) output.write(reusable.mp3, 0, encoded)
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
            .replace(INVALID_FILE_NAME_REGEX, " ")
            .replace(WHITESPACE_REGEX, " ")
            .trim()
            .take(120)
    }

    private inline fun <T> withBackgroundThreadPriority(block: () -> T): T
    {
        val threadId = android.os.Process.myTid()
        val previousPriority = android.os.Process.getThreadPriority(threadId)
        return try
        {
            android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND)
            block()
        }
        finally
        {
            try
            {
                android.os.Process.setThreadPriority(previousPriority)
            }
            catch (_: Exception)
            {
            }
        }
    }

    private val INVALID_FILE_NAME_REGEX = Regex("[\\\\/:*?\"<>|]")
    private val WHITESPACE_REGEX = Regex("\\s+")
}
