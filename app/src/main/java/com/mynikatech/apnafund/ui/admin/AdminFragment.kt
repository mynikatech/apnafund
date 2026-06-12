package com.mynikatech.apnafund.ui.admin

import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.google.android.material.color.MaterialColors
import com.google.android.material.tabs.TabLayoutMediator
import com.mynikatech.apnafund.R
import com.mynikatech.apnafund.databinding.FragmentAdminBinding
import com.mynikatech.apnafund.session.SessionManager
import com.mynikatech.apnafund.ui.admin.feedback.AdminFeedbackFragment
import com.mynikatech.apnafund.ui.admin.group.GroupFragment
import com.mynikatech.apnafund.ui.admin.loan.LoanEmiEntryFragment
import com.mynikatech.apnafund.ui.admin.user.PendingApprovalFragment
import com.mynikatech.apnafund.ui.admin.user.UserFragment
import com.mynikatech.apnafund.ui.deposit.DepositEntryFragment
import androidx.fragment.app.activityViewModels
import com.mynikatech.apnafund.ui.viewmodel.AdminSharedViewModel

class AdminFragment : Fragment() {

    private lateinit var binding: FragmentAdminBinding

    private lateinit var adminPagerAdapter: PagerAdapter

    private val sharedAdminViewModel: AdminSharedViewModel by activityViewModels()

    private var tabToGo: String? = ""

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding = FragmentAdminBinding.inflate(layoutInflater)
        arguments?.let {
            tabToGo = it.getString("tab")
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val isAdmin = SessionManager.isAdmin()
        val allTabs = listOf(
            getString(R.string.text_user),
            getString(R.string.text_group),
            getString(R.string.text_pending_approval),
            getString(R.string.text_deposits),
            getString(R.string.text_loan_emi),
            getString(R.string.tab_feedback)
        )

        val moderatorTabs = listOf(
            getString(R.string.text_user),
            getString(R.string.text_group),
            getString(R.string.text_pending_approval),
            getString(R.string.text_deposits),
            getString(R.string.text_loan_emi)
        )

        setupTabs()

        sharedAdminViewModel.refreshTabs.observe(viewLifecycleOwner) {
            val currentTab = binding.adminPager.currentItem

            setupTabs()

            if (currentTab < adminPagerAdapter.itemCount) {
                binding.adminPager.setCurrentItem(currentTab, false)
            }
        }

        binding.adminTabs.post {
            for (i in 0 until binding.adminTabs.tabCount) {
                val tab = binding.adminTabs.getTabAt(i)
                val tv = tab?.customView as? TextView

                tv?.setTextColor(
                    MaterialColors.getColor(
                        requireContext(),
                        com.google.android.material.R.attr.colorOnSurface,
                        Color.BLACK
                    )
                )
            }
        }
    }

    private fun setupTabs() {
        val tabsToShow = mutableListOf<String>()

        if (SessionManager.canManageUsers()) {
            tabsToShow.add(getString(R.string.text_user))
        }

        if (SessionManager.canManageGroups()) {
            tabsToShow.add(getString(R.string.text_group))
        }

        if (SessionManager.canViewPendingApprovals()) {
            tabsToShow.add(getString(R.string.text_pending_approval))
        }

        if (SessionManager.canManageDeposits()) {
            tabsToShow.add(getString(R.string.text_deposits))
        }

        if (SessionManager.canManageLoanEmi()) {
            tabsToShow.add(getString(R.string.text_loan_emi))
        }

        if (SessionManager.canManageFeedback()) {
            tabsToShow.add(getString(R.string.tab_feedback))
        }

        adminPagerAdapter = PagerAdapter(this, tabsToShow)
        binding.adminPager.adapter = adminPagerAdapter
        binding.adminPager.offscreenPageLimit = 1

        if (tabToGo == "Group") {
            val groupIndex = tabsToShow.indexOf(getString(R.string.text_group))
            if (groupIndex >= 0) {
                binding.adminPager.setCurrentItem(groupIndex, false)
            }
        } else {
            binding.adminPager.setCurrentItem(0, false)
        }

        TabLayoutMediator(binding.adminTabs, binding.adminPager) { tab, position ->
            val tabText = tabsToShow[position]

            tab.customView = TextView(requireContext()).apply {
                text = tabText
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 9f)
                gravity = Gravity.CENTER
                maxLines = 1
                setTypeface(null, Typeface.BOLD)
                isAllCaps = true
            }
        }.attach()
    }

    inner class PagerAdapter(
        fragment: Fragment,
        private val tabs: List<String>
    ) : FragmentStateAdapter(fragment) {

        override fun getItemCount(): Int = tabs.size

        override fun createFragment(position: Int): Fragment {
            return when (tabs[position]) {
                getString(R.string.text_user) -> UserFragment()
                getString(R.string.text_group) -> GroupFragment()
                getString(R.string.text_pending_approval) -> PendingApprovalFragment()
                getString(R.string.text_deposits) -> DepositEntryFragment()
                getString(R.string.text_loan_emi) -> LoanEmiEntryFragment()
                getString(R.string.tab_feedback) -> AdminFeedbackFragment()
                else -> Fragment()
            }
        }
    }



}

