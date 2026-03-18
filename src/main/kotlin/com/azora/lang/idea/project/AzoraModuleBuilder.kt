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

package com.azora.lang.idea.project

import com.azora.lang.idea.AzoraIcons
import com.intellij.ide.util.projectWizard.*
import com.intellij.openapi.Disposable
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.module.ModuleType
import com.intellij.openapi.options.ConfigurationException
import com.intellij.openapi.roots.ModifiableRootModel
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.util.ui.JBUI
import java.awt.*
import java.io.File
import javax.swing.*

/**
 * [ModuleBuilder] for creating new Azora modules.
 *
 * Used by the legacy "New Module" wizard to scaffold an Azora project
 * with a `src/` directory, a `main.az` entry point, and the SDK path
 * persisted to [AzoraSdkSettings].
 */
class AzoraModuleBuilder : ModuleBuilder() {

    /** The Azora SDK root path, initialized from persisted settings. */
    var langPath: String = AzoraSdkSettings.getInstance().state.langPath ?: ""

    /** Returns the [AzoraModuleType] singleton. */
    override fun getModuleType(): ModuleType<*> = AzoraModuleType.INSTANCE

    /** Returns `"Azora"` as the group name in the module wizard. */
    override fun getGroupName(): String = "Azora"

    /** Returns `"Azora"` as the display name in the module wizard. */
    override fun getPresentableName(): String = "Azora"

    /** Returns a short description shown in the module wizard. */
    override fun getDescription(): String = "Create a new Azora project"

    /** Returns the Azora icon for the module wizard entry. */
    override fun getNodeIcon(): Icon = AzoraIcons.AZORA

    /**
     * Returns the [AzoraConfigurationStep] for configuring the SDK path
     * during module creation.
     *
     * @param context the wizard context.
     * @param parentDisposable the disposable parent for cleanup.
     * @return the SDK configuration wizard step.
     */
    override fun getCustomOptionsStep(context: WizardContext, parentDisposable: Disposable): ModuleWizardStep {
        return AzoraConfigurationStep(this)
    }

    /**
     * Sets up the module's root model after the wizard completes.
     *
     * Persists the SDK path, creates the `src/` directory with a `main.az`
     * template file, and marks `src/` as a source root in the module content entry.
     *
     * @param modifiableRootModel the root model to configure.
     */
    override fun setupRootModel(modifiableRootModel: ModifiableRootModel) {
        val contentEntry = doAddContentEntry(modifiableRootModel)
        val projectPath = contentEntryPath ?: return

        // Persist the lang path for future use
        val settings = AzoraSdkSettings.getInstance()
        settings.state.langPath = langPath

        // Create project structure
        val srcDir = File(projectPath, "src")
        srcDir.mkdirs()

        val mainFile = File(srcDir, "main.az")
        if (!mainFile.exists()) {
            mainFile.writeText(
                """
                |// Azora project entry point
                |
                |fn main() {
                |
                |}
                """.trimMargin() + "\n"
            )
        }

        // Mark src as source root
        LocalFileSystem.getInstance().refreshAndFindFileByIoFile(srcDir)?.let { vSrc ->
            contentEntry?.addSourceFolder(vSrc, false)
        }
    }
}

/**
 * [ModuleWizardStep] for configuring the Azora SDK path during module creation.
 *
 * Displays a single folder-picker field for the SDK root directory,
 * with validation ensuring the path is non-empty and points to an existing directory.
 *
 * @param builder the [AzoraModuleBuilder] whose [langPath][AzoraModuleBuilder.langPath] is updated.
 */
private class AzoraConfigurationStep(private val builder: AzoraModuleBuilder) : ModuleWizardStep() {

    /** Text field with browse button for selecting the Azora SDK directory. */
    private val langPathField = TextFieldWithBrowseButton()

    /** The panel containing the SDK path field, help text, and layout. */
    private val panel: JPanel = JPanel(GridBagLayout()).apply {
        border = JBUI.Borders.empty(20)

        val gbc = GridBagConstraints().apply {
            fill = GridBagConstraints.HORIZONTAL
            anchor = GridBagConstraints.WEST
            insets = JBUI.insets(5)
        }

        // Lang path label
        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0.0
        add(JLabel("Azora SDK path:"), gbc)

        // Lang path field with folder browser
        gbc.gridx = 1; gbc.gridy = 0; gbc.weightx = 1.0
        langPathField.addBrowseFolderListener(
            null,
            FileChooserDescriptorFactory.createSingleFolderDescriptor()
        )
        langPathField.text = builder.langPath
        add(langPathField, gbc)

        // Help text
        gbc.gridx = 1; gbc.gridy = 1; gbc.weightx = 1.0
        val helpLabel = JLabel("<html><small>Directory containing the Azora executable and standard library</small></html>")
        helpLabel.foreground = JBUI.CurrentTheme.ContextHelp.FOREGROUND
        add(helpLabel, gbc)

        // Vertical spacer
        gbc.gridx = 0; gbc.gridy = 2; gbc.weighty = 1.0; gbc.gridwidth = 2
        add(JPanel(), gbc)
    }

    /** Returns the configuration panel as this step's UI component. */
    override fun getComponent(): JComponent = panel

    /**
     * Writes the current SDK path from the text field into the [builder].
     *
     * Called by the wizard framework when advancing to the next step or finishing.
     */
    override fun updateDataModel() {
        builder.langPath = langPathField.text
    }

    /**
     * Validates the SDK path field before allowing the user to proceed.
     *
     * @return `true` if the path is valid.
     * @throws ConfigurationException if the path is empty or not an existing directory.
     */
    override fun validate(): Boolean {
        val path = langPathField.text.trim()
        if (path.isEmpty()) {
            throw ConfigurationException("Azora SDK path must be specified.")
        }
        if (!File(path).isDirectory) {
            throw ConfigurationException("Azora SDK path must be an existing directory.")
        }
        return true
    }
}
