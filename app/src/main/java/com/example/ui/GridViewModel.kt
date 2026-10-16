package com.example.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.data.AppDatabase
import com.example.data.DefectEntity
import com.example.data.GridRepository
import com.example.data.TaskEntity
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

data class TaurusParsedResult(
    val lineName: String = "",
    val circuit: String = "",
    val distance: String = "",
    val rCurrent: String = "",
    val yCurrent: String = "",
    val bCurrent: String = ""
)

class GridViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val repository = GridRepository(database)

    // Observables from Database
    val tasks: StateFlow<List<TaskEntity>> = repository.allTasks
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val defects: StateFlow<List<DefectEntity>> = repository.allDefects
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Offline mode simulation
    private val _isOffline = MutableStateFlow(false)
    val isOffline = _isOffline.asStateFlow()

    // 5 Safety Gate checklist conditions
    private val _safetyChecks = MutableStateFlow(listOf(false, false, false, false, false))
    val safetyChecks = _safetyChecks.asStateFlow()

    // Safety Gate Unlocked cache
    private val _isSafetyUnlocked = MutableStateFlow(false)
    val isSafetyUnlocked = _isSafetyUnlocked.asStateFlow()

    // Camera Mock states
    private val _cameraImage = MutableStateFlow<String?>(null)
    val cameraImage = _cameraImage.asStateFlow()

    private val _cameraStatusText = MutableStateFlow("CHOOSE CATEGORY & TAP SHUTTER")
    val cameraStatusText = _cameraStatusText.asStateFlow()

    private val _isCameraScanning = MutableStateFlow(false)
    val isCameraScanning = _isCameraScanning.asStateFlow()

    // Taurus Parser States
    private val _taurusInput = MutableStateFlow("")
    val taurusInput = _taurusInput.asStateFlow()

    private val _parsedResult = MutableStateFlow<TaurusParsedResult?>(null)
    val parsedResult = _parsedResult.asStateFlow()

    // Gemini assistant response
    private val _geminiAnalysis = MutableStateFlow<String?>(null)
    val geminiAnalysis = _geminiAnalysis.asStateFlow()

    private val _isAnalyzing = MutableStateFlow(false)
    val isAnalyzing = _isAnalyzing.asStateFlow()

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    init {
        viewModelScope.launch {
            repository.checkAndSeedInitialTasks()
            // Check persistence or shared Preferences if you want to restore safety clearance status
            val sp = getApplication<Application>().getSharedPreferences("gridguard_prefs", Context.MODE_PRIVATE)
            _isSafetyUnlocked.value = sp.getBoolean("safety_cleared", false)
        }
    }

    fun toggleOfflineMode() {
        _isOffline.value = !_isOffline.value
    }

    fun toggleSafetyCheck(index: Int) {
        val current = _safetyChecks.value.toMutableList()
        if (index in current.indices) {
            current[index] = !current[index]
            _safetyChecks.value = current
        }
    }

    fun approveSafetyClearance() {
        if (_safetyChecks.value.all { it }) {
            _isSafetyUnlocked.value = true
            val sp = getApplication<Application>().getSharedPreferences("gridguard_prefs", Context.MODE_PRIVATE)
            sp.edit().putBoolean("safety_cleared", true).apply()
        }
    }

    fun resetSafetyChecks() {
        _safetyChecks.value = listOf(false, false, false, false, false)
        _isSafetyUnlocked.value = false
        val sp = getApplication<Application>().getSharedPreferences("gridguard_prefs", Context.MODE_PRIVATE)
        sp.edit().putBoolean("safety_cleared", false).apply()
    }

    fun startTask(taskId: String) {
        if (!_isSafetyUnlocked.value) return
        viewModelScope.launch {
            repository.updateTaskStatus(taskId, "IN_PROGRESS")
        }
    }

    fun completeTask(taskId: String) {
        viewModelScope.launch {
            repository.updateTaskStatus(taskId, "COMPLETED")
        }
    }

    // Capture standard camera mockup in preconfigured transmission imagery
    fun triggerCameraShutter() {
        _isCameraScanning.value = true
        _cameraStatusText.value = "ALIGNING SUN GLARE OPTICS..."
        _cameraImage.value = null

        val pylonUrls = listOf(
            "https://images.unsplash.com/photo-1473341304170-971dccb5ac1e?q=80&w=600&auto=format&fit=crop",
            "https://images.unsplash.com/photo-1541888946425-d81bb19240f5?q=80&w=600&auto=format&fit=crop",
            "https://images.unsplash.com/photo-1504307651254-35680f356dfd?q=80&w=600&auto=format&fit=crop"
        )
        val selectedImage = pylonUrls.random()

        viewModelScope.launch {
            kotlinx.coroutines.delay(1200) // realistic autofocus simulation
            _cameraImage.value = selectedImage
            _isCameraScanning.value = false
            _cameraStatusText.value = "✓ DATA ACQUIRED AND LOCATED"
        }
    }

    fun recordDefect(category: String, notes: String) {
        val image = _cameraImage.value ?: "https://images.unsplash.com/photo-1473341304170-971dccb5ac1e?q=80&w=200&auto=format&fit=crop"
        val format = SimpleDateFormat("hh:mm a", Locale.getDefault())
        val timeStr = format.format(Date())
        val idStr = "DEF-" + (1000..9999).random()

        viewModelScope.launch {
            val entity = DefectEntity(
                id = idStr,
                category = category,
                notes = notes,
                imageUrl = image,
                timestamp = timeStr,
                latitude = 16.5301,
                longitude = 80.6111,
                status = if (_isOffline.value) "RECORDED_CACHED" else "SYNCED"
            )
            repository.insertDefect(entity)
            // reset camera mock
            _cameraImage.value = null
            _cameraStatusText.value = "CHOOSE CATEGORY & TAP SHUTTER"
        }
    }

    fun purgeDefect(localId: Int) {
        viewModelScope.launch {
            repository.deleteDefect(localId)
        }
    }

    fun loadSampleTaurusText() {
        _taurusInput.value = """TRIPPING REPORT - NET:
LINE: 132KV NLR-VJA Segment NLR Ckt-2
TAURUS FAULT LOCATOR DISTANCE DETERMINED: NLR Circuit breaker reads distance 45.2 km 
PHASE EXTREM FAULT CURRENTS DETECTED DURING TRIP SECONDS:
R-PHASE: 16.8 kA (Primary earth short arcing)
Y-PHASE: 1.2 kA
B-PHASE: 0.95 kA
REASON: HIGH TENSION insulator flashover due to storm branches. Clearing gang dispatched."""
    }

    fun updateTaurusInput(input: String) {
        _taurusInput.value = input
    }

    // High performance local regex parsing
    fun parseTaurusText() {
        val input = _taurusInput.value
        if (input.isBlank()) return

        // Regex for Line name
        var lineName = "132KV Nellore-Vijayawada Ckt-2"
        val lineRegex = Regex("(?:LINE|Segment|Segment Line):\\s*([^\\n\\r]+)", RegexOption.IGNORE_CASE)
        val lineMatch = lineRegex.find(input)
        if (lineMatch != null) {
            lineName = lineMatch.groupValues.getOrNull(1)?.trim() ?: lineName
        } else {
            val alternateMath = Regex("(?:132KV|220KV|400KV)\\s*[A-Z\\-]+", RegexOption.IGNORE_CASE).find(input)
            if (alternateMath != null) {
                lineName = alternateMath.value
            }
        }

        // Parse circuit
        var circuit = "Circuit-2"
        if (input.contains("circuit-1", ignoreCase = true) || input.contains("ckt-1", ignoreCase = true) || input.contains("line-1", ignoreCase = true)) {
            circuit = "Circuit-1"
        } else if (input.contains("circuit-2", ignoreCase = true) || input.contains("ckt-2", ignoreCase = true) || input.contains("line-2", ignoreCase = true)) {
            circuit = "Circuit-2"
        }

        // Parse Distance
        var distance = "45.2 KM"
        val distRegex = Regex("(?:distance|locator|km|at)\\s*([\\d\\.]+)\\s*(?:km|distance)?", RegexOption.IGNORE_CASE)
        val distMatch = distRegex.find(input)
        if (distMatch != null) {
            distance = "${distMatch.groupValues.getOrNull(1) ?: "45.2"} KM"
        }

        // Parse currents
        fun parsePhase(phase: String): String {
            val r = Regex("$phase-PHASE:\\s*([\\d\\.]+)\\s*(?:kA|amperes)?", RegexOption.IGNORE_CASE)
            return r.find(input)?.groupValues?.getOrNull(1)?.let { "$it kA" } ?: "--"
        }

        _parsedResult.value = TaurusParsedResult(
            lineName = lineName,
            circuit = circuit,
            distance = distance,
            rCurrent = parsePhase("R"),
            yCurrent = parsePhase("Y"),
            bCurrent = parsePhase("B")
        )
        _geminiAnalysis.value = null // clear previous advice
    }

    // Call Gemini API to explain fault parameters
    fun triggerGeminiSmartAnalysis() {
        val result = _parsedResult.value ?: return
        val rawLogs = _taurusInput.value
        _isAnalyzing.value = true
        _geminiAnalysis.value = null

        val prompt = """
            You are a expert Power Grid Reliability Engineer at APTRANSCO.
            Analyze the following Substation Tripping event Logs and parsed metrics:
            
            LINE SEGMENT: ${result.lineName}
            CIRCUIT ID: ${result.circuit}
            TAURUS DISTANCE READ: ${result.distance}
            PHASE SURGE CURRENTS:
            - Red Phase: ${result.rCurrent}
            - Yellow Phase: ${result.yCurrent}
            - Blue Phase: ${result.bCurrent}
            
            RAW LOG TEXT:
            $rawLogs
            
            Format your response into three highly-readable bullet points matching these topics:
            1. **Incident Severity & Cause Analysis**: Clear explanation of which phase suffered the short (based on high current values e.g. R-Phase is high) and estimated root cause (arcing, ground strike, branches).
            2. **Material Clearances Packet**: List exactly which equipment/insulators the CBD patrolling gang needs to carry to standard height (e.g. 11kV/132kV disc polymer insulators, structural binding strings, clearing chains).
            3. **Primary Field Hazards**: Actionable warning precautions for Srinivas Rao (Supervisor) when patrolling around ${result.distance} from Nellore breaker.
            
            Keep the content highly concise, field-friendly, and professional. Do not write filler.
        """.trimIndent()

        viewModelScope.launch {
            val responseText = callGeminiRestApi(prompt)
            _geminiAnalysis.value = responseText
            _isAnalyzing.value = false
        }
    }

    private suspend fun callGeminiRestApi(prompt: String): String = withContext(Dispatchers.IO) {
        val key = BuildConfig.GEMINI_API_KEY
        if (key.isBlank() || key == "MY_GEMINI_API_KEY") {
            return@withContext "GridGuard Security Warning: No valid API key is configured. Please insert your GEMINI_API_KEY inside the Secrets panel of APTRANSCO console."
        }

        val jsonMediaType = "application/json; charset=utf-8".toMediaType()
        
        // Prepare request body manually to avoid any complex serialization plugin bugs
        val requestJson = """
            {
              "contents": [
                {
                  "parts": [
                    {
                      "text": ${escapeJsonText(prompt)}
                    }
                  ]
                }
              ]
            }
        """.trimIndent()

        val request = Request.Builder()
            .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$key")
            .post(requestJson.toRequestBody(jsonMediaType))
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext "API error: Substation transponder code ${response.code}"
                }
                val bodyText = response.body?.string() ?: return@withContext "No response body received"
                return@withContext parseGeminiTextResponse(bodyText)
            }
        } catch (e: Exception) {
            "Transponder Network offline: ${e.message}"
        }
    }

    private fun escapeJsonText(text: String): String {
        return moshi.adapter(String::class.java).toJson(text)
    }

    private fun parseGeminiTextResponse(bodyText: String): String {
        return try {
            val mapAdapter = moshi.adapter(Map::class.java)
            val jsonMap = mapAdapter.fromJson(bodyText)
            val candidates = jsonMap?.get("candidates") as? List<*>
            val candidate = candidates?.firstOrNull() as? Map<*, *>
            val content = candidate?.get("content") as? Map<*, *>
            val parts = content?.get("parts") as? List<*>
            val part = parts?.firstOrNull() as? Map<*, *>
            part?.get("text") as? String ?: "No text candidate parsed from transponder telemetry lines."
        } catch (e: Exception) {
            "Exception during parsing: ${e.message}"
        }
    }

    // --- TRANS TRANSCO GRID SECURE EXTENSION FLOWS & LOGGER STATES ---

    // 1. Lineman Maintenance Panel: Patrols, PMI logger, Completed ledger
    private val _feeders = MutableStateFlow(listOf(
        FeederStatus("L01 (Nellore-VJA)", "132 kV", "Srinivas Rao Crew", 12, 24),
        FeederStatus("L02 (Ongole-VJA)", "220 kV", "Prasad Reddy Crew", 8, 30),
        FeederStatus("L03 (Guntur-VJA)", "400 kV", "Mohan Naidu Crew", 18, 20)
    ))
    val feeders = _feeders.asStateFlow()

    private val _pmiLogs = MutableStateFlow(listOf(
        PmiLog("PMI-702", "08:15 AM", 2.4, 12.5, false, false),
        PmiLog("PMI-701", "Yesterday", 5.8, 34.2, true, true)
    ))
    val pmiLogs = _pmiLogs.asStateFlow()

    private val _completedWorkOrders = MutableStateFlow(listOf(
        CompletedWorkOrder("PTW-NLR-04", "Tower VJA-10 to VJA-12", "Srinivas Rao", "10:30 AM", "Grid area has been swept of all maintenance earthing ropes. Clear for re-energization!"),
        CompletedWorkOrder("PTW-NLR-03", "Tower NLR-45 to NLR-48", "Chandrasekhar", "Yesterday", "Clearances returned. All portable earths removed from line, safe to handback.")
    ))
    val completedWorkOrders = _completedWorkOrders.asStateFlow()

    // 2. Driver & Logistics
    private val _odometer = MutableStateFlow(42180)
    val odometer = _odometer.asStateFlow()

    private val _driverTrips = MutableStateFlow(listOf(
        DriverTrip("TRIP-51", "Kovur Substation", "Tower VJA-40 Patrol", "Active"),
        DriverTrip("TRIP-50", "Vijayawada HQ", "Nellore S/S", "Completed")
    ))
    val driverTrips = _driverTrips.asStateFlow()

    private val _isDispatchAlertActive = MutableStateFlow(true)
    val isDispatchAlertActive = _isDispatchAlertActive.asStateFlow()

    private val _mutualSatelliteUnlocked = MutableStateFlow(false)
    val mutualSatelliteUnlocked = _mutualSatelliteUnlocked.asStateFlow()

    // 3. Crew Proximity & Audio Visualizer
    private val _crewPositions = MutableStateFlow(listOf(
        CrewPosition("Suresh Rao", 8, false, "Lineman A"),
        CrewPosition("Ramesh Babu", 32, false, "Lineman B"),
        CrewPosition("Naidu Prasad", 4, true, "Safety Watch"),
        CrewPosition("Srinivas Rao", 2, true, "Supervisor")
    ))
    val crewPositions = _crewPositions.asStateFlow()

    private val _vanIdleMinutes = MutableStateFlow(18)
    val vanIdleMinutes = _vanIdleMinutes.asStateFlow()

    // 4. GIS Map Toggles and Thermal scanning
    private val _mapLayers = MutableStateFlow(mapOf(
        "132kV" to true,
        "220kV" to true,
        "400kV" to false
    ))
    val mapLayers = _mapLayers.asStateFlow()

    private val _thermalTowers = MutableStateFlow(listOf(
        GisTowerInfo("Tower VJA-12", "132kV", 42.0, "Operational"),
        GisTowerInfo("Tower VJA-14", "132kV", 71.5, "Needs Patrol"), // Hotspot!
        GisTowerInfo("Tower NLR-48", "220kV", 68.2, "Defect Reported"), // Hotspot!
        GisTowerInfo("Tower GUN-02", "400kV", 38.5, "Operational")
    ))
    val thermalTowers = _thermalTowers.asStateFlow()

    // Functions for updating states
    fun incrementFeederTowers(index: Int) {
        val current = _feeders.value.toMutableList()
        if (index in current.indices) {
            val f = current[index]
            val nextInspected = (f.inspectedTowers + 5).coerceAtMost(f.totalTowers)
            current[index] = f.copy(inspectedTowers = nextInspected)
            _feeders.value = current
        }
    }

    fun registerNewFeeder(name: String, voltage: String, crew: String, totalTowers: Int) {
        val newFeeder = FeederStatus(name, voltage, crew, 0, totalTowers)
        _feeders.value = _feeders.value + newFeeder
    }

    fun submitPmiLog(resistance: Double, leakage: Double) {
        val format = SimpleDateFormat("hh:mm a", Locale.getDefault())
        val timeStr = format.format(Date())
        val idStr = "PMI-" + (700..799).random()
        val resBreach = resistance > 5.0
        val leakBreach = leakage > 30.0
        val newLog = PmiLog(idStr, timeStr, resistance, leakage, resBreach, leakBreach)
        _pmiLogs.value = listOf(newLog) + _pmiLogs.value
    }

    fun submitCompletedWork(towerRange: String, supervisor: String, declaration: String) {
        val format = SimpleDateFormat("hh:mm a", Locale.getDefault())
        val timeStr = format.format(Date())
        val idStr = "PTW-" + (100..499).random()
        val newOrder = CompletedWorkOrder(idStr, towerRange, supervisor, timeStr, declaration)
        _completedWorkOrders.value = listOf(newOrder) + _completedWorkOrders.value
    }

    fun updateOdometer(kms: Int) {
        _odometer.value = kms
    }

    fun dismissDispatchAlert() {
        _isDispatchAlertActive.value = false
    }

    fun triggerMutilSatelliteLock() {
        _mutualSatelliteUnlocked.value = !_mutualSatelliteUnlocked.value
    }

    fun updateCrewDistance(name: String, distance: Int) {
        val current = _crewPositions.value.map {
            if (it.name == name) it.copy(distanceMeters = distance) else it
        }
        _crewPositions.value = current
    }

    fun updateVanIdleMinutes(mins: Int) {
        _vanIdleMinutes.value = mins
    }

    fun toggleMapLayer(layer: String) {
        val current = _mapLayers.value.toMutableMap()
        current[layer] = !(current[layer] ?: false)
        _mapLayers.value = current
    }

    fun updateTowerStatus(towerId: String, nextStatus: String) {
        val current = _thermalTowers.value.map {
            if (it.id == towerId) it.copy(status = nextStatus) else it
        }
        _thermalTowers.value = current
    }
}

// Data structures for the requested summary areas:

data class FeederStatus(
    val name: String,
    val voltage: String,
    val crewTag: String,
    val inspectedTowers: Int,
    val totalTowers: Int
)

data class PmiLog(
    val id: String,
    val timestamp: String,
    val earthResistance: Double,
    val leakageCurrent: Double,
    val resistanceBreach: Boolean,
    val leakageBreach: Boolean
)

data class CompletedWorkOrder(
    val id: String,
    val towerRange: String,
    val supervisorSignature: String,
    val handbackTime: String,
    val declaration: String
)

data class DriverTrip(
    val id: String,
    val startDest: String,
    val endDest: String,
    val status: String
)

data class CrewPosition(
    val name: String,
    val distanceMeters: Int,
    val isIdle: Boolean,
    val role: String
)

data class GisTowerInfo(
    val id: String,
    val voltageLine: String, // "132kV", "220kV", "400kV"
    val temperatureCelsius: Double,
    val status: String // "Needs Patrol", "Defect Reported", "Under Repair", "Operational"
)
