package com.ikotliner.batterydisplay.util

import android.Manifest
import android.app.Activity
import android.app.NotificationManager
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.database.ContentObserver
import android.media.AudioManager
import android.net.Uri
import android.net.wifi.WifiManager
import android.os.BatteryManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.ActivityCompat.startActivityForResult
import androidx.core.content.ContextCompat
import com.ikotliner.batterydisplay.broadcastReceiver.ConfigurationChanged
import com.ikotliner.batterydisplay.broadcastReceiver.CustomReceiver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


object Common {
    const val TAG = "BatteryApp"
    const val BATTERY_CHANGE = 0
    const val BATTERY_CHARGING = 1
    const val VOLUME_CHANGE = 2
    const val BLUETOOTH_STATE = 4
    const val WIFI_STATE = 5
    const val MUSE_STATE = 6
    const val INTENT_BATTERY_CHANGE = "android.intent.action.BATTERY_CHANGED"
    const val INTENT_VOLUME_CHANGE = "android.media.VOLUME_CHANGED_ACTION"
    const val MEDIA_VOLUME_CHANGE = "android.media.EXTRA_VOLUME_STREAM_TYPE"
    const val INTENT_BLUETOOTH_CHANGE = BluetoothAdapter.ACTION_STATE_CHANGED
    const val INTENT_BLUETOOTH_CONNECTED = BluetoothDevice.ACTION_ACL_CONNECTED
    const val INTENT_BLUETOOTH_DISCONNECTED = BluetoothDevice.ACTION_ACL_DISCONNECTED
    const val INTENT_WIFI_STATE_CHANGE = WifiManager.WIFI_STATE_CHANGED_ACTION
    const val INTENT_RINGER_MODE_CHANGE = AudioManager.RINGER_MODE_CHANGED_ACTION
    private lateinit var mAudioManager: AudioManager
    private lateinit var mWifiManager: WifiManager
    const val WIFI = "wifi"
    const val CONNECT = "connect"
    const val BT = "bluetooth"
    const val MUSE = "mute"
    const val FLY_MODE = "flyMode"
    private lateinit var mStateListener: StateListener
    private lateinit var mBluetoothAdapter: BluetoothAdapter
    private lateinit var mNotificationManager: NotificationManager

    private val appReceiver = CustomReceiver(object : ConfigurationChanged {
        override fun onChanged(type: Int, value: Int) {
            when (type) {
                BATTERY_CHANGE -> mStateListener.updateBatteryNum(value)
                VOLUME_CHANGE -> mStateListener.updateVolumeNum(value)
                BATTERY_CHARGING -> mStateListener.updateBatteryState(value == BatteryManager.BATTERY_STATUS_CHARGING)
                BLUETOOTH_STATE-> mStateListener.updateBluetoothState(value)
                WIFI_STATE-> mStateListener.updateWifiState(value)
                MUSE_STATE-> mStateListener.updateMuseState(value)
            }

        }
    })

    /**
     * 亮度变化回调函数
     */
    private val mBrightnessObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean) {
            super.onChange(selfChange)
            try {
                mStateListener.updateBrightness()
            } catch (e: Settings.SettingNotFoundException) {
                e.printStackTrace()
            }
        }
    }

    /**
     * 自动亮度变化函数
     */
    private val mBrightnessModeObserver =
        object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                super.onChange(selfChange)
                try {
                    mStateListener.updateBrightnessMode()
                } catch (e: Settings.SettingNotFoundException) {
                    e.printStackTrace()
                }
            }
        }

    fun init(context: Context, stateListener: StateListener) {
        mAudioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        mWifiManager =
            context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        mNotificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        mStateListener = stateListener
        val intentFilter = IntentFilter()
        intentFilter.addAction(INTENT_BATTERY_CHANGE)
        intentFilter.addAction(INTENT_VOLUME_CHANGE)
        intentFilter.addAction(INTENT_BLUETOOTH_CHANGE)
        intentFilter.addAction(INTENT_BLUETOOTH_CONNECTED)
        intentFilter.addAction(INTENT_BLUETOOTH_DISCONNECTED)
        intentFilter.addAction(INTENT_WIFI_STATE_CHANGE)
        intentFilter.addAction(INTENT_RINGER_MODE_CHANGE)
        context.registerReceiver(appReceiver, intentFilter)
        registerSystemBrightness(context)
        registerSystemBrightnessMode(context)
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
        }

        val checkBtAdminPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_ADMIN)
        val checkBtPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH)

        if (checkBtAdminPermission!=PackageManager.PERMISSION_GRANTED || checkBtPermission != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(
                context as Activity,
                arrayOf(Manifest.permission.BLUETOOTH_ADMIN, Manifest.permission.BLUETOOTH),
                0
            )
        }

        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.N && !mNotificationManager.isNotificationPolicyAccessGranted) {
            context.startActivity(Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS))
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
    private fun registerSystemBrightness(context: Context) {
        context.contentResolver.registerContentObserver(
            Settings.System.getUriFor(Settings.System.SCREEN_BRIGHTNESS),
            true,
            mBrightnessObserver
        )
    }

    private fun registerSystemBrightnessMode(
        context: Context
    ) {
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
        return Settings.System.getInt(
            context.contentResolver,
            Settings.System.SCREEN_BRIGHTNESS
        )
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
        mWifiManager.isWifiEnabled = state
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
        if (mBluetoothAdapter.isEnabled){
            return mBluetoothAdapter.state == BluetoothAdapter.STATE_ON
        }
        return false
    }

    fun setBluetoothStatus(state: Boolean) {
        Log.e(TAG, "setBluetoothStatus: $state")
        if (state) mBluetoothAdapter.enable() else mBluetoothAdapter.disable()
    }

    /**
     * 获取Muse状态
     */
    fun requestMuseStatus(): Boolean {
        return mAudioManager.ringerMode == AudioManager.RINGER_MODE_SILENT
    }

    fun setMuseStatus(state: Boolean) {
        mAudioManager.ringerMode = if (state) AudioManager.RINGER_MODE_SILENT else AudioManager.RINGER_MODE_NORMAL
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

    fun destroy(mContext: Context) {

    }


    interface StateListener {
        fun updateBatteryNum(value: Int)
        fun updateBatteryState(state: Boolean)
        fun updateVolumeNum(value: Int)
        fun updateBrightness()
        fun updateBrightnessMode()
        fun updateBluetoothState(value: Int)
        fun updateWifiState(value: Int)
        fun updateMuseState(value: Int)
    }
}