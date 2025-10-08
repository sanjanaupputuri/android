package com.example.dice_roller

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.random.Random

data class Dice(
    val value: Int = 1,
    val isRolling: Boolean = false,
    val color: Color = Color.White
)

data class RollResult(
    val dice1: Int,
    val dice2: Int? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val isLucky: Boolean = false
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {  // Replaced ChaosTheme with MaterialTheme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    DiceRollerScreen()
                }
            }
        }
    }
}

@Composable
fun DiceRollerScreen() {
    var dice1 by remember { mutableStateOf(Dice(value = 1)) }
    var dice2 by remember { mutableStateOf(Dice(value = 1)) }
    var rollHistory by remember { mutableStateOf(listOf<RollResult>()) }
    var isDoubleDice by remember { mutableStateOf(false) }
    var isRolling by remember { mutableStateOf(false) }
    var showLuckyMessage by remember { mutableStateOf(false) }

    fun rollSingleDie(): Int = Random.nextInt(1, 7)

    fun isLuckyRoll(dice1: Int, dice2: Int?): Boolean {
        return when {
            dice2 == null -> dice1 == 6
            dice1 == dice2 -> true
            dice1 + dice2 == 7 -> true
            dice1 + dice2 == 11 -> true
            else -> false
        }
    }

    fun rollDice() {
        if (isRolling) return
        isRolling = true
        showLuckyMessage = false
        dice1 = dice1.copy(isRolling = true)
        if (isDoubleDice) {
            dice2 = dice2.copy(isRolling = true)
        }
    }

    LaunchedEffect(isRolling) {
        if (isRolling) {
            delay(500)
            val newValue1 = rollSingleDie()
            val newValue2 = if (isDoubleDice) rollSingleDie() else null
            dice1 = dice1.copy(value = newValue1, isRolling = false)
            if (isDoubleDice && newValue2 != null) {
                dice2 = dice2.copy(value = newValue2, isRolling = false)
            }
            val rollResult = RollResult(
                dice1 = newValue1,
                dice2 = newValue2,
                isLucky = isLuckyRoll(newValue1, newValue2)
            )
            rollHistory = rollHistory + rollResult
            showLuckyMessage = rollResult.isLucky
            isRolling = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Header
        Text(
            text = "🎲 Build a Dice Game",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 16.dp),
            textAlign = TextAlign.Center
        )

        // Mode Toggle
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                FilterChip(
                    onClick = {
                        isDoubleDice = false
                        dice2 = dice2.copy(value = 1, isRolling = false)
                    },
                    label = { Text("Single Die") },
                    selected = !isDoubleDice
                )
                FilterChip(
                    onClick = { isDoubleDice = true },
                    label = { Text("Double Dice") },
                    selected = isDoubleDice
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Dice Display
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            DiceDisplay(dice = dice1)
            if (isDoubleDice) {
                Spacer(modifier = Modifier.width(32.dp))
                DiceDisplay(dice = dice2)
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Roll Button
        Button(
            onClick = { rollDice() },
            enabled = !isRolling,
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = if (isRolling) "Rolling..." else "🎲 ROLL DICE",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Lucky Message
        if (showLuckyMessage) {
            LuckyNumberEffect()
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Statistics
        if (rollHistory.isNotEmpty()) {
            RollStatistics(rollHistory)
            Spacer(modifier = Modifier.height(16.dp))
            // Roll History
            RollHistorySection(rollHistory) {
                rollHistory = emptyList()
            }
        }
    }
}

@Composable
fun DiceDisplay(dice: Dice, modifier: Modifier = Modifier) {
    val animatedRotation by animateFloatAsState(
        targetValue = if (dice.isRolling) 720f else 0f,
        animationSpec = tween(500),
        label = "rotation"
    )
    val animatedColor by animateColorAsState(
        targetValue = if (dice.isRolling) Color(0xFFFF6B6B) else Color.White,
        animationSpec = tween(300),
        label = "color"
    )
    Box(
        modifier = modifier
            .size(100.dp)
            .rotate(animatedRotation)
            .background(
                color = animatedColor,
                shape = RoundedCornerShape(16.dp)
            )
            .border(
                width = 3.dp,
                color = Color.Gray,
                shape = RoundedCornerShape(16.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        DiceDots(value = dice.value)
    }
}

@Composable
fun DiceDots(value: Int) {
    Box(modifier = Modifier.size(80.dp)) {
        when (value) {
            1 -> {
                Dot(Alignment.Center)
            }
            2 -> {
                Dot(Alignment.TopStart)
                Dot(Alignment.BottomEnd)
            }
            3 -> {
                Dot(Alignment.TopStart)
                Dot(Alignment.Center)
                Dot(Alignment.BottomEnd)
            }
            4 -> {
                Dot(Alignment.TopStart)
                Dot(Alignment.TopEnd)
                Dot(Alignment.BottomStart)
                Dot(Alignment.BottomEnd)
            }
            5 -> {
                Dot(Alignment.TopStart)
                Dot(Alignment.TopEnd)
                Dot(Alignment.Center)
                Dot(Alignment.BottomStart)
                Dot(Alignment.BottomEnd)
            }
            6 -> {
                Dot(Alignment.TopStart)
                Dot(Alignment.TopEnd)
                Dot(Alignment.CenterStart)
                Dot(Alignment.CenterEnd)
                Dot(Alignment.BottomStart)
                Dot(Alignment.BottomEnd)
            }
        }
    }
}

@Composable
fun BoxScope.Dot(alignment: Alignment) {
    Box(
        modifier = Modifier
            .size(10.dp)
            .background(Color.Black, CircleShape)
            .align(alignment)
    )
}

@Composable
fun LuckyNumberEffect() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF4CAF50).copy(alpha = 0.9f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "🍀 LUCKY ROLL! 🍀",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = "Fortune smiles upon you!",
                fontSize = 16.sp,
                color = Color.White.copy(alpha = 0.9f)
            )
        }
    }
}

@Composable
fun RollStatistics(history: List<RollResult>) {
    val totalRolls = history.size
    val luckyRolls = history.count { it.isLucky }
    val luckyPercentage = if (totalRolls > 0) (luckyRolls * 100) / totalRolls else 0
    val averageRoll = if (history.isNotEmpty()) {
        history.map {
            it.dice1 + (it.dice2 ?: 0)
        }.average()
    } else 0.0
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "📊 Statistics",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatItem("Total Rolls", totalRolls.toString())
                StatItem("Lucky Rolls", luckyRolls.toString())
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatItem("Lucky %", "$luckyPercentage%")
                StatItem("Average", String.format("%.1f", averageRoll))
            }
        }
    }
}

@Composable
fun StatItem(label: String, value: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = label,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun RollHistorySection(history: List<RollResult>, onClear: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "📜 Recent Rolls",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                TextButton(onClick = onClear) {
                    Text("Clear")
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            LazyColumn(
                modifier = Modifier.height(200.dp)
            ) {
                items(history.takeLast(10).reversed()) { roll ->
                    RollHistoryItem(roll)
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }
        }
    }
}

@Composable
fun RollHistoryItem(roll: RollResult) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (roll.isLucky)
                Color(0xFF4CAF50).copy(alpha = 0.1f)
            else
                MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "🎲",
                    fontSize = 16.sp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = buildString {
                        append(roll.dice1)
                        roll.dice2?.let { append(" + $it = ${roll.dice1 + it}") }
                    },
                    fontSize = 16.sp,
                    fontWeight = if (roll.isLucky) FontWeight.Bold else FontWeight.Normal
                )
            }
            if (roll.isLucky) {
                Text("🍀", fontSize = 16.sp)
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DiceRollerScreenPreview() {
    MaterialTheme {  // Replaced ChaosTheme with MaterialTheme
        DiceRollerScreen()
    }
}
