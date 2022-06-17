package com.ikotliner.batterydisplay.util

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.database.ContentObserver
import android.media.AudioManager
import android.net.Uri
import android.net.wifi.WifiManager
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


object Common {
    const val TAG = "BatteryApp"
    const val BATTERY_CHANGE = 0
    const val VOLUME_CHANGE = 1
    const val INTENT_BATTERY_CHANGE = "android.intent.action.BATTERY_CHANGED"
    const val INTENT_VOLUME_CHANGE = "android.media.VOLUME_CHANGED_ACTION"
    const val MEDIA_VOLUME_CHANGE = "android.media.EXTRA_VOLUME_STREAM_TYPE"
    private lateinit var mAudioManager: AudioManager
    private lateinit var mWifiManager: WifiManager
    const val WIFI = "wifi"
    const val CONNECT = "connect"
    const val BT = "bluetooth"
    const val MUSE = "mute"
    const val FLY_MODE = "flyMode"

    fun init(context: Context) {
        mAudioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        mWifiManager =
            context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
    }

    /**
     * 判断是否有修改系统设置的权限
     */
    fun requestPermission(context: Context) {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.M) {
            if (!Settings.System.canWrite(context)) {
                val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS)
                intent.data = Uri.parse("package:${context.packageName}")
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
            }
        }
        val checkCallPhonePermission =
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE)
        if (checkCallPhonePermission != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                context as Activity,
                arrayOf(Manifest.permission.READ_PHONE_STATE),
                0
            )
            return
        }
    }

    /**
     * 设置媒体音量
     */
    fun setMediaVolume(volume: Int) {
        mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, volume, 0)
    }

    /**
     * 获取当前音量
     */
    fun requestCurrentVolume(): Int {
        return mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
    }

    /**
     * 注册系统亮度监听
     */
    fun registerSystemBrightness(context: Context, mBrightnessObserver: ContentObserver) {
        context.contentResolver.registerContentObserver(
            Settings.System.getUriFor(Settings.System.SCREEN_BRIGHTNESS),
            true,
            mBrightnessObserver
        )
    }

    fun registerSystemBrightnessMode(context: Context, mBrightnessModeObserver: ContentObserver) {
        context.contentResolver.registerContentObserver(
            Settings.System.getUriFor(Settings.System.SCREEN_BRIGHTNESS_MODE),
            true,
            mBrightnessModeObserver
        )
    }

    /**
     * 修改系统亮度
     */
    fun changeBrightness(context: Context, scope: CoroutineScope, value: Int) {
        scope.launch(Dispatchers.IO) {
            Settings.System.putInt(
                context.contentResolver, Settings.System.SCREEN_BRIGHTNESS,
                value
            )
        }
    }

    /**
     * 获取系统亮度
     */
    fun requestCurrentBrightness(context: Context): Int {
        return Settings.System.getInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS)
    }

    fun isAutoBrightness(context: Context): Boolean {
        return Settings.System.getInt(
            context.contentResolver,
            Settings.System.SCREEN_BRIGHTNESS_MODE
        ) == Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC
    }

    /**
     * 自动亮度开关
     */
    fun closeAutoBrightness(context: Context, state: Boolean) {
        Settings.System.putInt(
            context.contentResolver, Settings.System.SCREEN_BRIGHTNESS_MODE,
            if (state) Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL else Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC
        )
    }

    /**
     * 获取WiFi状态
     */
    fun requestWifiStatus(): Boolean {
        if (mWifiManager.isWifiEnabled) {
            return mWifiManager.wifiState == WifiManager.WIFI_STATE_ENABLED
        }
        return false
    }

    fun setWifiStatus(state: Boolean) {
        Log.e(TAG, "setWifiStatus: $state")
    }

    /**
     * 获取数据状态
     */
    fun requestConnectStatus(): Boolean {
        return false
    }

    fun setConnectStatus(state: Boolean) {
        Log.e(TAG, "setConnectStatus: $state")
    }

    /**
     * 获取Bluetooth状态
     */
    fun requestBluetoothStatus(): Boolean {
        return false
    }

    fun setBluetoothStatus(state: Boolean) {
        Log.e(TAG, "setBluetoothStatus: $state")
    }

    /**
     * 获取Muse状态
     */
    fun requestMuseStatus(): Boolean {
        return false
    }

    fun setMuseStatus(state: Boolean) {
        Log.e(TAG, "setMuseStatus: $state")
    }

    /**
     * 获取FlyMode状态
     */
    fun requestFlyModeStatus(): Boolean {
        return false
    }

    fun setFlyMode(state: Boolean) {
        Log.e(TAG, "setFlyMode: $state")
    }

    /**
     * 截图
     */
    fun shotScreen() {
        Log.e(TAG, "shotScreen: ")
    }


}