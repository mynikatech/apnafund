package com.mynikatech.apnafund.ui.admin.user

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.mynikatech.apnafund.R
import com.mynikatech.apnafund.constants.ApnaBankConstants
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
            val requests = approvalViewModel.getPendingApprovalsWithDetails(SessionManager.userId)
            updateUI(requests)
        }
    }

    override fun onApproveClicked(item: PendingApprovalDto, position: Int) {

        lifecycleScope.launch {

            try {

                when (item.entityType) {

                    ApnaBankConstants.TEXT_GROUP ->
                        approvalViewModel.approveGroup(item.approvalId, SessionManager.userId)

                    ApnaBankConstants.TEXT_LOAN ->
                        approvalViewModel.approveLoan(item.approvalId, SessionManager.userId)

                    ApnaBankConstants.TEXT_LOAN_CLOSURE ->
                        approvalViewModel.approveLoanClosure(item.approvalId, SessionManager.userId)
                }
                val currentList = adapter.getItems().toMutableList()
                currentList.removeAt(position)
                updateUI(currentList)

                Toast.makeText(
                    requireContext(),
                    getString(R.string.text_approved), Toast.LENGTH_SHORT
                ).show()
                observePendingRequests()

            } catch (e: io.ktor.client.plugins.HttpRequestTimeoutException) {
                Log.e("PendingApprovalFragment", e.message.toString())
                Toast.makeText(
                    requireContext(),
                    getString(R.string.error_request_timed_out),
                    Toast.LENGTH_LONG
                ).show()
                adapter.notifyItemChanged(position)

            } catch (ex: Exception) {

                Log.e("PendingApprovalFragment", ex.message.toString())
                Toast.makeText(
                    requireContext(),
                    getString(R.string.error_server),
                    Toast.LENGTH_LONG
                ).show()
                adapter.notifyItemChanged(position)
            }
        }
    }

    override fun onRejectClicked(item: PendingApprovalDto, position: Int) {

        lifecycleScope.launch {
            try {

                when (item.entityType) {

                    ApnaBankConstants.TEXT_GROUP ->
                        approvalViewModel.rejectGroup(item.approvalId, SessionManager.userId)

                    ApnaBankConstants.TEXT_LOAN ->
                        approvalViewModel.rejectLoan(item.approvalId, SessionManager.userId)

                    ApnaBankConstants.TEXT_LOAN_CLOSURE ->
                        approvalViewModel.rejectLoanClosure(item.approvalId, SessionManager.userId)
                }
                val currentList = adapter.getItems().toMutableList()
                currentList.removeAt(position)
                updateUI(currentList)

                Toast.makeText(
                    requireContext(),
                    getString(R.string.text_rejected), Toast.LENGTH_SHORT
                ).show()
                observePendingRequests()
            } catch (e: io.ktor.client.plugins.HttpRequestTimeoutException) {
                Log.e("PendingApprovalFragment", e.message.toString())
                Toast.makeText(
                    requireContext(),
                    getString(R.string.error_request_timed_out),
                    Toast.LENGTH_LONG
                ).show()
                adapter.notifyItemChanged(position)

            } catch (e: Exception) {
                Log.e("PendingApprovalFragment", e.message.toString())
                Toast.makeText(
                    requireContext(),
                    getString(R.string.error_server),
                    Toast.LENGTH_LONG
                ).show()
                adapter.notifyItemChanged(position)
            }
        }
    }

    private fun updateUI(list: List<PendingApprovalDto>) {
        adapter.updateList(list)

        val isEmpty = list.isEmpty()

        binding.recyclerPendingApproval.visibility =
            if (isEmpty) View.GONE else View.VISIBLE

        binding.layoutEmpty.visibility =
            if (isEmpty) View.VISIBLE else View.GONE
    }
}
