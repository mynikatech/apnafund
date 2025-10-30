package com.mynikatech.apnafund.ui.admin.user

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.mynikatech.apnafund.databinding.FragmentPendingApprovalBinding
import com.mynikatech.apnafund.ui.viewmodel.UserViewModel
import com.mynikatech.apnafund.util.EmailUtils
import kotlinx.coroutines.launch

class PendingApprovalFragment : Fragment(), PendingApprovalAdapter.OnActionClickListener {

    private lateinit var binding: FragmentPendingApprovalBinding
    private val viewModel: UserViewModel by viewModels()
    private lateinit var adapter: PendingApprovalAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentPendingApprovalBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = PendingApprovalAdapter(emptyList(), this)
        binding.recyclerPendingApproval.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerPendingApproval.adapter = adapter

        observePendingRequests()
    }

    private fun observePendingRequests() {
        lifecycleScope.launch {
            val requests = viewModel.getPendingModeratorRequests()
            adapter.updateList(requests)
            //binding..visibility = if (requests.isEmpty()) View.VISIBLE else View.GONE

        }
    }

    override fun onApproveClicked(userId: Int, roleId: Int, groupId: Int) {
        lifecycleScope.launch {
            viewModel.approveModeratorAndGroup(userId, roleId, groupId)
            Toast.makeText(requireContext(), "Moderator Approved", Toast.LENGTH_SHORT).show()
            observePendingRequests()
        }
    }

    override fun onRejectClicked(userId: Int, roleId: Int, groupId: Int) {
        lifecycleScope.launch {
            //to do send email to the user of rejection and to connect with Support for more details
            val user = viewModel.fetchUser(userId)
            val emailId = user?.emailId
            if (null != emailId)
                EmailUtils.sendEmail(
                    emailId,
                    "Apna Fund: Moderator Request Rejected",
                    "The request was not approved, please connect with App Support",
                    requireContext()
                )
            viewModel.rejectModeratorAndGroup(userId, roleId, groupId)
            Toast.makeText(requireContext(), "Moderator Rejected", Toast.LENGTH_SHORT).show()
            observePendingRequests()
        }
    }
}
