package com.mindgate.app.activities

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.SeekBar
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.mindgate.app.databinding.ActivityCustomizeBinding
import com.mindgate.app.models.DefaultQuotes
import com.mindgate.app.utils.AppPreferences

class CustomizeScreenActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCustomizeBinding
    private var selectedImageUri: String? = null

    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            try {
                contentResolver.takePersistableUriPermission(
                    it, Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: Exception) {}
            selectedImageUri = it.toString()
            binding.ivPreviewImage.setImageURI(it)
            binding.tvImageStatus.text = "✓ Custom image selected"
            binding.btnRemoveImage.isEnabled = true
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCustomizeBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Customize Screen"
        loadCurrentSettings()
        setupUI()
    }

    private fun loadCurrentSettings() {
        val content = AppPreferences.getScreenContent(this)
        binding.etTitle.setText(content.title)
        binding.etQuote.setText(content.quote)
        selectedImageUri = content.imageUri
        binding.seekbarDuration.progress = (content.durationSeconds - 3).coerceAtLeast(0)
        updateDurationLabel(content.durationSeconds)

        if (!content.imageUri.isNullOrEmpty()) {
            try {
                binding.ivPreviewImage.setImageURI(Uri.parse(content.imageUri))
                binding.tvImageStatus.text = "✓ Custom image set"
                binding.btnRemoveImage.isEnabled = true
            } catch (_: Exception) {
                binding.tvImageStatus.text = "No image selected"
            }
        }
    }

    private fun setupUI() {
        binding.seekbarDuration.max = 27
        binding.seekbarDuration.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                updateDurationLabel(progress + 3)
            }
            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {}
        })

        binding.btnSelectImage.setOnClickListener {
            imagePickerLauncher.launch(arrayOf("image/*"))
        }

        binding.btnRemoveImage.setOnClickListener {
            selectedImageUri = null
            binding.ivPreviewImage.setImageDrawable(null)
            binding.tvImageStatus.text = "No image selected"
            binding.btnRemoveImage.isEnabled = false
        }

        setupQuoteSuggestions()

        binding.btnPreview.setOnClickListener {
            saveSettings()
            startActivity(Intent(this, MindGateOverlayActivity::class.java).apply {
                putExtra(MindGateOverlayActivity.EXTRA_TRIGGER_TYPE, "APP_LAUNCH")
            })
        }

        binding.btnSave.setOnClickListener {
            if (saveSettings()) {
                Toast.makeText(this, "Settings saved!", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private fun setupQuoteSuggestions() {
        val quotes = DefaultQuotes.quotes.map {
            "${it.text}${if (it.author.isNotEmpty()) " — ${it.author}" else ""}"
        }
        binding.listQuotes.adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, quotes)
        binding.listQuotes.setOnItemClickListener { _, _, position, _ ->
            val q = DefaultQuotes.quotes[position]
            val full = "${q.text}${if (q.author.isNotEmpty()) "\n— ${q.author}" else ""}"
            binding.etQuote.setText(full)
            binding.etQuote.setSelection(full.length)
        }
    }

    private fun updateDurationLabel(seconds: Int) {
        binding.tvDurationLabel.text = "Display Duration: ${seconds}s"
    }

    private fun saveSettings(): Boolean {
        val title = binding.etTitle.text.toString().trim()
        val quote = binding.etQuote.text.toString().trim()
        val duration = binding.seekbarDuration.progress + 3
        if (title.isEmpty()) { binding.etTitle.error = "Title cannot be empty"; return false }
        if (quote.isEmpty()) { binding.etQuote.error = "Quote cannot be empty"; return false }
        AppPreferences.setScreenTitle(this, title)
        AppPreferences.setScreenQuote(this, quote)
        AppPreferences.setScreenImageUri(this, selectedImageUri)
        AppPreferences.setScreenDuration(this, duration)
        return true
    }

    override fun onSupportNavigateUp(): Boolean { finish(); return true }
}
