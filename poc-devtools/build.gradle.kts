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

dependencies {
	implementation(platform(libs.spring.boot.dependencies))
	implementation(libs.spring.boot.devtools)
	implementation(libs.webjars.livereload)

	compileOnly(libs.servlet.api)
	compileOnly(libs.spring.webmvc)
}

testing {
	suites {
		@Suppress("UnstableApiUsage", "unused")
		val test by getting(JvmTestSuite::class) {
			useJUnitJupiter()
			dependencies {
				implementation(libs.spring.boot.starter.test)
				implementation(libs.spring.boot.starter.web)
			}
		}
	}
}
