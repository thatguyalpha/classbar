package com.thatguyalpha.classbar

import android.content.Context
import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class MainViewModel(
    private val appContext: Context,
    private val preferences: AppPreferences = AppPreferences(appContext)
) : ViewModel() {

    private val dateFormatApi = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    private val dateFormatDisplay = SimpleDateFormat("EEEE, d MMMM", Locale.getDefault())

    // --- Authentication State ---
    private val _isLoggedIn = MutableStateFlow(preferences.isLoggedIn())
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    private val _studentName = MutableStateFlow(preferences.getSavedPupilName())
    val studentName: StateFlow<String> = _studentName.asStateFlow()

    private val _className = MutableStateFlow(preferences.getSavedClassName())
    val className: StateFlow<String> = _className.asStateFlow()

    private val _isLoggingIn = MutableStateFlow(false)
    val isLoggingIn: StateFlow<Boolean> = _isLoggingIn.asStateFlow()

    private val _loginError = MutableStateFlow<String?>(null)
    val loginError: StateFlow<String?> = _loginError.asStateFlow()

    // --- Schedule State ---
    private var currentCalendar: Calendar = Calendar.getInstance()

    private val _selectedDateString = MutableStateFlow(dateFormatApi.format(currentCalendar.time))
    val selectedDateString: StateFlow<String> = _selectedDateString.asStateFlow()

    private val _selectedDateFormatted = MutableStateFlow(dateFormatDisplay.format(currentCalendar.time))
    val selectedDateFormatted: StateFlow<String> = _selectedDateFormatted.asStateFlow()

    private val _isToday = MutableStateFlow(true)
    val isToday: StateFlow<Boolean> = _isToday.asStateFlow()

    private val _lessons = MutableStateFlow<List<Lesson>>(emptyList())
    val lessons: StateFlow<List<Lesson>> = _lessons.asStateFlow()

    private val _isLoadingLessons = MutableStateFlow(false)
    val isLoadingLessons: StateFlow<Boolean> = _isLoadingLessons.asStateFlow()

    private val _lessonsError = MutableStateFlow<String?>(null)
    val lessonsError: StateFlow<String?> = _lessonsError.asStateFlow()

    // --- Icon & Settings State ---
    private val _settings = MutableStateFlow(preferences.loadSettings())
    val settings: StateFlow<IconSettings> = _settings.asStateFlow()

    private val _previewBitmap = MutableStateFlow(
        ClassBarIconGenerator.generatePreviewBitmap(_settings.value)
    )
    val previewBitmap: StateFlow<Bitmap> = _previewBitmap.asStateFlow()

    init {
        if (preferences.isLoggedIn()) {
            loadScheduleForDate(_selectedDateString.value)
            if (_settings.value.isEnabled) {
                ClassBarService.startOrRefresh(appContext)
            }
        }
    }

    // --- Auth Actions ---

    fun login(email: String, pass: String) {
        val trimmedEmail = email.trim()
        val trimmedPass = pass.trim()
        if (trimmedEmail.isEmpty() || trimmedPass.isEmpty()) {
            _loginError.value = "Please enter both username/email and password"
            return
        }

        _isLoggingIn.value = true
        _loginError.value = null

        viewModelScope.launch {
            val result = ShkoloApi.login(trimmedEmail, trimmedPass)
            result.onSuccess { loginRes ->
                val primaryPupil = loginRes.pupils.firstOrNull()
                val pupilId = primaryPupil?.id ?: 0L
                val name = primaryPupil?.name ?: loginRes.userName
                val cls = primaryPupil?.className ?: ""

                preferences.saveSession(
                    token = loginRes.token,
                    pupilId = pupilId,
                    pupilName = name,
                    className = cls
                )

                _studentName.value = name
                _className.value = cls
                _isLoggedIn.value = true
                _isLoggingIn.value = false

                goToToday()

                if (preferences.loadSettings().isEnabled) {
                    ClassBarService.startOrRefresh(appContext)
                }
            }.onFailure { err ->
                _isLoggingIn.value = false
                _loginError.value = err.message ?: "Authentication failed"
            }
        }
    }

    fun logout() {
        preferences.clearSession()
        _isLoggedIn.value = false
        _studentName.value = ""
        _className.value = ""
        _lessons.value = emptyList()
        _lessonsError.value = null
        ClassBarService.stop(appContext)
    }

    // --- Schedule & Navigation Actions ---

    fun navigateDate(deltaDays: Int) {
        currentCalendar.add(Calendar.DAY_OF_YEAR, deltaDays)
        updateDateStateAndFetch()
    }

    fun goToToday() {
        currentCalendar = Calendar.getInstance()
        updateDateStateAndFetch()
    }

    private fun updateDateStateAndFetch() {
        val dateStr = dateFormatApi.format(currentCalendar.time)
        val displayStr = dateFormatDisplay.format(currentCalendar.time)
        val todayStr = dateFormatApi.format(Date())

        _selectedDateString.value = dateStr
        _selectedDateFormatted.value = displayStr
        _isToday.value = (dateStr == todayStr)

        loadScheduleForDate(dateStr)
    }

    fun refreshSchedule() {
        loadScheduleForDate(_selectedDateString.value, forceRefresh = true)
    }

    private fun loadScheduleForDate(dateStr: String, forceRefresh: Boolean = false) {
        val token = preferences.getSavedToken() ?: return
        val pupilId = preferences.getSavedPupilId()
        if (pupilId <= 0L) return

        // Check local cache first for instant display
        if (!forceRefresh) {
            val cachedJson = preferences.loadScheduleCache(dateStr)
            if (!cachedJson.isNullOrBlank()) {
                val cachedLessons = ShkoloApi.parseLessonsJson(cachedJson)
                _lessons.value = cachedLessons
                _lessonsError.value = null
            }
        }

        _isLoadingLessons.value = true
        _lessonsError.value = null

        viewModelScope.launch {
            val result = ShkoloApi.getSchedule(token, pupilId, dateStr)
            _isLoadingLessons.value = false
            result.onSuccess { lessonList ->
                _lessons.value = lessonList
                _lessonsError.value = null

                // If this is today, update the upcoming lesson room preview
                val todayStr = dateFormatApi.format(Date())
                if (dateStr == todayStr) {
                    val nowTime = SimpleDateFormat("HH:mm", Locale.US).format(Date())
                    val upcoming = ShkoloApi.findUpcomingLesson(lessonList, nowTime)
                    if (upcoming != null) {
                        preferences.setPreviewRoom(upcoming.roomNumber)
                        val s = preferences.loadSettings()
                        _settings.value = s
                        _previewBitmap.value = ClassBarIconGenerator.generatePreviewBitmap(s)
                    }
                    if (preferences.loadSettings().isEnabled) {
                        ClassBarService.startOrRefresh(appContext)
                    }
                }
            }.onFailure { err ->
                if (err is UnauthorizedException) {
                    logout()
                    _loginError.value = "Your session has expired. Please sign in again."
                } else if (_lessons.value.isEmpty()) {
                    _lessonsError.value = err.message ?: "Failed to load schedule"
                }
            }
        }
    }

    // --- Icon Settings Actions ---

    fun setServiceEnabled(enabled: Boolean) {
        preferences.setEnabled(enabled)
        _settings.value = preferences.loadSettings()
        _previewBitmap.value = ClassBarIconGenerator.generatePreviewBitmap(_settings.value)
        if (enabled) {
            ClassBarService.startOrRefresh(appContext)
        } else {
            ClassBarService.stop(appContext)
        }
    }

    fun setFontSize(size: Float) {
        preferences.setFontSize(size)
        _settings.value = preferences.loadSettings()
        _previewBitmap.value = ClassBarIconGenerator.generatePreviewBitmap(_settings.value)
    }

    fun setYPosition(y: Float) {
        preferences.setYPosition(y)
        _settings.value = preferences.loadSettings()
        _previewBitmap.value = ClassBarIconGenerator.generatePreviewBitmap(_settings.value)
    }

    fun setTextScaleX(scale: Float) {
        preferences.setTextScaleX(scale)
        _settings.value = preferences.loadSettings()
        _previewBitmap.value = ClassBarIconGenerator.generatePreviewBitmap(_settings.value)
    }

    fun setBoldness(boldness: Float) {
        preferences.setBoldness(boldness)
        _settings.value = preferences.loadSettings()
        _previewBitmap.value = ClassBarIconGenerator.generatePreviewBitmap(_settings.value)
    }

    fun setPreviewRoom(room: String) {
        preferences.setPreviewRoom(room)
        _settings.value = preferences.loadSettings()
        _previewBitmap.value = ClassBarIconGenerator.generatePreviewBitmap(_settings.value)
    }

    fun resetPreviewRoom() {
        val nowTime = SimpleDateFormat("HH:mm", Locale.US).format(Date())
        val upcoming = ShkoloApi.findUpcomingLesson(_lessons.value, nowTime)
        val defaultRoom = upcoming?.roomNumber ?: AppPreferences.DEFAULT_PREVIEW_ROOM
        setPreviewRoom(defaultRoom)
    }

    fun setCountdownEnabled(enabled: Boolean) {
        preferences.setCountdownEnabled(enabled)
        _settings.value = preferences.loadSettings()
        if (_settings.value.isEnabled) {
            ClassBarService.startOrRefresh(appContext)
        }
    }

    fun resetScalingDefaults() {
        preferences.resetDefaults()
        _settings.value = preferences.loadSettings()
        _previewBitmap.value = ClassBarIconGenerator.generatePreviewBitmap(_settings.value)
    }

    fun setPriorityMode(mode: PriorityMode) {
        preferences.setPriorityMode(mode)
        _settings.value = preferences.loadSettings()
    }

    fun setTapAction(action: TapAction) {
        preferences.setTapAction(action)
        _settings.value = preferences.loadSettings()
    }

    fun setLauncherIconStyle(style: LauncherIconStyle) {
        preferences.setLauncherIconStyle(style)
        _settings.value = preferences.loadSettings()
    }

    class Factory(private val context: Context) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return MainViewModel(context.applicationContext) as T
        }
    }
}
