package com.mptube.app.ui.util

import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import retrofit2.HttpException

internal fun getYoutubeErrorMessage(error: Throwable): String {
    return when (error) {
        is IllegalStateException ->
            "Ứng dụng gặp lỗi trạng thái nội bộ. Vui lòng thử lại."

        is HttpException -> when (error.code()) {
            400 -> "Yêu cầu gửi tới YouTube không hợp lệ."
            401 -> "Máy chủ mpTube chưa được YouTube cấp quyền."
            403 -> "YouTube từ chối yêu cầu. Hãy kiểm tra quota hoặc cấu hình máy chủ."
            404 -> "Không tìm thấy dữ liệu video trên YouTube."
            429 -> "Đã gửi quá nhiều yêu cầu. Vui lòng thử lại sau."
            else -> "YouTube trả về lỗi HTTP ${error.code()}."
        }

        is UnknownHostException ->
            "Không có kết nối mạng.\n\nHãy kiểm tra Wi-Fi hoặc dữ liệu di động."

        is SocketTimeoutException ->
            "Kết nối YouTube hết thời gian.\n\nVui lòng kiểm tra mạng rồi thử lại."

        is IOException ->
            "Không thể kết nối đến YouTube.\n\nVui lòng kiểm tra kết nối mạng rồi thử lại."

        else -> "Đã xảy ra lỗi khi kết nối YouTube."
    }
}
