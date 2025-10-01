plugins {
	java
	alias(libs.plugins.spring.boot)
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
	implementation(libs.spring.boot.starter.thymeleaf)
	implementation(libs.spring.boot.starter.web)

	developmentOnly(project(":poc-devtools"))
}

testing {
	@Suppress("UnstableApiUsage")
	suites {
		named<JvmTestSuite>("test") {
			useJUnitJupiter()
			dependencies {
				implementation(libs.spring.boot.starter.test)
			}
		}
	}
}
