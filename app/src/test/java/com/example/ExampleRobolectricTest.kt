package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.model.Contact
import com.example.util.T9Matcher
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Phone", appName)
  }

  @Test
  fun `t9 matcher matches contact name correctly`() {
    val contact = Contact(
      id = "1",
      name = "ABBA",
      phoneNumber = "01862959728"
    )
    val result = T9Matcher.match(contact, "22")
    assertNotNull(result)
  }

  @Test
  fun `t9 matcher matches phone number digits`() {
    val contact = Contact(
      id = "2",
      name = "Abir",
      phoneNumber = "+8801642342285"
    )
    val result = T9Matcher.match(contact, "22")
    assertNotNull(result)
  }
}
