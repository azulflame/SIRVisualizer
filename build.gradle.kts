import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.gradle.jvm.tasks.Jar


plugins {
	kotlin("jvm") version "1.4.30"
	id("org.openjfx.javafxplugin") version "0.0.8"
	application
}

javafx {
    version = "11.0.2"
    modules = listOf("javafx.controls", "javafx.graphics")
}

application {
	mainClassName = "me.toddbensmiller.sirvisual.MainKt"
}

group = "me.toddbensmiller.sirvisual"
version = "1.0-SNAPSHOT"

repositories {
	maven("https://www.jitpack.io")
	mavenCentral()
}
dependencies {
/*    testImplementation(kotlin("test-junit"))*/
	// imgui deps
	implementation("no.tornado:tornadofx:1.7.20")
	implementation("com.natpryce:konfig:1.6.10.0")    // for the properties file
	implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.4.1")
}


tasks.withType<KotlinCompile> {
	kotlinOptions.jvmTarget = "11"
}

val fatJar = task("fatJar", type = Jar::class) {
    baseName = "${project.name}-fat"
    manifest {
        attributes["Implementation-Title"] = "SIR Visualizer"
        attributes["Implementation-Version"] = version
        attributes["Main-Class"] = "me.toddbensmiller.sirvisual.Main"
    }
    from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })
    with(tasks.jar.get() as CopySpec)
}



tasks {
	"build" {
		//dependsOn(fatJar)
	}
}
