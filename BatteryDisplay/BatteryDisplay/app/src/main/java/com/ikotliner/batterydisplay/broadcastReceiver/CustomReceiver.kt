package com.ikotliner.batterydisplay.broadcastReceiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.BatteryManager
import com.ikotliner.batterydisplay.util.Common

/**
 * 电池广播
 */
class CustomReceiver(private val configurationChanged: ConfigurationChanged) : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        val bundle = intent?.extras
        val status =
            intent?.getIntExtra(BatteryManager.EXTRA_STATUS, BatteryManager.BATTERY_STATUS_UNKNOWN)
        configurationChanged.onBatteryCharging(status == BatteryManager.BATTERY_STATUS_CHARGING)
        when (intent?.action) {
            Common.INTENT_BATTERY_CHANGE -> {
                configurationChanged.onChanged(Common.BATTERY_CHANGE, bundle?.getInt("level")!!)
            }
            Common.INTENT_VOLUME_CHANGE -> {
                if (intent.getIntExtra(Common.MEDIA_VOLUME_CHANGE, -1) == AudioManager.STREAM_MUSIC)
                    configurationChanged.onChanged(Common.VOLUME_CHANGE, 0)
            }

        }
    }

}

interface ConfigurationChanged {
    fun onChanged(type: Int, value: Int)
    fun onBatteryCharging(state: Boolean)
}