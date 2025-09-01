package com.example.menza.views

import com.example.menza.viewmodels.RestaurantViewModel
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.menza.R
import com.example.menza.models.Restaurant
import com.example.menza.models.Role
import com.example.menza.repositories.AuthRepository
import com.example.menza.viewmodels.LocationTracker
import com.example.menza.viewmodels.LocationViewModel
import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert

enum class RestaurantSortOrder {
    AlphabeticalAsc,
    AlphabeticalDesc
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RestaurantListScreen(
    viewModel: RestaurantViewModel,
    authRepository: AuthRepository = AuthRepository(),
    onRestaurantClick: (String) -> Unit,
    onAddRestaurantClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onEditRestaurantsClick: () -> Unit
) {
    val restaurants by viewModel.restaurants.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val locationViewModel: LocationViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
    LocationTracker(locationViewModel = locationViewModel)
    val userLocation by locationViewModel.userLocation.collectAsState()
    val userId = authRepository.getCurrentUserId() ?: ""
    var isAdmin by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    var showSortMenu by remember { mutableStateOf(false) }
    var sortOrder by remember { mutableStateOf(RestaurantSortOrder.AlphabeticalAsc) }
    var searchText by remember { mutableStateOf("") }

    LaunchedEffect(userId) {
        if (userId.isNotEmpty()) {
            val result = authRepository.getUserRole(userId)
            isAdmin = result.getOrNull() == Role.ADMIN
        }
    }

    LaunchedEffect(Unit) {
        viewModel.loadAllRestaurants()
    }

    LaunchedEffect(error) {
        error?.let { errorMsg ->
            Log.d("RestaurantListScreen", "Error: $errorMsg")
            snackbarHostState.showSnackbar(errorMsg)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.restaurants), fontSize = 22.sp, fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = { showMenu = !showMenu }) {
                        Icon(Icons.Default.MoreVert, contentDescription = stringResource(R.string.settings))
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false },
                        modifier = Modifier.background(MaterialTheme.colorScheme.background)
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.settings), color = MaterialTheme.colorScheme.onBackground) },
                            onClick = {
                                showMenu = false
                                onSettingsClick()
                            }
                        )
                        if (isAdmin) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.edit_restaurants), color = MaterialTheme.colorScheme.onBackground) },
                                onClick = {
                                    showMenu = false
                                    onEditRestaurantsClick()
                                }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                    actionIconContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        floatingActionButton = {
            if (isAdmin) {
                FloatingActionButton(
                    onClick = onAddRestaurantClick,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_restaurant))
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(padding)
                .padding(16.dp)
        ) {
            RoundedSearchBar(
                modifier = Modifier.padding(horizontal = 0.dp, vertical = 8.dp),
                hint = stringResource(R.string.search_restaurants),
                onTextChanged = { searchText = it }
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.End
            ) {
                Box {
                    IconButton(onClick = { showSortMenu = !showSortMenu }) {
                        Icon(
                            Icons.Default.Menu,
                            contentDescription = stringResource(R.string.sort_restaurants),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    DropdownMenu(
                        expanded = showSortMenu,
                        onDismissRequest = { showSortMenu = false },
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.background)
                            .align(Alignment.TopEnd)
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.alphabetical_asc), color = MaterialTheme.colorScheme.onBackground) },
                            onClick = {
                                sortOrder = RestaurantSortOrder.AlphabeticalAsc
                                showSortMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.alphabetical_desc), color = MaterialTheme.colorScheme.onBackground) },
                            onClick = {
                                sortOrder = RestaurantSortOrder.AlphabeticalDesc
                                showSortMenu = false
                            }
                        )
                    }
                }
            }
            if (error != null) {
                Text(stringResource(R.string.error_with_message, error ?: ""), color = MaterialTheme.colorScheme.error)
            } else if (restaurants.isEmpty() && !isLoading) {
                Text(stringResource(R.string.no_restaurants_available), color = MaterialTheme.colorScheme.onBackground)
            } else {
                val filteredAndSortedRestaurants = restaurants
                    .filter { it.name.contains(searchText, ignoreCase = true) }
                    .sortedBy { restaurant ->
                        when (sortOrder) {
                            RestaurantSortOrder.AlphabeticalAsc -> restaurant.name.lowercase()
                            RestaurantSortOrder.AlphabeticalDesc -> restaurant.name.lowercase()
                        }
                    }
                    .let { sortedList ->
                        if (sortOrder == RestaurantSortOrder.AlphabeticalDesc) sortedList.reversed() else sortedList
                    }
                if (filteredAndSortedRestaurants.isEmpty()) {
                    Text(
                        text = stringResource(R.string.no_restaurants_available),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(filteredAndSortedRestaurants) { restaurant ->
                            RestaurantCard(
                                restaurant = restaurant,
                                viewModel = viewModel,
                                onClick = { onRestaurantClick(restaurant.id) },
                                userLocation = userLocation
                            )
                        }
                    }
                }
            }
        }
    }
}


@Composable
fun RestaurantCard(
    restaurant: Restaurant,
    viewModel: RestaurantViewModel,
    onClick: () -> Unit,
    userLocation: Pair<Double, Double>? = null
) {
    var distanceText by remember { mutableStateOf<String?>(null) }
    var showDistance by remember { mutableStateOf(true) }

    LaunchedEffect(userLocation) {
        if (userLocation == null) {
            distanceText = null
            kotlinx.coroutines.delay(3000)
            showDistance = false
        } else {
            showDistance = true
            distanceText = viewModel.getDistanceToRestaurant(restaurant, userLocation)
        }
    }

    val restaurantBitmap: ImageBitmap? = remember(restaurant.imageUrl) {
        restaurant.imageUrl?.let { base64String ->
            try {
                val bytes = Base64.decode(base64String, Base64.DEFAULT)
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
            } catch (e: Exception) {
                null
            }
        }
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.onTertiary,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                if (restaurantBitmap != null) {
                    Image(
                        bitmap = restaurantBitmap,
                        contentDescription = stringResource(R.string.restaurant_photo),
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Image(
                        painter = painterResource(R.drawable.ic_launcher_foreground),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = restaurant.name,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Home,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Column {
                        Text(
                            text = "${restaurant.city}, ${restaurant.address}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (showDistance) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        if (distanceText == null) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = stringResource(R.string.loading_distance),
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            Text(
                                text = stringResource(R.string.distance, distanceText!!),
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}