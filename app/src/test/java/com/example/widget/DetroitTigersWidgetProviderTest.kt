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
        // Pixel 10 Fold inner screen standard 2-row height (e.g. minWidth 380dp, minHeight 100dp)
        provider.applyResponsiveLayout(views, minWidth = 380, minHeight = 100, context = context)
        assertNotNull(views)

        val frame = android.widget.FrameLayout(context)
        val view = views.apply(context, frame) as android.view.ViewGroup
        val standings = view.findViewById<android.view.View>(R.id.widget_standings_table)
        val pitcher = view.findViewById<android.view.View>(R.id.widget_stadium_pitcher_info)
        val matchup = view.findViewById<android.view.View>(R.id.widget_matchup_layout)
        val header = view.findViewById<android.view.View>(R.id.widget_header_layout)

        org.junit.Assert.assertEquals("Header must be VISIBLE in 3x2 inner screen", android.view.View.VISIBLE, header.visibility)
        org.junit.Assert.assertEquals("Matchup must be VISIBLE in 3x2 inner screen", android.view.View.VISIBLE, matchup.visibility)
        org.junit.Assert.assertEquals("Pitcher info must be VISIBLE in 3x2 inner screen", android.view.View.VISIBLE, pitcher.visibility)
        org.junit.Assert.assertEquals("Standings table (last line) must be VISIBLE in 3x2 inner screen", android.view.View.VISIBLE, standings.visibility)
    }

    @Test
    fun testApplyResponsiveLayout_Pixel10FoldFrontScreen3x2() {
        val views = RemoteViews(context.packageName, R.layout.detroit_tigers_widget_layout)
        // Pixel 10 Fold front screen standard 3x2 (e.g. minWidth 190dp, minHeight 110dp)
        provider.applyResponsiveLayout(views, minWidth = 190, minHeight = 110, context = context)
        assertNotNull(views)

        val frame = android.widget.FrameLayout(context)
        val view = views.apply(context, frame) as android.view.ViewGroup
        val standings = view.findViewById<android.view.View>(R.id.widget_standings_table)
        val pitcher = view.findViewById<android.view.View>(R.id.widget_stadium_pitcher_info)
        val matchup = view.findViewById<android.view.View>(R.id.widget_matchup_layout)

        org.junit.Assert.assertEquals("Matchup must be VISIBLE in 3x2 front screen", android.view.View.VISIBLE, matchup.visibility)
        org.junit.Assert.assertEquals("Pitcher info must be VISIBLE in 3x2 front screen", android.view.View.VISIBLE, pitcher.visibility)
        org.junit.Assert.assertEquals("Standings table must be VISIBLE in 3x2 front screen", android.view.View.VISIBLE, standings.visibility)
    }

    @Test
    fun testApplyResponsiveLayout_VariousGridSizes() {
        // Test common Android Launcher grid sizes: 3x2, 4x2, 5x2, 3x3, 4x3, 2x2
        val gridConfigs = listOf(
            Triple(180, 110, "3x2 Standard"),
            Triple(260, 110, "4x2 Wide"),
            Triple(340, 110, "5x2 Ultra-Wide"),
            Triple(180, 180, "3x3 Tall"),
            Triple(260, 180, "4x3 Large"),
            Triple(140, 95, "2x2 Compact"),
            Triple(380, 105, "Pixel 10 Fold Inner 3x2"),
            Triple(500, 105, "Pixel 10 Fold Inner 4x2")
        )

        for ((width, height, name) in gridConfigs) {
            val views = RemoteViews(context.packageName, R.layout.detroit_tigers_widget_layout)
            provider.applyResponsiveLayout(views, minWidth = width, minHeight = height, context = context)
            val frame = android.widget.FrameLayout(context)
            val view = views.apply(context, frame) as android.view.ViewGroup
            val standings = view.findViewById<android.view.View>(R.id.widget_standings_table)
            val matchup = view.findViewById<android.view.View>(R.id.widget_matchup_layout)

            org.junit.Assert.assertEquals("$name: Matchup must be VISIBLE", android.view.View.VISIBLE, matchup.visibility)
            if (height >= 90) {
                org.junit.Assert.assertEquals("$name: Standings must be VISIBLE when height >= 90dp", android.view.View.VISIBLE, standings.visibility)
            }
        }
    }

    @Test
    fun testApplyResponsiveLayout_CompactDimensions() {
        val views = RemoteViews(context.packageName, R.layout.detroit_tigers_widget_layout)
        provider.applyResponsiveLayout(views, minWidth = 120, minHeight = 90, context = context)
        assertNotNull(views)
    }

    @Test
    fun testAllWidgetThemes_fromIndexAndDrawables() {
        for (i in 0 until 6) {
            val theme = WidgetTheme.fromIndex(i)
            assertNotNull(theme)
            assertNotNull(theme.displayName)
            assertNotNull(theme.buttonLabel)
            val views = RemoteViews(context.packageName, R.layout.detroit_tigers_widget_layout)
            views.setInt(R.id.widget_root, "setBackgroundResource", theme.bgDrawableRes)
            views.setInt(R.id.widget_tag, "setBackgroundResource", theme.tagDrawableRes)
            views.setInt(R.id.widget_theme_toggle, "setBackgroundResource", theme.tagDrawableRes)
            provider.applyResponsiveLayout(views, minWidth = 180, minHeight = 110, context = context)
            assertNotNull(views)
        }
    }
}
