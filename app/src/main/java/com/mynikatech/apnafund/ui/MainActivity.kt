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
        setSupportActionBar(binding.toolbar)
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController
        if (savedInstanceState == null) {
            val graph = navController.navInflater.inflate(R.navigation.nav_graph)
            val startDest = intent.getStringExtra("start_dest")
            val intentUserId = intent.getIntExtra("user_id", -1)

            val userId = if (SessionManager.userId > 0) {
                SessionManager.userId
            } else if (intentUserId > 0) {
                intentUserId
            } else {
                -1
            }

            if (userId > 0) {
                SessionManager.userId = userId
            }
            val finalStartDest = when {
                startDest == "enter_pin" -> "enter_pin"
                userId > 0 -> "home"
                else -> "login"
            }

            val startArgs = if (finalStartDest == "enter_pin" && userId > 0) {
                bundleOf("userId" to userId)
            } else {
                null
            }

            graph.setStartDestination(
                when (finalStartDest) {
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
        }
        binding.bottomNavigationView.setupWithNavController(navController)
        setupToolbarMenu(navController)
        val toolbarRef = WeakReference(binding.toolbar)
        binding.toolbar.setNavigationOnClickListener {
            val currentDest = navController.currentDestination?.id

            val isTopLevel = currentDest in setOf(
                R.id.userSummaryFragment,
                R.id.fundFragment,
                R.id.adminFragment,
                R.id.supportFragment,
                R.id.groupChatFragment
            )

            if (isTopLevel) {
                navController.navigate(R.id.supportFragment)
            } else {
                navController.navigateUp()
            }
        }
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
                R.id.loginPinFragment, R.id.fundLoanSummaryFragment,
                R.id.aiChatFragment -> View.GONE

                else -> View.VISIBLE
            }
            if (destination.id in hideToolbarFor) {
                binding.imageLogo.visibility = View.GONE
                binding.bottomNavigationView.visibility = View.GONE
            } else {
                binding.imageLogo.visibility = View.VISIBLE
                binding.bottomNavigationView.visibility = View.VISIBLE
            }
            val isTopLevel = destination.id in setOf(
                R.id.userSummaryFragment,
                R.id.fundFragment,
                R.id.adminFragment,
                R.id.supportFragment,
                R.id.groupChatFragment
            )

            if (isTopLevel) {
                // Show support icon
                binding.toolbar.title = ""
                if (binding.toolbar.navigationIcon == null) {
                    binding.toolbar.setNavigationIcon(R.drawable.ic_contact_phone)
                }

            } else {
                // IMPORTANT: Let Navigation handle back arrow
                binding.toolbar.navigationIcon = null
                binding.toolbar.title = destination.label
            }

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
                    R.id.action_help -> {
                        navController.navigate(R.id.helpFragment)
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
