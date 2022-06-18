package com.ikotliner.batterydisplay.broadcastReceiver

import android.bluetooth.BluetoothAdapter
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.net.wifi.WifiManager
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
        configurationChanged.onChanged(Common.BATTERY_CHARGING,status!!)
        when (intent.action) {
            Common.INTENT_BATTERY_CHANGE -> {
                configurationChanged.onChanged(Common.BATTERY_CHANGE, bundle?.getInt("level")!!)
            }
            Common.INTENT_VOLUME_CHANGE -> {
                if (intent.getIntExtra(Common.MEDIA_VOLUME_CHANGE, -1) == AudioManager.STREAM_MUSIC)
                    configurationChanged.onChanged(Common.VOLUME_CHANGE, 0)
            }
            Common.INTENT_BLUETOOTH_CHANGE -> {
                configurationChanged.onChanged(Common.BLUETOOTH_STATE,intent.getIntExtra(BluetoothAdapter.EXTRA_STATE,0))
            }
            Common.INTENT_WIFI_STATE_CHANGE->{
                configurationChanged.onChanged(Common.WIFI_STATE,intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE,0))
            }

        }
    }

}

interface ConfigurationChanged {
    fun onChanged(type: Int, value: Int)
}