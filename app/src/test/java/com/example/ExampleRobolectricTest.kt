package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.CallRepository
import com.example.model.CallType
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

  @Test
  fun `consecutive calls to same number group together and split after different number`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val repository = CallRepository(context)
    repository.clearCallHistory()

    // 1. Call Number A
    repository.recordCall("Alice", "01711111111", CallType.OUTGOING)
    assertEquals(1, repository.callHistory.value.size)
    assertEquals(1, repository.callHistory.value[0].count)

    // 2. Call Number A again (consecutive) -> still 1 entry in list, count increments to 2
    repository.recordCall("Alice", "01711111111", CallType.OUTGOING)
    assertEquals(1, repository.callHistory.value.size)
    assertEquals(2, repository.callHistory.value[0].count)

    // 3. Call Number A a 3rd time (consecutive) -> count increments to 3
    repository.recordCall("Alice", "01711111111", CallType.OUTGOING)
    assertEquals(1, repository.callHistory.value.size)
    assertEquals(3, repository.callHistory.value[0].count)

    // 4. Call Number B (different number) -> creates a separate 2nd entry at top
    repository.recordCall("Bob", "01822222222", CallType.OUTGOING)
    assertEquals(2, repository.callHistory.value.size)
    assertEquals("Bob", repository.callHistory.value[0].name)
    assertEquals(1, repository.callHistory.value[0].count)
    assertEquals("Alice", repository.callHistory.value[1].name)
    assertEquals(3, repository.callHistory.value[1].count)

    // 5. Call Number A again after B -> creates a 3rd entry in the list!
    repository.recordCall("Alice", "01711111111", CallType.OUTGOING)
    assertEquals(3, repository.callHistory.value.size)
    assertEquals("Alice", repository.callHistory.value[0].name)
    assertEquals(1, repository.callHistory.value[0].count)
    assertEquals("Bob", repository.callHistory.value[1].name)
    assertEquals("Alice", repository.callHistory.value[2].name)
    assertEquals(3, repository.callHistory.value[2].count)
  }
}
