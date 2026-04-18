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
import com.mindgate.app.adapters.RedirectAppAdapter
import com.mindgate.app.databinding.ActivityRedirectAppPickerBinding
import com.mindgate.app.models.AppInfo
import com.mindgate.app.utils.AppPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class RedirectAppPickerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRedirectAppPickerBinding
    private lateinit var adapter: RedirectAppAdapter
    private var allApps = mutableListOf<AppInfo>()
    private var selectedPackage: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRedirectAppPickerBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Choose Alternative App"

        selectedPackage = AppPreferences.getRedirectApp(this)

        adapter = RedirectAppAdapter(selectedPackage) { pkg ->
            selectedPackage = pkg
            updateSaveButton()
        }
        binding.recyclerApps.layoutManager = LinearLayoutManager(this)
        binding.recyclerApps.adapter = adapter
        binding.recyclerApps.itemAnimator = null

        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filter(s?.toString() ?: "")
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        binding.btnClearSelection.setOnClickListener {
            selectedPackage = null
            adapter.setSelected(null)
            updateSaveButton()
        }

        binding.btnSave.setOnClickListener {
            AppPreferences.setRedirectApp(this, selectedPackage)
            val msg = if (selectedPackage == null) "Alternative app cleared"
            else "Alternative app saved"
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
            finish()
        }

        loadApps()
        updateSaveButton()
    }

    private fun loadApps() {
        binding.progressLoading.visibility = View.VISIBLE
        lifecycleScope.launch {
            val apps = withContext(Dispatchers.IO) {
                packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
                    .filter { app ->
                        app.packageName != packageName &&
                        packageManager.getLaunchIntentForPackage(app.packageName) != null
                    }
                    .mapNotNull { app ->
                        try {
                            AppInfo(
                                packageName = app.packageName,
                                appName = packageManager.getApplicationLabel(app).toString(),
                                icon = packageManager.getApplicationIcon(app.packageName),
                                isSelected = app.packageName == selectedPackage
                            )
                        } catch (_: Exception) { null }
                    }
                    .sortedBy { it.appName }
            }
            allApps.clear()
            allApps.addAll(apps)
            adapter.submitList(allApps.toList())
            binding.progressLoading.visibility = View.GONE
        }
    }

    private fun filter(query: String) {
        val filtered = if (query.isEmpty()) allApps.toList()
        else allApps.filter { it.appName.contains(query, ignoreCase = true) }
        adapter.submitList(filtered)
    }

    private fun updateSaveButton() {
        val name = selectedPackage?.let {
            try { packageManager.getApplicationLabel(packageManager.getApplicationInfo(it, 0)).toString() }
            catch (_: Exception) { null }
        }
        binding.tvCurrentSelection.text = if (name != null) "Selected: $name" else "No app selected"
    }

    override fun onSupportNavigateUp(): Boolean { finish(); return true }
}
