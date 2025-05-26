package com.example.menza

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.ShapeDefaults
import androidx.compose.material3.Shapes
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.menza.ui.theme.backgroundColor
import com.example.menza.ui.theme.primaryColor
import com.example.menza.ui.theme.primaryTextColor
import com.example.menza.ui.theme.secondaryColor
import com.example.menza.ui.theme.secondaryTextColor

@Preview(showBackground = true)
@Composable
fun RegisterScreen(modifier: Modifier = Modifier) {
    Box(modifier.background(backgroundColor).fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Register",
                fontSize = 48.sp,
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(80.dp))
            Box(contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    var email by remember { mutableStateOf("") }
                    var password by remember { mutableStateOf("") }
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(0.8f),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedTextColor = primaryTextColor,
                            unfocusedLabelColor = primaryTextColor,
                            focusedLabelColor = primaryColor,
                            focusedTextColor = secondaryTextColor,
                            unfocusedBorderColor = secondaryColor,
                            focusedBorderColor = primaryColor

                        )
                    )
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Password") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(0.8f),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedTextColor = primaryTextColor,
                            unfocusedLabelColor = primaryTextColor,
                            focusedLabelColor = primaryColor,
                            focusedTextColor = secondaryTextColor,
                            unfocusedBorderColor = secondaryColor,
                            focusedBorderColor = primaryColor

                        )
                    )
                    Spacer(Modifier.height(40.dp))

                    SubmitButton({}, "Submit")
                }
            }
        }
    }
}