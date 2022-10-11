package com.hao.idcard

import android.content.Context
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.annotation.RequiresApi
import com.hao.idcard.databinding.ActivityMainBinding
import com.hao.idcardlibrary.IDCardDevice
import com.hao.idcardlibrary.IDCardInfo
import com.hao.idcardlibrary.routon.IDR210
import com.hao.idcardlibrary.zkt.ID180

class MainActivity : AppCompatActivity() {
    private var idCardDevice: IDCardDevice? = null
    private val readListener: (Int, String, IDCardInfo?) -> Unit = { a, b, c ->
        runOnUiThread {
            c?.let {
                it as IDCardInfo.Chinese
                binding.imgPhoto.setImageBitmap(it.headImage)
            }

        }
    }
    private val binding: ActivityMainBinding by lazy { ActivityMainBinding.inflate(layoutInflater) }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        (getSystemService(Context.USB_SERVICE) as UsbManager).deviceList.values.forEach {
            if (isRouton(it)) {
                idCardDevice = IDR210(this)
            }
            if (isZKTDevice(it)) {
                idCardDevice = ID180(this)
            }
        }
        binding.btnStart.setOnClickListener {
            idCardDevice?.start(readListener)
        }
        binding.btnStop.setOnClickListener {
            try {
                idCardDevice?.stop()
            } catch (e: Exception) {
                e.printStackTrace()
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
}