package com.karthikkilari.expiryradar

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.work.Configuration
import androidx.work.testing.SynchronousExecutor
import androidx.work.testing.WorkManagerTestInitHelper
import com.karthikkilari.expiryradar.data.*
import com.karthikkilari.expiryradar.ui.*
import java.time.LocalDate
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = TestRadarApplication::class)
@LooperMode(LooperMode.Mode.PAUSED)
class UiFlowTest {
    @get:Rule val compose = createComposeRule()
    private val today = LocalDate.of(2027, 8, 15)

    @Test
    fun homeShowsDatabaseDerivedCountsAndOpensItem() {
        var opened = ""
        val item = Item(name = "Milk", expiry = today.plusDays(1).toString())
        compose.setContent {
            MaterialTheme {
                Home(listOf(item), Preferences(), today, { opened = it.name }, {}, {}, {}, {})
            }
        }
        compose.onAllNodesWithText("Milk").onLast().assertIsDisplayed().performClick()
        compose.runOnIdle { Assert.assertEquals("Milk", opened) }
        compose.onNodeWithText("Expires tomorrow").assertIsDisplayed()
    }

    @Test
    fun itemSearchFiltersRealRecords() {
        val a = Item(name = "Milk", expiry = today.toString())
        val b = Item(name = "Face cream", expiry = today.toString())
        compose.setContent {
            MaterialTheme { Items(listOf(a, b), emptyList(), Preferences(), today, "All", {}) }
        }
        compose.onNodeWithText("Search name, barcode or notes").performTextInput("Milk")
        compose.onAllNodesWithText("Milk").onLast().assertIsDisplayed()
        compose.onNodeWithText("Face cream").assertDoesNotExist()
    }

    @Test
    fun calendarShowsSelectedDateItems() {
        compose.setContent {
            MaterialTheme {
                CalendarScreen(
                    listOf(Item(name = "Warranty", expiry = today.toString())),
                    emptyList(),
                    Preferences(),
                    today,
                    {},
                )
            }
        }
        compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("Warranty"))
        compose.onNodeWithText("Warranty").assertIsDisplayed()
        compose
            .onNode(hasScrollToIndexAction())
            .performScrollToNode(hasContentDescription("Next month"))
        compose.onNodeWithContentDescription("Next month").performClick()
        compose.onNodeWithText("September 2027").assertIsDisplayed()
    }

    @Test
    fun onboardingCompletesOnlyAtLastPage() {
        var complete = false
        compose.setContent { MaterialTheme { Onboarding { complete = true } } }
        compose.onNodeWithText("Get Started").performClick()
        compose.runOnIdle { Assert.assertFalse(complete) }
        compose.onNodeWithText("Continue").performClick()
        compose.onNodeWithText("Start tracking").performClick()
        compose.runOnIdle { Assert.assertTrue(complete) }
    }

    @Test
    fun navigationAndItemSaveFlow() {
        val app = RuntimeEnvironment.getApplication() as TestRadarApplication
        WorkManagerTestInitHelper.initializeTestWorkManager(
            app,
            Configuration.Builder().setExecutor(SynchronousExecutor()).build(),
        )
        runBlocking {
            app.repository.dao.clearItems()
            app.repository.seed()
            app.preferences.set(Preferences(onboarded = true))
        }
        lateinit var vm: RadarViewModel
        compose.runOnIdle { vm = RadarViewModel(app) }
        compose.setContent { RadarApp(vm, null) }
        compose.waitUntil(15000) {
            compose.onAllNodesWithText("Items").fetchSemanticsNodes().isNotEmpty()
        }
        fun tab(name: String) =
            compose.onNode(
                hasText(name) and SemanticsMatcher.keyIsDefined(SemanticsProperties.Selected)
            )
        tab("Items").performClick()
        compose.onNodeWithText("Your items").assertIsDisplayed()
        tab("Calendar").performClick()
        compose.onNodeWithText("Expiry calendar").assertIsDisplayed()
        tab("Insights").performClick()
        compose.onNodeWithText("Your insights").assertIsDisplayed()
        tab("Settings").performClick()
        compose.onNodeWithText("Offline, private, and yours.").assertIsDisplayed()
        tab("Home").performClick()
        compose.onNodeWithContentDescription("Add item").performClick()
        compose.onNodeWithText("Product name *").performTextInput("UI saved milk")
        compose.onNodeWithText("Expiry date *: Not set").performScrollTo().performClick()
        compose
            .onNodeWithText(
                LocalDate.now()
                    .format(
                        java.time.format.DateTimeFormatter.ofPattern(
                            "MMMM d, yyyy",
                            java.util.Locale.US,
                        )
                    ),
                substring = true,
            )
            .performClick()
        compose.onNodeWithText("Confirm").performClick()
        compose.onNodeWithText("Save Item").performScrollTo().performClick()
        compose.waitUntil(15000) {
            compose.onAllNodesWithText("Home").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("UI saved milk"))
        compose.onNodeWithText("UI saved milk").assertIsDisplayed()
        Assert.assertEquals("UI saved milk", runBlocking { app.repository.dao.all().single().name })
    }
}
