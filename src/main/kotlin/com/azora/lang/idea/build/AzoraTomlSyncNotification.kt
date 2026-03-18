/*
 * Copyright 2026 AzoraTech
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.azora.lang.idea.build

import com.azora.lang.idea.AzoraIcons
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.*
import java.util.function.Function
import javax.swing.JComponent

/**
 * [EditorNotificationProvider] that shows a banner when the Azora build
 * configuration (`azora.toml`) needs syncing.
 *
 * The banner appears at the top of `.az` files and `azora.toml` when
 * [AzoraProjectConfigService.needsSync] is `true`, offering "Sync Now"
 * and "Dismiss" actions that both trigger a sync and refresh notifications.
 */
class AzoraTomlSyncNotification : EditorNotificationProvider {

    /**
     * Collects notification data for the given [file] in [project].
     *
     * Returns a factory function that creates an [EditorNotificationPanel]
     * if the file is an Azora-related file and sync is needed, or `null` otherwise.
     *
     * @param project the current IntelliJ project.
     * @param file the virtual file being opened in the editor.
     * @return a function that produces the notification panel for a given [FileEditor].
     */
    override fun collectNotificationData(
        project: Project,
        file: VirtualFile
    ): Function<in FileEditor, out JComponent?> {
        return Function { _ ->
            // Show on any .az file or azora.toml when sync is needed
            val service = AzoraProjectConfigService.getInstance(project)
            if (!service.needsSync) return@Function null

            val isAzoraFile = file.extension == "az" || file.name == "azora.toml"
            if (!isAzoraFile) return@Function null

            val panel = EditorNotificationPanel(EditorNotificationPanel.Status.Info)
            panel.icon(AzoraIcons.AZORA)
            panel.text = "Azora build configuration changed."
            panel.createActionLabel("Sync now") {
                service.sync()
                // Force re-check of notifications
                EditorNotifications.getInstance(project).updateAllNotifications()
            }
            panel.createActionLabel("Dismiss") {
                service.sync()
                EditorNotifications.getInstance(project).updateAllNotifications()
            }
            panel
        }
    }
}
