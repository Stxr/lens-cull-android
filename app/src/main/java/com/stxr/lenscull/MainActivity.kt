package com.stxr.lenscull

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.stxr.lenscull.ui.library.LibraryScreen
import com.stxr.lenscull.ui.library.LibraryViewModel
import com.stxr.lenscull.ui.theme.LensCullTheme

class MainActivity : ComponentActivity() {
  private val viewModel: LibraryViewModel by viewModels()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent { LensCullTheme { LibraryScreen(viewModel) } }
  }

  override fun onResume() {
    super.onResume()
    viewModel.refreshPermission()
  }
}
