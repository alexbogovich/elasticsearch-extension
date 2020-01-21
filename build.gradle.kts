import com.jfrog.bintray.gradle.BintrayExtension

plugins {
    `java-library`
    `maven-publish`
    kotlin("jvm") version "1.3.61"
    id("com.jfrog.bintray") version "1.8.4"
}

group = "com.alexbogovich"
version = "0.0.1"

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.3.3")

    compileOnly("org.elasticsearch.client:elasticsearch-rest-high-level-client:7.5.1")
    compileOnly("org.elasticsearch:elasticsearch:7.5.1")
    compileOnly("org.elasticsearch.client:elasticsearch-rest-client:7.5.1")
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

tasks {
    compileKotlin {
        kotlinOptions.jvmTarget = "1.8"
    }
    compileTestKotlin {
        kotlinOptions.jvmTarget = "1.8"
    }
    withType(JavaCompile::class) {
        options.compilerArgs.add("-parameters")
    }
}

val sourcesJar by tasks.registering(Jar::class) {
    archiveClassifier.set("sources")
    from(sourceSets.main.get().allSource)
}

publishing {
    publications {
        register("mavenJava", MavenPublication::class) {
            from(components["java"])
            artifact(sourcesJar.get())
        }
    }
}

bintray {
    user = System.getenv("BINTRAY_USER")
    key = System.getenv("BINTRAY_KEY")
    publish = true
    override = true
    setPublications("mavenJava")
    pkg(delegateClosureOf<BintrayExtension.PackageConfig> {
        repo = "repo"
        name = project.name
        userOrg = "alexbogovich"
        websiteUrl = "https://github.com/alexbogovich/${project.name}"
        githubRepo = "https://github.com/alexbogovich/${project.name}"
        vcsUrl = "https://github.com/alexbogovich/${project.name}"
        description = "Kotlin extension functions for elasticsearch"
        setLabels("Elasticsearch")
        setLicenses("MIT")
    })
}
