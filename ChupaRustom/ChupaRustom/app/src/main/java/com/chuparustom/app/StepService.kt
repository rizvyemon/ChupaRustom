package com.chuparustom.app
import android.Manifest
import android.app.*
import android.content.*
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.hardware.*
import android.os.*
import androidx.core.app.*
import androidx.core.content.ContextCompat

class StepService : Service(), SensorEventListener {
    private lateinit var s: Store
    override fun onBind(i: Intent?): IBinder? = null
    override fun onStartCommand(i: Intent?, f: Int, id: Int) = START_STICKY

    override fun onCreate() {
        super.onCreate(); s = Store(this); channels(this)
        val n = NotificationCompat.Builder(this, "svc").setSmallIcon(android.R.drawable.ic_menu_directions)
            .setContentTitle("ChupaRustom is counting your steps").setContentIntent(open(this)).setOngoing(true).build()
        ServiceCompat.startForeground(this, 1, n, if (Build.VERSION.SDK_INT >= 34) ServiceInfo.FOREGROUND_SERVICE_TYPE_HEALTH else 0)
        val sm = getSystemService(SENSOR_SERVICE) as SensorManager
        val sc = sm.getDefaultSensor(Sensor.TYPE_STEP_COUNTER) ?: sm.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR)
        if (sc != null) sm.registerListener(this, sc, SensorManager.SENSOR_DELAY_NORMAL)
    }
    override fun onDestroy() { (getSystemService(SENSOR_SERVICE) as SensorManager).unregisterListener(this); super.onDestroy() }

    override fun onSensorChanged(e: SensorEvent) {
        if (e.sensor.type == Sensor.TYPE_STEP_COUNTER) {
            val v = e.values[0].toLong(); val last = s.lastSensor
            val d = if (last < 0) 0 else if (v >= last) v - last else v
            s.lastSensor = v
            if (d > 0) s.addSteps(d)
        } else s.addSteps(1)
        check()
    }
    override fun onAccuracyChanged(sensor: Sensor?, a: Int) {}

    private fun check() {
        if (s.smokeOn && !s.smokeNotified && s.smokeProgress >= s.smokeSteps) {
            s.smokeNotified = true
            push(this, 2, "You earned a cigarette \uD83D\uDEAC", "You walked ${s.smokeSteps} steps. Or skip it - your lungs will thank you.")
        }
        if (s.fbOn && !s.fbNotified && s.fbProgress >= s.fbSteps) {
            s.fbNotified = true
            push(this, 3, "Facebook unlocked \uD83D\uDCF1", "Open Facebook to start your ${s.fbMin}-minute session.")
        }
    }

    companion object {
        fun open(c: Context) = PendingIntent.getActivity(c, 0, Intent(c, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
        fun channels(c: Context) {
            val m = c.getSystemService(NotificationManager::class.java)
            m.createNotificationChannel(NotificationChannel("svc", "Step counting", NotificationManager.IMPORTANCE_LOW))
            m.createNotificationChannel(NotificationChannel("alerts", "Eligibility alerts", NotificationManager.IMPORTANCE_HIGH))
        }
        fun push(c: Context, id: Int, title: String, body: String) {
            if (Build.VERSION.SDK_INT >= 33 && c.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return
            NotificationManagerCompat.from(c).notify(id, NotificationCompat.Builder(c, "alerts")
                .setSmallIcon(android.R.drawable.ic_dialog_info).setContentTitle(title).setContentText(body)
                .setContentIntent(open(c)).setAutoCancel(true).build())
        }
        fun startIfAllowed(c: Context) {
            val s = Store(c)
            if (!(s.smokeOn || s.fbOn)) return
            if (c.checkSelfPermission(Manifest.permission.ACTIVITY_RECOGNITION) != PackageManager.PERMISSION_GRANTED) return
            try { ContextCompat.startForegroundService(c, Intent(c, StepService::class.java)) } catch (_: Exception) {}
        }
    }
}
