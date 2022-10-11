package com.hao.idcardlibrary.routon

import android.content.Context
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.util.Log
import com.hao.idcardlibrary.IDCardDevice
import com.hao.idcardlibrary.IDCardInfo
import com.routon.plsy.reader.sdk.common.ErrorCode
import com.routon.plsy.reader.sdk.common.Info
import com.routon.plsy.reader.sdk.intf.IReader
import com.routon.plsy.reader.sdk.usb.USBImpl

/**
 * TODO
 * @date 2022/10/11
 * @author 锅得铁
 * @since v1.0
 */
class IDR210(private val context: Context) : IDCardDevice {
    companion object {
        const val TAG = "IDR210"
    }

    private var readListener: ((Int, String, IDCardInfo?) -> Unit)? = null

    private val readThread by lazy {
        Thread {
            while (true) {
                Thread.sleep(500)
                reader ?: continue
                readListener ?: continue
                //找卡
                val searchStatus: Int = reader!!.SDT_FindIDCard(ByteArray(8))
                Log.d(TAG, "searchStatus: $searchStatus")
                readListener?.let { it(searchStatus, "找卡失败", null) }
                if (searchStatus != 0) continue
                //选卡
                val selectStatus: Int = reader!!.SDT_SelectIDCard(ByteArray(8))
                Log.d(TAG, "selectStatus: $selectStatus")
                readListener?.let { it(selectStatus, "选卡失败", null) }
                if (selectStatus != 0) continue
                //读卡
                val idCard = Info.IDCardInfo()
                var readStatus = reader!!.RTN_ReadBaseMsg(idCard)
                readListener?.let { it(selectStatus, "读卡失败", null) }
                Log.d(TAG, "readStatus: $readStatus")
                if (readStatus != 0) continue
                if (readStatus == ErrorCode.SUCCESS) {
                    //读到卡 停顿1秒
                    //绿灯亮灭一次，蜂鸣器响一声 100ms
                    reader!!.RTN_TypeABeepLed(3, 0, 100)
                    readListener?.let {
                        val chinese = IDCardInfo.Chinese(
                            name = idCard.name,
                            number = idCard.id,
                            nationality = idCard.nation,
                            sex = idCard.gender,
                            birthday = idCard.birthday,
                            address = idCard.address,
                            signedDepartment = idCard.agency,
                            effectiveDate = idCard.expireStart,
                            expiryDate = idCard.expireEnd,
                            headImage = idCard.photo
                        )
                        it(readStatus, "读到卡", chinese)
                    }
                    Thread.sleep(500)
                    Log.d(TAG, "card info: ${idCard.name}")
                }
            }
        }
    }

    var reader: IReader? = null
    val usbManager by lazy { context.getSystemService(Context.USB_SERVICE) as UsbManager }
    private val usbDevPermissionMgr by lazy {
        UsbDevPermissionMgr(context, object : UsbDevPermissionMgr.UsbDevPermissionMgrCallback {
            override fun onNoticeStr(notice: String?) {}
            override fun onUsbDevReady(device: UsbDevice?) {
                Log.d(TAG, "onUsbDevReady: ")
                reader = USBImpl()
                val openPortStatus = reader!!.SDT_OpenPort(usbManager, device)
                if (openPortStatus >= ErrorCode.SUCCESS) {
                    Log.d(TAG, "onUsbDevReady: 打开USB成功")
                }
            }

            override fun onUsbDevRemoved(device: UsbDevice?) {
                println("移除了")
            }

            override fun onUsbRequestPermission() {
                println("授权中")
            }
        })
    }

    init {
        initIRD210()
    }

    private fun initIRD210() {
        //初始化USB设备对象
    }

    override fun start(call: (Int, String, IDCardInfo?) -> Unit) {
        //检查是否读卡器是否已连接
        val connected = usbDevPermissionMgr.initMgr()
        if (connected) {
            readListener = call
            readThread.start()
        }
    }

    override fun stop() {
        readThread.stop()
        reader?.let {
            it.SDT_ClosePort()
        }
    }

}