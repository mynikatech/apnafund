package com.mynikatech.apnafund.ui.user.notification

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.appbar.MaterialToolbar
import com.mynikatech.apnafund.R
import com.mynikatech.apnafund.databinding.FragmentNotificationBinding
import com.mynikatech.apnafund.session.SessionManager
import com.mynikatech.apnafund.ui.viewmodel.NotificationSharedViewModel

class NotificationFragment : Fragment() {
    private lateinit var binding: FragmentNotificationBinding

    private lateinit var adapter: NotificationAdapter

    val notificationViewModel: NotificationSharedViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        binding = FragmentNotificationBinding.inflate(layoutInflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        super.onViewCreated(view, savedInstanceState)
        /*(requireActivity() as AppCompatActivity).supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
            title = "Notifications"
        }*/
        binding.recyclerView.layoutManager =
            LinearLayoutManager(requireContext())

        adapter = NotificationAdapter { notification ->
            if (notification.readFlag) return@NotificationAdapter

            notificationViewModel.markAsRead(
                notification.userNotificationId,
                SessionManager.userId
            )
        }
        binding.recyclerView.adapter = adapter

        notificationViewModel.notifications.observe(viewLifecycleOwner) { list ->
            if (list.isNullOrEmpty()) {
                binding.textViewNoNotifications.visibility = View.VISIBLE
            } else {
                binding.textViewNoNotifications.visibility = View.GONE
                adapter.updateList(list)
            }
        }

        notificationViewModel.loadNotifications(SessionManager.userId)

        // to add notifications impl later for now consider there is no notifications


        val toolbar = view.findViewById<MaterialToolbar>(R.id.toolbar_noification)
        //(activity as? AppCompatActivity)?.setSupportActionBar(toolbar)
        //(activity as? AppCompatActivity)?.supportActionBar?.setDisplayShowTitleEnabled(false)

        // Enable back arrow
        val navController = findNavController()
        toolbar.setNavigationIcon(R.drawable.ic_back_arrow) // your back icon
        toolbar.setNavigationOnClickListener {
            navController.navigateUp()
        }
        toolbar.title = "Notifications"
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