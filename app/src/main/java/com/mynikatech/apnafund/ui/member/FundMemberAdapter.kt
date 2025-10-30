package com.mynikatech.apnafund.ui.member

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.mynikatech.apnafund.data.model.Users
import com.mynikatech.apnafund.databinding.ItemFundMemberBinding

class FundMemberAdapter(
    private val users: List<Users>,
    private val selectedUsers: MutableSet<Int>
) : RecyclerView.Adapter<FundMemberAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return ViewHolder(ItemFundMemberBinding.inflate(inflater, parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(users[position])
    }

    override fun getItemCount(): Int {
        return users.size
    }

    inner class ViewHolder(private val binding: ItemFundMemberBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(user: Users) {
            val fullName = "${user.firstName} ${user.lastName}"
            binding.checkboxMember.text = fullName
            binding.checkboxMember.isChecked = selectedUsers.contains(user.userId)

            // Avoid triggering listener when programmatically setting isChecked
            binding.checkboxMember.setOnCheckedChangeListener(null)

            binding.checkboxMember.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) selectedUsers.add(user.userId)
                else selectedUsers.remove(user.userId)
            }
        }
    }
}
