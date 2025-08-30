package com.example.menza.views

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.menza.R
import com.example.menza.viewmodels.AuthViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    authViewModel: AuthViewModel = viewModel(),
    onBack: () -> Unit,
    navController: NavController
) {
    val uiState by authViewModel.uiState
    val favorites by authViewModel.favorites.collectAsState()
    val favoritesLoading by authViewModel.favoritesLoading.collectAsState()
    var showDeleteAccountDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        authViewModel.loadFavorites()
    }

    LaunchedEffect(uiState.isLoggedIn) {
        if (!uiState.isLoggedIn) {
            navController.navigate("login") {
                popUpTo(navController.graph.startDestinationId) { inclusive = true }
            }
        }
    }
    uiState.errorMessage?.let { errorMessage ->
        LaunchedEffect(errorMessage) {
            snackbarHostState.showSnackbar(
                message = errorMessage,
                duration = SnackbarDuration.Short
            )
        }
    }

    Scaffold(
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.padding(16.dp)
            ) { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.settings_title),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            )
        },
        content = { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(paddingValues)
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Top
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.LightGray)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(text = stringResource(R.string.settings_username, uiState.username))
                        Text(text = stringResource(R.string.settings_email, uiState.email))
                        Text(text = stringResource(R.string.settings_role, uiState.role))
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                Text(
                    text = stringResource(R.string.settings_food_favorites),
                    style = MaterialTheme.typography.titleMedium,
                    fontSize = 18.sp
                )

                if (favoritesLoading) {
                    CircularProgressIndicator(modifier = Modifier.padding(top = 16.dp))
                } else if (favorites.isEmpty()) {
                    Text(text = stringResource(R.string.settings_no_favorites), fontSize = 14.sp)
                } else {
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(favorites) { item ->
                            FavoriteChip(
                                item = item,
                                onRemove = { authViewModel.removeFavorite(item.foodId) }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                Button(
                    onClick = { authViewModel.logout() },
                    shape = RectangleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    )
                ) {
                    Text(stringResource(R.string.logout))
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = { showDeleteAccountDialog = true },
                    colors = ButtonDefaults.buttonColors(MaterialTheme.colorScheme.error)
                ) {
                    Text(stringResource(R.string.delete_account))
                }

                if (uiState.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.padding(top = 16.dp))
                }

                if (showDeleteAccountDialog) {
                    AlertDialog(
                        onDismissRequest = { showDeleteAccountDialog = false },
                        title = { Text(stringResource(R.string.delete_account_confirmation_title)) },
                        text = { Text(stringResource(R.string.delete_account_confirmation_message)) },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    authViewModel.deleteAccount()
                                    showDeleteAccountDialog = false
                                }
                            ) {
                                Text(
                                    stringResource(R.string.delete_account_confirm),
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showDeleteAccountDialog = false }) {
                                Text(stringResource(R.string.cancel), color = MaterialTheme.colorScheme.onBackground)
                            }
                        }
                    )
                }
            }
        }
    )
}

@Composable
fun FavoriteChip(
    item: AuthViewModel.FavoriteItem,
    onRemove: () -> Unit
) {
    var showRemoveDialog by remember { mutableStateOf(false) }
    var startFillAnimation by remember { mutableStateOf(false) }
    val fillProgress = remember { Animatable(0f) }

    LaunchedEffect(startFillAnimation) {
        if (startFillAnimation) {
            fillProgress.snapTo(0f)
            fillProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing)
            )
            startFillAnimation = false
        }
    }

    LaunchedEffect(showRemoveDialog) {
        if (!showRemoveDialog) {
            fillProgress.snapTo(0f)
        }
    }

    val bitmap = remember(item.photoUrl) {
        item.photoUrl?.let {
            try {
                val bytes = Base64.decode(it, Base64.DEFAULT)
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            } catch (e: Exception) {
                null
            }
        }
    }

    Card(
        modifier = Modifier
            .width(200.dp)
            .height(180.dp)
            .border(1.dp, Color.Gray, RoundedCornerShape(8.dp)),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            val color = MaterialTheme.colorScheme.background

            Canvas(modifier = Modifier.fillMaxSize()) {
                if (fillProgress.value > 0f) {
                    val radius = size.maxDimension * fillProgress.value
                    drawCircle(
                        color = color,
                        radius = radius,
                        center = Offset(x = size.width - 50.dp.toPx(), y = 50.dp.toPx())
                    )
                }
            }

            if (showRemoveDialog) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = stringResource(R.string.remove_food_from_favorites, item.foodName),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        TextButton(onClick = { showRemoveDialog = false }) {
                            Text(
                                text = stringResource(android.R.string.cancel),
                                color = MaterialTheme.colorScheme.onBackground,
                            )
                        }
                        TextButton(
                            onClick = {
                                onRemove()
                                showRemoveDialog = false
                            }
                        ) {
                            Text(
                                text = stringResource(R.string.remove_favorite),
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            } else {
                Column {
                    if (bitmap != null) {
                        Image(
                            painter = BitmapPainter(bitmap.asImageBitmap()),
                            contentDescription = item.foodName,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(100.dp)
                                .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(100.dp)
                                .background(Color.Gray)
                        )
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = item.foodName,
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "${item.restaurantName}, ${item.city}",
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                IconButton(
                    onClick = {
                        showRemoveDialog = true
                        startFillAnimation = true
                    },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .size(30.dp)
                        .background(Color.White, CircleShape)
                        .border(1.dp, Color.Black, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(R.string.remove_favorite_content_description),
                        tint = Color.Black
                    )
                }
            }
        }
    }
}