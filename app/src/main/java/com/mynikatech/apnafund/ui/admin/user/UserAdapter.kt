package com.mynikatech.apnafund.ui.admin.user

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.mynikatech.apnafund.R
import com.mynikatech.apnafund.constants.ApnaBankConstants
import com.mynikatech.apnafund.data.model.UserWithGroup
import com.mynikatech.apnafund.databinding.ItemUserRowBinding
import com.mynikatech.apnafund.util.Converters

class UserAdapter(
    private val onEdit: (UserWithGroup) -> Unit,
    private val onToggle: (UserWithGroup) -> Unit
) : RecyclerView.Adapter<UserAdapter.UserViewHolder>() {

    private var items: List<UserWithGroup> = emptyList()

    inner class UserViewHolder(val binding: ItemUserRowBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(user: UserWithGroup, position: Int) {

            // Serial number
            binding.textSerial.text = "${position + 1}"

            // Text fields (same as your createTableCell)
            binding.textFirstName.text = user.firstName
            binding.textLastName.text = user.lastName
            binding.textEmail.text = user.emailId
            binding.textPhone.text = user.phoneNumber
            binding.textGroup.text = user.groupName

            // Tooltip (you had this earlier)
            ViewCompat.setTooltipText(binding.textEmail, user.emailId)

            // Edit button
            binding.buttonEdit.setOnClickListener {
                onEdit(user)
            }

            binding.buttonEdit.visibility =
                if (Converters.userHasPrivilege(ApnaBankConstants.ADD_USER_PRIV))
                    View.VISIBLE else View.GONE

            // Status toggle
            val drawable =
                if (user.status == ApnaBankConstants.STATUS_ACTIVE)
                    R.drawable.ic_block
                else
                    R.drawable.ic_check_circle

            binding.buttonStatus.setImageResource(drawable)

            binding.buttonStatus.setOnClickListener {
                onToggle(user)
            }

            // Alternate row background (IMPORTANT)
            binding.root.setBackgroundResource(
                if (position % 2 == 0)
                    R.drawable.table_row_white_bg
                else
                    R.drawable.table_row_gray_bg
            )
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val binding = ItemUserRowBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return UserViewHolder(binding)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        holder.bind(items[position], position)
    }

    override fun getItemCount() = items.size

    fun updateList(newItems: List<UserWithGroup>) {
        val diff = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
            override fun getOldListSize() = items.size
            override fun getNewListSize() = newItems.size

            override fun areItemsTheSame(oldPos: Int, newPos: Int) =
                items[oldPos].userId == newItems[newPos].userId

            override fun areContentsTheSame(oldPos: Int, newPos: Int) =
                items[oldPos] == newItems[newPos]
        })

        items = newItems
        diff.dispatchUpdatesTo(this)
    }
}