package me.donnie.doraemon.gadgets.bluetooth.advertisement.service

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.bluetooth.BluetoothManager
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.BluetoothLeAdvertiser
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.ActivityCompat
import me.donnie.doraemon.gadgets.bluetooth.advertisement.ADVERTISING_FAILED
import me.donnie.doraemon.gadgets.bluetooth.advertisement.ADVERTISING_TIMED_OUT
import me.donnie.doraemon.gadgets.bluetooth.advertisement.AdvertiseActivity
import me.donnie.doraemon.gadgets.bluetooth.advertisement.BLE_NOTIFICATION_CHANNEL_ID
import me.donnie.doraemon.gadgets.bluetooth.advertisement.BT_ADVERTISING_FAILED_EXTRA_CODE
import me.donnie.doraemon.gadgets.bluetooth.advertisement.FOREGROUND_NOTIFICATION_ID
import me.donnie.doraemon.gadgets.bluetooth.advertisement.R
import me.donnie.doraemon.gadgets.bluetooth.advertisement.ScanFilterService_UUID
import java.util.concurrent.TimeUnit

class AdvertiseService : Service() {

	companion object {
		var running: Boolean = false
	}

	private val TAG = "AdvertiseService"

	private var bluetoothLeAdvertiser: BluetoothLeAdvertiser? = null
	private var advertiseCallback: AdvertiseCallback? = null
	private var handler: Handler? = null

	private val TIMEOUT: Long = TimeUnit.MICROSECONDS.convert(10, TimeUnit.MINUTES)

	override fun onBind(intent: Intent): IBinder? = null

	override fun onCreate() {
		running = true
		initialize()
		startAdvertising()
		setTimeout()
		super.onCreate()
	}

	private fun initialize() {
		if (bluetoothLeAdvertiser != null) return
		val manager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
		val bluetoothAdapter = manager.adapter
		bluetoothLeAdvertiser = bluetoothAdapter.bluetoothLeAdvertiser
	}

	private fun startAdvertising() {
		goForeground()
		if (advertiseCallback != null) return
		val settings = buildAdvertiseSettings()
		val data = buildAdvertiseData()
		advertiseCallback = sampleAdvertiseCallback()
		if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADVERTISE) != PackageManager.PERMISSION_GRANTED) {
			// TODO: Consider calling
			//    ActivityCompat#requestPermissions
			// here to request the missing permissions, and then overriding
			//   public void onRequestPermissionsResult(int requestCode, String[] permissions,
			//                                          int[] grantResults)
			// to handle the case where the user grants the permission. See the documentation
			// for ActivityCompat#requestPermissions for more details.
			return
		}
		bluetoothLeAdvertiser?.startAdvertising(settings, data, advertiseCallback)
	}

	private fun goForeground() {
		val notificationIntent = Intent(this, AdvertiseActivity::class.java)
		val pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0)
		val notificationBuilder = when {
			Build.VERSION.SDK_INT >= Build.VERSION_CODES.O -> {
				val bleNotificationChannel = NotificationChannel(
					BLE_NOTIFICATION_CHANNEL_ID, "BLE", NotificationManager.IMPORTANCE_DEFAULT
				)
				val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
				notificationManager.createNotificationChannel(bleNotificationChannel)
				Notification.Builder(this, BLE_NOTIFICATION_CHANNEL_ID)
			}
			else -> Notification.Builder(this)
		}
		val notification = notificationBuilder.setContentTitle(getString(R.string.bt_notif_title))
			.setContentText(getString(R.string.bt_notif_txt))
			.setContentIntent(pendingIntent)
			.setSmallIcon(R.drawable.ic_bluetooth)
			.build()
		startForeground(FOREGROUND_NOTIFICATION_ID, notification)
	}

	private fun buildAdvertiseSettings() = AdvertiseSettings.Builder()
		.setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_POWER)
		.setTimeout(0).build()

	private fun buildAdvertiseData() = AdvertiseData.Builder()
		.addServiceUuid(ScanFilterService_UUID).setIncludeDeviceName(true).build()

	private fun sampleAdvertiseCallback() = object : AdvertiseCallback() {
		override fun onStartSuccess(settingsInEffect: AdvertiseSettings?) {
			super.onStartSuccess(settingsInEffect)
			Log.d(TAG, "Advertising successfully started")
		}

		override fun onStartFailure(errorCode: Int) {
			super.onStartFailure(errorCode)
			Log.d(TAG, "Advertising failed")
			broadcastFailureIntent(errorCode)
			stopSelf()
		}
	}

	private fun broadcastFailureIntent(errorCode: Int) {
		val failureIntent = Intent().setAction(ADVERTISING_FAILED).putExtra(
			BT_ADVERTISING_FAILED_EXTRA_CODE, errorCode
		)
		sendBroadcast(failureIntent)
	}

	private fun setTimeout() {
		handler = Handler(Looper.myLooper()!!)
		val runnable = Runnable {
			Log.d(TAG, "run: AdvertiserService has reached timeout of $TIMEOUT milliseconds, stopping advertising")
			broadcastFailureIntent(ADVERTISING_TIMED_OUT)
		}
		handler?.postDelayed(runnable, TIMEOUT)
	}

	private fun stopAdvertising() {
		if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADVERTISE) != PackageManager.PERMISSION_GRANTED) {
			// TODO: Consider calling
			//    ActivityCompat#requestPermissions
			// here to request the missing permissions, and then overriding
			//   public void onRequestPermissionsResult(int requestCode, String[] permissions,
			//                                          int[] grantResults)
			// to handle the case where the user grants the permission. See the documentation
			// for ActivityCompat#requestPermissions for more details.
			return
		}
		bluetoothLeAdvertiser?.stopAdvertising(advertiseCallback)
			.also { bluetoothLeAdvertiser = null }
	}

	override fun onDestroy() {
		running = false
		stopAdvertising()
		handler?.removeCallbacksAndMessages(null)
		stopForeground(true)
		super.onDestroy()
	}
}