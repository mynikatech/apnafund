package com.mynikatech.apnafund.ui.admin

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
import com.google.android.material.tabs.TabLayoutMediator
import com.mynikatech.apnafund.databinding.FragmentAdminBinding
import com.mynikatech.apnafund.session.SessionManager
import com.mynikatech.apnafund.ui.admin.feedback.AdminFeedbackFragment
import com.mynikatech.apnafund.ui.admin.group.GroupFragment
import com.mynikatech.apnafund.ui.admin.loan.LoanEmiEntryFragment
import com.mynikatech.apnafund.ui.admin.user.PendingApprovalFragment
import com.mynikatech.apnafund.ui.admin.user.UserFragment
import com.mynikatech.apnafund.ui.deposit.DepositEntryFragment

class AdminFragment : Fragment() {

    private lateinit var binding: FragmentAdminBinding

    private lateinit var adminPagerAdapter: PagerAdapter

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
        val isModerator = SessionManager.isModerator()

        val allTabs = listOf(
            "Users" to UserFragment(),
            "Groups" to GroupFragment(),
            "Approvals" to PendingApprovalFragment(),
            "Deposits" to DepositEntryFragment(),
            "Loans EMI" to LoanEmiEntryFragment(),
            "Feedback" to AdminFeedbackFragment()
        )

        val moderatorTabs = listOf(
            "Users" to UserFragment(),
            "Groups" to GroupFragment(),
            "Deposits" to DepositEntryFragment(),
            "Loans EMI" to LoanEmiEntryFragment()
        )
        val tabsToShow = when {
            isAdmin -> allTabs
            isModerator -> moderatorTabs
            else -> emptyList()
        }
        adminPagerAdapter = PagerAdapter(this, tabsToShow)
        binding.adminPager.adapter = adminPagerAdapter
        if (tabToGo.equals("Group"))
            binding.adminPager.setCurrentItem(1, false)
        else
            binding.adminPager.setCurrentItem(0, false)
        TabLayoutMediator(binding.adminTabs, binding.adminPager) { tab, position ->
            val tabText = tabsToShow[position].first
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

    inner class PagerAdapter(fragment: Fragment, private val items: List<Pair<String, Fragment>>) :
        FragmentStateAdapter(fragment) {

        override fun getItemCount(): Int = items.size

        override fun createFragment(position: Int): Fragment = items[position].second
    }

}

