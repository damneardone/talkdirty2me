package com.damneardone.talkdirty2me

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.damneardone.talkdirty2me.data.SettingsRepository
import com.damneardone.talkdirty2me.ui.AgeGate
import com.damneardone.talkdirty2me.ui.MainScreen
import com.damneardone.talkdirty2me.ui.Td2mTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Td2mTheme {
                val repo = remember { SettingsRepository(applicationContext) }
                val vm: GenViewModel = viewModel(factory = viewModelFactory { initializer { GenViewModel(repo) } })
                val ageOk by vm.ageOk.collectAsState()
                if (!ageOk) {
                    AgeGate(onConfirm = { rememberMe -> vm.confirmAge(rememberMe) })
                } else {
                    MainScreen(vm)
                }
            }
        }
    }
}