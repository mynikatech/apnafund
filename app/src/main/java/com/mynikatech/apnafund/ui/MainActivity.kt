package com.mynikatech.apnafund.ui

import android.os.Bundle
import android.os.StrictMode
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.bundleOf
import androidx.core.view.MenuProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.android.volley.BuildConfig
import com.mynikatech.apnafund.R
import com.mynikatech.apnafund.databinding.ActivityMainBinding
import com.mynikatech.apnafund.ui.viewmodel.NotificationSharedViewModel
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val notificationViewModel by viewModels<NotificationSharedViewModel>()

    private var currentBadgeView: WeakReference<TextView>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController
        val graph = navController.navInflater.inflate(R.navigation.nav_graph)
        val startDest = intent.getStringExtra("start_dest")
        val userId = intent.getIntExtra("user_id", -1)
        val startArgs = when (startDest) {
            "enter_pin", "home" -> bundleOf("userId" to userId)
            else -> null
        }
        graph.setStartDestination(
            when (startDest) {
                "enter_pin" -> R.id.loginPinFragment
                "home" -> R.id.userSummaryFragment
                else -> R.id.loginFragment
            }
        )
        if (startArgs != null) {
            navController.setGraph(graph, startArgs)
        } else {
            navController.graph = graph
        }
        binding.bottomNavigationView.setupWithNavController(navController)

        setSupportActionBar(binding.toolbar)
        setupToolbarMenu(navController)
        val toolbarRef = WeakReference(binding.toolbar)
        val hideToolbarFor = setOf(
            R.id.loginFragment, R.id.registerFragment,
            R.id.changePasswordFragment, R.id.setPasswordFragment,
            R.id.loginPinFragment,R.id.fundMemberDetailsFragment,
            R.id.fundAddMembersFragment,
        )
        navController.addOnDestinationChangedListener { _, destination, _ ->
            toolbarRef.get()?.visibility = when (destination.id) {
                R.id.fundDepositsFragment, R.id.detailedFundSummaryFragment,
                R.id.groupDetailsFragment, R.id.supportFragment,
                R.id.notificationFragment, R.id.loanEmiFragment,
                R.id.loginFragment, R.id.registerFragment,
                R.id.changePasswordFragment, R.id.setPasswordFragment,
                R.id.fundMemberDetailsFragment, R.id.fundAddMembersFragment,
                R.id.loginPinFragment-> View.GONE

                else -> View.VISIBLE
            }
            if (destination.id in hideToolbarFor) {
                binding.imageLogo.visibility = View.GONE
                binding.bottomNavigationView.visibility = View.GONE
            } else {
                binding.imageLogo.visibility = View.VISIBLE
                binding.bottomNavigationView.visibility = View.VISIBLE
            }

        }
        binding.toolbar.setNavigationOnClickListener {
            navController.navigate(R.id.supportFragment)
        }

        val sideNavigationView = binding.sideNavigationView
        val drawerLayout = binding.drawerLayout
        sideNavigationView.setNavigationItemSelectedListener { menuItem ->
            drawerLayout.closeDrawers()
            when (menuItem.itemId) {
                R.id.nav_change_theme -> true
                R.id.nav_user_info -> true
                R.id.nav_feedback -> true
                R.id.nav_share -> true
                R.id.nav_logout -> true
                else -> false
            }
        }
        if (BuildConfig.DEBUG) {
            StrictMode.setThreadPolicy(
                StrictMode.ThreadPolicy.Builder()
                    .detectAll()
                    .penaltyLog()
                    .build()
            )
        }
    }
    private fun setupToolbarMenu(navController: androidx.navigation.NavController) {
        addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.toolbar_menu, menu)

                val menuItem = menu.findItem(R.id.action_notifications)
                val badgeTextView = menuItem?.actionView
                    ?.findViewById<TextView>(R.id.notification_badge)

                // Avoid capturing badgeTextView in observer
                currentBadgeView = WeakReference(badgeTextView)

                menuItem?.actionView?.setOnClickListener {
                    navController.navigate(R.id.notificationFragment)
                }
                // Load once
                lifecycleScope.launch {
                    val userId = 1
                    notificationViewModel.loadNotifications(userId)
                }

                // Observe safely without view capture
                notificationViewModel.notifications.observe(this@MainActivity) { list ->
                    val unreadCount = list?.count { !it.readFlag } ?: 0
                    updateNotificationBadge(unreadCount)
                }
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return when (menuItem.itemId) {
                    R.id.action_notifications -> {
                        navController.navigate(R.id.notificationFragment)
                        true
                    }

                    else -> false
                }
            }
        })
    }
    private fun updateNotificationBadge(count: Int) {
        currentBadgeView?.get()?.apply {
            text = if (count > 99) "99+" else count.toString()
            visibility = if (count > 0) View.VISIBLE else View.GONE
        }
    }
}
