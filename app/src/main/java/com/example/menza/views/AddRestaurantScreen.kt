package com.example.menza.views

import com.example.menza.viewmodels.RestaurantViewModel
import android.Manifest
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.menza.R
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddRestaurantScreen(
    viewModel: RestaurantViewModel,
    onRestaurantAdded: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var name by remember { mutableStateOf("") }
    var addressQuery by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var photoUri: Uri? by remember { mutableStateOf(null) }
    var photoBitmap: ImageBitmap? by remember { mutableStateOf(null) }
    var hasPermission by remember { mutableStateOf(false) }
    val isLoading by viewModel.isLoading.collectAsState()
    val serverError by viewModel.error.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    val isFormValid by remember {
        derivedStateOf { name.isNotBlank() && addressQuery.isNotBlank() && photoUri != null }
    }

    val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_IMAGES
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            photoUri = uri
            try {
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    val bytes = inputStream.readBytes()
                    val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    if (bitmap != null) {
                        photoBitmap = bitmap.asImageBitmap()
                    }
                }
                coroutineScope.launch {
                    val message = context.getString(R.string.photo_selected, uri.lastPathSegment ?: "")
                    snackbarHostState.showSnackbar(message)
                }
            } catch (e: Exception) {
                coroutineScope.launch {
                    snackbarHostState.showSnackbar(context.getString(R.string.photo_error, e.message ?: ""))
                }
            }
        } else {
            coroutineScope.launch {
                snackbarHostState.showSnackbar(context.getString(R.string.no_photo_selected))
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            hasPermission = true
            imagePickerLauncher.launch("image/*")
        } else {
            coroutineScope.launch {
                snackbarHostState.showSnackbar(context.getString(R.string.permission_required))
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.events.collectLatest { event ->
            if (event is RestaurantViewModel.Event.RestaurantCreated) {
                snackbarHostState.showSnackbar(context.getString(R.string.restaurant_added))
                onRestaurantAdded()
            }
        }
    }

    LaunchedEffect(viewModel.error) {
        viewModel.error.collectLatest { errorMsg ->
            if (errorMsg != null) {
                snackbarHostState.showSnackbar(errorMsg)
            }
        }
    }

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
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(30.dp))
                TextField(
                    value = name,
                    onValueChange = {
                        name = it
                        errorMessage = null
                    },
                    label = { Text(stringResource(R.string.restaurant_name_label)) },
                    isError = name.isBlank() && errorMessage != null,
                    modifier = Modifier.fillMaxWidth(),
                    colors = TextFieldDefaults.colors(unfocusedContainerColor = MaterialTheme.colorScheme.background, focusedContainerColor = MaterialTheme.colorScheme.background)
                )
            }
            item {
                TextField(
                    value = addressQuery,
                    onValueChange = {
                        addressQuery = it
                        errorMessage = null
                    },
                    label = { Text(stringResource(R.string.address_label)) },
                    isError = addressQuery.isBlank() && errorMessage != null,
                    modifier = Modifier.fillMaxWidth(),
                    colors = TextFieldDefaults.colors(unfocusedContainerColor = MaterialTheme.colorScheme.background, focusedContainerColor = MaterialTheme.colorScheme.background)
                )
            }
            item {
                Button(
                    onClick = { permissionLauncher.launch(permission) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = RectangleShape
                ) {
                    Text(stringResource(R.string.choose_photo_button))
                }
            }
            if (photoBitmap != null) {
                item {
                    Image(
                        bitmap = photoBitmap!!,
                        contentDescription = stringResource(R.string.restaurant_photo),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentScale = ContentScale.Crop
                    )
                }
            }
            item {
                Button(
                    onClick = {
                        if (isFormValid) {
                            viewModel.searchAddress(addressQuery)
                        } else {
                            errorMessage = when {
                                name.isBlank() -> context.getString(R.string.name_required)
                                addressQuery.isBlank() -> context.getString(R.string.address_required)
                                photoUri == null -> context.getString(R.string.photo_required)
                                else -> context.getString(R.string.fill_all_fields)
                            }
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar(errorMessage!!)
                            }
                        }
                    },
                    enabled = !isLoading && isFormValid,
                    modifier = Modifier
                        .padding(horizontal = 16.dp),
                    shape = RectangleShape
                ) {
                    Text(stringResource(R.string.search_button))
                }
            }
            if (isLoading) {
                item {
                    CircularProgressIndicator()
                }
            }
            serverError?.let {
                item {
                    Text(text = it, color = Color.Red)
                }
            }
            errorMessage?.let {
                item {
                    Text(text = it, color = Color.Red)
                }
            }
            items(searchResults) { result ->
                Button(
                    onClick = {
                        if (name.isBlank()) {
                            errorMessage = context.getString(R.string.name_required)
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar(errorMessage!!)
                            }
                        } else if (photoUri == null) {
                            errorMessage = context.getString(R.string.photo_required)
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar(errorMessage!!)
                            }
                        } else {
                            try {
                                val photoBytes = context.contentResolver.openInputStream(photoUri!!)?.readBytes()
                                viewModel.createRestaurantFromGeocoding(
                                    name,
                                    result,
                                    photoBytes,
                                )
                            } catch (e: Exception) {
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar(context.getString(R.string.photo_error, e.message ?: ""))
                                }
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    enabled = !isLoading && name.isNotBlank() && photoUri != null,
                    shape = RectangleShape
                ) {
                    Column {
                        Text(result.name, fontWeight = FontWeight.Bold)
                        Text("${result.address}, ${result.city} ${result.postcode ?: ""}")
                    }
                }
            }
        }
    }
}