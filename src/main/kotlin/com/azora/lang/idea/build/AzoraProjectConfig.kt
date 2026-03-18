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

import com.azora.lang.idea.run.*
import com.intellij.execution.RunManager
import com.intellij.execution.configurations.ConfigurationTypeUtil
import com.intellij.openapi.application.*
import com.intellij.openapi.components.Service
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.event.*
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import com.intellij.ui.EditorNotifications
import java.io.File

/**
 * Parsed representation of an `azora.toml` project configuration.
 *
 * @param name the project name from the TOML `name` field.
 * @param version the project version from the TOML `version` field.
 * @param targets the list of compilation/execution targets (e.g. `"interpret"`, `"native"`).
 * @param entry the entry point file name (defaults to `"Main.az"`).
 * @param src the source directory relative to the project root (defaults to `"src"`).
 */
data class AzoraConfig(
    val name: String = "",
    val version: String = "",
    val targets: List<String> = listOf("interpret"),
    val entry: String = "Main.az",
    val src: String = "src"
)

/** The set of all recognized Azora compilation/execution target identifiers. */
val VALID_TARGETS = setOf(
    "interpret",
    "native",
    "web-js",
    "web-wasm",
    "kotlin-jvm",
    "kmp",
    "csharp",
    "python"
)

/**
 * Project-level service that manages the Azora build configuration.
 *
 * Parses `azora.toml` from the project root, tracks whether a sync is needed
 * (when the file is edited), and creates/updates run configurations for each
 * valid target when [sync] is called.
 *
 * @param project the IntelliJ project this service belongs to.
 */
@Service(Service.Level.PROJECT)
class AzoraProjectConfigService(private val project: Project) {

    /** The currently parsed [AzoraConfig], updated on [sync]. */
    var config: AzoraConfig = AzoraConfig()
        private set

    /** Whether the `azora.toml` has been modified since the last [sync]. */
    var needsSync: Boolean = false
        private set

    init {
        // Listen for document changes (real-time as you type)
        EditorFactory.getInstance().eventMulticaster.addDocumentListener(object : DocumentListener {
            override fun documentChanged(event: DocumentEvent) {
                val doc = event.document
                val file = FileDocumentManager.getInstance().getFile(doc) ?: return
                if (file.name == "azora.toml") {
                    needsSync = true
                    EditorNotifications.getInstance(project).updateAllNotifications()
                }
            }
        }, project)
    }

    /**
     * Parses the `azora.toml` file and recreates run configurations for all valid targets.
     *
     * Resets [needsSync] to `false` after a successful sync.
     */
    fun sync() {
        val basePath = project.basePath ?: return
        val tomlFile = File(basePath, "azora.toml")
        if (!tomlFile.exists()) return

        config = parseToml(tomlFile.readText())
        needsSync = false

        // Recreate run configurations for all valid targets
        createRunConfigurations()
    }

    /**
     * Removes all existing Azora run configurations and creates new ones
     * for each valid target defined in [config].
     *
     * The first target's configuration is set as the selected run configuration.
     */
    private fun createRunConfigurations() {
        ApplicationManager.getApplication().invokeLater {
            WriteAction.run<RuntimeException> {
                val runManager = RunManager.getInstance(project)
                val type = ConfigurationTypeUtil.findConfigurationType(AzoraConfigurationType::class.java)
                val factory = type.configurationFactories[0]

                val basePath = project.basePath ?: return@run
                val entryPath = File(basePath, config.src + "/" + config.entry).absolutePath

                // Remove old Azora run configurations
                runManager.allSettings
                    .filter { it.type == type }
                    .forEach { runManager.removeConfiguration(it) }

                // Only create configs for valid targets
                val validTargets = config.targets.filter { it in VALID_TARGETS }

                var first = true
                for (target in validTargets) {
                    val label = target.replaceFirstChar { it.uppercase() }
                    val rc = factory.createTemplateConfiguration(project) as AzoraRunConfiguration
                    rc.filePath = entryPath
                    rc.target = target
                    rc.name = "Run ${config.entry.removeSuffix(".az")} [$label]"

                    val settings = runManager.createConfiguration(rc, factory)
                    runManager.addConfiguration(settings)

                    if (first) {
                        runManager.selectedConfiguration = settings
                        first = false
                    }
                }
            }
        }
    }

    companion object {

        /**
         * Returns the [AzoraProjectConfigService] instance for the given [project].
         *
         * @param project the IntelliJ project.
         * @return the project-level config service.
         */
        fun getInstance(project: Project): AzoraProjectConfigService =
            project.getService(AzoraProjectConfigService::class.java)

        /**
         * Parses an `azora.toml` [content] string into an [AzoraConfig].
         *
         * Extracts `name`, `version`, `targets`, `entry`, and `src` from
         * the `[project]` section. Falls back to defaults for missing fields.
         *
         * @param content the raw TOML file content.
         * @return the parsed configuration.
         */
        fun parseToml(content: String): AzoraConfig {
            var name = ""
            var version = ""
            var targets = listOf<String>()
            var entry = "Main.az"
            var src = "src"
            var inProject = false

            for (line in content.lines()) {
                val trimmed = line.trim()
                if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
                    inProject = trimmed == "[project]"
                    continue
                }
                if (!inProject) continue
                if (!trimmed.contains("=")) continue

                val key = trimmed.substringBefore("=").trim()
                val value = trimmed.substringAfter("=").trim()

                when (key) {
                    "name" -> name = value.removeSurrounding("\"")
                    "version" -> version = value.removeSurrounding("\"")
                    "entry" -> entry = value.removeSurrounding("\"")
                    "src" -> src = value.removeSurrounding("\"")
                    "target" -> targets = listOf(value.removeSurrounding("\""))
                    "targets" -> targets = value
                        .removeSurrounding("[", "]")
                        .split(",")
                        .map { it.trim().removeSurrounding("\"") }
                        .filter { it.isNotEmpty() }
                }
            }

            return AzoraConfig(name, version, targets.ifEmpty { listOf("interpret") }, entry, src)
        }
    }
}

/**
 * [ProjectActivity] that triggers an initial [AzoraProjectConfigService.sync]
 * when the project opens, ensuring run configurations are created from `azora.toml`.
 */
class AzoraProjectStartupActivity : ProjectActivity {

    /**
     * Executes the initial sync on project startup.
     *
     * @param project the project being opened.
     */
    override suspend fun execute(project: Project) {
        AzoraProjectConfigService.getInstance(project).sync()
    }
}
