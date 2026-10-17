package com.mptube.app.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.mptube.app.BuildConfig
import com.mptube.app.R

@Composable
fun AboutScreen() {
    val context = LocalContext.current

    fun openLink(url: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        context.startActivity(intent)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = painterResource(R.drawable.mptube_logo),
            contentDescription = "mpTube",
            modifier = Modifier
                .size(76.dp)
                .clip(RoundedCornerShape(22.dp)),
            contentScale = ContentScale.Fit
        )

        Spacer(Modifier.height(12.dp))
        Text("mpTube", style = MaterialTheme.typography.headlineMedium)
        Text(
            "YouTube Client tối giản cho Android",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(6.dp))
        Text(
            "v${BuildConfig.VERSION_NAME}",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(24.dp))

        AboutCard("mpTube Information", R.drawable.ic_info) {
            Text(
                "mpTube là ứng dụng YouTube tối giản dành cho hệ điều hành Android, tập trung vào trải nghiệm xem video nhanh, nhẹ và thuận tiện giúp các máy yếu hoạt động tốt!",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))
            AboutLinkItem(R.drawable.ic_code, "Mã nguồn trên GitHub") {
                openLink("https://github.com/minhphuocit/mpTube")
            }
        }

        Spacer(Modifier.height(16.dp))

        AboutCard("Author", R.drawable.ic_person) {
            Text(
                "Được phát triển bởi @wlug.bim với sự tập trung vào trải nghiệm đơn giản, mượt mà và thân thiện.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))
            AboutLinkItem(R.drawable.ic_public, "Facebook") {
                openLink("https://www.facebook.com/bimmer.26th9/")
            }
            AboutLinkItem(R.drawable.ic_camera, "Instagram") {
                openLink("https://www.instagram.com/wlug.bim?stkn=MXJhMGdvZWhxYnE1")
            }
            AboutLinkItem(R.drawable.ic_chat, "Zalo") {
                openLink("https://chat.zalo.me/")
            }
        }

        Spacer(Modifier.height(16.dp))

        AboutCard("Thông tin ứng dụng", R.drawable.ic_settings) {
            AboutInfoRow("Phiên bản", "v${BuildConfig.VERSION_NAME}")
            AboutInfoRow("Nền tảng", "Android + Kotlin")
            AboutInfoRow("Trạng thái", "Đang phát triển...")
            AboutInfoRow("Version code", BuildConfig.VERSION_CODE.toString())
            AboutInfoRow("Android tối thiểu", "Android 8.0 (API 26)")
            AboutInfoRow("Khu vực nội dung", "Việt Nam (VN)")
        }

        Spacer(Modifier.height(16.dp))

        AboutCard("Hiệu năng & kiến trúc", R.drawable.ic_code) {
            AboutBullet("Ưu tiên máy cấu hình yếu: hạn chế recomposition không cần thiết và dùng ảnh chất lượng phù hợp.")
            AboutBullet("Cache RAM + disk cho dữ liệu YouTube và request deduplication để giảm gọi mạng lặp.")
            AboutBullet("Related nhạc có cache cục bộ và cache phía Worker để giảm độ trễ và tiết kiệm quota.")
            AboutBullet("Retrofit + OkHttp với timeout ngắn và fallback để giữ trải nghiệm ổn định khi mạng yếu.")
        }

        Spacer(Modifier.height(24.dp))
        Text("mpTube", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(4.dp))
        Text(
            "Made with ♥ for Android",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(4.dp))
        Text(
            "© 2026",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun AboutCard(
    title: String,
    @DrawableRes icon: Int,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painter = painterResource(icon),
                    contentDescription = null,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(Modifier.width(10.dp))
                Text(title, style = MaterialTheme.typography.titleMedium)
            }
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
private fun AboutLinkItem(
    @DrawableRes icon: Int,
    title: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.width(12.dp))
        Text(
            title,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyLarge
        )
        Text(
            "›",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun AboutBullet(text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            "•",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun AboutInfoRow(title: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 7.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            title,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}
