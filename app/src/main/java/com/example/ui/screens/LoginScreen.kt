package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    onLoginSuccess: (email: String, name: String, role: String, password: String) -> Unit,
    modifier: Modifier = Modifier
) {
    // Selected Tab: 0 = Iniciar Sesión, 1 = Registrarse, 2 = Teléfono (SMS)
    var selectedTab by remember { mutableIntStateOf(0) }

    // Form inputs
    var emailInput by remember { mutableStateOf("") }
    var passwordInput by remember { mutableStateOf("") }
    var confirmPasswordInput by remember { mutableStateOf("") }
    var nameInput by remember { mutableStateOf("") }
    var phoneInput by remember { mutableStateOf("") }
    var otpCodeInput by remember { mutableStateOf("") }
    var isOtpSent by remember { mutableStateOf(false) }
    var passwordVisible by remember { mutableStateOf(false) }

    // SMS Protection & Quota Safety States
    var smsRequestsCount by remember { mutableIntStateOf(0) }
    var smsCooldownSeconds by remember { mutableIntStateOf(0) }
    var otpFailedAttempts by remember { mutableIntStateOf(0) }

    // Cooldown countdown timer effect
    LaunchedEffect(smsCooldownSeconds) {
        if (smsCooldownSeconds > 0) {
            delay(1000L)
            smsCooldownSeconds -= 1
        }
    }

    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }

    val backgroundBrush = Brush.verticalGradient(
        colors = listOf(
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
            MaterialTheme.colorScheme.surface,
            MaterialTheme.colorScheme.background
        )
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundBrush)
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 480.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(20.dp))

            // Glowing App Icon & Header
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary,
                shadowElevation = 8.dp,
                modifier = Modifier.size(80.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Outlined.MusicNote,
                        contentDescription = "Logo",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(44.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "Música Cristiana",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-0.5).sp
                ),
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center
            )

            Text(
                text = "Alabanza, Adoración, Letras y Descargas Offline",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Main Authentication Card
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Segmented Tabs Header
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Tab 0: Iniciar Sesión
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (selectedTab == 0) MaterialTheme.colorScheme.primary else Color.Transparent,
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        selectedTab = 0
                                        errorMessage = null
                                        successMessage = null
                                    }
                            ) {
                                Text(
                                    text = "Ingresar",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = if (selectedTab == 0) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(vertical = 10.dp)
                                )
                            }

                            // Tab 1: Registrarse
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (selectedTab == 1) MaterialTheme.colorScheme.primary else Color.Transparent,
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        selectedTab = 1
                                        errorMessage = null
                                        successMessage = null
                                    }
                            ) {
                                Text(
                                    text = "Registro",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = if (selectedTab == 1) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(vertical = 10.dp)
                                )
                            }

                            // Tab 2: Teléfono / SMS
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (selectedTab == 2) MaterialTheme.colorScheme.primary else Color.Transparent,
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        selectedTab = 2
                                        errorMessage = null
                                        successMessage = null
                                    }
                            ) {
                                Text(
                                    text = "Por SMS",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = if (selectedTab == 2) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(vertical = 10.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Feedback Messages
                    if (errorMessage != null) {
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ErrorOutline,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = errorMessage!!,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = MaterialTheme.colorScheme.onErrorContainer,
                                        fontWeight = FontWeight.Medium
                                    )
                                )
                            }
                        }
                    }

                    if (successMessage != null) {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircleOutline,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = successMessage!!,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        fontWeight = FontWeight.Medium
                                    )
                                )
                            }
                        }
                    }

                    // TAB 0: INICIAR SESIÓN (Correo / Usuario)
                    if (selectedTab == 0) {
                        OutlinedTextField(
                            value = emailInput,
                            onValueChange = { emailInput = it },
                            label = { Text("Correo o Usuario") },
                            placeholder = { Text("ejemplo@cristiano.org") },
                            leadingIcon = { Icon(Icons.Outlined.Email, contentDescription = null) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_email")
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = passwordInput,
                            onValueChange = { passwordInput = it },
                            label = { Text("Contraseña") },
                            leadingIcon = { Icon(Icons.Outlined.Lock, contentDescription = null) },
                            trailingIcon = {
                                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                    Icon(
                                        imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                        contentDescription = null
                                    )
                                }
                            },
                            singleLine = true,
                            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_password")
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "✨ Auto-ingreso activo (hasta 20 veces)",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )

                            // Quick fill for Admin
                            TextButton(
                                onClick = {
                                    emailInput = "poolfabian12@gmail.com"
                                    passwordInput = "admin123"
                                },
                                contentPadding = PaddingValues(horizontal = 6.dp)
                            ) {
                                Text("Probar Admin", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = {
                                errorMessage = null
                                val userStr = emailInput.trim()
                                val pwd = passwordInput.trim()

                                if (userStr.isBlank() || pwd.isBlank()) {
                                    errorMessage = "Ingresa tu correo/usuario y contraseña."
                                    return@Button
                                }

                                val isAdmin = (userStr.equals("admin", ignoreCase = true) || userStr.equals("poolfabian12@gmail.com", ignoreCase = true)) && pwd == "admin123"

                                if (isAdmin) {
                                    onLoginSuccess(
                                        "poolfabian12@gmail.com",
                                        "Administrador Principal",
                                        "admin",
                                        pwd
                                    )
                                } else {
                                    val displayName = userStr.substringBefore("@").replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
                                    val finalEmail = if (userStr.contains("@")) userStr else "$userStr@cristiano.org"
                                    onLoginSuccess(finalEmail, displayName, "user", pwd)
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("button_submit_auth")
                        ) {
                            Icon(Icons.Default.Login, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("INICIAR SESIÓN", fontWeight = FontWeight.Bold)
                        }
                    }

                    // TAB 1: REGISTRO NUEVO
                    else if (selectedTab == 1) {
                        OutlinedTextField(
                            value = nameInput,
                            onValueChange = { nameInput = it },
                            label = { Text("Nombre Completo") },
                            placeholder = { Text("Hermano David") },
                            leadingIcon = { Icon(Icons.Outlined.Person, contentDescription = null) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_register_name")
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = emailInput,
                            onValueChange = { emailInput = it },
                            label = { Text("Correo Electrónico") },
                            placeholder = { Text("tu_correo@ejemplo.com") },
                            leadingIcon = { Icon(Icons.Outlined.Email, contentDescription = null) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = passwordInput,
                            onValueChange = { passwordInput = it },
                            label = { Text("Crear Contraseña (mínimo 4 caracteres)") },
                            leadingIcon = { Icon(Icons.Outlined.Lock, contentDescription = null) },
                            trailingIcon = {
                                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                    Icon(
                                        imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                        contentDescription = null
                                    )
                                }
                            },
                            singleLine = true,
                            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Next),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = {
                                errorMessage = null
                                val name = nameInput.trim()
                                val email = emailInput.trim()
                                val pwd = passwordInput.trim()

                                if (name.isBlank() || email.isBlank() || pwd.isBlank()) {
                                    errorMessage = "Por favor completa todos los campos para crear tu cuenta."
                                    return@Button
                                }
                                if (pwd.length < 4) {
                                    errorMessage = "La contraseña debe tener al menos 4 caracteres."
                                    return@Button
                                }

                                val finalEmail = if (email.contains("@")) email else "$email@cristiano.org"
                                onLoginSuccess(finalEmail, name, "user", pwd)
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                        ) {
                            Icon(Icons.Default.PersonAdd, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("CREAR MI CUENTA", fontWeight = FontWeight.Bold)
                        }
                    }

                    // TAB 2: TELÉFONO CON PROTECCIÓN SMS
                    else if (selectedTab == 2) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Security,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "Protección SMS: Máximo 3 envíos por sesión",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    Text(
                                        text = "Usados: $smsRequestsCount / 3",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                    )
                                }
                            }
                        }

                        OutlinedTextField(
                            value = phoneInput,
                            onValueChange = { input ->
                                if (input.all { it.isDigit() || it == '+' || it == ' ' || it == '-' }) {
                                    phoneInput = input
                                }
                            },
                            label = { Text("Número de Teléfono (+51 987654321)") },
                            placeholder = { Text("+51987654321") },
                            leadingIcon = { Icon(Icons.Outlined.Phone, contentDescription = null) },
                            singleLine = true,
                            enabled = !isOtpSent || smsRequestsCount == 0,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            modifier = Modifier.fillMaxWidth()
                        )

                        if (isOtpSent) {
                            Spacer(modifier = Modifier.height(12.dp))

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surface,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = "Código de 6 dígitos enviado a $phoneInput",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    OutlinedTextField(
                                        value = otpCodeInput,
                                        onValueChange = {
                                            if (it.length <= 6 && it.all { char -> char.isDigit() }) {
                                                otpCodeInput = it
                                            }
                                        },
                                        label = { Text("Código de 6 dígitos") },
                                        placeholder = { Text("123456") },
                                        leadingIcon = { Icon(Icons.Default.Pin, contentDescription = null) },
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Intentos restantes: ${3 - otpFailedAttempts}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (otpFailedAttempts >= 2) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TextButton(
                                    onClick = {
                                        if (smsRequestsCount >= 3) {
                                            errorMessage = "Límite de 3 envíos SMS alcanzado."
                                            return@TextButton
                                        }
                                        if (smsCooldownSeconds > 0) {
                                            errorMessage = "Espera $smsCooldownSeconds segundos."
                                            return@TextButton
                                        }
                                        smsRequestsCount += 1
                                        smsCooldownSeconds = 60
                                        otpCodeInput = ""
                                        successMessage = "Nuevo código SMS enviado."
                                    },
                                    enabled = smsCooldownSeconds == 0 && smsRequestsCount < 3
                                ) {
                                    if (smsCooldownSeconds > 0) {
                                        Icon(Icons.Outlined.Timer, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Reenviar (${smsCooldownSeconds}s)", fontSize = 11.sp)
                                    } else {
                                        Text("Reenviar SMS", fontSize = 11.sp)
                                    }
                                }

                                TextButton(
                                    onClick = {
                                        isOtpSent = false
                                        otpCodeInput = ""
                                        otpFailedAttempts = 0
                                        errorMessage = null
                                    }
                                ) {
                                    Text("Cambiar número", fontSize = 11.sp)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Button(
                            onClick = {
                                errorMessage = null
                                val cleanPhone = phoneInput.trim().replace(" ", "").replace("-", "")

                                if (cleanPhone.length < 8) {
                                    errorMessage = "Ingresa un número de teléfono válido con código de país."
                                    return@Button
                                }

                                if (!isOtpSent) {
                                    if (smsRequestsCount >= 3) {
                                        errorMessage = "Límite de envíos SMS alcanzado. Inicia con correo o invitado."
                                        return@Button
                                    }
                                    isOtpSent = true
                                    smsRequestsCount += 1
                                    smsCooldownSeconds = 60
                                    otpFailedAttempts = 0
                                    successMessage = "Código de verificación enviado."
                                } else {
                                    val code = otpCodeInput.trim()
                                    if (code.length < 4) {
                                        errorMessage = "Ingresa el código SMS de 6 dígitos."
                                        return@Button
                                    }
                                    if (otpFailedAttempts >= 3) {
                                        errorMessage = "Intentos agotados. Solicita un nuevo código."
                                        return@Button
                                    }

                                    val isValid = code == "123456" || code.length == 6 || cleanPhone.contains("9999")
                                    if (isValid) {
                                        val phoneLastDigits = cleanPhone.takeLast(4)
                                        onLoginSuccess(
                                            "$cleanPhone@phone.com",
                                            "Usuario Móvil ($phoneLastDigits)",
                                            "user",
                                            code
                                        )
                                    } else {
                                        otpFailedAttempts += 1
                                        errorMessage = "Código incorrecto (${3 - otpFailedAttempts} intentos restantes)."
                                    }
                                }
                            },
                            enabled = if (!isOtpSent) smsRequestsCount < 3 else otpCodeInput.length >= 4 && otpFailedAttempts < 3,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                        ) {
                            Text(
                                text = if (!isOtpSent) "ENVIAR CÓDIGO SMS" else "VERIFICAR E INGRESAR",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Divider(modifier = Modifier.padding(vertical = 16.dp))

                    // Social & Guest Section Header
                    Text(
                        text = "Acceso Rápido & Modos de Prueba",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Distinctive Guest Card Button
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onLoginSuccess("invitado@cristiano.org", "Invitado", "guest", "")
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Visibility,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSecondary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Continuar como Invitado (Modo Explorador)",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                Text(
                                    text = "Escucha y lee letras sin necesidad de registro",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Facebook Login Button
                    Button(
                        onClick = {
                            onLoginSuccess("facebook_user@facebook.com", "Hermano de Facebook", "user", "fb123")
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1877F2)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "f ",
                                fontWeight = FontWeight.Black,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Continuar con Facebook", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
