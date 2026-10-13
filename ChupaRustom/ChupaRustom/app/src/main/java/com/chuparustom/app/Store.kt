package com.chuparustom.app
import android.content.Context

class Store(c: Context) {
    val sp = c.applicationContext.getSharedPreferences("chupa", 0)
    private fun put(k: String, v: Any) { sp.edit().apply { when (v) { is Boolean -> putBoolean(k, v); is Int -> putInt(k, v); is Long -> putLong(k, v); is String -> putString(k, v) } }.apply() }
    private fun b(k: String) = sp.getBoolean(k, false)
    private fun i(k: String, d: Int = 0) = sp.getInt(k, d)
    private fun l(k: String, d: Long = 0) = sp.getLong(k, d)
    private fun day() = java.text.SimpleDateFormat("yyyyMMdd", java.util.Locale.US).format(java.util.Date())

    var name: String get() = sp.getString("name", "") ?: ""; set(v) = put("name", v)
    var onboarded get() = b("onboarded"); set(v) = put("onboarded", v)
    var totalSteps get() = l("total"); set(v) = put("total", v)
    var lastSensor get() = l("lastSensor", -1); set(v) = put("lastSensor", v)
    // smoking
    var smokeOn get() = b("smokeOn"); set(v) = put("smokeOn", v)
    var smokeSteps get() = i("smokeSteps", 2000); set(v) = put("smokeSteps", v)
    var smokeMark get() = l("smokeMark"); set(v) = put("smokeMark", v)
    var smokeNotified get() = b("smokeNotified"); set(v) = put("smokeNotified", v)
    var smokeTotal get() = i("smokeTotal"); set(v) = put("smokeTotal", v)
    var smokeEarly get() = i("smokeEarly"); set(v) = put("smokeEarly", v)
    // social
    var fbOn get() = b("fbOn"); set(v) = put("fbOn", v)
    var fbSteps get() = i("fbSteps", 2000); set(v) = put("fbSteps", v)
    var fbMin get() = i("fbMin", 15); set(v) = put("fbMin", v)
    var fbMark get() = l("fbMark"); set(v) = put("fbMark", v)
    var fbNotified get() = b("fbNotified"); set(v) = put("fbNotified", v)
    var fbEnd get() = l("fbEnd"); set(v) = put("fbEnd", v)
    var fbSessions get() = i("fbSessions"); set(v) = put("fbSessions", v)
    var fbUsedMin get() = i("fbUsedMin"); set(v) = put("fbUsedMin", v)
    var fbPkg: String get() = sp.getString("fbPkg", "") ?: ""; set(v) = put("fbPkg", v)

    val smokeProgress get() = (totalSteps - smokeMark).coerceAtLeast(0)
    val fbProgress get() = (totalSteps - fbMark).coerceAtLeast(0)

    fun addSteps(d: Long) {
        if (sp.getString("day", "") != day()) { put("day", day()); put("today", 0L) }
        totalSteps += d; put("today", l("today") + d)
    }
    fun stepsToday() = if (sp.getString("day", "") == day()) l("today") else 0L
    fun smokedToday() = if (sp.getString("smokeDay", "") == day()) i("smokeToday") else 0
    fun logSmoke() {
        if (smokeProgress < smokeSteps) smokeEarly++
        put("smokeToday", smokedToday() + 1); put("smokeDay", day())
        smokeTotal++; smokeMark = totalSteps; smokeNotified = false
    }
    fun startFbSession() {
        fbEnd = System.currentTimeMillis() + fbMin * 60000L
        fbMark = totalSteps; fbSessions++; fbUsedMin += fbMin; fbNotified = false
    }
}
