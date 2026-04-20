package com.example.fbuddy
//test commit

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.fbuddy.data.repository.UserProfileRepository
import com.example.fbuddy.sms.SmsParserService
import com.example.fbuddy.ui.analytics.AnalyticsScreen
import com.example.fbuddy.ui.chat.ChatScreen
import com.example.fbuddy.ui.home.HomeScreen
import com.example.fbuddy.ui.onboarding.OnboardingScreen
import com.example.fbuddy.ui.scan.ScanScreen
import com.example.fbuddy.ui.settings.EditProfileScreen
import com.example.fbuddy.ui.settings.SettingsScreen
import com.example.fbuddy.ui.theme.*
import com.example.fbuddy.ui.transactions.TransactionsScreen
import kotlinx.coroutines.launch

data class NavItem(val route: String, val label: String, val icon: ImageVector)

class MainActivity : ComponentActivity() {

    private val smsPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> if (granted) SmsParserService.startFullScan(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FBuddyTheme {
                FBuddyNavGraph(
                    onRescan = {
                        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS)
                            == PackageManager.PERMISSION_GRANTED
                        ) SmsParserService.startFullScan(this)
                        else smsPermissionLauncher.launch(Manifest.permission.READ_SMS)
                    }
                )
            }
        }
    }
}

@Composable
fun FBuddyNavGraph(onRescan: () -> Unit) {
    val navController = rememberNavController()

    // Check if onboarding is done
    var startDest by remember { mutableStateOf<String?>(null) }
    val context = androidx.compose.ui.platform.LocalContext.current

    LaunchedEffect(Unit) {
        val repo = UserProfileRepository(context)
        startDest = if (repo.isOnboardingComplete()) "main" else "onboarding"
    }

    if (startDest == null) {
        // Splash — show blank while checking DB
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Teal)
        }
        return
    }

    NavHost(navController = navController, startDestination = startDest!!) {

        composable("onboarding") {
            OnboardingScreen(
                onComplete = {
                    // After onboarding done, go to main and start SMS scan
                    navController.navigate("main") {
                        popUpTo("onboarding") { inclusive = true }
                    }
                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS)
                        == PackageManager.PERMISSION_GRANTED
                    ) SmsParserService.startFullScan(context)
                }
            )
        }

        composable("main") {
            MainScaffold(onRescan = onRescan)
        }
    }
}

@Composable
fun MainScaffold(onRescan: () -> Unit) {
    val navController = rememberNavController()

    val navItems = listOf(
        NavItem("home",         "Home",      Icons.Filled.Home),
        NavItem("transactions", "Txns",      Icons.Filled.List),
        NavItem("scan",         "Scan",      Icons.Outlined.CameraAlt),
        NavItem("analytics",    "Analytics", Icons.Filled.Analytics),
        NavItem("settings",     "Profile",   Icons.Filled.Person),
    )

    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route

    fun goTo(route: String) = navController.navigate(route) {
        popUpTo(navController.graph.startDestinationId) { saveState = true }
        launchSingleTop = true
        restoreState    = true
    }

    Scaffold(
        containerColor = BgSand,
        bottomBar = {
            NavigationBar(containerColor = White, tonalElevation = 0.dp) {
                navItems.forEach { item ->
                    val selected = currentRoute == item.route
                    if (item.route == "scan") {
                        NavigationBarItem(
                            selected = selected,
                            onClick  = { goTo(item.route) },
                            icon = {
                                Surface(
                                    shape           = RoundedCornerShape(16.dp),
                                    color           = Teal,
                                    shadowElevation = 6.dp,
                                    modifier        = Modifier.size(48.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(item.icon, contentDescription = "Scan",
                                            tint = Color.White, modifier = Modifier.size(22.dp))
                                    }
                                }
                            },
                            label  = {},
                            colors = NavigationBarItemDefaults.colors(indicatorColor = Color.Transparent)
                        )
                    } else {
                        NavigationBarItem(
                            selected = selected,
                            onClick  = { goTo(item.route) },
                            icon  = { Icon(item.icon, contentDescription = item.label) },
                            label = { Text(item.label, style = MaterialTheme.typography.labelSmall) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor   = Teal,
                                selectedTextColor   = Teal,
                                indicatorColor      = TealLight,
                                unselectedIconColor = Ink4,
                                unselectedTextColor = Ink4,
                            )
                        )
                    }
                }
            }
        }
    ) { inner ->
        NavHost(navController, startDestination = "home",
            modifier = Modifier.padding(inner)) {
            composable("home") {
                HomeScreen(onChatClick = { navController.navigate("chat") })
            }
            composable("transactions") { TransactionsScreen() }
            composable("scan") {
                ScanScreen(
                    onBackClick = { navController.navigateUp() }
                )
            }
            composable("analytics")    { AnalyticsScreen() }
            composable("settings") {
                SettingsScreen(
                    onRescan = onRescan,
                    onEditProfile = { navController.navigate("edit_profile") }
                )
            }
            composable("edit_profile") {
                EditProfileScreen(
                    onBackClick = { navController.navigateUp() }
                )
            }
            composable("chat") {
                ChatScreen(
                    onBackClick = { navController.navigateUp() }
                )
            }
        }
    }
}