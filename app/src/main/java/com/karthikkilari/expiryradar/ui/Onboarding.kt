@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.karthikkilari.expiryradar.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.karthikkilari.expiryradar.R
import com.karthikkilari.expiryradar.data.*
import com.karthikkilari.expiryradar.domain.*
import java.time.*
import kotlinx.coroutines.*

@Composable
internal fun Onboarding(done: () -> Unit) {
    var page by rememberSaveable { mutableIntStateOf(0) }
    Column(
        Modifier.fillMaxSize().padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Image(painterResource(R.drawable.radar), "Expiry Radar logo", Modifier.size(140.dp))
        Spacer(Modifier.height(28.dp))
        Text(
            listOf(
                "EXPIRY RADAR",
                "A little reminder. Less waste.",
                "Timely reminders, your choice.",
            )[page],
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(16.dp))
        Text(
            listOf(
                stringResource(R.string.tagline),
                "Track expiry dates, find items at a glance, and reduce forgotten food, skincare and warranties. Everything stays on your phone.",
                "Enable reminders when adding an item. Notification permission is optional. Android may delay reminders to save battery.",
            )[page],
            style = MaterialTheme.typography.bodyLarge,
        )
        Spacer(Modifier.height(32.dp))
        Button(onClick = { if (page < 2) page++ else done() }, modifier = Modifier.fillMaxWidth()) {
            Text(if (page == 0) "Get Started" else if (page == 2) "Start tracking" else "Continue")
        }
        if (page > 0) TextButton(onClick = { page-- }) { Text(stringResource(R.string.ui_back)) }
        Text("${page+1} of 3", style = MaterialTheme.typography.labelMedium)
    }
}
