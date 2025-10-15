package com.example.birthdayreminder

import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*

import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope
import com.google.api.client.extensions.android.http.AndroidHttp
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.calendar.Calendar
import com.google.api.services.calendar.CalendarScopes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.Month

// DataStore extension
val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "birthdays")

data class Birthday(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String,
    val month: Month,
    val day: Int,
    val isFromCalendar: Boolean = false,
    val calendarEventId: String? = null
) {
    fun toJson(): JSONObject {
        return JSONObject().apply {
            put("id", id)
            put("name", name)
            put("month", month.value)
            put("day", day)
            put("isFromCalendar", isFromCalendar)
            put("calendarEventId", calendarEventId ?: "")
        }
    }

    companion object {
        fun fromJson(json: JSONObject): Birthday {
            return Birthday(
                id = json.getString("id"),
                name = json.getString("name"),
                month = Month.of(json.getInt("month")),
                day = json.getInt("day"),
                isFromCalendar = json.getBoolean("isFromCalendar"),
                calendarEventId = json.optString("calendarEventId").takeIf { it.isNotEmpty() }
            )
        }
    }
}

class BirthdayRepository(private val context: Context) {
    private val birthdaysKey = stringPreferencesKey("birthdays_list")

    suspend fun saveBirthdays(birthdays: List<Birthday>) {
        context.dataStore.edit { prefs ->
            val jsonArray = JSONArray()
            birthdays.forEach { birthday ->
                jsonArray.put(birthday.toJson())
            }
            prefs[birthdaysKey] = jsonArray.toString()
        }
    }

    suspend fun loadBirthdays(): List<Birthday> {
        return context.dataStore.data.map { prefs ->
            try {
                val jsonString = prefs[birthdaysKey] ?: return@map emptyList()
                val jsonArray = JSONArray(jsonString)
                List(jsonArray.length()) { i ->
                    Birthday.fromJson(jsonArray.getJSONObject(i))
                }
            } catch (e: Exception) {
                android.util.Log.e("BirthdayRepository", "Error loading birthdays", e)
                emptyList()
            }
        }.first()
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BirthdayTrackerTheme {
                BirthdayTrackerApp()
            }
        }
    }
}

@Composable
fun BirthdayTrackerTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Color(0xFFFF6B9D),
            secondary = Color(0xFFC44569),
            tertiary = Color(0xFFFFA07A),
            background = Color(0xFF1A1625),
            surface = Color(0xFF2D2640),
            onPrimary = Color.White,
            onSecondary = Color.White,
            onBackground = Color(0xFFF5F5F5),
            onSurface = Color(0xFFF5F5F5)
        ),
        content = content
    )
}

class CalendarService(private val context: Context) {
    private var googleSignInClient: GoogleSignInClient? = null
    private var calendarService: Calendar? = null

    // REPLACE THIS WITH YOUR ACTUAL WEB CLIENT ID FROM GOOGLE CLOUD CONSOLE
    private val webClientId = "213370881987-p8g76vejjvgeilckcs8vha7lpirdeb8h.apps.googleusercontent.com"

    fun setupGoogleSignIn(): GoogleSignInClient {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestIdToken(webClientId)
            .requestServerAuthCode(webClientId)
            .requestScopes(Scope(CalendarScopes.CALENDAR_READONLY))
            .build()

        googleSignInClient = GoogleSignIn.getClient(context, gso)
        return googleSignInClient!!
    }

    fun initializeCalendarService(account: GoogleSignInAccount) {
        try {
            val credential = GoogleAccountCredential.usingOAuth2(
                context,
                listOf(CalendarScopes.CALENDAR_READONLY)
            )
            credential.selectedAccount = account.account

            calendarService = Calendar.Builder(
                AndroidHttp.newCompatibleTransport(),
                GsonFactory.getDefaultInstance(),
                credential
            )
                .setApplicationName("Birthday Reminder")
                .build()

            android.util.Log.d("CalendarService", "Calendar service initialized successfully")
        } catch (e: Exception) {
            android.util.Log.e("CalendarService", "Error initializing calendar service", e)
            throw e
        }
    }

    suspend fun fetchBirthdaysFromCalendar(): List<Birthday> = withContext(Dispatchers.IO) {
        try {
            val service = calendarService ?: run {
                android.util.Log.e("CalendarService", "Calendar service is null")
                return@withContext emptyList()
            }

            val birthdays = mutableListOf<Birthday>()

            android.util.Log.d("CalendarService", "Starting to fetch calendars...")

            // Get all calendars
            val calendarList = service.calendarList().list().execute()
            android.util.Log.d("CalendarService", "Found ${calendarList.items?.size ?: 0} calendars")

            calendarList.items?.forEach { calendar ->
                android.util.Log.d("CalendarService", "Calendar: '${calendar.summary}' (ID: ${calendar.id})")

                // Check for birthday-related calendars
                val isBirthdayCalendar = calendar.summary?.contains("birthday", ignoreCase = true) == true ||
                        calendar.summary?.contains("birthdays", ignoreCase = true) == true ||
                        calendar.id?.contains("birthday", ignoreCase = true) == true ||
                        calendar.id?.contains("#contacts@group.v.calendar.google.com") == true

                if (isBirthdayCalendar) {
                    android.util.Log.d("CalendarService", "✓ Identified as birthday calendar: ${calendar.summary}")

                    try {
                        // Fetch events from this calendar - use a wider time range
                        val now = System.currentTimeMillis()
                        val oneYearAgo = now - (365L * 24 * 60 * 60 * 1000)
                        val oneYearAhead = now + (365L * 24 * 60 * 60 * 1000)

                        val events = service.events().list(calendar.id)
                            .setMaxResults(500)
                            .setTimeMin(com.google.api.client.util.DateTime(oneYearAgo))
                            .setTimeMax(com.google.api.client.util.DateTime(oneYearAhead))
                            .setSingleEvents(true)
                            .setOrderBy("startTime")
                            .execute()

                        android.util.Log.d("CalendarService", "Found ${events.items?.size ?: 0} events in '${calendar.summary}'")

                        events.items?.forEach { event ->
                            try {
                                val summary = event.summary ?: ""
                                android.util.Log.d("CalendarService", "Event: '$summary'")

                                // Parse birthday date
                                val dateStr = event.start?.date?.toString() ?: event.start?.dateTime?.toString()

                                if (dateStr != null) {
                                    android.util.Log.d("CalendarService", "  Date: $dateStr")

                                    val parts = dateStr.split("-")
                                    if (parts.size >= 3) {
                                        val month = Month.of(parts[1].toInt())
                                        val dayStr = parts[2].substringBefore("T")
                                        val day = dayStr.toInt()

                                        var name = summary

                                        // Clean up the name - remove common birthday indicators
                                        name = name
                                            .replace(Regex("'s [Bb]irthday"), "")
                                            .replace(Regex("[Bb]irthday:?"), "")
                                            .replace("🎂", "")
                                            .replace("🎉", "")
                                            .replace("🎈", "")
                                            .replace("🎁", "")
                                            .trim()

                                        if (name.isNotBlank()) {
                                            val birthday = Birthday(
                                                name = name,
                                                month = month,
                                                day = day,
                                                isFromCalendar = true,
                                                calendarEventId = event.id
                                            )
                                            birthdays.add(birthday)
                                            android.util.Log.d("CalendarService", "  ✓ Added: $name on $month $day")
                                        } else {
                                            android.util.Log.w("CalendarService", "  ✗ Skipped: Empty name after cleanup")
                                        }
                                    } else {
                                        android.util.Log.w("CalendarService", "  ✗ Invalid date format: $dateStr")
                                    }
                                } else {
                                    android.util.Log.w("CalendarService", "  ✗ No date found for event")
                                }
                            } catch (e: Exception) {
                                android.util.Log.e("CalendarService", "Error parsing event: ${event.summary}", e)
                            }
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("CalendarService", "Error fetching events from '${calendar.summary}'", e)
                        e.printStackTrace()
                    }
                } else {
                    android.util.Log.d("CalendarService", "✗ Not a birthday calendar")
                }
            }

            android.util.Log.d("CalendarService", "Total birthdays found: ${birthdays.size}")
            birthdays
        } catch (e: Exception) {
            android.util.Log.e("CalendarService", "Error fetching calendars", e)
            e.printStackTrace()
            emptyList()
        }
    }

    fun signOut() {
        googleSignInClient?.signOut()
        calendarService = null
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BirthdayTrackerApp() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repository = remember { BirthdayRepository(context) }
    val calendarService = remember { CalendarService(context) }

    var birthdays by remember { mutableStateOf(listOf<Birthday>()) }
    var showDialog by remember { mutableStateOf(false) }
    var showFilterPanel by remember { mutableStateOf(false) }
    val currentMonth = LocalDate.now().month
    var selectedMonth by remember { mutableStateOf(currentMonth) }
    var isSignedIn by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }

    // Load saved birthdays on startup
    LaunchedEffect(Unit) {
        isLoading = true
        try {
            birthdays = repository.loadBirthdays()
            android.util.Log.d("BirthdayTracker", "Loaded ${birthdays.size} birthdays from storage")
        } catch (e: Exception) {
            android.util.Log.e("BirthdayTracker", "Error loading birthdays", e)
        } finally {
            isLoading = false
        }
    }

    // Save birthdays whenever they change
    LaunchedEffect(birthdays) {
        if (birthdays.isNotEmpty()) {
            try {
                repository.saveBirthdays(birthdays)
                android.util.Log.d("BirthdayTracker", "Saved ${birthdays.size} birthdays to storage")
            } catch (e: Exception) {
                android.util.Log.e("BirthdayTracker", "Error saving birthdays", e)
            }
        }
    }

    val signInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        android.util.Log.d("BirthdayTracker", "Sign-in result code: ${result.resultCode}")
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            if (account != null) {
                android.util.Log.d("BirthdayTracker", "Sign-in successful: ${account.email}")
                android.util.Log.d("BirthdayTracker", "Granted scopes: ${account.grantedScopes}")

                calendarService.initializeCalendarService(account)
                isSignedIn = true

                // Fetch birthdays
                scope.launch {
                    isLoading = true
                    try {
                        android.util.Log.d("BirthdayTracker", "Starting to fetch calendar birthdays...")
                        val calendarBirthdays = calendarService.fetchBirthdaysFromCalendar()
                        android.util.Log.d("BirthdayTracker", "Fetched ${calendarBirthdays.size} birthdays from calendar")

                        // Merge with existing birthdays, avoiding duplicates
                        val existingBirthdays = birthdays.filter { !it.isFromCalendar }
                        birthdays = (existingBirthdays + calendarBirthdays)
                            .distinctBy { "${it.name.lowercase().trim()}_${it.month}_${it.day}" }

                        Toast.makeText(
                            context,
                            "Loaded ${calendarBirthdays.size} birthdays from Calendar",
                            Toast.LENGTH_LONG
                        ).show()
                    } catch (e: Exception) {
                        Toast.makeText(context, "Error loading birthdays: ${e.message}", Toast.LENGTH_LONG).show()
                        android.util.Log.e("BirthdayTracker", "Error fetching birthdays", e)
                        e.printStackTrace()
                    } finally {
                        isLoading = false
                    }
                }
            } else {
                android.util.Log.e("BirthdayTracker", "Account is null after sign-in")
                Toast.makeText(context, "Sign-in failed: No account returned", Toast.LENGTH_LONG).show()
            }
        } catch (e: ApiException) {
            val errorMessage = when (e.statusCode) {
                10 -> "Developer error: Check OAuth client ID configuration"
                12501 -> "Sign-in cancelled by user"
                7 -> "Network error: Check internet connection"
                else -> "Sign-in failed: ${e.message} (Code: ${e.statusCode})"
            }
            Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
            android.util.Log.e("BirthdayTracker", "Sign-in ApiException: $errorMessage", e)
            e.printStackTrace()
        } catch (e: Exception) {
            Toast.makeText(context, "Sign-in failed: ${e.message}", Toast.LENGTH_LONG).show()
            android.util.Log.e("BirthdayTracker", "Sign-in error: ${e.javaClass.simpleName}", e)
            e.printStackTrace()
        }
    }

    val permissionsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        android.util.Log.d("BirthdayTracker", "Permissions result: $permissions")
        val allGranted = permissions.all { it.value }

        if (allGranted) {
            android.util.Log.d("BirthdayTracker", "All permissions granted, starting sign-in")
            try {
                val signInIntent = calendarService.setupGoogleSignIn().signInIntent
                signInLauncher.launch(signInIntent)
            } catch (e: Exception) {
                Toast.makeText(context, "Error setting up sign-in: ${e.message}", Toast.LENGTH_LONG).show()
                android.util.Log.e("BirthdayTracker", "Error setting up Google Sign-In", e)
            }
        } else {
            val deniedPermissions = permissions.filter { !it.value }.keys
            val message = "Required permissions denied: ${deniedPermissions.joinToString(", ") {
                it.substringAfterLast(".")
            }}"
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
            android.util.Log.w("BirthdayTracker", message)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Birthday Tracker", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { showFilterPanel = !showFilterPanel }) {
                        Icon(
                            Icons.Default.Menu,
                            contentDescription = "Filter by month",
                            tint = if (showFilterPanel) Color(0xFFFF6B9D) else Color.White
                        )
                    }
                },
                actions = {
                    if (isSignedIn) {
                        IconButton(
                            onClick = {
                                scope.launch {
                                    isLoading = true
                                    try {
                                        val calendarBirthdays = calendarService.fetchBirthdaysFromCalendar()
                                        val existingBirthdays = birthdays.filter { !it.isFromCalendar }
                                        birthdays = (existingBirthdays + calendarBirthdays)
                                            .distinctBy { "${it.name.lowercase().trim()}_${it.month}_${it.day}" }
                                        Toast.makeText(context, "Refreshed: ${calendarBirthdays.size} birthdays", Toast.LENGTH_SHORT).show()
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                                        android.util.Log.e("BirthdayTracker", "Error refreshing", e)
                                    } finally {
                                        isLoading = false
                                    }
                                }
                            }
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(Icons.Default.Refresh, "Sync Calendar", tint = Color.White)
                            }
                        }
                    } else {
                        Button(
                            onClick = {
                                android.util.Log.d("BirthdayTracker", "Connect Calendar button clicked")
                                permissionsLauncher.launch(
                                    arrayOf(
                                        android.Manifest.permission.READ_CALENDAR,
                                        android.Manifest.permission.GET_ACCOUNTS
                                    )
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF6B9D)),
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Text("Connect Calendar", fontSize = 12.sp)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF2D2640),
                    titleContentColor = Color.White
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showDialog = true },
                containerColor = Color(0xFFFF6B9D),
                contentColor = Color.White,
                modifier = Modifier.size(64.dp),
                shape = CircleShape
            ) {
                Icon(Icons.Default.Add, "Add Birthday", modifier = Modifier.size(28.dp))
            }
        }
    ) { padding ->
        Row(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF1A1625),
                            Color(0xFF2D2640)
                        )
                    )
                )
                .padding(padding)
        ) {
            AnimatedVisibility(
                visible = showFilterPanel,
                enter = slideInHorizontally(initialOffsetX = { -it }) + fadeIn(),
                exit = slideOutHorizontally(targetOffsetX = { -it }) + fadeOut()
            ) {
                MonthFilterPanel(
                    selectedMonth = selectedMonth,
                    onMonthSelect = { month ->
                        selectedMonth = month
                        showFilterPanel = false
                    }
                )
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        val monthBirthdays = birthdays.filter { it.month == selectedMonth }

                        item {
                            AnimatedVisibility(
                                visible = true,
                                enter = fadeIn() + expandVertically(),
                                exit = fadeOut() + shrinkVertically()
                            ) {
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = Color(0xFF3D3450)
                                    ),
                                    shape = RoundedCornerShape(20.dp),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                                ) {
                                    Column(modifier = Modifier.padding(20.dp)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(
                                                text = "${selectedMonth.name.lowercase().replaceFirstChar { it.uppercase() }}",
                                                style = MaterialTheme.typography.headlineSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = if (selectedMonth == currentMonth) Color(0xFFFF6B9D) else Color(0xFFFFA07A)
                                            )
                                            Surface(
                                                shape = RoundedCornerShape(12.dp),
                                                color = if (selectedMonth == currentMonth)
                                                    Color(0xFFFF6B9D).copy(alpha = 0.2f)
                                                else
                                                    Color(0xFFFFA07A).copy(alpha = 0.2f)
                                            ) {
                                                Text(
                                                    text = "${monthBirthdays.size}",
                                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                                    color = if (selectedMonth == currentMonth) Color(0xFFFF6B9D) else Color(0xFFFFA07A),
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(12.dp))
                                        if (monthBirthdays.isEmpty()) {
                                            Text(
                                                "No birthdays this month",
                                                color = Color.White.copy(alpha = 0.6f),
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                        } else {
                                            monthBirthdays.sortedBy { it.day }.forEach { birthday ->
                                                BirthdayItem(birthday) {
                                                    if (!birthday.isFromCalendar) {
                                                        birthdays = birthdays.filter { it.id != birthday.id }
                                                    } else {
                                                        Toast.makeText(context, "Cannot delete calendar birthdays", Toast.LENGTH_SHORT).show()
                                                    }
                                                }
                                                Spacer(modifier = Modifier.height(8.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        if (monthBirthdays.isEmpty()) {
                            item {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 48.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(text = "🎈", fontSize = 64.sp)
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        text = "No birthdays yet",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = Color.White.copy(alpha = 0.6f)
                                    )
                                    Text(
                                        text = if (!isSignedIn) "Connect Google Calendar or tap + to add" else "Tap the + button to add one",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color.White.copy(alpha = 0.4f)
                                    )
                                }
                            }
                        }
                    }
                }

                if (showDialog) {
                    AddBirthdayDialog(
                        selectedMonth = selectedMonth,
                        onDismiss = { showDialog = false },
                        onAdd = { name, month, day ->
                            birthdays = birthdays + Birthday(name = name, month = month, day = day)
                            selectedMonth = month  // Switch to the month of the newly added birthday
                            showDialog = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun MonthFilterPanel(
    selectedMonth: Month,
    onMonthSelect: (Month) -> Unit
) {
    Surface(
        modifier = Modifier
            .width(200.dp)
            .fillMaxHeight(),
        color = Color(0xFF2D2640),
        shadowElevation = 8.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Text(
                text = "Select Month",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFFF6B9D),
                modifier = Modifier.padding(bottom = 16.dp)
            )

            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(Month.values().toList()) { month ->
                    MonthFilterItem(
                        month = month,
                        isSelected = selectedMonth == month,
                        onClick = { onMonthSelect(month) }
                    )
                }
            }
        }
    }
}

@Composable
fun MonthFilterItem(
    month: Month,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor = if (isSelected) {
        Color(0xFFFF6B9D).copy(alpha = 0.2f)
    } else {
        Color(0xFF3D3450)
    }

    val borderColor = if (isSelected) Color(0xFFFF6B9D) else Color.Transparent

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .border(width = 2.dp, color = borderColor, shape = RoundedCornerShape(12.dp)),
        color = backgroundColor,
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = month.name.lowercase().replaceFirstChar { it.uppercase() },
                color = if (isSelected) Color.White else Color.White.copy(alpha = 0.7f),
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                fontSize = 14.sp
            )

            if (isSelected) {
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFFF6B9D)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "✓",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun BirthdayItem(birthday: Birthday, onDelete: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        if (birthday.isFromCalendar)
                            Color(0xFF4285F4).copy(alpha = 0.2f)
                        else
                            Color(0xFFFF6B9D).copy(alpha = 0.2f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${birthday.day}",
                    color = if (birthday.isFromCalendar) Color(0xFF4285F4) else Color(0xFFFF6B9D),
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    birthday.name,
                    color = Color.White,
                    fontWeight = FontWeight.Medium
                )
                if (birthday.isFromCalendar) {
                    Text(
                        "From Google Calendar",
                        color = Color(0xFF4285F4).copy(alpha = 0.7f),
                        fontSize = 10.sp
                    )
                }
            }
        }
        IconButton(onClick = onDelete) {
            Icon(
                Icons.Default.Delete,
                contentDescription = "Delete",
                tint = if (birthday.isFromCalendar) Color.White.copy(alpha = 0.3f) else Color.White.copy(alpha = 0.6f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddBirthdayDialog(
    selectedMonth: Month,
    onDismiss: () -> Unit,
    onAdd: (String, Month, Int) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var selectedMonthState by remember { mutableStateOf(selectedMonth) }
    var day by remember { mutableStateOf("") }
    var expandedMonth by remember { mutableStateOf(false) }
    var showError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank() && day.isNotBlank()) {
                        val dayInt = day.toIntOrNull()
                        if (dayInt != null && dayInt in 1..31) {
                            onAdd(name, selectedMonthState, dayInt)
                            showError = false
                        } else {
                            showError = true
                        }
                    } else {
                        showError = true
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF6B9D)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Add", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, shape = RoundedCornerShape(12.dp)) {
                Text("Cancel", color = Color.White.copy(alpha = 0.7f))
            }
        },
        title = {
            Text(
                "Add Birthday",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFFF6B9D)
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        showError = false
                    },
                    label = { Text("Name") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFFFF6B9D),
                        focusedLabelColor = Color(0xFFFF6B9D),
                        unfocusedBorderColor = Color.White.copy(alpha = 0.5f),
                        unfocusedLabelColor = Color.White.copy(alpha = 0.7f),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )
                Spacer(modifier = Modifier.height(12.dp))
                ExposedDropdownMenuBox(
                    expanded = expandedMonth,
                    onExpandedChange = { expandedMonth = it }
                ) {
                    OutlinedTextField(
                        value = selectedMonthState.name.lowercase().replaceFirstChar { it.uppercase() },
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Month") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedMonth) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFFF6B9D),
                            focusedLabelColor = Color(0xFFFF6B9D),
                            unfocusedBorderColor = Color.White.copy(alpha = 0.5f),
                            unfocusedLabelColor = Color.White.copy(alpha = 0.7f),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                    ExposedDropdownMenu(
                        expanded = expandedMonth,
                        onDismissRequest = { expandedMonth = false },
                        modifier = Modifier.background(Color(0xFF3D3450))
                    ) {
                        Month.values().forEach { month ->
                            DropdownMenuItem(
                                text = { Text(month.name.lowercase().replaceFirstChar { it.uppercase() }, color = Color.White) },
                                onClick = {
                                    selectedMonthState = month
                                    expandedMonth = false
                                }
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = day,
                    onValueChange = {
                        if (it.all { c -> c.isDigit() } && it.length <= 2) {
                            day = it
                            showError = false
                        }
                    },
                    label = { Text("Day") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    isError = showError && (day.isBlank() || day.toIntOrNull() !in 1..31),
                    supportingText = {
                        if (showError) {
                            Text("Please enter valid information", color = Color(0xFFFF6B9D))
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFFFF6B9D),
                        focusedLabelColor = Color(0xFFFF6B9D),
                        unfocusedBorderColor = Color.White.copy(alpha = 0.5f),
                        unfocusedLabelColor = Color.White.copy(alpha = 0.7f),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        errorBorderColor = Color(0xFFFF6B9D),
                        errorLabelColor = Color(0xFFFF6B9D)
                    )
                )
            }
        },
        containerColor = Color(0xFF2D2640),
        shape = RoundedCornerShape(24.dp)
    )
}