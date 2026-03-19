plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "2.1.20"
    id("org.jetbrains.intellij.platform") version "2.10.2"
}

group = "org.azdtasks"
version = "2.1"

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
    mavenLocal()
}

// Read more: https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin.html
dependencies {
    intellijPlatform {
        intellijIdea("2025.3.3")
        testFramework(org.jetbrains.intellij.platform.gradle.TestFrameworkType.Platform)

        testImplementation("junit:junit:4.13.2")

        bundledPlugin("com.intellij.tasks")
    }
    implementation("org.azdtasks:azdcore:1.2") {
        exclude(group = "org.slf4j", module = "slf4j-api")
        exclude(group = "org.slf4j", module = "slf4j-reload4j")
        exclude(group = "org.slf4j", module = "jul-to-slf4j")
    }
}

intellijPlatform {
    pluginConfiguration {
        ideaVersion {
            sinceBuild = "252.25557"
        }

        changeNotes = """
            Initial version
        """.trimIndent()
    }
}

tasks {
    // Set the JVM compatibility versions
    withType<JavaCompile> {
        sourceCompatibility = "21"
        targetCompatibility = "21"
    }
    test {
        useJUnit()
        systemProperty("jdk.httpclient.HttpClient.log", "requests,headers,content,errors")
    }
    runIde {
//        systemProperty("idea.log.console.level", "DEBUG")
//        systemProperty("idea.log.debug.categories", "#org.azdtasks")
//        systemProperty("jdk.httpclient.HttpClient.log", "requests,headers,content,errors")
    }
}


kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
    }
}
