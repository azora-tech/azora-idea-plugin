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

import com.intellij.execution.lineMarker.*
import com.intellij.icons.AllIcons
import com.intellij.psi.PsiElement

/**
 * [RunLineMarkerContributor] that places a green "Run" gutter icon next to
 * `func main()` declarations in `.az` files.
 *
 * Detects the entry point by matching PSI leaf elements whose text is `"main"`
 * and whose preceding non-whitespace sibling is the `"func"` keyword, then
 * attaches the standard run/debug executor actions to the gutter icon.
 */
class AzoraRunLineMarkerContributor : RunLineMarkerContributor() {

    /**
     * Returns run marker [Info] if [element] is the `"main"` identifier token
     * in a top-level `func main()` declaration inside a `.az` file.
     *
     * The detection walks backward through siblings to verify that the
     * preceding non-whitespace token is `"func"`.
     *
     * @param element the PSI leaf element to inspect.
     * @return an [Info] with a run icon and executor actions, or `null` if not a match.
     */
    override fun getInfo(element: PsiElement): Info? {
        // Match on leaf elements whose text is "main" preceded by "func"
        if (element.node == null) return null
        val text = element.text ?: return null
        if (text != "main") return null

        // Check that this is a top-level "func main()" declaration
        val file = element.containingFile ?: return null
        if (file.virtualFile?.extension != "az") return null

        // Walk back to find "func" keyword before "main"
        var prev = element.prevSibling
        while (prev != null && prev.text?.isBlank() == true) {
            prev = prev.prevSibling
        }
        if (prev?.text != "func") return null

        val actions = ExecutorAction.getActions(0)
        return Info(AllIcons.RunConfigurations.TestState.Run, actions) { "Run main()" }
    }
}
