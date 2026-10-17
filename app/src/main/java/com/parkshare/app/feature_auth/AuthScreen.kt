package com.parkshare.app.feature_auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.HomeWork
import androidx.compose.material.icons.filled.LocalParking
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

/**
 * Material 3 Authentication Screen for Parkshare.
 * Features Email/Password input, Role Selection via FilterChips,
 * dynamic Login/Sign-up toggle, state-driven session feedback, and seamless transition.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthScreen(
    modifier: Modifier = Modifier,
    viewModel: AuthViewModel = viewModel(),
    onAuthSuccess: (UserSession) -> Unit = {}
) {
    val sessionState by viewModel.sessionState.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isSignUpMode by viewModel.isSignUpMode.collectAsState()
    val selectedRole by viewModel.selectedRole.collectAsState()

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    val focusManager = LocalFocusManager.current

    if (sessionState.isAuthenticated) {
        // Authenticated Profile View
        AuthenticatedProfileView(
            session = sessionState,
            onSignOut = {
                viewModel.logout()
                email = ""
                password = ""
            },
            onContinue = { onAuthSuccess(sessionState) }
        )
    } else {
        // Sign In / Sign Up Form
        Surface(
            modifier = modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Header Brand Logo
                Box(
                    modifier = Modifier
                        .size(84.dp)
                        .background(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.LocalParking,
                        contentDescription = "ParkShare Logo",
                        modifier = Modifier.size(52.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "ParkShare",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )

                Text(
                    text = "Peer-to-Peer Parking Marketplace",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(28.dp))

                // Error Banner
                AnimatedVisibility(
                    visible = errorMessage != null,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    errorMessage?.let { errorText ->
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                text = errorText,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                }

                // Role Selection Filter Chips
                Text(
                    text = "Select Marketplace Role",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    FilterChip(
                        selected = selectedRole == UserRole.DRIVER,
                        onClick = { viewModel.selectRole(UserRole.DRIVER) },
                        label = { Text("Driver (Book Spot)") },
                        leadingIcon = {
                            if (selectedRole == UserRole.DRIVER) {
                                Icon(Icons.Default.Check, contentDescription = null, Modifier.size(18.dp))
                            } else {
                                Icon(Icons.Default.DirectionsCar, contentDescription = null, Modifier.size(18.dp))
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )

                    FilterChip(
                        selected = selectedRole == UserRole.HOST,
                        onClick = { viewModel.selectRole(UserRole.HOST) },
                        label = { Text("Host (List Space)") },
                        leadingIcon = {
                            if (selectedRole == UserRole.HOST) {
                                Icon(Icons.Default.Check, contentDescription = null, Modifier.size(18.dp))
                            } else {
                                Icon(Icons.Default.HomeWork, contentDescription = null, Modifier.size(18.dp))
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Email Input
                OutlinedTextField(
                    value = email,
                    onValueChange = {
                        email = it
                        if (errorMessage != null) viewModel.clearError()
                    },
                    label = { Text("Email Address") },
                    placeholder = { Text("e.g. user@domain.com") },
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.Email, contentDescription = "Email Icon")
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = { focusManager.moveFocus(FocusDirection.Down) }
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Password Input
                OutlinedTextField(
                    value = password,
                    onValueChange = {
                        password = it
                        if (errorMessage != null) viewModel.clearError()
                    },
                    label = { Text("Password") },
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.Lock, contentDescription = "Password Icon")
                    },
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = if (passwordVisible) "Hide password" else "Show password"
                            )
                        }
                    },
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            focusManager.clearFocus()
                            if (isSignUpMode) {
                                viewModel.signUp(email, password, selectedRole) { success ->
                                    if (success) onAuthSuccess(viewModel.sessionState.value)
                                }
                            } else {
                                viewModel.login(email, password, selectedRole) { success ->
                                    if (success) onAuthSuccess(viewModel.sessionState.value)
                                }
                            }
                        }
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Submit Button
                Button(
                    onClick = {
                        focusManager.clearFocus()
                        if (isSignUpMode) {
                            viewModel.signUp(email, password, selectedRole) { success ->
                                if (success) onAuthSuccess(viewModel.sessionState.value)
                            }
                        } else {
                            viewModel.login(email, password, selectedRole) { success ->
                                if (success) onAuthSuccess(viewModel.sessionState.value)
                            }
                        }
                    },
                    enabled = !isLoading,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text = if (isSignUpMode) {
                                "Create ${if (selectedRole == UserRole.DRIVER) "Driver" else "Host"} Account"
                            } else {
                                "Sign In as ${if (selectedRole == UserRole.DRIVER) "Driver" else "Host"}"
                            },
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Toggle between Login and Signup
                TextButton(onClick = { viewModel.toggleAuthMode() }) {
                    Text(
                        text = if (isSignUpMode) {
                            "Already have an account? Sign In"
                        } else {
                            "Don't have an account? Create one"
                        },
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Module 1 • Auth & User Profile System",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}

/**
 * Profile and Session Overview screen displayed when user is successfully authenticated.
 */
@Composable
fun AuthenticatedProfileView(
    session: UserSession,
    onSignOut: () -> Unit,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .background(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (session.role == UserRole.DRIVER) Icons.Default.DirectionsCar else Icons.Default.HomeWork,
                            contentDescription = "Role Icon",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(40.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Authenticated Session",
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = session.email,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    SuggestionChip(
                        onClick = {},
                        label = { Text("Role: ${session.role.name}") }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = onContinue,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "Enter Marketplace",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
                onClick = onSignOut,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Sign Out")
            }
        }
    }
}
