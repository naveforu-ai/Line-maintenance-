package com.example

import android.os.Bundle
import android.widget.Toast
import android.speech.tts.TextToSpeech
import android.media.ToneGenerator
import android.media.AudioManager
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.text.TextStyle
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.Date
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.data.DefectEntity
import com.example.data.TaskEntity
import com.example.ui.GridViewModel
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.SafetyOrange
import com.example.ui.theme.SafetyYellow
import com.example.ui.theme.SafetyRed
import com.example.ui.theme.FieldGreen
import com.example.ui.theme.CyberBlue
import com.example.ui.theme.CyberDark
import com.example.ui.theme.CyberCard
import com.example.ui.theme.CyberGray
import com.example.ui.theme.CyberTextSecondary
import com.example.ui.theme.BorderColor
import com.example.ui.theme.CustomLavBackground
import com.example.ui.theme.CustomLavText
import com.example.ui.theme.CustomBlueBackground
import com.example.ui.theme.CustomBlueText

enum class AppSubTab {
    JOBS, LOGISTICS, GIS, CALENDAR, FAULTS
}

class MainActivity : ComponentActivity() {
    private var tts: TextToSpeech? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize Real system TTS for Telugu / English vocalizations
        tts = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val result = tts?.setLanguage(Locale("te", "IN"))
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    tts?.language = Locale.US
                }
            }
        }

        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                MainScreen(tts = tts)
            }
        }
    }

    override fun onDestroy() {
        tts?.shutdown()
        super.onDestroy()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: GridViewModel = viewModel(), tts: TextToSpeech? = null) {
    val tasks by viewModel.tasks.collectAsState()
    val defects by viewModel.defects.collectAsState()
    val isOffline by viewModel.isOffline.collectAsState()
    val safetyChecks by viewModel.safetyChecks.collectAsState()
    val isSafetyUnlocked by viewModel.isSafetyUnlocked.collectAsState()

    // Screen State
    var activeTab by remember { mutableStateOf(AppSubTab.JOBS) }
    var showSosNotification by remember { mutableStateOf(false) }
    var selectedSosType by remember { mutableStateOf<String?>(null) }

    // Alert simulation banner
    var alertBannerText by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current

    // Birthday and Build Suite states
    val coroutineScope = rememberCoroutineScope()
    var showBuildSuite by remember { mutableStateOf(false) }
    var birthdayWorkerName by remember { mutableStateOf("Y. Ramanappa (Driver)") }
    var showBirthdayBanner by remember { mutableStateOf(true) }
    var isEditingDob by remember { mutableStateOf(false) }
    var dobInput by remember { mutableStateOf("May 26") }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            HeaderBar(
                isOffline = isOffline,
                onOfflineToggle = { viewModel.toggleOfflineMode() },
                onTriggerSos = {
                    showSosNotification = true
                }
            )
        },
        bottomBar = {
            BottomDeck(
                activeTab = activeTab,
                onTabSelect = { activeTab = it },
                onTriggerSafety = {
                    // Force display gate checklist
                    activeTab = AppSubTab.JOBS
                }
            )
        },
        containerColor = CyberDark
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                // Offline Notice Bar
                if (isOffline) {
                    OfflineWarningBar()
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // Build Suite and Title row
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("APTRANSCO Grid & Logistics Hub", color = Color.Gray, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(CustomLavText.copy(alpha = 0.15f))
                            .clickable { showBuildSuite = true }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text("🛠️ BUILD SUITE", color = CustomLavText, fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    }
                }

                // Birthday Greeting Header Banner
                if (showBirthdayBanner) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFF8B0000) // Deep Crimson
                        ),
                        border = BorderStroke(2.dp, Color(0xFFD4AF37)) // Golden rim
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("🎂 GRID TEAM BIRTHDAY HONOURS 🎂", color = Color(0xFFD4AF37), fontSize = 11.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "APTRANSCO warmly greets $birthdayWorkerName on their anniversary today!",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = {
                                        // Play chiptune chime
                                        try {
                                            val toneGen = ToneGenerator(AudioManager.STREAM_ALARM, 100)
                                            toneGen.startTone(ToneGenerator.TONE_CDMA_HIGH_L, 200)
                                            coroutineScope.launch {
                                                delay(200)
                                                toneGen.startTone(ToneGenerator.TONE_CDMA_MED_L, 200)
                                                delay(200)
                                                toneGen.startTone(ToneGenerator.TONE_CDMA_LOW_L, 250)
                                            }
                                        } catch (e: Exception) {
                                            e.printStackTrace()
                                        }
                                        alertBannerText = "🎉 Played Chiptune Birthday Chimes for $birthdayWorkerName!"
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD4AF37)),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text("🎵 PLAY BELLS", color = Color.Black, fontSize = 9.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                                }
                                Button(
                                    onClick = { showBirthdayBanner = false },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text("CLOSE", color = Color.White, fontSize = 9.sp)
                                }
                            }
                            
                            if (isEditingDob) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.padding(top = 8.dp)
                                ) {
                                    OutlinedTextField(
                                        value = dobInput,
                                        onValueChange = { dobInput = it },
                                        placeholder = { Text("e.g. May 26", fontSize = 10.sp) },
                                        modifier = Modifier.width(120.dp).height(44.dp),
                                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White)
                                    )
                                    Button(
                                        onClick = {
                                            isEditingDob = false
                                            if (dobInput.lowercase().contains("may 26")) {
                                                birthdayWorkerName = "Y. Ramanappa (Driver)"
                                                showBirthdayBanner = true
                                            } else {
                                                birthdayWorkerName = "Custom Worker ($dobInput)"
                                                showBirthdayBanner = true
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD4AF37))
                                    ) {
                                        Text("SET", color = Color.Black, fontSize = 9.sp)
                                    }
                                }
                            } else {
                                Text(
                                    "Change DOB",
                                    color = Color(0xFFD4AF37),
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    modifier = Modifier.clickable { isEditingDob = true }.padding(top = 4.dp),
                                    style = TextStyle(textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline)
                                )
                            }
                        }
                    }
                }

                // Alert notification banner (dynamic feedback)
                alertBannerText?.let {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        colors = CardDefaults.cardColors(containerColor = SafetyOrange.copy(alpha = 0.15f)),
                        border = BorderStroke(1.dp, SafetyOrange.copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = "Alert",
                                    tint = SafetyOrange,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = it,
                                    color = CustomLavText,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            IconButton(
                                onClick = { alertBannerText = null },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "CloseAlert",
                                    tint = Color.LightGray,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }

                // Section view selection
                when (activeTab) {
                    AppSubTab.JOBS -> {
                        var jobsMenuTab by remember { mutableStateOf("OVERVIEW") }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp)
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(
                                "OVERVIEW" to "🏛️ OVERVIEW",
                                "DUTIES" to "⚡ JOBS",
                                "FEEDERS" to "📊 FEEDERS",
                                "PMI_LOG" to "🔌 GROUND GRID",
                                "PTW" to "✓ PTW LEDGER"
                            ).forEach { (id, label) ->
                                Button(
                                    onClick = { jobsMenuTab = id },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (jobsMenuTab == id) CustomLavText else CyberGray
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text(label, fontSize = 8.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, maxLines = 1)
                                }
                            }
                        }

                        when (jobsMenuTab) {
                            "OVERVIEW" -> {
                                MainOverviewDashboard(
                                    tasks = tasks,
                                    defects = defects,
                                    completedWorkOrders = viewModel.completedWorkOrders.collectAsState().value,
                                    isSafetyUnlocked = isSafetyUnlocked,
                                    onNavigateToTab = { targetSubTab ->
                                        jobsMenuTab = targetSubTab
                                    },
                                    onStartPatrol = { taskId ->
                                        viewModel.startTask(taskId)
                                        alertBannerText = "⚡ PATROL ACTIVE: $taskId ON-AIR"
                                    },
                                    onCompletePatrol = { taskId ->
                                        viewModel.completeTask(taskId)
                                        alertBannerText = "✓ patrol completed: $taskId archived"
                                    },
                                    onApproveSafety = {
                                        viewModel.approveSafetyClearance()
                                        alertBannerText = "✓ PRE-WORK CLEARANCE UNLOCKED: S-4 SEGMENT ACTIVE"
                                    },
                                    onToggleCheck = { index ->
                                        viewModel.toggleSafetyCheck(index)
                                    },
                                    safetyChecks = safetyChecks
                                )
                            }

                            "DUTIES" -> {
                                SupervisorBanner(tasks = tasks, isSafetyUnlocked = isSafetyUnlocked)
                                Spacer(modifier = Modifier.height(16.dp))

                                // SAFETY PASS UNLOCK WINDOW
                                if (!isSafetyUnlocked) {
                                    SafetyLocknagBox(
                                        onNavigateToUnlock = { }
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))

                                    // Interactive inline safety gate checklist
                                    SafetyGateChecksInteractive(
                                        safetyChecks = safetyChecks,
                                        onCheckToggle = { viewModel.toggleSafetyCheck(it) },
                                        onApprove = {
                                            viewModel.approveSafetyClearance()
                                            alertBannerText = "✓ PRE-WORK CLEARANCE UNLOCKED: S-4 SEGMENT ACTIVE"
                                        }
                                    )
                                } else {
                                    // Display full jobs list
                                    JobDutyTitleRow(onResetSafety = {
                                        viewModel.resetSafetyChecks()
                                        alertBannerText = "⚠ WORK SAFETIES DE-AUTHORIZED"
                                    })
                                    Spacer(modifier = Modifier.height(12.dp))

                                    if (tasks.isEmpty()) {
                                        EmptyStateCard(message = "No duties dispatched today.")
                                    } else {
                                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                            tasks.forEach { task ->
                                                TaskCard(
                                                    task = task,
                                                    isSafetyCleared = isSafetyUnlocked,
                                                    onStart = {
                                                        viewModel.startTask(task.id)
                                                        alertBannerText = "⚡ PATROL ACTIVE: ${task.id} ON-AIR"
                                                    },
                                                    onComplete = {
                                                        viewModel.completeTask(task.id)
                                                        alertBannerText = "✓patrol completed: ${task.id} archived"
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                            
                            "FEEDERS" -> {
                                PatrollingTrackerDashboard(viewModel = viewModel)
                            }
                            
                            "PMI_LOG" -> {
                                PmiGroundGridLoggerView(viewModel = viewModel, alertBannerText = alertBannerText) { alertBannerText = it }
                            }
                            
                            "PTW" -> {
                                CompletedWorkLedgerView(viewModel = viewModel, alertBannerText = alertBannerText) { alertBannerText = it }
                            }
                        }
                    }

                    AppSubTab.LOGISTICS -> {
                        LogisticsSubDashboard(viewModel = viewModel, tts = tts, alertBannerText = alertBannerText) { alertBannerText = it }
                    }

                    AppSubTab.GIS -> {
                        GisSubDashboard(viewModel = viewModel, tts = tts, alertBannerText = alertBannerText) { alertBannerText = it }
                    }

                    AppSubTab.CALENDAR -> {
                        CalendarSubDashboard(viewModel = viewModel, alertBannerText = alertBannerText) { alertBannerText = it }
                    }

                    AppSubTab.FAULTS -> {
                        var faultSubSection by remember { mutableStateOf("TAURUS") }
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { faultSubSection = "TAURUS" },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (faultSubSection == "TAURUS") CustomLavText else CyberGray
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("TAURUS RECTIFIER DECODER", fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                            }

                            Button(
                                onClick = { faultSubSection = "DEFECT_LOGGER" },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (faultSubSection == "DEFECT_LOGGER") CustomLavText else CyberGray
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("DEFECT LOGGER & LEDGER", fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                            }
                        }

                        if (faultSubSection == "TAURUS") {
                            TaurusLogParserDashboard(viewModel = viewModel)
                        } else {
                            DefectCaptureLogForm(viewModel = viewModel, onSubmitted = {
                                alertBannerText = "✓ FAULT LEDGER RECORDED CONSOLIDATED"
                            })
                            Spacer(modifier = Modifier.height(20.dp))
                            DefectsHistorySegmentList(defects = defects, onPurge = {
                                viewModel.purgeDefect(it)
                                alertBannerText = "Removed defect cache record"
                            })
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(80.dp)) // padding for floating buttons & bottom bar
            }

            // CRITICAL SOS HUD OVERLAY - Full screen modal trigger
            if (showSosNotification) {
                SosEmergencyScreen(
                    onClose = { showSosNotification = false },
                    onConfirmSos = { hazardType ->
                        selectedSosType = hazardType
                        showSosNotification = false
                        Toast.makeText(context, "🚨 SOLID BROADCASTED TO APTRANSCO GRID CONTROL", Toast.LENGTH_LONG).show()
                        alertBannerText = "🚀 CONTROL SOS DISPATCHED: TELEMETRY EN-ROUTE"
                    }
                )
            }

            // BUILD SUITE GUIDE OVERLAY
            if (showBuildSuite) {
                BuildSuiteGuideModal(onClose = { showBuildSuite = false })
            }
        }
    }
}

@Composable
fun HeaderBar(isOffline: Boolean, onOfflineToggle: () -> Unit, onTriggerSos: () -> Unit) {
    Surface(
        color = Color.White,
        border = BorderStroke(width = 1.dp, color = BorderColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(CustomLavBackground)
                        .border(1.dp, CustomLavBackground, RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Logo",
                        tint = CustomLavText,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "GRID GUARD",
                            color = CustomLavText,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(if (isOffline) SafetyRed.copy(alpha = 0.15f) else FieldGreen.copy(alpha = 0.15f))
                                .border(1.dp, if (isOffline) SafetyRed else FieldGreen, RoundedCornerShape(4.dp))
                                .padding(horizontal = 5.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = if (isOffline) "OFFLINE" else "ONLINE",
                                color = if (isOffline) SafetyRed else FieldGreen,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                    Text(
                        text = "APTRANSCO CBD CORE",
                        color = CustomLavText.copy(alpha = 0.8f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(top = 1.dp)
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Connection Toggler
                IconButton(
                    onClick = onOfflineToggle,
                    modifier = Modifier
                        .size(38.dp)
                        .testTag("offline_toggle_btn")
                ) {
                    Icon(
                        imageVector = if (isOffline) Icons.Default.WifiOff else Icons.Default.Wifi,
                        contentDescription = "OfflineToggle",
                        tint = if (isOffline) SafetyRed else FieldGreen,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                // Alarm button
                IconButton(
                    onClick = onTriggerSos,
                    modifier = Modifier
                        .size(38.dp)
                        .background(SafetyRed.copy(alpha = 0.12f), CircleShape)
                        .border(1.dp, SafetyRed.copy(alpha = 0.3f), CircleShape)
                        .testTag("sos_hotline_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "AlertSos",
                        tint = SafetyRed,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun OfflineWarningBar() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SafetyOrange.copy(alpha = 0.15f)),
        border = BorderStroke(1.dp, SafetyOrange.copy(alpha = 0.35f))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .background(SafetyOrange.copy(alpha = 0.2f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "OfflineInfo",
                    tint = SafetyOrange,
                    modifier = Modifier.size(14.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "LOCAL OFFLINE CACHE ENFORCED",
                    color = SafetyOrange,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "Reports cached on rugged flash. Sync triggers when GSM returns.",
                    color = CustomLavText.copy(alpha = 0.7f),
                    fontSize = 10.sp,
                    modifier = Modifier.padding(top = 1.dp)
                )
            }
        }
    }
}

@Composable
fun SupervisorBanner(tasks: List<TaskEntity>, isSafetyUnlocked: Boolean) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CyberCard),
        border = BorderStroke(1.dp, BorderColor),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(CustomLavBackground)
                            .border(1.dp, CustomLavBackground, RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "CBD1",
                            color = CustomLavText,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "GANG UNIT 1 DISPATCHED",
                            color = CustomLavText.copy(alpha = 0.6f),
                            fontSize = 8.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Srinivas Rao K.",
                            color = CustomLavText,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "SHIELD ID",
                        color = CustomLavText.copy(alpha = 0.6f),
                        fontSize = 8.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "#CBD-9844-AP",
                        color = CustomLavText,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(color = BorderColor.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(12.dp))

            // Quick performance metrics
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val assignedJobs = tasks.size
                val completedJobs = tasks.count { it.status == "COMPLETED" }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(CyberGray, RoundedCornerShape(14.dp))
                        .border(1.dp, BorderColor, RoundedCornerShape(14.dp))
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("ASSIGNED", color = CustomLavText.copy(alpha = 0.6f), fontSize = 8.sp, fontFamily = FontFamily.Monospace)
                        Text("$assignedJobs Jobs", color = CustomLavText, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 1.dp))
                    }
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(CyberGray, RoundedCornerShape(14.dp))
                        .border(1.dp, BorderColor, RoundedCornerShape(14.dp))
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("COMPLETED", color = CustomLavText.copy(alpha = 0.6f), fontSize = 8.sp, fontFamily = FontFamily.Monospace)
                        Text("$completedJobs Jobs", color = FieldGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 1.dp))
                    }
                }
                Box(
                    modifier = Modifier
                        .weight(1.2f)
                        .background(CyberGray, RoundedCornerShape(14.dp))
                        .border(1.dp, BorderColor, RoundedCornerShape(14.dp))
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("SAFETY CODES", color = CustomLavText.copy(alpha = 0.6f), fontSize = 8.sp, fontFamily = FontFamily.Monospace)
                        Text(
                            text = if (isSafetyUnlocked) "VERIFIED OK" else "LOCKED",
                            color = if (isSafetyUnlocked) FieldGreen else SafetyRed,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(top = 1.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SafetyLocknagBox(onNavigateToUnlock: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SafetyRed.copy(alpha = 0.08f)),
        border = BorderStroke(1.dp, SafetyRed.copy(alpha = 0.25f)),
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = "Safety Block",
                tint = SafetyRed,
                modifier = Modifier
                    .size(28.dp)
                    .padding(top = 2.dp)
            )
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Text(
                    text = "MANDATORY PRE-WORK GATE BLOCKED",
                    color = SafetyRed,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "APTRANSCO Code-7 mandates physical checks before work in 220kV segments. Toggle all safeties below to unlock your active daily work list.",
                    color = CustomLavText.copy(alpha = 0.8f),
                    fontSize = 11.sp,
                    lineHeight = 15.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

@Composable
fun SafetyGateChecksInteractive(
    safetyChecks: List<Boolean>,
    onCheckToggle: (Int) -> Unit,
    onApprove: () -> Unit
) {
    val checkLabels = listOf(
        "SOP-1: Confirm Permit-To-Work (PTW) match",
        "SOP-2: Verify conductor is dead via wand detector",
        "SOP-3: Secure and lock solid earth discharge rods",
        "SOP-4: Deploy double-safety fall harness & hardhat",
        "SOP-5: Station ground watchdog supervisor"
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CyberCard),
        border = BorderStroke(1.dp, BorderColor),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "SAFETY CODES TASK LOCK GATE",
                color = CustomLavText,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
            Spacer(modifier = Modifier.height(10.dp))

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                checkLabels.forEachIndexed { index, label ->
                    val isChecked = safetyChecks.getOrElse(index) { false }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isChecked) CustomLavBackground.copy(alpha = 0.25f) else CyberGray)
                            .border(
                                1.dp,
                                if (isChecked) CustomLavBackground else BorderColor,
                                RoundedCornerShape(12.dp)
                            )
                            .clickable { onCheckToggle(index) }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isChecked) SafetyOrange else Color.White)
                                .border(1.dp, BorderColor, RoundedCornerShape(6.dp))
                                .testTag("safety_check_$index"),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isChecked) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Checked",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = label,
                            color = if (isChecked) CustomLavText else Color.DarkGray,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            val allChecked = safetyChecks.all { it }
            Button(
                onClick = onApprove,
                enabled = allChecked,
                colors = ButtonDefaults.buttonColors(
                    containerColor = FieldGreen,
                    disabledContainerColor = CyberGray
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("safety_approve_btn")
            ) {
                Text(
                    text = if (allChecked) "UNLOCK DE-BLOCK WORK" else "COMPLETE SAFETY SOP CHECKS",
                    color = if (allChecked) Color.White else Color.Gray,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

object Colors {
    val Transparent = Color(0x00000000)
}

@Composable
fun JobDutyTitleRow(onResetSafety: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.List,
                contentDescription = "listIcon",
                tint = CustomLavText,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "DAILY GANG DUTY LIST",
                color = CustomLavText,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
        }
        Text(
            text = "RESET LOCK 🔒",
            color = SafetyRed,
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .clickable { onResetSafety() }
                .padding(vertical = 4.dp, horizontal = 8.dp)
        )
    }
}

@Composable
fun EmptyStateCard(message: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        colors = CardDefaults.cardColors(containerColor = CyberGray),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, BorderColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = "Done",
                tint = CustomLavText.copy(alpha = 0.4f),
                modifier = Modifier.size(36.dp)
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = message,
                color = CustomLavText.copy(alpha = 0.6f),
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

@Composable
fun TaskCard(
    task: TaskEntity,
    isSafetyCleared: Boolean,
    onStart: () -> Unit,
    onComplete: () -> Unit
) {
    val isAwaiting = task.status == "AWAITING_PTW"
    val isWork = task.status == "IN_PROGRESS"
    val isDone = task.status == "COMPLETED"

    val cardBorder = when {
        isAwaiting -> BorderStroke(1.dp, SafetyOrange.copy(alpha = 0.5f))
        isWork -> BorderStroke(1.dp, CyberBlue.copy(alpha = 0.5f))
        else -> BorderStroke(1.dp, BorderColor)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isDone) CustomLavBackground.copy(alpha = 0.15f) else CyberCard
        ),
        shape = RoundedCornerShape(24.dp),
        border = cardBorder
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .background(CyberGray, RoundedCornerShape(8.dp))
                        .border(1.dp, BorderColor, RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = task.id,
                        color = CustomLavText,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            when {
                                isAwaiting -> SafetyOrange.copy(alpha = 0.15f)
                                isWork -> CyberBlue.copy(alpha = 0.15f)
                                else -> FieldGreen.copy(alpha = 0.15f)
                            }
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = when {
                            isAwaiting -> "AWAITING SAFETY CLEARANCE"
                            isWork -> "ACTIVE ON TOWER"
                            else -> "COMPLETED SAFE"
                        },
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        color = when {
                            isAwaiting -> SafetyOrange
                            isWork -> CyberBlue
                            else -> FieldGreen
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "${task.voltage} • ${task.lineName}",
                color = CustomLavText.copy(alpha = 0.5f),
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = task.title,
                color = CustomLavText,
                fontSize = 13.sp,
                fontWeight = FontWeight.Black,
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(CyberGray, RoundedCornerShape(12.dp))
                        .border(1.dp, BorderColor, RoundedCornerShape(12.dp))
                        .padding(8.dp)
                ) {
                    Column {
                        Text("TOWERS", color = CustomLavText.copy(alpha = 0.6f), fontSize = 8.sp, fontFamily = FontFamily.Monospace)
                        Text("${task.locationFrom} to ${task.locationTo}", color = CustomLavText, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(CyberGray, RoundedCornerShape(12.dp))
                        .border(1.dp, BorderColor, RoundedCornerShape(12.dp))
                        .padding(8.dp)
                ) {
                    Column {
                        Text("DISPATCHER", color = CustomLavText.copy(alpha = 0.6f), fontSize = 8.sp, fontFamily = FontFamily.Monospace)
                        Text(task.reportTime, color = CustomLavText, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            if (task.numMembersOtherWorks > 0) {
                Spacer(modifier = Modifier.height(10.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(CyberGray, RoundedCornerShape(12.dp))
                        .border(1.dp, BorderColor, RoundedCornerShape(12.dp))
                        .padding(8.dp)
                ) {
                    Text(
                        text = "👥 Crew splits: ${task.numMembersOtherWorks} auxiliary members engaged in: ${task.otherWorksDetails}",
                        color = CustomLavText.copy(alpha = 0.7f),
                        fontSize = 10.sp,
                        lineHeight = 12.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            if (isAwaiting) {
                Button(
                    onClick = onStart,
                    enabled = isSafetyCleared,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SafetyOrange),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("task_start_${task.id}")
                ) {
                    Icon(imageVector = Icons.Default.PlayArrow, contentDescription = "Start", tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("START Patrolling Clearance", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                }
            } else if (isWork) {
                Button(
                    onClick = onComplete,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = FieldGreen),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("task_complete_${task.id}")
                ) {
                    Icon(imageVector = Icons.Default.Check, contentDescription = "Done", tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("CLOSE DISPATCH clearance", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, BorderColor.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                        .padding(10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Check, contentDescription = "DoneIcon", tint = FieldGreen, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "GRID CLEARANCE SECURE",
                            color = FieldGreen,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DefectCaptureLogForm(viewModel: GridViewModel, onSubmitted: () -> Unit) {
    val cameraImage by viewModel.cameraImage.collectAsState()
    val cameraStatus by viewModel.cameraStatusText.collectAsState()
    val isScanning by viewModel.isCameraScanning.collectAsState()

    var selectedCategory by remember { mutableStateOf("") }
    var notesText by remember { mutableStateOf("") }

    val categories = listOf(
        "Insulator Flashover" to "⚡ INSULATOR FLASHOVER",
        "Jungle Clearance Req" to "🌳 JUNGLE GROWN ENCL",
        "Conductor Broken Strand" to "⛓️ WIRE SNAPPED",
        "Earthing Broken Str" to "🪨 PIT EARTH DETACHED"
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CyberCard),
        border = BorderStroke(1.dp, BorderColor),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "EHT TRANSMISSION FIELD DEFECT CAPTURE",
                color = CustomLavText,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
            Spacer(modifier = Modifier.height(12.dp))

            // CAMERA DISPLAY MOCKUP
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.Black)
                    .border(1.dp, BorderColor, RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (cameraImage == null) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(20.dp)
                    ) {
                        Icon(
                            imageVector = if (isScanning) Icons.Default.Refresh else Icons.Default.PlayArrow,
                            contentDescription = "Optics",
                            tint = Color.Gray,
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = cameraStatus,
                            color = Color.LightGray,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                } else {
                    AsyncImage(
                        model = cameraImage,
                        contentDescription = "DefectPreview",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )

                    // Watermark GPS coords
                    val optDate = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date()) }
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(10.dp)
                            .background(Color.Black.copy(alpha = 0.8f))
                            .border(1.dp, BorderColor, RoundedCornerShape(8.dp))
                            .padding(6.dp)
                    ) {
                        Column {
                            Text("📍 GPS SECURED", color = SafetyYellow, fontSize = 7.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                            Text("LATITUDE: 16.5301° N", color = Color.White, fontSize = 7.sp, fontFamily = FontFamily.Monospace)
                            Text("LONGITUDE: 80.6111° E", color = Color.White, fontSize = 7.sp, fontFamily = FontFamily.Monospace)
                            Text("TIME: $optDate UTC", color = Color.Gray, fontSize = 6.sp, fontFamily = FontFamily.Monospace)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Button(
                onClick = { viewModel.triggerCameraShutter() },
                colors = ButtonDefaults.buttonColors(containerColor = CustomLavBackground),
                border = BorderStroke(1.dp, CustomLavBackground),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "camIcon", tint = CustomLavText, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("TRIGGER GLARE-REFLECT SATELLITE OPTICS", color = CustomLavText, fontSize = 10.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "SELECT FAILURE TYPE *",
                color = CustomLavText.copy(alpha = 0.6f),
                fontSize = 8.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))

            // Asymmetric buttons layout
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    categories.take(2).forEach { (id, label) ->
                        val isSel = selectedCategory == id
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSel) CustomLavBackground.copy(alpha = 0.35f) else Color.White)
                                .border(1.dp, if (isSel) CustomLavBackground else BorderColor, RoundedCornerShape(10.dp))
                                .clickable { selectedCategory = id }
                                .padding(10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                color = if (isSel) CustomLavText else Color.DarkGray,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    categories.drop(2).forEach { (id, label) ->
                        val isSel = selectedCategory == id
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSel) CustomLavBackground.copy(alpha = 0.35f) else Color.White)
                                .border(1.dp, if (isSel) CustomLavBackground else BorderColor, RoundedCornerShape(10.dp))
                                .clickable { selectedCategory = id }
                                .padding(10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                color = if (isSel) CustomLavText else Color.DarkGray,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "LOCATION SEVERITY REMARKS *",
                color = CustomLavText.copy(alpha = 0.6f),
                fontSize = 8.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))

            OutlinedTextField(
                value = notesText,
                onValueChange = { notesText = it },
                placeholder = { Text("Describe physical tower, e.g. broken shell on C-phase. Tower VJA-40.", color = Color.Gray, fontSize = 11.sp) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = CustomLavText,
                    unfocusedTextColor = CustomLavText,
                    focusedBorderColor = CustomLavText,
                    unfocusedBorderColor = BorderColor,
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White
                ),
                textStyle = TextStyle(fontSize = 11.sp, fontFamily = FontFamily.Monospace),
                modifier = Modifier.fillMaxWidth(),
                maxLines = 3
            )

            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = {
                    if (selectedCategory.isBlank() || notesText.isBlank()) return@Button
                    viewModel.recordDefect(selectedCategory, notesText)
                    selectedCategory = ""
                    notesText = ""
                    onSubmitted()
                },
                enabled = selectedCategory.isNotBlank() && notesText.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = SafetyOrange),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("TRANSMIT DEFECT LEDGER", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
            }
        }
    }
}

@Composable
fun DefectsHistorySegmentList(defects: List<DefectEntity>, onPurge: (Int) -> Unit) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(imageVector = Icons.Default.Favorite, contentDescription = "ledger", tint = CustomLavText, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "SUBMITTED INSULATOR/LINE DEFECTS",
                color = CustomLavText,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.height(8.dp))

        if (defects.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CyberGray, RoundedCornerShape(16.dp))
                    .border(1.dp, BorderColor, RoundedCornerShape(16.dp))
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No recent records tracked today. Clearances secure.",
                    color = CustomLavText.copy(alpha = 0.6f),
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                defects.forEach { def ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = CyberCard),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, BorderColor)
                    ) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            AsyncImage(
                                model = def.imageUrl,
                                contentDescription = "Defect mini",
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .border(1.dp, BorderColor, RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.Crop
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = def.category.uppercase(),
                                        color = CustomLavText,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    Text(
                                        text = "${def.timestamp} • ${def.status}",
                                        color = CustomLavText.copy(alpha = 0.5f),
                                        fontSize = 8.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                                Text(
                                    text = "${def.id}: \"${def.notes}\"",
                                    color = CustomLavText,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                                Text(
                                    text = "COORD: ${def.latitude}°N, ${def.longitude}°E",
                                    color = CustomLavText.copy(alpha = 0.5f),
                                    fontSize = 8.sp,
                                    fontFamily = FontFamily.Monospace,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                            IconButton(onClick = { onPurge(def.localId) }) {
                                Icon(imageVector = Icons.Default.Delete, contentDescription = "delete", tint = Color.DarkGray, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TaurusLogParserDashboard(viewModel: GridViewModel) {
    val taurusInput by viewModel.taurusInput.collectAsState()
    val parsedResult by viewModel.parsedResult.collectAsState()
    val geminiAnalysis by viewModel.geminiAnalysis.collectAsState()
    val isAnalyzing by viewModel.isAnalyzing.collectAsState()

    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CyberCard),
        border = BorderStroke(1.dp, BorderColor),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "TAURUS MULTIPLEX WHATSAPP TRIP LOADER",
                color = CustomLavText,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
            Spacer(modifier = Modifier.height(10.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CustomLavBackground.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
                    .border(1.dp, CustomLavBackground, RoundedCornerShape(12.dp))
                    .padding(10.dp)
            ) {
                Text(
                    text = "Substations broadcast raw tripping logs to Whatsapp groups. Paste that messy text below to decode, organize to charts, and summon Gemini AI for precise gear recommendations.",
                    color = CustomLavText.copy(alpha = 0.8f),
                    fontSize = 11.sp,
                    lineHeight = 14.sp
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = taurusInput,
                onValueChange = { viewModel.updateTaurusInput(it) },
                placeholder = { Text("Paste raw text containing tripping meters...", color = Color.Gray, fontSize = 11.sp) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = CustomLavText,
                    unfocusedTextColor = CustomLavText,
                    focusedBorderColor = CustomLavText,
                    unfocusedBorderColor = BorderColor,
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White
                ),
                textStyle = TextStyle(fontSize = 11.sp, fontFamily = FontFamily.Monospace),
                modifier = Modifier.fillMaxWidth(),
                maxLines = 6
            )

            Spacer(modifier = Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { viewModel.parseTaurusText() },
                    colors = ButtonDefaults.buttonColors(containerColor = CustomLavText),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1.2f)
                ) {
                    Text("DECODE TRIP event", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                }

                Button(
                    onClick = { viewModel.loadSampleTaurusText() },
                    colors = ButtonDefaults.buttonColors(containerColor = CyberGray),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("LOAD SAMPLE", color = CustomLavText, fontWeight = FontWeight.Bold, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                }
            }

            // Results board
            parsedResult?.let { result ->
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = BorderColor.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "DECODED AUDIT SPECIFICATIONS",
                        color = CustomLavText,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )

                    IconButton(
                        onClick = {
                            val payload = """
                                ⚡ APTRANSCO STANDARDIZED TRIPPING LOG SUMMARY
                                ---------------------------------------------
                                LINE SEGMENT: ${result.lineName}
                                CKT BREAKER ID: ${result.circuit}
                                TAURUS FAULT LOCATOR DISTANCE: ${result.distance}
                                PHASE FEED FAULT CURRENTS:
                                 - R-PHASE: ${result.rCurrent}
                                 - Y-PHASE: ${result.yCurrent}
                                 - B-PHASE: ${result.bCurrent}
                                
                                ADVICE: ${geminiAnalysis ?: "None obtained"}
                            """.trimIndent()
                            clipboardManager.setText(AnnotatedString(payload))
                            Toast.makeText(context, "Copied summary to clipboard", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = "share", tint = CustomLavText, modifier = Modifier.size(16.dp))
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(CyberGray)
                        .border(1.dp, BorderColor, RoundedCornerShape(12.dp))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ResultFieldRow("LINE SEGMENT", result.lineName.uppercase())
                    ResultFieldRow("BREAKER CIRCUIT", result.circuit.uppercase())
                    ResultFieldRow("TAURUS FAULT RANGE", result.distance, isHighlight = true)
                    
                    ResultFieldRow("R-PHASE CURRENT", result.rCurrent, isDanger = result.rCurrent.contains("1"))
                    ResultFieldRow("Y-PHASE CURRENT", result.yCurrent, isDanger = result.yCurrent.contains("1"))
                    ResultFieldRow("B-PHASE CURRENT", result.bCurrent, isDanger = result.bCurrent.contains("1"))
                }

                // Call Gemini Advisor Box
                Spacer(modifier = Modifier.height(14.dp))
                Button(
                    onClick = { viewModel.triggerGeminiSmartAnalysis() },
                    colors = ButtonDefaults.buttonColors(containerColor = SafetyOrange),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isAnalyzing) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("CONSULTING INTELLIGENT TRANSPONDER...", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                    } else {
                        Icon(imageVector = Icons.Default.Info, contentDescription = "gemini", tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("GET AI REPAIR & CLEARANCE INSTRUCTIONS", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                    }
                }

                geminiAnalysis?.let { analysis ->
                    Spacer(modifier = Modifier.height(12.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(CustomLavBackground.copy(alpha = 0.25f))
                            .border(1.dp, CustomLavBackground, RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = Icons.Default.Star, contentDescription = "g", tint = CustomLavText, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "GEMINI-3.5-FLASH INTEL ADVISORY",
                                    color = CustomLavText,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = analysis,
                                color = CustomLavText,
                                fontSize = 11.sp,
                                lineHeight = 15.sp,
                                fontFamily = FontFamily.SansSerif,
                                modifier = Modifier.padding(start = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ResultFieldRow(field: String, value: String, isHighlight: Boolean = false, isDanger: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = field, color = CustomLavText.copy(alpha = 0.6f), fontSize = 10.sp, fontFamily = FontFamily.Monospace)
        Text(
            text = value,
            color = when {
                isHighlight -> SafetyOrange
                isDanger -> SafetyRed
                else -> CustomLavText
            },
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
fun BottomDeck(activeTab: AppSubTab, onTabSelect: (AppSubTab) -> Unit, onTriggerSafety: () -> Unit) {
    Surface(
        color = Color.White,
        border = BorderStroke(1.dp, BorderColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(vertical = 10.dp, horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            // Tab 1: Home/Jobs
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clickable { onTabSelect(AppSubTab.JOBS) }
                    .padding(vertical = 4.dp, horizontal = 2.dp)
                    .width(70.dp)
                    .testTag("tab_patrol")
            ) {
                Icon(
                    imageVector = Icons.Default.Home,
                    contentDescription = "Jobs",
                    tint = if (activeTab == AppSubTab.JOBS) CustomLavText else Color.Gray,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "DASHBOARD",
                    color = if (activeTab == AppSubTab.JOBS) CustomLavText else Color.Gray,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(top = 2.dp),
                    maxLines = 1
                )
            }

            // Tab 2: Logistics
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clickable { onTabSelect(AppSubTab.LOGISTICS) }
                    .padding(vertical = 4.dp, horizontal = 2.dp)
                    .width(70.dp)
                    .testTag("tab_logistics")
            ) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = "Logistics",
                    tint = if (activeTab == AppSubTab.LOGISTICS) CustomLavText else Color.Gray,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "LOGISTICS",
                    color = if (activeTab == AppSubTab.LOGISTICS) CustomLavText else Color.Gray,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(top = 2.dp),
                    maxLines = 1
                )
            }

            // Tab 3: GIS Tower Map
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clickable { onTabSelect(AppSubTab.GIS) }
                    .padding(vertical = 4.dp, horizontal = 2.dp)
                    .width(70.dp)
                    .testTag("tab_gis")
            ) {
                Icon(
                    imageVector = Icons.Default.Place,
                    contentDescription = "GIS Map",
                    tint = if (activeTab == AppSubTab.GIS) CustomLavText else Color.Gray,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "GIS TOWER",
                    color = if (activeTab == AppSubTab.GIS) CustomLavText else Color.Gray,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(top = 2.dp),
                    maxLines = 1
                )
            }

            // Tab 4: Calendar
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clickable { onTabSelect(AppSubTab.CALENDAR) }
                    .padding(vertical = 4.dp, horizontal = 2.dp)
                    .width(70.dp)
                    .testTag("tab_calendar")
            ) {
                Icon(
                    imageVector = Icons.Default.DateRange,
                    contentDescription = "Calendar",
                    tint = if (activeTab == AppSubTab.CALENDAR) CustomLavText else Color.Gray,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "CALENDAR",
                    color = if (activeTab == AppSubTab.CALENDAR) CustomLavText else Color.Gray,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(top = 2.dp),
                    maxLines = 1
                )
            }

            // Tab 5: Faults & Taurus
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clickable { onTabSelect(AppSubTab.FAULTS) }
                    .padding(vertical = 4.dp, horizontal = 2.dp)
                    .width(70.dp)
                    .testTag("tab_faults")
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "Faults",
                    tint = if (activeTab == AppSubTab.FAULTS) CustomLavText else Color.Gray,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "FAULTS/TAUR",
                    color = if (activeTab == AppSubTab.FAULTS) CustomLavText else Color.Gray,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(top = 2.dp),
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
fun SosEmergencyScreen(onClose: () -> Unit, onConfirmSos: (String) -> Unit) {
    val pulseTransition = rememberInfiniteTransition()
    val animatedBgAlpha by pulseTransition.animateFloat(
        initialValue = 0.15f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        )
    )

    Surface(
        color = Color.Black.copy(alpha = 0.95f),
        modifier = Modifier
            .fillMaxSize()
            .drawBehind {
                drawRect(
                    color = SafetyRed.copy(alpha = animatedBgAlpha * 0.12f),
                    size = size
                )
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .statusBarsPadding()
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(90.dp)
                    .clip(CircleShape)
                    .background(Color.White)
                    .border(4.dp, SafetyRed, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "SOS Alert",
                    tint = SafetyRed,
                    modifier = Modifier.size(48.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color.Black)
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "CRITICAL SOS EMERGENCY MODE",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp
                )
            }

            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "GRID HOT CALL INITIATED",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "GPS Coordinate watermark lat 16.5301° N, lng 80.6111° E has been packed into transponder buffer. Choose a hazard to notify grid dispatcher.",
                color = Color.LightGray,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                fontFamily = FontFamily.SansSerif,
                modifier = Modifier.padding(horizontal = 16.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))
            Column(
                modifier = Modifier.width(320.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = { onConfirmSos("SNAPPED_CONDUCTOR") },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                ) {
                    Icon(imageVector = Icons.Default.Warning, contentDescription = "zap", tint = SafetyRed)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("TRANSMISSION CONDUCTOR SNAPPED", fontWeight = FontWeight.Bold, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                }

                Button(
                    onClick = { onConfirmSos("ARCING_SPARK") },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Black, contentColor = Color.White),
                    border = BorderStroke(1.dp, BorderColor),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                ) {
                    Icon(imageVector = Icons.Default.Info, contentDescription = "arcing", tint = SafetyYellow)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("SEVERE CONDUCTOR DISCHARGE APPARATUS", fontWeight = FontWeight.Bold, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                }

                Button(
                    onClick = { onConfirmSos("MEDICAL_RESCUE") },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Black, contentColor = FieldGreen),
                    border = BorderStroke(1.dp, BorderColor),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                ) {
                    Icon(imageVector = Icons.Default.List, contentDescription = "med", tint = FieldGreen)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("CREW MEDICAL RESCUE REQUEST", fontWeight = FontWeight.Bold, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
            Text(
                text = "✕ STAND-DOWN EXTRA EMERGENCY",
                color = Color.LightGray,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .clickable { onClose() }
                    .padding(10.dp)
            )
        }
    }
}

@Composable
fun CalendarSubDashboard(
    viewModel: GridViewModel,
    alertBannerText: String?,
    onAlertChanged: (String) -> Unit
) {
    val context = LocalContext.current
    var selectedMonth by remember { mutableStateOf("MAY") }
    var selectedDay by remember { mutableStateOf(26) }
    
    var newEventTitle by remember { mutableStateOf("") }
    val calendarEvents = remember {
        mutableStateMapOf<String, List<String>>(
            "MAY-26" to listOf("Route Patrolling L01 Segment", "PMI Earth Rod Check NLR"),
            "MAY-01" to listOf("May Day Maintenance Standby"),
            "JUNE-18" to listOf("Bakrid Grid High Tension Fire Prevention Duty")
        )
    }

    val holidays = mapOf(
        "MAY-01" to "May Day (State Gazetted Holiday)",
        "MAY-07" to "Buddha Purnima (Gazetted Holiday)",
        "MAY-18" to "AP Power Safety Awareness Day",
        "JUNE-18" to "Bakrid / Eid al-Adha"
    )

    val birthdayWorkers = listOf(
        "Y. Ramanappa (Driver)" to "May 26",
        "Srinivas Rao (Supervisor)" to "May 30",
        "Suresh Rao (Lineman)" to "June 12"
    )

    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = CyberGray),
        border = BorderStroke(1.dp, BorderColor)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "🗓️ SUBSTATION ROSTER & HOLIDAYS",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
                
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("MAY", "JUNE").forEach { month ->
                        Button(
                            onClick = { 
                                selectedMonth = month
                                selectedDay = if (month == "MAY") 26 else 1
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (selectedMonth == month) CustomLavText else Color.DarkGray
                            ),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(month, fontSize = 9.sp, fontFamily = FontFamily.Monospace, color = Color.White)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            val daysInMonth = if (selectedMonth == "MAY") 31 else 30
            val columns = 7
            val dayNames = listOf("S", "M", "T", "W", "T", "F", "S")

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                dayNames.forEach { name ->
                    Text(
                        text = name,
                        color = Color.Gray,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.width(36.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            val firstDayOffset = if (selectedMonth == "MAY") 5 else 1
            val totalCells = daysInMonth + firstDayOffset
            val rows = (totalCells + 6) / 7

            for (row in 0 until rows) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    for (col in 0 until columns) {
                        val cellIndex = row * 7 + col
                        val dayNumber = cellIndex - firstDayOffset + 1
                        
                        if (dayNumber in 1..daysInMonth) {
                            val key = "${selectedMonth}-$dayNumber"
                            val isHoliday = holidays.containsKey(key)
                            val isSelected = selectedDay == dayNumber
                            val isToday = (selectedMonth == "MAY" && dayNumber == 26)
                            
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .padding(2.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(
                                        when {
                                            isSelected -> CustomLavText
                                            isToday -> SafetyOrange.copy(alpha = 0.35f)
                                            isHoliday -> SafetyYellow.copy(alpha = 0.2f)
                                            else -> Color.Transparent
                                        }
                                    )
                                    .border(
                                        width = 1.dp,
                                        color = when {
                                            isToday -> SafetyOrange
                                            isHoliday -> SafetyYellow
                                            else -> Color.Transparent
                                        },
                                        shape = RoundedCornerShape(6.dp)
                                    )
                                    .clickable { selectedDay = dayNumber },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "$dayNumber",
                                    color = if (isSelected) Color.Black else Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        } else {
                            Spacer(modifier = Modifier.size(36.dp))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            val currentKey = "${selectedMonth}-$selectedDay"
            val holidayName = holidays[currentKey]
            val activeEvents = calendarEvents[currentKey] ?: emptyList()

            holidayName?.let { name ->
                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = SafetyYellow.copy(alpha = 0.15f))
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Default.Info, contentDescription = "Hold", tint = SafetyYellow)
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("⚠ AP STATE HOLIDAY CLASH WARNING", color = SafetyYellow, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            Text(name, color = Color.White, fontSize = 11.sp)
                        }
                    }
                }
            }

            val activeBdays = birthdayWorkers.filter { it.second.equals("${selectedMonth.lowercase().capitalize()} $selectedDay", ignoreCase = true) }
            if (activeBdays.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFD4AF37).copy(alpha = 0.15f)),
                    border = BorderStroke(1.dp, Color(0xFFD4AF37))
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("🎂", fontSize = 20.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("HAPPY BIRTHDAY GREETING", color = Color(0xFFD4AF37), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            Text("Best wishes to: ${activeBdays.map { it.first }.joinToString(", ")}!", color = Color.White, fontSize = 11.sp)
                        }
                    }
                }
            }

            Text("ASSIGNED ROUTE DUTIES & EVENTS ($currentKey):", color = Color.LightGray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            if (activeEvents.isEmpty()) {
                Text("No maintenance tasks scheduled on this date.", color = Color.Gray, fontSize = 11.sp, modifier = Modifier.padding(vertical = 4.dp))
            } else {
                activeEvents.forEach { event ->
                    Text("• $event", color = Color.White, fontSize = 11.sp, modifier = Modifier.padding(vertical = 2.dp))
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text("QUICK CALENDAR TASK ASSIGNMENT:", color = Color.LightGray, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = newEventTitle,
                    onValueChange = { newEventTitle = it },
                    placeholder = { Text("Task e.g. Patrolling L03", fontSize = 11.sp) },
                    modifier = Modifier.weight(1.0f).height(50.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = CustomLavText,
                        unfocusedBorderColor = Color.DarkGray
                    ),
                    textStyle = TextStyle(fontSize = 11.sp)
                )

                Button(
                    onClick = {
                        if (newEventTitle.isNotBlank()) {
                            val list = calendarEvents[currentKey] ?: emptyList()
                            calendarEvents[currentKey] = list + newEventTitle
                            onAlertChanged("Assigned: '$newEventTitle' registered on $currentKey!")
                            newEventTitle = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CustomLavText),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("BOOK", fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                }
            }
        }
    }
}

@Composable
fun PatrollingTrackerDashboard(viewModel: GridViewModel) {
    val feeders by viewModel.feeders.collectAsState()
    
    var newFeederName by remember { mutableStateOf("") }
    var newFeederVoltage by remember { mutableStateOf("220 kV") }
    var newFeederCrew by remember { mutableStateOf("") }
    var newFeederTowers by remember { mutableStateOf("30") }
    var showAddForm by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = CyberGray),
        border = BorderStroke(1.dp, BorderColor)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "⚡ AP TRANSMISSION FEEDER STATUS LEDGER",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )

                IconButton(onClick = { showAddForm = !showAddForm }) {
                    Icon(
                        imageVector = if (showAddForm) Icons.Default.Close else Icons.Default.Add,
                        contentDescription = "Toggle add form",
                        tint = CustomLavText
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (showAddForm) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Black),
                    border = BorderStroke(1.dp, BorderColor)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("REGISTER NEW SECTOR FEEDER:", color = CustomLavText, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        OutlinedTextField(
                            value = newFeederName,
                            onValueChange = { newFeederName = it },
                            label = { Text("Feeder Name (e.g. L04 Ongole-KNL)", fontSize = 10.sp) },
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            textStyle = TextStyle(fontSize = 11.sp, color = Color.White)
                        )

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = newFeederCrew,
                                onValueChange = { newFeederCrew = it },
                                label = { Text("Crew Tag", fontSize = 10.sp) },
                                modifier = Modifier.weight(1f),
                                textStyle = TextStyle(fontSize = 11.sp, color = Color.White)
                            )
                            OutlinedTextField(
                                value = newFeederTowers,
                                onValueChange = { newFeederTowers = it },
                                label = { Text("Total Towers", fontSize = 10.sp) },
                                modifier = Modifier.weight(1f),
                                textStyle = TextStyle(fontSize = 11.sp, color = Color.White)
                            )
                        }

                        Button(
                            onClick = {
                                if (newFeederName.isNotBlank()) {
                                    val totalT = newFeederTowers.toIntOrNull() ?: 30
                                    viewModel.registerNewFeeder(newFeederName, newFeederVoltage, newFeederCrew, totalT)
                                    showAddForm = false
                                    newFeederName = ""
                                    newFeederCrew = ""
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = CustomLavText),
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                        ) {
                            Text("SAVE SECTOR", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            feeders.forEachIndexed { index, feeder ->
                val progress = if (feeder.totalTowers > 0) feeder.inspectedTowers.toFloat() / feeder.totalTowers.toFloat() else 0f
                Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Row {
                                Text(feeder.name, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(Color.DarkGray)
                                        .padding(horizontal = 4.dp, vertical = 1.dp)
                                ) {
                                    Text(feeder.voltage, color = SafetyYellow, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            Text("Inspected: ${feeder.inspectedTowers}/${feeder.totalTowers} Towers", color = Color.LightGray, fontSize = 10.sp)
                            Text("Active Crew: ${feeder.crewTag}", color = Color.Gray, fontSize = 10.sp)
                        }

                        Button(
                            onClick = { viewModel.incrementFeederTowers(index) },
                            colors = ButtonDefaults.buttonColors(containerColor = CustomLavText),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text("+5 TOWERS OK", fontSize = 10.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    LinearProgressIndicator(
                        progress = progress,
                        color = FieldGreen,
                        trackColor = Color.DarkGray,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                    )
                }
                HorizontalDivider(color = Color.DarkGray, thickness = 0.5.dp, modifier = Modifier.padding(vertical = 4.dp))
            }
        }
    }
}

@Composable
fun PmiGroundGridLoggerView(
    viewModel: GridViewModel,
    alertBannerText: String?,
    onAlertChanged: (String) -> Unit
) {
    val pmiLogs by viewModel.pmiLogs.collectAsState()
    
    var resistanceInput by remember { mutableStateOf("2.5") }
    var leakageInput by remember { mutableStateOf("15.0") }

    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = CyberGray),
        border = BorderStroke(1.dp, BorderColor)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "🔌 PMI PREVENTIVE GROUND GRID LOGGER",
                color = Color.White,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = resistanceInput,
                    onValueChange = { resistanceInput = it },
                    label = { Text("Earth Resistance (Ω)", fontSize = 10.sp) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier.weight(1f),
                    textStyle = TextStyle(fontSize = 11.sp)
                )

                OutlinedTextField(
                    value = leakageInput,
                    onValueChange = { leakageInput = it },
                    label = { Text("Leakage Current (μA)", fontSize = 10.sp) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier.weight(1f),
                    textStyle = TextStyle(fontSize = 11.sp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = {
                    val res = resistanceInput.toDoubleOrNull() ?: 2.5
                    val leak = leakageInput.toDoubleOrNull() ?: 15.0
                    viewModel.submitPmiLog(res, leak)
                    
                    if (res > 5.0 || leak > 30.0) {
                        onAlertChanged("⚠ PMI EXTREME PARAMETER OUT-OF-LIMITS DETECTED!")
                    } else {
                        onAlertChanged("✓ PMG ground log registered.")
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = CustomLavText)
            ) {
                Text("RECORD LOG & AUDIT", fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
            }

            Spacer(modifier = Modifier.height(16.dp))

            val mostRecentLog = pmiLogs.firstOrNull()
            if (mostRecentLog != null && (mostRecentLog.earthResistance > 5.0 || mostRecentLog.leakageCurrent > 30.0)) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = SafetyRed.copy(alpha = 0.15f)),
                    border = BorderStroke(1.dp, SafetyRed)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Warning, contentDescription = "hazard", tint = SafetyRed)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("LIMIT BREACHED: EXCESS IMPEDANCE/LEAKAGE", color = SafetyRed, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            "SOIL-CHEMICAL REFILL DIRECTIONS:\n" +
                            "1. Immediately add Bentonite/Charcoal/Salt compound mixture around the earth pit electrode reservoir.\n" +
                            "2. Pour 10 liters of pure saline water inside the electrode transponder node to restore sub-ohm ground paths (< 5.0 Ω).",
                            color = Color.White,
                            fontSize = 10.sp,
                            lineHeight = 14.sp,
                            fontFamily = FontFamily.SansSerif
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            } else {
                Card(
                    colors = CardDefaults.cardColors(containerColor = FieldGreen.copy(alpha = 0.12f)),
                    border = BorderStroke(1.dp, FieldGreen.copy(alpha = 0.4f))
                ) {
                    Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.CheckCircle, contentDescription = "ok", tint = FieldGreen)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("All PMG electrode resistances verify within safe sub-5Ω standards.", color = Color.LightGray, fontSize = 10.sp)
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            Text("HISTORICAL PARAMETER CHECKS:", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            pmiLogs.forEach { log ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(log.id, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        Text(log.timestamp, color = Color.Gray, fontSize = 9.sp)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Res: ${log.earthResistance} Ω ${if (log.resistanceBreach) "⚠" else "✓"}", color = if (log.resistanceBreach) SafetyRed else FieldGreen, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Text("Leak: ${log.leakageCurrent} μA ${if (log.leakageBreach) "⚠" else "✓"}", color = if (log.leakageBreach) SafetyRed else FieldGreen, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
                HorizontalDivider(color = Color.DarkGray, thickness = 0.5.dp)
            }
        }
    }
}

@Composable
fun CompletedWorkLedgerView(
    viewModel: GridViewModel,
    alertBannerText: String?,
    onAlertChanged: (String) -> Unit
) {
    val orders by viewModel.completedWorkOrders.collectAsState()
    
    var towerRangeInput by remember { mutableStateOf("") }
    var supervisorInput by remember { mutableStateOf("Srinivas Rao") }
    var declarationInput by remember { mutableStateOf("All shorting ropes disconnected. Insulators visual check clear. Safe to re-energize.") }

    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = CyberGray),
        border = BorderStroke(1.dp, BorderColor)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "✅ ARCHIVED SAFETY PTW CLEARANCE LEDGER",
                color = Color.White,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(10.dp))

            Card(
                colors = CardDefaults.cardColors(containerColor = Color.Black),
                border = BorderStroke(1.dp, BorderColor),
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("LOG COMPLETED PTW HANDBACK SIGN-OFF:", color = CustomLavText, fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    Spacer(modifier = Modifier.height(6.dp))

                    OutlinedTextField(
                        value = towerRangeInput,
                        onValueChange = { towerRangeInput = it },
                        placeholder = { Text("Tower Segment (e.g. Tower VJA-12 to VJA-15)", fontSize = 10.sp) },
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                        textStyle = TextStyle(fontSize = 11.sp)
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedTextField(
                            value = supervisorInput,
                            onValueChange = { supervisorInput = it },
                            placeholder = { Text("Supervisor", fontSize = 10.sp) },
                            modifier = Modifier.weight(1f).height(50.dp),
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                            textStyle = TextStyle(fontSize = 11.sp)
                        )
                        Button(
                            onClick = {
                                if (towerRangeInput.isNotBlank()) {
                                    viewModel.submitCompletedWork(towerRangeInput, supervisorInput, declarationInput)
                                    onAlertChanged("✓ SYSTEM RE-ARMED: $towerRangeInput archived!")
                                    towerRangeInput = ""
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = CustomLavText),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(50.dp)
                        ) {
                            Text("SAVE HANDBACK", fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = declarationInput,
                        onValueChange = { declarationInput = it },
                        placeholder = { Text("Clearance Declaration Note", fontSize = 10.sp) },
                        modifier = Modifier.fillMaxWidth().height(60.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                        textStyle = TextStyle(fontSize = 10.sp)
                    )
                }
            }

            Text("CLOSED OUT ORDERS (PENDING RE-ENERGIZING):", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            orders.forEach { order ->
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.DarkGray.copy(alpha = 0.5f)),
                    border = BorderStroke(0.5.dp, BorderColor)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(order.id, color = FieldGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                            Text(order.handbackTime, color = Color.LightGray, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                        }
                        Text("Segment: ${order.towerRange}", color = Color.White, fontSize = 11.sp)
                        Text("Signed by Supervisor: ${order.supervisorSignature}", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Text("Declaration: ${order.declaration}", color = Color.Gray, fontSize = 10.sp, lineHeight = 13.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun LogisticsSubDashboard(
    viewModel: GridViewModel,
    tts: TextToSpeech?,
    alertBannerText: String?,
    onAlertChanged: (String) -> Unit
) {
    val context = LocalContext.current
    val odoKms by viewModel.odometer.collectAsState()
    val isDispatchActive by viewModel.isDispatchAlertActive.collectAsState()
    val satelliteUnlocked by viewModel.mutualSatelliteUnlocked.collectAsState()
    val crewPositions by viewModel.crewPositions.collectAsState()
    val vanMins by viewModel.vanIdleMinutes.collectAsState()

    var odoInputText by remember { mutableStateOf(odoKms.toString()) }
    var simulatingHour by remember { mutableStateOf("08:30 AM") }

    var showLocalDispatchPanel by remember { mutableStateOf(isDispatchActive) }

    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = CyberGray),
        border = BorderStroke(1.dp, BorderColor)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "🚚 AP-07-TU-1260 GURKHA LINE CRUISER WORKSPACE",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Card(
                colors = CardDefaults.cardColors(containerColor = Color.Black),
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("🌩️ ATMOSPHERIC HAZARD RADAR", color = SafetyYellow, fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        Text("Vijayawada Grid Plains: 38°C", color = Color.White, fontSize = 12.sp)
                        Text("Relative Humidity: 78% | Temp Winds: 24 km/h", color = Color.LightGray, fontSize = 10.sp)
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(SafetyRed.copy(alpha = 0.2f))
                            .border(1.dp, SafetyRed, RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text("HIGH LIGHTNING OUTAGE RISK", color = SafetyRed, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = odoInputText,
                    onValueChange = { 
                        odoInputText = it
                        it.toIntOrNull()?.let { kms -> viewModel.updateOdometer(kms) }
                    },
                    label = { Text("Odometer Reading (KM)", fontSize = 9.sp) },
                    modifier = Modifier.weight(1f),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                    textStyle = TextStyle(fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                )

                Column(modifier = Modifier.weight(1f)) {
                    Text("SIMULATED SHIFT HOURS:", color = Color.Gray, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        listOf("08:30 AM", "11:30 AM", "03:15 PM").forEach { hrs ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(if (simulatingHour == hrs) CustomLavText else Color.DarkGray)
                                    .clickable { simulatingHour = hrs }
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                            ) {
                                Text(hrs, color = if (simulatingHour == hrs) Color.Black else Color.White, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.4f)),
                border = BorderStroke(1.dp, BorderColor),
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("📈 LOGISTICS REGULATORY HOURLY COMPLIANCE HUD", color = CustomLavText, fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Rule A: Tracking Without Carriage (7:30AM - 2:00PM)", color = Color.White, fontSize = 10.sp)
                        val isRuleAActive = simulatingHour == "08:30 AM" || simulatingHour == "11:30 AM"
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(if (isRuleAActive) FieldGreen.copy(alpha = 0.2f) else Color.DarkGray)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(if (isRuleAActive) "ENGAGED ACTIVE" else "STANDBY", color = if (isRuleAActive) FieldGreen else Color.Gray, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Rule B: Carriage Safety Transponder (9:00AM - 5:00PM)", color = Color.White, fontSize = 10.sp)
                        val isRuleBActive = simulatingHour == "11:30 AM" || simulatingHour == "03:15 PM"
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(if (isRuleBActive) FieldGreen.copy(alpha = 0.2f) else Color.DarkGray)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(if (isRuleBActive) "ENGAGED ACTIVE" else "STANDBY", color = if (isRuleBActive) FieldGreen else Color.Gray, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Rule C: Active Siren-to-Chime Support Override Code", color = Color.White, fontSize = 10.sp)
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(FieldGreen.copy(alpha = 0.2f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text("FAIL-SAFE ENGAGED", color = FieldGreen, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            if (showLocalDispatchPanel) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                        .border(
                            width = 2.dp,
                            color = SafetyRed,
                            shape = RoundedCornerShape(12.dp)
                        ),
                    colors = CardDefaults.cardColors(containerColor = SafetyRed.copy(alpha = 0.15f))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Warning, contentDescription = "Emergency", tint = SafetyRed)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("⚠ HIGH PRIORITY EMERGENCY DISPATCH ASSIGNED", color = SafetyRed, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            "AEE DISPATCH REF: AP-9020. 132kV Line Insulator Flashed near NLR Segment. Conductor short circuit danger.",
                            color = Color.White, fontSize = 11.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = {
                                    onAlertChanged("🚚 Route set: AP-07-TU-1260 moving to Nellore Sector.")
                                    showLocalDispatchPanel = false
                                    viewModel.dismissDispatchAlert()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("🚚 SET TRIP ROUTE", color = Color.Black, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                            }
                            Button(
                                onClick = { showLocalDispatchPanel = false },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                                border = BorderStroke(1.dp, Color.Gray),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("DISMISS", color = Color.White, fontSize = 10.sp)
                            }
                        }
                    }
                }
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = Color.DarkGray.copy(alpha = 0.5f)),
                border = BorderStroke(0.5.dp, BorderColor),
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("📡 MUTUAL SATELLITE PICKUP RADAR", color = CustomLavText, fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "Crew working hours tracking system active. Coordinates locked to 16.5301°N, 80.6111°E. Remote telemetry confirms all 4 crew members within safety radius.",
                        color = Color.LightGray, fontSize = 10.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            viewModel.triggerMutilSatelliteLock()
                            val textToSay = "సిబ్బంది పికప్ కొరకు సిద్ధంగా ఉన్నారు. దయచేసి జుపి యస్ మార్గాన్ని అనుసరించండి."
                            tts?.speak(textToSay, TextToSpeech.QUEUE_FLUSH, null, null)
                            onAlertChanged("✓ Mutual GPS track initialized: routing loaded!")
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = if (satelliteUnlocked) FieldGreen else CustomLavText),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (satelliteUnlocked) "✓ MUTUAL ROUTING SYNCED" else "INIT MUTUAL SATELLITE ROUTE (TELUGU ALERT)", fontSize = 10.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = Color.Black),
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("🚨 CREW PROXIMITY RADAR MONITOR", color = SafetyRed, fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    Spacer(modifier = Modifier.height(6.dp))
                    crewPositions.forEach { pos ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("${pos.name} (${pos.role})", color = Color.White, fontSize = 10.sp)
                            Text("${pos.distanceMeters}m ${if (pos.isIdle) "(Van Idle)" else "(Active Field)"}", color = if (pos.distanceMeters > 20) SafetyRed else FieldGreen, fontSize = 10.sp)
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            val warningText = "సురేష్ రావు, రమేష్ బాబు గారు దయచేసి వ్యాన్ దగ్గరకు వచ్చి సహాయం చేయండి."
                            tts?.speak(warningText, TextToSpeech.QUEUE_FLUSH, null, null)
                            onAlertChanged("🔊 RADAR PROXIMITY TELEMETRY ANNOUNCED")
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("ANNOUNCE PHONE LAGGING CREW (TELUGU)", color = Color.White, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                    }
                }
            }

            Text("🔊 VEHICLE TELEMETRY ALARM SOUNDERS:", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        try {
                            val toneGen = ToneGenerator(AudioManager.STREAM_ALARM, 100)
                            toneGen.startTone(ToneGenerator.TONE_CDMA_PIP, 500)
                            onAlertChanged("🔊 Vehicle Start Siren acoustic cue sounded")
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = SafetyOrange),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("VEHICLE START SIREN", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }

                Button(
                    onClick = {
                        try {
                            val toneGen = ToneGenerator(AudioManager.STREAM_ALARM, 100)
                            toneGen.startTone(ToneGenerator.TONE_CDMA_CONFIRM, 500)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                        
                        val welcomeTelugu = "శ్రీనివాస్ రావు గారు, ప్రసాద్ రెడ్డి గారు, మోహన్ నాయుడు గారు మరియు రమణప్ప గారు మీకు మరియు మీ టీమ్ సభ్యులందరికీ ధన్యవాదాలు."
                        tts?.speak(welcomeTelugu, TextToSpeech.QUEUE_FLUSH, null, null)
                        onAlertChanged("🔊 WORK COMPLETED CHIME & GRATITUDE BROADCAST SENT")
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = FieldGreen),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("WORK COMPLETED CHIME", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }
}

@Composable
fun GisSubDashboard(
    viewModel: GridViewModel,
    tts: TextToSpeech?,
    alertBannerText: String?,
    onAlertChanged: (String) -> Unit
) {
    val mapLayers by viewModel.mapLayers.collectAsState()
    val towers by viewModel.thermalTowers.collectAsState()
    var selectedTowerId by remember { mutableStateOf<String?>("Tower VJA-14") }

    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = CyberGray),
        border = BorderStroke(1.dp, BorderColor)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "🛰️ APTRANSCO GIS SATELLITE SEGMENT MAP",
                color = Color.White,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Map Overlays:", color = Color.Gray, fontSize = 10.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    mapLayers.forEach { (layer, isChecked) ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isChecked) CustomLavText.copy(alpha = 0.2f) else Color.DarkGray)
                                .clickable { viewModel.toggleMapLayer(layer) }
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(if (isChecked) FieldGreen else Color.Gray)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(layer, color = if (isChecked) Color.White else Color.Gray, fontSize = 8.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Black)
                    .border(1.dp, BorderColor),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height
                    
                    for (i in 1..4) {
                        val gx = w * i / 5f
                        val gy = h * i / 5f
                        drawLine(color = Color.DarkGray.copy(alpha = 0.5f), start = Offset(gx, 0f), end = Offset(gx, h))
                        drawLine(color = Color.DarkGray.copy(alpha = 0.5f), start = Offset(0f, gy), end = Offset(w, gy))
                    }

                    drawLine(color = Color.Cyan.copy(alpha = 0.6f), start = Offset(40f, 60f), end = Offset(w * 0.4f, h * 0.4f), strokeWidth = 3f)
                    drawLine(color = Color.Cyan.copy(alpha = 0.6f), start = Offset(w * 0.4f, h * 0.4f), end = Offset(w * 0.7f, h * 0.8f), strokeWidth = 3f)
                    drawLine(color = Color.Yellow.copy(alpha = 0.5f), start = Offset(20f, h - 20f), end = Offset(w * 0.8f, h * 0.3f), strokeWidth = 2f)
                }

                val towerPositions = listOf(
                    "Tower VJA-12" to (0.15f to 0.25f),
                    "Tower VJA-14" to (0.42f to 0.38f),
                    "Tower NLR-48" to (0.72f to 0.75f),
                    "Tower GUN-02" to (0.81f to 0.22f)
                )

                BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                    val maxW = maxWidth
                    val maxH = maxHeight
                    
                    towerPositions.forEach { (id, pos) ->
                        val (px, py) = pos
                        val isSelected = selectedTowerId == id
                        val isHotspot = towers.find { it.id == id }?.temperatureCelsius ?: 0.0 > 65.0
                        
                        Box(
                            modifier = Modifier
                                .offset(x = maxW * px - 12.dp, y = maxH * py - 12.dp)
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(
                                    when {
                                        isSelected -> CustomLavText
                                        isHotspot -> SafetyRed
                                        else -> Color.DarkGray
                                    }
                                )
                                .border(
                                    width = 2.dp,
                                    color = if (isHotspot) Color.White else Color.Transparent,
                                    shape = CircleShape
                                )
                                .clickable { selectedTowerId = id }
                                .padding(2.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = id.takeLast(2),
                                color = if (isSelected) Color.Black else Color.White,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Text(
                    text = "GPS LOCK: 16.53° N, 80.61° E • NLR AREA",
                    color = Color.Gray,
                    fontSize = 8.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(6.dp)
                )

                Text(
                    text = "🔥 THERMAL SCAN OVERVIEW: 2 SECTIONS OVERHEAT >65°C",
                    color = SafetyRed,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(6.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            val currentTower = towers.find { it.id == selectedTowerId }
            if (currentTower != null) {
                val isHotspot = currentTower.temperatureCelsius > 65.0
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.Black),
                    border = BorderStroke(1.dp, if (isHotspot) SafetyRed else BorderColor)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("SELECTED SEGMENT: ${currentTower.id}", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(if (isHotspot) SafetyRed.copy(alpha = 0.2f) else FieldGreen.copy(alpha = 0.2f))
                                    .padding(horizontal = 4.dp, vertical = 1.dp)
                            ) {
                                Text(
                                    text = if (isHotspot) "🔥 HOTSPOT: ${currentTower.temperatureCelsius}°C" else "✓ NORMAL: ${currentTower.temperatureCelsius}°C",
                                    color = if (isHotspot) SafetyRed else FieldGreen,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Text("kV Segment Line: ${currentTower.voltageLine}", color = Color.LightGray, fontSize = 10.sp)
                        Text("Current Patrol Status: ${currentTower.status}", color = Color.Gray, fontSize = 10.sp)

                        Spacer(modifier = Modifier.height(10.dp))

                        Text("SUPERVISOR ACTION - CHANGE STATUS:", color = Color.LightGray, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            listOf("Needs Patrol", "Defect Reported", "Under Repair", "Operational").forEach { st ->
                                Button(
                                    onClick = {
                                        viewModel.updateTowerStatus(currentTower.id, st)
                                        onAlertChanged("Status updated: ${currentTower.id} is now '$st'")
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (currentTower.status == st) CustomLavText else CyberGray
                                    ),
                                    shape = RoundedCornerShape(6.dp),
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(st, fontSize = 7.sp, fontFamily = FontFamily.Monospace, maxLines = 1)
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Card(
                colors = CardDefaults.cardColors(containerColor = Color.DarkGray.copy(alpha = 0.5f)),
                border = BorderStroke(0.5.dp, BorderColor),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("📢 SATELLITE REGIONAL VOICE TRANSMITTER", color = CustomLavText, fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Click below to broadcast regional structural alerts using browser Telugu Speech Synthesis:", color = Color.LightGray, fontSize = 9.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                val text = "హెచ్చరిక! టవర్ 14 వద్ద 71 డిగ్రీల అధిక వేడి నమోదైంది. దయచేసి తనిఖీ చేయండి."
                                tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
                                onAlertChanged("🔊 BROADCASTING SUBSTATION THERMAL WARNING IN TELUGU")
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = CustomLavText),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("🔥 BROADCAST HOTSPOT WARNING", fontSize = 8.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                val text = "లైన్ క్లియరెన్స్ ఇవ్వబడింది. గ్రిడ్ రీ ఎనర్జైజింగ్ ప్రక్రియను ప్రారంభించండి."
                                tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
                                onAlertChanged("🔊 BROADCASTING LINE RETURNING SAFETY IN TELUGU")
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = CustomLavText),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("⚡ BROADCAST RE-ARMING CLEAR", fontSize = 8.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BuildSuiteGuideModal(onClose: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .border(2.dp, CustomLavText, RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = CyberGray)
    ) {
        Column(modifier = Modifier.padding(16.dp).verticalScroll(rememberScrollState())) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "🛠️ APTRANSCO MOBILE BUILD & SYNC SUITE",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                IconButton(onClick = onClose) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "close", tint = Color.White)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text("COMPILATION & OFFLINE TEST STEPS:", color = CustomLavText, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                "1. **Local Room SQLite Sync**:\n" +
                "   The application utilizes local Android Room Database mapping to preserve safety logs, offline defects, and trip reports.\n\n" +
                "2. **Capacitor Geofencing Setup**:\n" +
                "   To run on device, configure `AndroidManifest.xml` with appropriate permission nodes:\n" +
                "   - `android.permission.ACCESS_FINE_LOCATION`\n" +
                "   - `android.permission.ACCESS_BACKGROUND_LOCATION` (Required for background crew tracking)\n\n" +
                "3. **Local Storage Testing**:\n" +
                "   Toggle the `OFFLINE/ONLINE` mode transponder button in the top header. Recorded faults transition into `RECORDED_CACHED` states and persist on-device across clean restarts.\n\n" +
                "4. **Native Gradle Command Build**:\n" +
                "   Execute regional package assembly via the build terminal:\n" +
                "   `gradle assembleDebug` to compile LineCare-Release.apk.",
                color = Color.LightGray,
                fontSize = 11.sp,
                lineHeight = 15.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onClose,
                colors = ButtonDefaults.buttonColors(containerColor = CustomLavText),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("DISMISS SUITE INTELLIGENCE", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun MainOverviewDashboard(
    tasks: List<TaskEntity>,
    defects: List<DefectEntity>,
    completedWorkOrders: List<com.example.ui.CompletedWorkOrder>,
    isSafetyUnlocked: Boolean,
    onNavigateToTab: (String) -> Unit,
    onStartPatrol: (String) -> Unit,
    onCompletePatrol: (String) -> Unit,
    onApproveSafety: () -> Unit,
    onToggleCheck: (Int) -> Unit,
    safetyChecks: List<Boolean>
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- Tailwind-style Premium Header Banner ---
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = CyberCard),
            border = BorderStroke(1.dp, BorderColor),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .drawBehind {
                        // Drawing a subtle decorative indigo accent line on the left edge
                        drawRect(
                            color = SafetyOrange,
                            topLeft = Offset(0f, 0f),
                            size = androidx.compose.ui.geometry.Size(6.dp.toPx(), size.height)
                        )
                    }
                    .padding(start = 18.dp, top = 16.dp, end = 16.dp, bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "APTRANSCO GRID GUARD",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = CustomLavText
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(FieldGreen.copy(alpha = 0.12f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "MONITOR ACTIVE",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = FieldGreen,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Real-time EHT transmission dispatch log, defect hotspots tracking, and safety clearance interlocks.",
                        fontSize = 11.sp,
                        color = Color(0xFF475569) // slate-600
                    )
                }
            }
        }

        // --- SECTION 1: STATS GRID COUNTERS (3-Column Tailwind Metric Cards) ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val ongoingCount = tasks.filter { it.status != "COMPLETED" }.size
            val pendingCount = defects.size
            val ptwCount = completedWorkOrders.size

            // Metric 1: Patrols
            Card(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onNavigateToTab("DUTIES") },
                colors = CardDefaults.cardColors(containerColor = CyberCard),
                border = BorderStroke(1.dp, BorderColor),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .drawBehind {
                            drawRect(
                                color = CyberBlue,
                                topLeft = Offset(0f, 0f),
                                size = androidx.compose.ui.geometry.Size(size.width, 3.dp.toPx())
                            )
                        }
                        .padding(top = 10.dp, start = 12.dp, end = 12.dp, bottom = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "PATROL ROUTES",
                        fontSize = 8.sp,
                        color = Color(0xFF64748B), // slate-500
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = ongoingCount.toString(),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black,
                        color = CyberBlue,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Manage →",
                        fontSize = 8.sp,
                        color = CyberBlue,
                        fontWeight = FontWeight.Bold,
                        textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline
                    )
                }
            }

            // Metric 2: Defect Log
            Card(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onNavigateToTab("DUTIES") },
                colors = CardDefaults.cardColors(containerColor = CyberCard),
                border = BorderStroke(1.dp, BorderColor),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .drawBehind {
                            drawRect(
                                color = SafetyYellow,
                                topLeft = Offset(0f, 0f),
                                size = androidx.compose.ui.geometry.Size(size.width, 3.dp.toPx())
                            )
                        }
                        .padding(top = 10.dp, start = 12.dp, end = 12.dp, bottom = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "FAULT REPORTS",
                        fontSize = 8.sp,
                        color = Color(0xFF64748B), // slate-500
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = pendingCount.toString(),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFFB45309), // dark amber
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Register →",
                        fontSize = 8.sp,
                        color = SafetyOrange,
                        fontWeight = FontWeight.Bold,
                        textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline
                    )
                }
            }

            // Metric 3: Safe Clearances (PTW)
            Card(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onNavigateToTab("PTW") },
                colors = CardDefaults.cardColors(containerColor = CyberCard),
                border = BorderStroke(1.dp, BorderColor),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .drawBehind {
                            drawRect(
                                color = FieldGreen,
                                topLeft = Offset(0f, 0f),
                                size = androidx.compose.ui.geometry.Size(size.width, 3.dp.toPx())
                            )
                        }
                        .padding(top = 10.dp, start = 12.dp, end = 12.dp, bottom = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "ACTIVE PTWS",
                        fontSize = 8.sp,
                        color = Color(0xFF64748B), // slate-500
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = ptwCount.toString(),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black,
                        color = FieldGreen,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Ledger →",
                        fontSize = 8.sp,
                        color = FieldGreen,
                        fontWeight = FontWeight.Bold,
                        textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline
                    )
                }
            }
        }

        // --- SECTION 2: ACTIVE PATROL ROUTES ---
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = CyberCard),
            border = BorderStroke(1.dp, BorderColor),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(CyberBlue.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Active Patrol Icon",
                                tint = CyberBlue,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                        Text(
                            text = "ACTIVE PATROL ROUTES",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = Color(0xFF1E293B) // slate-800
                        )
                    }
                    Text(
                        text = "VIEW BOARD",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = CyberBlue,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier
                            .clickable { onNavigateToTab("DUTIES") }
                            .padding(4.dp)
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))

                val incompleteTasks = tasks.filter { it.status != "COMPLETED" }.take(2)
                if (incompleteTasks.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(CyberGray.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                            .border(1.dp, BorderColor, RoundedCornerShape(12.dp))
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "✓ All patrol routes finished successfully. Station is secure.",
                            color = FieldGreen,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        incompleteTasks.forEach { task ->
                            Box(modifier = Modifier.weight(1f)) {
                                OngoingPatrolGridItem(
                                    task = task,
                                    isSafetyUnlocked = isSafetyUnlocked,
                                    onStart = { onStartPatrol(task.id) },
                                    onComplete = { onCompletePatrol(task.id) }
                                )
                            }
                        }
                        if (incompleteTasks.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }

        // --- SECTION 3: PERMIT TO WORK STATUS CARDS ---
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = CyberCard),
            border = BorderStroke(1.dp, BorderColor),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(FieldGreen.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "PTW Clearance Icon",
                                tint = FieldGreen,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                        Text(
                            text = "PERMIT TO WORK GATEWAY",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = Color(0xFF1E293B)
                        )
                    }
                    Text(
                        text = "PTW INDEX",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = FieldGreen,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier
                            .clickable { onNavigateToTab("PTW") }
                            .padding(4.dp)
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))

                if (!isSafetyUnlocked) {
                    // Modern alert box
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFFFFBEB), RoundedCornerShape(10.dp))
                            .border(1.dp, Color(0xFFFEF3C7), RoundedCornerShape(10.dp))
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Lock Icon",
                            tint = Color(0xFFD97706),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "SAFETY BLOCKS IN PLACE. Check pre-requisites to activate the line clearance key:",
                            color = Color(0xFF92400E),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))

                    // 2x3 Grid of Interactive safety checks
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        val checkLabels = listOf(
                            "Ground ropes ready",
                            "Lockout-Tagout active",
                            "PPE & Harness worn",
                            "Tester zero verified",
                            "Control log approved"
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Box(modifier = Modifier.weight(1f)) {
                                SafetyChip(0, checkLabels[0], safetyChecks[0], onToggleCheck)
                            }
                            Box(modifier = Modifier.weight(1f)) {
                                SafetyChip(1, checkLabels[1], safetyChecks[1], onToggleCheck)
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Box(modifier = Modifier.weight(1f)) {
                                SafetyChip(2, checkLabels[2], safetyChecks[2], onToggleCheck)
                            }
                            Box(modifier = Modifier.weight(1f)) {
                                SafetyChip(3, checkLabels[3], safetyChecks[3], onToggleCheck)
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Box(modifier = Modifier.weight(1f)) {
                                SafetyChip(4, checkLabels[4], safetyChecks[4], onToggleCheck)
                            }
                            Box(modifier = Modifier.weight(1f)) {
                                val allChecked = safetyChecks.all { it }
                                Button(
                                    onClick = { if (allChecked) onApproveSafety() },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (allChecked) FieldGreen else Color(0xFFE2E8F0)
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(34.dp),
                                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = if (allChecked) "✓ UNLOCK PTW" else "PENDING SAFETIES",
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (allChecked) Color.White else Color(0xFF94A3B8),
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        }
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFECFDF5), RoundedCornerShape(10.dp))
                                .border(1.dp, Color(0xFFA7F3D0), RoundedCornerShape(10.dp))
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Ready",
                                tint = FieldGreen,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "✓ INTEGRATED KEY SAFETY MASTER ENGAGED. General line entries cleared.",
                                color = Color(0xFF065F46),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        
                        Text(
                            text = "LATEST CLEARANCE LEDGERS:",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF64748B),
                            fontFamily = FontFamily.Monospace
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            completedWorkOrders.take(2).forEach { ptw ->
                                Box(modifier = Modifier.weight(1f)) {
                                    PtwGridItem(ptw)
                                }
                            }
                            if (completedWorkOrders.isEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(CyberGray, RoundedCornerShape(8.dp))
                                        .padding(12.dp)
                                ) {
                                    Text("No clearance records logged.", color = Color(0xFF475569), fontSize = 9.sp)
                                }
                            } else if (completedWorkOrders.size == 1) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }

        // --- SECTION 4: PENDING FAULT REPORTS & DECAY LOG ---
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = CyberCard),
            border = BorderStroke(1.dp, BorderColor),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFFFF1F2)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Fault Warning Icon",
                                tint = Color(0xFFE11D48),
                                modifier = Modifier.size(14.dp)
                            )
                        }
                        Text(
                            text = "PENDING FAULT REPORTS",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = Color(0xFF1E293B)
                        )
                    }
                    Text(
                        text = "+ NEW FAULT",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFE11D48),
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier
                            .clickable { onNavigateToTab("DUTIES") }
                            .padding(4.dp)
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))

                if (defects.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(CyberGray.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                            .border(1.dp, BorderColor, RoundedCornerShape(12.dp))
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "✓ Grid healthy. No mechanical or hot-spot faults recorded today.",
                            color = FieldGreen,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        defects.take(2).forEach { defect ->
                            Box(modifier = Modifier.weight(1f)) {
                                DefectGridItem(defect)
                            }
                        }
                        if (defects.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun OngoingPatrolGridItem(
    task: TaskEntity,
    isSafetyUnlocked: Boolean,
    onStart: () -> Unit,
    onComplete: () -> Unit
) {
    val isWork = task.status == "IN_PROGRESS"
    val isAwaiting = task.status == "AWAITING_PTW"

    Card(
        colors = CardDefaults.cardColors(containerColor = CyberGray),
        border = BorderStroke(1.dp, BorderColor),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .drawBehind {
                    // Tailwind border stripe equivalent
                    drawRect(
                        color = if (isWork) CyberBlue else Color(0xFF94A3B8),
                        topLeft = Offset(0f, 0f),
                        size = androidx.compose.ui.geometry.Size(4.dp.toPx(), size.height)
                    )
                }
                .padding(start = 12.dp, top = 10.dp, end = 10.dp, bottom = 10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = task.id,
                    fontSize = 8.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = CyberBlue
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(
                            when {
                                isWork -> CyberBlue.copy(alpha = 0.12f)
                                isAwaiting -> Color(0xFFFEF3C7)
                                else -> Color(0xFFF1F5F9)
                            }
                        )
                        .padding(horizontal = 5.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = if (isWork) "ON-AIR" else "AWAITING",
                        fontSize = 7.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        color = if (isWork) CyberBlue else Color(0xFFB45309) // contrast warm brown
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = task.title,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1E293B), // high contrast slate-800
                maxLines = 1
            )
            Text(
                text = "${task.voltage} • ${task.lineName}",
                fontSize = 8.sp,
                color = Color(0xFF475569), // high contrast slate-600
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(8.dp))

            if (isAwaiting) {
                Button(
                    onClick = { if (isSafetyUnlocked) onStart() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isSafetyUnlocked) CyberBlue else Color(0xFFE2E8F0)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(26.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = if (isSafetyUnlocked) "START PATROL" else "LOCKED",
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isSafetyUnlocked) Color.White else Color(0xFF94A3B8),
                        fontFamily = FontFamily.Monospace
                    )
                }
            } else if (isWork) {
                Button(
                    onClick = onComplete,
                    colors = ButtonDefaults.buttonColors(containerColor = FieldGreen),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(26.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = "COMPLETE SAFE",
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}

@Composable
fun SafetyChip(
    index: Int,
    label: String,
    checked: Boolean,
    onToggle: (Int) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle(index) },
        colors = CardDefaults.cardColors(
            containerColor = if (checked) Color(0xFFECFDF5) else CyberGray
        ),
        border = BorderStroke(
            width = 1.dp,
            color = if (checked) Color(0xFF10B981) else BorderColor
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(if (checked) FieldGreen else Color(0xFFCBD5E1))
                    .border(1.dp, if (checked) FieldGreen else Color(0xFF94A3B8), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (checked) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "OK",
                        tint = Color.White,
                        modifier = Modifier.size(8.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
                color = if (checked) Color(0xFF047857) else Color(0xFF1E293B),
                maxLines = 1,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

@Composable
fun PtwGridItem(ptw: com.example.ui.CompletedWorkOrder) {
    Card(
        colors = CardDefaults.cardColors(containerColor = CyberGray),
        border = BorderStroke(1.dp, BorderColor),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .drawBehind {
                    drawRect(
                        color = FieldGreen,
                        topLeft = Offset(0f, 0f),
                        size = androidx.compose.ui.geometry.Size(4.dp.toPx(), size.height)
                    )
                }
                .padding(start = 12.dp, top = 8.dp, end = 8.dp, bottom = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = ptw.id,
                    fontSize = 8.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = FieldGreen
                )
                Text(
                    text = ptw.handbackTime,
                    fontSize = 7.sp,
                    color = Color(0xFF64748B),
                    fontFamily = FontFamily.Monospace
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = ptw.towerRange,
                fontSize = 10.sp,
                fontWeight = FontWeight.Black,
                color = Color(0xFF1E293B),
                maxLines = 1
            )
            Text(
                text = "Sig: ${ptw.supervisorSignature}",
                fontSize = 8.sp,
                color = CustomLavText,
                maxLines = 1,
                fontFamily = FontFamily.Monospace
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = ptw.declaration,
                fontSize = 8.sp,
                color = Color(0xFF475569),
                maxLines = 2,
                lineHeight = 10.sp,
                fontStyle = FontStyle.Italic
            )
        }
    }
}

@Composable
fun DefectGridItem(defect: DefectEntity) {
    val isSynced = defect.status == "SYNCED"

    Card(
        colors = CardDefaults.cardColors(containerColor = CyberGray),
        border = BorderStroke(1.dp, BorderColor),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .drawBehind {
                    drawRect(
                        color = if (isSynced) FieldGreen else Color(0xFFE11D48),
                        topLeft = Offset(0f, 0f),
                        size = androidx.compose.ui.geometry.Size(4.dp.toPx(), size.height)
                    )
                }
                .padding(start = 12.dp, top = 8.dp, end = 8.dp, bottom = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = defect.id,
                    fontSize = 8.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFE11D48)
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(
                            if (isSynced) Color(0xFFECFDF5) else Color(0xFFFFF1F2)
                        )
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = if (isSynced) "SYNCED" else "CACHED",
                        fontSize = 7.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        color = if (isSynced) Color(0xFF047857) else Color(0xFFE11D48)
                    )
                }
            }
            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .border(0.5.dp, BorderColor, RoundedCornerShape(6.dp))
                        .background(Color(0xFFE2E8F0))
                ) {
                    AsyncImage(
                        model = defect.imageUrl,
                        contentDescription = "defect-img",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = defect.category,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E293B),
                        maxLines = 1
                    )
                    Text(
                        text = defect.timestamp,
                        fontSize = 7.sp,
                        color = Color(0xFF64748B),
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = defect.notes,
                fontSize = 8.sp,
                color = Color(0xFF475569),
                maxLines = 2,
                lineHeight = 10.sp
            )
        }
    }
}

// End of MainActivity.kt

// End of MainActivity.kt
