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

// most of this is just a "hack" to break the circular dependencies between projects
// which exist because Eclipse does not distinguish between build phases (e.g. compile, test, runtime)
allprojects {
    apply(plugin = "eclipse")
    
    configure<org.gradle.plugins.ide.eclipse.model.EclipseModel> {
        jdt {
            sourceCompatibility = JavaVersion.VERSION_1_7
            targetCompatibility = JavaVersion.VERSION_1_7
        }
        
        classpath {
            file {
                whenMerged {
                    if (this is org.gradle.plugins.ide.eclipse.model.Classpath) {
                        entries.removeAll { entry -> entry.path == "/groovy-groovydoc" }
                        entries.removeAll { entry -> entry.path.contains("groovy-ant") }
                        entries.removeAll { entry -> entry.path.contains("target") }
                        entries.distinctBy { entry -> entry.path }
                    }
                }
                withXml {
                    val node = asNode()
                    node.appendNode("classpathentry", mapOf(
                        "kind" to "lib",
                        "path" to rootProject.tasks.named<Jar>("jar").get().archiveFile.get().asFile.path
                    ))
                }
            }
        }
    }
    
    tasks.named("eclipse") {
        doLast {
            val groovyPrefs = file("${project.projectDir}/.settings/org.eclipse.jdt.groovy.core.prefs")
            if (!groovyPrefs.exists()) {
                groovyPrefs.appendText("groovy.compiler.level=-1\n")
            }
        }
    }
}

configure<org.gradle.plugins.ide.eclipse.model.EclipseModel> {
    classpath {
        file {
            whenMerged {
                if (this is org.gradle.plugins.ide.eclipse.model.Classpath) {
                    entries.find { it.path.contains("src/main") }?.let {
                        if (it is org.gradle.plugins.ide.eclipse.model.SourceFolder) {
                            it.path = "/groovy/src/main"
                            it.includes = emptyList()
                        }
                    }
                    entries.forEach { entry ->
                        if (entry.path == "src/test" && entry is org.gradle.plugins.ide.eclipse.model.SourceFolder) {
                            entry.excludes = listOf("groovy/PropertyTest.groovy")
                        }
                    }
                    entries.removeAll { it.path == "/groovy-test" }
                    entries.removeAll { it.path.contains("subprojects") }
                    entries.removeAll { it.path.contains("examples") }
                }
            }
            withXml {
                val node = asNode()
                listOf("groovy-test", "groovy-groovydoc", "groovy-jmx", "groovy-xml", "groovy-ant").forEach { proj ->
                    node.appendNode("classpathentry", mapOf(
                        "kind" to "src",
                        "path" to "/groovy/subprojects/$proj/src/main/groovy"
                    ))
                    node.appendNode("classpathentry", mapOf(
                        "kind" to "src",
                        "path" to "/groovy/subprojects/$proj/src/main/java"
                    ))
                }
                node.appendNode("classpathentry", mapOf(
                    "kind" to "src",
                    "path" to "/groovy/subprojects/groovy-templates/src/main/groovy"
                ))
            }
        }
    }
}
