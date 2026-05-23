package com.qme.mobile.ui.components


import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Image
import androidx.compose.material3.Divider
import androidx.compose.material3.HorizontalDivider
import com.qme.mobile.R
import com.qme.mobile.ui.theme.QmeBlue
import com.qme.mobile.ui.theme.QmeDarkBlue
import com.qme.mobile.ui.theme.QmeError
import com.qme.mobile.ui.theme.QmeErrorBg
import com.qme.mobile.ui.theme.QmeSky
import com.qme.mobile.ui.theme.QmeSubtext
import com.qme.mobile.ui.theme.QmeSuccess
import com.qme.mobile.ui.theme.QmeSuccessBg
import com.qme.mobile.ui.theme.QmeWhite

private val avatarPalette = listOf(
    Color(0xFF1E88E5), Color(0xFF00ACC1), Color(0xFF43A047),
    Color(0xFF7B1FA2), Color(0xFFF4511E), Color(0xFF00897B),
    Color(0xFFE91E63), Color(0xFF6D4C41), Color(0xFF0288D1),
    Color(0xFFFB8C00)
)

private fun orgColor(name: String): Color {
    val idx = (name.firstOrNull()?.uppercaseChar()?.code ?: 65) % avatarPalette.size
    return avatarPalette[idx]
}

@Composable
fun OrgAvatar(orgName: String, photoBase64: String? = null, size: Dp = 56.dp) {
    val imageBitmap = remember(photoBase64) {
        if (photoBase64.isNullOrBlank()) null
        else runCatching {
            val raw = if (photoBase64.contains(",")) photoBase64.substringAfter(",") else photoBase64
            val bytes = android.util.Base64.decode(raw, android.util.Base64.DEFAULT)
            android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                ?.let { bmp ->
                    android.graphics.Bitmap
                        .createScaledBitmap(bmp, 200, 200, true)
                        .asImageBitmap()
                }
        }.getOrNull()
    }

    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(if (imageBitmap == null) orgColor(orgName) else Color.LightGray),
        contentAlignment = Alignment.Center
    ) {
        if (imageBitmap != null) {
            Image(
                bitmap = imageBitmap,
                contentDescription = orgName,
                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            val initial = orgName.firstOrNull()?.uppercaseChar()?.toString() ?: "?"
            Text(
                text       = initial,
                fontSize   = (size.value * 0.42f).sp,
                fontWeight = FontWeight.Bold,
                color      = QmeWhite
            )
        }
    }
}

@Composable
fun QmeLogo(modifier: Modifier = Modifier, large: Boolean = false) {
    val context = LocalContext.current
    val bitmap = remember {
        val original = android.graphics.BitmapFactory.decodeResource(
            context.resources, R.drawable.qme_logo
        )
        val mutable = original.copy(android.graphics.Bitmap.Config.ARGB_8888, true)
        val pixels = IntArray(mutable.width * mutable.height)
        mutable.getPixels(pixels, 0, mutable.width, 0, 0, mutable.width, mutable.height)
        for (i in pixels.indices) {
            val r = android.graphics.Color.red(pixels[i])
            val g = android.graphics.Color.green(pixels[i])
            val b = android.graphics.Color.blue(pixels[i])
            if (r > 240 && g > 240 && b > 240) {
                pixels[i] = android.graphics.Color.TRANSPARENT
            }
        }
        mutable.setPixels(pixels, 0, mutable.width, 0, 0, mutable.width, mutable.height)
        mutable.asImageBitmap()
    }
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Image(
            bitmap             = bitmap,
            contentDescription = "QMe Logo",
            modifier           = Modifier.height(if (large) 72.dp else 52.dp)
        )
        Text(
            text       = "Queue Me",
            fontSize   = if (large) 18.sp else 14.sp,
            fontWeight = FontWeight.Medium,
            color      = QmeSubtext
        )
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QmeTopBar(

    navigationIcon:
    @Composable (() -> Unit)
    = {}

) {

    Column {

        TopAppBar(

            navigationIcon =
            navigationIcon,

            title = {

                Box(

                    modifier =
                    Modifier.fillMaxWidth(),

                    contentAlignment =
                    Alignment.Center

                ) {

                    Image(

                        painter =
                        painterResource(
                            id =
                            R.drawable.qme_logo
                        ),

                        contentDescription =
                        "QMe Logo",

                        modifier =
                        Modifier.height(
                            40.dp
                        )

                    )

                }

            },

            colors =
            TopAppBarDefaults.topAppBarColors(

                containerColor =
                Color.White

            )

        )


        HorizontalDivider(

            color =
            Color(
                0xFF4A90E2
            ),

            thickness =
            2.dp

        )

    }

}

@Composable
fun QmeTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    enabled: Boolean = true,
    singleLine: Boolean = true
) {
    OutlinedTextField(
        value                = value,
        onValueChange        = onValueChange,
        label                = { Text(label) },
        modifier             = modifier.fillMaxWidth(),
        leadingIcon          = leadingIcon,
        trailingIcon         = trailingIcon,
        visualTransformation = visualTransformation,
        keyboardOptions      = keyboardOptions,
        enabled              = enabled,
        singleLine           = singleLine,
        shape                = RoundedCornerShape(12.dp),
        colors               = OutlinedTextFieldDefaults.colors(
            focusedBorderColor   = QmeBlue,
            focusedLabelColor    = QmeBlue,
            cursorColor          = QmeBlue
        )
    )
}

@Composable
fun QmePrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    loading: Boolean = false,
    enabled: Boolean = true,
    containerColor: Color = QmeBlue
) {
    Button(
        onClick  = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp),
        enabled  = enabled && !loading,
        shape    = RoundedCornerShape(12.dp),
        colors   = ButtonDefaults.buttonColors(
            containerColor         = containerColor,
            disabledContainerColor = containerColor.copy(alpha = 0.4f)
        )
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier    = Modifier.size(20.dp),
                color       = QmeWhite,
                strokeWidth = 2.dp
            )
        } else {
            Text(
                text       = text,
                fontWeight = FontWeight.SemiBold,
                fontSize   = 15.sp,
                color      = QmeWhite
            )
        }
    }
}

@Composable
fun QmeErrorBanner(message: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(QmeErrorBg, RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text       = "⚠ $message",
            color      = QmeError,
            fontSize   = 13.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun QmeSuccessBanner(message: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(QmeSuccessBg, RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Text("✓ $message", color = QmeSuccess, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun QmeLoader(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = QmeBlue)
    }
}

@Composable
fun QmeInfoCard(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(QmeSky, RoundedCornerShape(14.dp))
            .padding(16.dp)
    ) {
        content()
    }
}

@Composable
fun QmeBorderedCard(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, QmeBlue.copy(alpha = 0.25f), RoundedCornerShape(14.dp))
            .background(QmeWhite, RoundedCornerShape(14.dp))
            .padding(16.dp)
    ) {
        content()
    }
}

@Composable
fun StatusBadge(status: String, bold: Boolean = false) {
    val (bg, fg) = when (status.uppercase()) {
        "ACTIVE" -> Color(0xFFE8F5E9) to Color(0xFF2E7D32)
        "PAUSED" -> Color(0xFFFFF3E0) to Color(0xFFE65100)
        "OPEN"   -> Color(0xFFE3F2FD) to Color(0xFF1565C0)
        else     -> Color(0xFFECEFF1) to Color(0xFF607D8B)
    }
    Box(
        modifier = Modifier
            .background(bg, RoundedCornerShape(20.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            text       = status.uppercase(),
            color      = fg,
            fontSize   = 11.sp,
            fontWeight = if (bold) FontWeight.ExtraBold else FontWeight.Bold,
            letterSpacing = 0.5.sp
        )
    }
}

@Composable
fun QmeSpacer(height: Int = 16) = Spacer(Modifier.height(height.dp))