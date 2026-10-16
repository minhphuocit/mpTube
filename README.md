# mpTube 1.26.04

mpTube là YouTube client tối giản cho Android, ưu tiên máy yếu và màn hình Android ô tô.

## Chức năng chính

- Home chỉ có tìm kiếm video.
- Tab Âm Nhạc lấy video âm nhạc đang thịnh hành tại Việt Nam bằng `videos.list(chart=mostPopular, videoCategoryId=10, regionCode=VN)`.
- Xem video, fullscreen bằng một `YouTubePlayerView` duy nhất.
- Autoplay video liên quan, chống lặp và giới hạn tối đa 10 video.
- Kéo từ trên xuống để tải lại kết quả tìm kiếm hoặc trang Âm Nhạc.
- Nút Back hoạt động theo từng cấp: fullscreen -> player -> kết quả/tab -> Home -> thoát app.
- Cache RAM + disk, request deduplication, giới hạn cache và force refresh.
- UI tự tăng touch target/chữ ở màn hình lớn hoặc Android car mode.

## Tối ưu quota

- Home không gọi API khi mở app.
- Tìm kiếm chỉ gọi API khi người dùng thực sự bấm tìm.
- Mỗi trang search lấy 25 kết quả để giảm số lần phân trang.
- Kết quả search được cache 12 giờ.
- Related được cache 24 giờ và chỉ prefetch khi video đã xem khoảng 82%.
- Tab Âm Nhạc dùng `videos.list` thay vì `search.list`, cache 15 phút.
- Pull-to-refresh bỏ qua cache và lấy dữ liệu mới.

## Cấu hình API key