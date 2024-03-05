import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    id("java")
    id("maven-publish")
    id("com.github.johnrengelman.shadow") version "8.1.1"
    signing
}

group = "com.alessiodp.libby.maven.resolver"
version = "1.0.1"

repositories {
    mavenCentral()
}

dependencies {
    compileOnly("org.jetbrains:annotations:24.1.0")
    testCompileOnly("org.jetbrains:annotations:24.1.0")

    implementation("org.apache.maven.resolver:maven-resolver-impl:1.9.18")
    implementation("org.apache.maven.resolver:maven-resolver-supplier:1.9.18")
    implementation("org.slf4j:slf4j-nop:1.7.36")
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8

    withJavadocJar()
    withSourcesJar()
}

tasks.withType<ShadowJar> {
    archiveClassifier.set("")
    minimize()
}

tasks.jar {
    finalizedBy("shadowJar")
}

publishing {
    repositories {
        maven {
            val releaseUrl = "https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/"
            val snapshotUrl = "https://s01.oss.sonatype.org/content/repositories/snapshots/"
            url = uri(if (version.toString().endsWith("SNAPSHOT")) snapshotUrl else releaseUrl)

            credentials {
                username = (project.properties["ossrhUsername"] ?: "").toString()
                password = (project.properties["ossrhPassword"] ?: "").toString()
            }
        }

        maven {
            val releaseUrl = "https://repo.alessiodp.com/releases"
            val snapshotUrl = "https://repo.alessiodp.com/snapshots"

            url = uri(if (version.toString().endsWith("SNAPSHOT")) snapshotUrl else releaseUrl)

            credentials {
                username = (project.properties["alessiodpRepoUsername"] ?: "").toString()
                password = (project.properties["alessiodpRepoPassword"] ?: "").toString()
            }
        }
    }

    publications {
        create<MavenPublication>("mavenShadowJar") {
            from(components["java"])

            pom {
                name.set("libby-maven-resolver")
                description.set("Utility to download transitive dependencies for Libby")
                url.set("https://github.com/AlessioDP/libby-maven-resolver")

                licenses {
                    license {
                        name.set("MIT License")
                        url.set("https://opensource.org/license/mit/")
                    }
                }

                developers {
                    developer {
                        id = "AlessioDP"
                        email = "me@alessiodp.com"
                    }
                }

                scm {
                    connection = "scm:git:git://github.com/AlessioDP/libby-maven-resolver.git"
                    developerConnection = "scm:git:git@github.com:AlessioDP/libby-maven-resolver.git"
                    url = "https://github.com/AlessioDP/libby-maven-resolver"
                }
            }
        }
    }

    signing {
        setRequired {
            gradle.taskGraph.allTasks.any { it is PublishToMavenRepository }
        }
        useGpgCmd()
        sign(publishing.publications["mavenShadowJar"])
    }
}
