# Lynket Browser - Complete Modernization Plan

## Executive Summary

This document outlines the complete modernization of Lynket Browser from a legacy Android app (RxJava, XML Views, manual Dagger) to a modern Android application using current best practices (Kotlin Flows, Jetpack Compose, Hilt).

### Current State Analysis

**Technical Debt:**
- **Triple Rx Stack**: RxJava 1.x (44 files) + RxJava 2.x (22 files) + interop library
- **Raw SQLite**: Manual SQL queries with SQLiteOpenHelper (Room dependency present but unused)
- **Legacy View Binding**: Butterknife annotations across codebase
- **SharedPreferences**: 106 occurrences (DataStore present but unused)
- **Mixed Languages**: 171 Kotlin files + 91 Java files
- **XML Layouts**: 92 layout files (Compose only in excluded playground module)
- **Manual DI**: Custom Dagger 2 scoping with manual component wiring (Hilt commented out)
- **Legacy UI Libraries**: Material Dialogs 0.9.6.0, MaterialDrawer 6.1.2, Epoxy 4.6.4
- **Dual Paging**: Both Paging 2.1.2 and Paging 3.3.5 present

### Target Architecture

**Modern Android Stack:**
- **Reactive**: Kotlin Coroutines + Flow + StateFlow/SharedFlow
- **Database**: Room with KSP, suspend functions, Flow queries
- **Preferences**: DataStore (Proto or Preferences)
- **DI**: Hilt with @HiltAndroidApp, @HiltViewModel, @AndroidEntryPoint
- **UI**: Jetpack Compose with Material3
- **Navigation**: Navigation Compose
- **Images**: Coil (already present)
- **Lists**: LazyColumn/LazyGrid (replace Epoxy)
- **Paging**: Paging 3 with Compose integration
- **Language**: 100% Kotlin
- **SDK**: Target SDK 35

---

## Phase 1: Foundation - Data Layer & DI (Weeks 1-3)

**Goal:** Establish modern foundation without breaking existing UI

### 1.1 Build Configuration & Hilt Setup

**Files:**
- `android-app/lynket/build.gradle.kts`
- `android-app/lynket/src/main/AndroidManifest.xml`

**Tasks:**
- ✅ Enable Hilt plugin (uncomment line 18)
- ✅ Remove Butterknife dependencies
- ✅ Remove RxJava 1.x dependencies
- ✅ Update RxJava 2.x → 3.x (prepare for removal)
- ✅ Remove Epoxy (after Compose migration)
- ✅ Remove Material Dialogs legacy version
- ✅ Configure KSP for Room and Hilt

**Impact:** Build system ready for modern libraries

### 1.2 Application Class Migration

**Current File:** `arun.com.chromer.ChromerApplication`

**Changes:**
```kotlin
// Before: Manual Dagger
@Component(modules = [AppModule::class])
interface AppComponent { ... }

// After: Hilt
@HiltAndroidApp
class ChromerApplication : Application()
```

**Tasks:**
- Convert Application to @HiltAndroidApp
- Remove manual AppComponent initialization
- Migrate AppModule to Hilt modules with @InstallIn(SingletonComponent::class)
- Convert ActivityComponent.Factory → @AndroidEntryPoint pattern

**Files Affected:**
- `arun.com.chromer.ChromerApplication`
- `arun.com.chromer.di.app.*`
- All Activities extending BaseActivity

### 1.3 Room Database Implementation

**Current:** Raw SQLite in `HistorySqlDiskStore.kt`

**New Architecture:**
```
ChromerDatabase (Room)
├── HistoryDao
├── WebsiteDao
├── WebArticleDao
├── ProviderDao
└── AppDao
```

**Entity Models:**
```kotlin
@Entity(tableName = "history")
data class Website(
    @PrimaryKey val url: String,
    val title: String,
    val favicon: String?,
    val visitCount: Int,
    val lastVisit: Long
)
```

**DAO Pattern:**
```kotlin
@Dao
interface HistoryDao {
    @Query("SELECT * FROM history ORDER BY lastVisit DESC")
    fun getAllFlow(): Flow<List<Website>>

    @Query("SELECT * FROM history WHERE url = :url")
    suspend fun getByUrl(url: String): Website?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(website: Website)

    @Delete
    suspend fun delete(website: Website)
}
```

**Migration Strategy:**
```kotlin
val MIGRATION_0_1 = object : Migration(0, 1) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Create new Room schema
        database.execSQL("CREATE TABLE IF NOT EXISTS history ...")
        // Migrate data from old SQLite DB
    }
}
```

**Files to Create:**
- `arun.com.chromer.data.database.ChromerDatabase`
- `arun.com.chromer.data.database.dao.*Dao`
- `arun.com.chromer.data.database.entity.*Entity`
- `arun.com.chromer.data.database.DatabaseModule`

**Files to Update:**
- `arun.com.chromer.data.history.HistorySqlDiskStore` → Delete after migration
- `arun.com.chromer.data.history.DefaultHistoryRepository` → Use DAO

### 1.4 DataStore Migration

**Current:** `Preferences.java` with 106 SharedPreferences calls

**Strategy:** Proto DataStore for type-safety

**Proto Schema:**
```protobuf
// preferences.proto
syntax = "proto3";

message UserPreferences {
    bool web_heads_enabled = 1;
    string default_browser_package = 2;
    bool incognito_mode = 3;
    bool amp_mode = 4;
    bool article_mode = 5;
    int32 web_heads_count = 6;
    string theme_color = 7;
    // ... 50+ preferences
}
```

**Implementation:**
```kotlin
@Singleton
class UserPreferencesRepository @Inject constructor(
    private val dataStore: DataStore<UserPreferences>
) {
    val preferences: Flow<UserPreferences> = dataStore.data

    suspend fun setWebHeadsEnabled(enabled: Boolean) {
        dataStore.updateData { current ->
            current.copy { webHeadsEnabled = enabled }
        }
    }
}
```

**Migration:**
```kotlin
val preferencesDataStore = PreferenceDataStoreFactory.create(
    produceFile = { context.dataStoreFile("user_prefs.preferences_pb") },
    migrations = listOf(
        SharedPreferencesMigration(
            context,
            "chromer_preferences",
            keysToMigrate = setOf("web_heads_enabled", "default_browser", ...)
        )
    )
)
```

**Files to Create:**
- `arun.com.chromer.data.preferences.proto/preferences.proto`
- `arun.com.chromer.data.preferences.UserPreferencesRepository`
- `arun.com.chromer.data.preferences.DataStoreModule`

**Files to Delete:**
- `arun.com.chromer.settings.Preferences.java` (after migration)
- `arun.com.chromer.settings.RxPreferences` (after migration)

### 1.5 Repository Layer - RxJava to Flows

**Pattern Transformation:**

```kotlin
// Before: RxJava 2
interface HistoryRepository {
    fun getHistory(): Observable<List<Website>>
    fun insertWebsite(website: Website): Completable
}

class DefaultHistoryRepository @Inject constructor(
    @Disk private val diskStore: HistoryStore
) : HistoryRepository {
    override fun getHistory(): Observable<List<Website>> {
        return diskStore.getHistory()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
    }
}

// After: Kotlin Flows
interface HistoryRepository {
    fun getHistory(): Flow<List<Website>>
    suspend fun insertWebsite(website: Website)
}

class DefaultHistoryRepository @Inject constructor(
    private val historyDao: HistoryDao
) : HistoryRepository {
    override fun getHistory(): Flow<List<Website>> {
        return historyDao.getAllFlow()
            .flowOn(Dispatchers.IO)
    }

    override suspend fun insertWebsite(website: Website) {
        withContext(Dispatchers.IO) {
            historyDao.insert(website)
        }
    }
}
```

**Repositories to Migrate:**
- HistoryRepository (from RxJava Observable to Flow)
- WebsiteRepository (from RxJava to Flow)
- AppsRepository (from RxJava to Flow)
- ProviderRepository (from RxJava to Flow)

**Files:**
- `arun.com.chromer.data.history.*`
- `arun.com.chromer.data.website.*`
- `arun.com.chromer.data.apps.*`
- `arun.com.chromer.data.provider.*`

---

## Phase 2: ViewModel & UI State Management (Weeks 4-5)

### 2.1 ViewModel Migration to Hilt

**Pattern Transformation:**

```kotlin
// Before: Custom Dagger Factory
class HomeActivityViewModel @Inject constructor(
    private val historyRepository: HistoryRepository
) : RxViewModel() {
    val history = MutableLiveData<List<Website>>()

    private val subs = CompositeDisposable()

    init {
        subs.add(
            historyRepository.getHistory()
                .subscribe({ history.value = it }, { error -> })
        )
    }

    override fun onCleared() {
        subs.clear()
    }
}

// After: Hilt + Flows
@HiltViewModel
class HomeActivityViewModel @Inject constructor(
    private val historyRepository: HistoryRepository
) : ViewModel() {
    val history: StateFlow<List<Website>> = historyRepository.getHistory()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
}
```

**ViewModels to Migrate:**
- HomeActivityViewModel
- BrowsingViewModel
- BrowsingArticleViewModel
- HistoryFragmentViewModel
- TabsViewModel
- PerAppSettingsViewModel
- ProviderSelectionViewModel
- SearchViewModel (if exists)

**Files:**
- `arun.com.chromer.home.HomeActivityViewModel`
- `arun.com.chromer.browsing.BrowsingViewModel`
- `arun.com.chromer.history.HistoryFragmentViewModel`
- etc.

**Changes:**
- Remove custom ViewModelFactory
- Add @HiltViewModel annotation
- Replace RxJava subscriptions with Flow collectors in viewModelScope
- Replace LiveData with StateFlow/SharedFlow where appropriate
- Remove manual lifecycle management (onCleared for subscriptions)

### 2.2 RxEventBus Replacement

**Current:** `arun.com.chromer.util.RxEventBus` with PublishSubject

**New Pattern:** Singleton SharedFlow

```kotlin
@Singleton
class EventBus @Inject constructor() {
    private val _events = MutableSharedFlow<Event>(
        replay = 0,
        extraBufferCapacity = 64,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val events: SharedFlow<Event> = _events.asSharedFlow()

    suspend fun emit(event: Event) {
        _events.emit(event)
    }

    inline fun <reified T : Event> observe(): Flow<T> {
        return events.filterIsInstance<T>()
    }
}

sealed interface Event {
    data class WebHeadCreated(val url: String) : Event
    data class TabAdded(val tab: Tab) : Event
    data object RefreshRequested : Event
}
```

**Usage in ViewModel:**
```kotlin
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val eventBus: EventBus
) : ViewModel() {
    init {
        viewModelScope.launch {
            eventBus.observe<Event.RefreshRequested>()
                .collect { refreshData() }
        }
    }
}
```

**Files:**
- Create: `arun.com.chromer.util.EventBus`
- Delete: `arun.com.chromer.util.RxEventBus`
- Update: All files using RxEventBus (23+ occurrences)

### 2.3 Remove RxJava Dependencies

**After all migrations complete:**

```kotlin
// Remove from build.gradle.kts:
// - RxJava 1.x (com.netflix.rxjava:rxjava-android)
// - RxJava 2.x (io.reactivex.rxjava2:rxjava)
// - RxAndroid
// - RxBinding
// - RxKPrefs
// - RxRelay
// - RxJava 1-2 Interop
```

**Verify:** No imports of `rx.*` or `io.reactivex.*` remain

---

## Phase 3: Compose Migration (Weeks 6-10)

### 3.1 Navigation Setup

**Create Navigation Graph:**

```kotlin
// ChromerNavGraph.kt
sealed class Screen(val route: String) {
    object Home : Screen("home")
    object History : Screen("history")
    object Tabs : Screen("tabs")
    object Settings : Screen("settings")
    object BrowserCustomTab : Screen("browser/{url}") {
        fun createRoute(url: String) = "browser/$url"
    }
    object PerAppSettings : Screen("per_app_settings/{package}") {
        fun createRoute(pkg: String) = "per_app_settings/$pkg"
    }
}

@Composable
fun ChromerNavGraph(
    navController: NavHostController = rememberNavController()
) {
    NavHost(navController, startDestination = Screen.Home.route) {
        composable(Screen.Home.route) {
            HomeScreen(navController)
        }
        composable(Screen.History.route) {
            HistoryScreen(navController)
        }
        composable(Screen.Settings.route) {
            SettingsScreen(navController)
        }
        composable(
            Screen.BrowserCustomTab.route,
            arguments = listOf(navArgument("url") { type = NavType.StringType })
        ) {
            BrowserScreen(url = it.arguments?.getString("url")!!)
        }
    }
}
```

**Main Activity:**
```kotlin
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ChromerTheme {
                ChromerNavGraph()
            }
        }
    }
}
```

### 3.2 HomeActivity Compose Migration

**Current:** `activity_main.xml` with EpoxyRecyclerView, MaterialSearchView

**New Structure:**

```kotlin
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel(),
    navController: NavController
) {
    val history by viewModel.history.collectAsStateWithLifecycle()
    val tabs by viewModel.tabs.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            ChromerSearchBar(
                query = searchQuery,
                onQueryChange = viewModel::onSearchQueryChanged,
                onSearch = viewModel::onSearch
            )
        },
        floatingActionButton = {
            if (viewModel.webHeadsEnabled) {
                FloatingActionButton(onClick = viewModel::onWebHeadToggle) {
                    Icon(Icons.Default.BubbleChart, "Web Heads")
                }
            }
        }
    ) { padding ->
        HomeContent(
            modifier = Modifier.padding(padding),
            history = history,
            tabs = tabs,
            onHistoryItemClick = { website ->
                navController.navigate(Screen.BrowserCustomTab.createRoute(website.url))
            },
            onTabClick = { tab ->
                navController.navigate(Screen.BrowserCustomTab.createRoute(tab.url))
            }
        )
    }
}

@Composable
private fun HomeContent(
    modifier: Modifier = Modifier,
    history: List<Website>,
    tabs: List<Tab>,
    onHistoryItemClick: (Website) -> Unit,
    onTabClick: (Tab) -> Unit
) {
    LazyColumn(modifier = modifier) {
        // Tabs section
        if (tabs.isNotEmpty()) {
            item {
                Text(
                    "Active Tabs",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(16.dp)
                )
            }
            items(tabs, key = { it.id }) { tab ->
                TabItem(
                    tab = tab,
                    onClick = { onTabClick(tab) }
                )
            }
        }

        // History section
        item {
            Text(
                "Recent History",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(16.dp)
            )
        }
        items(history, key = { it.url }) { website ->
            HistoryItem(
                website = website,
                onClick = { onHistoryItemClick(website) }
            )
        }
    }
}

@Composable
private fun HistoryItem(
    website: Website,
    onClick: () -> Unit
) {
    ListItem(
        headlineContent = { Text(website.title) },
        supportingContent = { Text(website.url) },
        leadingContent = {
            AsyncImage(
                model = website.favicon,
                contentDescription = null,
                modifier = Modifier.size(40.dp)
            )
        },
        modifier = Modifier.clickable(onClick = onClick)
    )
}
```

**Files:**
- Create: `arun.com.chromer.home.ui.HomeScreen.kt`
- Create: `arun.com.chromer.home.ui.components.*`
- Delete: `res/layout/activity_main.xml`
- Update: `arun.com.chromer.home.HomeActivity` to use setContent

### 3.3 Settings Migration to Compose

**Current:** Multiple PreferenceFragments in XML

**New:** Compose Settings with Material3

```kotlin
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel(),
    navController: NavController
) {
    val preferences by viewModel.preferences.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding)) {
            // Web Heads Section
            item {
                PreferenceCategory("Features")
            }
            item {
                SwitchPreference(
                    title = "Web Heads",
                    summary = "Enable floating bubble heads",
                    checked = preferences.webHeadsEnabled,
                    onCheckedChange = viewModel::setWebHeadsEnabled
                )
            }
            item {
                SwitchPreference(
                    title = "Incognito Mode",
                    summary = "Browse privately",
                    checked = preferences.incognitoMode,
                    onCheckedChange = viewModel::setIncognitoMode
                )
            }

            // Browser Section
            item {
                PreferenceCategory("Browser")
            }
            item {
                ListPreference(
                    title = "Default Browser",
                    summary = preferences.defaultBrowser ?: "System",
                    onClick = {
                        navController.navigate(Screen.BrowserSelection.route)
                    }
                )
            }

            // Per-app Settings
            item {
                PreferenceCategory("Per-app Settings")
            }
            item {
                Preference(
                    title = "App-specific behavior",
                    summary = "Configure behavior for each app",
                    onClick = {
                        navController.navigate(Screen.PerAppSettings.route)
                    }
                )
            }
        }
    }
}

@Composable
private fun SwitchPreference(
    title: String,
    summary: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(summary) },
        trailingContent = {
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange
            )
        },
        modifier = Modifier.clickable { onCheckedChange(!checked) }
    )
}
```

**Files to Create:**
- `arun.com.chromer.settings.ui.SettingsScreen.kt`
- `arun.com.chromer.settings.ui.GeneralSettingsScreen.kt`
- `arun.com.chromer.settings.ui.WebHeadsSettingsScreen.kt`
- `arun.com.chromer.settings.ui.PerAppSettingsScreen.kt`
- `arun.com.chromer.settings.ui.components.PreferenceComponents.kt`

**Files to Delete:**
- `res/xml/*.xml` preference XMLs
- `arun.com.chromer.settings.fragments.*` Java preference fragments

### 3.4 Browsing Activity Compose Wrapper

**Challenge:** Custom Tabs and WebView cannot be fully Compose

**Solution:** Hybrid approach with AndroidView

```kotlin
@Composable
fun BrowserScreen(
    url: String,
    viewModel: BrowsingViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Box(modifier = Modifier.fillMaxSize()) {
        when (uiState.browserMode) {
            BrowserMode.CUSTOM_TAB -> {
                CustomTabView(
                    url = url,
                    customTabsSession = viewModel.customTabsSession
                )
            }
            BrowserMode.WEBVIEW -> {
                WebViewCompose(
                    url = url,
                    onPageLoaded = viewModel::onPageLoaded,
                    onError = viewModel::onError
                )
            }
        }

        // Bottom action bar
        AnimatedVisibility(
            visible = uiState.showActionBar,
            enter = slideInVertically { it },
            exit = slideOutVertically { it }
        ) {
            BrowserActionBar(
                onShare = viewModel::onShare,
                onOpenInBrowser = viewModel::onOpenInBrowser,
                onArticleMode = viewModel::onArticleMode
            )
        }
    }
}

@Composable
private fun WebViewCompose(
    url: String,
    onPageLoaded: (String) -> Unit,
    onError: (String) -> Unit
) {
    AndroidView(
        factory = { context ->
            WebView(context).apply {
                webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView?, url: String?) {
                        url?.let { onPageLoaded(it) }
                    }

                    override fun onReceivedError(
                        view: WebView?,
                        request: WebResourceRequest?,
                        error: WebResourceError?
                    ) {
                        onError(error?.description?.toString() ?: "Unknown error")
                    }
                }
                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                }
            }
        },
        update = { webView ->
            webView.loadUrl(url)
        }
    )
}
```

### 3.5 Theme & Material3 Setup

```kotlin
// ChromerTheme.kt
@Composable
fun ChromerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context)
            else dynamicLightColorScheme(context)
        }
        darkTheme -> darkColorScheme(
            primary = Color(0xFF1E88E5), // Chromer blue
            secondary = Color(0xFF26C6DA),
            background = Color(0xFF121212)
        )
        else -> lightColorScheme(
            primary = Color(0xFF1E88E5),
            secondary = Color(0xFF26C6DA),
            background = Color(0xFFFAFAFA)
        )
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography(
            // Custom font if needed
        ),
        content = content
    )
}
```

---

## Phase 4: Services & Background Work (Week 11)

### 4.1 WebHeadService Modernization

**Current:** Foreground service with overlay permissions

**Challenges:**
- Window Manager overlay (cannot be Compose)
- Needs to remain a Service

**Approach:** Keep Service, modernize internals

```kotlin
@AndroidEntryPoint
class WebHeadService : Service() {
    @Inject lateinit var webHeadManager: WebHeadManager
    @Inject lateinit var preferencesRepository: UserPreferencesRepository

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onCreate() {
        super.onCreate()

        scope.launch {
            preferencesRepository.preferences.collect { prefs ->
                webHeadManager.updateConfiguration(prefs)
            }
        }
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }
}
```

**Files:**
- Update: `arun.com.chromer.webheads.WebHeadService.java` → Kotlin
- Update: `arun.com.chromer.webheads.ui.WebHeadContract` → StateFlow

### 4.2 WorkManager for Background Tasks

**Replace:** Background services where appropriate

```kotlin
@HiltWorker
class CleanupHistoryWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val historyRepository: HistoryRepository
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        return try {
            historyRepository.deleteOldHistory(days = 30)
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}

// Schedule in Application
WorkManager.getInstance(context)
    .enqueueUniquePeriodicWork(
        "cleanup_history",
        ExistingPeriodicWorkPolicy.KEEP,
        PeriodicWorkRequestBuilder<CleanupHistoryWorker>(
            repeatInterval = 1,
            repeatIntervalTimeUnit = TimeUnit.DAYS
        ).build()
    )
```

---

## Phase 5: Testing & Quality (Week 12-13)

### 5.1 Unit Tests

**ViewModel Tests:**
```kotlin
@HiltAndroidTest
class HomeViewModelTest {
    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @Inject lateinit var historyRepository: HistoryRepository

    private lateinit var viewModel: HomeViewModel

    @Before
    fun setup() {
        hiltRule.inject()
        viewModel = HomeViewModel(historyRepository)
    }

    @Test
    fun `when history loaded, state updates correctly`() = runTest {
        // Given
        val mockHistory = listOf(
            Website(url = "https://example.com", title = "Example")
        )
        coEvery { historyRepository.getHistory() } returns flowOf(mockHistory)

        // When
        val state = viewModel.history.first()

        // Then
        assertEquals(mockHistory, state)
    }
}
```

**Repository Tests:**
```kotlin
@RunWith(AndroidJUnit4::class)
class HistoryRepositoryTest {
    private lateinit var database: ChromerDatabase
    private lateinit var historyDao: HistoryDao
    private lateinit var repository: DefaultHistoryRepository

    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            ChromerDatabase::class.java
        ).build()
        historyDao = database.historyDao()
        repository = DefaultHistoryRepository(historyDao)
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun insertAndRetrieve() = runTest {
        // Given
        val website = Website(
            url = "https://test.com",
            title = "Test",
            lastVisit = System.currentTimeMillis()
        )

        // When
        repository.insertWebsite(website)
        val retrieved = repository.getByUrl("https://test.com")

        // Then
        assertEquals(website, retrieved)
    }
}
```

### 5.2 UI Tests

**Compose Tests:**
```kotlin
@HiltAndroidTest
class HomeScreenTest {
    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun historyDisplayed() {
        // Given
        val mockHistory = listOf(
            Website(url = "https://example.com", title = "Example Site")
        )

        // When
        composeRule.setContent {
            ChromerTheme {
                HomeScreen(
                    navController = rememberNavController()
                )
            }
        }

        // Then
        composeRule.onNodeWithText("Example Site").assertIsDisplayed()
        composeRule.onNodeWithText("https://example.com").assertIsDisplayed()
    }

    @Test
    fun searchBarWorks() {
        composeRule.setContent {
            ChromerTheme {
                HomeScreen(navController = rememberNavController())
            }
        }

        composeRule.onNodeWithContentDescription("Search")
            .performClick()
            .performTextInput("https://google.com")

        composeRule.onNodeWithText("https://google.com")
            .assertIsDisplayed()
    }
}
```

---

## Phase 6: Java to Kotlin Migration (Week 14-15)

### 6.1 Automated Conversion

**Android Studio:** Code → Convert Java File to Kotlin File

**Priority Order:**
1. Data models (POJOs)
2. Repositories and stores
3. Activities and Fragments
4. Preference classes
5. Utility classes
6. Services

**Key Files:**
- `arun.com.chromer.settings.Preferences.java` (HIGH PRIORITY)
- `arun.com.chromer.webheads.WebHeadService.java`
- All preference fragments

### 6.2 Kotlin Idioms Refactoring

**After conversion, modernize:**

```kotlin
// Before (Java-style Kotlin)
class Website {
    private var url: String? = null
    private var title: String? = null

    fun getUrl(): String? = url
    fun setUrl(url: String?) { this.url = url }
}

// After (Kotlin idioms)
data class Website(
    val url: String,
    val title: String,
    val favicon: String? = null,
    val visitCount: Int = 0,
    val lastVisit: Long = System.currentTimeMillis()
)
```

---

## Phase 7: Final Cleanup & Polish (Week 16)

### 7.1 Remove Legacy Dependencies

**From build.gradle.kts:**
```kotlin
// DELETE:
implementation("com.jakewharton:butterknife:10.2.3")
implementation("com.airbnb.android:epoxy:4.6.4")
implementation("com.netflix.rxjava:rxjava-android:1.0.17")
implementation("io.reactivex.rxjava2:rxjava:2.2.21")
implementation("io.reactivex.rxjava2:rxandroid:2.1.1")
implementation("com.afollestad.material-dialogs:core:0.9.6.0")
implementation("com.mikepenz:materialdrawer:6.1.2")
implementation("androidx.paging:paging-runtime:2.1.2")
implementation("com.github.bumptech.glide:glide:4.14.2")

// KEEP (already modern):
implementation(libs.androidx.compose.bom)
implementation(libs.androidx.room.runtime)
implementation(libs.androidx.datastore.core)
implementation(libs.coil.compose)
implementation(libs.androidx.hilt.navigation.compose)
implementation(libs.androidx.paging.compose)
```

### 7.2 Update Target SDK

```kotlin
// build.gradle.kts
android {
    compileSdk = 35

    defaultConfig {
        minSdk = 24
        targetSdk = 35 // UP FROM 33
    }
}
```

**Handle Android 14/15 Changes:**
- Foreground service types
- Exact alarm permissions
- Full-screen intent permissions
- PendingIntent mutability

### 7.3 Lint & Code Quality

```bash
./gradlew lint
./gradlew detekt
```

**Fix all warnings:**
- Deprecations
- Security issues
- Performance issues
- Accessibility issues

### 7.4 Performance Optimization

**Baseline Profiles:**
```kotlin
// Generate baseline profile
./gradlew generateBaselineProfile
```

**R8/ProGuard:**
- Optimize code shrinking
- Remove unused resources

---

## Migration Metrics & Success Criteria

### Code Metrics

**Target State:**
- ✅ 0 RxJava imports
- ✅ 0 Butterknife annotations
- ✅ 0 Java files
- ✅ 0 XML layouts (except for things like widgets, notifications)
- ✅ 0 SharedPreferences direct usage
- ✅ 100% Kotlin
- ✅ 100% Compose UI
- ✅ 100% Hilt DI
- ✅ Room for all database operations
- ✅ DataStore for all preferences
- ✅ Target SDK 35

### Quality Gates

**Before merging each phase:**
1. All unit tests pass
2. All UI tests pass
3. No lint errors
4. Code coverage >70%
5. App launches successfully
6. Key features work (Custom Tabs, Web Heads, History)
7. No performance regressions

---

## Risk Mitigation

### High-Risk Areas

**1. Web Heads Service**
- **Risk:** Overlay system is complex, hard to test
- **Mitigation:** Keep existing logic, only modernize data flow
- **Rollback:** Feature flag to disable

**2. Custom Tabs Integration**
- **Risk:** Breaking Chrome Custom Tabs connection
- **Mitigation:** Extensive manual testing with multiple browsers
- **Rollback:** Keep old Activity code in separate branch

**3. Database Migration**
- **Risk:** User data loss during Room migration
- **Mitigation:**
  - Test migration on copy of database
  - Backup before migration
  - Rollback to SQLite if migration fails
  - Add telemetry for migration success/failure

**4. SharedPreferences → DataStore**
- **Risk:** Losing user settings
- **Mitigation:**
  - Use SharedPreferencesMigration built-in
  - Fallback to SharedPreferences if DataStore fails
  - Log all migration attempts

---

## Development Guidelines

### Code Style

**Kotlin:**
```kotlin
// Use explicit types for public APIs
val repository: HistoryRepository = DefaultHistoryRepository(dao)

// Use type inference for locals
val history = repository.getHistory()

// Prefer expression bodies
fun isValid(url: String): Boolean = url.startsWith("http")

// Use scope functions appropriately
val intent = Intent(this, BrowserActivity::class.java).apply {
    putExtra("url", url)
    flags = Intent.FLAG_ACTIVITY_NEW_TASK
}
```

**Compose:**
```kotlin
// State hoisting
@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel = hiltViewModel()
) {
    val history by viewModel.history.collectAsStateWithLifecycle()

    HistoryContent(
        history = history,
        onItemClick = viewModel::onHistoryItemClick
    )
}

@Composable
private fun HistoryContent(
    history: List<Website>,
    onItemClick: (Website) -> Unit
) {
    LazyColumn {
        items(history, key = { it.url }) { website ->
            HistoryItem(website, onItemClick)
        }
    }
}

// Separate stateless components
@Composable
private fun HistoryItem(
    website: Website,
    onClick: (Website) -> Unit
) {
    // ...
}
```

### Git Strategy

**Branch Naming:**
- `feature/phase1-hilt-setup`
- `feature/phase1-room-database`
- `feature/phase2-viewmodels`
- `feature/phase3-home-compose`
- etc.

**Commit Strategy:**
- Small, atomic commits
- Each commit should compile
- Clear commit messages

**PR Strategy:**
- One PR per sub-phase
- Request review after each major component
- Don't merge broken code

---

## Timeline Summary

| Phase | Duration | Focus | Deliverable |
|-------|----------|-------|-------------|
| 1 | 3 weeks | Foundation | Hilt + Room + DataStore |
| 2 | 2 weeks | ViewModels | All VMs using Flows |
| 3 | 5 weeks | UI | Compose for all screens |
| 4 | 1 week | Services | Modernized services |
| 5 | 2 weeks | Testing | Comprehensive tests |
| 6 | 2 weeks | Kotlin | 100% Kotlin |
| 7 | 1 week | Polish | Production ready |
| **Total** | **16 weeks** | | **Modern app** |

---

## Conclusion

This modernization transforms Lynket Browser from a legacy Android app into a showcase of modern Android development:

- **Developer Experience:** Easier to maintain with Kotlin, Compose, Hilt
- **Performance:** Better performance with coroutines, Room, Compose
- **User Experience:** Modern Material3 UI, smooth animations
- **Future-Proof:** Built on Jetpack libraries with long-term Google support

The playground module already demonstrates the target architecture. This plan systematically brings the main app to that standard while maintaining stability throughout the migration.
