package com.ikotliner.batterydisplay

import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.content.IntentFilter
import android.database.ContentObserver
import android.graphics.Color
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
import com.ikotliner.batterydisplay.util.Common.setBluetoothStatus
import com.ikotliner.batterydisplay.util.Common.setConnectStatus
import com.ikotliner.batterydisplay.util.Common.setFlyMode
import com.ikotliner.batterydisplay.util.Common.setMuseStatus
import com.ikotliner.batterydisplay.util.Common.setWifiStatus
import com.ikotliner.batterydisplay.util.Common.shotScreen
import com.ikotliner.batterydisplay.view.CustomSwitch
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
     * 自动亮度按钮监听
     */
    private val mAutoBrightnessListener =
        View.OnClickListener { closeAutoBrightness(Common.isAutoBrightness(mContext)) }


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

    /**
     * 各按钮状态回调修改
     */
    private val wifiListener = CustomSwitch.ClickListener { status -> setWifiStatus(status) }
    private val connectListener = CustomSwitch.ClickListener { status -> setConnectStatus(status) }
    private val bluetoothListener =
        CustomSwitch.ClickListener { status -> setBluetoothStatus(status) }
    private val museListener = CustomSwitch.ClickListener { status -> setMuseStatus(status) }
    private val flyModeListener = CustomSwitch.ClickListener { status -> setFlyMode(status) }
    private val screenShotListener = CustomSwitch.ClickListener { shotScreen() }

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
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        );

        Common.init(mContext)
        activityMainBinding.batteryView.isEnabled = false
        activityMainBinding.autoBrightness.setOnClickListener(mAutoBrightnessListener)
        activityMainBinding.volumeView.setOnSeekBarChangeListener(mVolumeListener)
        activityMainBinding.lightView.setOnSeekBarChangeListener(mBrightnessListener)
        Common.registerSystemBrightness(mContext, mBrightnessObserver)
        Common.registerSystemBrightnessMode(mContext, mBrightnessModeObserver)

        /**
         * 使用自定义组合view后的WiFi按钮
         */
        activityMainBinding.wifiSwitch.addClickListener(wifiListener)
        activityMainBinding.connectSwtich.addClickListener(connectListener)
        activityMainBinding.bluetoothSwitch.addClickListener(bluetoothListener)
        activityMainBinding.muteSwitch.addClickListener(museListener)
        activityMainBinding.flyModeSwitch.addClickListener(flyModeListener)

        activityMainBinding.screenShoot.setOnClickListener {
            updateScreenShotButton()
        }
        activityMainBinding.screenShootIcon.setOnClickListener {
            updateScreenShotButton()
        }
        initViewStatus()
    }


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

        activityMainBinding.wifiSwitch.updateStatus(Common.requestWifiStatus(), Common.WIFI)
        activityMainBinding.connectSwtich.updateStatus(Common.requestConnectStatus(), Common.CONNECT)
        activityMainBinding.bluetoothSwitch.updateStatus(Common.requestBluetoothStatus(), Common.BT)
        activityMainBinding.muteSwitch.updateStatus(Common.requestMuseStatus(), Common.MUSE)
        activityMainBinding.flyModeSwitch.updateStatus(Common.requestFlyModeStatus(), Common.FLY_MODE)
    }


    private fun updateScreenShotButton() {
        ObjectAnimator.ofFloat(activityMainBinding.screenShoot, "alpha", 1F, 0.5F, 1F)
            .setDuration(100L).start()
    }
}