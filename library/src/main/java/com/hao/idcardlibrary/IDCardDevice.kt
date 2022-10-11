package com.hao.idcardlibrary

/**
 * 身份证模块
 * @date 2022/10/9
 * @author hao
 * @since v1.0
 */
interface IDCardDevice {
    /**
     *
     *开始读卡
     * @param call (status:Int message:String info:IDCardInfo?)
     */
    fun start(call: (Int, String, IDCardInfo?) -> Unit)

    /**
     * 停止读卡
     *
     */
    fun stop()
}