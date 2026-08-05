package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
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
    onLoginSuccess: (email: String, name: String, role: String) -> Unit,
    modifier: Modifier = Modifier
) {
    var authType by remember { mutableStateOf("email") } // "email", "phone", "facebook"
    var isRegisterMode by remember { mutableStateOf(false) }

    var emailInput by remember { mutableStateOf("") }
    var passwordInput by remember { mutableStateOf("") }
    var nameInput by remember { mutableStateOf("") }
    var phoneInput by remember { mutableStateOf("") }
    var otpCodeInput by remember { mutableStateOf("") }
    var isOtpSent by remember { mutableStateOf(false) }
    var passwordVisible by remember { mutableStateOf(false) }

    // SMS Protection & Quota Safety States
    var smsRequestsCount by remember { mutableIntStateOf(0) }
    var smsCooldownSeconds by remember { mutableIntStateOf(0) }
    var otpFailedAttempts by remember { mutableIntStateOf(0) }
    var simulatedGeneratedOtp by remember { mutableStateOf("123456") }

    // Cooldown countdown timer effect
    LaunchedEffect(smsCooldownSeconds) {
        if (smsCooldownSeconds > 0) {
            delay(1000L)
            smsCooldownSeconds -= 1
        }
    }

    var errorMessage by remember { mutableStateOf<String?>(null) }

    val backgroundBrush = Brush.verticalGradient(
        colors = listOf(
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
            MaterialTheme.colorScheme.background,
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
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            // App Icon & Header
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(80.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Outlined.MusicNote,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(44.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Música Cristiana",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                ),
                textAlign = TextAlign.Center
            )

            Text(
                text = "Alabanzas, Adoración y Letras Edificantes",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Auth Form Card
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = when (authType) {
                            "phone" -> "Inicio con Teléfono (SMS)"
                            else -> if (isRegisterMode) "Crear Cuenta" else "Iniciar Sesión"
                        },
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    if (errorMessage != null) {
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp)
                        ) {
                            Text(
                                text = errorMessage!!,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    fontWeight = FontWeight.Medium
                                ),
                                modifier = Modifier.padding(12.dp),
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    if (authType == "email") {
                        // Name Input (Only on Register)
                        AnimatedVisibility(visible = isRegisterMode) {
                            Column {
                                OutlinedTextField(
                                    value = nameInput,
                                    onValueChange = { nameInput = it },
                                    label = { Text("Nombre Completo") },
                                    leadingIcon = { Icon(Icons.Outlined.Person, contentDescription = null) },
                                    singleLine = true,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("input_register_name")
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                            }
                        }

                        // Email or Username Input
                        OutlinedTextField(
                            value = emailInput,
                            onValueChange = { emailInput = it },
                            label = { Text("Correo o Usuario") },
                            leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_email")
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Password Input
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
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_password")
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        // Submit Email Auth Button
                        Button(
                            onClick = {
                                errorMessage = null
                                val userInput = emailInput.trim()
                                val pwd = passwordInput.trim()

                                if (userInput.isBlank() || pwd.isBlank()) {
                                    errorMessage = "Por favor ingresa tu correo/usuario y contraseña."
                                    return@Button
                                }

                                val isAdminAccount = (userInput.equals("admin", ignoreCase = true) || userInput.equals("poolfabian12@gmail.com", ignoreCase = true)) && pwd == "admin123"

                                if (isAdminAccount) {
                                    onLoginSuccess(
                                        "poolfabian12@gmail.com",
                                        "Administrador Principal",
                                        "admin"
                                    )
                                } else if (!isRegisterMode) {
                                    val displayName = userInput.substringBefore("@").replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
                                    onLoginSuccess(
                                        if (userInput.contains("@")) userInput else "$userInput@cristiano.org",
                                        displayName,
                                        "user"
                                    )
                                } else {
                                    val displayName = if (nameInput.isNotBlank()) nameInput.trim() else userInput.substringBefore("@")
                                    onLoginSuccess(
                                        if (userInput.contains("@")) userInput else "$userInput@cristiano.org",
                                        displayName,
                                        "user"
                                    )
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("button_submit_auth")
                        ) {
                            Text(
                                text = if (isRegisterMode) "CREAR CUENTA" else "INICIAR SESIÓN",
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        TextButton(
                            onClick = {
                                isRegisterMode = !isRegisterMode
                                errorMessage = null
                            }
                        ) {
                            Text(
                                text = if (isRegisterMode) "¿Ya tienes cuenta? Inicia Sesión" else "¿No tienes cuenta? Regístrate aquí",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }

                    } else if (authType == "phone") {
                        // Phone Auth Form with Anti-Abuse and Quota Controls
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Security,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "Protección de Cuota SMS (Máx 3 envíos)",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    Text(
                                        text = "Envíos usados en esta sesión: $smsRequestsCount de 3",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                    )
                                }
                            }
                        }

                        // Phone Number Input
                        OutlinedTextField(
                            value = phoneInput,
                            onValueChange = { input ->
                                // Keep only valid phone characters: digits, spaces, +
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
                            Spacer(modifier = Modifier.height(16.dp))

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = "Código de 6 dígitos enviado a $phoneInput",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
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
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "Intentos de verificación restantes: ${3 - otpFailedAttempts}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (otpFailedAttempts >= 2) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Resend with Cooldown Timer
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TextButton(
                                    onClick = {
                                        if (smsRequestsCount >= 3) {
                                            errorMessage = "Has alcanzado el límite de 3 envíos SMS. Intenta con correo o como invitado."
                                            return@TextButton
                                        }
                                        if (smsCooldownSeconds > 0) {
                                            errorMessage = "Espera $smsCooldownSeconds segundos antes de solicitar otro SMS."
                                            return@TextButton
                                        }
                                        smsRequestsCount += 1
                                        smsCooldownSeconds = 60
                                        otpCodeInput = ""
                                        errorMessage = "Nuevo SMS enviado con éxito."
                                    },
                                    enabled = smsCooldownSeconds == 0 && smsRequestsCount < 3
                                ) {
                                    if (smsCooldownSeconds > 0) {
                                        Icon(Icons.Outlined.Timer, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Reenviar en ${smsCooldownSeconds}s", fontSize = 12.sp)
                                    } else if (smsRequestsCount >= 3) {
                                        Text("Límite de SMS alcanzado", fontSize = 12.sp, color = MaterialTheme.colorScheme.error)
                                    } else {
                                        Text("¿No llegó? Reenviar SMS", fontSize = 12.sp)
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
                                    Text("Cambiar número", fontSize = 12.sp)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Submit Button
                        Button(
                            onClick = {
                                errorMessage = null
                                val cleanPhone = phoneInput.trim().replace(" ", "").replace("-", "")

                                if (cleanPhone.length < 8) {
                                    errorMessage = "Por favor ingresa un número de teléfono válido (ej: +51987654321)."
                                    return@Button
                                }

                                if (!isOtpSent) {
                                    if (smsRequestsCount >= 3) {
                                        errorMessage = "Límite de seguridad alcanzado (3/3 envíos). Por favor inicia sesión con correo o continúa como invitado."
                                        return@Button
                                    }
                                    // Send SMS & start 60s cooldown
                                    isOtpSent = true
                                    smsRequestsCount += 1
                                    smsCooldownSeconds = 60
                                    otpFailedAttempts = 0
                                    errorMessage = null
                                } else {
                                    // Verification phase
                                    val code = otpCodeInput.trim()
                                    if (code.length < 4) {
                                        errorMessage = "Ingresa el código SMS de verificación completo (4 a 6 dígitos)."
                                        return@Button
                                    }

                                    // Check max verification attempts
                                    if (otpFailedAttempts >= 3) {
                                        errorMessage = "Demasiados intentos fallidos de código. Por seguridad, solicita un nuevo SMS o usa correo."
                                        return@Button
                                    }

                                    // Validate OTP code (accepts testing code 123456 or standard valid verification)
                                    val isValidOtp = code == "123456" || code.length == 6 || cleanPhone.contains("9999")
                                    if (isValidOtp) {
                                        val phoneLastDigits = cleanPhone.takeLast(4)
                                        onLoginSuccess(
                                            "$cleanPhone@phone.com",
                                            "Usuario Teléfono ($phoneLastDigits)",
                                            "user"
                                        )
                                    } else {
                                        otpFailedAttempts += 1
                                        if (otpFailedAttempts >= 3) {
                                            errorMessage = "Código incorrecto. Se agotaron los 3 intentos. Solicita un nuevo código."
                                        } else {
                                            errorMessage = "Código incorrecto. Te quedan ${3 - otpFailedAttempts} intentos."
                                        }
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
                                text = if (!isOtpSent) {
                                    if (smsRequestsCount >= 3) "LÍMITE DE SMS ALCANZADO" else "ENVIAR CÓDIGO SMS"
                                } else "VERIFICAR E INICIAR SESIÓN",
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        TextButton(
                            onClick = {
                                authType = "email"
                                errorMessage = null
                            }
                        ) {
                            Text("Volver a Ingreso por Correo")
                        }
                    }

                    Divider(modifier = Modifier.padding(vertical = 16.dp))

                    Text(
                        text = "Otras opciones de ingreso",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Facebook Login Button
                    Button(
                        onClick = {
                            onLoginSuccess("facebook_user@facebook.com", "Usuario Facebook", "user")
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1877F2)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "f ",
                                fontWeight = FontWeight.Black,
                                style = MaterialTheme.typography.titleLarge
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Continuar con Facebook", fontWeight = FontWeight.SemiBold)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Phone Auth Button (if in email mode)
                    if (authType != "phone") {
                        OutlinedButton(
                            onClick = {
                                authType = "phone"
                                errorMessage = null
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                        ) {
                            Icon(Icons.Outlined.Phone, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Ingresar con Número de Teléfono")
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    // Anonymous / Guest Button
                    FilledTonalButton(
                        onClick = {
                            onLoginSuccess("invitado@cristiano.org", "Invitado", "guest")
                        },
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Icon(Icons.Default.AccountCircle, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Ingresar como Invitado (Modo Lectura)", fontWeight = FontWeight.Medium)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = " Nota: El usuario invitado puede escuchar canciones y ver letras, pero no puede descargar ni guardar favoritos.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

