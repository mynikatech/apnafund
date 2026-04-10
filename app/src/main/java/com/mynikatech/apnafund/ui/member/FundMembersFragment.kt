package com.mynikatech.apnafund.ui.member

import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.mynikatech.apnafund.R
import com.mynikatech.apnafund.constants.ApnaBankConstants
import com.mynikatech.apnafund.data.model.FundMembers
import com.mynikatech.apnafund.data.model.Users
import com.mynikatech.apnafund.databinding.FragmentFundMembersBinding
import com.mynikatech.apnafund.ui.viewmodel.FundViewModel
import com.mynikatech.apnafund.ui.viewmodel.GroupViewModel
import com.mynikatech.apnafund.ui.viewmodel.UserViewModel
import com.mynikatech.apnafund.util.Converters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class FundMembersFragment : Fragment() {

    private lateinit var binding: FragmentFundMembersBinding
    private lateinit var tableLayoutFundMemberDetails: TableLayout

    private val groupViewModel: GroupViewModel by viewModels()
    private val fundViewModel: FundViewModel by viewModels()
    private val userViewModel: UserViewModel by viewModels()
    private var groupId: Int = -1
    private var dialog: BottomSheetDialog? = null

    private var fetchMembersJob: Job? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentFundMembersBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        /*(requireActivity() as AppCompatActivity).supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
            title = "Fund Member Details"  // Optional
        }*/
        arguments?.let {
            groupId = it.getInt("groupId")
        }
        val navArgsFundId = arguments?.let {
            FundMembersFragmentArgs.fromBundle(it).fundId
        }
        var fundId = 0

        if (navArgsFundId != null) {
            fetchAllMembers(navArgsFundId)
            fundId = navArgsFundId
        } else {
            // Fallback to something to be implemented

        }
        if (Converters.userHasPrivilege(ApnaBankConstants.ADD_FUND_MEMBERS_PRIV))
            binding.fundMembersFab.visibility = View.VISIBLE
        else
            binding.fundMembersFab.visibility = View.GONE
        binding.fundMembersFab.setOnClickListener {
            val action = FundMembersFragmentDirections
                .actionFundMembersFragmentToAddFundMembersFragment(
                    fundId = fundId,
                    groupId = groupId
                )
            findNavController().navigate(action)
        }
        val toolbar = view.findViewById<MaterialToolbar>(R.id.fund_member_details_toolbar)
        // (activity as? AppCompatActivity)?.setSupportActionBar(toolbar)
        //(activity as? AppCompatActivity)?.supportActionBar?.setDisplayShowTitleEnabled(false)

        // Enable back arrow
        val navController = findNavController()
        toolbar.setNavigationIcon(R.drawable.ic_back_arrow) // your back icon
        toolbar.setNavigationOnClickListener {
            navController.navigateUp()
        }
        toolbar.title = "Fund Member Details"
    }

    private fun fetchAllMembers(fundId: Int) {
        // Cancel any ongoing job if you still have that logic (optional now)
        fetchMembersJob?.cancel()
        fetchMembersJob = lifecycleScope.launch {
            val fundMembers = withContext(Dispatchers.IO) {
                fundViewModel.fetchFundMembersforFund(fundId)
            }
            val fundDetails = fundViewModel.getFundDetails(fundId)
            val fund = fundViewModel.fetchFund(fundId)
            if(null != fund)
                groupId = fund.groupId
            if (fundDetails.totalCurrentDeposit > 0)
                binding.fundMembersFab.visibility = View.GONE
            updateTable(fundId, fundMembers)
        }
    }

    private suspend fun updateTable(
        fundId: Int,
        fundMembers: List<FundMembers>
    ) = coroutineScope {
        // 1) Fetch everything off the main thread, concurrently where it helps
        val fundDeferred = async(Dispatchers.IO) { fundViewModel.fetchFund(fundId) }

        val usersDeferred = fundMembers.map { member ->
            async(Dispatchers.IO) { userViewModel.fetchUser(member.userId) } // returns Users?
        }

        val fund = fundDeferred.await()
        val groupDeferred = if (fund != null) {
            async(Dispatchers.IO) { groupViewModel.fetchGroup(fund.groupId ?: 0) } // Groups?
        } else null

        val moderatorDeferred = if (fund?.moderator != null) {
            async(Dispatchers.IO) { userViewModel.fetchUser(fund.moderator!!) } // Users?
        } else null

        val group = groupDeferred?.await()
        val moderatorUser: Users? = moderatorDeferred?.await()

        val users = usersDeferred.awaitAll() // List<Users?>

        // 2) Switch to main exactly once to update the UI
        withContext(Dispatchers.Main) {
            // clear table first
            cleanTable(binding.tableFundMembersDetails)

            // header info
            binding.textViewFundNameValue.text = fund?.fundName.orEmpty()
            binding.textViewGroupNameValue.text = Converters.formatWithBraces(group?.groupName.orEmpty())
            binding.textViewModeratorValue.text = Converters.formatUserName(
                moderatorUser?.firstName.orEmpty(),
                moderatorUser?.lastName.orEmpty()
            )

            val table = binding.tableFundMembersDetails

            fundMembers.forEachIndexed { index, member ->
                val user = users.getOrNull(index)
                val tvNo = TextView(activity).apply {
                    text = (index + 1).toString()
                    gravity = Gravity.CENTER
                }
                val tvMemberName = TextView(activity).apply {
                    text = Converters.formatUserName(user?.firstName.orEmpty(), user?.lastName.orEmpty())
                    gravity = Gravity.START
                }
                val tvJoiningDate = TextView(activity).apply {
                    text = member.joiningDate.orEmpty()
                    gravity = Gravity.START
                }

                val row = TableRow(activity).apply {
                    addView(tvNo)
                    addView(tvMemberName)
                    addView(tvJoiningDate)
                }
                table.addView(row)
            }
        }
    }

    private fun cleanTable(table: TableLayout) {
        while (table.childCount > 1) {
            table.removeViewAt(1)
        }
    }

    @Deprecated("Deprecated in Java", ReplaceWith("menu.clear()"))
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        menu.clear()
    }

    @Suppress("DEPRECATION")
    @Deprecated("Deprecated in Java")
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                requireActivity().onBackPressedDispatcher.onBackPressed()
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()

        fetchMembersJob?.cancel()
        dialog?.dismiss()
        dialog = null

        cleanTable(binding.tableFundMembersDetails)
        binding.textViewFundNameValue.text = ""
        binding.textViewGroupNameValue.text = ""
        binding.textViewModeratorValue.text = ""

        val activity = requireActivity() as AppCompatActivity
        activity.supportActionBar?.show()
    }
}