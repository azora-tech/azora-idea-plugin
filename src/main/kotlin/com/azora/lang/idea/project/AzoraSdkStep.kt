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

import com.intellij.ide.wizard.AbstractNewProjectWizardStep
import com.intellij.ide.wizard.NewProjectWizardStep
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.project.Project
import com.intellij.ui.dsl.builder.COLUMNS_LARGE
import com.intellij.ui.dsl.builder.Panel
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.columns
import java.io.File

/**
 * New Project Wizard step that prompts the user for the Azora SDK path.
 *
 * Displays a folder-picker field with validation ensuring the selected
 * directory exists and contains `bin/azora`. On project creation, persists
 * the SDK path to [AzoraSdkSettings] and scaffolds the initial project
 * structure (`src/Main.az` and `azora.toml`).
 *
 * @param parentStep the parent wizard step this step is nested under.
 */
class AzoraSdkStep(parentStep: NewProjectWizardStep) : AbstractNewProjectWizardStep(parentStep) {

    /** Observable property bound to the SDK path text field, initialized from persisted settings. */
    private val langPathProperty = propertyGraph.property(AzoraSdkSettings.getInstance().state.langPath ?: "")

    /**
     * Builds the UI for this wizard step.
     *
     * Adds an "Azora SDK path:" row with a folder-picker text field that
     * validates the path on apply (must be non-empty, an existing directory,
     * and contain `bin/azora`).
     *
     * @param builder the DSL panel builder to add UI components to.
     */
    override fun setupUI(builder: Panel) {
        builder.row("Azora SDK path:") {
            textFieldWithBrowseButton(
                FileChooserDescriptorFactory.createSingleFolderDescriptor()
            )
                .bindText(langPathProperty)
                .columns(COLUMNS_LARGE)
                .validationOnApply {
                    val path = it.text.trim()
                    when {
                        path.isEmpty() -> error("Azora SDK path must be specified.")
                        !File(path).isDirectory -> error("Azora SDK path does not exist.")
                        !File(path, "bin/azora").exists() -> error("Invalid Azora SDK: bin/azora not found.")
                        else -> null
                    }
                }
                .comment("Directory containing the Azora executable and standard library (e.g. ~/.azoralang)")
        }
    }

    /**
     * Called after the wizard finishes to configure the newly created project.
     *
     * Persists the selected SDK path to [AzoraSdkSettings], creates the
     * `src/` directory with a `Main.az` template file containing a hello-world
     * program, and generates an `azora.toml` project manifest.
     *
     * @param project the newly created IntelliJ project.
     */
    override fun setupProject(project: Project) {
        super.setupProject(project)

        // Persist SDK path
        AzoraSdkSettings.getInstance().state.langPath = langPathProperty.get()

        // Create project structure
        val basePath = project.basePath ?: return
        val projectName = File(basePath).name.toSnakeCase()
        val srcDir = File(basePath, "src")
        srcDir.mkdirs()

        val mainFile = File(srcDir, "Main.az")
        if (!mainFile.exists()) {
            mainFile.writeText(
                """
                |package $projectName
                |
                |func main() {
                |    println("Hello world!")
                |}
                """.trimMargin() + "\n"
            )
        }

        // Create azora.toml
        val tomlFile = File(basePath, "azora.toml")
        if (!tomlFile.exists()) {
            tomlFile.writeText(generateAzoraToml(projectName))
        }
    }
}
