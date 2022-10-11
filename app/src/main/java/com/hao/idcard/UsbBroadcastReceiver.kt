package com.hao.idcard

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.Build
import android.os.Parcelable
import android.util.Log
import androidx.annotation.RequiresApi

/**
 * USB 插拔监听
 * @date 2022/10/9
 * @author 锅得铁
 * @since v1.0
 */
class UsbBroadcastReceiver : BroadcastReceiver() {
    private val TAG = "UsbBroadcastReceiver"

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        val device =
            intent.getParcelableExtra<Parcelable>(UsbManager.EXTRA_DEVICE) as UsbDevice? ?: return
        when (action) {
            UsbManager.ACTION_USB_DEVICE_ATTACHED -> {
                if (isZKTDevice(device)) {
                    Log.d(TAG, "onReceive: attached zkt")
                    restart(context)
                }
                if (isRouton(device)) {
                    Log.d(TAG, "onReceive:  attached routon")
                    restart(context)
                }
            }
            UsbManager.ACTION_USB_DEVICE_DETACHED -> {
                if (isZKTDevice(device)) {
                    Log.d(TAG, "onReceive: detached zkt")
                }
                if (isRouton(device)) {
                    Log.d(TAG, "onReceive: detached routon")
                }
            }

        }
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private fun isRouton(device: UsbDevice): Boolean {
        val productName = device.manufacturerName ?: return false
        return productName.contains("Routon", true)
    }


    private fun isZKTDevice(device: UsbDevice): Boolean {
        return (device.vendorId == 1024 && device.productId == 50010)
    }


    private fun restart(context: Context) {
        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
        intent?.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        context.startActivity(intent)
        //杀掉以前进程
        android.os.Process.killProcess(android.os.Process.myPid())
    }
}