package io.Jhonyrod.HueRuler

import kotlinx.coroutines.*
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.foundation.*
import androidx.compose.material3.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.Icons
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.*
import androidx.compose.ui.unit.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.Modifier
//import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.foundation.text.selection.*
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import io.Jhonyrod.HueRuler.ui.theme.HueRulerTheme

@OptIn(ExperimentalMaterial3Api::class)
class MainActivity:ComponentActivity(){
    val initString = "Tap the refresh button to load."
    val fetching = "Fetching…"
    
    override fun onCreate(savedInstanceState:Bundle?){
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent{
            val scope = rememberCoroutineScope()
            val request = remember { Request() }
            val job = remember {
                mutableStateOf<Job?>(null)
            }
            var disp by rememberSaveable {
                mutableStateOf(initString)
            }
            
            val fabColor by animateColorAsState(
                if (disp == fetching)
                MaterialTheme
                .colorScheme
                .error
                else
                MaterialTheme
                .colorScheme
                .primary
            )
            
            HueRulerTheme{
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = {
                                Text("Hue Ruler")
                            },
                            colors =
                            TopAppBarDefaults
                            .topAppBarColors(
                                containerColor =
                                MaterialTheme
                                .colorScheme
                                .primaryContainer,
                                titleContentColor =
                                MaterialTheme
                                .colorScheme.primary
                            )
                        )
                    },
                    floatingActionButton = {
                        FloatingActionButton(
                            onClick = {
                                if (
                                    job
                                    .value?.isActive
                                    == true
                                )
                                {
                                    job
                                    .value?.cancel()
                                    disp = initString
                                } else {
                                    job
                                    .value?.cancel()
                                    disp = fetching
                                    job.value = scope
                                    .launch {
                                        disp =
                                        request
                                        .fetch()
                                    }
                                }
                            },
                            shape = CircleShape,
                            containerColor = fabColor
                        ){
                            if (
                                job
                                .value?.isActive == 
                                true
                            ) Icon(
                                Icons.Filled.Close,
                                "Cancel",
                                Modifier.size(50.dp)
                            ) else Icon(
                                Icons.Filled.Refresh,
                                "Fetch",
                                Modifier.size(40.dp)
                            )
                        }
                    }
                ){ innerPadding ->
                    SelectionContainer {
                        Text(
                            disp,
                            modifier = Modifier
                            .verticalScroll(
                                rememberScrollState()
                            )
                            .padding(innerPadding)
                            .fillMaxWidth()
                        )
                    }
                }
                LaunchedEffect(Unit) {
                    discover("_hue._tcp.")
                }
                DisposableEffect(request) {
                    onDispose {
                        request.close()
                        //disp += "\nrequest closed"
                    }
                }
            }
        }
    }
}