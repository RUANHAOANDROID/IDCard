package com.hao.idcardlibrary

import android.graphics.Bitmap

/**
 * ID card info
 * @date 2022/10/9
 * @author hao
 * @since v1.0
 */
sealed class IDCardInfo {
    data class Chinese(
        val name: String,
        val sex: String,
        val nationality: String,
        val birthday: String,
        val address: String,
        val number: String,
        val signedDepartment: String,
        val effectiveDate: String,
        val expiryDate: String,
        var headImage: Bitmap?
    ) : IDCardInfo()

    data class Other(
        val name: String,
        val sex: String,
        val nationality: String,
        val birthday: String,
        val address: String,
        val number: String,
        val signedDepartment: String,
        val effectiveDate: String,
        val expiryDate: String,
        var headImage: Bitmap?
    ) : IDCardInfo()
}

