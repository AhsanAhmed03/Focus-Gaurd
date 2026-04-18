package com.mindgate.app.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.mindgate.app.databinding.ItemRedirectAppBinding
import com.mindgate.app.models.AppInfo

class RedirectAppAdapter(
    private var selectedPkg: String?,
    private val onSelected: (String?) -> Unit
) : ListAdapter<AppInfo, RedirectAppAdapter.VH>(Diff()) {

    fun setSelected(pkg: String?) { selectedPkg = pkg; notifyDataSetChanged() }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(ItemRedirectAppBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(getItem(position))

    inner class VH(private val b: ItemRedirectAppBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(app: AppInfo) {
            b.tvAppName.text = app.appName
            b.tvPackageName.text = app.packageName
            b.ivAppIcon.setImageDrawable(app.icon)
            b.radioButton.isChecked = app.packageName == selectedPkg
            val select = {
                val prev = selectedPkg
                selectedPkg = if (prev == app.packageName) null else app.packageName
                onSelected(selectedPkg)
                notifyDataSetChanged()
            }
            b.root.setOnClickListener { select() }
            b.radioButton.setOnClickListener { select() }
        }
    }

    class Diff : DiffUtil.ItemCallback<AppInfo>() {
        override fun areItemsTheSame(a: AppInfo, b: AppInfo) = a.packageName == b.packageName
        override fun areContentsTheSame(a: AppInfo, b: AppInfo) = a.appName == b.appName
    }
}
