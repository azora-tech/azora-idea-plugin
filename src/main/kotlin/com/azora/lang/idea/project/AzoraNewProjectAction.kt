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
import com.intellij.ide.impl.ProjectUtil
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.ui.*
import com.intellij.util.ui.JBUI
import java.awt.*
import java.io.File
import java.nio.file.Path
import javax.swing.*

/**
 * Action that opens the [AzoraNewProjectDialog] and scaffolds a new Azora project.
 *
 * When the user confirms the dialog, this action persists the SDK path,
 * creates the project directory with `src/Main.az` and `azora.toml`,
 * and opens the project in the IDE.
 */
class AzoraNewProjectAction : AnAction(
    "Azora Project",
    "Create a new Azora project",
    AzoraIcons.AZORA
) {
    /**
     * Invoked when the user triggers the "New Azora Project" action.
     *
     * Shows the [AzoraNewProjectDialog], and on confirmation creates the
     * project structure (directory, `src/Main.az`, `azora.toml`) and opens it.
     *
     * @param e the action event providing context.
     */
    override fun actionPerformed(e: AnActionEvent) {
        val dialog = AzoraNewProjectDialog()
        if (dialog.showAndGet()) {
            val projectName = dialog.projectName
            val projectLocation = dialog.projectLocation
            val langPath = dialog.langPath

            // Save SDK path
            AzoraSdkSettings.getInstance().state.langPath = langPath

            // Create project structure
            val projectDir = File(projectLocation, projectName)
            projectDir.mkdirs()

            val srcDir = File(projectDir, "src")
            srcDir.mkdirs()

            val mainFile = File(srcDir, "Main.az")
            if (!mainFile.exists()) {
                mainFile.writeText(
                    """
                    |package ${projectName.toSnakeCase()}
                    |
                    |func main() {
                    |    println("Hello world!")
                    |}
                    """.trimMargin() + "\n"
                )
            }

            // Create azora.toml
            val tomlFile = File(projectDir, "azora.toml")
            if (!tomlFile.exists()) {
                tomlFile.writeText(generateAzoraToml(projectName.toSnakeCase()))
            }

            // Open the project
            ProjectUtil.openOrImport(Path.of(projectDir.absolutePath), null, true)
        }
    }
}

/**
 * Modal dialog for creating a new Azora project.
 *
 * Presents three fields:
 * - **Name** - the project name (defaults to `"untitled"`).
 * - **Location** - the parent directory (defaults to `~/AzoraProjects`).
 * - **Azora SDK path** - the SDK root directory (defaults to persisted or `~/.azoralang`).
 *
 * Validates that all fields are filled, the SDK path exists, and `bin/azora` is present.
 */
private class AzoraNewProjectDialog : DialogWrapper(true) {

    /** Text field for entering the project name. */
    private val nameField = JTextField("untitled")

    /** Text field with browse button for selecting the project parent directory. */
    private val locationField = TextFieldWithBrowseButton()

    /** Text field with browse button for selecting the Azora SDK root directory. */
    private val langPathField = TextFieldWithBrowseButton()

    /** The trimmed project name entered by the user. */
    val projectName: String get() = nameField.text.trim()

    /** The trimmed project location path entered by the user. */
    val projectLocation: String get() = locationField.text.trim()

    /** The trimmed Azora SDK path entered by the user. */
    val langPath: String get() = langPathField.text.trim()

    init {
        title = "New Azora Project"
        val defaultLocation = System.getProperty("user.home") + File.separator + "AzoraProjects"
        locationField.text = defaultLocation

        locationField.addBrowseFolderListener(
            null,
            FileChooserDescriptorFactory.createSingleFolderDescriptor()
        )

        val savedPath = AzoraSdkSettings.getInstance().state.langPath
        langPathField.text = if (savedPath.isNullOrEmpty())
            System.getProperty("user.home") + "/.azoralang"
        else savedPath
        langPathField.addBrowseFolderListener(
            null,
            FileChooserDescriptorFactory.createSingleFolderDescriptor()
        )

        init()
    }

    /**
     * Builds the dialog's center panel with a [GridBagLayout] containing
     * the name, location, and SDK path fields plus a help label.
     *
     * @return the root [JComponent] of the dialog content.
     */
    override fun createCenterPanel(): JComponent {
        val panel = JPanel(GridBagLayout())
        panel.preferredSize = Dimension(500, 200)
        panel.border = JBUI.Borders.empty(10)

        val gbc = GridBagConstraints().apply {
            fill = GridBagConstraints.HORIZONTAL
            anchor = GridBagConstraints.WEST
            insets = JBUI.insets(4)
        }

        // Project name
        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0.0
        panel.add(JLabel("Name:"), gbc)
        gbc.gridx = 1; gbc.gridy = 0; gbc.weightx = 1.0
        panel.add(nameField, gbc)

        // Location
        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0.0
        panel.add(JLabel("Location:"), gbc)
        gbc.gridx = 1; gbc.gridy = 1; gbc.weightx = 1.0
        panel.add(locationField, gbc)

        // SDK path
        gbc.gridx = 0; gbc.gridy = 2; gbc.weightx = 0.0
        panel.add(JLabel("Azora SDK path:"), gbc)
        gbc.gridx = 1; gbc.gridy = 2; gbc.weightx = 1.0
        panel.add(langPathField, gbc)

        // Help text
        gbc.gridx = 1; gbc.gridy = 3; gbc.weightx = 1.0
        val helpLabel = JLabel("<html><small>Directory containing the Azora executable and standard library (e.g. ~/.azoralang)</small></html>")
        helpLabel.foreground = JBUI.CurrentTheme.ContextHelp.FOREGROUND
        panel.add(helpLabel, gbc)

        // Spacer
        gbc.gridx = 0; gbc.gridy = 4; gbc.weighty = 1.0; gbc.gridwidth = 2
        panel.add(JPanel(), gbc)

        return panel
    }

    /**
     * Validates the dialog fields before allowing the user to proceed.
     *
     * Checks that the project name and location are non-empty, the SDK path
     * exists as a directory, and contains `bin/azora`.
     *
     * @return a [ValidationInfo] describing the first error found, or `null` if valid.
     */
    override fun doValidate(): ValidationInfo? {
        if (projectName.isEmpty()) {
            return ValidationInfo("Project name must not be empty.", nameField)
        }
        if (projectLocation.isEmpty()) {
            return ValidationInfo("Project location must not be empty.", locationField)
        }
        if (langPath.isEmpty()) {
            return ValidationInfo("Azora SDK path must be specified.", langPathField)
        }
        if (!File(langPath).isDirectory) {
            return ValidationInfo("Azora SDK path does not exist.", langPathField)
        }
        if (!File(langPath, "bin/azora").exists()) {
            return ValidationInfo("Invalid Azora SDK: bin/azora not found.", langPathField)
        }
        return null
    }
}
