plugins {
	application
}

repositories {
	mavenCentral()

	maven {
		name = "Fabric"
		url = uri("https://maven.fabricmc.net/")
	}
}

dependencies {
	// TODO: display license information in app
	// TODO: shade deps
	implementation("net.fabricmc:fabric-installer:0.9.0")
	implementation("commons-validator:commons-validator:1.7")
	implementation("com.squareup.okio:okio:3.1.0")
	implementation("com.moandjiezana.toml:toml4j:0.7.2")
	implementation("com.google.code.gson:gson:2.9.0")
	implementation("commons-io:commons-io:2.11.0")
	implementation("org.apache.commons:commons-lang3:3.12.0")
	implementation("com.formdev:flatlaf:2.4")
}

application {
	mainClass.set("link.infra.packwiz.vanillainstaller.VanillaInstaller")
}

// TODO: META-INF
// TODO: build fabric-installer-native-bootstrap .exe