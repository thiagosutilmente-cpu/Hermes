package com.example

import com.example.coordinator.RadarSettings
import org.junit.Assert.*
import org.junit.Test

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun testRadarSettingsAutoRejectDefaultsAndCustomValues() {
    val settings = RadarSettings()
    assertFalse(settings.isAutoRejectEnabled)
    assertEquals(10.0, settings.autoRejectMinFare, 0.001)

    val customSettings = settings.copy(
      isAutoRejectEnabled = true,
      autoRejectMinFare = 15.5
    )
    assertTrue(customSettings.isAutoRejectEnabled)
    assertEquals(15.5, customSettings.autoRejectMinFare, 0.001)
  }
}
