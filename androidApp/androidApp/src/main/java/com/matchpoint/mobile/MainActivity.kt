package com.matchpoint.mobile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType

// Design System Tokens (Cálido y Acogedor)
val WarmBackground = Color(0xFFFFF9F5)
val Terracotta = Color(0xFFE07A5F)
val SoftGreen = Color(0xFF81B29A)
val NavyText = Color(0xFF3D405B)
val WhiteSurface = Color(0xFFFFFFFF)
val CardShape = RoundedCornerShape(24.dp)
val ButtonShape = RoundedCornerShape(100.dp) // Fully rounded
val InputShape = RoundedCornerShape(16.dp)

fun formatDate(dateStr: String): String {
    return try {
        dateStr.replace("T", " ").substring(0, 16)
    } catch(e: Exception) {
        dateStr
    }
}

fun getDayOfWeekSpanish(dateStr: String): String {
    return try {
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
        val date = sdf.parse(dateStr)
        val cal = java.util.Calendar.getInstance()
        if (date != null) cal.time = date
        when (cal.get(java.util.Calendar.DAY_OF_WEEK)) {
            java.util.Calendar.MONDAY -> "Lunes"
            java.util.Calendar.TUESDAY -> "Martes"
            java.util.Calendar.WEDNESDAY -> "Miercoles"
            java.util.Calendar.THURSDAY -> "Jueves"
            java.util.Calendar.FRIDAY -> "Viernes"
            java.util.Calendar.SATURDAY -> "Sabado"
            java.util.Calendar.SUNDAY -> "Domingo"
            else -> "Lunes"
        }
    } catch (e: Exception) {
        "Lunes"
    }
}

fun formatFriendlyAvailability(availStr: String?): String {
    if (availStr == null) return "Sin disponibilidad"
    val clean = availStr.trim()
    if (clean.startsWith("{")) {
        return try {
            val json = org.json.JSONObject(clean)
            val daysList = listOf("Lunes", "Martes", "Miercoles", "Jueves", "Viernes", "Sabado", "Domingo")
            val parts = mutableListOf<String>()
            for (day in daysList) {
                if (json.has(day)) {
                    val array = json.getJSONArray(day)
                    if (array.length() > 0) {
                        val ranges = mutableListOf<String>()
                        for (i in 0 until array.length()) {
                            ranges.add(array.getString(i))
                        }
                        parts.add("$day: ${ranges.joinToString(", ")}")
                    }
                }
            }
            if (parts.isNotEmpty()) parts.joinToString(" | ") else "Sin disponibilidad"
        } catch (e: Exception) {
            availStr
        }
    }
    return availStr
}

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
                var passwordVisible by remember { mutableStateOf(false) }


                val context = LocalContext.current
                val gso = remember {
                    GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                        .requestIdToken("203684276873-gfrrh4brb30rguelmebrvn279isg4k77.apps.googleusercontent.com")
                        .requestEmail()
                        .build()
                }
                val googleSignInClient = remember {
                    GoogleSignIn.getClient(context, gso)
                }

                val googleSignInLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.StartActivityForResult()
                ) { result ->
                    val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                    try {
                        val account = task.getResult(ApiException::class.java)
                        val idToken = account?.idToken
                        if (idToken != null) {
                            scope.launch {
                                val resp = api.loginWithGoogle(idToken, "ROLE_USER")
                                if (resp != null) {
                                    token = resp.token
                                    userId = resp.id
                                    currentUsername = account.email ?: "google-user"
                                    userEmail = account.email ?: "google-user"
                                    api.setToken(resp.token)
                                    
                                    try {
                                        val nameToUse = account.displayName ?: (account.email?.substringBefore("@") ?: "Google Athlete")
                                        api.createUserProfile(nameToUse, account.email ?: "", "999999999")
                                    } catch (e: Exception) {}
                                    
                                    try {
                                        val profile = api.getUserProfile(account.email ?: "")
                                        if (profile != null) {
                                            userId = profile.id
                                        }
                                    } catch (e: Exception) {}
                                    
                                    navController.navigate("coaches") { popUpTo("login") { inclusive = true } }
                                } else {
                                    error = "Error al autenticar token con el servidor"
                                }
                            }
                        } else {
                            error = "No se recibió ID Token de Google"
                        }
                    } catch (e: ApiException) {
                        error = "Google Sign-In falló: ${e.statusCode}"
                    }
                }

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
                            OutlinedTextField(
                                value = password,
                                onValueChange = { password = it },
                                label = { Text("Password") },
                                shape = InputShape,
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = Color.LightGray, focusedBorderColor = Terracotta),
                                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                trailingIcon = {
                                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                        Text(if (passwordVisible) "👁️" else "🔒", fontSize = 20.sp)
                                    }
                                }
                            )
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

                            Spacer(Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Divider(modifier = Modifier.weight(1f), color = Color.LightGray)
                                Text("O", modifier = Modifier.padding(horizontal = 8.dp), fontSize = 12.sp, color = Color.Gray)
                                Divider(modifier = Modifier.weight(1f), color = Color.LightGray)
                            }
                            Spacer(Modifier.height(12.dp))

                            var showGoogleMockDialog by remember { mutableStateOf(false) }

                            Button(
                                onClick = { 
                                    // Trigger native Google Sign-in flow
                                    googleSignInLauncher.launch(googleSignInClient.signInIntent)
                                },
                                modifier = Modifier.fillMaxWidth().height(56.dp),
                                shape = ButtonShape,
                                colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                                border = ButtonDefaults.outlinedButtonBorder
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Text("G", fontWeight = FontWeight.Bold, color = Terracotta, fontSize = 20.sp, modifier = Modifier.padding(end = 8.dp))
                                    Text("Continuar con Google", color = NavyText, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            Spacer(Modifier.height(8.dp))
                            TextButton(
                                onClick = { showGoogleMockDialog = true },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "¿Probar en local? Simular Google Login",
                                    color = NavyText,
                                    fontSize = 13.sp,
                                    style = androidx.compose.ui.text.TextStyle(
                                        textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                )
                            }

                            if (showGoogleMockDialog) {
                                var googleEmail by remember { mutableStateOf("") }
                                var googleName by remember { mutableStateOf("") }
                                var googleError by remember { mutableStateOf<String?>(null) }
                                
                                AlertDialog(
                                    onDismissRequest = { showGoogleMockDialog = false },
                                    title = { Text("Google Sign-In (OAuth Mock)") },
                                    text = {
                                        Column {
                                            googleError?.let {
                                                Text(it, color = Color.Red, fontSize = 14.sp)
                                                Spacer(Modifier.height(8.dp))
                                            }
                                            OutlinedTextField(
                                                value = googleEmail,
                                                onValueChange = { googleEmail = it },
                                                label = { Text("Correo de Google") },
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                            Spacer(Modifier.height(8.dp))
                                            OutlinedTextField(
                                                value = googleName,
                                                onValueChange = { googleName = it },
                                                label = { Text("Nombre") },
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                        }
                                    },
                                    confirmButton = {
                                        Button(
                                            onClick = {
                                                if (!googleEmail.contains("@")) {
                                                    googleError = "Correo electrónico inválido"
                                                } else {
                                                    scope.launch {
                                                        val mockToken = "mock-token-$googleEmail"
                                                        val resp = api.loginWithGoogle(mockToken, "ROLE_USER")
                                                        if (resp != null) {
                                                            token = resp.token
                                                            userId = resp.id
                                                            currentUsername = googleEmail
                                                            userEmail = googleEmail
                                                            api.setToken(resp.token)
                                                            
                                                            try {
                                                                val nameToUse = googleName.ifBlank { googleEmail.substringBefore("@") }
                                                                api.createUserProfile(nameToUse, googleEmail, "999999999")
                                                            } catch (e: Exception) {}
                                                            
                                                            try {
                                                                val profile = api.getUserProfile(googleEmail)
                                                                if (profile != null) {
                                                                    userId = profile.id
                                                                }
                                                            } catch(e: Exception) {}
                                                            
                                                            showGoogleMockDialog = false
                                                            navController.navigate("coaches") { popUpTo("login") { inclusive = true } }
                                                        } else {
                                                            googleError = "Error al conectar con el backend"
                                                        }
                                                    }
                                                }
                                            }
                                        ) {
                                            Text("Ingresar")
                                        }
                                    },
                                    dismissButton = {
                                        TextButton(onClick = { showGoogleMockDialog = false }) {
                                            Text("Cancelar")
                                        }
                                    }
                                )
                            }
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
                 var passwordVisible by remember { mutableStateOf(false) }
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
                             OutlinedTextField(
                                 value = password,
                                 onValueChange = { password = it },
                                 label = { Text("Password") },
                                 shape = InputShape,
                                 modifier = Modifier.fillMaxWidth(),
                                 visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                 keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                 trailingIcon = {
                                     IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                         Text(if (passwordVisible) "👁️" else "🔒", fontSize = 20.sp)
                                     }
                                 }
                             )
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
                var allCoachesList by remember { mutableStateOf<List<Coach>>(emptyList()) }
                var sportType by remember { mutableStateOf("") }
                var location by remember { mutableStateOf("") }
                var maxPrice by remember { mutableStateOf("") }
                var minRating by remember { mutableStateOf("") }
                var loading by remember { mutableStateOf(true) }

                LaunchedEffect(Unit) {
                    loading = true
                    allCoachesList = api.getCoaches()
                    loading = false
                }

                val filteredCoaches = remember(sportType, location, maxPrice, minRating, allCoachesList) {
                    allCoachesList.filter { coach ->
                        val matchSport = sportType.isBlank() || 
                            (coach.sportType?.contains(sportType, ignoreCase = true) == true) ||
                            coach.expertise.contains(sportType, ignoreCase = true) ||
                            coach.name.contains(sportType, ignoreCase = true)
                        val matchLocation = location.isBlank() || 
                            (coach.location?.contains(location, ignoreCase = true) == true)
                        val matchPrice = maxPrice.toDoubleOrNull()?.let {
                            coach.pricePerHour == null || coach.pricePerHour <= it
                        } ?: true
                        val matchRating = minRating.toDoubleOrNull()?.let {
                            coach.rating == null || coach.rating >= it
                        } ?: true
                        matchSport && matchLocation && matchPrice && matchRating
                    }
                }

                Column(modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())) {
                    Text("Nuestros Coaches", fontWeight = FontWeight.ExtraBold, fontSize = 28.sp, color = NavyText, modifier = Modifier.padding(bottom = 8.dp))
                    
                    // Filter Panel
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                        shape = CardShape,
                        colors = CardDefaults.cardColors(containerColor = WhiteSurface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Filtros de búsqueda", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = NavyText, modifier = Modifier.padding(bottom = 8.dp))
                            
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = sportType,
                                    onValueChange = { sportType = it },
                                    label = { Text("Deporte / Nombre", fontSize = 12.sp) },
                                    shape = InputShape,
                                    modifier = Modifier.weight(1f),
                                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Terracotta)
                                )
                                OutlinedTextField(
                                    value = location,
                                    onValueChange = { location = it },
                                    label = { Text("Ubicación", fontSize = 12.sp) },
                                    shape = InputShape,
                                    modifier = Modifier.weight(1f),
                                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Terracotta)
                                )
                            }
                            Spacer(Modifier.height(8.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = maxPrice,
                                    onValueChange = { maxPrice = it },
                                    label = { Text("Precio Max", fontSize = 12.sp) },
                                    shape = InputShape,
                                    modifier = Modifier.weight(1f),
                                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Terracotta)
                                )
                                OutlinedTextField(
                                    value = minRating,
                                    onValueChange = { minRating = it },
                                    label = { Text("Rating Min", fontSize = 12.sp) },
                                    shape = InputShape,
                                    modifier = Modifier.weight(1f),
                                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Terracotta)
                                )
                            }
                        }
                    }
                    
                    if (loading) {
                        Text("Cargando coaches...", color = Color.Gray, modifier = Modifier.padding(16.dp))
                    } else if (filteredCoaches.isEmpty()) {
                        Text("No se encontraron coaches.", color = Color.Gray, modifier = Modifier.padding(16.dp))
                    } else {
                        filteredCoaches.forEach { coach ->
                            Card(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                                shape = CardShape,
                                colors = CardDefaults.cardColors(containerColor = WhiteSurface),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) { 
                                        Text(coach.name, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = NavyText)
                                        Text(coach.sportType ?: coach.expertise, fontSize = 14.sp, color = Color.Gray) 
                                        coach.availability?.let { 
                                            Text(
                                                text = "Horario: ${formatFriendlyAvailability(it)}",
                                                fontSize = 12.sp,
                                                color = Terracotta,
                                                maxLines = 1,
                                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                                modifier = Modifier.padding(top = 4.dp)
                                            )
                                        } 
                                    }
                                    Column(
                                        horizontalAlignment = Alignment.End,
                                        modifier = Modifier.wrapContentWidth()
                                    ) {
                                        coach.pricePerHour?.let { Text("$${it}/hr", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = SoftGreen) }
                                        coach.rating?.let { Text("⭐ ${String.format("%.1f", it)}", fontSize = 14.sp, color = Terracotta, fontWeight = FontWeight.Medium) }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            composable("courts") {
                var activeTab by remember { mutableStateOf("services") } // "services" or "courts"
                
                // Coach Services Tab State
                var coaches by remember { mutableStateOf<List<Coach>>(emptyList()) }
                var services by remember { mutableStateOf<List<CoachService>>(emptyList()) }
                var selectedService by remember { mutableStateOf<CoachService?>(null) }
                var selectedCoach by remember { mutableStateOf<Coach?>(null) }
                var loadingServices by remember { mutableStateOf(true) }

                // Courts Tab State
                var allCourtsList by remember { mutableStateOf<List<Court>>(emptyList()) }
                var courtSportType by remember { mutableStateOf("") }
                var courtLocation by remember { mutableStateOf("") }
                var selectedCourt by remember { mutableStateOf<Court?>(null) }
                var loadingCourts by remember { mutableStateOf(true) }

                var showBookingDialog by remember { mutableStateOf(false) }

                val filteredCourts = remember(courtSportType, courtLocation, allCourtsList) {
                    allCourtsList.filter { court ->
                        val matchSport = courtSportType.isBlank() || 
                            (court.sportType?.contains(courtSportType, ignoreCase = true) == true) ||
                            court.name.contains(courtSportType, ignoreCase = true)
                        val matchLocation = courtLocation.isBlank() || 
                            court.location.contains(courtLocation, ignoreCase = true)
                        matchSport && matchLocation
                    }
                }

                LaunchedEffect(activeTab) {
                    if (activeTab == "services") {
                        coaches = api.getCoaches()
                        loadingServices = false
                        if (coaches.isNotEmpty()) {
                            val firstCoach = coaches.first()
                            selectedCoach = firstCoach
                            services = api.getCoachServices(firstCoach.id)
                        }
                    } else {
                        loadingCourts = true
                        allCourtsList = api.getCourts()
                        loadingCourts = false
                    }
                }

                Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                    // Top tab switcher
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { activeTab = "services" },
                            modifier = Modifier.weight(1f),
                            shape = ButtonShape,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (activeTab == "services") Terracotta else Color.LightGray
                            )
                        ) {
                            Text("Servicios de Coach", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                        Button(
                            onClick = { activeTab = "courts" },
                            modifier = Modifier.weight(1f),
                            shape = ButtonShape,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (activeTab == "courts") Terracotta else Color.LightGray
                            )
                        ) {
                            Text("Alquiler Canchas", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }

                    if (activeTab == "services") {
                        Text("Contratar Entrenador", fontWeight = FontWeight.ExtraBold, fontSize = 22.sp, color = NavyText, modifier = Modifier.padding(bottom = 12.dp))
                        
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

                        if (loadingServices) {
                            Text("Cargando...", color = Color.Gray)
                        } else if (services.isEmpty()) {
                            Text("Este coach no tiene servicios disponibles.", color = Color.Gray, modifier = Modifier.padding(vertical = 16.dp))
                        } else {
                            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                                services.forEach { service ->
                                    Card(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                                        shape = CardShape,
                                        colors = CardDefaults.cardColors(containerColor = WhiteSurface),
                                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                                        onClick = {
                                            selectedService = service
                                            selectedCourt = null
                                            showBookingDialog = true
                                        }
                                    ) {
                                        Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(service.name, fontWeight = FontWeight.Bold, fontSize = 17.sp, color = NavyText)
                                                Text(selectedCoach?.name ?: "", fontSize = 13.sp, color = Color.Gray)
                                                service.description?.let { Text(it, fontSize = 12.sp, color = Color.Gray) }
                                            }
                                            Column(horizontalAlignment = Alignment.End, modifier = Modifier.padding(start = 8.dp)) {
                                                service.price?.let { Text("$${it}", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = SoftGreen) }
                                                Text("📅 Reservar", fontSize = 12.sp, color = Terracotta, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        // Courts Search and List Tab
                        Text("Alquilar Canchas", fontWeight = FontWeight.ExtraBold, fontSize = 22.sp, color = NavyText, modifier = Modifier.padding(bottom = 12.dp))
                        
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                            shape = CardShape,
                            colors = CardDefaults.cardColors(containerColor = WhiteSurface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(
                                        value = courtSportType,
                                        onValueChange = { courtSportType = it },
                                        label = { Text("Deporte / Nombre", fontSize = 11.sp) },
                                        shape = InputShape,
                                        modifier = Modifier.weight(1f)
                                    )
                                    OutlinedTextField(
                                        value = courtLocation,
                                        onValueChange = { courtLocation = it },
                                        label = { Text("Ubicación", fontSize = 11.sp) },
                                        shape = InputShape,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }

                        if (loadingCourts) {
                            Text("Cargando...", color = Color.Gray)
                        } else if (filteredCourts.isEmpty()) {
                            Text("No se encontraron canchas.", color = Color.Gray, modifier = Modifier.padding(vertical = 16.dp))
                        } else {
                            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                                filteredCourts.forEach { court ->
                                    Card(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                                        shape = CardShape,
                                        colors = CardDefaults.cardColors(containerColor = WhiteSurface),
                                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                                        onClick = {
                                            selectedCourt = court
                                            selectedService = null
                                            showBookingDialog = true
                                        }
                                    ) {
                                        Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(court.name, fontWeight = FontWeight.Bold, fontSize = 17.sp, color = NavyText)
                                                Text("${court.location} • ${court.type}", fontSize = 13.sp, color = Color.Gray)
                                                court.sportType?.let { Text("Deporte: $it", fontSize = 12.sp, color = Color.Gray) }
                                                court.openingHours?.let { Text("Horario: $it", fontSize = 12.sp, color = Terracotta) }
                                            }
                                            Column(horizontalAlignment = Alignment.End, modifier = Modifier.padding(start = 8.dp)) {
                                                court.pricePerHour?.let { Text("$${it}/hr", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = SoftGreen) }
                                                Text("📅 Alquilar", fontSize = 12.sp, color = Terracotta, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                if (showBookingDialog && (selectedService != null || selectedCourt != null)) {
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
                    val nameToDisplay = selectedService?.name ?: selectedCourt!!.name
                    val priceToDisplay = selectedService?.price ?: selectedCourt!!.pricePerHour ?: 40.0
                    val availabilityToDisplay = selectedCoach?.availability ?: selectedCourt?.openingHours ?: "08:00-22:00"
                    val times = remember(availabilityToDisplay, selectedDate) {
                        val defaultTimes = listOf("08:00", "09:00", "10:00", "11:00", "12:00", "13:00", "14:00", "15:00", "16:00", "17:00", "18:00", "19:00", "20:00")
                        val cleanAvail = availabilityToDisplay.trim()
                        if (cleanAvail.startsWith("{")) {
                            try {
                                val dayName = getDayOfWeekSpanish(selectedDate)
                                val json = org.json.JSONObject(cleanAvail)
                                if (json.has(dayName)) {
                                    val array = json.getJSONArray(dayName)
                                    val slots = mutableListOf<String>()
                                    for (i in 0 until array.length()) {
                                        val range = array.getString(i)
                                        if (range.contains("-")) {
                                            val parts = range.split("-")
                                            val startHour = parts.getOrNull(0)?.split(":")?.getOrNull(0)?.trim()?.toIntOrNull()
                                            val endHour = parts.getOrNull(1)?.split(":")?.getOrNull(0)?.trim()?.toIntOrNull()
                                            if (startHour != null && endHour != null && startHour < endHour) {
                                                for (h in startHour until endHour) {
                                                    slots.add("%02d:00".format(h))
                                                }
                                            }
                                        }
                                    }
                                    if (slots.isNotEmpty()) slots.sorted() else emptyList<String>()
                                } else {
                                    emptyList<String>()
                                }
                            } catch (e: Exception) {
                                defaultTimes
                            }
                        } else {
                            if (availabilityToDisplay.contains("-")) {
                                val parts = availabilityToDisplay.split("-")
                                val startHour = parts.getOrNull(0)?.split(":")?.getOrNull(0)?.trim()?.toIntOrNull()
                                val endHour = parts.getOrNull(1)?.split(":")?.getOrNull(0)?.trim()?.toIntOrNull()
                                if (startHour != null && endHour != null && startHour < endHour) {
                                    (startHour..endHour).map { "%02d:00".format(it) }
                                } else defaultTimes
                            } else defaultTimes
                        }
                    }
                    LaunchedEffect(times) {
                        if (times.isNotEmpty()) {
                            if (!times.contains(selectedTime)) {
                                selectedTime = times.first()
                            }
                        } else {
                            selectedTime = "No disponible"
                        }
                    }

                    AlertDialog(
                        onDismissRequest = { showBookingDialog = false },
                        title = { Text("Reservar $nameToDisplay") },
                        text = {
                            Column {
                                if (selectedService != null) {
                                    Text("Coach: ${selectedCoach?.name ?: ""}")
                                } else {
                                    Text("Tipo: Alquiler de Cancha")
                                }
                                Text("Horarios: ${formatFriendlyAvailability(availabilityToDisplay)}", fontSize = 13.sp, color = Terracotta)
                                Spacer(Modifier.height(4.dp))
                                Text("Precio: $${priceToDisplay}")
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
                            Button(
                                enabled = times.isNotEmpty() && selectedTime != "No disponible",
                                onClick = {
                                    scope.launch {
                                        val userIdVal = userId ?: 1L
                                        api.createBooking(
                                            selectedCourt?.id,
                                            userIdVal,
                                            "${selectedDate}T${selectedTime}:00",
                                            if (selectedService != null) "Service: ${selectedService!!.name}" else "Court Rental",
                                            priceToDisplay,
                                            selectedService?.id
                                        )
                                        showBookingDialog = false
                                    }
                                }, 
                                colors = ButtonDefaults.buttonColors(containerColor = SoftGreen)
                            ) { Text("CONFIRMAR") }
                        },
                        dismissButton = {
                            TextButton(onClick = { showBookingDialog = false }) { Text("CANCELAR") }
                        }
                    )
                }
            }

            composable("bookings") {
                var bookings by remember { mutableStateOf<List<Booking>>(emptyList()) }
                var showReviewDialog by remember { mutableStateOf(false) }
                var reviewingCoachId by remember { mutableStateOf<Long?>(null) }
                var reviewingCoachName by remember { mutableStateOf("") }
                var reviewRating by remember { mutableStateOf(5) }
                var reviewComment by remember { mutableStateOf("") }

                val loadUserBookings = {
                    scope.launch {
                        val userIdVal = userId ?: 1L
                        bookings = api.getBookingsByUser(userIdVal)
                    }
                }

                LaunchedEffect(Unit) {
                    loadUserBookings()
                }

                Column(modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())) {
                    Text("Mis Reservas", fontWeight = FontWeight.ExtraBold, fontSize = 28.sp, color = NavyText, modifier = Modifier.padding(bottom = 16.dp))
                    
                    bookings.forEach { b ->
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                            shape = CardShape,
                            colors = CardDefaults.cardColors(containerColor = WhiteSurface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        val title = if (b.court != null) {
                                            b.court.name ?: "Cancha Reservada"
                                        } else {
                                            b.serviceName ?: "Servicio Reservado"
                                        }
                                        Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = NavyText)
                                        
                                        if (b.court != null) {
                                            b.court.location?.let {
                                                Text("📍 $it", fontSize = 13.sp, color = Color.Gray, modifier = Modifier.padding(top = 2.dp))
                                            }
                                        } else if (b.coach != null) {
                                            b.coach.name?.let {
                                                Text("👤 Entrenador: $it", fontSize = 13.sp, color = Color.Gray, modifier = Modifier.padding(top = 2.dp))
                                            }
                                        }
                                        
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text("Reserva #${b.id} • ${formatDate(b.startTime)}", fontSize = 12.sp, color = Color.Gray)
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = when (b.status.uppercase()) { "CONFIRMED", "COMPLETED" -> SoftGreen.copy(alpha=0.2f); "PENDING" -> Color(0xFFFFF3E0); else -> Color(0xFFF5F5F5) },
                                        modifier = Modifier.padding(start = 8.dp)
                                    ) {
                                        Text(
                                            when (b.status.uppercase()) { "PENDING" -> "PENDIENTE"; "CONFIRMED" -> "CONFIRMADA"; "COMPLETED" -> "COMPLETADA"; else -> b.status }, 
                                            color = when (b.status.uppercase()) { "CONFIRMED", "COMPLETED" -> SoftGreen; "PENDING" -> Color(0xFFFF9800); else -> Color.Gray },
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 10.sp,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                                
                                if (b.status.uppercase() == "COMPLETED" && b.coach != null) {
                                    Button(
                                        onClick = {
                                            reviewingCoachId = b.coach.id
                                            reviewingCoachName = b.coach.name ?: "Coach"
                                            reviewRating = 5
                                            reviewComment = ""
                                            showReviewDialog = true
                                        },
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Terracotta),
                                        modifier = Modifier.padding(top = 8.dp).align(Alignment.End)
                                    ) {
                                        Text("Valorar Coach", fontSize = 11.sp, color = Color.White)
                                    }
                                }
                            }
                        }
                    }
                    if (bookings.isEmpty()) Text("Aún no tienes reservas", color = Color.Gray, modifier = Modifier.padding(16.dp))
                }

                if (showReviewDialog && reviewingCoachId != null) {
                    AlertDialog(
                        onDismissRequest = { showReviewDialog = false },
                        title = { Text("Valorar a $reviewingCoachName") },
                        text = {
                            Column {
                                Text("Selecciona puntuación:")
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.padding(vertical = 8.dp)
                                ) {
                                    (1..5).forEach { stars ->
                                        IconButton(onClick = { reviewRating = stars }) {
                                            Text(
                                                if (stars <= reviewRating) "★" else "☆",
                                                color = Terracotta,
                                                fontSize = 28.sp
                                            )
                                        }
                                    }
                                }
                                OutlinedTextField(
                                    value = reviewComment,
                                    onValueChange = { reviewComment = it },
                                    label = { Text("Comentario (opcional)") },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        },
                        confirmButton = {
                            Button(onClick = {
                                scope.launch {
                                    val userIdVal = userId ?: 1L
                                    api.createReview(reviewingCoachId!!, userIdVal, reviewRating, reviewComment)
                                    showReviewDialog = false
                                }
                            }, colors = ButtonDefaults.buttonColors(containerColor = SoftGreen)) {
                                Text("ENVIAR")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showReviewDialog = false }) { Text("CANCELAR") }
                        }
                    )
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