package com.ourlist.app.ui.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import kotlinx.coroutines.launch
import com.ourlist.app.OurListApp
import com.ourlist.app.data.model.BucketCollection
import com.ourlist.app.ui.components.AppBottomBar
import com.ourlist.app.ui.screens.detail.ItemDetailScreen
import com.ourlist.app.ui.screens.detail.ItemDetailViewModel
import com.ourlist.app.ui.screens.heart.HeartScreen
import com.ourlist.app.ui.screens.memories.MemoriesScreen
import com.ourlist.app.ui.screens.memories.MemoriesViewModel
import com.ourlist.app.ui.screens.more.BackupScreen
import com.ourlist.app.ui.screens.more.DriveSetupScreen
import com.ourlist.app.ui.screens.more.MoreScreen
import com.ourlist.app.ui.screens.more.PersonalScreen
import com.ourlist.app.ui.screens.more.SecretPersonalScreen
import com.ourlist.app.ui.screens.more.TrashScreen
import com.ourlist.app.ui.screens.notifications.NotificationsScreen
import com.ourlist.app.ui.screens.notifications.NotificationsViewModel
import com.ourlist.app.ui.screens.shared.BucketListViewModel
import com.ourlist.app.ui.screens.shared.SharedScreen

@Composable
fun AppNavGraph(
    app: OurListApp,
    navController: NavHostController = rememberNavController(),
    modifier: Modifier = Modifier
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val isBottomBarVisible = currentRoute in listOf(
        AppRoutes.SHARED,
        AppRoutes.HEART,
        AppRoutes.MEMORIES,
        AppRoutes.NOTIFICATIONS,
        AppRoutes.MORE
    )

    val notificationsViewModel: NotificationsViewModel = viewModel(
        factory = NotificationsViewModel.Factory(app.notificationRepository)
    )
    val unreadCount by notificationsViewModel.unreadCount.collectAsState()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        bottomBar = {
            if (isBottomBarVisible) {
                AppBottomBar(
                    navController = navController,
                    unreadNotificationCount = unreadCount
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = AppRoutes.SHARED,
            modifier = Modifier.padding(innerPadding)
        ) {
            // 1. Shared Bucket List (Home, Q40)
            composable(AppRoutes.SHARED) {
                val sharedViewModel: BucketListViewModel = viewModel(
                    key = "shared_vm",
                    factory = BucketListViewModel.Factory(
                        BucketCollection.SHARED,
                        app.bucketRepository,
                        app.conflictRepository,
                        app.dataStoreManager,
                        app.syncEngine
                    )
                )
                SharedScreen(
                    viewModel = sharedViewModel,
                    onNavigateToDetail = { itemId ->
                        navController.navigate(AppRoutes.createDetailRoute(itemId))
                    },
                    onNavigateToAdd = {
                        navController.navigate(AppRoutes.createAddRoute(BucketCollection.SHARED.name))
                    }
                )
            }

            // 2. Heart Bucket List (Q17, Q39)
            composable(AppRoutes.HEART) {
                val heartViewModel: BucketListViewModel = viewModel(
                    key = "heart_vm",
                    factory = BucketListViewModel.Factory(
                        BucketCollection.HEART,
                        app.bucketRepository,
                        app.conflictRepository,
                        app.dataStoreManager,
                        app.syncEngine
                    )
                )
                HeartScreen(
                    viewModel = heartViewModel,
                    onNavigateToDetail = { itemId ->
                        navController.navigate(AppRoutes.createDetailRoute(itemId))
                    },
                    onNavigateToAdd = {
                        navController.navigate(AppRoutes.createAddRoute(BucketCollection.HEART.name))
                    }
                )
            }

            // 3. Memories (Q10, Q26)
            composable(AppRoutes.MEMORIES) {
                val memoriesViewModel: MemoriesViewModel = viewModel(
                    factory = MemoriesViewModel.Factory(app.bucketRepository)
                )
                MemoriesScreen(
                    viewModel = memoriesViewModel,
                    onNavigateToDetail = { itemId ->
                        navController.navigate(AppRoutes.createDetailRoute(itemId))
                    }
                )
            }

            // 4. In-App Notifications (Q22, Q23)
            composable(AppRoutes.NOTIFICATIONS) {
                NotificationsScreen(
                    viewModel = notificationsViewModel,
                    onNavigateToDetail = { itemId ->
                        navController.navigate(AppRoutes.createDetailRoute(itemId))
                    }
                )
            }

            // 5. More (Q5, Q15, Q31, Q43)
            composable(AppRoutes.MORE) {
                MoreScreen(
                    dataStoreManager = app.dataStoreManager,
                    syncEngine = app.syncEngine,
                    db = app.database,
                    onNavigate = { route -> navController.navigate(route) }
                )
            }

            // Personal List Screen
            composable(AppRoutes.PERSONAL) {
                val personalViewModel: BucketListViewModel = viewModel(
                    key = "personal_vm",
                    factory = BucketListViewModel.Factory(
                        BucketCollection.PERSONAL,
                        app.bucketRepository,
                        app.conflictRepository,
                        app.dataStoreManager,
                        app.syncEngine
                    )
                )
                PersonalScreen(
                    viewModel = personalViewModel,
                    onNavigateToDetail = { itemId ->
                        navController.navigate(AppRoutes.createDetailRoute(itemId))
                    },
                    onNavigateToAdd = {
                        navController.navigate(AppRoutes.createAddRoute(BucketCollection.PERSONAL.name))
                    },
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // Hidden Secret Personal List Screen (Q38)
            composable(AppRoutes.SECRET_PERSONAL) {
                val secretViewModel: BucketListViewModel = viewModel(
                    key = "secret_vm",
                    factory = BucketListViewModel.Factory(
                        BucketCollection.SECRET_LOCAL,
                        app.bucketRepository,
                        app.conflictRepository,
                        app.dataStoreManager,
                        app.syncEngine
                    )
                )
                val secretMemories by app.bucketRepository.getSecretMemories().collectAsState(initial = emptyList())

                SecretPersonalScreen(
                    viewModel = secretViewModel,
                    secretMemories = secretMemories,
                    onNavigateToDetail = { itemId ->
                        navController.navigate(AppRoutes.createDetailRoute(itemId))
                    },
                    onNavigateToAdd = {
                        navController.navigate(AppRoutes.createAddRoute(BucketCollection.SECRET_LOCAL.name))
                    },
                    onNavigateBack = { navController.popBackStack() },
                    onDoAgain = { originalId ->
                        app.scope.launch { app.bucketRepository.doAgain(originalId, null) }
                    },
                    onRevert = { itemId ->
                        app.scope.launch { app.bucketRepository.uncompleteItem(itemId) }
                    }
                )
            }

            // Trash Screen (Q15, 30 days)
            composable(AppRoutes.TRASH) {
                TrashScreen(
                    bucketRepository = app.bucketRepository,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // Encrypted Backup Screen (Q31, Q34, Q35)
            composable(AppRoutes.BACKUP) {
                BackupScreen(
                    backupManager = app.backupManager,
                    dataStoreManager = app.dataStoreManager,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // Drive Setup Screen (Q36)
            composable(AppRoutes.DRIVE_SETUP) {
                DriveSetupScreen(
                    dataStoreManager = app.dataStoreManager,
                    driveClient = app.driveClient,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // Item Detail Screen
            composable(
                route = AppRoutes.DETAIL,
                arguments = listOf(navArgument("itemId") { type = NavType.StringType })
            ) { backStackEntry ->
                val itemId = backStackEntry.arguments?.getString("itemId") ?: ""
                val detailViewModel: ItemDetailViewModel = viewModel(
                    key = "detail_$itemId",
                    factory = ItemDetailViewModel.Factory(
                        itemId = itemId,
                        initialCollection = BucketCollection.SHARED,
                        bucketRepository = app.bucketRepository,
                        dataStoreManager = app.dataStoreManager
                    )
                )
                ItemDetailScreen(
                    viewModel = detailViewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // Add Item Screen
            composable(
                route = AppRoutes.ADD,
                arguments = listOf(
                    navArgument("collection") {
                        type = NavType.StringType
                        defaultValue = BucketCollection.SHARED.name
                    }
                )
            ) { backStackEntry ->
                val colName = backStackEntry.arguments?.getString("collection") ?: BucketCollection.SHARED.name
                val collection = try { BucketCollection.valueOf(colName) } catch (e: Exception) { BucketCollection.SHARED }

                val detailViewModel: ItemDetailViewModel = viewModel(
                    key = "add_$colName",
                    factory = ItemDetailViewModel.Factory(
                        itemId = "new",
                        initialCollection = collection,
                        bucketRepository = app.bucketRepository,
                        dataStoreManager = app.dataStoreManager
                    )
                )
                ItemDetailScreen(
                    viewModel = detailViewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }
}
