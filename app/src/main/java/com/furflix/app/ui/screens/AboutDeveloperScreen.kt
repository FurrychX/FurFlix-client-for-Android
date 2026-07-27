package com.furflix.app.ui.screens

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.furflix.app.R
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.furflix.app.ui.components.HazeTopAppBar
import com.furflix.app.ui.theme.DarkSurfaceVariant
import com.furflix.app.ui.theme.SubtleGray
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import androidx.core.net.toUri

@SuppressLint("ObsoleteSdkInt")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutDeveloperScreen(
    onBack: () -> Unit
) {
    val hazeState = remember { HazeState() }
    val context = LocalContext.current

    val copiedTemplate = stringResource(R.string.about_copied_toast)
    val copyToClipboard = { text: String, label: String ->
        val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
        val clip = android.content.ClipData.newPlainText(label, text)
        clipboard.setPrimaryClip(clip)
        val toastMsg = copiedTemplate.format(label)
        Toast.makeText(context, toastMsg, Toast.LENGTH_SHORT).show()
    }

    Scaffold(
        topBar = {
            HazeTopAppBar(
                title = { Text(stringResource(R.string.about_developer_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                hazeState = hazeState
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .hazeSource(state = hazeState)
                .verticalScroll(rememberScrollState())
                .padding(padding)
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(24.dp))
            
            // Avatar & Name
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Pets,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(50.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Pavel (Furrych)",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = stringResource(R.string.about_dev_role),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.about_dev_bio),
                style = MaterialTheme.typography.bodyMedium,
                color = SubtleGray,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))
            
            // Socials
            Text(
                text = stringResource(R.string.about_connect),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SocialButton(
                    icon = Icons.AutoMirrored.Filled.Send, // Telegram proxy icon
                    label = stringResource(R.string.about_telegram),
                    modifier = Modifier.weight(1f),
                    onClick = { 
                        val intent = Intent(Intent.ACTION_VIEW, "https://t.me/PavelWork_HR".toUri())
                        context.startActivity(intent)
                    }
                )
                SocialButton(
                    icon = Icons.Default.AlternateEmail, // Twitter/X proxy icon
                    label = stringResource(R.string.about_twitter),
                    modifier = Modifier.weight(1f),
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, "https://x.com/Furrych".toUri())
                        context.startActivity(intent)
                    }
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Donations
            Text(
                text = stringResource(R.string.about_support),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(12.dp))

            val monobankCardNumber = stringResource(R.string.about_card_monobank_number)
            DonationCard(
                icon = Icons.Default.CreditCard,
                title = stringResource(R.string.about_card_monobank),
                subtitle = "${stringResource(R.string.about_card_monobank_sub)}\n$monobankCardNumber",
                buttonText = stringResource(R.string.about_btn_donate),
                onClick = {
                    val intent = Intent(Intent.ACTION_VIEW, "https://send.monobank.ua/jar/31dqVo9pHr".toUri())
                    context.startActivity(intent)
                },
                secondaryIcon = Icons.Default.ContentCopy,
                onSecondaryClick = {
                    copyToClipboard(monobankCardNumber, "Card Number")
                }
            )

            Spacer(modifier = Modifier.height(12.dp))

            DonationCard(
                icon = Icons.Default.CurrencyExchange, // Using CurrencyExchange for Tether/Crypto generic
                title = "USDT (TRC20)",
                subtitle = "TVX7biFztNzd2P85y8iDGTN6uLyuduGEj7",
                buttonText = stringResource(R.string.about_btn_copy),
                onClick = { copyToClipboard("TVX7biFztNzd2P85y8iDGTN6uLyuduGEj7", "USDT Address") }
            )

            Spacer(modifier = Modifier.height(12.dp))

            DonationCard(
                icon = Icons.Default.CurrencyBitcoin,
                title = "Bitcoin (BTC)",
                subtitle = "12mRZTgc1dPr8iqPmzuwszKK7Ee9uQafwX",
                buttonText = stringResource(R.string.about_btn_copy),
                onClick = { copyToClipboard("12mRZTgc1dPr8iqPmzuwszKK7Ee9uQafwX", "BTC Address") }
            )

            Spacer(modifier = Modifier.height(48.dp))
        }
    }
}

@Composable
fun SocialButton(
    icon: ImageVector,
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier.height(48.dp),
        shape = RoundedCornerShape(16.dp),
        color = DarkSurfaceVariant,
        onClick = onClick
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

@Composable
fun DonationCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    buttonText: String,
    onClick: () -> Unit,
    secondaryIcon: ImageVector? = null,
    onSecondaryClick: (() -> Unit)? = null
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = DarkSurfaceVariant
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = SubtleGray)
            }
            Spacer(modifier = Modifier.width(16.dp))
            
            if (secondaryIcon != null && onSecondaryClick != null) {
                IconButton(onClick = onSecondaryClick, modifier = Modifier.size(32.dp)) {
                    Icon(secondaryIcon, contentDescription = "Copy", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                }
                Spacer(modifier = Modifier.width(8.dp))
            }

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                onClick = onClick
            ) {
                Text(
                    text = buttonText,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }
        }
    }
}
