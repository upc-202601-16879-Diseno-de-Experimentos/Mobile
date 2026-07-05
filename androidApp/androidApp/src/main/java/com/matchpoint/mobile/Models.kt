package com.matchpoint.mobile

data class Court(
    val id: Long,
    val name: String,
    val location: String,
    val type: String,
    val sportType: String?,
    val pricePerHour: Double?,
    val description: String?,
    val imageUrl: String?,
    val isAvailable: Boolean?,
    val openingHours: String?,
    val phone: String?,
    val address: String?,
    val latitude: Double?,
    val longitude: Double?
)

data class Coach(
    val id: Long,
    val name: String,
    val expertise: String,
    val phone: String,
    val email: String?,
    val sportType: String?,
    val pricePerHour: Double?,
    val location: String?,
    val description: String?,
    val imageUrl: String?,
    val rating: Double?,
    val totalReviews: Int?,
    val isAvailable: Boolean?,
    val availability: String?,
    val experienceYears: Int?
)

data class Booking(
    val id: Long,
    val startTime: String,
    val endTime: String,
    val user: UserSummary?,
    val court: CourtSummary?,
    val coach: CoachSummary?,
    val status: String,
    val totalPrice: Double?,
    val serviceName: String?,
    val notes: String?
) {
    data class UserSummary(val id: Long, val name: String?, val email: String?, val phone: String?)
    data class CourtSummary(val id: Long, val name: String?, val location: String?, val sportType: String?)
    data class CoachSummary(val id: Long, val name: String?, val sportType: String?)
}

data class UserProfile(
    val id: Long,
    val name: String,
    val email: String,
    val phone: String,
    val address: String?,
    val profileImageUrl: String?,
    val dateOfBirth: String?,
    val favoriteSports: String?
)

data class LoginRequest(val username: String, val password: String)
data class LoginResponse(val id: Long, val username: String, val token: String, val roles: List<String>? = null)
data class SignUpRequest(val username: String, val password: String, val name: String, val email: String, val phone: String)
data class CoachService(
    val id: Long,
    val name: String,
    val description: String?,
    val price: Double?,
    val coachId: Long?
)

data class Review(
    val id: Long,
    val coachId: Long,
    val userProfileId: Long,
    val rating: Int,
    val comment: String?,
    val createdAt: String?
)