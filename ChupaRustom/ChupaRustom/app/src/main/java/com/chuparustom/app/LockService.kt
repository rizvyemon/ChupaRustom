package com.chuparustom.app
import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.os.*
import android.view.accessibility.AccessibilityEvent

class LockService : AccessibilityService() {
    private val h = Handler(Looper.getMainLooper())
    private var cur = ""
    private val fb = setOf("com.facebook.katana", "com.facebook.lite")
    private val recheck = Runnable { enforce() }

    override fun onAccessibilityEvent(e: AccessibilityEvent) {
        if (e.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val p = e.packageName?.toString() ?: return
        if (p == "com.android.systemui" || p.contains("inputmethod") || p.contains("keyboard")) return
        cur = p; h.removeCallbacks(recheck)
        if (p in fb) { Store(this).fbPkg = p; enforce() }
    }

    private fun enforce() {
        val s = Store(this)
        if (!s.fbOn || cur !in fb) return
        val left = s.fbEnd - System.currentTimeMillis()
        if (left > 0) { h.removeCallbacks(recheck); h.postDelayed(recheck, left + 500); return }
        startActivity(Intent(this, LockActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP))
    }
    override fun onInterrupt() {}
}
