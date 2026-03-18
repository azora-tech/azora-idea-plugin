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

package com.azora.lang.idea.run

import com.azora.lang.idea.build.AzoraProjectConfigService
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.util.ui.JBUI
import java.awt.*
import javax.swing.*

/**
 * [SettingsEditor] UI for [AzoraRunConfiguration].
 *
 * Displays a two-field form in the "Run/Debug Configurations" dialog:
 * - **File** - a text field with a browse button filtered to `.az` files.
 * - **Target** - a combo box populated from the project's configured targets
 *   (e.g. `"interpret"`, `"native"`).
 *
 * @param project the current IntelliJ project, used for file browsing and target lookup.
 */
class AzoraRunConfigurationEditor(private val project: Project) : SettingsEditor<AzoraRunConfiguration>() {

    /** Text field with browse button for selecting the `.az` source file to run. */
    private val filePathField = TextFieldWithBrowseButton()

    /** Combo box listing the available compilation/execution targets. */
    private val targetCombo = JComboBox<String>()

    init {
        filePathField.addBrowseFolderListener(
            project,
            FileChooserDescriptorFactory.createSingleFileDescriptor("az")
        )
        refreshTargets()
    }

    /**
     * Reloads the target combo box from the project's [AzoraProjectConfigService].
     *
     * Falls back to `["interpret", "native"]` if the config is unavailable.
     */
    private fun refreshTargets() {
        val targets = try {
            AzoraProjectConfigService.getInstance(project).config.targets
        } catch (_: Exception) {
            listOf("interpret", "native")
        }
        targetCombo.model = DefaultComboBoxModel(targets.toTypedArray())
    }

    /**
     * Builds and returns the editor panel with a [GridBagLayout] containing
     * the file path field and target combo box.
     *
     * @return the root [JComponent] of the editor UI.
     */
    override fun createEditor(): JComponent {
        val panel = JPanel(GridBagLayout())
        panel.border = JBUI.Borders.empty(10)

        val gbc = GridBagConstraints().apply {
            fill = GridBagConstraints.HORIZONTAL
            anchor = GridBagConstraints.WEST
            insets = JBUI.insets(4)
        }

        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0.0
        panel.add(JLabel("File:"), gbc)
        gbc.gridx = 1; gbc.gridy = 0; gbc.weightx = 1.0
        panel.add(filePathField, gbc)

        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0.0
        panel.add(JLabel("Target:"), gbc)
        gbc.gridx = 1; gbc.gridy = 1; gbc.weightx = 1.0
        panel.add(targetCombo, gbc)

        return panel
    }

    /**
     * Writes the current editor field values into the given [config].
     *
     * @param config the run configuration to update.
     */
    override fun applyEditorTo(config: AzoraRunConfiguration) {
        config.filePath = filePathField.text.trim()
        config.target = targetCombo.selectedItem as? String ?: "interpret"
    }

    /**
     * Populates the editor fields from the given [config].
     *
     * Also refreshes the target combo box to reflect any project config changes.
     *
     * @param config the run configuration to read from.
     */
    override fun resetEditorFrom(config: AzoraRunConfiguration) {
        filePathField.text = config.filePath
        refreshTargets()
        targetCombo.selectedItem = config.target
    }
}
