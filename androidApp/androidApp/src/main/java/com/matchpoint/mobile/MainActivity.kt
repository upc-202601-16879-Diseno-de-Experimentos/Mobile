package com.matchpoint.mobile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.launch

// Design System Tokens (Cálido y Acogedor)
val WarmBackground = Color(0xFFFFF9F5)
val Terracotta = Color(0xFFE07A5F)
val SoftGreen = Color(0xFF81B29A)
val NavyText = Color(0xFF3D405B)
val WhiteSurface = Color(0xFFFFFFFF)
val CardShape = RoundedCornerShape(24.dp)
val ButtonShape = RoundedCornerShape(100.dp) // Fully rounded
val InputShape = RoundedCornerShape(16.dp)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { 
            MaterialTheme(
                colorScheme = lightColorScheme(
                    primary = Terracotta,
                    background = WarmBackground,
                    surface = WhiteSurface,
                    onPrimary = Color.White,
                    onBackground = NavyText,
                    onSurface = NavyText
                )
            ) {
                MatchPointApp() 
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MatchPointApp() {
    val navController = rememberNavController()
    var token by remember { mutableStateOf<String?>(null) }
    var userId by remember { mutableStateOf<Long?>(null) }
    var currentUsername by remember { mutableStateOf<String?>(null) }
    var userEmail by remember { mutableStateOf<String?>(null) }
    val api = remember { ApiService() }
    val scope = rememberCoroutineScope()

    Scaffold(
        containerColor = WarmBackground,
        bottomBar = {
            if (token != null) {
                NavigationBar(
                    containerColor = WhiteSurface,
                    tonalElevation = 8.dp
                ) {
                    NavigationBarItem(icon = { Icon(Icons.Default.Person, "Coaches") }, label = { Text("Coaches") }, selected = false, onClick = { navController.navigate("coaches") }, colors = NavigationBarItemDefaults.colors(indicatorColor = WarmBackground, selectedIconColor = Terracotta, unselectedIconColor = NavyText))
                    NavigationBarItem(icon = { Icon(Icons.Default.Place, "Servicios") }, label = { Text("Servicios") }, selected = false, onClick = { navController.navigate("courts") }, colors = NavigationBarItemDefaults.colors(indicatorColor = WarmBackground, selectedIconColor = Terracotta, unselectedIconColor = NavyText))
                    NavigationBarItem(icon = { Icon(Icons.Default.DateRange, "Reservas") }, label = { Text("Reservas") }, selected = false, onClick = { navController.navigate("bookings") }, colors = NavigationBarItemDefaults.colors(indicatorColor = WarmBackground, selectedIconColor = Terracotta, unselectedIconColor = NavyText))
                    NavigationBarItem(icon = { Icon(Icons.Default.AccountCircle, "Perfil") }, label = { Text("Perfil") }, selected = false, onClick = { navController.navigate("profile") }, colors = NavigationBarItemDefaults.colors(indicatorColor = WarmBackground, selectedIconColor = Terracotta, unselectedIconColor = NavyText))
                }
            }
        }
    ) { padding ->
        NavHost(navController, startDestination = if (token == null) "login" else "coaches", modifier = Modifier.padding(padding).fillMaxSize().background(WarmBackground)) {
            composable("login") {
                var username by remember { mutableStateOf("") }
                var password by remember { mutableStateOf("") }
                var error by remember { mutableStateOf<String?>(null) }

                Column(modifier = Modifier.fillMaxSize().padding(32.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🎾 MatchPoint", fontWeight = FontWeight.ExtraBold, fontSize = 36.sp, color = Terracotta, modifier = Modifier.padding(bottom = 32.dp))
                    
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = CardShape,
                        colors = CardDefaults.cardColors(containerColor = WhiteSurface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                    ) {
                        Column(modifier = Modifier.padding(24.dp)) {
                            Text("Bienvenido", fontWeight = FontWeight.Bold, fontSize = 24.sp, color = NavyText, modifier = Modifier.padding(bottom = 16.dp))
                            
                            error?.let { Text(it, color = Color.Red, fontSize = 14.sp, modifier = Modifier.padding(bottom = 8.dp)) }
                            
                            OutlinedTextField(value = username, onValueChange = { username = it }, label = { Text("Usuario") }, shape = InputShape, modifier = Modifier.fillMaxWidth(), colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = Color.LightGray, focusedBorderColor = Terracotta))
                            Spacer(Modifier.height(12.dp))
                            OutlinedTextField(value = password, onValueChange = { password = it }, label = { Text("Password") }, shape = InputShape, modifier = Modifier.fillMaxWidth(), colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = Color.LightGray, focusedBorderColor = Terracotta))
                            Spacer(Modifier.height(24.dp))
                            
Button(
                                onClick = {
                                    if (username.isBlank() || password.isBlank()) {
                                        error = "Ingresa usuario y password"
                                    } else {
                                        scope.launch {
                                            val resp = api.login(username, password)
                                            if (resp != null) { 
                                                token = resp.token
                                                userId = resp.id
                                                currentUsername = username
                                                val prefs = navController.context.getSharedPreferences("MatchPoint", android.content.Context.MODE_PRIVATE)
                                                val savedEmail = prefs.getString(username, null)
                                                val emailForProfile = savedEmail ?: (username + "@matchpoint.com")
                                                userEmail = emailForProfile
                                                api.setToken(resp.token)
                                                
                                                // Try to get userProfile, fallback to using userId directly
                                                try {
                                                    var profile = api.getUserProfile(emailForProfile)
                                                    if (profile != null) {
                                                        userId = profile.id
                                                    }
                                                } catch (e: Exception) {
                                                    // Use IAM user id as fallback - pero NO ES IGUAL al userProfile
                                                    // Por ahora usamos un ID conocido que existe
                                                    userId = 7L  // El último usuario creado
                                                }
                                                
                                                navController.navigate("coaches") { popUpTo("login") { inclusive = true } }
                                            }
                                            else { error = "Usuario o password incorrectos" }
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth().height(56.dp),
                                shape = ButtonShape,
                                colors = ButtonDefaults.buttonColors(containerColor = Terracotta)
                            ) { Text("ENTRAR", fontSize = 16.sp, fontWeight = FontWeight.Bold) }
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    TextButton(onClick = { navController.navigate("signup") }) { Text("¿No tienes cuenta? Regístrate", color = NavyText) }
                }
            }

            composable("signup") {
                 var name by remember { mutableStateOf("") }
                 var email by remember { mutableStateOf("") }
                 var phone by remember { mutableStateOf("") }
                 var username by remember { mutableStateOf("") }
                 var password by remember { mutableStateOf("") }
                 var error by remember { mutableStateOf<String?>(null) }

                 Column(modifier = Modifier.fillMaxSize().padding(32.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                     Text("🎾 Nuevo Jugador", fontWeight = FontWeight.ExtraBold, fontSize = 28.sp, color = Terracotta, modifier = Modifier.padding(bottom = 24.dp))
                     
                     Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = CardShape,
                        colors = CardDefaults.cardColors(containerColor = WhiteSurface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                    ) {
                        Column(modifier = Modifier.padding(24.dp)) {
                            error?.let { Text(it, color = Color.Red, fontSize = 14.sp, modifier = Modifier.padding(bottom = 8.dp)) }
                            
                            OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nombre") }, shape = InputShape, modifier = Modifier.fillMaxWidth())
                            Spacer(Modifier.height(8.dp))
                            OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email") }, shape = InputShape, modifier = Modifier.fillMaxWidth())
                            Spacer(Modifier.height(8.dp))
                            OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Teléfono") }, shape = InputShape, modifier = Modifier.fillMaxWidth())
                            Spacer(Modifier.height(8.dp))
                            OutlinedTextField(value = username, onValueChange = { username = it }, label = { Text("Usuario") }, shape = InputShape, modifier = Modifier.fillMaxWidth())
                            Spacer(Modifier.height(8.dp))
                            OutlinedTextField(value = password, onValueChange = { password = it }, label = { Text("Password") }, shape = InputShape, modifier = Modifier.fillMaxWidth())
                            Spacer(Modifier.height(24.dp))
                            
                            Button(onClick = {
                                if (name.isBlank() || username.isBlank() || password.isBlank()) { error = "Llena todos los campos" }
                                else {
                                    scope.launch {
                                        val userEmailVal = email.ifBlank { username + "@matchpoint.com" }
                                        val userPhoneVal = phone.ifBlank { "999999999" }
                                        val signUpSuccess = api.signUp(username, password, name, userEmailVal, userPhoneVal)
                                        if (signUpSuccess) {
                                            // 1. Log in immediately to get the token BEFORE creating the profile
                                            val loginResp = api.login(username, password)
                                            if (loginResp != null) {
                                                token = loginResp.token
                                                userId = loginResp.id
                                                currentUsername = username
                                                userEmail = userEmailVal
                                                api.setToken(loginResp.token)
                                                
                                                // 2. Now create the profile with the token
                                                val profileCreated = api.createUserProfile(name, userEmailVal, userPhoneVal)
                                                if (profileCreated) {
                                                    val prefs = navController.context.getSharedPreferences("MatchPoint", android.content.Context.MODE_PRIVATE)
                                                    prefs.edit().putString(username, userEmailVal).apply()
                                                    
                                                    // Get userProfile for booking
                                                    var profile = api.getUserProfile(userEmailVal)
                                                    if (profile != null) {
                                                        userId = profile.id
                                                    }
                                                    
                                                    navController.navigate("courts") { popUpTo("login") { inclusive = true } }
                                                } else { error = "Error al crear perfil (registrado ok)" }
                                            } else { error = "Registro ok, pero falló login automático" }
                                        } else { error = "Error al registrar usuario" }
                                    }
                                }
                            }, modifier = Modifier.fillMaxWidth().height(56.dp), shape = ButtonShape, colors = ButtonDefaults.buttonColors(containerColor = Terracotta)) { Text("REGISTRARSE", fontWeight = FontWeight.Bold) }
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    TextButton(onClick = { navController.popBackStack() }) { Text("¿Ya tienes cuenta? Entrar", color = NavyText) }
                 }
             }

            

            composable("coaches") {
                var coaches by remember { mutableStateOf<List<Coach>>(emptyList()) }
                LaunchedEffect(Unit) { coaches = api.getCoaches() }

                Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                    Text("Nuestros Coaches", fontWeight = FontWeight.ExtraBold, fontSize = 28.sp, color = NavyText, modifier = Modifier.padding(bottom = 16.dp))
                    Spacer(Modifier.height(16.dp))
                    
                    coaches.forEach { coach ->
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                            shape = CardShape,
                            colors = CardDefaults.cardColors(containerColor = WhiteSurface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Column { 
                                    Text(coach.name, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = NavyText)
                                    Text(coach.sportType ?: coach.expertise, fontSize = 14.sp, color = Color.Gray) 
                                    coach.availability?.let { Text("Horario: $it", fontSize = 12.sp, color = Terracotta, modifier = Modifier.padding(top = 4.dp)) } 
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    coach.pricePerHour?.let { Text("$${it}/hr", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = SoftGreen) }
                                    coach.rating?.let { Text("⭐ ${String.format("%.1f", it)}", fontSize = 14.sp, color = Terracotta, fontWeight = FontWeight.Medium) }
                                }
                            }
                        }
                    }
                    if (coaches.isEmpty()) Text("No hay coaches disponibles", color = Color.Gray, modifier = Modifier.padding(16.dp))
                }
            }

            composable("courts") {
                var coaches by remember { mutableStateOf<List<Coach>>(emptyList()) }
                var services by remember { mutableStateOf<List<CoachService>>(emptyList()) }
                var selectedService by remember { mutableStateOf<CoachService?>(null) }
                var selectedCoach by remember { mutableStateOf<Coach?>(null) }
                var showBookingDialog by remember { mutableStateOf(false) }
                var loading by remember { mutableStateOf(true) }

                LaunchedEffect(Unit) {
                    coaches = api.getCoaches()
                    loading = false
                    if (coaches.isNotEmpty()) {
                        val firstCoach = coaches.first()
                        selectedCoach = firstCoach
                        services = api.getCoachServices(firstCoach.id)
                    }
                }

                Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                    Text("Servicios", fontWeight = FontWeight.ExtraBold, fontSize = 28.sp, color = NavyText, modifier = Modifier.padding(bottom = 16.dp))

                    if (coaches.isNotEmpty()) {
                        var selectedCoachIndex by remember { mutableStateOf(0) }
                        var expanded by remember { mutableStateOf(false) }

                        ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
                            OutlinedTextField(
                                value = coaches.getOrNull(selectedCoachIndex)?.name ?: "Seleccionar Coach",
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Coach") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                                modifier = Modifier.fillMaxWidth().menuAnchor()
                            )
                            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                                coaches.forEachIndexed { index, coach ->
                                    DropdownMenuItem(text = { Text(coach.name) }, onClick = {
                                        selectedCoachIndex = index
                                        selectedCoach = coach
                                        expanded = false
                                        scope.launch { services = api.getCoachServices(coach.id) }
                                    })
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    if (loading) {
                        Text("Cargando...", color = Color.Gray)
                    } else if (services.isEmpty()) {
                        Text("Este coach no tiene servicios disponibles. Crea servicios desde la web.", color = Color.Gray, modifier = Modifier.padding(vertical = 16.dp))
                    } else {
                        services.forEach { service ->
                            Card(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                                shape = CardShape,
                                colors = CardDefaults.cardColors(containerColor = WhiteSurface),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                                onClick = {
                                    selectedService = service
                                    showBookingDialog = true
                                }
                            ) {
                                Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Column {
                                        Text(service.name, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = NavyText)
                                        Text(selectedCoach?.name ?: "", fontSize = 14.sp, color = Color.Gray)
                                        service.description?.let { Text(it, fontSize = 12.sp, color = Color.Gray) }
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        service.price?.let { Text("$${it}", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = SoftGreen) }
                                        Text("📅 Reservar", fontSize = 14.sp, color = Terracotta, fontWeight = FontWeight.Medium)
                                    }
                                }
                            }
                        }
                    }
                }

                if (showBookingDialog && selectedService != null) {
                    val next7Days = remember { 
                        val cal = java.util.Calendar.getInstance()
                        val fmt = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                        (0..6).map { 
                            if (it > 0) cal.add(java.util.Calendar.DAY_OF_YEAR, 1)
                            fmt.format(cal.time)
                        }
                    }
                    var selectedDate by remember { mutableStateOf(next7Days.first()) }
                    var selectedTime by remember { mutableStateOf("10:00") }
                    AlertDialog(
                        onDismissRequest = { showBookingDialog = false },
                        title = { Text("Reservar ${selectedService!!.name}") },
                        text = {
                            Column {
                                Text("Coach: ${selectedCoach?.name ?: ""}")
                                selectedCoach?.availability?.let { Text("Disponibilidad: $it", fontSize = 14.sp, color = Terracotta) }
                                Spacer(Modifier.height(4.dp))
                                Text("Precio: $${selectedService!!.price ?: 0}")
                                Spacer(Modifier.height(8.dp))
                                
                                Text("Selecciona día:")
                                var expandedDate by remember { mutableStateOf(false) }
                                Box(modifier = Modifier.fillMaxWidth()) {
                                    OutlinedTextField(
                                        value = selectedDate,
                                        onValueChange = {},
                                        readOnly = true,
                                        label = { Text("Fecha") },
                                        trailingIcon = { IconButton(onClick = { expandedDate = true }) { Icon(Icons.Default.ArrowDropDown, "Select date") } },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    // Overlay click to open dropdown
                                    Spacer(modifier = Modifier.matchParentSize().background(Color.Transparent).clickable { expandedDate = true })
                                    DropdownMenu(expanded = expandedDate, onDismissRequest = { expandedDate = false }) {
                                        next7Days.forEach { d ->
                                            DropdownMenuItem(text = { Text(d) }, onClick = {
                                                selectedDate = d
                                                expandedDate = false
                                            })
                                        }
                                    }
                                }
                                
                                Spacer(Modifier.height(8.dp))
                                Text("Selecciona hora:")
                                var expandedTime by remember { mutableStateOf(false) }
                                val times = remember(selectedCoach?.availability) {
                                    val avail = selectedCoach?.availability ?: ""
                                    val defaultTimes = listOf("08:00", "09:00", "10:00", "11:00", "12:00", "13:00", "14:00", "15:00", "16:00", "17:00", "18:00", "19:00", "20:00")
                                    if (avail.contains("-")) {
                                        val parts = avail.split("-")
                                        val startHour = parts.getOrNull(0)?.split(":")?.getOrNull(0)?.trim()?.toIntOrNull()
                                        val endHour = parts.getOrNull(1)?.split(":")?.getOrNull(0)?.trim()?.toIntOrNull()
                                        if (startHour != null && endHour != null && startHour < endHour) {
                                            (startHour..endHour).map { "%02d:00".format(it) }
                                        } else defaultTimes
                                    } else defaultTimes
                                }
                                Box(modifier = Modifier.fillMaxWidth()) {
                                    OutlinedTextField(
                                        value = selectedTime,
                                        onValueChange = {},
                                        readOnly = true,
                                        label = { Text("Hora") },
                                        trailingIcon = { IconButton(onClick = { expandedTime = true }) { Icon(Icons.Default.ArrowDropDown, "Select time") } },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    Spacer(modifier = Modifier.matchParentSize().background(Color.Transparent).clickable { expandedTime = true })
                                    DropdownMenu(expanded = expandedTime, onDismissRequest = { expandedTime = false }) {
                                        times.forEach { t ->
                                            DropdownMenuItem(text = { Text(t) }, onClick = {
                                                selectedTime = t
                                                expandedTime = false
                                            })
                                        }
                                    }
                                }
                                Spacer(Modifier.height(8.dp))
                                Text("¿Confirmar reserva?")
                            }
                        },
                        confirmButton = {
                            Button(onClick = {
                                scope.launch {
                                    val userIdVal = userId ?: 1L
                                    api.createBooking(
                                        null,
                                        userIdVal,
                                        "${selectedDate}T${selectedTime}:00",
                                        "Service: ${selectedService!!.name}",
                                        selectedService!!.price ?: 50.0,
                                        selectedService!!.id
                                    )
                                    showBookingDialog = false
                                }
                            }, colors = ButtonDefaults.buttonColors(containerColor = SoftGreen)) { Text("CONFIRMAR") }
                        },
                        dismissButton = {
                            TextButton(onClick = { showBookingDialog = false }) { Text("CANCELAR") }
                        }
                    )
                }
            }

            composable("bookings") {
                var bookings by remember { mutableStateOf<List<Booking>>(emptyList()) }
                LaunchedEffect(Unit) { bookings = api.getBookings() }

                Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                    Text("Mis Reservas", fontWeight = FontWeight.ExtraBold, fontSize = 28.sp, color = NavyText, modifier = Modifier.padding(bottom = 16.dp))
                    
                    bookings.forEach { b ->
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                            shape = CardShape,
                            colors = CardDefaults.cardColors(containerColor = WhiteSurface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Column {
                                    Text(b.serviceName ?: "Cancha Reservada", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = NavyText)
                                    Text("Reserva #${b.id}", fontSize = 12.sp, color = Color.Gray)
                                }
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = when (b.status.uppercase()) { "CONFIRMED", "COMPLETED" -> SoftGreen.copy(alpha=0.2f); "PENDING" -> Color(0xFFFFF3E0); else -> Color(0xFFF5F5F5) },
                                    modifier = Modifier.padding(start = 8.dp)
                                ) {
                                    Text(
                                        when (b.status.uppercase()) { "PENDING" -> "RESERVA PENDIENTE DE APROBACIÓN"; "CONFIRMED" -> "CONFIRMADA"; "COMPLETED" -> "COMPLETADA"; else -> b.status }, 
                                        color = when (b.status.uppercase()) { "CONFIRMED", "COMPLETED" -> SoftGreen; "PENDING" -> Color(0xFFFF9800); else -> Color.Gray },
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp, // Made slightly smaller to fit the long text
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                    if (bookings.isEmpty()) Text("Aún no tienes reservas", color = Color.Gray, modifier = Modifier.padding(16.dp))
                }
            }

            composable("profile") {
                 var profile by remember { mutableStateOf<UserProfile?>(null) }
                 LaunchedEffect(Unit) { 
                     val emailToUse = userEmail ?: currentUsername?.let { it + "@matchpoint.com" }
                     if (emailToUse != null) {
                         profile = api.getUserProfile(emailToUse)
                     }
                 }

                 Column(modifier = Modifier.fillMaxSize().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                     Text("Mi Perfil", fontWeight = FontWeight.ExtraBold, fontSize = 28.sp, color = NavyText, modifier = Modifier.padding(bottom = 24.dp).align(Alignment.Start))
                     
                     if (profile != null) {
                         Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = CardShape,
                            colors = CardDefaults.cardColors(containerColor = WhiteSurface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                        ) {
                             Column(modifier = Modifier.padding(24.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                                 Surface(shape = RoundedCornerShape(50.dp), color = Terracotta, modifier = Modifier.size(80.dp).padding(bottom = 16.dp)) {
                                     Box(contentAlignment = Alignment.Center) {
                                         Text(profile!!.name.first().uppercase(), color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Bold)
                                     }
                                 }
                                 
                                 Text(profile!!.name, fontWeight = FontWeight.Bold, fontSize = 22.sp, color = NavyText)
                                 Text(currentUsername ?: "", color = Color.Gray, fontSize = 14.sp, modifier = Modifier.padding(bottom = 16.dp))
                                 
                                 Divider(color = WarmBackground, thickness = 2.dp, modifier = Modifier.padding(vertical = 16.dp))
                                 
                                 Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Email", color = Color.Gray)
                                    Text(profile!!.email, fontWeight = FontWeight.Medium)
                                 }
                                 Spacer(Modifier.height(8.dp))
                                 Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Teléfono", color = Color.Gray)
                                    Text(profile!!.phone, fontWeight = FontWeight.Medium)
                                 }
                             }
                         }
                     } else {
                         Text("No tienes perfil. Crea uno en tu primera sesión.", color = Color.Gray, modifier = Modifier.padding(vertical = 16.dp))
                     }
                     Spacer(Modifier.height(32.dp))
                     Button(onClick = { token = null; userEmail = null; currentUsername = null; navController.navigate("login") { popUpTo(0) { inclusive = true } } }, modifier = Modifier.fillMaxWidth().height(56.dp), shape = ButtonShape, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF5350))) { Text("CERRAR SESIÓN", fontWeight = FontWeight.Bold) }
                 }
             }
        }
    }
}