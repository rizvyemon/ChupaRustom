package com.chuparustom.app
import android.content.*
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(c: Context, i: Intent) { StepService.startIfAllowed(c) }
}
