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
import com.intellij.execution.actions.*
import com.intellij.execution.configurations.*
import com.intellij.openapi.util.Ref
import com.intellij.psi.PsiElement

/**
 * [LazyRunConfigurationProducer] that creates an "Interpret" run configuration
 * for `.az` files containing a `func main()` entry point.
 *
 * Only produces a configuration when `"interpret"` is listed as a target
 * in the project's [AzoraProjectConfigService].
 */
class AzoraRunInterpretProducer : LazyRunConfigurationProducer<AzoraRunConfiguration>() {

    /**
     * Returns the first [ConfigurationFactory] registered under [AzoraConfigurationType].
     */
    override fun getConfigurationFactory(): ConfigurationFactory =
        ConfigurationTypeUtil.findConfigurationType(AzoraConfigurationType::class.java)
            .configurationFactories[0]

    /**
     * Checks whether the given [configuration] matches the current [context].
     *
     * Returns `true` if the context file is a `.az` file whose path and
     * `"interpret"` target match the configuration.
     *
     * @param configuration the existing run configuration to check.
     * @param context the current editor/action context.
     * @return `true` if the configuration was produced from this context.
     */
    override fun isConfigurationFromContext(
        configuration: AzoraRunConfiguration,
        context: ConfigurationContext
    ): Boolean {
        val file = context.location?.psiElement?.containingFile?.virtualFile ?: return false
        return file.extension == "az" && configuration.filePath == file.path && configuration.target == "interpret"
    }

    /**
     * Populates [configuration] from the current [context] if applicable.
     *
     * Sets the file path, target to `"interpret"`, and a descriptive name.
     * Returns `false` if the context file is not a `.az` file, does not
     * contain `func main()`, or `"interpret"` is not a configured target.
     *
     * @param configuration the run configuration to populate.
     * @param context the current editor/action context.
     * @param sourceElement reference to the PSI element that triggered the action.
     * @return `true` if the configuration was successfully set up.
     */
    override fun setupConfigurationFromContext(
        configuration: AzoraRunConfiguration,
        context: ConfigurationContext,
        sourceElement: Ref<PsiElement>
    ): Boolean {
        val file = context.location?.psiElement?.containingFile ?: return false
        val vFile = file.virtualFile ?: return false
        if (vFile.extension != "az") return false
        if (!file.text.contains(Regex("""func\s+main\s*\("""))) return false

        val targets = try {
            AzoraProjectConfigService.getInstance(context.project).config.targets
        } catch (_: Exception) { listOf("interpret") }

        if ("interpret" !in targets) return false

        configuration.filePath = vFile.path
        configuration.target = "interpret"
        configuration.name = "Run ${vFile.nameWithoutExtension} [Interpret]"
        return true
    }
}

/**
 * [LazyRunConfigurationProducer] that creates a "Native" run configuration
 * for `.az` files containing a `func main()` entry point.
 *
 * Only produces a configuration when `"native"` is listed as a target
 * in the project's [AzoraProjectConfigService].
 */
class AzoraRunNativeProducer : LazyRunConfigurationProducer<AzoraRunConfiguration>() {

    /**
     * Returns the first [ConfigurationFactory] registered under [AzoraConfigurationType].
     */
    override fun getConfigurationFactory(): ConfigurationFactory =
        ConfigurationTypeUtil.findConfigurationType(AzoraConfigurationType::class.java)
            .configurationFactories[0]

    /**
     * Checks whether the given [configuration] matches the current [context].
     *
     * Returns `true` if the context file is a `.az` file whose path and
     * `"native"` target match the configuration.
     *
     * @param configuration the existing run configuration to check.
     * @param context the current editor/action context.
     * @return `true` if the configuration was produced from this context.
     */
    override fun isConfigurationFromContext(
        configuration: AzoraRunConfiguration,
        context: ConfigurationContext
    ): Boolean {
        val file = context.location?.psiElement?.containingFile?.virtualFile ?: return false
        return file.extension == "az" && configuration.filePath == file.path && configuration.target == "native"
    }

    /**
     * Populates [configuration] from the current [context] if applicable.
     *
     * Sets the file path, target to `"native"`, and a descriptive name.
     * Returns `false` if the context file is not a `.az` file, does not
     * contain `func main()`, or `"native"` is not a configured target.
     *
     * @param configuration the run configuration to populate.
     * @param context the current editor/action context.
     * @param sourceElement reference to the PSI element that triggered the action.
     * @return `true` if the configuration was successfully set up.
     */
    override fun setupConfigurationFromContext(
        configuration: AzoraRunConfiguration,
        context: ConfigurationContext,
        sourceElement: Ref<PsiElement>
    ): Boolean {
        val file = context.location?.psiElement?.containingFile ?: return false
        val vFile = file.virtualFile ?: return false
        if (vFile.extension != "az") return false
        if (!file.text.contains(Regex("""func\s+main\s*\("""))) return false

        val targets = try {
            AzoraProjectConfigService.getInstance(context.project).config.targets
        } catch (_: Exception) { listOf("interpret") }

        if ("native" !in targets) return false

        configuration.filePath = vFile.path
        configuration.target = "native"
        configuration.name = "Run ${vFile.nameWithoutExtension} [Native]"
        return true
    }
}
