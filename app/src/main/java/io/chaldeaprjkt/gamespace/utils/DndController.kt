/*
 * Copyright (C) 2026 VoltageOS
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.chaldeaprjkt.gamespace.utils

import android.app.NotificationManager
import android.content.Context
import android.util.Log
import io.chaldeaprjkt.gamespace.data.AppSettings
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Controller for managing Do Not Disturb (DND) mode during game sessions.
 *
 * This controller saves the original DND state when a game starts and restores it
 * when the game ends. It uses the NotificationManager to control the interruption filter.
 */
@Singleton
class DndController @Inject constructor(
    private val context: Context,
    private val appSettings: AppSettings
) {
    private val notificationManager: NotificationManager by lazy {
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    private var originalInterruptionFilter: Int = NotificationManager.INTERRUPTION_FILTER_ALL
    private var isDndApplied: Boolean = false

    /**
     * Called when a game session starts.
     * Enables DND if the feature is enabled in settings and DND is not already active.
     */
    fun onGameStart() {
        if (!appSettings.autoDndEnabled) {
           Log.d(TAG, "Auto DND is disabled, skipping")
            return
        }

        if (!notificationManager.isNotificationPolicyAccessGranted) {
            Log.w(TAG, "Notification policy access not granted, cannot control DND")
            return
        }

        originalInterruptionFilter = notificationManager.currentInterruptionFilter
        Log.d(TAG, "Saved original interruption filter: $originalInterruptionFilter")

        if (originalInterruptionFilter == NotificationManager.INTERRUPTION_FILTER_ALL) {
            try {
                notificationManager.setInterruptionFilter(
                    NotificationManager.INTERRUPTION_FILTER_PRIORITY
                )
                isDndApplied = true
                Log.d(TAG, "DND enabled for game session")
            } catch (e: SecurityException) {
                Log.e(TAG, "Failed to enable DND: ${e.message}")
            }
        } else {
            Log.d(TAG, "DND already active, not changing")
            isDndApplied = false
        }
    }

    /**
     * Called when a game session ends.
     * Restores the original DND state that was saved when the game started.
     */
    fun onGameStop() {
        if (!isDndApplied) {
            Log.d(TAG, "DND was not applied by GameSpace, skipping restore")
            return
        }

        try {
            notificationManager.setInterruptionFilter(originalInterruptionFilter)
            Log.d(TAG, "DND restored to: $originalInterruptionFilter")
        } catch (e: SecurityException) {
            Log.e(TAG, "Failed to restore DND: ${e.message}")
        }
        isDndApplied = false
    }

    companion object {
        private const val TAG = "DndController"
    }
}
