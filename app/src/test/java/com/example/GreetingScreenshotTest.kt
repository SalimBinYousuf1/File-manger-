package com.example

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.example.data.model.StorageCategoryBreakdown
import com.example.ui.components.SalimStorageBar
import com.example.ui.theme.SalimTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [34])
class GreetingScreenshotTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun greeting_screenshot() {
    composeTestRule.setContent {
      SalimTheme {
        SalimStorageBar(
          breakdown = StorageCategoryBreakdown(
            imagesBytes = 12L * 1024 * 1024 * 1024,
            videosBytes = 25L * 1024 * 1024 * 1024,
            documentsBytes = 4L * 1024 * 1024 * 1024,
            totalBytes = 128L * 1024 * 1024 * 1024,
            usedBytes = 60L * 1024 * 1024 * 1024,
            freeBytes = 68L * 1024 * 1024 * 1024
          )
        )
      }
    }

    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/greeting.png")
  }
}
