package com.mindgate.app.activities

import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.mindgate.app.adapters.AppListAdapter
import com.mindgate.app.databinding.ActivityAppSelectionBinding
import com.mindgate.app.models.AppInfo
import com.mindgate.app.utils.AppPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AppSelectionActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAppSelectionBinding
    private lateinit var adapter: AppListAdapter

    // Single source of truth — all apps list, mutated in place by the adapter callback
    private val allApps = mutableListOf<AppInfo>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAppSelectionBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Select Apps to Monitor"

        setupRecyclerView()
        setupSearch()
        loadApps()

        binding.btnSave.setOnClickListener { saveSelection() }
        binding.btnSelectAll.setOnClickListener { selectAll() }
        binding.btnClearAll.setOnClickListener { clearAll() }
    }

    private fun setupRecyclerView() {
        adapter = AppListAdapter { app, isSelected ->
            // The adapter already mutated app.isSelected directly.
            // We just need to refresh the count label.
            updateSelectionCount()
        }
        binding.recyclerApps.layoutManager = LinearLayoutManager(this)
        binding.recyclerApps.adapter = adapter
        binding.recyclerApps.setHasFixedSize(true)
        // Prevent RecyclerView from losing scroll state when list updates
        binding.recyclerApps.itemAnimator = null
    }

    private fun setupSearch() {
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterApps(s?.toString() ?: "")
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun loadApps() {
        binding.progressLoading.visibility = View.VISIBLE
        binding.recyclerApps.visibility = View.GONE

        lifecycleScope.launch {
            val loaded = withContext(Dispatchers.IO) { loadInstalledApps() }
            allApps.clear()
            allApps.addAll(loaded)
            // Submit a snapshot — RecyclerView gets its own copy, adapter callbacks mutate allApps
            adapter.submitList(allApps.toList())
            binding.progressLoading.visibility = View.GONE
            binding.recyclerApps.visibility = View.VISIBLE
            updateSelectionCount()
        }
    }

    private fun loadInstalledApps(): List<AppInfo> {
        val pm = packageManager
        val selectedApps = AppPreferences.getSelectedApps(this)
        val myPackage = packageName

        return pm.getInstalledApplications(PackageManager.GET_META_DATA)
            .filter { app ->
                app.packageName != myPackage &&
                        pm.getLaunchIntentForPackage(app.packageName) != null
            }
            .mapNotNull { app ->
                try {
                    AppInfo(
                        packageName = app.packageName,
                        appName = pm.getApplicationLabel(app).toString(),
                        icon = pm.getApplicationIcon(app.packageName),
                        isSelected = selectedApps.contains(app.packageName)
                    )
                } catch (_: Exception) { null }
            }
            .sortedWith(compareByDescending<AppInfo> { it.isSelected }.thenBy { it.appName })
    }

    private fun filterApps(query: String) {
        // Filter allApps (which has current selection state) — do NOT reload from disk
        val filtered = if (query.isEmpty()) allApps.toList()
        else allApps.filter { it.appName.contains(query, ignoreCase = true) }
        adapter.submitList(filtered)
    }

    private fun updateSelectionCount() {
        val count = allApps.count { it.isSelected }
        binding.tvSelectionCount.text = "$count app${if (count == 1) "" else "s"} selected"
    }

    private fun selectAll() {
        allApps.forEach { it.isSelected = true }
        // Resubmit current filtered view with updated states
        val query = binding.etSearch.text.toString()
        filterApps(query)
        updateSelectionCount()
    }

    private fun clearAll() {
        allApps.forEach { it.isSelected = false }
        val query = binding.etSearch.text.toString()
        filterApps(query)
        updateSelectionCount()
    }

    private fun saveSelection() {
        // Read selection state from allApps (not the filtered adapter list)
        val selected = allApps.filter { it.isSelected }.map { it.packageName }.toSet()
        AppPreferences.setSelectedApps(this, selected)
        Toast.makeText(this, "${selected.size} app${if (selected.size == 1) "" else "s"} saved", Toast.LENGTH_SHORT).show()
        finish()
    }

    override fun onSupportNavigateUp(): Boolean { finish(); return true }
}
