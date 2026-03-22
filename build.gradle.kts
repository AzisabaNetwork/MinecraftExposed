plugins {
    `java-library`
    alias(libs.plugins.kotlin.jvm)
}

group = "net.azisaba.exposed"
version = System.getenv("VERSION")

repositories {
    mavenCentral()
}

kotlin { jvmToolchain(21) }
java { toolchain.languageVersion.set(JavaLanguageVersion.of(21)) }

val exposed = libs.exposed

subprojects {
    group = rootProject.group
    version = rootProject.version

    apply(plugin = "java-library")
    apply(plugin = "maven-publish")
    apply(plugin = "org.jetbrains.kotlin.jvm")

    repositories {
        mavenCentral()
    }

    dependencies {
        compileOnly(exposed.core)
        testImplementation(kotlin("test"))
    }

    configure<PublishingExtension> {
        publications {
            create<MavenPublication>("mavenJava") {
                from(components["java"])
                groupId = group.toString()
                artifactId = project.name
                version = version.toString()
            }
        }
        repositories {
            maven {
                name = "azisaba"
                url = if (version.toString().contains("SNAPSHOT")) {
                    uri("https://repo.azisaba.net/repository/maven-snapshots/")
                } else {
                    uri("https://repo.azisaba.net/repository/maven-releases/")
                }
                credentials {
                    username = System.getenv("REPO_USERNAME")
                    password = System.getenv("REPO_PASSWORD")
                }
            }
        }
    }
}
