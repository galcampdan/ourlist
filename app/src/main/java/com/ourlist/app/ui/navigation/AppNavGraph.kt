package com.ourlist.app.ui.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.ourlist.app.OurListApp
import com.ourlist.app.BuildConfig
import com.ourlist.app.data.model.BucketCollection
import com.ourlist.app.data.update.AppUpdateInfo
import com.ourlist.app.ui.components.AppBottomBar
import com.ourlist.app.ui.components.AppUpdateDialog
import com.ourlist.app.ui.rememberHaptics
import com.ourlist.app.ui.screens.detail.ItemDetailScreen
import com.ourlist.app.ui.screens.detail.ItemDetailViewModel
import com.ourlist.app.ui.screens.heart.HeartScreen
import com.ourlist.app.ui.screens.memories.MemoriesScreen
import com.ourlist.app.ui.screens.memories.MemoriesViewModel
import com.ourlist.app.ui.screens.more.BackupScreen
import com.ourlist.app.ui.screens.more.CoupleConnectScreen
import com.ourlist.app.ui.screens.more.DriveSetupScreen
import com.ourlist.app.ui.screens.more.MoreScreen
import com.ourlist.app.ui.screens.more.PersonalScreen
import com.ourlist.app.ui.screens.more.SecretPersonalScreen
import com.ourlist.app.ui.screens.more.TrashScreen
import com.ourlist.app.ui.screens.notifications.NotificationsScreen
import com.ourlist.app.ui.screens.notifications.NotificationsViewModel
import com.ourlist.app.ui.screens.shared.BucketListViewModel
import com.ourlist.app.ui.screens.shared.SharedScreen
import com.ourlist.app.ui.theme.Ink
import kotlinx.coroutines.launch

@Composable
fun AppNavGraph(
    app: OurListApp,
    navController: NavHostController = rememberNavController(),
    modifier: Modifier = Modifier,
    openItemId: String? = null,
    onOpenItemHandled: () -> Unit = {}
) {
    val notificationsViewModel: NotificationsViewModel = viewModel(
        factory = NotificationsViewModel.Factory(app.notificationRepository)
    )
    val unreadCount by notificationsViewModel.unreadCount.collectAsState()

    LaunchedEffect(openItemId) {
        if (!openItemId.isNullOrBlank()) {
            navController.navigate(AppRoutes.createDetailRoute(openItemId)) { launchSingleTop = true }
            onOpenItemHandled()
        }
    }
    NavHost(
        navController = navController,
        startDestination = AppRoutes.SHARED,
        modifier = modifier.fillMaxSize()
    ) {
        // 1. Root 4-Tab Screen with HorizontalPager (Shared, Heart, Memories, More, UI-08)
        composable(AppRoutes.SHARED) {
            MainTabsScreen(
                app = app,
                unreadNotificationCount = unreadCount,
                onNavigateToNotifications = {
                    navController.navigate(AppRoutes.NOTIFICATIONS)
                },
                onNavigateToDetail = { itemId ->
                    navController.navigate(AppRoutes.createDetailRoute(itemId))
                },
                onNavigateToAdd = { collection ->
                    navController.navigate(AppRoutes.createAddRoute(collection))
                },
                onNavigate = { route ->
                    navController.navigate(route)
                }
            )
        }

        // 2. In-App Notifications Screen (Accessed via bell icon in Shared header, UI-09)
        composable(AppRoutes.NOTIFICATIONS) {
            NotificationsScreen(
                viewModel = notificationsViewModel,
                onNavigateToDetail = { itemId ->
                    navController.navigate(AppRoutes.createDetailRoute(itemId))
                },
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        // 3. Personal List Screen
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

        // 4. Hidden Secret Personal List Screen
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

        // 5. Trash Screen (30 days retention)
        composable(AppRoutes.TRASH) {
            TrashScreen(
                bucketRepository = app.bucketRepository,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // 6. Encrypted Backup Screen
        composable(AppRoutes.BACKUP) {
            BackupScreen(
                backupManager = app.backupManager,
                dataStoreManager = app.dataStoreManager,
                onNavigateBack = { navController.popBackStack() },
                onConnectDrive = { navController.navigate(AppRoutes.DRIVE_SETUP) }
            )
        }

        // 7. Google Drive Setup Screen
        composable(AppRoutes.DRIVE_SETUP) {
            DriveSetupScreen(
                dataStoreManager = app.dataStoreManager,
                driveClient = app.driveClient,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // 7.5 Couple Connect Screen (6-digit PIN Realtime Sync)
        composable(AppRoutes.COUPLE_CONNECT) {
            CoupleConnectScreen(
                dataStoreManager = app.dataStoreManager,
                coupleClient = app.coupleClient,
                syncEngine = app.syncEngine,
                bucketRepository = app.bucketRepository,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // 8. Item Detail Screen
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

        // 9. Add Item Screen
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

/**
 * Main 4-Tab Screen hosting Shared, Heart, Memories, and More tabs with HorizontalPager.
 * Supports swiping left/right with tactile haptic feedback and bidirectional bottom dock synchronization.
 */
@Composable
fun MainTabsScreen(
    app: OurListApp,
    unreadNotificationCount: Int,
    onNavigateToNotifications: () -> Unit,
    onNavigateToDetail: (String) -> Unit,
    onNavigateToAdd: (String) -> Unit,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { 4 })
    val coroutineScope = rememberCoroutineScope()
    val haptics = rememberHaptics()

    // Tactile haptic feedback on tab swipe
    var isFirstLaunch by remember { mutableStateOf(true) }
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }
            .collect {
                if (isFirstLaunch) {
                    isFirstLaunch = false
                } else {
                    haptics.tick()
                }
            }
    }

    // Startup automatic update check
    var updateInfo by remember { mutableStateOf<AppUpdateInfo?>(null) }
    var showUpdateDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val res = app.appUpdateManager.checkForUpdate(BuildConfig.VERSION_CODE)
        res.onSuccess { info ->
            if (info != null) {
                updateInfo = info
                showUpdateDialog = true
            }
        }
    }

    if (showUpdateDialog && updateInfo != null) {
        AppUpdateDialog(
            updateInfo = updateInfo!!,
            appUpdateManager = app.appUpdateManager,
            onDismiss = { showUpdateDialog = false }
        )
    }

    // On tabs 1, 2, or 3, pressing system back returns to the first tab (Shared)
    BackHandler(enabled = pagerState.currentPage != 0) {
        coroutineScope.launch {
            pagerState.animateScrollToPage(0)
        }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding(),
        containerColor = Ink,
        bottomBar = {
            AppBottomBar(
                selectedIndex = pagerState.currentPage,
                onTabSelected = { index ->
                    coroutineScope.launch {
                        pagerState.animateScrollToPage(index)
                    }
                }
            )
        }
    ) { innerPadding ->
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxSize()
                .padding(top = innerPadding.calculateTopPadding()),
            userScrollEnabled = true,
            key = { it }
        ) { page ->
            when (page) {
                0 -> {
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
                        unreadNotificationCount = unreadNotificationCount,
                        onNavigateToNotifications = onNavigateToNotifications,
                        onNavigateToDetail = onNavigateToDetail,
                        onNavigateToAdd = { onNavigateToAdd(BucketCollection.SHARED.name) }
                    )
                }
                1 -> {
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
                        onNavigateToDetail = onNavigateToDetail,
                        onNavigateToAdd = { onNavigateToAdd(BucketCollection.HEART.name) }
                    )
                }
                2 -> {
                    val memoriesViewModel: MemoriesViewModel = viewModel(
                        factory = MemoriesViewModel.Factory(app.bucketRepository)
                    )
                    MemoriesScreen(
                        viewModel = memoriesViewModel,
                        onNavigateToDetail = onNavigateToDetail
                    )
                }
                3 -> {
                    MoreScreen(
                        dataStoreManager = app.dataStoreManager,
                        syncEngine = app.syncEngine,
                        safBackupManager = app.safBackupManager,
                        coupleClient = app.coupleClient,
                        appUpdateManager = app.appUpdateManager,
                        db = app.database,
                        onNavigate = onNavigate
                    )
                }
            }
        }
    }
}
