package com.smartmonitor.app

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import androidx.core.app.NotificationCompat
import kotlin.math.abs

class MotionGuardService : Service() {
    private var projection: MediaProjection? = null
    private var reader: ImageReader? = null
    private var display: VirtualDisplay? = null
    private var thread: HandlerThread? = null
    private var handler: Handler? = null
    private var baseline = IntArray(0)
    private var lastAlert = 0L
    private var frameCount = 0
    private var width = 0
    private var height = 0
    override fun onBind(intent: Intent?): IBinder? = null
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) { stopCapture(); stopSelf(); return START_NOT_STICKY }
        val resultCode = intent?.getIntExtra(EXTRA_RESULT, Activity.RESULT_CANCELED) ?: Activity.RESULT_CANCELED
        val data = if (Build.VERSION.SDK_INT >= 33) intent?.getParcelableExtra(EXTRA_DATA, Intent::class.java) else @Suppress("DEPRECATION") intent?.getParcelableExtra(EXTRA_DATA)
        if (resultCode != Activity.RESULT_OK || data == null) { stopSelf(); return START_NOT_STICKY }
        startForegroundCompat()
        val manager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        projection = manager.getMediaProjection(resultCode, data)
        beginCapture()
        return START_STICKY
    }
    private fun startForegroundCompat() {
        val channel = "motion_guard_channel"
        if (Build.VERSION.SDK_INT >= 26) getSystemService(NotificationManager::class.java).createNotificationChannel(NotificationChannel(channel, "نگهبان تصویر", NotificationManager.IMPORTANCE_LOW))
        val n = NotificationCompat.Builder(this, channel).setSmallIcon(android.R.drawable.ic_menu_view).setContentTitle("نگهبان تصویر فعال است").setContentText("بررسی حرکت در محدوده انتخاب‌شده").setOngoing(true).build()
        if (Build.VERSION.SDK_INT >= 29) startForeground(71, n, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION) else startForeground(71, n)
    }
    private fun beginCapture() {
        val m = resources.displayMetrics
        width = m.widthPixels; height = m.heightPixels
        thread = HandlerThread("MotionGuardFrames").also { it.start() }; handler = Handler(thread!!.looper)
        reader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2)
        reader!!.setOnImageAvailableListener({ src -> val image = src.acquireLatestImage() ?: return@setOnImageAvailableListener; try { inspect(image) } finally { image.close() } }, handler)
        display = projection?.createVirtualDisplay("SmartMonitorGuard", width, height, m.densityDpi, DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR, reader!!.surface, null, handler)
        projection?.registerCallback(object : MediaProjection.Callback() { override fun onStop() { stopCapture(); stopSelf() } }, handler)
    }
    private fun inspect(image: Image) {
        frameCount++; if (frameCount % 2 != 0) return
        val p = getSharedPreferences("motion_guard", 0)
        val l = (p.getFloat("left", .15f) * width).toInt().coerceIn(0, width - 1)
        val t = (p.getFloat("top", .20f) * height).toInt().coerceIn(0, height - 1)
        val r = (p.getFloat("right", .85f) * width).toInt().coerceIn(l + 1, width)
        val b = (p.getFloat("bottom", .80f) * height).toInt().coerceIn(t + 1, height)
        val plane = image.planes[0]; val buf = plane.buffer; val values = IntArray(24 * 24); var k = 0
        for (gy in 0 until 24) for (gx in 0 until 24) {
            val y = t + (b - t - 1) * gy / 24; val x = l + (r - l - 1) * gx / 24
            val off = y * plane.rowStride + x * plane.pixelStride
            val red = buf.get(off).toInt() and 255; val green = buf.get(off + 1).toInt() and 255; val blue = buf.get(off + 2).toInt() and 255
            values[k++] = (red * 30 + green * 59 + blue * 11) / 100
        }
        if (baseline.isNotEmpty()) {
            var changed = 0
            for (i in values.indices) if (abs(values[i] - baseline[i]) > 28) changed++
            val percent = changed * 100 / values.size
            if (percent >= p.getInt("sensitivity", 12) && System.currentTimeMillis() - lastAlert > 10000) { lastAlert = System.currentTimeMillis(); sendMotionAlert(percent) }
        }
        baseline = values
    }
    private fun sendMotionAlert(percent: Int) {
        val channel = "motion_guard_alerts"
        if (Build.VERSION.SDK_INT >= 26) getSystemService(NotificationManager::class.java).createNotificationChannel(NotificationChannel(channel, "هشدار حرکت", NotificationManager.IMPORTANCE_HIGH))
        val n = NotificationCompat.Builder(this, channel).setSmallIcon(android.R.drawable.ic_dialog_alert).setContentTitle("حرکت در محدوده نگهبانی!").setContentText("تغییر تصویری قابل توجه تشخیص داده شد ($percent٪)").setPriority(NotificationCompat.PRIORITY_HIGH).setAutoCancel(true).build()
        getSystemService(NotificationManager::class.java).notify(72, n)
    }
    private fun stopCapture() {
        display?.release(); display = null; reader?.close(); reader = null; projection?.stop(); projection = null
        thread?.quitSafely(); thread = null; handler = null; baseline = intArrayOf()
        if (Build.VERSION.SDK_INT >= 24) stopForeground(STOP_FOREGROUND_REMOVE) else @Suppress("DEPRECATION") stopForeground(true)
    }
    override fun onDestroy() { stopCapture(); super.onDestroy() }
    companion object { const val ACTION_STOP = "com.smartmonitor.app.STOP_MOTION_GUARD"; const val EXTRA_RESULT = "projection_result"; const val EXTRA_DATA = "projection_data" }
}