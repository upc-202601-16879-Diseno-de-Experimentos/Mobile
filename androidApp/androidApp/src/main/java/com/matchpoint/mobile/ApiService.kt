package com.matchpoint.mobile

import org.json.JSONArray
import org.json.JSONObject

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

class ApiService(private val baseUrl: String = "https://matchpoint-api-production-9e17.up.railway.app/api/v1") {
    private val client = OkHttpClient()
    private var token: String? = null

    fun setToken(newToken: String?) { token = newToken }

    private fun makeRequest(endpoint: String, method: String = "GET", body: String? = null): String? {
        return try {
            val requestBuilder = Request.Builder().url("$baseUrl$endpoint")
            
            if (method != "GET") {
                requestBuilder.method(method, body?.toRequestBody("application/json".toMediaType()))
            }
            
            token?.let { requestBuilder.addHeader("Authorization", "Bearer $it") }
            
            val response = client.newCall(requestBuilder.build()).execute()
            response.body?.string()
        } catch (e: IOException) {
            null
        }
    }

    suspend fun login(username: String, password: String): LoginResponse? = withContext(Dispatchers.IO) {
        val json = """{"username":"$username","password":"$password"}"""
        val response = makeRequest("/authentication/sign-in", "POST", json)
        response?.let {
            try {
                val jsonObject = JSONObject(it)
                val token = jsonObject.optString("token", null)
                val id = jsonObject.optLong("id", 0L)
                val user = jsonObject.optString("username", "")
                
                val roles = mutableListOf<String>()
                val rolesArray = jsonObject.optJSONArray("roles")
                if (rolesArray != null) {
                    for (i in 0 until rolesArray.length()) {
                        roles.add(rolesArray.getString(i))
                    }
                }
                
                if (token != null) LoginResponse(id, user, token, roles) else null
            } catch (e: Exception) { 
                println("Login parse error: $e")
                null 
            }
        }
    }

    suspend fun signUp(username: String, password: String, name: String, email: String, phone: String): Boolean = withContext(Dispatchers.IO) {
        val json = JSONObject().apply {
            put("username", username)
            put("password", password)
            put("name", name)
            put("email", email)
            put("phone", phone)
            put("roles", JSONArray().put("ROLE_USER"))
        }.toString()
        val response = makeRequest("/authentication/sign-up", "POST", json)
        response != null && response.contains("id")
    }

    suspend fun getCourts(): List<Court> = withContext(Dispatchers.IO) {
        val response = makeRequest("/courts")
        parseCourts(response ?: "[]")
    }

    suspend fun searchCourts(sportType: String?, location: String?, maxPrice: Double?): List<Court> = withContext(Dispatchers.IO) {
        var endpoint = "/courts/search?"
        sportType?.let { endpoint += "sportType=$it&" }
        location?.let { endpoint += "location=$it&" }
        maxPrice?.let { endpoint += "maxPrice=$it" }
        val response = makeRequest(endpoint)
        parseCourts(response ?: "[]")
    }

    suspend fun getCoaches(): List<Coach> = withContext(Dispatchers.IO) {
        val response = makeRequest("/coaches")
        parseCoaches(response ?: "[]")
    }

    suspend fun searchCoaches(sportType: String?, location: String?, maxPrice: Double?, minRating: Double?): List<Coach> = withContext(Dispatchers.IO) {
        var endpoint = "/coaches/search?"
        sportType?.let { endpoint += "sportType=$it&" }
        location?.let { endpoint += "location=$it&" }
        maxPrice?.let { endpoint += "maxPrice=$it&" }
        minRating?.let { endpoint += "minRating=$it" }
        val response = makeRequest(endpoint)
        parseCoaches(response ?: "[]")
    }

    suspend fun getCoachServices(coachId: Long): List<CoachService> = withContext(Dispatchers.IO) {
        val response = makeRequest("/coaches/$coachId/services")
        parseCoachServices(response ?: "[]")
    }

    suspend fun getBookings(): List<Booking> = withContext(Dispatchers.IO) {
        val response = makeRequest("/bookings")
        parseBookings(response ?: "[]")
    }

    suspend fun getUserProfile(email: String): UserProfile? = withContext(Dispatchers.IO) {
        val response = makeRequest("/user-profiles")
        if (response == null || !response.startsWith("[")) return@withContext null
        try {
            val array = JSONArray(response)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                if (obj.optString("email") == email) {
                    return@withContext UserProfile(
                        id = obj.optLong("id"),
                        name = obj.optString("name"),
                        email = obj.optString("email"),
                        phone = obj.optString("phone"),
                        address = null,
                        profileImageUrl = null,
                        dateOfBirth = null,
                        favoriteSports = null
                    )
                }
            }
        } catch (e: Exception) { e.printStackTrace() }
        null
    }

    suspend fun createUserProfile(name: String, email: String, phone: String): Boolean = withContext(Dispatchers.IO) {
        val json = """{"name":"$name","email":"$email","phone":"$phone"}"""
        val response = makeRequest("/user-profiles", "POST", json)
        response != null && response.contains("id")
    }
    suspend fun createBooking(
        courtId: Long?,
        userProfileId: Long,
        schedule: String,
        notes: String,
        totalPrice: Double,
        coachServiceId: Long? = null
    ): Boolean = withContext(Dispatchers.IO) {
        
        var courtIdPart = "null"
        if (courtId != null) {
            courtIdPart = "$courtId"
        }
        var coachIdPart = ""
        if (coachServiceId != null) {
            coachIdPart = "," + "\"coachServiceId\":$coachServiceId"
        }
        val json = """{"courtId":$courtIdPart,"userId":$userProfileId,"startTime":"$schedule","endTime":"$schedule"$coachIdPart}"""
        val response = makeRequest("/bookings", "POST", json)
        response != null && response.contains("id")
    }

    private fun parseCourts(json: String): List<Court> {
        val courts = mutableListOf<Court>()
        if (!json.contains("[")) return courts
        val items = json.split("},{").map { if (it.startsWith("[")) it.substring(1) else it }
        for (item in items) {
            try {
                val id = item.split("\"id\":").getOrNull(1)?.split(",")?.getOrNull(0)?.toLongOrNull() ?: continue
                val name = item.split("\"name\":\"").getOrNull(1)?.split("\"")?.getOrNull(0) ?: ""
                val location = item.split("\"location\":\"").getOrNull(1)?.split("\"")?.getOrNull(0) ?: ""
                val type = item.split("\"type\":\"").getOrNull(1)?.split("\"")?.getOrNull(0) ?: ""
                val sportType = item.split("\"sportType\":\"").getOrNull(1)?.split("\"")?.getOrNull(0)
                val price = item.split("\"pricePerHour\":").getOrNull(1)?.split(",")?.getOrNull(0)?.toDoubleOrNull()
                val desc = item.split("\"description\":\"").getOrNull(1)?.split("\"")?.getOrNull(0)
                val img = item.split("\"imageUrl\":\"").getOrNull(1)?.split("\"")?.getOrNull(0)
                val avail = item.split("\"isAvailable\":").getOrNull(1)?.split(",")?.getOrNull(0)?.toBooleanStrictOrNull()
                val hours = item.split("\"openingHours\":\"").getOrNull(1)?.split("\"")?.getOrNull(0)
                val phone = item.split("\"phone\":\"").getOrNull(1)?.split("\"")?.getOrNull(0)
                val address = item.split("\"address\":\"").getOrNull(1)?.split("\"")?.getOrNull(0)
                courts.add(Court(id, name, location, type, sportType, price, desc, img, avail, hours, phone, address))
            } catch (e: Exception) { }
        }
        return courts
    }

    private fun parseCoaches(json: String): List<Coach> {
        val coaches = mutableListOf<Coach>()
        if (!json.startsWith("[")) return coaches
        try {
            val array = JSONArray(json)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                coaches.add(Coach(
                    id = obj.optLong("id"),
                    name = obj.optString("name"),
                    expertise = obj.optString("expertise"),
                    phone = obj.optString("phone"),
                    email = if (obj.has("email") && !obj.isNull("email")) obj.getString("email") else null,
                    sportType = if (obj.has("sportType") && !obj.isNull("sportType")) obj.getString("sportType") else null,
                    pricePerHour = if (obj.has("pricePerHour") && !obj.isNull("pricePerHour")) obj.getDouble("pricePerHour") else null,
                    location = if (obj.has("location") && !obj.isNull("location")) obj.getString("location") else null,
                    description = if (obj.has("description") && !obj.isNull("description")) obj.getString("description") else null,
                    imageUrl = if (obj.has("imageUrl") && !obj.isNull("imageUrl")) obj.getString("imageUrl") else null,
                    rating = if (obj.has("rating") && !obj.isNull("rating")) obj.getDouble("rating") else null,
                    totalReviews = if (obj.has("totalReviews") && !obj.isNull("totalReviews")) obj.getInt("totalReviews") else null,
                    isAvailable = if (obj.has("isAvailable") && !obj.isNull("isAvailable")) obj.getBoolean("isAvailable") else null,
                    availability = if (obj.has("availability") && !obj.isNull("availability")) obj.getString("availability") else null,
                    experienceYears = if (obj.has("experienceYears") && !obj.isNull("experienceYears")) obj.getInt("experienceYears") else null
                ))
            }
        } catch (e: Exception) { e.printStackTrace() }
        return coaches
    }

    private fun parseCoachServices(json: String): List<CoachService> {
        val services = mutableListOf<CoachService>()
        if (!json.startsWith("[")) return services
        try {
            val array = JSONArray(json)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                services.add(CoachService(
                    id = obj.optLong("id"),
                    name = obj.optString("name"),
                    description = if (obj.has("description") && !obj.isNull("description")) obj.getString("description") else null,
                    price = if (obj.has("price") && !obj.isNull("price")) obj.getDouble("price") else null,
                    coachId = if (obj.has("coachId") && !obj.isNull("coachId")) obj.getLong("coachId") else null
                ))
            }
        } catch (e: Exception) { e.printStackTrace() }
        return services
    }

    private fun parseBookings(json: String): List<Booking> {
        val bookings = mutableListOf<Booking>()
        if (!json.startsWith("[")) return bookings
        try {
            val array = JSONArray(json)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                
                val userObj = obj.optJSONObject("user")
                val user = if (userObj != null) Booking.UserSummary(userObj.optLong("id"), userObj.optString("name"), null, null) else null
                
                val courtObj = obj.optJSONObject("court")
                val court = if (courtObj != null) Booking.CourtSummary(courtObj.optLong("id"), courtObj.optString("name"), null, null) else null
                
                val coachServiceObj = obj.optJSONObject("coachService")
                val serviceName = coachServiceObj?.optString("name")
                val price = obj.optDouble("amount", 0.0)
                
                bookings.add(Booking(
                    id = obj.optLong("id"),
                    startTime = obj.optString("startTime"),
                    endTime = obj.optString("endTime"),
                    user = user,
                    court = court,
                    coach = null,
                    status = obj.optString("status", "PENDING"),
                    totalPrice = if (price > 0) price else null,
                    serviceName = serviceName,
                    notes = null
                ))
            }
        } catch (e: Exception) { e.printStackTrace() }
        return bookings
    }

    private fun parseUserProfile(json: String): UserProfile? {
        if (json == "null" || json == "{}") return null
        try {
            val id = json.split("\"id\":").getOrNull(1)?.split(",")?.getOrNull(0)?.toLongOrNull() ?: return null
            val name = json.split("\"name\":\"").getOrNull(1)?.split("\"")?.getOrNull(0) ?: ""
            val email = json.split("\"email\":\"").getOrNull(1)?.split("\"")?.getOrNull(0) ?: ""
            val phone = json.split("\"phone\":\"").getOrNull(1)?.split("\"")?.getOrNull(0) ?: ""
            val address = json.split("\"address\":\"").getOrNull(1)?.split("\"")?.getOrNull(0)
            val img = json.split("\"profileImageUrl\":\"").getOrNull(1)?.split("\"")?.getOrNull(0)
            val dob = json.split("\"dateOfBirth\":\"").getOrNull(1)?.split("\"")?.getOrNull(0)
            val fav = json.split("\"favoriteSports\":\"").getOrNull(1)?.split("\"")?.getOrNull(0)
            return UserProfile(id, name, email, phone, address, img, dob, fav)
        } catch (e: Exception) { return null }
    }
}