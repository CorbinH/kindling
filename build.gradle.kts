import java.time.LocalDate

plugins {
    alias(libs.plugins.kotlin)
    alias(libs.plugins.serialization)
    alias(libs.plugins.spotless)
    alias(libs.plugins.conveyor)
    alias(libs.plugins.dokka)
    application

    // Beta
    alias(libs.plugins.shadow)
    alias(libs.plugins.runtime)
}

repositories {
    mavenCentral()
    maven("https://nexus.inductiveautomation.com/repository/public/")
}

dependencies {
    api(libs.serialization.json)
    api(libs.serialization.yaml)
    api(libs.serialization.csv)
    api(libs.xerial.jdbc)
    api(libs.hsql)
    api(libs.miglayout)
    api(libs.jide.common)
    api(libs.swingx)
    api(libs.logback)
    api(libs.jsvg)
    api(libs.bundles.coroutines)
    api(libs.bundles.flatlaf)
    api(libs.bundles.ignition) // Gradle will not include these packages unless they are transitive
    api(libs.poi)
    api(libs.excelkt) {
        // bringing in POI manually, since this wrapper appears unmaintained
        isTransitive = false
    }
    api(libs.jfreechart)
    api(libs.questdb)
    api(libs.rsyntaxtextarea)
    api(libs.bundles.jackson)
    runtimeOnly(libs.bundles.ia.transitive)
    testImplementation(libs.bundles.kotest)

    // Beta
    api(libs.bundles.ktor)
    api(libs.jpmml)
}

group = "io.github.inductiveautomation"

application {
    mainClass = "io.github.inductiveautomation.kindling.MainPanel"
    applicationDefaultJvmArgs += listOf(
        "--add-exports=java.base/sun.security.action=ALL-UNNAMED",
        "--add-exports=java.desktop/com.sun.java.swing.plaf.windows=ALL-UNNAMED",
        "--add-exports=java.desktop/apple.laf=ALL-UNNAMED",
        "-XX:+UseZGC",
        "-XX:+ZGenerational",
    )
}

val javadocDirectory = project.layout.buildDirectory.dir("javadocs")

tasks {
    test {
        useJUnitPlatform()
    }

    val download79 by registering(DownloadJavadocs::class) {
        version = "7.9"
        urls = listOf(
            "https://files.inductiveautomation.com/sdk/javadoc/ignition79/7921/allclasses-noframe.html",
            "https://docs.oracle.com/javase/8/docs/api/allclasses-noframe.html",
            "https://www.javadoc.io/static/org.python/jython-standalone/2.5.3/allclasses-noframe.html",
        )
        baseOutputDirectory = javadocDirectory
    }
    val download80 by registering(DownloadJavadocs::class) {
        version = "8.0"
        urls = listOf(
            "https://files.inductiveautomation.com/sdk/javadoc/ignition80/8.0.14/allclasses.html",
            "https://docs.oracle.com/en/java/javase/11/docs/api/allclasses.html",
            "https://www.javadoc.io/static/org.python/jython-standalone/2.7.1/allclasses-noframe.html",
        )
        baseOutputDirectory = javadocDirectory
    }
    val download81 by registering(DownloadJavadocs::class) {
        version = "8.1"
        urls = listOf(
            "https://files.inductiveautomation.com/sdk/javadoc/ignition81/8.1.48/allclasses-index.html",
            "https://docs.oracle.com/en/java/javase/17/docs/api/allclasses-index.html",
            "https://www.javadoc.io/static/org.python/jython-standalone/2.7.3/allclasses-noframe.html",
        )
        baseOutputDirectory = javadocDirectory
    }
    processResources {
        duplicatesStrategy = DuplicatesStrategy.WARN
        dependsOn(download79, download80, download81)
    }
    shadowJar {
        manifest {
            attributes["Main-Class"] = "io.github.paulgriffith.kindling.MainPanel"
        }
        archiveBaseName.set("kindling-bundle")
        archiveClassifier.set("")
        archiveVersion.set("")
        mergeServiceFiles()
    }
}

kotlin {
    jvmToolchain {
        languageVersion = libs.versions.java.map(JavaLanguageVersion::of)
        vendor = JvmVendorSpec.AMAZON
    }
    sourceSets {
        main {
            resources.srcDir(javadocDirectory)
        }
    }
    compilerOptions {
        freeCompilerArgs.addAll(
            "-Xcontext-parameters",
        )
    }
}

spotless {
    ratchetFrom = "e639479c2bef3553f16c08f8114b4a177c0ebf09"
    format("misc") {
        target("*.gradle", ".gitattributes", ".gitignore")
        trimTrailingWhitespace()
        indentWithSpaces()
        endWithNewline()
    }
    java {
        palantirJavaFormat()
        formatAnnotations()
    }
    kotlin {
        ktlint()
    }
    kotlinGradle {
        ktlint()
    }
}

runtime {
    options.set(listOf("--strip-debug", "--compress", "2", "--no-header-files", "--no-man-pages"))

    modules.set(
        listOf(
            "java.desktop",
            "java.sql",
            "java.logging",
            "java.naming",
            "java.xml",
            "jdk.zipfs",
            "jdk.crypto.ec",
        ),
    )

    jpackage {
        val currentOs = org.gradle.internal.os.OperatingSystem.current()
        val imgType = if (currentOs.isWindows) "ico" else "png"
        appVersion = project.version.toString()
        imageOptions = listOf("--icon", "src/main/resources/icons/ignition.$imgType")
        val options: Map<String, String?> = buildMap {
            put("resource-dir", "src/main/resources")
            put("vendor", "Paul Griffith")
            put("copyright", LocalDate.now().year.toString())
            put("description", "A collection of useful tools for troubleshooting Ignition")

            when {
                currentOs.isWindows -> {
                    put("win-per-user-install", null)
                    put("win-dir-chooser", null)
                    put("win-menu", null)
                    put("win-shortcut", null)
                    // random (consistent) UUID makes upgrades smoother
                    put("win-upgrade-uuid", "8e7428c8-bbc6-460a-9995-db6d8b04a690")
                }

                currentOs.isLinux -> {
                    put("linux-shortcut", null)
                }
            }
        }

        // add-exports is used to bypass Java modular restrictions
        jvmArgs = listOf("--add-exports", "java.desktop/com.sun.java.swing.plaf.windows=ALL-UNNAMED")

        installerOptions = options.flatMap { (key, value) ->
            listOfNotNull("--$key", value)
        }

        imageName = "kindling beta"
        installerName = "kindling"
        mainJar = "kindling-bundle.jar"
    }
}
