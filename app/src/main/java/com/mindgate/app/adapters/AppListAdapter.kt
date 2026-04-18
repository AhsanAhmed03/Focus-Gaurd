package com.mindgate.app.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.mindgate.app.databinding.ItemAppBinding
import com.mindgate.app.models.AppInfo

class AppListAdapter(
    private val onSelectionChanged: (AppInfo, Boolean) -> Unit
) : ListAdapter<AppInfo, AppListAdapter.AppViewHolder>(AppDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppViewHolder {
        val binding = ItemAppBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return AppViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AppViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class AppViewHolder(private val binding: ItemAppBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(app: AppInfo) {
            binding.tvAppName.text = app.appName
            binding.tvPackageName.text = app.packageName
            binding.ivAppIcon.setImageDrawable(app.icon)

            // BUG FIX: Must null the listener BEFORE setting isChecked, otherwise
            // RecyclerView recycling triggers the old listener with the wrong app reference,
            // causing items to be unchecked when scrolling.
            binding.switchApp.setOnCheckedChangeListener(null)
            binding.switchApp.isChecked = app.isSelected

            binding.switchApp.setOnCheckedChangeListener { _, isChecked ->
                // Update the model directly so the state survives recycling
                app.isSelected = isChecked
                onSelectionChanged(app, isChecked)
            }

            binding.root.setOnClickListener {
                val newState = !app.isSelected
                app.isSelected = newState
                binding.switchApp.isChecked = newState
                onSelectionChanged(app, newState)
            }
        }
    }

    class AppDiffCallback : DiffUtil.ItemCallback<AppInfo>() {
        override fun areItemsTheSame(oldItem: AppInfo, newItem: AppInfo): Boolean =
            oldItem.packageName == newItem.packageName

        override fun areContentsTheSame(oldItem: AppInfo, newItem: AppInfo): Boolean =
            oldItem.isSelected == newItem.isSelected && oldItem.appName == newItem.appName
    }
}
