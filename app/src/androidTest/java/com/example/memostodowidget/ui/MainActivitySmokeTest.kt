package com.example.memostodowidget.ui

import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainActivitySmokeTest {
    @Test
    fun publishToggleAndArchiveTemporaryMemo() {
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        val context = ApplicationProvider.getApplicationContext<Context>()
        val launchIntent = context.packageManager
            .getLaunchIntentForPackage(context.packageName)!!
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(launchIntent)

        assertTrue(device.wait(Until.hasObject(By.res(context.packageName, "publish_button")), TIMEOUT))

        val marker = "codex smoke ${System.currentTimeMillis()}"
        val content = "- [ ] $marker"
        device.findObject(By.res(context.packageName, "memo_content_input")).text = content
        device.findObject(By.res(context.packageName, "publish_button")).click()

        val unchecked = By.text("☐ $marker")
        assertTrue(device.wait(Until.hasObject(unchecked), LONG_TIMEOUT))
        device.findObject(unchecked).click()

        val checked = By.text("☑ $marker")
        assertTrue(device.wait(Until.hasObject(checked), LONG_TIMEOUT))

        val archiveButtons = device.findObjects(By.text("归档"))
        assertTrue(archiveButtons.isNotEmpty())
        archiveButtons.first().click()

        assertTrue(device.wait(Until.gone(checked), LONG_TIMEOUT))
    }

    private companion object {
        const val TIMEOUT = 5_000L
        const val LONG_TIMEOUT = 20_000L
    }
}
