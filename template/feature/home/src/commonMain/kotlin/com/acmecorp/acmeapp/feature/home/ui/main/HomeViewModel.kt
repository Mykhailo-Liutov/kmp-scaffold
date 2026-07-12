package com.acmecorp.acmeapp.feature.home.ui.main

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

// Plain ViewModel — no base class. State is a single StateFlow the UI collects.
class HomeViewModel : ViewModel() {
    val title: StateFlow<String> = MutableStateFlow("Acme App").asStateFlow()
}
