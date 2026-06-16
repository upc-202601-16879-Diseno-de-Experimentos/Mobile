package com.matchpoint.mobile

import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class ApiServiceTest {
    private lateinit var apiService: ApiService

    @Before
    fun setup() {
        apiService = ApiService("http://localhost:8080/api/v1")
    }

    @Test
    fun testAuthenticationAndDataFetching() = runTest {
        // 1. Test Login (Using seeded admin user)
        val loginResponse = apiService.login("admin", "admin")
        assertNotNull("El login falló o el backend no está corriendo", loginResponse)
        assertNotNull("Token no fue recibido", loginResponse?.token)
        
        // 2. Set token
        apiService.setToken(loginResponse!!.token)

        // 3. Test Coaches Fetching
        val coaches = apiService.getCoaches()
        assertNotNull("No se pudo obtener coaches", coaches)
        
        // 4. Test Bookings Fetching
        val bookings = apiService.getBookings()
        assertNotNull("No se pudo obtener reservas", bookings)
        
        println("=== RESUMEN DE PRUEBAS ===")
        println("✅ Login exitoso: ${loginResponse.username} (ID: ${loginResponse.id})")
        println("✅ Coaches recuperados: ${coaches.size}")
        println("✅ Reservas recuperadas: ${bookings.size}")
    }
}
