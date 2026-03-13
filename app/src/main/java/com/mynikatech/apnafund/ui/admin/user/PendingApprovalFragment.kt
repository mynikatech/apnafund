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
import com.mynikatech.apnafund.net.dto.PendingApprovalDto
import com.mynikatech.apnafund.session.SessionManager
import com.mynikatech.apnafund.ui.viewmodel.ApprovalViewModel
import kotlinx.coroutines.launch

class PendingApprovalFragment : Fragment(), PendingApprovalAdapter.OnActionClickListener {

    private lateinit var binding: FragmentPendingApprovalBinding

    private val approvalViewModel: ApprovalViewModel by viewModels()

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
            val requests = approvalViewModel.getPendingApprovals(SessionManager.userId)
            adapter.updateList(requests)
        }
    }

    override fun onApproveClicked(item: PendingApprovalDto) {

        lifecycleScope.launch {

            when (item.entityType) {

                "GROUP" ->
                    approvalViewModel.approveGroup(item.approvalId, SessionManager.userId)

                "LOAN" ->
                    approvalViewModel.approveLoan(item.approvalId, SessionManager.userId)
            }

            Toast.makeText(requireContext(), "Approved", Toast.LENGTH_SHORT).show()
            observePendingRequests()
        }
    }

    override fun onRejectClicked(item: PendingApprovalDto) {

        lifecycleScope.launch {

            when (item.entityType) {

                "GROUP" ->
                    approvalViewModel.rejectGroup(item.approvalId, SessionManager.userId)

                "LOAN" ->
                    approvalViewModel.rejectLoan(item.approvalId, SessionManager.userId)
            }

            Toast.makeText(requireContext(), "Rejected", Toast.LENGTH_SHORT).show()
            observePendingRequests()
        }
    }
}
