import javax.inject.Inject

plugins {
    id("java-library")
    id("maven-publish")
    id("xyz.jpenilla.run-paper") version "3.1.0"
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:26.3.build.+")
}

java {
    toolchain.languageVersion = JavaLanguageVersion.of(25)
}

// "gradlew publishToMavenLocal" makes the API usable in other projects:
// repositories { mavenLocal() }  +  compileOnly("me.mangokevin:packforge:1.0-SNAPSHOT")
publishing {
    publications {
        create<MavenPublication>("maven") {
            artifactId = "packforge"
            from(components["java"])
        }
    }
}

tasks {
    runServer {
        // Standalone test server in run/
        minecraftVersion("26.3")
        jvmArgs("-Xms2G", "-Xmx2G")
    }

    processResources {
        val props = mapOf("version" to version)
        filesMatching("paper-plugin.yml") {
            expand(props)
        }
    }

    // "gradlew deploy" copies the jar into every folder listed in deployDirs (gradle.properties)
    register<Deploy>("deploy") {
        jarFile = jar.flatMap { it.archiveFile }
        targets = providers.gradleProperty("deployDirs").getOrElse("")
            .split(";").map { it.trim() }.filter { it.isNotEmpty() }.map { file(it).absolutePath }
    }
}

abstract class Deploy : DefaultTask() {
    @get:InputFile
    abstract val jarFile: RegularFileProperty

    @get:Input
    abstract val targets: ListProperty<String>

    @get:Inject
    abstract val fs: FileSystemOperations

    @TaskAction
    fun deploy() {
        for (target in targets.get()) {
            fs.copy {
                from(jarFile)
                into(target)
            }
            logger.lifecycle("Copied ${jarFile.get().asFile.name} to $target")
        }
    }
}
