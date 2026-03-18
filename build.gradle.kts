plugins {
    id("java")
    kotlin("jvm") version "2.1.10"
    id("org.jetbrains.intellij.platform") version "2.13.1"
}

group = "com.azora.lang"
version = "0.0.1"

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

val useLocalIde = file("/Applications/Android Studio.app").exists() && System.getenv("CI") == null

dependencies {
    intellijPlatform {
        if (useLocalIde) {
            local("/Applications/Android Studio.app")
        } else {
            intellijIdeaCommunity("2025.1")
        }
    }
    testImplementation("org.junit.jupiter:junit-jupiter:5.11.4")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testRuntimeOnly("junit:junit:4.13.2")
}

kotlin {
    jvmToolchain(17)
}

intellijPlatform {
    pluginConfiguration {
        name = "Azora Language"
        version = project.version.toString()
        description = "Full language support for the Azora programming language."
        ideaVersion {
            sinceBuild = "253"
        }
        changeNotes = """
            <ul>
                <li>Initial release</li>
                <li>Syntax highlighting with semantic keyword coloring</li>
                <li>Code completion for keywords, symbols, and snippets</li>
                <li>Go-to-definition and quick documentation</li>
                <li>Error detection, brace matching, and code folding</li>
                <li>Structure view, find usages, and run configurations</li>
                <li>New Project wizard with Azora SDK configuration</li>
            </ul>
        """.trimIndent()
    }

    publishing {
        token = providers.environmentVariable("PUBLISH_TOKEN")
    }
}

tasks {
    test {
        useJUnitPlatform()
    }
    buildSearchableOptions {
        enabled = false
    }
    prepareJarSearchableOptions {
        enabled = false
    }
}
