package com.example.menza.views

import com.example.menza.viewmodels.RestaurantViewModel
import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.menza.R
import com.example.menza.models.Food
import com.example.menza.models.FoodStatus
import com.example.menza.repositories.AuthRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FoodSearchScreen(
    viewModel: RestaurantViewModel,
    authRepository: AuthRepository = AuthRepository(),
    restaurantId: String,
    onAddFoodClick: (String) -> Unit,
    onFoodClick: (String) -> Unit,
    onBack: () -> Unit
) {
    val restaurant by viewModel.currentRestaurant.collectAsState()
    val foods by viewModel.foods.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val userId = authRepository.getCurrentUserId() ?: ""
    val snackbarHostState = remember { SnackbarHostState() }
    var searchText by remember { mutableStateOf("") }
    var selectedTagIndexes by remember { mutableStateOf(setOf<Int>()) }
    var selectedStatusFilters by remember { mutableStateOf(setOf<FoodStatus>()) }

    val context = LocalContext.current
    val allTags = foods.flatMap { it.tags.map { tag -> tag.getDisplayName(context) } }.distinct()

    LaunchedEffect(restaurantId) { viewModel.loadRestaurantById(restaurantId) }
    LaunchedEffect(restaurantId) {
        searchText = ""
        selectedTagIndexes = setOf()
        selectedStatusFilters = setOf()
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(restaurant?.name ?: stringResource(R.string.restaurant_not_found)) },
                navigationIcon = {
                    IconButton(onClick = {
                        viewModel.clearRestaurantData()
                        onBack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                }
            )
        },
        floatingActionButton = {
            if (restaurant?.staffIds?.contains(userId) == true && restaurant?.id == restaurantId) {
                FloatingActionButton(
                    onClick = { onAddFoodClick(restaurantId) },
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_food))
                }
            }
        }
    ) { padding ->
        if (isLoading || restaurant?.id != restaurantId) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(48.dp),
                    color = MaterialTheme.colorScheme.primary,
                    strokeWidth = 4.dp
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .background(MaterialTheme.colorScheme.background)
            ) {
                RoundedSearchBar(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    hint = stringResource(R.string.search_food_hint),
                    onTextChanged = { searchText = it }
                )

                ChipsRowUI(
                    chipItems = allTags,
                    selectedChipIndexes = selectedTagIndexes,
                    onChipSelected = { index ->
                        selectedTagIndexes = if (selectedTagIndexes.contains(index)) {
                            selectedTagIndexes - index
                        } else {
                            selectedTagIndexes + index
                        }
                    }
                )
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(FoodStatus.entries.filter { it != FoodStatus.UNAVAILABLE }) { status ->
                        FilterChip(
                            selected = selectedStatusFilters.contains(status),
                            onClick = {
                                selectedStatusFilters = if (selectedStatusFilters.contains(status)) {
                                    selectedStatusFilters - status
                                } else {
                                    selectedStatusFilters + status
                                }
                            },
                            label = { Text(status.getDisplayName(context)) },
                            leadingIcon = if (selectedStatusFilters.contains(status)) {
                                {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        modifier = Modifier.size(FilterChipDefaults.IconSize)
                                    )
                                }
                            } else null,
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = Color.Black
                            )
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))

                @Suppress("KotlinConstantConditions")
                when {
                    error != null -> Text(error!!, color = MaterialTheme.colorScheme.error)
                    restaurant == null -> Text(stringResource(R.string.restaurant_not_found))
                    else -> {
                        val selectedTags = selectedTagIndexes.map { allTags[it] }
                        var filteredFoods = foods.filter { food ->
                            (searchText.isEmpty() || food.displayName().contains(searchText, ignoreCase = true)) &&
                                    (selectedTags.isEmpty() || food.tags.any { it.displayName() in selectedTags }) &&
                                    (selectedStatusFilters.isEmpty() || food.status in selectedStatusFilters)
                        }

                        filteredFoods = filteredFoods.sortedBy {
                            when (it.status) {
                                FoodStatus.SERVING -> 1
                                FoodStatus.PREPARING -> 2
                                FoodStatus.UNAVAILABLE -> 3
                            }
                        }

                        if (filteredFoods.isEmpty()) {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text(stringResource(R.string.no_foods_available))
                            }
                        } else {
                            LazyVerticalGrid(
                                columns = GridCells.Adaptive(minSize = 140.dp),
                                contentPadding = PaddingValues(8.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(filteredFoods) { food ->
                                    FoodItemCard(food = food) { onFoodClick(food.id) }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FoodItemCard(food: Food, onClick: () -> Unit) {
    val context = LocalContext.current
    val statusColor = when (food.status) {
        FoodStatus.UNAVAILABLE -> MaterialTheme.colorScheme.error
        FoodStatus.PREPARING -> MaterialTheme.colorScheme.primary
        FoodStatus.SERVING -> MaterialTheme.colorScheme.secondary
    }

    Card(
        modifier = Modifier
            .clickable { onClick() }
            .fillMaxWidth()
            .height(220.dp),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(4.dp),
        colors = CardColors(
            containerColor = MaterialTheme.colorScheme.background,
            contentColor = MaterialTheme.colorScheme.onBackground,
            disabledContentColor = MaterialTheme.colorScheme.onPrimary,
            disabledContainerColor = MaterialTheme.colorScheme.onPrimary
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 8.dp)
        ) {
            if (food.photoUrl != null) {
                val bytes = Base64.decode(food.photoUrl, Base64.DEFAULT)
                val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = food.displayName(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                        contentScale = ContentScale.Crop
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = Color.Gray)
                }
            }

            Spacer(Modifier.height(8.dp))
            Text(
                text = food.displayName(),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .padding(horizontal = 8.dp)
                    .fillMaxWidth()
                    .weight(1f),
            )
            Text(
                text = food.status.getDisplayName(context),
                style = MaterialTheme.typography.bodySmall,
                color = statusColor,
                modifier = Modifier
                    .padding(horizontal = 8.dp, vertical = 4.dp)
                    .fillMaxWidth()
            )
        }
    }
}

@Composable
fun RoundedSearchBar(
    modifier: Modifier = Modifier,
    hint: String,
    onTextChanged: (String) -> Unit = {}
) {
    var searchText by remember { mutableStateOf("") }

    TextField(
        value = searchText,
        onValueChange = {
            searchText = it
            onTextChanged(it)
        },
        placeholder = { Text(text = hint, color = Color.Gray) },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
        shape = RoundedCornerShape(50),
        singleLine = true,
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.onTertiary,
            unfocusedContainerColor = MaterialTheme.colorScheme.onTertiary,
            disabledContainerColor = MaterialTheme.colorScheme.onTertiary,
            focusedTextColor = MaterialTheme.colorScheme.onSurface,
            unfocusedTextColor = MaterialTheme.colorScheme.onSurface
        )
    )
}

@Composable
fun ChipsRowUI(
    chipItems: List<String>,
    selectedChipIndexes: Set<Int>,
    onChipSelected: (Int) -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        itemsIndexed(chipItems) { index, label ->
            val isSelected = selectedChipIndexes.contains(index)
            FilterChip(
                selected = isSelected,
                onClick = { onChipSelected(index) },
                label = { Text(label) },
                leadingIcon = if (isSelected) {
                    {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(FilterChipDefaults.IconSize)
                        )
                    }
                } else null,
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = Color.Black
                )
            )
        }
    }
}