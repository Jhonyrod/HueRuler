package io.Jhonyrod.HueRuler

import kotlinx.coroutines.launch
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
//import androidx.compose.foundation.shape.*
import androidx.compose.foundation.*
import androidx.compose.material3.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.Icons
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.*
import androidx.compose.ui.unit.*
import androidx.compose.ui.Modifier
//import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.foundation.text.selection.*
import io.Jhonyrod.HueRuler.ui.theme.HueRulerTheme

@OptIn(ExperimentalMaterial3Api::class)
class MainActivity:ComponentActivity(){
    override fun onCreate(savedInstanceState:Bundle?){
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent{
            val scope = rememberCoroutineScope()
            val request = remember { Request() }
            var disp by rememberSaveable {
                mutableStateOf("")
            }
            
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
                            onClick = { scope.launch {
                                disp = "Fetching…"
                                disp = request.fetch()
                            }}
                        ){
                                Icon(
                                    Icons
                                    .Filled
                                    .Refresh,
                                    "Refresh")
                        }
                    }
                ){ innerPadding -> SelectionContainer{
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