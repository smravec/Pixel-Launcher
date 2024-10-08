package de.jrpie.android.launcher.list

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Rect
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import androidx.viewpager.widget.ViewPager
import com.google.android.material.tabs.TabLayout
import de.jrpie.android.launcher.PREF_SCREEN_FULLSCREEN
import de.jrpie.android.launcher.R
import de.jrpie.android.launcher.REQUEST_UNINSTALL
import de.jrpie.android.launcher.UIObject
import de.jrpie.android.launcher.getPreferences
import de.jrpie.android.launcher.list.apps.ListFragmentApps
import de.jrpie.android.launcher.list.other.LauncherAction
import de.jrpie.android.launcher.list.other.ListFragmentOther
import de.jrpie.android.launcher.vibrantColor
import de.jrpie.android.launcher.databinding.ListBinding


var intendedChoosePause = false // know when to close

// TODO: Better solution for this intercommunication functionality (used in list-fragments)
var intention = ListActivity.ListActivityIntention.VIEW
var forGesture: String? = null

/**
 * The [ListActivity] is the most general purpose activity in Launcher:
 * - used to view all apps and edit their settings
 * - used to choose an app / intent to be launched
 *
 * The activity itself can also be chosen to be launched as an action.
 */
class ListActivity : AppCompatActivity(), UIObject {
    private lateinit var binding: ListBinding


    enum class ListActivityIntention(val titleResource: Int) {
        VIEW(R.string.list_title_view), /* view list of apps */
        PICK(R.string.list_title_pick)  /* choose app or action to associate to a gesture */
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialise layout
        binding = ListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.listSettings.setOnClickListener {
            LauncherAction.SETTINGS.launch(this@ListActivity)
        }

        // android:windowSoftInputMode="adjustResize" doesn't work in full screen.
        // workaround from https://stackoverflow.com/a/57623505
        this.window.decorView.viewTreeObserver.addOnGlobalLayoutListener {
            val r = Rect()
            window.decorView.getWindowVisibleDisplayFrame(r)
            val height: Int =
                binding.listContainer.context.resources.displayMetrics.heightPixels
            val diff = height - r.bottom
            if (diff != 0 && getPreferences(this).getBoolean(PREF_SCREEN_FULLSCREEN, false)) {
                if (binding.listContainer.paddingBottom !== diff) {
                    binding.listContainer.setPadding(0, 0, 0, diff)
                }
            } else {
                if (binding.listContainer.paddingBottom !== 0) {
                    binding.listContainer.setPadding(0, 0, 0, 0)
                }
            }
        }
    }

    override fun onStart(){
        super<AppCompatActivity>.onStart()
        super<UIObject>.onStart()
    }

    override fun onPause() {
        super.onPause()
        finish()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_UNINSTALL) {
            if (resultCode == Activity.RESULT_OK) {
                Toast.makeText(this, getString(R.string.list_removed), Toast.LENGTH_LONG).show()
                finish()
            } else if (resultCode == Activity.RESULT_FIRST_USER) {
                Toast.makeText(this, getString(R.string.list_not_removed), Toast.LENGTH_LONG).show()
                finish()
            }
        }
    }

    override fun applyTheme() {
        // list_close.setTextColor(vibrantColor)

        binding.listTabs.setSelectedTabIndicatorColor(vibrantColor)
    }

    override fun setOnClicks() {
        binding.listClose.setOnClickListener { finish() }
    }

    override fun adjustLayout() {
        // get info about which action this activity is open for
        intent.extras?.let { bundle ->
            intention = bundle.getString("intention")
                ?.let { ListActivityIntention.valueOf(it) }
                ?: ListActivityIntention.VIEW

            if (intention != ListActivityIntention.VIEW)
                forGesture = bundle.getString("forGesture")
        }

        // Hide tabs for the "view" action
        if (intention == ListActivityIntention.VIEW) {
            binding.listTabs.visibility = View.GONE
        }

        binding.listHeading.text = getString(intention.titleResource)

        val sectionsPagerAdapter = ListSectionsPagerAdapter(this, supportFragmentManager)
        val viewPager: ViewPager = findViewById(R.id.list_viewpager)
        viewPager.adapter = sectionsPagerAdapter
        val tabs: TabLayout = findViewById(R.id.list_tabs)
        tabs.setupWithViewPager(viewPager)
    }
}

private val TAB_TITLES = arrayOf(
    R.string.list_tab_app,
    R.string.list_tab_other
)

/**
 * The [ListSectionsPagerAdapter] returns the fragment,
 * which corresponds to the selected tab in [ListActivity].
 */
class ListSectionsPagerAdapter(private val context: Context, fm: FragmentManager)
    : FragmentPagerAdapter(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {

    override fun getItem(position: Int): Fragment {
        return when (position){
            0 -> ListFragmentApps()
            1 -> ListFragmentOther()
            else -> Fragment()
        }
    }

    override fun getPageTitle(position: Int): CharSequence {
        return context.resources.getString(TAB_TITLES[position])
    }

    override fun getCount(): Int {
        return when (intention) {
            ListActivity.ListActivityIntention.VIEW -> 1
            else -> 2
        }
    }
}
