package cz.csob.smartbanking.compose

import InfoDialogPreview
import TEST_TAG_INFO_DIALOG
import cz.csob.smartbanking.compose.core.ScreenshotTest
import cz.csob.smartbanking.compose.core.SmartScreenshotTest
import org.junit.Test

/**
 * Screenshot test for Dialogs.kt.
 *
 * @author eMan a.s.
 */
internal class DialogsScreenshotTest : SmartScreenshotTest()  {

    @Test
    @ScreenshotTest
    fun testInfoDialog() {
        testContent(
            screenshotName = "InfoDialog",
            testTag = TEST_TAG_INFO_DIALOG
        ) { InfoDialogPreview() }
    }
}

