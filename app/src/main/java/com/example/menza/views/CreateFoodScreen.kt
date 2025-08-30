package com.example.menza.views

import com.example.menza.viewmodels.RestaurantViewModel
import android.annotation.SuppressLint
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.menza.R
import com.example.menza.models.Allergen
import com.example.menza.models.FoodTag
import com.example.menza.repositories.AuthRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@SuppressLint("StateFlowValueCalledInComposition")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateFoodScreen(
    restaurantViewModel: RestaurantViewModel,
    restaurantId: String,
    onFoodAdded: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val authRepository = AuthRepository()

    var name by remember { mutableStateOf("") }
    var englishName by remember { mutableStateOf("") }
    var germanName by remember { mutableStateOf("") }
    var selectedAllergens by remember { mutableStateOf(setOf<Allergen>()) }
    var selectedTags by remember { mutableStateOf(setOf<FoodTag>()) }
    var regularPrice by remember { mutableStateOf("") }
    var studentPrice by remember { mutableStateOf("") }
    var photoUri: Uri? by remember { mutableStateOf(null) }
    var isLoading by remember { mutableStateOf(false) }
    var isTranslating by remember { mutableStateOf(false) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? -> photoUri = uri }

    LaunchedEffect(name) {
        if (name.isNotEmpty()) {
            isTranslating = true
            delay(500)
            try {
                val englishTranslation = restaurantViewModel.translateText(name, "en")
                val germanTranslation = restaurantViewModel.translateText(name, "de")
                if (englishTranslation.isNotEmpty() && englishTranslation != name) {
                    englishName = englishTranslation
                } else {
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar(context.getString(R.string.translation_error))
                    }
                }
                if (germanTranslation.isNotEmpty() && germanTranslation != name) {
                    germanName = germanTranslation
                } else {
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar(context.getString(R.string.translation_error))
                    }
                }
            } catch (e: Exception) {
                if (englishName.isEmpty() || germanName.isEmpty()) {
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar(context.getString(R.string.translation_error))
                    }
                }
            }
            isTranslating = false
        } else {
            englishName = ""
            germanName = ""
        }
    }

    LaunchedEffect(restaurantId) {
        if (restaurantViewModel.currentRestaurant.value == null) {
            restaurantViewModel.loadRestaurantById(restaurantId)
        }
    }

    LaunchedEffect(Unit) {
        restaurantViewModel.events.collectLatest { event ->
            if (event is RestaurantViewModel.Event.FoodCreated) {
                snackbarHostState.showSnackbar(context.getString(R.string.food_added))
                onFoodAdded()
            }
        }
    }

    LaunchedEffect(restaurantViewModel.error) {
        restaurantViewModel.error.collectLatest { errorMsg ->
            if (!errorMsg.isNullOrEmpty()) snackbarHostState.showSnackbar(errorMsg)
        }
    }

    LaunchedEffect(restaurantViewModel.isLoading) {
        restaurantViewModel.isLoading.collectLatest { loading -> isLoading = loading }
    }

    val userId = authRepository.getCurrentUserId() ?: ""

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.add_restaurant_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                if (restaurantViewModel.currentRestaurant.value == null && isLoading) {
                    CircularProgressIndicator()
                } else if (restaurantViewModel.currentRestaurant.value == null) {
                    Text(stringResource(R.string.restaurant_not_found))
                } else {
                    TextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text(stringResource(R.string.food_name_label)) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.background,
                            unfocusedContainerColor = MaterialTheme.colorScheme.background,
                            disabledContainerColor = MaterialTheme.colorScheme.background,
                            focusedTextColor = MaterialTheme.colorScheme.onBackground,
                            unfocusedTextColor = MaterialTheme.colorScheme.onBackground
                        )
                    )
                }
            }

            item {
                if (isTranslating) {
                    CircularProgressIndicator()
                } else if (name.isNotEmpty()) {
                    TextField(
                        value = englishName,
                        onValueChange = { englishName = it },
                        label = { Text("English Translation") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.background,
                            unfocusedContainerColor = MaterialTheme.colorScheme.background,
                            disabledContainerColor = MaterialTheme.colorScheme.background,
                            focusedTextColor = MaterialTheme.colorScheme.onBackground,
                            unfocusedTextColor = MaterialTheme.colorScheme.onBackground
                        )
                    )
                }
            }

            item {
                if (!isTranslating && name.isNotEmpty()) {
                    TextField(
                        value = germanName,
                        onValueChange = { germanName = it },
                        label = { Text("German Translation") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.background,
                            unfocusedContainerColor = MaterialTheme.colorScheme.background,
                            disabledContainerColor = MaterialTheme.colorScheme.background,
                            focusedTextColor = MaterialTheme.colorScheme.onBackground,
                            unfocusedTextColor = MaterialTheme.colorScheme.onBackground
                        )
                    )
                }
            }

            item {
                Text(stringResource(R.string.allergens_label), style = MaterialTheme.typography.titleMedium)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(Allergen.entries.toTypedArray()) { allergen ->
                        FilterChip(
                            selected = allergen in selectedAllergens,
                            onClick = {
                                selectedAllergens = if (allergen in selectedAllergens) {
                                    selectedAllergens - allergen
                                } else {
                                    selectedAllergens + allergen
                                }
                            },
                            label = { Text(allergen.displayName()) },
                            leadingIcon = if (allergen in selectedAllergens) {
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
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            )
                        )
                    }
                }
            }

            item {
                Text(stringResource(R.string.tags_label), style = MaterialTheme.typography.titleMedium)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(FoodTag.entries.toTypedArray()) { tag ->
                        FilterChip(
                            selected = tag in selectedTags,
                            onClick = {
                                selectedTags = if (tag in selectedTags) {
                                    selectedTags - tag
                                } else {
                                    selectedTags + tag
                                }
                            },
                            label = { Text(tag.displayName()) },
                            leadingIcon = if (tag in selectedTags) {
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
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            )
                        )
                    }
                }
            }

            item {
                TextField(
                    value = regularPrice,
                    onValueChange = { input ->
                        val filtered = input.filter { it.isDigit() || it == '.' }
                        val number = filtered.toDoubleOrNull()
                        if ((number != null && number in 0.0..9999.0) || filtered.isEmpty()) {
                            regularPrice = filtered
                        }
                    },
                    label = { Text(stringResource(R.string.regular_price_label)) },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.background,
                        unfocusedContainerColor = MaterialTheme.colorScheme.background,
                        disabledContainerColor = MaterialTheme.colorScheme.background,
                        focusedTextColor = MaterialTheme.colorScheme.onBackground,
                        unfocusedTextColor = MaterialTheme.colorScheme.onBackground
                    )
                )
            }

            item {
                TextField(
                    value = studentPrice,
                    onValueChange = { input ->
                        val filtered = input.filter { it.isDigit() || it == '.' }
                        val number = filtered.toDoubleOrNull()
                        if ((number != null && number in 0.0..9999.0) || filtered.isEmpty()) {
                            studentPrice = filtered
                        }
                    },
                    label = { Text(stringResource(R.string.student_price_label)) },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.background,
                        unfocusedContainerColor = MaterialTheme.colorScheme.background,
                        disabledContainerColor = MaterialTheme.colorScheme.background,
                        focusedTextColor = MaterialTheme.colorScheme.onBackground,
                        unfocusedTextColor = MaterialTheme.colorScheme.onBackground
                    )
                )
            }

            item {
                Button(
                    onClick = { imagePickerLauncher.launch("image/*") },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.choose_photo_button))
                }
            }

            item {
                photoUri?.let { uri ->
                    val inputStream = context.contentResolver.openInputStream(uri)
                    val bitmap = BitmapFactory.decodeStream(inputStream)
                    bitmap?.let {
                        Image(
                            bitmap = it.asImageBitmap(),
                            contentDescription = stringResource(R.string.photo_selected, ""),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                                .padding(8.dp)
                        )
                    }
                }
            }

            item {
                if (isLoading) {
                    CircularProgressIndicator()
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                val regPrice = regularPrice.toDoubleOrNull()
                                val stuPrice = studentPrice.toDoubleOrNull()
                                val photoData = photoUri?.let { uri ->
                                    context.contentResolver.openInputStream(uri)?.readBytes()
                                }

                                if (name.isNotEmpty()
                                    && regPrice != null
                                    && stuPrice != null
                                    && userId.isNotEmpty()
                                    && photoData != null
                                ) {
                                    restaurantViewModel.createFood(
                                        name = name,
                                        englishName = englishName,
                                        germanName = germanName,
                                        allergens = selectedAllergens.toList(),
                                        tags = selectedTags.toList(),
                                        regularPrice = regPrice,
                                        studentPrice = stuPrice,
                                        photoData = photoData,
                                        userId = userId
                                    )
                                } else {
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar(
                                            context.getString(R.string.fill_required_fields)
                                        )
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(stringResource(R.string.save_food))
                        }

                        Button(
                            onClick = { onBack() },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Text(stringResource(R.string.discard_food))
                        }
                    }
                }
            }
        }
    }
}
