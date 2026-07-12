package com.acmecorp.acmeapp.feature.home.ui.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.acmecorp.acmeapp.core.ui.AppScaffold
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun HomeScreen(
    onOpenCatalog: () -> Unit,
    viewModel: HomeViewModel = koinViewModel(),
) {
    val title by viewModel.title.collectAsStateWithLifecycle()

    AppScaffold(title = title) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("Welcome to the Acme App skeleton.")
            Button(onClick = onOpenCatalog) { Text("Open catalog") }
        }
    }
}
