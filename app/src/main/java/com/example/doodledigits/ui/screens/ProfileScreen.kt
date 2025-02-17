package com.example.doodledigits.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.navigation.NavHostController
import coil.compose.rememberAsyncImagePainter
import com.example.doodledigits.googlesigninclient.GoogleSingInClient
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ProfileScreen(navController: NavHostController) {
    val context = LocalContext.current
    val googleSignInClient = remember { GoogleSingInClient(context) }
    val coroutineScope = rememberCoroutineScope()

    val user = googleSignInClient.getUser()
    val userId = FirebaseAuth.getInstance().currentUser?.uid
    val userPhotoUrl = user?.photoUrl?.toString()
    var userName by remember { mutableStateOf(user?.displayName ?: "Unknown User") }

    // 🔢 Статистика
    var correctCount by remember { mutableStateOf(0) }
    var incorrectCount by remember { mutableStateOf(0) }
    var skippedCount by remember { mutableStateOf(0) }
    var lastActivity by remember { mutableStateOf("No Data") }

    val db = FirebaseFirestore.getInstance()

    // 🎯 Завантаження статистики
    LaunchedEffect(userId) {
        if (userId != null) {
            db.collection("progress_tracking")
                .document(userId)
                .collection("digits")
                .get()
                .addOnSuccessListener { documents ->
                    var correct = 0
                    var incorrect = 0
                    var skipped = 0
                    var lastTimestamp: Long? = null

                    for (document in documents) {
                        val recognized = document.getBoolean("recognized") ?: false
                        val skippedEntry = document.getBoolean("skipped") ?: false
                        val timestamp = document.getLong("timestamp") ?: 0L

                        if (recognized) correct++
                        if (!recognized && !skippedEntry) incorrect++
                        if (skippedEntry) skipped++

                        if (lastTimestamp == null || timestamp > lastTimestamp) {
                            lastTimestamp = timestamp
                        }
                    }

                    correctCount = correct
                    incorrectCount = incorrect
                    skippedCount = skipped

                    lastActivity = lastTimestamp?.let {
                        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                        sdf.format(Date(it))
                    } ?: "No Data"
                }
                .addOnFailureListener {
                    Toast.makeText(context, "Failed to load statistics", Toast.LENGTH_SHORT).show()
                }
        }
    }

    // 🗑️ Функція очищення статистики
    fun clearStats() {
        if (userId != null) {
            db.collection("progress_tracking")
                .document(userId)
                .collection("digits")
                .get()
                .addOnSuccessListener { documents ->
                    for (document in documents) {
                        document.reference.delete()
                    }
                    correctCount = 0
                    incorrectCount = 0
                    skippedCount = 0
                    lastActivity = "No Data"
                    Toast.makeText(context, "Statistics cleared!", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener {
                    Toast.makeText(context, "Failed to clear statistics", Toast.LENGTH_SHORT).show()
                }
        }
    }

    Scaffold { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 🖼️ Аватар користувача
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
            ) {
                if (userPhotoUrl != null) {
                    Image(
                        painter = rememberAsyncImagePainter(userPhotoUrl),
                        contentDescription = "User Profile Photo",
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Text(
                        text = "No Profile Photo",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 🏷️ Ім'я користувача
            Text(
                text = userName,
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(24.dp))

            // 🔥 Статистика
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "📊 Your Progress",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    StatisticRow("✅ Correct Answers", correctCount, MaterialTheme.colorScheme.secondary)
                    StatisticRow("❌ Incorrect Answers", incorrectCount, MaterialTheme.colorScheme.error)
                    StatisticRow("⏭️ Skipped Numbers", skippedCount, MaterialTheme.colorScheme.tertiary)
                    StatisticRow("📅 Last Activity", lastActivity, MaterialTheme.colorScheme.onSurface)

                    Spacer(modifier = Modifier.height(16.dp))

                    // 🗑️ Кнопка очищення статистики
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                clearStats()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(text = "🗑️ Clear Stats", color = Color.White)
                    }
                }
            }
        }
    }
}

// 📊 Функція для виводу статистичних рядків
@Composable
fun StatisticRow(label: String, value: Any, color: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
            color = color
        )
        Text(
            text = value.toString(),
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
            color = color,
            textAlign = TextAlign.End
        )
    }
}
