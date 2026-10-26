package com.thatguyalpha.classbar

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

data class PupilInfo(
    val id: Long,
    val name: String,
    val className: String
)

data class LoginResult(
    val token: String,
    val userId: Long,
    val userName: String,
    val pupils: List<PupilInfo>
)

class UnauthorizedException(message: String = "Session expired or unauthorized (HTTP 401)") : Exception(message)

data class Lesson(
    val hourNumber: Int,
    val fromTime: String,
    val toTime: String,
    val courseName: String,
    val shortCourseName: String,
    val teacherName: String,
    val roomName: String,
    val isFreeHour: Boolean,
    val isTaken: Boolean,
    val group: String?
) {
    val roomNumber: String
        get() = ShkoloApi.extractRoomNumber(roomName)
}

object ShkoloApi {
    private const val BASE_URL = "https://api.shkolo.bg/v1"
    private const val USER_AGENT = "Shkolo-app-Android/1.42.8"
    private const val CONNECT_TIMEOUT_MS = 15_000
    private const val READ_TIMEOUT_MS = 20_000

    /**
     * Extracts pure room number from strings like "307 (Етаж 3)", "Каб. 24", "102".
     * If no digits exist (e.g. "Спортна зала"), returns clean short fallback like "СЗ".
     */
    fun extractRoomNumber(roomName: String): String {
        val trimmed = roomName.trim()
        if (trimmed.isEmpty()) return "—"

        // First look for contiguous numbers: e.g. 307
        val digitMatch = Regex("""\b\d{1,4}[A-Za-zА-Яа-я]?\b""").find(trimmed)
            ?: Regex("""\d+""").find(trimmed)

        if (digitMatch != null) {
            return digitMatch.value
        }

        // Fallback for names without numbers (e.g. "Спортна зала" -> "СЗ")
        val words = trimmed.split(Regex("""\s+""")).filter { it.isNotEmpty() }
        if (words.size > 1) {
            return words.map { it.first().uppercaseChar() }.joinToString("").take(3)
        }
        return trimmed.take(3).uppercase()
    }

    /**
     * Extracts teacher initials without periods (e.g. "Зефир Ников" -> "ЗН", "John Doe" -> "JD")
     */
    fun extractTeacherInitials(teacherName: String): String {
        val trimmed = teacherName.trim()
        if (trimmed.isEmpty()) return ""
        val parts = trimmed.split(Regex("""\s+""")).filter { it.isNotEmpty() }
        if (parts.isEmpty()) return ""
        return parts.map { it.first().uppercaseChar() }.joinToString("")
    }

    /**
     * Finds the active lesson (if currently in class) or the upcoming lesson (if in break/before school).
     * Returns Pair(Lesson, isActiveNow).
     */
    fun findCurrentOrUpcomingLesson(lessons: List<Lesson>, nowTimeStr: String): Pair<Lesson?, Boolean> {
        val sorted = lessons.sortedBy { it.fromTime }
        // First check if currently in a class
        val active = sorted.firstOrNull { nowTimeStr >= it.fromTime && nowTimeStr < it.toTime }
        if (active != null) {
            return Pair(active, true)
        }
        // Next upcoming class
        val upcoming = sorted.firstOrNull { it.toTime > nowTimeStr }
        return Pair(upcoming, false)
    }

    /**
     * Computes minutes remaining until toTime given nowTime (both formatted as "HH:mm").
     */
    fun computeMinutesRemaining(toTimeStr: String, nowTimeStr: String): Int {
        val toParts = toTimeStr.split(":").mapNotNull { it.toIntOrNull() }
        val nowParts = nowTimeStr.split(":").mapNotNull { it.toIntOrNull() }
        if (toParts.size < 2 || nowParts.size < 2) return 0

        val toMinutes = toParts[0] * 60 + toParts[1]
        val nowMinutes = nowParts[0] * 60 + nowParts[1]
        val diff = toMinutes - nowMinutes
        return diff.coerceAtLeast(1)
    }

    /**
     * Finds the active or upcoming lesson given the current time formatted as "HH:mm".
     * A lesson is active or upcoming if its end time (toTime) is greater than now.
     */
    fun findUpcomingLesson(lessons: List<Lesson>, nowTimeStr: String): Lesson? {
        val validLessons = lessons.sortedBy { it.fromTime }
        return validLessons.firstOrNull { it.toTime > nowTimeStr }
    }

    /**
     * Checks if all scheduled lessons for the day have ended.
     */
    fun isDayFinished(lessons: List<Lesson>, nowTimeStr: String): Boolean {
        if (lessons.isEmpty()) return false
        return lessons.all { nowTimeStr >= it.toTime }
    }

    suspend fun login(email: String, pass: String): Result<LoginResult> = withContext(Dispatchers.IO) {
        runCatching {
            val url = URL("$BASE_URL/auth/login")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = CONNECT_TIMEOUT_MS
                readTimeout = READ_TIMEOUT_MS
                doInput = true
                doOutput = true
                setRequestProperty("Accept", "application/json")
                setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
                setRequestProperty("Language", "bg")
                setRequestProperty("User-Agent", USER_AGENT)
            }

            val postData = "email=" + URLEncoder.encode(email, "UTF-8") +
                    "&password=" + URLEncoder.encode(pass, "UTF-8")

            conn.outputStream.use { os ->
                OutputStreamWriter(os, "UTF-8").use { writer ->
                    writer.write(postData)
                    writer.flush()
                }
            }

            val responseCode = conn.responseCode
            val responseText = if (responseCode in 200..299) {
                conn.inputStream.bufferedReader().use(BufferedReader::readText)
            } else {
                conn.errorStream?.bufferedReader()?.use(BufferedReader::readText).orEmpty()
            }

            val json = JSONObject(responseText)
            if (responseCode !in 200..299 || !json.has("api_token")) {
                val errorMsg = json.optString("message", json.optString("error", "Login failed (HTTP $responseCode)"))
                throw Exception(errorMsg)
            }

            val token = json.getString("api_token")
            val userObj = json.optJSONObject("user")
                ?: json.optJSONArray("users")?.optJSONObject(0)
                ?: JSONObject()

            val userId = userObj.optLong("id", 0L)
            val userName = userObj.optString("name", userObj.optString("user_name", "Student"))

            // Fetch pupils for this authenticated user
            val pupils = fetchPupilsInternal(token)

            LoginResult(
                token = token,
                userId = userId,
                userName = userName,
                pupils = pupils
            )
        }
    }

    suspend fun getPupils(token: String): Result<List<PupilInfo>> = withContext(Dispatchers.IO) {
        runCatching {
            fetchPupilsInternal(token)
        }
    }

    private fun fetchPupilsInternal(token: String): List<PupilInfo> {
        val url = URL("$BASE_URL/diary/pupils")
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = CONNECT_TIMEOUT_MS
            readTimeout = READ_TIMEOUT_MS
            setRequestProperty("Accept", "application/json")
            setRequestProperty("Authorization", "Bearer $token")
            setRequestProperty("Language", "bg")
            setRequestProperty("User-Agent", USER_AGENT)
        }

        val responseCode = conn.responseCode
        if (responseCode == 401) {
            throw UnauthorizedException("Session expired or unauthorized (HTTP 401)")
        }
        val responseText = if (responseCode in 200..299) {
            conn.inputStream.bufferedReader().use(BufferedReader::readText)
        } else {
            conn.errorStream?.bufferedReader()?.use(BufferedReader::readText).orEmpty()
        }

        if (responseCode !in 200..299) {
            throw Exception("Failed to fetch pupils (HTTP $responseCode)")
        }

        val pupilsList = mutableListOf<PupilInfo>()
        if (responseText.isBlank()) return pupilsList

        val json = JSONObject(responseText)
        val pupilsArray = json.optJSONArray("childPupils")
            ?: json.optJSONArray("pupils")
            ?: json.optJSONObject("childPupils")?.let { obj ->
                val arr = JSONArray()
                obj.keys().forEach { k -> arr.put(obj.get(k)) }
                arr
            }

        if (pupilsArray != null) {
            for (i in 0 until pupilsArray.length()) {
                val p = pupilsArray.optJSONObject(i) ?: continue
                val id = p.optLong("id", p.optLong("target_id", 0L))
                val name = p.optString("name", p.optString("target_name", "Pupil #$id"))
                val className = p.optString("class_name", "")
                if (id > 0) {
                    pupilsList.add(PupilInfo(id = id, name = name, className = className))
                }
            }
        }

        return pupilsList
    }

    suspend fun getSchedule(
        token: String,
        pupilId: Long,
        dateString: String,
        preferences: AppPreferences? = null
    ): Result<List<Lesson>> =
        withContext(Dispatchers.IO) {
            runCatching {
                val url = URL("$BASE_URL/diary/pupils/$pupilId/scheduleHours?date=$dateString")
                val conn = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = CONNECT_TIMEOUT_MS
                    readTimeout = READ_TIMEOUT_MS
                    setRequestProperty("Accept", "application/json")
                    setRequestProperty("Authorization", "Bearer $token")
                    setRequestProperty("Language", "bg")
                    setRequestProperty("User-Agent", USER_AGENT)
                }

                val responseCode = conn.responseCode
                if (responseCode == 401) {
                    throw UnauthorizedException("Session expired or unauthorized (HTTP 401)")
                }
                val responseText = if (responseCode in 200..299) {
                    conn.inputStream.bufferedReader().use(BufferedReader::readText)
                } else {
                    conn.errorStream?.bufferedReader()?.use(BufferedReader::readText).orEmpty()
                }

                if (responseCode !in 200..299) {
                    throw Exception("Failed to fetch schedule (HTTP $responseCode)")
                }

                if (preferences != null && responseText.isNotBlank()) {
                    preferences.saveScheduleCache(dateString, responseText)
                }

                parseLessonsJson(responseText)
            }
        }

    fun parseLessonsJson(responseText: String): List<Lesson> {
        val list = mutableListOf<Lesson>()
        if (responseText.isBlank()) return list

        val json = JSONObject(responseText)
        val hoursArray = json.optJSONArray("scheduleHours") ?: return list

        for (i in 0 until hoursArray.length()) {
            val h = hoursArray.optJSONObject(i) ?: continue
            val hourNum = h.optInt("school_hour", i + 1)
            val fromTime = h.optString("from_time", h.optString("start_time", "--:--"))
            val toTime = h.optString("to_time", h.optString("end_time", "--:--"))
            val course = h.optString("course_name", h.optString("subject_name", "Lesson"))
            val shortCourse = h.optString("short_course_name", course.take(4))
            val teacher = h.optString("teacher_name", h.optString("teacher", ""))
            val room = h.optString("room_name", h.optString("room", ""))
            val isFree = h.optInt("is_free_hour", 0) == 1
            val isTaken = h.optInt("is_taken", 0) == 1
            val grp = h.optString("group").takeIf { it.isNotBlank() && !it.contains("Обща", ignoreCase = true) }

            list.add(
                Lesson(
                    hourNumber = hourNum,
                    fromTime = fromTime,
                    toTime = toTime,
                    courseName = course,
                    shortCourseName = shortCourse,
                    teacherName = teacher,
                    roomName = room,
                    isFreeHour = isFree,
                    isTaken = isTaken,
                    group = grp
                )
            )
        }

        return list.sortedBy { it.fromTime }
    }
}
