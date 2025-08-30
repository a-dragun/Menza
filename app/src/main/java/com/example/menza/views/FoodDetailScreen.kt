package com.example.menza.views

import com.example.menza.ui.theme.Orange
import com.example.menza.viewmodels.RestaurantViewModel
import android.annotation.SuppressLint
import android.graphics.BitmapFactory
import android.util.Base64
import android.util.Log
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import com.example.menza.R
import com.example.menza.models.FoodStatus
import com.example.menza.models.Role
import com.example.menza.models.User
import com.example.menza.viewmodels.AuthViewModel
import com.example.menza.viewmodels.ReviewViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


enum class SortOrder {
    NewestFirst,
    OldestFirst
}

enum class RatingFilter {
    None,
    Critical,
    Excellent
}

@SuppressLint("StateFlowValueCalledInComposition", "DefaultLocale")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FoodDetailScreen(
    foodId: String,
    viewModel: RestaurantViewModel,
    reviewViewModel: ReviewViewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
    authViewModel: AuthViewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
    onBack: () -> Unit,
    onRateFoodClick: (String, String) -> Unit
) {
    val context = LocalContext.current
    val foods by viewModel.foods.collectAsState()
    val food = foods.find { it.id == foodId }
    val uiState by authViewModel.uiState
    val reviews by reviewViewModel.reviews.collectAsState()
    val errorMessage by reviewViewModel.errorMessage.collectAsState()

    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(foodId) {
        reviewViewModel.loadReviews(foodId)
    }

    var showStatusDialog by remember { mutableStateOf(false) }
    var selectedStatus by remember { mutableStateOf(FoodStatus.UNAVAILABLE) }
    var currentUser by remember { mutableStateOf<User?>(null) }
    var showSortMenu by remember { mutableStateOf(false) }
    var sortOrder by remember { mutableStateOf(SortOrder.NewestFirst) }
    var ratingFilter by remember { mutableStateOf(RatingFilter.None) }

    LaunchedEffect(Unit) {
        val uid = authViewModel.repository.getCurrentUserId()
        if (uid != null) {
            val result = authViewModel.repository.getUserById(uid)
            if (result.isSuccess) {
                currentUser = result.getOrNull()
            }
        }
    }

    val isRestaurantStaff = currentUser?.let { user ->
        user.role == Role.STAFF && viewModel.currentRestaurant.value?.staffIds?.contains(user.uid) == true
    } ?: false

    val isFavorite = currentUser?.favorites?.contains(foodId) ?: false
    var showFavoriteDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(food?.displayName() ?: stringResource(R.string.food_details_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                actions = {
                    if (uiState.isLoggedIn && !isRestaurantStaff) {
                        IconButton(onClick = { showFavoriteDialog = true }) {
                            Icon(
                                Icons.Default.Star,
                                contentDescription = stringResource(
                                    if (isFavorite) R.string.remove_from_favorites else R.string.add_to_favorites
                                ),
                                tint = if (isFavorite) Orange else colorScheme.tertiary
                            )
                        }
                    }
                }
            )
        },
        bottomBar = {
            BottomAppBar(
                containerColor = colorScheme.background
            ) {
                if (isRestaurantStaff && uiState.isLoggedIn) {
                    Button(
                        onClick = { showStatusDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(end = 4.dp)
                    ) {
                        Text(stringResource(R.string.change_status_button))
                    }
                    Button(
                        onClick = { showDeleteDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(start = 4.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = colorScheme.error)
                    ) {
                        Text(stringResource(R.string.delete_food_button))
                    }
                } else {
                    Button(
                        onClick = {
                            if (food != null) {
                                onRateFoodClick(food.id, food.displayName())
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.rate_food_button))
                    }
                }
            }
        }
    ) { padding ->
        if (food == null) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(colorScheme.background),
                contentAlignment = Alignment.Center,
            ) {
                Text(stringResource(R.string.food_not_found))
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .offset(y = (-10).dp)
            ) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    food.photoUrl?.let { base64String ->
                        val bytes = Base64.decode(base64String, Base64.DEFAULT)
                        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                        bitmap?.let {
                            Image(
                                bitmap = it.asImageBitmap(),
                                contentDescription = stringResource(R.string.food_name_label),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(300.dp)
                                    .clip(RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(16.dp)
                            .background(colorScheme.surface.copy(alpha = 0.7f), RoundedCornerShape(8.dp))
                            .padding(8.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.Start) {
                            Text(
                                text = food.displayName(),
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = colorScheme.onSurface
                            )
                            Text(
                                text = "${viewModel.currentRestaurant.value?.name ?: ""}, ${viewModel.currentRestaurant.value?.city ?: ""}",
                                fontSize = 14.sp,
                                color = colorScheme.onSurface
                            )
                            Text(
                                text = stringResource(R.string.price_label, food.regularPrice.toString(), food.studentPrice.toString()),
                                fontSize = 14.sp,
                                color = colorScheme.onSurface
                            )
                            var allergenList = food.allergens.joinToString(", ") { it.getDisplayName(context) }
                            if (allergenList.isEmpty()) {
                                allergenList = stringResource(R.string.no_allergen)
                            }
                            Text(
                                text = "${stringResource(R.string.allergens_label)}: $allergenList",
                                fontSize = 14.sp,
                                color = colorScheme.onSurface
                            )
                        }
                    }
                    val averageRating = if (reviews.isNotEmpty()) reviews.map { it.rating }.average() else 0.0
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(16.dp)
                            .background(colorScheme.surface.copy(alpha = 0.7f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = String.format("%.1f", averageRating),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = colorScheme.onPrimary
                            )
                            Spacer(Modifier.width(4.dp))
                            Icon(
                                Icons.Default.Star,
                                contentDescription = null,
                                tint = Orange,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                ) {
                    val statusText = stringResource(R.string.status_label)
                    val statusDisplay = food.status.getDisplayName(context)
                    val statusColor = when (food.status) {
                        FoodStatus.UNAVAILABLE -> colorScheme.error
                        FoodStatus.PREPARING -> colorScheme.primary
                        FoodStatus.SERVING -> colorScheme.secondary
                    }
                    Text(
                        text = buildAnnotatedString {
                            withStyle(style = SpanStyle(color = colorScheme.onPrimary, fontSize = 20.sp)) {
                                append("$statusText ")
                            }
                            withStyle(style = SpanStyle(color = statusColor, fontSize = 20.sp)) {
                                append(statusDisplay)
                            }
                        },
                        modifier = Modifier
                            .padding(vertical = 8.dp)
                            .fillMaxWidth()
                            .wrapContentWidth(Alignment.CenterHorizontally)
                    )
                    Spacer(Modifier.height(24.dp))
                    Box {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val ratingCount = reviews.size
                            Text(
                                text = stringResource(R.string.comments_label, ratingCount),
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                            Box {
                                IconButton(onClick = { showSortMenu = !showSortMenu }) {
                                    Icon(
                                        Icons.Default.Menu,
                                        contentDescription = stringResource(R.string.sort_reviews),
                                        tint = colorScheme.onBackground
                                    )
                                }
                                DropdownMenu(
                                    expanded = showSortMenu,
                                    onDismissRequest = { showSortMenu = false },
                                    modifier = Modifier
                                        .background(colorScheme.background)
                                        .align(Alignment.TopEnd)
                                ) {
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.newest_first), color = colorScheme.onBackground) },
                                        onClick = {
                                            sortOrder = SortOrder.NewestFirst
                                            showSortMenu = false
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.oldest_first), color = colorScheme.onBackground) },
                                        onClick = {
                                            sortOrder = SortOrder.OldestFirst
                                            showSortMenu = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        TextButton(
                            onClick = { ratingFilter = if (ratingFilter == RatingFilter.Critical) RatingFilter.None else RatingFilter.Critical },
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = if (ratingFilter == RatingFilter.Critical) colorScheme.error else colorScheme.onBackground
                            )
                        ) {
                            Text(stringResource(R.string.critical_ratings))
                        }
                        TextButton(
                            onClick = { ratingFilter = if (ratingFilter == RatingFilter.Excellent) RatingFilter.None else RatingFilter.Excellent },
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = if (ratingFilter == RatingFilter.Excellent) colorScheme.primary else colorScheme.onBackground
                            )
                        ) {
                            Text(stringResource(R.string.excellent_ratings))
                        }
                    }
                    if (errorMessage != null) {
                        Text(stringResource(R.string.error_prefix, errorMessage!!), color = colorScheme.error)
                    } else if (reviews.isEmpty()) {
                        Text(stringResource(R.string.no_comments))
                    } else {
                        val sortedAndFilteredReviews = when (sortOrder) {
                            SortOrder.NewestFirst -> reviews.sortedByDescending { it.timestamp }
                            SortOrder.OldestFirst -> reviews.sortedBy { it.timestamp }
                        }.filter { review ->
                            when (ratingFilter) {
                                RatingFilter.None -> true
                                RatingFilter.Critical -> review.rating <= 2
                                RatingFilter.Excellent -> review.rating >= 4
                            }
                        }
                        if (sortedAndFilteredReviews.isEmpty()) {
                            Text(
                                text = stringResource(R.string.no_comments),
                                color = colorScheme.onBackground
                            )
                        } else {
                            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                items(sortedAndFilteredReviews) { review ->
                                    val loading = stringResource(R.string.loading)
                                    val username = remember { mutableStateOf(loading) }
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
                                    val unknownUser = stringResource(R.string.unknown_user)
                                    val error = stringResource(R.string.error)
                                    LaunchedEffect(review.userId) {
                                        try {
                                            val userResult = authViewModel.repository.getUserById(review.userId)
                                            if (userResult.isSuccess) {
                                                val user = userResult.getOrNull()
                                                if (user != null) {
                                                    username.value = user.username
                                                } else {
                                                    username.value = unknownUser
                                                    Log.d("tag", username.value)
                                                }
                                            } else {
                                                username.value = error
                                            }
                                        } catch (e: Exception) {
                                            username.value = error
                                        }
                                    }
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(8.dp),
                                        border = BorderStroke(1.dp, colorScheme.onSurface.copy(alpha = 0.3f)),
                                        colors = CardDefaults.cardColors(containerColor = colorScheme.surface)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(IntrinsicSize.Min)
                                        ) {
                                            val color = colorScheme.background
                                            Canvas(modifier = Modifier.matchParentSize()) {
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
                                                        text = stringResource(R.string.delete_review_confirmation),
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        color = colorScheme.onBackground,
                                                        textAlign = TextAlign.Center
                                                    )
                                                    Spacer(Modifier.height(8.dp))
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceEvenly
                                                    ) {
                                                        TextButton(onClick = { showRemoveDialog = false }) {
                                                            Text(stringResource(R.string.cancel), color = colorScheme.onBackground)
                                                        }
                                                        TextButton(
                                                            onClick = {
                                                                coroutineScope.launch {
                                                                    reviewViewModel.deleteReview(foodId = foodId, reviewId = review.id)
                                                                    showRemoveDialog = false
                                                                }
                                                            }
                                                        ) {
                                                            Text(stringResource(R.string.delete_review), color = colorScheme.error)
                                                        }
                                                    }
                                                }
                                            } else {
                                                Column(modifier = Modifier.padding(12.dp)) {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        repeat(review.rating) {
                                                            Icon(
                                                                Icons.Default.Star,
                                                                contentDescription = null,
                                                                tint = Orange,
                                                                modifier = Modifier.size(18.dp)
                                                            )
                                                        }
                                                    }
                                                    review.comment?.let {
                                                        Spacer(Modifier.height(4.dp))
                                                        Text(it, fontSize = 14.sp)
                                                    }
                                                    Spacer(Modifier.height(4.dp))
                                                    Text(
                                                        text = username.value,
                                                        fontSize = 12.sp,
                                                        color = Color.Gray
                                                    )
                                                }
                                                Text(
                                                    text = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(
                                                        Date(review.timestamp)
                                                    ),
                                                    fontSize = 12.sp,
                                                    color = Color.Gray,
                                                    modifier = Modifier
                                                        .align(Alignment.BottomEnd)
                                                        .padding(12.dp)
                                                )
                                                if (review.userId == currentUser?.uid) {
                                                    IconButton(
                                                        onClick = {
                                                            showRemoveDialog = true
                                                            startFillAnimation = true
                                                        },
                                                        modifier = Modifier
                                                            .align(Alignment.TopEnd)
                                                            .padding(4.dp)
                                                            .size(20.dp)
                                                            .background(Color.White, CircleShape)
                                                            .border(1.dp, Color.Black, CircleShape)
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.Close,
                                                            contentDescription = stringResource(R.string.delete_review_content_description),
                                                            tint = Color.Black
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(24.dp))
                }
            }
        }
        if (showStatusDialog) {
            Dialog(
                onDismissRequest = { showStatusDialog = false }
            ) {
                Column(
                    modifier = Modifier
                        .background(colorScheme.background, RoundedCornerShape(16.dp))
                        .padding(16.dp)
                ) {
                    Text(
                        stringResource(R.string.select_status_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = colorScheme.onBackground
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        stringResource(R.string.status_update_note),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                    Spacer(Modifier.height(16.dp))
                    HorizontalDivider()
                    Spacer(Modifier.height(16.dp))
                    FoodStatus.entries.forEach { status ->
                        TextButton(
                            onClick = {
                                selectedStatus = status
                                viewModel.updateFoodStatus(foodId, status)
                                showStatusDialog = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(status.displayName(), color = colorScheme.onBackground)
                        }
                        HorizontalDivider()
                    }
                    Spacer(Modifier.height(8.dp))
                    TextButton(
                        onClick = { showStatusDialog = false },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text(stringResource(R.string.cancel))
                    }
                }
            }
        }
        if (showFavoriteDialog) {
            Dialog(
                onDismissRequest = { showFavoriteDialog = false }
            ) {
                Column(
                    modifier = Modifier
                        .background(colorScheme.background, RoundedCornerShape(16.dp))
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        stringResource(if (isFavorite) R.string.remove_from_favorites else R.string.add_to_favorites),
                        style = MaterialTheme.typography.titleMedium,
                        color = colorScheme.onBackground
                    )
                    Spacer(Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        TextButton(
                            onClick = { showFavoriteDialog = false }
                        ) {
                            Text(text = stringResource(R.string.cancel), color = colorScheme.error)
                        }
                        TextButton(
                            onClick = {
                                coroutineScope.launch {
                                    if (currentUser != null) {
                                        val newFavorites = if (isFavorite) {
                                            currentUser!!.favorites - foodId
                                        } else {
                                            currentUser!!.favorites + foodId
                                        }
                                        authViewModel.repository.updateFavorites(currentUser!!.uid, newFavorites)
                                        currentUser = currentUser!!.copy(favorites = newFavorites)
                                    }
                                    showFavoriteDialog = false
                                }
                            }
                        ) {
                            Text(stringResource(R.string.confirm))
                        }
                    }
                }
            }
        }
        if (showDeleteDialog) {
            Dialog(
                onDismissRequest = { showDeleteDialog = false }
            ) {
                Column(
                    modifier = Modifier
                        .background(colorScheme.background, RoundedCornerShape(16.dp))
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        stringResource(R.string.delete_food_confirmation),
                        style = MaterialTheme.typography.titleMedium,
                        color = colorScheme.onBackground
                    )
                    Spacer(Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        TextButton(
                            onClick = { showDeleteDialog = false }
                        ) {
                            Text(text = stringResource(R.string.cancel), color = colorScheme.onPrimary)
                        }
                        TextButton(
                            onClick = {
                                coroutineScope.launch {
                                    viewModel.deleteFood(foodId)
                                    showDeleteDialog = false
                                    onBack()
                                }
                            }
                        ) {
                            Text(text = stringResource(R.string.confirm), color = colorScheme.error)
                        }
                    }
                }
            }
        }
    }
}