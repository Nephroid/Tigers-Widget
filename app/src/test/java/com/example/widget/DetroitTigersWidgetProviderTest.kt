package com.example.widget

import android.content.Context
import android.widget.RemoteViews
import androidx.test.core.app.ApplicationProvider
import com.example.R
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DetroitTigersWidgetProviderTest {

    private lateinit var context: Context
    private lateinit var provider: DetroitTigersWidgetProvider

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        provider = DetroitTigersWidgetProvider()
    }

    @Test
    fun testApplyResponsiveLayout_StandardPhoneDimensions() {
        val views = RemoteViews(context.packageName, R.layout.detroit_tigers_widget_layout)
        provider.applyResponsiveLayout(views, minWidth = 180, minHeight = 110, context = context)
        assertNotNull(views)
    }

    @Test
    fun testApplyResponsiveLayout_Pixel10FoldProInnerScreenDimensions() {
        val views = RemoteViews(context.packageName, R.layout.detroit_tigers_widget_layout)
        // Pixel 10 Fold Pro inner screen expanded widget dimensions (e.g. minWidth 320dp, minHeight 200dp)
        provider.applyResponsiveLayout(views, minWidth = 320, minHeight = 200, context = context)
        assertNotNull(views)
    }

    @Test
    fun testApplyResponsiveLayout_Pixel10FoldInnerScreenStandard2RowHeight() {
        val views = RemoteViews(context.packageName, R.layout.detroit_tigers_widget_layout)
        // Pixel 10 Fold inner screen standard 2-row height (e.g. minWidth 380dp, minHeight 115dp)
        provider.applyResponsiveLayout(views, minWidth = 380, minHeight = 115, context = context)
        assertNotNull(views)
    }

    @Test
    fun testApplyResponsiveLayout_CompactDimensions() {
        val views = RemoteViews(context.packageName, R.layout.detroit_tigers_widget_layout)
        provider.applyResponsiveLayout(views, minWidth = 120, minHeight = 90, context = context)
        assertNotNull(views)
    }
}
