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

import com.azora.lang.idea.project.AzoraSdkSettings
import com.intellij.execution.Executor
import com.intellij.execution.configurations.*
import com.intellij.execution.process.*
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.project.Project
import java.io.File

/**
 * Persistent options for [AzoraRunConfiguration].
 *
 * Stores the source file path and the compilation/execution target
 * (e.g. `"interpret"`, `"native"`) across IDE restarts.
 */
class AzoraRunConfigurationOptions : RunConfigurationOptions() {

    /** The absolute path to the `.az` source file to run. */
    var filePath by string("")

    /** The execution target, defaults to `"interpret"`. */
    var target by string("interpret")
}

/**
 * Run configuration for executing Azora programs.
 *
 * Supports two execution modes:
 * - **interpret** - runs the program directly via `azora-build run --target interpret`.
 * - **native** (and other compile targets) - builds first, then runs via
 *   `azora-build build --target <target> && azora-build run --target <target>`.
 *
 * @param project the IntelliJ project this configuration belongs to.
 * @param factory the [ConfigurationFactory] that created this configuration.
 * @param name the user-visible name of this run configuration.
 */
class AzoraRunConfiguration(
    project: Project,
    factory: ConfigurationFactory,
    name: String
) : RunConfigurationBase<AzoraRunConfigurationOptions>(project, factory, name) {

    /** The absolute path to the `.az` source file to run. Delegates to [AzoraRunConfigurationOptions.filePath]. */
    var filePath: String
        get() = options.filePath ?: ""
        set(value) { options.filePath = value }

    /** The execution target (e.g. `"interpret"`, `"native"`). Delegates to [AzoraRunConfigurationOptions.target]. */
    var target: String
        get() = options.target ?: "interpret"
        set(value) { options.target = value }

    /** Returns the typed [AzoraRunConfigurationOptions] for this configuration. */
    override fun getOptions(): AzoraRunConfigurationOptions =
        super.getOptions() as AzoraRunConfigurationOptions

    /** Returns a new [AzoraRunConfigurationEditor] for editing this configuration's settings. */
    override fun getConfigurationEditor() = AzoraRunConfigurationEditor(project)

    /**
     * Validates the configuration before execution.
     *
     * @throws RuntimeConfigurationError if the file path is empty, the file does not exist,
     *   or the Azora SDK `bin/azora` binary is not found.
     */
    override fun checkConfiguration() {
        if (filePath.isEmpty()) {
            throw RuntimeConfigurationError("Azora file is not specified.")
        }
        if (!File(filePath).exists()) {
            throw RuntimeConfigurationError("Azora file does not exist: $filePath")
        }
        val sdkPath = AzoraSdkSettings.getInstance().sdkPath()
        val azoraBin = File(sdkPath, "bin/azora")
        if (!azoraBin.exists()) {
            throw RuntimeConfigurationError("Azora SDK not found at '$sdkPath'. bin/azora missing.")
        }
    }

    /**
     * Creates the [RunProfileState] that launches the Azora process.
     *
     * For compile targets (e.g. `"native"`), runs a build step followed by execution.
     * For `"interpret"`, runs the program directly without a separate build step.
     *
     * @param executor the executor (Run, Debug, etc.) requesting the launch.
     * @param environment the execution environment providing project and context.
     * @return a [CommandLineState] that starts the appropriate `azora-build` process.
     */
    override fun getState(executor: Executor, environment: ExecutionEnvironment): RunProfileState {
        return object : CommandLineState(environment) {
            /**
             * Builds and starts the OS process for running the Azora program.
             *
             * @return a colored [OSProcessHandler] with a termination listener attached.
             */
            override fun startProcess(): OSProcessHandler {
                val sdkPath = AzoraSdkSettings.getInstance().sdkPath()
                val basePath = project.basePath ?: ""

                val buildBin = File(sdkPath, "bin/azora-build").absolutePath

                // For compile targets: build first (to produce output files), then run
                // For interpret: just run
                val commandLine = if (target != "interpret") {
                    GeneralCommandLine("sh", "-c", "$buildBin build --target $target && $buildBin run --target $target")
                        .withWorkDirectory(basePath)
                        .withCharset(Charsets.UTF_8)
                } else {
                    GeneralCommandLine(buildBin, "run", "--target", target)
                        .withWorkDirectory(basePath)
                        .withCharset(Charsets.UTF_8)
                }

                val handler = ProcessHandlerFactory.getInstance().createColoredProcessHandler(commandLine)
                ProcessTerminatedListener.attach(handler)
                return handler
            }
        }
    }
}
