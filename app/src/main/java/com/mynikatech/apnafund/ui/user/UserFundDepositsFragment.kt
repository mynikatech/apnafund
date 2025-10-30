package com.mynikatech.apnafund.ui.user

import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.appbar.MaterialToolbar
import com.mynikatech.apnafund.R
import com.mynikatech.apnafund.databinding.FragmentUserFundDepositsBinding
import com.mynikatech.apnafund.ui.viewmodel.DepositViewModel
import kotlinx.coroutines.launch

class UserFundDepositsFragment: Fragment() {

    private lateinit var binding: FragmentUserFundDepositsBinding

    private lateinit var adapter: FundDepositAdapter

    private val depositViewModel: DepositViewModel by viewModels()

    private val args: UserFundDepositsFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding = FragmentUserFundDepositsBinding.inflate(inflater, container, false)
        return binding.root
    }

    @Suppress("DEPRECATION")
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (requireActivity() as AppCompatActivity).supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
            title = "Fund Deposits"  // Optional
        }
        setHasOptionsMenu(true)
        val fundId = args.fundId
        val userId = args.userId
        val fundName = args.fundName
        binding.textViewFundName.text = fundName
        val recyclerView = binding.recyclerFundDeposits
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        viewLifecycleOwner.lifecycleScope.launch {
            val deposits = depositViewModel.getDepositsForFundAndMember(fundId, userId)
            adapter = FundDepositAdapter(deposits)
            binding.recyclerFundDeposits.adapter = adapter
        }
        val toolbar = view.findViewById<MaterialToolbar>(R.id.user_fund_deposits_toolbar)
        (activity as? AppCompatActivity)?.setSupportActionBar(toolbar)
        (activity as? AppCompatActivity)?.supportActionBar?.setDisplayShowTitleEnabled(false)

        // Enable back arrow
        val navController = findNavController()
        toolbar.setNavigationIcon(R.drawable.ic_back_arrow) // your back icon
        toolbar.setNavigationOnClickListener {
            navController.navigateUp()
        }
        toolbar.title = "Fund Deposits"
    }
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        menu.clear()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        val activity = requireActivity() as AppCompatActivity
        activity.supportActionBar?.show()
    }

    @Suppress("DEPRECATION")
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                requireActivity().onBackPressedDispatcher.onBackPressed()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

}