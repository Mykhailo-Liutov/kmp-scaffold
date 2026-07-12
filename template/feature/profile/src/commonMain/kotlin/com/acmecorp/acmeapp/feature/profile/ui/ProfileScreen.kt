package com.acmecorp.acmeapp.feature.profile.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.acmecorp.acmeapp.core.ui.AppScaffold
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ProfileScreen(
    @Suppress("UNUSED_PARAMETER") viewModel: ProfileViewModel = koinViewModel(),
) {
    AppScaffold(title = "Profile") { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("Profile placeholder.")
        }
    }
}
