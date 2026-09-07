package dev.tomatopotato.asterion.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview

@Composable
fun ProjectsScreen(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize().background(Color.White))
}

@Preview
@Composable
private fun ProjectsScreenPreview() {
    ProjectsScreen()
}
