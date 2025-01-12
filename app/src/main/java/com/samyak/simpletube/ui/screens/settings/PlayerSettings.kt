package com.samyak.simpletube.ui.screens.settings

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.ClearAll
import androidx.compose.material.icons.rounded.FastForward
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Lyrics
import androidx.compose.material.icons.rounded.NoCell
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.navigation.NavController
import com.samyak.simpletube.LocalPlayerAwareWindowInsets
import com.samyak.simpletube.R
import com.samyak.simpletube.constants.AudioNormalizationKey
import com.samyak.simpletube.constants.AudioQuality
import com.samyak.simpletube.constants.AudioQualityKey
import com.samyak.simpletube.constants.AudioOffload
import com.samyak.simpletube.constants.KeepAliveKey
import com.samyak.simpletube.constants.PersistentQueueKey
import com.samyak.simpletube.constants.SkipOnErrorKey
import com.samyak.simpletube.constants.SkipSilenceKey
import com.samyak.simpletube.constants.StopMusicOnTaskClearKey
import com.samyak.simpletube.constants.minPlaybackDurKey
import com.samyak.simpletube.playback.KeepAlive
import com.samyak.simpletube.ui.component.ActionPromptDialog
import com.samyak.simpletube.ui.component.EnumListPreference
import com.samyak.simpletube.ui.component.IconButton
import com.samyak.simpletube.ui.component.PreferenceEntry
import com.samyak.simpletube.ui.component.PreferenceGroupTitle
import com.samyak.simpletube.ui.component.SwitchPreference
import com.samyak.simpletube.ui.utils.backToMain
import com.samyak.simpletube.utils.rememberEnumPreference
import com.samyak.simpletube.utils.rememberPreference
import com.samyak.simpletube.utils.reportException

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerSettings(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
) {
    val context = LocalContext.current

    val (audioQuality, onAudioQualityChange) = rememberEnumPreference(key = AudioQualityKey, defaultValue = AudioQuality.AUTO)
    val (persistentQueue, onPersistentQueueChange) = rememberPreference(key = PersistentQueueKey, defaultValue = true)
    val (skipSilence, onSkipSilenceChange) = rememberPreference(key = SkipSilenceKey, defaultValue = false)
    val (skipOnErrorKey, onSkipOnErrorChange) = rememberPreference(key = SkipOnErrorKey, defaultValue = true)
    val (audioNormalization, onAudioNormalizationChange) = rememberPreference(key = AudioNormalizationKey, defaultValue = true)
    val (stopMusicOnTaskClear, onStopMusicOnTaskClearChange) = rememberPreference(key = StopMusicOnTaskClearKey, defaultValue = false)
    val (minPlaybackDur, onMinPlaybackDurChange) = rememberPreference(minPlaybackDurKey, defaultValue = 30)
    val (audioOffload, onAudioOffloadChange) = rememberPreference(key = AudioOffload, defaultValue = false)
    val (keepAlive, onKeepAliveChange) = rememberPreference(key = KeepAliveKey, defaultValue = false)

    var showMinPlaybackDur by remember {
        mutableStateOf(false)
    }
    var tempminPlaybackDur by remember {
        mutableIntStateOf(minPlaybackDur)
    }

    fun toggleKeepAlive(newValue: Boolean) {
        // disable and request if disabled
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
            && context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            onKeepAliveChange(false)
            Toast.makeText(
                context,
                "Notification permission is required",
                Toast.LENGTH_SHORT
            ).show()

            ActivityCompat.requestPermissions(
                context as Activity,
                arrayOf( Manifest.permission.POST_NOTIFICATIONS), PackageManager.PERMISSION_GRANTED
            )
            return
        }

        if (keepAlive != newValue) {
            onKeepAliveChange(newValue)
            // start/stop service accordingly
            if (newValue) {
                try {
                    context.startService(Intent(context, KeepAlive::class.java))
                } catch (e: Exception) {
                    reportException(e)
                }
            } else {
                try {
                    context.stopService(Intent(context, KeepAlive::class.java))
                } catch (e: Exception) {
                    reportException(e)
                }
            }
        }
    }

    // reset if no permission
    LaunchedEffect(keepAlive) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
            && context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            onKeepAliveChange(false)
        }
    }


    if (showMinPlaybackDur) {
        ActionPromptDialog(
            title = "Minimum playback duration",
            onDismiss = { showMinPlaybackDur = false },
            onConfirm = {
                showMinPlaybackDur = false
                onMinPlaybackDurChange(tempminPlaybackDur)
            },
            onCancel = {
                showMinPlaybackDur = false
                tempminPlaybackDur = minPlaybackDur
            }
        ) {
            Text(
                text = "The minimum amount of a song that must be played before it is considered \"played\"",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "${tempminPlaybackDur}%",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
                Slider(
                    value = tempminPlaybackDur.toFloat(),
                    onValueChange = { tempminPlaybackDur = it.toInt() },
                    valueRange = 0f..100f
                )
            }
        }
    }

    Column(
        Modifier
            .windowInsetsPadding(LocalPlayerAwareWindowInsets.current)
            .verticalScroll(rememberScrollState())
    ) {
        PreferenceGroupTitle(
            title = "Interface"
        )
        SwitchPreference(
            title = { Text(stringResource(R.string.persistent_queue)) },
            description = stringResource(R.string.persistent_queue_desc),
            icon = { Icon(Icons.AutoMirrored.Rounded.QueueMusic, null) },
            checked = persistentQueue,
            onCheckedChange = onPersistentQueueChange
        )
        // lyrics settings
        PreferenceEntry(
            title = { Text(stringResource(R.string.lyrics_settings_title)) },
            icon = { Icon(Icons.Rounded.Lyrics, null) },
            onClick = { navController.navigate("settings/player/lyrics") }
        )
        PreferenceEntry(
            title = { Text("Minimum playback duration") },
            icon = { Icon(Icons.Rounded.Sync, null) },
            onClick = { showMinPlaybackDur = true }
        )

        PreferenceGroupTitle(
            title = "Audio"
        )
        EnumListPreference(
            title = { Text(stringResource(R.string.audio_quality)) },
            icon = { Icon(Icons.Rounded.GraphicEq, null) },
            selectedValue = audioQuality,
            onValueSelected = onAudioQualityChange,
            valueText = {
                when (it) {
                    AudioQuality.AUTO -> stringResource(R.string.audio_quality_auto)
                    AudioQuality.HIGH -> stringResource(R.string.audio_quality_high)
                    AudioQuality.LOW -> stringResource(R.string.audio_quality_low)
                }
            }
        )
        SwitchPreference(
            title = { Text(stringResource(R.string.audio_normalization)) },
            icon = { Icon(Icons.AutoMirrored.Rounded.VolumeUp, null) },
            checked = audioNormalization,
            onCheckedChange = onAudioNormalizationChange
        )
        SwitchPreference(
            title = { Text(stringResource(R.string.skip_silence)) },
            icon = { Icon(painterResource(R.drawable.skip_next), null) },
            checked = skipSilence,
            onCheckedChange = onSkipSilenceChange
        )
        SwitchPreference(
            title = { Text(stringResource(R.string.auto_skip_next_on_error)) },
            description = stringResource(R.string.auto_skip_next_on_error_desc),
            icon = { Icon(Icons.Rounded.FastForward, null) },
            checked = skipOnErrorKey,
            onCheckedChange = onSkipOnErrorChange
        )

        PreferenceGroupTitle(
            title = "Advanced"
        )
        SwitchPreference(
            title = { Text(stringResource(R.string.stop_music_on_task_clear)) },
            icon = { Icon(Icons.Rounded.ClearAll, null) },
            checked = stopMusicOnTaskClear,
            onCheckedChange = onStopMusicOnTaskClearChange
        )
        SwitchPreference(
            title = { Text(stringResource(R.string.audio_offload)) },
            description = stringResource(R.string.audio_offload_description),
            icon = { Icon(Icons.Rounded.Bolt, null) },
            checked = audioOffload,
            onCheckedChange = onAudioOffloadChange
        )
        SwitchPreference(
            title = { Text(stringResource(R.string.keep_alive_title)) },
            description = stringResource(R.string.keep_alive_description),
            icon = { Icon(Icons.Rounded.NoCell, null) },
            checked = keepAlive,
            onCheckedChange = { toggleKeepAlive(it) }
        )
    }

    TopAppBar(
        title = { Text(stringResource(R.string.player_and_audio)) },
        navigationIcon = {
            IconButton(
                onClick = navController::navigateUp,
                onLongClick = navController::backToMain
            ) {
                Icon(
                    Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = null
                )
            }
        },
        scrollBehavior = scrollBehavior
    )
}
