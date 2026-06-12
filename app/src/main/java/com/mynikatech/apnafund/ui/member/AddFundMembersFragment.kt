package com.mynikatech.apnafund.ui.member

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.appbar.MaterialToolbar
import com.mynikatech.apnafund.R
import com.mynikatech.apnafund.databinding.FragmentAddFundMembersBinding
import com.mynikatech.apnafund.net.dto.FundMembersDto
import com.mynikatech.apnafund.session.SessionManager
import com.mynikatech.apnafund.ui.viewmodel.FundSharedViewModel
import com.mynikatech.apnafund.ui.viewmodel.FundViewModel
import com.mynikatech.apnafund.util.ApnaBankDate
import kotlinx.coroutines.launch
import kotlin.getValue

class AddFundMembersFragment : Fragment() {
    private lateinit var adapter: FundMemberAdapter
    private lateinit var binding: FragmentAddFundMembersBinding
    private val fundViewModel: FundViewModel by viewModels()
    private val fundSharedViewModel: FundSharedViewModel by activityViewModels()

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
        // Enable back arrow
        val navController = findNavController()
        toolbar.setNavigationIcon(R.drawable.ic_back_arrow) // your back icon
        toolbar.setNavigationOnClickListener {
            navController.navigateUp()
        }
        toolbar.title = getString(R.string.text_add_fund_members)

        lifecycleScope.launch {
            try {
                val users = fundViewModel.getAvailableFundMembers(groupId, fundId)

                Log.d("AddFundMembersFragment", "The number of available users are: ${users.size}")
                binding.recyclerFundMembers.layoutManager = LinearLayoutManager(requireContext())
                val items = users.map {
                    FundMemberSelection(
                        userId = it.userId,
                        displayName = "${it.firstName} ${it.lastName}"
                    )
                }.toMutableList()
                adapter = FundMemberAdapter(items)
                binding.recyclerFundMembers.adapter = adapter
            } catch (e: Exception) {
                Log.e("AddFundMembers", "Error", e)
                Toast.makeText(context, "Failed to load members", Toast.LENGTH_SHORT).show()
            }

            binding.checkboxSelectAll.setOnCheckedChangeListener { _, isChecked ->

                adapter.items.forEach {
                    it.isSelected = isChecked
                    if (!isChecked) it.isModerator = false
                }

                adapter.notifyDataSetChanged()
            }

            binding.buttonSaveFundMember.setOnClickListener {
                binding.buttonSaveFundMember.isEnabled = true
                binding.buttonSaveFundMember.text = getString(R.string.button_saving_progress)
                lifecycleScope.launch {
                    try {
                        val selectedMembers = adapter.getSelectedMembers()

                        if (selectedMembers.isEmpty()) {
                            Toast.makeText(
                                context,
                                getString(R.string.message_no_member_add),
                                Toast.LENGTH_SHORT
                            ).show()
                            return@launch
                        }

                        val payload = selectedMembers.map {
                            FundMembersDto(
                                userId = it.userId,
                                fundId = fundId,
                                joiningDate = ApnaBankDate.getCurrentDate(), // or your helper
                                role = if (it.isModerator) "MODERATOR" else "MEMBER",
                                updatedBy = SessionManager.userId
                            )
                        }

                        fundViewModel.addFundMembersBatch(fundId, payload)
                        fundSharedViewModel.shouldForceRefreshFundDetails = true
                        fundSharedViewModel.refreshFunds(fundId)

                        Toast.makeText(
                            context,
                            getString(R.string.message_member_added),
                            Toast.LENGTH_SHORT
                        ).show()
                        findNavController().navigateUp()
                    } catch (e: Exception) {

                        Log.e("FundMember", "Error saving fund member", e)
                        binding.buttonSaveFundMember.isEnabled = true
                        binding.buttonSaveFundMember.text = getString(R.string.text_save_button)

                        Toast.makeText(
                            requireContext(),
                            getString(R.string.message_unexpected_error),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
            binding.buttonCancelFundMember.setOnClickListener {
                findNavController().navigateUp()
            }
        }
    }
}