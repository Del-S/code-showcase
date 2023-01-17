package cz.csob.smartbanking.compose.core

import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import com.karumi.shot.ScreenshotTest
import cz.csob.smartbanking.compose.theme.CsobTheme
import org.junit.Assume.assumeTrue
import org.junit.Rule

/**
 * Base class for Screenshot testing. Classes that implement screenshot testing should extend this
 * class to trigger screenshots.
 *
 * Example usage of the test:
 *
 * ```kotlin
 * internal class CardsScreenshotTest : SmartScreenshotTest() {
 *
 *   @Test
 *   @ScreenshotTest
 *   @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
 *   fun testInfoCard() {
 *     testContent("default") { InfoCardPreview() }
 *   }
 * }
 * ```
 *
 * Create a test function annotated with `@Test` and `@ScreenshotTest` to make sure it is a
 * screenshot test. Both of these annotations need to be provided so the test is considered as a
 * screenshot test. And in the test function call `testContent` with name of the test/content with
 * composable being tested as a second parameter.
 *
 * Note: name of the test/content must be unique within the `CardsScreenshotTest` since it is used
 * to create the name of the screenshot which is composed of `className_name_theme`.
 *
 * Note2: you can provide `@SdkSuppress` to make sure it does not run on devices with lower API than
 * `Build.VERSION_CODES.O` since on those the test would always resulted with an exception.
 * Screenshot testing is not supported for APIs lower than Android O.
 *
 * @author eMan a.s.
 */
internal abstract class SmartScreenshotTest : ScreenshotTest {

    @get:Rule
    val composeTestRule: ComposeContentTestRule = createComposeRule()

    override fun compareScreenshot(rule: ComposeTestRule, name: String?) {
        assumeOreo()
        super.compareScreenshot(rule, name)
    }

    override fun compareScreenshot(node: SemanticsNodeInteraction, name: String?) {
        assumeOreo()
        super.compareScreenshot(node, name)
    }

    /**
     * Tests composable content using a [composeTestRule]. Sets the content wrapped in [CsobTheme]
     * to make sure components use the proper theme. Then it compares the screenshot using the rule
     * and [screenshotName].
     *
     * @param screenshotName name of the screenshot created, note that is must be unique withing one
     * test class since it is contained in the screenshot name (with the class name).
     * @param testTag allows to test composable with specific tag. It can be used to test
     * AlertDialog layout which cannot be tested the standard way. More info can be found here:
     * https://github.com/pedrovgs/Shot/issues/305.
     * @param composable content which is being tested
     */
    protected fun testContent(
        screenshotName: String,
        testTag: String? = null,
        composable: @Composable () -> Unit,
    ) {
        composeTestRule.apply {
            setContent { CsobTheme(content = composable) }
            testTag?.let {
                compareScreenshot(
                    node = this.onNodeWithTag(testTag = it, useUnmergedTree = true),
                    name = screenshotName
                )
            } ?: compareScreenshot(rule = this, name = screenshotName)
        }
    }

    /**
     * Forces test to crash on API lower than Oreo (26) since they would not work on lower APIs
     * anyway. If you want to skip the test completely on lower APIs you can use
     * `@SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)` annotation with the `@Test` annotation.
     */
    private fun assumeOreo() {
        assumeTrue(
            "Minimal API level for Compose Screenshot test is 26.",
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
        )
    }
}

