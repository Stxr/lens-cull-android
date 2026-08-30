package com.stxr.lenscull.scan

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Environment
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.ForegroundInfo
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.stxr.lenscull.LensCullApplication
import kotlinx.coroutines.CancellationException

class ScanWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {
  override suspend fun doWork(): Result {
    if (!Environment.isExternalStorageManager()) {
      return Result.failure(Data.Builder().putString(KEY_ERROR, "需要管理所有文件权限").build())
    }
    setForeground(foregroundInfo(ScanProgress(0, 0, 0)))
    val scanner = (applicationContext as LensCullApplication).container.storageScanner
    return try {
      val result = scanner.scan { progress ->
        setProgress(progress.toData())
        setForeground(foregroundInfo(progress))
      }
      Result.success(
        Data.Builder()
          .putInt(KEY_INDEXED, result.indexed)
          .putInt(KEY_FAILURES, result.failures)
          .build(),
      )
    } catch (cancelled: CancellationException) {
      throw cancelled
    } catch (error: Exception) {
      Result.failure(Data.Builder().putString(KEY_ERROR, error.message ?: "扫描失败").build())
    }
  }

  private fun foregroundInfo(progress: ScanProgress): ForegroundInfo {
    val manager = applicationContext.getSystemService(NotificationManager::class.java)
    manager.createNotificationChannel(
      NotificationChannel(CHANNEL_ID, "照片扫描", NotificationManager.IMPORTANCE_LOW),
    )
    val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
      .setSmallIcon(android.R.drawable.ic_menu_gallery)
      .setContentTitle("LensCull 正在扫描照片")
      .setContentText("已索引 ${progress.indexed} 张，${progress.failures} 个异常")
      .setOngoing(true)
      .setProgress(0, 0, true)
      .build()
    return ForegroundInfo(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
  }

  companion object {
    const val UNIQUE_WORK = "lenscull-photo-scan"
    const val KEY_SCANNED = "scanned"
    const val KEY_INDEXED = "indexed"
    const val KEY_FAILURES = "failures"
    const val KEY_ERROR = "error"
    private const val CHANNEL_ID = "lenscull_scan"
    private const val NOTIFICATION_ID = 4101

    fun enqueue(context: Context) {
      val request = OneTimeWorkRequestBuilder<ScanWorker>().addTag(UNIQUE_WORK).build()
      WorkManager.getInstance(context).enqueueUniqueWork(UNIQUE_WORK, ExistingWorkPolicy.REPLACE, request)
    }

    fun cancel(context: Context) {
      WorkManager.getInstance(context).cancelUniqueWork(UNIQUE_WORK)
    }

    private fun ScanProgress.toData(): Data = Data.Builder()
      .putInt(KEY_SCANNED, scanned)
      .putInt(KEY_INDEXED, indexed)
      .putInt(KEY_FAILURES, failures)
      .build()
  }
}
