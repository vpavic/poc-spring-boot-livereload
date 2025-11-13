plugins {
	java
}

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(17)
	}
}

repositories {
	mavenCentral()
}

val livereload by configurations.creating

dependencies {
	implementation(platform(libs.spring.boot.dependencies))
	implementation(libs.spring.boot.devtools)

	livereload(libs.webjars.livereload)

	compileOnly(libs.servlet.api)
	compileOnly(libs.spring.webmvc)
}

val copyLiveReloadScript by tasks.registering(Copy::class) {
	from(zipTree(livereload.incoming.files.singleFile)) {
		include("**/dist/livereload.*js")
		eachFile {
			relativePath = RelativePath(true, relativePath.segments.last())
		}
		includeEmptyDirs = false
	}
	into(layout.buildDirectory.dir("resources/main/livereload"))
}

tasks.named<Copy>("processResources") {
	dependsOn(copyLiveReloadScript)
}

testing {
	@Suppress("UnstableApiUsage")
	suites {
		named<JvmTestSuite>("test") {
			useJUnitJupiter()
			dependencies {
				implementation(libs.spring.boot.starter.test)
				implementation(libs.spring.boot.starter.web)
			}
		}
	}
}
