package abhishek.jewellers.jewellerypricecalculator

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import java.util.UUID

class MainActivity : AppCompatActivity() {

    private val tabData = mutableListOf<TabItem>()
    private var mediator: TabLayoutMediator? = null
    private lateinit var adapter: FragmentStateAdapter

    data class TabItem(val id: String, var title: String)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        loadTabs()

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val tabLayout: TabLayout = findViewById(R.id.tabLayout)
        val viewPager: ViewPager2 = findViewById(R.id.viewPager)
        val addTabFab: FloatingActionButton = findViewById(R.id.addTabFab)
        val removeTabFab: FloatingActionButton = findViewById(R.id.removeTabFab)

        adapter = object : FragmentStateAdapter(this) {
            override fun getItemCount(): Int = tabData.size
            override fun createFragment(position: Int): Fragment = 
                CalculatorFragment.newInstance(tabData[position].id, tabData[position].title)
            override fun getItemId(position: Int): Long = tabData[position].id.hashCode().toLong()
            override fun containsItem(itemId: Long): Boolean = tabData.any { it.id.hashCode().toLong() == itemId }
        }

        viewPager.adapter = adapter

        setupMediator(tabLayout, viewPager)

        addTabFab.setOnClickListener {
            val nextItemNumber = tabData.size + 1
            val newTitle = "Item $nextItemNumber"
            tabData.add(TabItem(UUID.randomUUID().toString(), newTitle))
            saveTabs()
            adapter.notifyItemInserted(tabData.size - 1)
            
            refreshMediator(tabLayout, viewPager)
            
            viewPager.post {
                viewPager.setCurrentItem(tabData.size - 1, true)
            }
        }

        removeTabFab.setOnClickListener {
            if (tabData.size > 1) {
                val currentPosition = viewPager.currentItem
                showDeleteConfirmation(currentPosition, tabLayout, viewPager)
            } else {
                // Optional: Show message that last tab cannot be removed
            }
        }
    }

    private fun showDeleteConfirmation(position: Int, tabLayout: TabLayout, viewPager: ViewPager2) {
        AlertDialog.Builder(this)
            .setTitle(R.string.remove_tab)
            .setMessage("Are you sure you want to remove '${tabData[position].title}'?")
            .setPositiveButton("Remove") { _, _ ->
                tabData.removeAt(position)
                saveTabs()
                adapter.notifyItemRemoved(position)
                refreshMediator(tabLayout, viewPager)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun setupMediator(tabLayout: TabLayout, viewPager: ViewPager2) {
        mediator = TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = tabData[position].title
        }
        mediator?.attach()
    }

    private fun refreshMediator(tabLayout: TabLayout, viewPager: ViewPager2) {
        mediator?.detach()
        setupMediator(tabLayout, viewPager)
    }

    fun updateTabTitle(tabId: String, title: String) {
        val index = tabData.indexOfFirst { it.id == tabId }
        if (index != -1 && tabData[index].title != title) {
            tabData[index].title = title
            saveTabs()
            val tabLayout: TabLayout = findViewById(R.id.tabLayout)
            tabLayout.getTabAt(index)?.text = title
        }
    }

    private fun saveTabs() {
        val sharedPref = getSharedPreferences("JewelleryPrefs", MODE_PRIVATE)
        // Store as id|title pairs
        val encoded = tabData.joinToString(",") { "${it.id}|${it.title}" }
        sharedPref.edit { putString("tabs_v3", encoded) }
    }

    private fun loadTabs() {
        val sharedPref = getSharedPreferences("JewelleryPrefs", MODE_PRIVATE)
        val savedData = sharedPref.getString("tabs_v3", null)
        
        tabData.clear()
        savedData?.split(",")?.forEach {
            val parts = it.split("|")
            if (parts.size == 2) {
                tabData.add(TabItem(parts[0], parts[1]))
            }
        }
        
        if (tabData.isEmpty()) {
            // Migration from v2 or first run
            val oldTabs = sharedPref.getString("tabs_v2", "Item 1")
            oldTabs?.split(",")?.forEach {
                tabData.add(TabItem(UUID.randomUUID().toString(), it))
            }
        }
        
        if (tabData.isEmpty()) {
            tabData.add(TabItem(UUID.randomUUID().toString(), "Item 1"))
        }
    }
}
