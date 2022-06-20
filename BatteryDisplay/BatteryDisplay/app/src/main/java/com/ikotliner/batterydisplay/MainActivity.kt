package com.ikotliner.batterydisplay

import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.IntentFilter
import android.database.ContentObserver
import android.graphics.Color
import android.media.AudioManager
import android.net.wifi.WifiManager
import android.os.BatteryManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.View
import android.view.WindowManager
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import com.ikotliner.batterydisplay.broadcastReceiver.ConfigurationChanged
import com.ikotliner.batterydisplay.broadcastReceiver.CustomReceiver
import com.ikotliner.batterydisplay.databinding.ActivityMainBinding
import com.ikotliner.batterydisplay.util.Common
import com.ikotliner.batterydisplay.util.Common.requestMuseStatus
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
    private val job = Job()
    private val scope = CoroutineScope(job)
    private lateinit var mContext: Context

    private val mStateListener = object : Common.StateListener {
        override fun updateBatteryNum(value: Int) {
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

        override fun updateBatteryState(state: Boolean) {
            activityMainBinding.chargingIcon.visibility =
                if (state) View.VISIBLE else View.GONE
        }

        override fun updateVolumeNum(value: Int) {
            activityMainBinding.volumeView.progress = Common.requestCurrentVolume() * 10
            activityMainBinding.muteSwitch.isSelected = requestMuseStatus()
        }

        override fun updateBrightness() {
            activityMainBinding.lightView.progress = Common.requestCurrentBrightness(mContext)
        }

        override fun updateBrightnessMode() {
            val state = Common.isAutoBrightness(mContext)

            activityMainBinding.autoBrightness.setTextColor(if (state) Color.GRAY else Color.WHITE)

            activityMainBinding.autoBrightness.background =
                if (state) getDrawable(R.drawable.auto_brightness_open) else getDrawable(R.drawable.auto_brightness_close)
        }

        override fun updateBluetoothState(value: Int) {
            if (value == BluetoothAdapter.STATE_ON) {
                activityMainBinding.bluetoothSwitch.updateStatus(true, Common.BT)
            }
            if (value == BluetoothAdapter.STATE_OFF) {
                activityMainBinding.bluetoothSwitch.updateStatus(false, Common.BT)
            }
        }

        override fun updateWifiState(value: Int) {
            activityMainBinding.wifiSwitch.updateStatus(value == WifiManager.WIFI_STATE_ENABLED,Common.WIFI)
        }

        override fun updateMuseState(value: Int) {
            activityMainBinding.muteSwitch.isSelected = value == AudioManager.RINGER_MODE_SILENT
        }
    }


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
            Common.setMediaVolume(progress / 10)
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
        initViewStatus()
        Common.requestPermission(mContext)
    }

    override fun onResume() {
        super.onResume()
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
        job.cancel()
        Common.destroy(mContext)
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
        //window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN
        val controller = ViewCompat.getWindowInsetsController(activityMainBinding.root)
        controller?.isAppearanceLightStatusBars = true
        controller?.isAppearanceLightNavigationBars = true


        Common.init(mContext, mStateListener)
        activityMainBinding.batteryView.isEnabled = false
        activityMainBinding.autoBrightness.setOnClickListener(mAutoBrightnessListener)
        activityMainBinding.volumeView.setOnSeekBarChangeListener(mVolumeListener)
        activityMainBinding.lightView.setOnSeekBarChangeListener(mBrightnessListener)

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