package com.mynikatech.apnafund.ui.member

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.mynikatech.apnafund.data.model.Users
import com.mynikatech.apnafund.databinding.ItemFundMemberBinding

class FundMemberAdapter(
    val items: MutableList<FundMemberSelection>
) : RecyclerView.Adapter<FundMemberAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return ViewHolder(ItemFundMemberBinding.inflate(inflater, parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int {
        return items.size
    }

    inner class ViewHolder(private val binding: ItemFundMemberBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: FundMemberSelection) {

            val fullName = item.displayName

            // REMOVE old listeners first (very important)
            binding.checkboxMember.setOnCheckedChangeListener(null)
            binding.checkboxModerator.setOnCheckedChangeListener(null)

            // Set values
            binding.checkboxMember.text = fullName
            binding.checkboxMember.isChecked = item.isSelected
            binding.checkboxModerator.isChecked = item.isModerator

            // Enable moderator only if selected
            binding.checkboxModerator.isEnabled = item.isSelected

            // Member checkbox logic
            binding.checkboxMember.setOnCheckedChangeListener { _, isChecked ->
                item.isSelected = isChecked

                binding.checkboxModerator.isEnabled = isChecked

                if (!isChecked) {
                    binding.checkboxModerator.isChecked = false
                    item.isModerator = false
                }
            }

            // Moderator checkbox logic
            binding.checkboxModerator.setOnCheckedChangeListener { _, isChecked ->
                item.isModerator = isChecked
            }
        }
    }
    fun getSelectedMembers(): List<FundMemberSelection> {
        return items.filter { it.isSelected }
    }
}
