package com.ikotliner.batterydisplay

import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.content.IntentFilter
import android.database.ContentObserver
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.View
import android.view.WindowManager
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import androidx.appcompat.app.AppCompatActivity
import com.ikotliner.batterydisplay.broadcastReceiver.ConfigurationChanged
import com.ikotliner.batterydisplay.broadcastReceiver.CustomReceiver
import com.ikotliner.batterydisplay.databinding.ActivityMainBinding
import com.ikotliner.batterydisplay.util.Common
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel


@SuppressLint("UseCompatLoadingForDrawables")
class MainActivity : AppCompatActivity() {

    private lateinit var activityMainBinding: ActivityMainBinding
    private var mCurrentVolume: Int = 0
    private var mScreenBrightness: Int = 0
    private val job = Job()
    private val scope = CoroutineScope(job)
    private lateinit var mContext: Context

    private val appReceiver = CustomReceiver(object : ConfigurationChanged {
        override fun onChanged(type: Int, value: Int) {
            when (type) {
                Common.BATTERY_CHANGE -> {
                    when (value) {
                        in 0..10 -> {
                            activityMainBinding.batteryView.progressDrawable =
                                getDrawable(R.drawable.battery_background_low)
                            activityMainBinding.batteryNum.setTextColor(Color.WHITE)
                        }
                        in 11..60 -> {
                            activityMainBinding.batteryView.progressDrawable =
                                getDrawable(R.drawable.battery_background_middle)
                            activityMainBinding.batteryNum.setTextColor(Color.WHITE)
                        }
                        else -> {
                            activityMainBinding.batteryView.progressDrawable =
                                getDrawable(R.drawable.battery_background_high)
                            activityMainBinding.batteryNum.setTextColor(Color.GRAY)
                        }
                    }
                    activityMainBinding.batteryView.progress = value
                    activityMainBinding.batteryNum.text = value.toString()
                }
                Common.VOLUME_CHANGE -> {
                    activityMainBinding.volumeView.progress = Common.requestCurrentVolume() * 10
                }
            }

        }

        override fun onBatteryCharging(state: Boolean) {
            activityMainBinding.chargingIcon.visibility = if (state) View.VISIBLE else View.GONE
        }
    })

    /**
     * 音量seekBar监听
     */
    private val mVolumeListener = object : OnSeekBarChangeListener {
        override fun onProgressChanged(p0: SeekBar?, progress: Int, fromUser: Boolean) {
            mCurrentVolume = progress / 10
            Common.setMediaVolume(mCurrentVolume)
        }

        override fun onStartTrackingTouch(p0: SeekBar?) {}

        override fun onStopTrackingTouch(p0: SeekBar?) {}
    }

    /**
     * 亮度seekBar监听
     */
    private val mBrightnessListener = object : OnSeekBarChangeListener {
        override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
            Common.changeBrightness(mContext, scope, progress)
        }

        override fun onStartTrackingTouch(seekBar: SeekBar) {}

        override fun onStopTrackingTouch(seekBar: SeekBar) {}
    }

    /**
     * 亮度变化回调函数
     */
    private val mBrightnessObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean) {
            super.onChange(selfChange)
            try {
                mScreenBrightness = Common.requestCurrentBrightness(mContext)
                activityMainBinding.lightView.progress = mScreenBrightness
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
                    initViewStatus()
                } catch (e: Settings.SettingNotFoundException) {
                    e.printStackTrace()
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        init()
        Common.requestPermission(mContext)
    }

    override fun onResume() {
        super.onResume()
        initViewStatus()
        val intentFilter = IntentFilter()
        intentFilter.addAction(Common.INTENT_BATTERY_CHANGE)
        intentFilter.addAction(Common.INTENT_VOLUME_CHANGE)
        mContext.registerReceiver(appReceiver, intentFilter)
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
        job.cancel()
        mContext.unregisterReceiver(appReceiver)
        contentResolver.unregisterContentObserver(mBrightnessObserver)
    }

    /**
     * 初始化控件
     */

    private fun init() {
        mContext = this
        activityMainBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(activityMainBinding.root)
        //隐藏导航栏
        supportActionBar?.hide()
        window.statusBarColor = getColor(R.color.white)
        window.navigationBarColor = getColor(R.color.white)
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN
        window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        Common.init(mContext)
        activityMainBinding.batteryView.isEnabled = false
        activityMainBinding.autoBrightness.setOnClickListener {
            closeAutoBrightness(
                Common.isAutoBrightness(
                    mContext
                )
            )
        }
        activityMainBinding.volumeView.setOnSeekBarChangeListener(mVolumeListener)
        activityMainBinding.lightView.setOnSeekBarChangeListener(mBrightnessListener)
        Common.registerSystemBrightness(mContext, mBrightnessObserver)
        Common.registerSystemBrightnessMode(mContext, mBrightnessModeObserver)
        activityMainBinding.wifiState.setOnClickListener {
            //updateWifiButton(Common.requestWifiStatus())
            updateWifiButton(isWifiOpen)
            isWifiOpen = !isWifiOpen
        }
        activityMainBinding.wifiStateIcon.setOnClickListener {
            //updateWifiButton(Common.requestWifiStatus())
            updateWifiButton(isWifiOpen)
            isWifiOpen = !isWifiOpen
        }
        activityMainBinding.internetState.setOnClickListener {
            updateInternetButton(isInternetOpen)
            isInternetOpen = !isInternetOpen
        }
        activityMainBinding.internetStateIcon.setOnClickListener {
            updateInternetButton(isInternetOpen)
            isInternetOpen = !isInternetOpen
        }
        activityMainBinding.bluetoothState.setOnClickListener {
            updateBluetoothButton(isBluetoothOpen)
            isBluetoothOpen = !isBluetoothOpen
        }
        activityMainBinding.bluetoothStateIcon.setOnClickListener {
            updateBluetoothButton(isBluetoothOpen)
            isBluetoothOpen = !isBluetoothOpen
        }
        activityMainBinding.muteState.setOnClickListener {
            updateMuteButton(isMuteOpen)
            isMuteOpen = !isMuteOpen
        }
        activityMainBinding.muteStateIcon.setOnClickListener {
            updateMuteButton(isMuteOpen)
            isMuteOpen = !isMuteOpen
        }
        activityMainBinding.flyModeState.setOnClickListener {
            updateFlyModeButton(isFlyModeOpen)
            isFlyModeOpen = !isFlyModeOpen
        }
        activityMainBinding.flyModeStateIcon.setOnClickListener {
            updateFlyModeButton(isFlyModeOpen)
            isFlyModeOpen = !isFlyModeOpen
        }
        activityMainBinding.screenShoot.setOnClickListener {
            updateScreenShotButton()
        }
        activityMainBinding.screenShootIcon.setOnClickListener {
            updateScreenShotButton()
        }
        initViewStatus()
    }

    private var isWifiOpen = false
    private var isInternetOpen = false
    private var isBluetoothOpen = false
    private var isMuteOpen = false
    private var isFlyModeOpen = false
    private var isScreenShotOpen = false

    private fun closeAutoBrightness(state: Boolean) {
        activityMainBinding.autoBrightness.setTextColor(if (state) Color.WHITE else Color.GRAY)

        activityMainBinding.autoBrightness.background =
            if (state) getDrawable(R.drawable.auto_brightness_close) else getDrawable(R.drawable.auto_brightness_open)

        Common.closeAutoBrightness(mContext, state)
    }

    private fun initViewStatus() {
        val state = Common.isAutoBrightness(mContext)

        activityMainBinding.autoBrightness.setTextColor(if (state) Color.GRAY else Color.WHITE)

        activityMainBinding.autoBrightness.background =
            if (state) getDrawable(R.drawable.auto_brightness_open) else getDrawable(R.drawable.auto_brightness_close)

        activityMainBinding.volumeView.progress = Common.requestCurrentVolume() * 10
        activityMainBinding.lightView.progress = Common.requestCurrentBrightness(mContext)

        activityMainBinding.wifiState.alpha = if (Common.requestWifiStatus()) 1F else 0.5F
        activityMainBinding.wifiStateIcon.background =
            if (Common.requestWifiStatus()) getDrawable(R.drawable.wifi_open) else getDrawable(R.drawable.wifi_default)
    }

    private fun updateWifiButton(state: Boolean) {
        if (state) {
            ObjectAnimator.ofFloat(activityMainBinding.wifiState, "alpha", 1F, 0.5F)
                .setDuration(100L).start()
        } else {
            ObjectAnimator.ofFloat(activityMainBinding.wifiState, "alpha", 0.5F, 1F)
                .setDuration(100L).start()
        }
        activityMainBinding.wifiStateIcon.background =
            if (!state) getDrawable(R.drawable.wifi_open) else getDrawable(R.drawable.wifi_default)
        Common.setWifiStatus(!state)
    }

    private fun updateInternetButton(state: Boolean) {
        if (state) {
            ObjectAnimator.ofFloat(activityMainBinding.internetState, "alpha", 1F, 0.5F)
                .setDuration(100L).start()
        } else {
            ObjectAnimator.ofFloat(activityMainBinding.internetState, "alpha", 0.5F, 1F)
                .setDuration(100L).start()
        }
        activityMainBinding.internetStateIcon.background =
            if (!state) getDrawable(R.drawable.internet_open) else getDrawable(R.drawable.internet_default)
    }

    private fun updateBluetoothButton(state: Boolean) {
        if (state) {
            ObjectAnimator.ofFloat(activityMainBinding.bluetoothState, "alpha", 1F, 0.5F)
                .setDuration(100L).start()
        } else {
            ObjectAnimator.ofFloat(activityMainBinding.bluetoothState, "alpha", 0.5F, 1F)
                .setDuration(100L).start()
        }
        activityMainBinding.bluetoothStateIcon.background =
            if (!state) getDrawable(R.drawable.bluetooth_open) else getDrawable(R.drawable.bluetooth_default)
    }

    private fun updateMuteButton(state: Boolean) {
        if (state) {
            ObjectAnimator.ofFloat(activityMainBinding.muteState, "alpha", 1F, 0.5F)
                .setDuration(100L).start()
        } else {
            ObjectAnimator.ofFloat(activityMainBinding.muteState, "alpha", 0.5F, 1F)
                .setDuration(100L).start()
        }
        activityMainBinding.muteStateIcon.background =
            if (!state) getDrawable(R.drawable.mute_open) else getDrawable(R.drawable.mute_default)
    }

    private fun updateFlyModeButton(state: Boolean) {
        if (state) {
            ObjectAnimator.ofFloat(activityMainBinding.flyModeState, "alpha", 1F, 0.5F)
                .setDuration(100L).start()
        } else {
            ObjectAnimator.ofFloat(activityMainBinding.flyModeState, "alpha", 0.5F, 1F)
                .setDuration(100L).start()
        }
        activityMainBinding.flyModeStateIcon.background =
            if (!state) getDrawable(R.drawable.fly_mode_open) else getDrawable(R.drawable.fly_mode_default)
    }

    private fun updateScreenShotButton() {
        ObjectAnimator.ofFloat(activityMainBinding.screenShoot, "alpha", 0.5F, 1F, 0.5F)
            .setDuration(100L).start()
    }
}