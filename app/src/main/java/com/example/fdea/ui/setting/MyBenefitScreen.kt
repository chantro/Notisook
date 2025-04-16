package com.example.fdea.ui.setting

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideIn
import androidx.compose.animation.slideOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults.SecondaryIndicator
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import coil.compose.rememberImagePainter
import com.example.fdea.R
import com.example.fdea.data.BenefitLocation
import com.example.fdea.data.Gifticon
import com.example.fdea.login.Auth
import com.example.fdea.login.UserService
import com.example.fdea.ui.BenefitViewModel
import com.example.fdea.ui.form.CustomYesOrNoDialog
import com.example.fdea.ui.theme.DarkBlue
import com.example.fdea.ui.theme.LightBlue
import com.example.fdea.ui.theme.Orange
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import com.google.firebase.auth.FirebaseAuth
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import kotlin.math.min

@Composable
fun MyBenefitScreen(navController: NavController, viewModel: BenefitViewModel) {
    var selectedTabIndex by remember { mutableStateOf(0) }
    val tabTitles = listOf("기프티콘", "제휴 매장")

    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
        Column {
            MyTopBar(navController, "나의 혜택")
            TabRow(
                selectedTabIndex = selectedTabIndex,
                indicator = { tabPositions ->
                    SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                        color = DarkBlue
                    )
                }
            ) {
                tabTitles.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        selectedContentColor = LightBlue,
                        unselectedContentColor = Color.Red.copy(alpha = 0.8f),
                        text = { Text(title, fontSize = 22.sp, color = Color.Black) }
                    )
                }

            }
            BenefitTabContent(selectedTabIndex, navController, viewModel)
        }
    }
}

@Composable
fun BenefitTabContent(
    selectedTabIndex: Int,
    navController: NavController,
    viewModel: BenefitViewModel
) {
    when (selectedTabIndex) {
        0 -> GifticonContent(viewModel, navController)
        1 -> AffiliateStoreContent(viewModel)
    }
}

@Composable
fun GifticonContent(viewModel: BenefitViewModel, navController: NavController) {
    var showConfirmDialog by remember { mutableStateOf(false) }
    var showUploadDialog by remember { mutableStateOf(false) }
    var gifticons by remember { mutableStateOf(emptyList<Gifticon>()) }

    // Collect the current state of gifticons
    LaunchedEffect(viewModel) {
        viewModel.gifticons.collect { updatedGifticons ->
            gifticons = updatedGifticons
        }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showConfirmDialog = true },
                modifier = Modifier.padding(16.dp)
            ) {
                Icon(imageVector = Icons.Filled.Add, contentDescription = "Add Photo")
            }
        },
        floatingActionButtonPosition = FabPosition.End,
        modifier = Modifier.fillMaxSize()
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            Column {
                if (viewModel.imminentExpiryCount.value > 0) {  // 유효기간이 10일 이내 남은 것이 있을 때 박스 표시
                    ExpiredGifticonBox(navController)
                }
                GifticonGrid(gifticons, viewModel = viewModel) { gifticon ->
                    viewModel.removeGifticonImage(gifticon) {
                        gifticons = gifticons.filter { it.id != gifticon.id }
                    }
                }
            }
        }
    }

    if (showConfirmDialog) {
        ConfirmDialog(
            onConfirm = {
                showUploadDialog = true
                showConfirmDialog = false
            },
            onDismiss = { showConfirmDialog = false }
        )
    }
    if (showUploadDialog) {
        UploadDialog(viewModel, onDismiss = { showUploadDialog = false })
    }
}

@Composable
fun GifticonGrid(gifticons: List<Gifticon>, viewModel: BenefitViewModel, onRemove: (Gifticon) -> Unit) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(gifticons.size) { index ->
            val gifticon = gifticons[index]
            GifticonCard(gifticon, viewModel, onRemove)
        }
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GifticonCard(gifticon: Gifticon, viewModel: BenefitViewModel, onRemove: (Gifticon) -> Unit) {
    var showZoomDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var editedDate by remember { mutableStateOf(gifticon.expiryDate) }
    var isDateInvalid by remember { mutableStateOf(false) }
    var isConfirmUploading by remember { mutableStateOf(false) }
    val dateFormatRegex = Regex("""\d{4}\.\d{2}\.\d{2}""")

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(350.dp)  // Increased height for larger image
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = {
                        showZoomDialog = true
                    },
                    onLongPress = {
                        showDeleteDialog = true
                    }
                )
            },
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column {
            if (gifticon.imageUrl.isNotEmpty()) {
                Image(
                    painter = rememberImagePainter(gifticon.imageUrl),
                    contentDescription = "Gifticon Image",
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                )
            } else {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.weight(1f) ) {  // Match image height
                    Text("No Image", color = Color.White)
                }
            }
            //Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "유효기간: ${gifticon.expiryDate}",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
                    .background(Orange)
                    .clickable {
                        editedDate = gifticon.expiryDate
                        showEditDialog = true
                    },
                textAlign = TextAlign.Center,
                color = Color.White  // Set text color to white for contrast
            )
        }
    }

    ImageZoomDialog(
        showDialog = showZoomDialog,
        onDismiss = { showZoomDialog = false },
        imageUri = Uri.parse(gifticon.imageUrl),
        DpSize(500.dp, 600.dp),
        1
    )
    CustomYesOrNoDialog(
        showAlert = showDeleteDialog,
        onConfirm = {
            onRemove(gifticon)
            showDeleteDialog = false
        },
        onCancel = { showDeleteDialog = false },
        title = "기프티콘 삭제",
        alertMessage = "해당 기프티콘을 삭제하시겠습니까?"
    )

    if (showEditDialog) {
        Dialog(onDismissRequest = { showEditDialog = false }) {
            Card(
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DarkBlue),
            ) {
                Column(
                    modifier = Modifier
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("유효기간 수정", fontSize = 24.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    TextField(
                        value = editedDate,
                        onValueChange = { editedDate = it },
                        label = { Text("유효기간 입력(예시: 2024.09.02)", color = Color.Gray, fontSize = 16.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = TextFieldDefaults.textFieldColors(
                            focusedIndicatorColor = if (isDateInvalid) Color.Red else LightBlue,
                            unfocusedIndicatorColor = if (isDateInvalid) Color.Red else LightBlue
                        )
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Button(
                            onClick = { showEditDialog = false },
                            colors = ButtonDefaults.buttonColors(containerColor = DarkBlue)
                        ) {
                            Text("취소", color = Color.White)
                        }
                        Button(
                            onClick = {
                                if (dateFormatRegex.matches(editedDate)) {
                                    isDateInvalid = false
                                    isConfirmUploading = true
                                    viewModel.updateGifticonExpiryDate(gifticon, editedDate) {
                                        isConfirmUploading = false
                                        showEditDialog = false
                                    }
                                } else {
                                    isDateInvalid = true
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = DarkBlue)
                        ) {
                            Text("확인", color = Color.White)
                        }
                    }
                    if (isConfirmUploading) {
                        Spacer(modifier = Modifier.height(16.dp))
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                    }
                }
            }
        }
    }
}
@Composable
fun UploadDialog(viewModel: BenefitViewModel, onDismiss: () -> Unit) {
    val context = LocalContext.current
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    val pickImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri: Uri? ->
            imageUri = uri
        }
    )
    var isUploading by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = {
        if (!isUploading) onDismiss()
    }) {
        Card(
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(350.dp)
                .background(DarkBlue),
        ) {
            Column(
                modifier = Modifier
                    .padding(start = 20.dp, top = 30.dp, end = 20.dp, bottom = 30.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (imageUri != null) {
                    Image(
                        painter = rememberImagePainter(imageUri),
                        contentDescription = "Selected Image",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                    )
                } else {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_photoadd),
                        contentDescription = "갤러리추가",
                        modifier = Modifier
                            .size(75.dp)
                            .align(Alignment.CenterHorizontally)
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                // 업로드, 등록 버튼 및 로딩 표시기
                if (isUploading) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("유효기간을 확인하고 있어요", color = Color.Gray, fontSize = 16.sp)
                } else {
                    Row(
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Button(
                            onClick = { pickImageLauncher.launch("image/*") },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = DarkBlue)
                        ) {
                            Text("업로드하기", color = Color.White)
                        }
                        Spacer(modifier = Modifier.width(24.dp))
                        Button(
                            onClick = {
                                if (imageUri != null && !isUploading) {
                                    isUploading = true
                                    viewModel.registerGifticonImage(context, imageUri) {
                                        isUploading = false
                                        onDismiss()
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = DarkBlue)
                        ) {
                            Text("등록하기", color = Color.White)
                        }
                    }
                }
            }
        }
    }

}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfirmDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
            color = Color.White
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {

                Text(
                    text = "기프티콘 이미지를\n추가하시겠습니까?",
                    color = Color.Black,
                    fontSize = 20.sp,
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(DarkBlue)
                    ) {
                        Text("아니요", fontSize = 17.sp, color = Color.White)
                    }
                    Spacer(modifier = Modifier.width(46.dp))
                    Button(
                        onClick = onConfirm,
                        colors = ButtonDefaults.buttonColors(DarkBlue)
                    ) {
                        Text("네", fontSize = 17.sp, color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
fun ExpiredGifticonBox(navController: NavController) {
    val icon: Painter = painterResource(id = R.drawable.ic_arrowforward)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(10.dp)
            .height(170.dp)
            .background(LightBlue),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = "유효기간이 얼마 남지 않은\n기프티콘이 있어요!",
                color = Color.White,
                fontSize = 17.sp,
                textAlign = TextAlign.Center,
                // modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(46.dp))
            Button(
                onClick = {
                    navController.navigate("imminent_gifticons_screen")
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(Color.Transparent)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center

                ) {
                    Text(
                        text = "지금 보러가기",
                        color = Color.White,
                        fontSize = 16.sp,
                        //textAlign = TextAlign.Center,
                        //modifier = Modifier.weight(1f),
                        textDecoration = TextDecoration.Underline
                    )
                    Icon(
                        painter = icon,
                        contentDescription = "지금 보러가기",
                        tint = Color.White,
                        modifier = Modifier
                            .size(80.dp)
                    )
                }
            }

        }
    }

}

@Composable
fun AffiliateStoreContent(viewModel: BenefitViewModel) {
    val initialZoom = 17f
    val initialPosition = LatLng(37.54514694213867, 126.96501159667969)
    val context = LocalContext.current
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(initialPosition, initialZoom)
    }

    val locations by viewModel.locations.collectAsState()
    var showList by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf(TextFieldValue("")) }
    var selectedLocation by remember { mutableStateOf<LatLng?>(null) }
    var newStoreName by remember { mutableStateOf("") }
    var newStoreBenefit by remember { mutableStateOf("") }
    var editingLocation by remember { mutableStateOf<BenefitLocation?>(null) }
    var temporaryLocation by remember { mutableStateOf<LatLng?>(null) }
    var currentLocation by remember { mutableStateOf<LatLng?>(null) }

    val user = FirebaseAuth.getInstance().currentUser
    val major by UserService.major.collectAsState()
    val auth = Auth(context)
    var role by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        UserService.loadUserData()
    }

    LaunchedEffect(user) {
        user?.let {
            role = auth.getRole(it)
            role="FA"   //TODO 권한 임의 설정
            Log.d("role", role.toString())
        }
    }

    LaunchedEffect(true) {
        major?.let { viewModel.fetchLocations(it) }
        cameraPositionState.move(CameraUpdateFactory.newLatLngZoom(initialPosition, initialZoom))
        getCurrentLocation(context, fusedLocationClient) { location ->
            currentLocation = location
        }
    }


    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.align(Alignment.TopCenter)) {
            PlaceSearchView(
                query = query,
                onQueryChanged = { queryText ->
                    query = queryText
                },
                onPlaceSelected = { placeName ->
                    val request = FindAutocompletePredictionsRequest.builder()
                        .setQuery(placeName)
                        .build()
                    val client = Places.createClient(context)
                    client.findAutocompletePredictions(request)
                        .addOnSuccessListener { response ->
                            val placeId = response.autocompletePredictions.firstOrNull()?.placeId
                            if (placeId != null) {
                                val placeRequest = FetchPlaceRequest.builder(placeId, listOf(Place.Field.LAT_LNG, Place.Field.NAME)).build()
                                client.fetchPlace(placeRequest)
                                    .addOnSuccessListener { placeResponse ->
                                        val place = placeResponse.place
                                        if (place.name == placeName) {
                                            temporaryLocation = place.latLng
                                            selectedLocation = place.latLng
                                            newStoreName = placeName

                                            val existingLocation = viewModel.locations.value.find { loc ->
                                                loc.lat == place.latLng?.latitude && loc.lng == place.latLng?.longitude
                                            }
                                            if (existingLocation != null) {
                                                newStoreBenefit = existingLocation.benefit
                                                editingLocation = existingLocation
                                            } else {
                                                newStoreBenefit = ""
                                                editingLocation = null
                                            }

                                            cameraPositionState.move(CameraUpdateFactory.newLatLngZoom(
                                                place.latLng!!, cameraPositionState.position.zoom))
                                        } else {
                                            Toast.makeText(context, "검색 결과가 없습니다.", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                            } else {
                                Toast.makeText(context, "검색 결과가 없습니다.", Toast.LENGTH_SHORT).show()
                            }
                        }
                        .addOnFailureListener { exception ->
                            Toast.makeText(context, "장소 검색에 실패했습니다. ${exception.message}", Toast.LENGTH_SHORT).show()
                        }
                },
                onMenuClick = { showList = true },
                role
            )

            Spacer(modifier = Modifier.height(8.dp))

            GoogleMap(
                modifier = Modifier.weight(1f),
                cameraPositionState = cameraPositionState,
                uiSettings = MapUiSettings(zoomControlsEnabled = false)
            ) {
                // Firestore에서 불러온 모든 위치에 마커 표시
                locations.forEach { location ->
                    val isSelected = selectedLocation == LatLng(location.lat, location.lng)
                    Marker(
                        state = MarkerState(position = LatLng(location.lat, location.lng)),
                        icon = if (isSelected) BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE) else null,
                        onClick = { marker ->
                            selectedLocation = LatLng(location.lat, location.lng)
                            temporaryLocation = null
                            newStoreName = location.name
                            newStoreBenefit = location.benefit
                            editingLocation = location
                            query = TextFieldValue(location.name)
                            true // 정보창을 띄우지 않음
                        }
                    )
                }

                // 검색된 위치에 마커 표시 (일시적)
                temporaryLocation?.let {
                    Marker(
                        state = MarkerState(position = it),
                        title = newStoreName,
                        icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE),
                        onClick = { marker ->
                            viewModel.locations.value.find { loc ->
                                loc.lat == it.latitude && loc.lng == it.longitude
                            }?.let { loc ->
                                selectedLocation = LatLng(loc.lat, loc.lng)
                                newStoreName = loc.name
                                newStoreBenefit = loc.benefit
                                editingLocation = loc
                                query = TextFieldValue(loc.name)
                            } ?: run {
                                selectedLocation = LatLng(it.latitude, it.longitude)
                                newStoreBenefit = ""
                                editingLocation = null
                            }
                            true // 정보창을 띄우지 않음
                        }
                    )
                }

                // 현재 위치에 마커 표시
                currentLocation?.let {
                    Marker(
                        state = MarkerState(position = it),
                        title = "현재 위치",
                        icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_CYAN),
                        onClick = {
                            true
                        }
                    )
                }
            }
            if (selectedLocation != null || temporaryLocation != null) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White)
                        .padding(8.dp)
                ) {
                    Text(
                        text = newStoreName,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 10.dp)
                    )

                    BasicTextField(
                        value = newStoreBenefit,
                        onValueChange = { newStoreBenefit = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.White)
                            .padding(8.dp),
                        decorationBox = { innerTextField ->
                            if (newStoreBenefit.isEmpty()) {
                                Text("혜택을 입력하세요.", color = Color.Gray)
                            }
                            innerTextField()
                        }
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    if(role.equals("DA") || role.equals("FA")) {

                        Row {
                            if (editingLocation != null) {
                                Button(
                                    onClick = {
                                        viewModel.updateLocation(editingLocation!!.copy(benefit = newStoreBenefit))
                                        selectedLocation = null
                                        temporaryLocation = null
                                        newStoreBenefit = ""
                                        Toast.makeText(context, "수정이 완료되었습니다.", Toast.LENGTH_SHORT)
                                            .show()
                                    },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = DarkBlue),
                                ) {
                                    Text("수정하기")
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                Button(
                                    onClick = {
                                        viewModel.deleteLocation(editingLocation!!)
                                        selectedLocation = null
                                        temporaryLocation = null
                                        newStoreBenefit = ""
                                        Toast.makeText(context, "삭제가 완료되었습니다.", Toast.LENGTH_SHORT)
                                            .show()
                                    },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = DarkBlue)
                                ) {
                                    Text("삭제하기")
                                }
                            } else {
                                Button(
                                    onClick = {
                                        val location = major?.let {
                                            val finalMajor = if (role.equals("FA")) "FA" else it
                                            BenefitLocation(
                                                newStoreName,
                                                finalMajor,
                                                temporaryLocation!!.latitude,
                                                temporaryLocation!!.longitude,
                                                newStoreBenefit
                                            )
                                        }
                                        if (location != null) {
                                            viewModel.saveLocation(location)
                                        }
                                        selectedLocation = null
                                        temporaryLocation = null
                                        newStoreBenefit = ""
                                        Toast.makeText(context, "저장이 완료되었습니다.", Toast.LENGTH_SHORT)
                                            .show()
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = DarkBlue)
                                ) {
                                    Text("저장하기")
                                }
                            }
                        }
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = showList,
            enter = slideIn { fullSize -> IntOffset(-fullSize.width, 0) },
            exit = slideOut { fullSize -> IntOffset(-fullSize.width, 0) }
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(250.dp),
                color = LightBlue
            ) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(LightBlue),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.End
                    ) {
                        IconButton(onClick = { showList = false }) {
                            Icon(Icons.Filled.Close, contentDescription = "Close", tint = Color.White)
                        }
                    }
                    Column(modifier = Modifier.padding(8.dp)) {
                        locations.forEach { location ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedLocation = LatLng(location.lat, location.lng)
                                        temporaryLocation = null
                                        newStoreName = location.name
                                        newStoreBenefit = location.benefit
                                        editingLocation = location
                                        // 현재 줌 레벨 유지하면서 카메라 이동
                                        cameraPositionState.move(
                                            CameraUpdateFactory.newLatLngZoom(
                                                LatLng(location.lat, location.lng),
                                                cameraPositionState.position.zoom
                                            )
                                        )
                                        showList = false
                                    }
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = location.name, fontWeight = FontWeight.Bold, color = Color.White)
                                Spacer(Modifier.width(8.dp))
                                Text(text = ": ${location.benefit}", color = Color.White)
                            }
                        }
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = {
                currentLocation?.let {
                    cameraPositionState.move(CameraUpdateFactory.newLatLngZoom(it, cameraPositionState.position.zoom))
                }
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            Icon(Icons.Filled.MyLocation, contentDescription = "현재 위치")
        }
    }
}

@Composable
fun PlaceSearchView(
    query: TextFieldValue,
    onQueryChanged: (TextFieldValue) -> Unit,
    onPlaceSelected: (String) -> Unit,
    onMenuClick: () -> Unit,
    role: String?
) {
    val keyboardController = LocalSoftwareKeyboardController.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onMenuClick) {
            Icon(Icons.Filled.Menu, contentDescription = "Open list")
        }
        if(role.equals("DA") || role.equals("FA")) {
            TextField(
                value = query,
                onValueChange = { queryText ->
                    onQueryChanged(queryText)
                },
                modifier = Modifier.weight(1f),
                placeholder = { Text("장소를 검색하세요.") }
            )
            IconButton(onClick = {
                keyboardController?.hide() // 키보드 숨기기
                onPlaceSelected(query.text)
            }) {
                Icon(Icons.Filled.Search, contentDescription = "Search")
            }
        }
    }
}

fun getCurrentLocation(context: Context, fusedLocationClient: FusedLocationProviderClient, onLocationResult: (LatLng) -> Unit) {
    if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
        fusedLocationClient.lastLocation
            .addOnSuccessListener { location: Location? ->
                location?.let {
                    onLocationResult(LatLng(it.latitude, it.longitude))
                }
            }
    } else {
        ActivityCompat.requestPermissions(
            context as Activity,
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
            1
        )
    }
}

@Composable
fun ZoomControls(
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Button(onClick = onZoomIn) {
            Text("+")
        }
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = onZoomOut) {
            Text("-")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyTopBar(navController: NavController, title: String) {
    //val bottomPadding = if (title == "Form") 1.dp else 8.dp
    CenterAlignedTopAppBar(
        title = {
            Text(
                title,
                color = Color.Black,
                textAlign = TextAlign.Center,
                fontSize = 30.sp,
                modifier = Modifier.padding(top = 10.dp, bottom = 10.dp)
            )
        },
        modifier = Modifier.padding(bottom = 8.dp, top = 8.dp),
        navigationIcon = {
            if (title == "공지사항") {

            } else {
                IconButton(onClick = {
                    val previousRoute = navController.previousBackStackEntry?.destination?.route
                    val currentRoute = navController.currentBackStackEntry?.destination?.route
                    Log.d("MyTopBar", "Current Route: $currentRoute, Previous Route: $previousRoute")

                    if (title == "계정설정") {
                        Log.d("dddfff","hellodfd")
                        navController.navigate("setting_screen") {
                            popUpTo("setting_screen") { inclusive = true }
                        }
                    }
                    else {
                        Log.d("dddfff","hello")
                        navController.popBackStack()



                    }
                }) {
                    if (title == "글쓰기") {
                        Icon(
                            Icons.Filled.Close,
                            contentDescription = "x표시",
                            tint = Color.Black,
                            modifier = Modifier.padding(top = 5.dp)
                        )
                    } else {
                        Icon(
                            Icons.Filled.ArrowBack,
                            contentDescription = "뒤로 가기",
                            tint = Color.Black,
                            modifier = Modifier.padding(top = 5.dp)
                        )
                    }
                }
            }
        }
    )
    HorizontalDivider(thickness = 1.dp, color = Color.Gray)
}

@Preview(showBackground = true)
@Composable
fun PreviewMyBenefitScreen() {
    val navController = rememberNavController()
    val benefitViewModel = BenefitViewModel()
    MyBenefitScreen(navController, benefitViewModel)
}

@Composable
fun ZoomableImage(
    imageUri: Uri?,
    modifier: Modifier = Modifier
) {
    var scale by remember { mutableStateOf(1f) }

    val maxScale = 5f
    val minScale = 1f

    Image(
        painter = rememberImagePainter(imageUri),
        contentDescription = "Zoomable Image",
        modifier = modifier
            .graphicsLayer(
                scaleX = maxOf(minScale, min(maxScale, scale)),
                scaleY = maxOf(minScale, min(maxScale, scale))
            )
            .pointerInput(Unit) {
                detectTransformGestures { _, _, zoom, _ ->
                    scale = maxOf(minScale, min(maxScale, scale * zoom))
                }
            }
    )
}
