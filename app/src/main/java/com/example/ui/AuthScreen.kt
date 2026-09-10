package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.viewmodel.ChatViewModel

@Composable
fun AuthScreen(viewModel: ChatViewModel) {
    val username by viewModel.usernameInput.collectAsState()
    val password by viewModel.passwordInput.collectAsState()
    val displayName by viewModel.displayNameInput.collectAsState()
    val isRegister by viewModel.isRegisterMode.collectAsState()
    val authError by viewModel.authError.collectAsState()
    val isBlockedDialog by viewModel.isBlockedDialog.collectAsState()

    var passwordVisible by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        PrimaryBlue.copy(alpha = 0.08f),
                        MaterialTheme.colorScheme.background
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 440.dp)
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Brand Logo & Header
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(PrimaryBlue, AccentTeal)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.ChatBubble,
                    contentDescription = "App Logo",
                    tint = Color.White,
                    modifier = Modifier.size(38.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "DirectChat",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Text(
                text = "المراسلة الفورية ومشاركة الملفات والصوتيات",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Mode Selector (Login vs Register)
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (!isRegister) PrimaryBlue else Color.Transparent)
                                .clickable { viewModel.isRegisterMode.value = false }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "تسجيل الدخول",
                                fontWeight = if (!isRegister) FontWeight.Bold else FontWeight.Normal,
                                color = if (!isRegister) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isRegister) PrimaryBlue else Color.Transparent)
                                .clickable { viewModel.isRegisterMode.value = true }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "إنشاء حساب جديد",
                                fontWeight = if (isRegister) FontWeight.Bold else FontWeight.Normal,
                                color = if (isRegister) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Username Field
                    OutlinedTextField(
                        value = username,
                        onValueChange = { viewModel.usernameInput.value = it },
                        label = { Text("اسم المستخدم / Username") },
                        leadingIcon = {
                            Icon(Icons.Default.Person, contentDescription = "Username", tint = PrimaryBlue)
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("username_input")
                    )

                    // Display Name (Only in Register mode)
                    AnimatedVisibility(visible = isRegister) {
                        Column {
                            Spacer(modifier = Modifier.height(14.dp))
                            OutlinedTextField(
                                value = displayName,
                                onValueChange = { viewModel.displayNameInput.value = it },
                                label = { Text("الاسم الكامل / Display Name") },
                                leadingIcon = {
                                    Icon(Icons.Default.Badge, contentDescription = "Display Name", tint = PrimaryBlue)
                                },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("display_name_input")
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Password Field
                    OutlinedTextField(
                        value = password,
                        onValueChange = { viewModel.passwordInput.value = it },
                        label = { Text("كلمة السر / Password") },
                        leadingIcon = {
                            Icon(Icons.Default.Lock, contentDescription = "Password", tint = PrimaryBlue)
                        },
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = "Toggle password visibility"
                                )
                            }
                        },
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = {
                            focusManager.clearFocus()
                            if (isRegister) viewModel.register() else viewModel.login()
                        }),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("password_input")
                    )

                    // Error Message
                    if (authError != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(BlockedLight)
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.ErrorOutline,
                                contentDescription = "Error",
                                tint = BlockedRed,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = authError ?: "",
                                color = BlockedRed,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(22.dp))

                    // Submit Button
                    Button(
                        onClick = {
                            focusManager.clearFocus()
                            if (isRegister) viewModel.register() else viewModel.login()
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("auth_submit_button")
                    ) {
                        Icon(
                            imageVector = if (isRegister) Icons.Default.PersonAdd else Icons.Default.Login,
                            contentDescription = "Submit"
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isRegister) "إنشاء الحساب وبدء المراسلة" else "دخول إلى المحادثات",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Quick Demo Accounts Bar
            Text(
                text = "حسابات تجريبية جاهزة للاختبار السريع / Quick Demo Accounts:",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DemoAccountChip(
                    title = "👑 مسؤول / Admin",
                    modifier = Modifier.weight(1f),
                    color = AdminNavy
                ) {
                    viewModel.isRegisterMode.value = false
                    viewModel.usernameInput.value = "admin"
                    viewModel.passwordInput.value = "admin"
                    viewModel.login()
                }

                DemoAccountChip(
                    title = "👩 سارة / Sara",
                    modifier = Modifier.weight(1f),
                    color = Color(0xFFE91E63)
                ) {
                    viewModel.isRegisterMode.value = false
                    viewModel.usernameInput.value = "sara"
                    viewModel.passwordInput.value = "123"
                    viewModel.login()
                }

                DemoAccountChip(
                    title = "👨 عمر / Omar",
                    modifier = Modifier.weight(1f),
                    color = Color(0xFF2196F3)
                ) {
                    viewModel.isRegisterMode.value = false
                    viewModel.usernameInput.value = "omar"
                    viewModel.passwordInput.value = "123"
                    viewModel.login()
                }
            }
        }

        // Blocked User Dialog
        if (isBlockedDialog) {
            AlertDialog(
                onDismissRequest = { viewModel.isBlockedDialog.value = false },
                icon = {
                    Icon(
                        imageVector = Icons.Default.Block,
                        contentDescription = "Blocked",
                        tint = BlockedRed,
                        modifier = Modifier.size(44.dp)
                    )
                },
                title = {
                    Text(
                        text = "الحساب محظور / Account Blocked",
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                },
                text = {
                    Text(
                        text = "تم حظر هذا الحساب من قبل إدارة التطبيق لمخالفة شروط الاستخدام. لا يمكنك تسجيل الدخول أو إرسال الرسائل.",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center
                    )
                },
                confirmButton = {
                    Button(
                        onClick = { viewModel.isBlockedDialog.value = false },
                        colors = ButtonDefaults.buttonColors(containerColor = BlockedRed)
                    ) {
                        Text("حسناً / OK")
                    }
                }
            )
        }
    }
}

@Composable
fun DemoAccountChip(
    title: String,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = color.copy(alpha = 0.1f),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.3f)),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = title,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = color,
                maxLines = 1
            )
        }
    }
}
