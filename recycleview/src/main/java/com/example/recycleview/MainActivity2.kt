package com.example.recycleview

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Data class for person
data class Person(
    val firstName: String,
    val lastName: String,
    val profession: String,
    val email: String,
    val avatarColor: Color
)

class MainActivity2 : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                ToggleableRecycleView()
            }
        }
    }
}

@Composable
fun ToggleableRecycleView() {
    var selectedTabIndex by remember { mutableStateOf(0) }
    val tabTitles = listOf("Fruits", "Profiles", "Add Fruit")

    // Fixed initial fruit list for "Fruits" tab
    val initialFruits = listOf(
        "Apple", "Banana", "Cherry", "Watermelon", "Orange"
    )

    // Separate mutable state for the "Add Fruit" tab's added fruits
    var addedFruits by remember {
        mutableStateOf(emptyList<String>())
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "RECYCLE VIEW",
            fontSize = 36.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp, horizontal = 16.dp)
        )

        // Tabs Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .background(Color.LightGray)
        ) {
            tabTitles.forEachIndexed { index, title ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable { selectedTabIndex = index },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = title,
                        fontSize = 18.sp,
                        fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Normal,
                        color = Color.Black
                    )
                }
            }
        }

        // Underline slider below tabs
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .background(Color.LightGray)
        ) {
            tabTitles.forEachIndexed { index, _ ->
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(if (selectedTabIndex == index) Color.Black else Color.Transparent)
                )
            }
        }

        Divider(color = Color.Gray, thickness = 1.dp)

        Spacer(modifier = Modifier.height(8.dp))

        when (selectedTabIndex) {
            0 -> FruitListScreen(initialFruits)       // fixed fruit list
            1 -> PersonListScreen(samplePersons)
            2 -> AddFruitScreen(fruits = addedFruits, onAddFruit = { newFruit ->
                if (newFruit.isNotBlank()) {
                    addedFruits = addedFruits + newFruit.trim()
                }
            })  // dynamic added fruits list
        }
    }
}

@Composable
fun FruitListScreen(fruits: List<String>) {
    if (fruits.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF0F0F0)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No fruits in the list.",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.Gray
            )
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF0F0F0))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(fruits) { fruit ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.LightGray),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Text(
                        text = fruit,
                        color = Color(0xFF333333),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    )
                }
            }
        }
    }
}

val samplePersons = listOf(
    Person("Alice", "Johnson", "Software Engineer", "alice.johnson@example.com", Color(0xFF4CAF50)),
    Person("Bob", "Smith", "Product Manager", "bob.smith@example.com", Color(0xFF2196F3)),
    Person("Carol", "Williams", "UX Designer", "carol.williams@example.com", Color(0xFFFF5722)),
    Person("David", "Brown", "QA Tester", "david.brown@example.com", Color(0xFF9C27B0)),
    Person("Eva", "Davis", "Data Scientist", "eva.davis@example.com", Color(0xFFF44336)),
    Person("Frank", "Miller", "DevOps Engineer", "frank.miller@example.com", Color(0xFF795548)),
    Person("Grace", "Lee", "Product Owner", "grace.lee@example.com", Color(0xFF3F51B5)),
    Person("Hank", "Wilson", "Technical Lead", "hank.wilson@example.com", Color(0xFFE91E63)),
    Person("Ivy", "Clark", "Business Analyst", "ivy.clark@example.com", Color(0xFF009688)),
    Person("Jack", "Walker", "Support Engineer", "jack.walker@example.com", Color(0xFFFF9800))
)

@Composable
fun PersonListScreen(persons: List<Person>) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(persons) { person ->
            PersonRow(person = person)
            Divider(color = Color.Gray, thickness = 0.5.dp, modifier = Modifier.padding(vertical = 4.dp))
        }
    }
}

@Composable
fun PersonRow(person: Person) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AvatarBox(
                firstName = person.firstName,
                lastName = person.lastName,
                backgroundColor = person.avatarColor
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${person.firstName} ${person.lastName}",
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = person.profession,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    InfoChip(text = person.email)
                }
            }
        }
    }
}

@Composable
fun AvatarBox(firstName: String, lastName: String, backgroundColor: Color) {
    val initials = buildString {
        if (firstName.isNotEmpty()) append(firstName.first().uppercaseChar())
        if (lastName.isNotEmpty()) append(lastName.first().uppercaseChar())
    }
    Box(
        modifier = Modifier
            .size(56.dp)
            .clip(CircleShape)
            .background(backgroundColor),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = initials,
            color = Color.White,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun InfoChip(text: String) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
        modifier = Modifier.padding(vertical = 2.dp)
    ) {
        Text(
            text = text,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
fun AddFruitScreen(fruits: List<String>, onAddFruit: (String) -> Unit) {
    var text by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextField(
                value = text,
                onValueChange = { text = it },
                label = { Text("Enter fruit name") },
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Button(onClick = {
                if (text.isNotBlank()) {
                    onAddFruit(text.trim())
                    text = ""
                }
            }) {
                Text("Add Fruit")
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            "Added Fruits:",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            modifier = Modifier.padding(bottom = 8.dp)
        )
        if (fruits.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("No fruits added yet.", color = Color.Gray)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(fruits) { fruit ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.LightGray),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Text(
                            text = fruit,
                            modifier = Modifier.padding(16.dp),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
        }
    }
}
