package com.bnsh.torchplus

import android.annotation.SuppressLint
import android.content.Context
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Dispatchers
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FlashlightOff
import androidx.compose.material.icons.outlined.FlashlightOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.view.ContentInfoCompat
import com.bnsh.torchplus.data.SettingsDataStore
import com.bnsh.torchplus.service.TorchPlusTileService
import com.bnsh.torchplus.ui.components.CustomSlider
import com.bnsh.torchplus.ui.components.TopBar
import com.bnsh.torchplus.ui.theme.TorchTheme
import com.bnsh.torchplus.util.Version
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.launch
import kotlin.system.exitProcess

class MainActivity : ComponentActivity() {

    companion object {
        lateinit var activity: ComponentActivity
        var torchBrightness: Float = 0f
        var torchEnabled: Boolean = false
        var compatible: Boolean = true
        lateinit var cameraManager: CameraManager
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        this.setShowWhenLocked(true)
        activity = this
        super.onCreate(savedInstanceState)

        if(Version.isTiramisu()) {
            TorchPlusTileService.requestPlacement()
        }

        cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager

        setContent {
            TorchTheme {
                App()
            }
        }
    }
}

@OptIn(DelicateCoroutinesApi::class)
@SuppressLint("NewApi")
fun updateTorch(
    mgr: CameraManager,
    id: String,
    enabled: Boolean,
    context: Context
) {
    if(!Version.isTiramisu()) {
        mgr.setTorchMode(id, enabled)
        return
    }

    val settingsDataStore = SettingsDataStore(context)

    if(enabled) {
        val maxBrightness = mgr.getCameraCharacteristics(id).get(CameraCharacteristics.FLASH_INFO_STRENGTH_MAXIMUM_LEVEL) ?: 0

        GlobalScope.launch(Dispatchers.Main) {
            settingsDataStore.getTorchBrightness.collect {
                if (it != null) {
                    if (it < 0.1f) {
                        mgr.turnOnTorchWithStrengthLevel(id, 1)
                        return@collect
                    }

                    mgr.turnOnTorchWithStrengthLevel(id, (it * maxBrightness).toInt())
                }
            }
        }
    } else {
        mgr.setTorchMode(id, enabled)
    }
}

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun App() {
    val context = MainActivity.activity

    if(!Version.isTiramisu()) {
        MainActivity.compatible = false

        android.app.AlertDialog.Builder(context)
            .setTitle("Error")
            .setMessage("This app requires Android 13 or higher")
            .setPositiveButton("Exit") { _, _ ->
                exitProcess(0)
            }
            .show()
        return
    }

    val cameraManager = MainActivity.cameraManager

    if(cameraManager.cameraIdList.isEmpty()) {
        Toast.makeText(context, "No camera available", Toast.LENGTH_LONG).show()
        android.app.AlertDialog.Builder(context)
            .setTitle("Error")
            .setMessage("Your device has no camera.")
            .setPositiveButton("Exit") { _, _ ->
                exitProcess(0)
            }
            .show()
        MainActivity.compatible = false
        return
    }

    val cameraId = cameraManager.cameraIdList[0]
    val cameraInfo = cameraManager.getCameraCharacteristics(cameraId)

    val flashAvailable = cameraInfo.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) ?: false
    val flashMaxBrightness = cameraInfo.get(CameraCharacteristics.FLASH_INFO_STRENGTH_MAXIMUM_LEVEL)
    val flashDefaultBrightness = cameraInfo.get(CameraCharacteristics.FLASH_INFO_STRENGTH_DEFAULT_LEVEL)

    var torchEnabled by remember { mutableStateOf( false ) }

    if(!flashAvailable) {
        android.app.AlertDialog.Builder(context)
            .setTitle("Error")
            .setMessage("Your device doesn't have a flashlight")
            .setPositiveButton("Exit") { _, _ ->
                exitProcess(0)
            }
            .show()
        MainActivity.compatible = false
        return
    }

    if(flashMaxBrightness == null || flashDefaultBrightness == null) {
        android.app.AlertDialog.Builder(context)
            .setTitle("Error")
            .setMessage("Your flashlight doesn't have support brightness")
            .setPositiveButton("Exit") { _, _ ->
                exitProcess(0)
            }
            .show()
        MainActivity.compatible = false
        return
    }

    val scope = rememberCoroutineScope()
    val settingsDataStore = SettingsDataStore(context)

    var brightness by remember { mutableStateOf(flashMaxBrightness.let { flashDefaultBrightness.toFloat() / it }) }

    LaunchedEffect(key1 = settingsDataStore.getTorchBrightness) {
        scope.launch {
            settingsDataStore.getTorchBrightness.collect {
                if(it != null) {
                    brightness = it
                    MainActivity.torchBrightness = it
                }
            }
        }
    }

    val brightnessAnimation = animateFloatAsState(
        targetValue = brightness,
        animationSpec = tween(
            durationMillis = 60,
            easing = LinearEasing
        )
    )

    Scaffold(
        topBar = { TopBar() }
    ) {
        Surface(color = MaterialTheme.colorScheme.background) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom,
                modifier = Modifier.fillMaxSize().padding(bottom = 64.dp)
            ) {

                CustomSlider(
                    value = brightnessAnimation.value,
                    onValueChange = {
                        MainActivity.torchBrightness = it
                        brightness = it

                        if(torchEnabled) updateTorch(cameraManager, cameraId, torchEnabled, context)
                    },
                    onDragEnd = {
                        scope.launch {
                            settingsDataStore.setTorchBrightness(it)
                        }
                    },
                    icon = {
                        Icons.Outlined.FlashlightOn
                        /*if(it < 1 / 3) return@CustomSlider Icons.Outlined.BrightnessLow
                                if(it >= 1 / 3 && it < (1 / 3) * 2) return@CustomSlider Icons.Outlined.BrightnessMedium

                                return@CustomSlider Icons.Outlined.BrightnessHigh*/
                    }
                )

                Spacer(Modifier.height(24.dp))

                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(0.8f)
                ) {
                    Button(
                        onClick = {
                            torchEnabled = !torchEnabled
                            MainActivity.torchEnabled = torchEnabled

                            updateTorch(
                                cameraManager,
                                cameraId,
                                torchEnabled,
                                context
                            )
                        }
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth(0.4f)
                        ) {
                            Icon(
                                if(torchEnabled) Icons.Outlined.FlashlightOn else Icons.Outlined.FlashlightOff,
                                null
                            )
                            Text("${if(torchEnabled) "Disable" else "Enable"} torch")
                        }
                    }

                    OutlinedButton(
                        onClick = {
                            val defaultBrightness =
                                flashMaxBrightness.let { it1 ->
                                    flashDefaultBrightness.toFloat().div(
                                        it1
                                    )
                                }

                            scope.launch {
                                settingsDataStore.setTorchBrightness(defaultBrightness)
                            }
                            MainActivity.torchBrightness = defaultBrightness
                            brightness = defaultBrightness

                            updateTorch(
                                cameraManager,
                                cameraId,
                                torchEnabled,
                                context
                            )
                        }
                    ) {
                        Text("Reset", Modifier.fillMaxWidth(0.8f), textAlign = TextAlign.Center)
                    }
                }
            }
        }
    }
}


@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    TorchTheme {
        App()
    }
}