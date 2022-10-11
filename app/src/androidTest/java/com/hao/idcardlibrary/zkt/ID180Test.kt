package com.hao.idcardlibrary.zkt

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
internal class ID180Test {

    @Test
    fun zktID180TEST() {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        val iD180 = ID180(appContext)
        iD180.start { a, b, c ->
            println(a)
            println(b)
            println(c)
            c?.let {
                iD180.stop()
                Assert.assertNotNull(c)
            }
        }
        Thread.sleep(10000)
    }
}