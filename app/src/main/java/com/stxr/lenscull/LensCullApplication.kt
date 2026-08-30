package com.stxr.lenscull

import android.app.Application

class LensCullApplication : Application() {
  val container: AppContainer by lazy { AppContainer(this) }
}
