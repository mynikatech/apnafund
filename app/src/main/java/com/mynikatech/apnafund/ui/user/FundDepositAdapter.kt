package com.mynikatech.apnafund.ui.user

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.mynikatech.apnafund.data.model.Deposits
import com.mynikatech.apnafund.databinding.ItemUserDepositBinding
import com.mynikatech.apnafund.util.Converters

class FundDepositAdapter(private val deposits: List<Deposits>) :
    RecyclerView.Adapter<FundDepositAdapter.DepositViewHolder>() {

    private lateinit var context: Context
    override fun getItemCount() = deposits.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DepositViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        context = parent.context
        return DepositViewHolder(ItemUserDepositBinding.inflate(inflater, parent, false))
    }

    override fun onBindViewHolder(holder: DepositViewHolder, position: Int) {
        val deposit = deposits[position]
        holder.bind(deposit)
    }

    inner class DepositViewHolder(private val binding: ItemUserDepositBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(deposit: Deposits) {
            binding.apply {
                textDate.text = deposit.depositedDate
                textAmount.text = Converters.formatCurrency(deposit.depositAmount)
                if (deposit.lateFee != null && deposit.lateFee >= 0)
                    textLateFee.text = Converters.formatCurrency(deposit.lateFee)
                else
                    textLateFee.text = ""
            }
        }

    }

}
