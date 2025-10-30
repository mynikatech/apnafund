package com.mynikatech.apnafund.ui.member

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.appbar.MaterialToolbar
import com.mynikatech.apnafund.R
import com.mynikatech.apnafund.databinding.FragmentAddFundMembersBinding
import com.mynikatech.apnafund.ui.viewmodel.FundViewModel
import kotlinx.coroutines.launch

class AddFundMembersFragment : Fragment() {
    private lateinit var adapter: FundMemberAdapter
    private lateinit var binding: FragmentAddFundMembersBinding
    private val selectedUserIds = mutableSetOf<Int>()
    private val fundViewModel: FundViewModel by viewModels()

    override fun onCreateView(

        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentAddFundMembersBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        var fundId = -1
        var groupId = -1
        arguments?.let {
            fundId = it.getInt("fundId")
            groupId = it.getInt("groupId")
        }

        val toolbar = view.findViewById<MaterialToolbar>(R.id.fund_add_member_toolbar)
        (activity as? AppCompatActivity)?.setSupportActionBar(toolbar)
        (activity as? AppCompatActivity)?.supportActionBar?.setDisplayShowTitleEnabled(false)

        // Enable back arrow
        val navController = findNavController()
        toolbar.setNavigationIcon(R.drawable.ic_back_arrow) // your back icon
        toolbar.setNavigationOnClickListener {
            navController.navigateUp()
        }
        toolbar.title = "Add Fund Member"

        lifecycleScope.launch {
            val users = fundViewModel.getAvailableFundMembers(groupId, fundId)
            Log.d("Sunil", "The number of available users are: ${users.size}")
            binding.recyclerFundMembers.layoutManager = LinearLayoutManager(requireContext())
            adapter = FundMemberAdapter(users, selectedUserIds)
            binding.recyclerFundMembers.adapter = adapter

            binding.checkboxSelectAll.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    selectedUserIds.clear()
                    selectedUserIds.addAll(users.map { it.userId })
                } else {
                    selectedUserIds.clear()
                }
                adapter.notifyDataSetChanged()
            }

            binding.buttonSaveFundMember.setOnClickListener {
                lifecycleScope.launch {
                    if(selectedUserIds.toList().isEmpty()){
                        Toast.makeText(context, "No Members to add", Toast.LENGTH_SHORT).show()
                        return@launch
                    }
                    fundViewModel.addFundMembers(fundId, selectedUserIds.toList())
                    Toast.makeText(context, "Members added", Toast.LENGTH_SHORT).show()
                    findNavController().navigateUp()
                }
            }
            binding.buttonCancelFundMember.setOnClickListener {
                findNavController().navigateUp()
            }
        }
    }
}