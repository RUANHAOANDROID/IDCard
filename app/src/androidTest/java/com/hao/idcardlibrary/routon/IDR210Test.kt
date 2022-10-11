package com.hao.idcardlibrary.routon

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith


/**
 * TODO
 * @date 2022/10/9
 * @author hao
 * @since v1.0
 */
@RunWith(AndroidJUnit4::class)
internal class IDR210Test {


    @Test
    fun idr210Test() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val id210 = IDR210(context)
        id210.start { status, message, idCard ->
            println("$status - $message - ${idCard.toString()}")
            idCard?.let {
                id210.stop()
                Assert.assertNotNull(idCard)
            }
        }
        Thread.sleep(10000)
    }
}