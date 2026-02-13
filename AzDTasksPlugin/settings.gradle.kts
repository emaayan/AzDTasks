rootProject.name = "AzDTasksPlugin"
dependencyResolutionManagement {
    repositories {
        // ... other repos ....
        maven("https://www.jetbrains.com/intellij-repository/releases") // <- this helped
    }
}