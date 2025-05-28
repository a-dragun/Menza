package com.example.menza.views

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.menza.ui.theme.backgroundColor
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.tooling.preview.Preview

@Preview(showBackground = true)
@Composable
fun FoodDetailScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        // Image placeholder (top section)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .background(backgroundColor),
            contentAlignment = Alignment.BottomStart
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Hrana 1", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Text("Kategorija", color = Color.White)
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = { /* TODO */ },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                    shape = RoundedCornerShape(20.dp),
                ) {
                    Icon(Icons.Default.Star, contentDescription = null, tint = Color.Black)
                    Spacer(Modifier.width(8.dp))
                    Text("Dodaj u omiljeno", color = Color.Black)
                }
            }
        }

        // Info Section
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Poslužuje se u Menza 1", fontWeight = FontWeight.Bold)
            Text("Unutar 500m", style = MaterialTheme.typography.bodySmall)

            Spacer(Modifier.height(16.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = {},
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB4E4C2)),
                    shape = RoundedCornerShape(50)
                ) {
                    Text("Pozitivno", color = Color.Black)
                }
                Button(
                    onClick = {},
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFC2C2)),
                    shape = RoundedCornerShape(50)
                ) {
                    Text("Kritično", color = Color.Black)
                }
            }

            Spacer(Modifier.height(16.dp))

            // List items
            repeat(3) {
                ReviewListItem()
                Spacer(Modifier.height(12.dp))
            }

            Spacer(Modifier.height(16.dp))

            // Komentiraj Button
            Button(
                onClick = {},
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFA1D99B))
            ) {
                Text("Komentiraj", color = Color.Black)
            }
        }
    }
}

@Composable
fun ReviewListItem() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF5F5F5), RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(Color.Gray, RoundedCornerShape(8.dp))
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text("List item", fontWeight = FontWeight.Bold)
            Row {
                Text("Category · $$ · 1.2 miles away", style = MaterialTheme.typography.bodySmall)
                Spacer(modifier = Modifier.width(4.dp))
            }
            Text(
                "Supporting line text lorem ipsum...",
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1
            )
            Row {
                repeat(5) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = Color(0xFFFFD700),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
