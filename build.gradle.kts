plugins {
    alias(libs.plugins.fabric.loom)
    alias(libs.plugins.shadow)
}

val buildNumber: String? by project
val library: Configuration by configurations.creating

base {
    archivesName = properties["archives_base_name"] as String

    version = libs.versions.minecraft.get()
    version = if (!buildNumber.isNullOrBlank()) {
        "$version-$buildNumber"
    } else {
        "$version-SNAPSHOT"
    }

    group = properties["maven_group"] as String
}

repositories {
    maven {
        name = "meteor-maven"
        url = uri("https://maven.meteordev.org/releases")
    }
    maven {
        name = "meteor-maven-snapshots"
        url = uri("https://maven.meteordev.org/snapshots")
    }
}



configurations {
    // include libraries
    implementation.configure {
        extendsFrom(library)
    }
    shadow.configure{
        extendsFrom(library)
    }
}

dependencies {
    // Fabric
    minecraft(libs.minecraft)
    implementation(libs.fabric.loader)

    // Meteor
    implementation(libs.meteor.client)

    // Library
    library(libs.java.websocket)
}

tasks {
    processResources {
        val propertyMap = mapOf(
            "version" to project.version,
            "mc_version" to libs.versions.minecraft.get(),
        )

        filesMatching("fabric.mod.json") {
            expand(propertyMap)
        }
    }

    jar {
        from("LICENSE") {
            rename { "${it}_${inputs.properties["archivesName"]}" }
        }
    }

    java {
        sourceCompatibility = JavaVersion.VERSION_25
        targetCompatibility = JavaVersion.VERSION_25
    }

    withType<JavaCompile> {
        options.encoding = "UTF-8"
        options.release = 25
    }

    shadowJar {
        configurations = listOf(project.configurations.shadow.get())

        from("LICENSE") {
            rename { "${it}_${project.base.archivesName.get()}" }
        }

        dependencies {
            exclude {
                it.moduleGroup == "org.slf4j"
            }
        }
    }

    withType<JavaCompile> {
        options.encoding = "UTF-8"
        options.release.set(25)
    }

    build {
        dependsOn(shadowJar)
    }
}
