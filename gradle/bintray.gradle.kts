/**
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */
import org.gradle.api.internal.artifacts.publish.DefaultPublishArtifact

val bintrayUser: String? = if (project.hasProperty("bintrayUser")) {
    project.property("bintrayUser") as String
} else {
    System.getenv("BINTRAY_USER")
}

val bintrayPassword: String? = if (project.hasProperty("bintrayKey")) {
    project.property("bintrayKey") as String
} else {
    System.getenv("BINTRAY_KEY")
}

extra["bintrayUser"] = bintrayUser
extra["bintrayPassword"] = bintrayPassword

if (bintrayUser == null || bintrayUser.isEmpty()) {
    // try to read from properties
    val bintrayFile = file("bintray.properties")
    if (bintrayFile.exists()) {
        val props = java.util.Properties()
        props.load(bintrayFile.reader())
        extra["bintrayUser"] = props.getProperty("bintrayUser", "")
        extra["bintrayPassword"] = props.getProperty("bintrayPassword", "")
    }
}

logger.lifecycle("Bintray user: ${extra["bintrayUser"]}")

allprojects {
    apply(plugin = "com.jfrog.artifactory-upload")

    configure<org.jfrog.gradle.plugin.artifactory.dsl.ArtifactoryPluginConvention> {
        setContextUrl("https://oss.jfrog.org")
        resolve(delegateClosureOf<org.jfrog.gradle.plugin.artifactory.dsl.ResolverConfig> {
            repository(delegateClosureOf<groovy.lang.GroovyObject> {
                setProperty("repoKey", "libs-release")
            })
        })
        publish(delegateClosureOf<org.jfrog.gradle.plugin.artifactory.dsl.PublisherConfig> {
            repository(delegateClosureOf<groovy.lang.GroovyObject> {
                setProperty("repoKey", "oss-snapshot-local") //The Artifactory repository key to publish to
                //when using oss.jfrog.org the credentials are from Bintray. For local build we expect them to be found in
                //~/.gradle/gradle.properties, otherwise to be set in the build server
                setProperty("username", rootProject.extra["bintrayUser"])
                setProperty("password", rootProject.extra["bintrayPassword"])
            })
        })
    }
}

tasks.named("artifactoryPublish") {
    extra["mavenDescriptor"] = file("$projectDir/target/poms/pom-groovy.xml")
    doFirst {
        val curDate = java.util.Date()
        val additionalFiles = mutableListOf(
            gradleDeployDetails(
                DefaultPublishArtifact("groovy-all", "jar", "jar", null,
                    curDate, file("$projectDir/target/libs/groovy-all-${version}.jar")),
                "artifacts",
                "org/codehaus/groovy/groovy-all/${version}/groovy-all-${version}.jar"
            ),
            gradleDeployDetails(
                DefaultPublishArtifact("groovy-all", "jar", "jar", "sources",
                    curDate, file("$projectDir/target/libs/groovy-all-${version}-sources.jar")),
                "artifacts",
                "org/codehaus/groovy/groovy-all/${version}/groovy-all-${version}-sources.jar"
            ),
            gradleDeployDetails(
                DefaultPublishArtifact("groovy-all", "jar", "jar", "javadoc",
                    curDate, file("$projectDir/target/libs/groovy-all-${version}-javadoc.jar")),
                "artifacts",
                "org/codehaus/groovy/groovy-all/${version}/groovy-all-${version}-javadoc.jar"
            ),
            gradleDeployDetails(
                DefaultPublishArtifact("groovy-all", "jar", "jar", "groovydoc",
                    curDate, file("$projectDir/target/libs/groovy-all-${version}-groovydoc.jar")),
                "artifacts",
                "org/codehaus/groovy/groovy-all/${version}/groovy-all-${version}-groovydoc.jar"
            ),
            gradleDeployDetails(
                DefaultPublishArtifact("groovy-all", "jar", "jar", "indy",
                    curDate, file("$projectDir/target/libs/groovy-all-${version}-indy.jar")),
                "artifacts",
                "org/codehaus/groovy/groovy-all/${version}/groovy-all-${version}-indy.jar"
            ),
            gradleDeployDetails(
                DefaultPublishArtifact("groovy-all", "pom", "pom", null,
                    curDate, file("$projectDir/target/poms/pom-all.xml")),
                "artifacts",
                "org/codehaus/groovy/groovy-all/${version}/groovy-all-${version}.pom"
            ),
            gradleDeployDetails(
                DefaultPublishArtifact("groovy-binary", "zip", "zip", null,
                    curDate, file("$projectDir/target/distributions/groovy-binary-${version}.zip")),
                "artifacts",
                "org/codehaus/groovy/groovy-binary/${version}/groovy-binary-${version}.zip"
            ),
            gradleDeployDetails(
                DefaultPublishArtifact("groovy-binary", "pom", "pom", null,
                    curDate, file("$projectDir/target/poms/pom-binary.xml")),
                "artifacts",
                "org/codehaus/groovy/groovy-binary/${version}/groovy-binary-${version}.pom"
            )
        )
        
        tasks.withType<Jar>().matching { it.name.startsWith("backport") }.all {
            additionalFiles.add(
                gradleDeployDetails(
                    DefaultPublishArtifact(archiveBaseName.get(), "jar", "jar", archiveClassifier.orNull, curDate, archiveFile.get().asFile),
                    "artifacts",
                    "org/codehaus/groovy/${archiveBaseName.get()}/${version}/${archiveFileName.get()}"
                )
            )
            if (archiveClassifier.orNull == null || archiveClassifier.get().isEmpty()) {
                additionalFiles.add(
                    gradleDeployDetails(
                        DefaultPublishArtifact(archiveBaseName.get(), "pom", "pom", null, curDate,
                            file("$projectDir/target/poms/pom-${archiveBaseName.get().removePrefix("groovy-")}.xml")),
                        "artifacts",
                        "org/codehaus/groovy/${archiveBaseName.get()}/${version}/${archiveBaseName.get()}-${version}.pom"
                    )
                )
            }
        }
        additionalFiles.forEach { (this as org.jfrog.gradle.plugin.artifactory.task.ArtifactoryTask).deployDetails.add(it) }
    }
}

tasks.named("artifactoryPublish") {
    dependsOn("backportJars")
}
