package com.hao.idcardlibrary.zkt

import android.content.Context
import android.graphics.Bitmap
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.util.Log
import com.hao.idcardlibrary.IDCardDevice
import com.hao.idcardlibrary.IDCardInfo
import com.zkteco.android.IDReader.IDPhotoHelper
import com.zkteco.android.IDReader.WLTService
import com.zkteco.android.biometric.core.device.ParameterHelper
import com.zkteco.android.biometric.core.device.TransportType
import com.zkteco.android.biometric.core.utils.LogHelper
import com.zkteco.android.biometric.module.idcard.IDCardReader
import com.zkteco.android.biometric.module.idcard.IDCardReaderFactory
import com.zkteco.android.biometric.module.idcard.IDCardType
import com.zkteco.android.biometric.module.idcard.exception.IDCardReaderException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * ZKT id180
 * @date 2022/10/9
 * @author hao
 * @since v1.0
 */
class ID180(private val context: Context) : IDCardDevice {
    private var zkusbManager: ZKUSBManager? = null
    private var bStarted = false
    private var bCancel = false
    private var bRepeatRead = false
    private var countDownLatch: CountDownLatch? = null

    companion object {
        const val VID = 1024 //IDR VID
        const val PID = 50010 //IDR PID
        const val TAG = "ZKT_ID180"
    }

    private var idCardReader: IDCardReader? = null

    private val zkusbManagerListener by lazy {
        object : ZKUSBManagerListener {
            override fun onCheckPermission(result: Int) {
                openDevice()
                Log.d(TAG, "onCheckPermission:检查权限")
            }


            override fun onUSBArrived(device: UsbDevice?) {
                Log.d(TAG, "onUSBArrived:发现阅读器接入")
            }

            override fun onUSBRemoved(device: UsbDevice?) {
                //setResult("阅读器USB被拔出")
                Log.d(TAG, "onUSBRemoved: 阅读器USB被拔出")
            }
        }
    }

    init {
        initDevice()
    }

    private fun initDevice() {
        zkusbManager = ZKUSBManager(context, zkusbManagerListener)
        zkusbManager?.registerUSBPermissionReceiver()
    }

    private var readListener: ((Int, String, IDCardInfo?) -> Unit)? = null
    override fun start(call: (Int, String, IDCardInfo?) -> Unit) {
        if (!enumSensor()) {
            Log.d(TAG, "start: 找不到设备")
            return
        }
        readListener = call
        zkusbManager?.initUSBPermission(VID, PID)
    }

    override fun stop() {
        Log.d(TAG, "stop: 停止读卡")
        closeDevice()
    }

    private fun enumSensor(): Boolean {
        val usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager
        for (device in usbManager.deviceList.values) {
            if (device.vendorId == VID && device.productId == PID) {
                return true
            }
        }
        return false
    }

    private fun openDevice() {
        startIDCardReader()
        try {
            idCardReader?.open(0)
            countDownLatch = CountDownLatch(1)
            Thread {
                while (!bCancel) {
                    bCancel = false;
                    Thread.sleep(500)
                    val nTickstart = System.currentTimeMillis()
                    try {
                        idCardReader?.findCard(0)
                        idCardReader?.selectCard(0)
                    } catch (e: IDCardReaderException) {
                        if (!bRepeatRead) {
                            readListener?.let { it(-1, "重复读卡", null) }
                            continue//重复读
                        }
                    }
                    var cardType = 0
                    cardType = try {
                        idCardReader!!.readCardEx(0, 0)
                    } catch (e: IDCardReaderException) {
                        Log.d(TAG, "openDevice: 读卡失败，错误信息 ${e.message} ")
                        readListener?.let { it(-1, "读卡失败", null) }
                        continue
                    }
                    if (cardType == IDCardType.TYPE_CARD_SFZ || cardType == IDCardType.TYPE_CARD_PRP || cardType == IDCardType.TYPE_CARD_GAT) {
                        val nTickCommuUsed = System.currentTimeMillis() - nTickstart
                        Log.d(TAG, "openDevice: cardType ${cardType}")
                        if (cardType == IDCardType.TYPE_CARD_SFZ || cardType == IDCardType.TYPE_CARD_GAT) {
                            val idCardInfo = idCardReader!!.lastIDCardInfo
                            val name = idCardInfo.name
                            val sex = idCardInfo.sex
                            val nation = idCardInfo.nation
                            val born = idCardInfo.birth
                            val licid = idCardInfo.id
                            val depart = idCardInfo.depart
                            val expireDate = idCardInfo.validityTime
                            val addr = idCardInfo.address
                            val passNo = idCardInfo.passNum
                            val visaTimes = idCardInfo.visaTimes
                            var bmpPhoto: Bitmap? = null
                            if (idCardInfo.photolength > 0) {
                                val buf = ByteArray(WLTService.imgLength)
                                if (1 == WLTService.wlt2Bmp(idCardInfo.photo, buf)) {
                                    bmpPhoto = IDPhotoHelper.Bgr2Bitmap(buf)
                                }
                            }
                            val chinese = IDCardInfo.Chinese(
                                name = name,
                                number = licid,
                                nationality = nation,
                                sex = sex,
                                birthday = born,
                                address = addr,
                                signedDepartment = depart,
                                effectiveDate = visaTimes.toString(),
                                expiryDate = expireDate,
                                headImage = bmpPhoto
                            )
                            readListener?.let { it(0, "读卡成功", chinese) }
                        } else {
                            val idprpCardInfo = idCardReader!!.lastPRPIDCardInfo
                            val cnName = idprpCardInfo.cnName
                            val enName = idprpCardInfo.enName
                            val sex = idprpCardInfo.sex
                            val country =
                                idprpCardInfo.country + "/" + idprpCardInfo.countryCode //国家/国家地区代码

                            val born = idprpCardInfo.birth
                            val licid = idprpCardInfo.id
                            val expireDate = idprpCardInfo.validityTime
                            val depart = "公安部"
                            var bmpPhoto: Bitmap? = null
                            if (idprpCardInfo.photolength > 0) {
                                val buf = ByteArray(WLTService.imgLength)
                                if (1 == WLTService.wlt2Bmp(idprpCardInfo.photo, buf)) {
                                    bmpPhoto = IDPhotoHelper.Bgr2Bitmap(buf)
                                }
                            }
                            Log.d(TAG, "openDevice: idprpCardInfo $idprpCardInfo")
                            val other = IDCardInfo.Other(
                                name = cnName,
                                number = licid,
                                nationality = country,
                                sex = sex,
                                birthday = born,
                                address = country,
                                signedDepartment = depart,
                                effectiveDate = expireDate,
                                expiryDate = expireDate,
                                headImage = bmpPhoto
                            )
                            readListener?.let { it(0, "读卡成功", other) }
                        }
                    }
                }
                countDownLatch?.countDown()
            }.start()
            bStarted = true
            Log.d(TAG, "openDevice:打开设备成功，SAMID: ${idCardReader!!.getSAMID(0)}  ")
        } catch (e: IDCardReaderException) {
            Log.d(TAG, "openDevice:打开设备失败 ")
            e.printStackTrace()
        }
    }

    private fun startIDCardReader() {
        if (null != idCardReader) {
            IDCardReaderFactory.destroy(idCardReader)
            idCardReader = null
        }
        // Define output log level
        LogHelper.setLevel(Log.VERBOSE)
        // Start fingerprint sensor
        val idrparams: MutableMap<String, Any> = HashMap()
        idrparams[ParameterHelper.PARAM_KEY_VID] = VID
        idrparams[ParameterHelper.PARAM_KEY_PID] = PID
        idCardReader = IDCardReaderFactory.createIDCardReader(context, TransportType.USB, idrparams)
        idCardReader?.SetBaudRate(115200)
    }

    private fun closeDevice() {
        if (bStarted) {
            bCancel = true
            if (null != countDownLatch) {
                try {
                    countDownLatch?.await((2 * 1000).toLong(), TimeUnit.MILLISECONDS)
                } catch (e: InterruptedException) {
                    e.printStackTrace()
                }
                countDownLatch = null
            }
            try {
                idCardReader!!.close(0)
            } catch (e: IDCardReaderException) {
                e.printStackTrace()
            }
            bStarted = false
        }
        zkusbManager?.unRegisterUSBPermissionReceiver()
        zkusbManager == null
    }
}