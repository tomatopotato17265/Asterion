package dev.tomatopotato.asterion.viewmodel

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import dev.tomatopotato.asterion.tabs.AppTab
import dev.tomatopotato.asterion.tabs.TabLayout

class TabsViewModel(app: Application) : AndroidViewModel(app) {

    private val prefs = app.getSharedPreferences("settings", Application.MODE_PRIVATE)

    var freeOrder: List<AppTab> by mutableStateOf(loadFreeOrder())
        private set

    val visibleTabs: List<AppTab>
        get() = TabLayout.resolveVisibleTabs(freeOrder.map { it.id })

    val freeCandidates: List<AppTab>
        get() = TabLayout.freeCandidates

    fun updateFreeOrder(newOrder: List<AppTab>) {
        freeOrder = TabLayout.resolveVisibleTabs(newOrder.map { it.id }).take(TabLayout.freeSlotCount)
        persist()
    }

    private fun loadFreeOrder(): List<AppTab> {
        val stored = prefs.getString(KEY_FREE_ORDER, null)?.split(",").orEmpty().filter { it.isNotBlank() }
        val resolved = TabLayout.resolveVisibleTabs(stored).take(TabLayout.freeSlotCount)
        if (stored != resolved.map { it.id }) {
            prefs.edit().putString(KEY_FREE_ORDER, resolved.joinToString(",") { it.id }).apply()
        }
        return resolved
    }

    private fun persist() {
        prefs.edit().putString(KEY_FREE_ORDER, freeOrder.joinToString(",") { it.id }).apply()
    }

    private companion object {
        const val KEY_FREE_ORDER = "selectedFreeTabOrder"
    }
}
