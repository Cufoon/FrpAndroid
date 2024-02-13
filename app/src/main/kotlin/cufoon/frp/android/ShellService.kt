package cufoon.frp.android

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Binder
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat

class ShellService : Service() {
    private var p: Process? = null

    // Binder given to clients
    private val binder = LocalBinder()

    /**
     * Class used for the client Binder.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with IPC.
     */
    inner class LocalBinder : Binder(), IBinder {
        // Return this instance of LocalService so clients can call public methods
        fun getService(): ShellService = this@ShellService
    }

    override fun onBind(intent: Intent): IBinder {
        return binder
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val filename: String
        if (p != null) {
            Log.w("cufoon_log", "process isn't null,service won't start")
            Toast.makeText(this, "process isn't null,service won't start", Toast.LENGTH_SHORT)
                .show()
            return START_NOT_STICKY
        }
        if (intent != null) {
            filename = intent.extras?.getString("filename") as String
        } else {
            filename = "Error:filename unknown!!!"
            Log.e("cufoon_log", filename)
            Toast.makeText(this, filename, Toast.LENGTH_LONG).show()
            stopSelf()
            return START_NOT_STICKY
        }
        val applicationInfo =
            packageManager.getApplicationInfo(packageName, PackageManager.GET_SHARED_LIBRARY_FILES)
        Log.d("cufoon_log", "native library dir ${applicationInfo.nativeLibraryDir}")
        try {
            Log.d("cufoon_log", "我执行到这了！${applicationInfo.nativeLibraryDir}/${filename}")
            p = Runtime.getRuntime().exec(
                "${applicationInfo.nativeLibraryDir}/${filename} -c config.toml",
                arrayOf<String>(),
                this.filesDir
            )
            Log.d("cufoon_log", p?.outputStream.toString())
        } catch (e: Exception) {
            Log.d("cufoon_log", "服务运行失败")
            Log.e("cufoon_log", e.stackTraceToString())
            Toast.makeText(this, e.message, Toast.LENGTH_LONG).show()
            stopSelf()
            return START_NOT_STICKY
        }
        Toast.makeText(this, "已启动服务", Toast.LENGTH_SHORT).show()
        startForeground(1, showModification())
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        p?.destroy()
        p = null
        Toast.makeText(this, "已关闭服务", Toast.LENGTH_SHORT).show()
    }

    private fun showModification(): Notification {
        val pendingIntent: PendingIntent =
            Intent(this, MainActivity::class.java).let { notificationIntent ->
                PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE)
            }
        return NotificationCompat.Builder(this, "shell_bg")
            .setSmallIcon(R.mipmap.ic_launcher_foreground).setContentTitle("frp后台服务")
            .setContentText("已启动frp")
            .setContentIntent(pendingIntent).build()
    }
}