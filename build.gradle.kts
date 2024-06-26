import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.gradle.jvm.tasks.Jar


plugins {
	kotlin("jvm") version "1.6.10"
	id("org.openjfx.javafxplugin") version "0.0.8"
	application
}

javafx {
    version = "11.0.2"
    modules = listOf("javafx.controls", "javafx.graphics")
}

application {
//	getMainClass().set("me.toddbensmiller.sirvisual.MainKt")
}

group = "me.toddbensmiller.sirvisual"
version = "1.0"

repositories {
	maven("https://www.jitpack.io")
	mavenCentral()
}
dependencies {
	implementation("no.tornado:tornadofx:1.7.20")
	implementation("com.natpryce:konfig:1.6.10.0")
	implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.0-RC2")
	implementation("org.jetbrains.kotlin:kotlin-reflect:1.6.10")
}


tasks.withType<KotlinCompile> {
	kotlinOptions.jvmTarget = "11"
}

val fatJar = task("fatJar", type = Jar::class) {
	duplicatesStrategy = DuplicatesStrategy.EXCLUDE
	archiveBaseName.set("${project.name}-fat")
    manifest {
        attributes["Implementation-Title"] = "SIR Visualizer"
        attributes["Implementation-Version"] = archiveVersion
        attributes["Main-Class"] = "me.toddbensmiller.sirvisual.MainKt"
    }
    from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })
    with(tasks.jar.get() as CopySpec)
}

tasks.withType<Jar>() {

	duplicatesStrategy = DuplicatesStrategy.EXCLUDE

	manifest {
		attributes["Main-Class"] = "me.toddbensmiller.sirvisual.MainKt"
	}

	configurations["compileClasspath"].forEach { file: File ->
		from(zipTree(file.absoluteFile))
	}
}


tasks {
	"build" {
		dependsOn(fatJar)
	}
}
