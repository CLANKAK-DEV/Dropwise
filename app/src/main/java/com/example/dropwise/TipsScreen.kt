package com.example.dropwise

import android.content.Intent
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dropwise.BuildConfig
import com.google.ai.client.generativeai.GenerativeModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// Data class to structure the support request
data class SupportRequest(
    val from: String,
    val content: String,
    val objective: String
) {
    // Function to format the support request into a structured email body
    fun toEmailBody(): String {
        return """
            Support Request:
            
            From: $from
            
            Content: $content
            
            Objective: $objective
        """.trimIndent()
    }
}

@Composable
fun TipsScreen(activity: ComponentActivity) {
    val scope = rememberCoroutineScope()
    var messages by remember { mutableStateOf(listOf<Pair<String, Boolean>>()) } // (message, isUser)
    var inputText by remember { mutableStateOf("") }
    var showSupportForm by remember { mutableStateOf(false) } // State to show/hide form
    var supportRequest by remember { mutableStateOf(SupportRequest("", "", "")) }
    val listState = rememberLazyListState()

    // Initial welcome message on screen open
    LaunchedEffect(Unit) {
        delay(500) // Small delay for effect
        val welcomeMessages = listOf(
            "We are online!" to false,
            "Welcome to our Hydration Chatbot! 😊 How can I help you?" to false
        )
        messages = welcomeMessages
        scope.launch {
            listState.animateScrollToItem(0)
        }
    }

    val gradientBrush = Brush.verticalGradient(
        colors = listOf(Color(0xFF4A90E2), Color(0xFFB3E5FC))
    )

    Scaffold(containerColor = Color.Transparent) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(gradientBrush)
                .padding(paddingValues),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Hydration Chatbot",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(16.dp)
            )

            // Chat messages
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(8.dp),
                reverseLayout = false
            ) {
                items(messages) { (message, isUser) ->
                    AnimatedVisibility(
                        visible = true,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut()
                    ) {
                        ChatMessage(
                            message = message,
                            isUser = isUser,
                            isHydrationTip = message.contains("Here are some simple hydration tips:") || message.contains("More hydration tips:"),
                            onShareClick = {
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, message)
                                }
                                try {
                                    activity.startActivity(Intent.createChooser(shareIntent, "Share hydration tips via..."))
                                } catch (e: Exception) {
                                    Log.e("ChatMessage", "Failed to open share intent: ${e.message}", e)
                                }
                            }
                        )
                    }
                }
            }

            // Support Form (shown when "Support" is clicked)
            AnimatedVisibility(
                visible = showSupportForm,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                SupportForm(
                    supportRequest = supportRequest,
                    onSupportRequestChange = { supportRequest = it },
                    onSubmit = {
                        scope.launch {
                            val newMessages = messages.toMutableList()
                            newMessages.add("I've submitted a support request:\nContent: ${supportRequest.content}\nObjective: ${supportRequest.objective}" to true)
                            messages = newMessages
                            listState.animateScrollToItem(newMessages.size - 1)

                            newMessages.add("Opening your email client to send the support request..." to false)
                            messages = newMessages
                            listState.animateScrollToItem(newMessages.size - 1)

                            // Open email client directly with structured data
                            val emailIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "message/rfc822"
                                putExtra(Intent.EXTRA_EMAIL, arrayOf("choukerlahoucine@gmail.com"))
                                putExtra(Intent.EXTRA_SUBJECT, "Hydration Chatbot Support Request")
                                putExtra(Intent.EXTRA_TEXT, supportRequest.toEmailBody())
                            }
                            try {
                                activity.startActivity(Intent.createChooser(emailIntent, "Send email..."))
                                newMessages.add("Email client opened. Please send the email to complete your request." to false)
                                supportRequest = SupportRequest("", "", "")
                                showSupportForm = false
                            } catch (e: Exception) {
                                Log.e("SupportForm", "Failed to open email client: ${e.message}", e)
                                newMessages.add("Failed to open email client. Please contact support manually at choukerlahoucine@gmail.com." to false)
                            }
                            messages = newMessages
                            listState.animateScrollToItem(newMessages.size - 1)
                        }
                    },
                    onCancel = {
                        showSupportForm = false
                        supportRequest = SupportRequest("", "", "")
                    }
                )
            }

            // Quick action buttons (only shown when form is not visible)
            if (!showSupportForm) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    QuickActionButton(
                        text = "Support",
                        onClick = { showSupportForm = true }
                    )
                    QuickActionButton(
                        text = "Conseils d'hydratation",
                        onClick = {
                            sendSimpleHydrationTips(messages, listState, scope, { updatedMessages -> messages = updatedMessages }, inputText) { newInput ->
                                inputText = newInput
                            }
                        }
                    )

                }
            }

            // Input & send button (only shown when form is not visible)
            if (!showSupportForm) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        label = { Text("Ask about hydration...") },
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 8.dp),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White,
                            focusedIndicatorColor = Color(0xFF4A90E2),
                            unfocusedIndicatorColor = Color(0xFFB3E5FC)
                        )
                    )

                    Button(
                        onClick = {
                            if (inputText.isNotBlank()) {
                                val currentInput = inputText
                                inputText = ""
                                sendMessage(currentInput, messages, listState, scope) { updatedMessages ->
                                    messages = updatedMessages
                                }
                            }
                        },
                        enabled = inputText.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4A90E2)),
                        modifier = Modifier.size(56.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Send, contentDescription = "Send", tint = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
fun SupportForm(
    supportRequest: SupportRequest,
    onSupportRequestChange: (SupportRequest) -> Unit,
    onSubmit: () -> Unit,
    onCancel: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Support Request",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF4A90E2),
                modifier = Modifier.padding(bottom = 16.dp)
            )

            OutlinedTextField(
                value = supportRequest.from,
                onValueChange = { onSupportRequestChange(supportRequest.copy(from = it)) },
                label = { Text("From (Email)") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    focusedIndicatorColor = Color(0xFF4A90E2),
                    unfocusedIndicatorColor = Color(0xFFB3E5FC)
                )
            )

            OutlinedTextField(
                value = supportRequest.content,
                onValueChange = { onSupportRequestChange(supportRequest.copy(content = it)) },
                label = { Text("Content (Problem/Explanation)") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                maxLines = 3,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    focusedIndicatorColor = Color(0xFF4A90E2),
                    unfocusedIndicatorColor = Color(0xFFB3E5FC)
                )
            )

            OutlinedTextField(
                value = supportRequest.objective,
                onValueChange = { onSupportRequestChange(supportRequest.copy(objective = it)) },
                label = { Text("Objective") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                maxLines = 2,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    focusedIndicatorColor = Color(0xFF4A90E2),
                    unfocusedIndicatorColor = Color(0xFFB3E5FC)
                )
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Button(
                    onClick = onCancel,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB0BEC5)),
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp)
                ) {
                    Text("Cancel", color = Color.White)
                }
                Button(
                    onClick = {
                        if (supportRequest.from.isNotBlank() && supportRequest.content.isNotBlank() && supportRequest.objective.isNotBlank()) {
                            onSubmit()
                        }
                    },
                    enabled = supportRequest.from.isNotBlank() && supportRequest.content.isNotBlank() && supportRequest.objective.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4A90E2)),
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 8.dp)
                ) {
                    Text("Submit", color = Color.White)
                }
            }
        }
    }
}

fun sendMessage(
    userInput: String,
    messages: List<Pair<String, Boolean>>,
    listState: LazyListState,
    scope: CoroutineScope,
    updateMessages: (List<Pair<String, Boolean>>) -> Unit
) {
    scope.launch {
        val newMessages = messages.toMutableList()
        newMessages.add(userInput to true) // Add user message
        updateMessages(newMessages)

        // Auto-scroll to bottom
        delay(100)
        listState.animateScrollToItem(newMessages.size - 1)

        // Show "Typing..." immediately
        newMessages.add("Typing..." to false)
        updateMessages(newMessages)
        listState.animateScrollToItem(newMessages.size - 1)

        try {
            val model = GenerativeModel("gemini-1.5-flash", BuildConfig.API_KEY)
            val prompt = """
                You are a hydration chatbot. Provide a short, helpful response to: "$userInput".
                Focus on hydration tips, daily water goals, and friendly health advice.
            """.trimIndent()

            val response = model.generateContent(prompt)?.text?.trim()
                ?: "I couldn't understand that. Try asking about hydration tips!"

            // Remove "Typing..." and replace with actual response
            newMessages.removeLast()
            newMessages.add(response to false)
            updateMessages(newMessages)

            // Auto-scroll again
            listState.animateScrollToItem(newMessages.size - 1)
        } catch (e: Exception) {
            Log.e("TipsScreen", "Error fetching response: ${e.message}", e)
            newMessages.removeLast()
            newMessages.add("Error getting response. Try again!" to false)
            updateMessages(newMessages)
        }
    }
}

fun sendPredefinedMessage(
    message: String,
    messages: List<Pair<String, Boolean>>,
    listState: LazyListState,
    scope: CoroutineScope,
    updateMessages: (List<Pair<String, Boolean>>) -> Unit
) {
    scope.launch {
        val newMessages = messages.toMutableList()
        newMessages.add(message to true) // Add predefined user message
        updateMessages(newMessages)

        // Auto-scroll to bottom
        delay(100)
        listState.animateScrollToItem(newMessages.size - 1)

        // Show "Typing..." immediately
        newMessages.add("Typing..." to false)
        updateMessages(newMessages)
        listState.animateScrollToItem(newMessages.size - 1)

        try {
            val model = GenerativeModel("gemini-1.5-flash", BuildConfig.API_KEY)
            val prompt = """
                You are a hydration chatbot. Provide a short, helpful response to: "$message".
                Focus on hydration tips, daily water goals, and friendly health advice.
            """.trimIndent()

            val response = model.generateContent(prompt)?.text?.trim()
                ?: "I couldn't understand that. Try asking about hydration tips!"

            // Remove "Typing..." and replace with actual response
            newMessages.removeLast()
            newMessages.add(response to false)
            updateMessages(newMessages)

            // Auto-scroll again
            listState.animateScrollToItem(newMessages.size - 1)
        } catch (e: Exception) {
            Log.e("TipsScreen", "Error fetching response: ${e.message}", e)
            newMessages.removeLast()
            newMessages.add("Error getting response. Try again!" to false)
            updateMessages(newMessages)
        }
    }
}

// Updated function to provide simple hydration tips with more options
fun sendSimpleHydrationTips(
    messages: List<Pair<String, Boolean>>,
    listState: LazyListState,
    scope: CoroutineScope,
    updateMessages: (List<Pair<String, Boolean>>) -> Unit,
    inputText: String,
    onInputTextChange: (String) -> Unit
) {
    scope.launch {
        val newMessages = messages.toMutableList()
        newMessages.add("Give me some hydration tips." to true) // Add user message
        updateMessages(newMessages)

        // Auto-scroll to bottom
        delay(100)
        listState.animateScrollToItem(newMessages.size - 1)

        // Show "Typing..." immediately
        newMessages.add("Typing..." to false)
        updateMessages(newMessages)
        listState.animateScrollToItem(newMessages.size - 1)

        // Provide initial simple hydration tips
        var tips = """
            Here are some simple hydration tips:
            - Drink a glass of water first thing in the morning.
            - Keep a water bottle with you throughout the day.
            - Add a slice of lemon or cucumber for flavor.
            - Set reminders to drink every hour.
            - Drink water before and after meals.
            Want more tips? Type 'More tips!' and send.
        """.trimIndent()

        // Remove "Typing..." and replace with tips
        newMessages.removeLast()
        newMessages.add(tips to false)
        updateMessages(newMessages)

        // Auto-scroll again
        listState.animateScrollToItem(newMessages.size - 1)

        // Listen for "More tips!" response
        while (true) {
            delay(1000) // Check every second for new input
            if (inputText == "More tips!") {
                onInputTextChange("") // Clear the input text
                newMessages.add("More tips!" to true)
                updateMessages(newMessages)
                listState.animateScrollToItem(newMessages.size - 1)

                newMessages.add("Typing..." to false)
                updateMessages(newMessages)
                listState.animateScrollToItem(newMessages.size - 1)
                delay(1000)

                tips = """
                    More hydration tips:
                    - Carry a reusable water bottle everywhere.
                    - Drink water during breaks or exercise.
                    - Eat water-rich foods like fruits and vegetables.
                    - Avoid sugary drinks; stick to water.
                    - Track your daily intake with a journal.
                    Want even more tips? Type 'More tips!' and send.
                """.trimIndent()

                newMessages.removeLast()
                newMessages.add(tips to false)
                updateMessages(newMessages)
                listState.animateScrollToItem(newMessages.size - 1)
            }
        }
    }
}

@Composable
fun ChatMessage(
    message: String,
    isUser: Boolean,
    isHydrationTip: Boolean,
    onShareClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (isUser) Color(0xFFBBDEFB) else Color.White
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = message,
                fontSize = 16.sp,
                color = if (isUser) Color(0xFF0D47A1) else Color(0xFF424242),
                modifier = Modifier.padding(12.dp),
                textAlign = if (isUser) TextAlign.End else TextAlign.Start
            )
        }
        // Show share button only for bot's hydration tips
        if (!isUser && isHydrationTip) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
            ) {
                IconButton(
                    onClick = onShareClick,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Share",
                        tint = Color(0xFF4A90E2)
                    )
                }
            }
        }
    }
}

@Composable
fun QuickActionButton(text: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4A90E2)),
        modifier = Modifier
            .padding(4.dp)
            .height(40.dp),
        shape = RoundedCornerShape(20.dp)
    ) {
        Text(
            text = text,
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
    }
}