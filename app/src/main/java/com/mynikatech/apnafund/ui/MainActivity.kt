package com.mynikatech.apnafund.ui

import android.os.Bundle
import android.os.StrictMode
import android.util.Log
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.bundleOf
import androidx.core.view.MenuProvider
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.google.firebase.auth.FirebaseAuth
import com.mynikatech.apnafund.BuildConfig
import com.mynikatech.apnafund.R
import com.mynikatech.apnafund.databinding.ActivityMainBinding
import com.mynikatech.apnafund.session.SessionManager
import com.mynikatech.apnafund.ui.viewmodel.NotificationSharedViewModel
import java.lang.ref.WeakReference

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val notificationViewModel by viewModels<NotificationSharedViewModel>()

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
            R.id.loginPinFragment, R.id.fundMemberDetailsFragment,
            R.id.fundAddMembersFragment, R.id.verifyEmailFragment,
            R.id.resetPasswordFragment, R.id.fundLoanSummaryFragment
        )
        navController.addOnDestinationChangedListener { _, destination, _ ->
            toolbarRef.get()?.visibility = when (destination.id) {
                R.id.fundDepositsFragment, R.id.detailedFundSummaryFragment,
                R.id.groupDetailsFragment, R.id.supportFragment,
                R.id.notificationFragment, R.id.loanEmiFragment,
                R.id.loginFragment, R.id.registerFragment,
                R.id.changePasswordFragment, R.id.setPasswordFragment,
                R.id.fundMemberDetailsFragment, R.id.fundAddMembersFragment,
                R.id.verifyEmailFragment, R.id.resetPasswordFragment,
                R.id.loginPinFragment, R.id.fundLoanSummaryFragment -> View.GONE

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
        binding.bottomNavigationView.setOnItemReselectedListener { item ->
            if (item.itemId == R.id.userSummaryFragment) {
                navController.popBackStack(
                    R.id.userSummaryFragment,
                    inclusive = false
                )
            }
        }

        notificationViewModel.unreadCount.observe(this) { count ->
            updateNotificationBadge(count)
        }
    }

    private fun setupToolbarMenu(navController: androidx.navigation.NavController) {
        addMenuProvider(object : MenuProvider {

            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.toolbar_menu, menu)
                /* ---------------- NOTIFICATIONS ---------------- */
                val notifItem = menu.findItem(R.id.action_notifications)

                notifItem?.actionView?.setOnClickListener {
                    navController.navigate(R.id.notificationFragment)
                }
                // Apply latest count immediately if already loaded
                notificationViewModel.unreadCount.value?.let {
                    updateNotificationBadge(it)
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
        val menu = binding.toolbar.menu
        val notifItem = menu.findItem(R.id.action_notifications)
        val badgeTextView =
            notifItem?.actionView?.findViewById<TextView>(R.id.notification_badge)

        badgeTextView?.apply {
            text = if (count > 99) "99+" else count.toString()
            visibility = if (count > 0) View.VISIBLE else View.GONE
        }
    }

    override fun onStart() {
        super.onStart()

        if (!SessionManager.hasValidSession) {
            // App session says user is logged out → ignore Firebase
            FirebaseAuth.getInstance().signOut()
            SessionManager.firebaseUid = ""
            SessionManager.isFirebaseSynced = false
            return
        }

        val firebaseUser = FirebaseAuth.getInstance().currentUser
        if (firebaseUser != null) {
            SessionManager.firebaseUid = firebaseUser.uid
            SessionManager.isFirebaseSynced = true
            Log.d("FirebaseAuth", "Restored firebaseUid=${firebaseUser.uid}")
        }

    }

    override fun onResume() {
        super.onResume()
        val userId = SessionManager.userId
        if (userId > 0) {
            notificationViewModel.loadUnreadCount(userId)
        }
    }


}
