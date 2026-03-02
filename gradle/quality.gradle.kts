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

import groovy.text.markup.MarkupTemplateEngine
import groovy.text.markup.TemplateConfiguration

allprojects {
    //apply(plugin = "com.github.hierynomus.license")
    apply(plugin = "checkstyle")
    apply(plugin = "codenarc")
    apply(plugin = "findbugs")
    
    configurations.named("codenarc") {
        // because we will rely on the version we build
        // because version ranges are evil
        // and because it causes bnd to be brought transitively
        // I am unsure why; says it is required by groovy-ant but its pom.xml does not declare so
        exclude(group = "org.codehaus.groovy")
    }

//    license {
//        header rootProject.file("gradle/LICENSE.txt")
//        include "**/*.groovy"
//        include "**/*.java"
//        include "**/*.properties"
//        include "**/*.js"
//        include "**/*.css"
//        include "**/*.html"
//        include "**/*.xml"
//        exclude "org/codehaus/groovy/antlr/**"
//        exclude "reloading/**" // test resources for documentation of reloading
//        exclude "includes/**" // documentation resources included as snippets of code
//        //dryRun = true
//        ignoreFailures = true
//        //skipExistingHeaders = true
//        //ext.year = Calendar.instance.get(Calendar.YEAR)
//    }

    // don't fail build on CodeNarc tasks
    tasks.withType<CodeNarc> {
        ignoreFailures = true
        configFile = file("$rootDir/config/codenarc/codenarc.groovy")
        codenarcClasspath = rootProject.sourceSets["main"].output +
            project(":groovy-templates").sourceSets["main"].output +
            project(":groovy-xml").sourceSets["main"].output +
            configurations["compile"] +
            files(configurations["codenarc"].filter { !(it.name.contains("groovy") || it.name.contains("junit")) })
    }

    tasks.withType<Checkstyle> {
        isShowViolations = false
        ignoreFailures = true
        configFile = file("$rootDir/config/checkstyle/checkstyle.xml")
        configProperties = mapOf("rootProject.projectDir" to rootProject.projectDir.toString())
        val reportFile = file("${layout.buildDirectory.get()}/reports/checkstyle/${name}.xml")
        reports {
            include("**/*.java")
            xml.destination = reportFile
        }
        val checkstyleTask = this
        tasks.register("${name}Report") {
            val configDir = file("$rootDir/config/checkstyle")
            val templateFile = "checkstyle-report.groovy"
            val htmlReportFile = file("${layout.buildDirectory.get()}/reports/checkstyle/${checkstyleTask.name}.html")
            inputs.file(file("$configDir/$templateFile"))
            inputs.file(reportFile)
            outputs.file(htmlReportFile)

            doLast {
                if (reportFile.exists()) {
                    val templateConfiguration = TemplateConfiguration().apply {
                        isAutoIndent = true
                        isAutoNewLine = true
                    }
                    val engine = MarkupTemplateEngine(this.javaClass.classLoader, configDir, templateConfiguration)
                    val xml = groovy.xml.XmlSlurper().parse(reportFile.reader(Charsets.UTF_8))
                    val files = mutableListOf<Map<String, Any>>()
                    xml.getProperty("file").iterator().forEach { f ->
                        val fNode = f as groovy.util.slurpersupport.Node
                        val errorsProp = fNode.getProperty("error") as groovy.util.slurpersupport.NodeChildren
                        if (errorsProp.size() > 0) {
                            val errors = mutableListOf<Map<String, String>>()
                            errorsProp.iterator().forEach { e ->
                                val eNode = e as groovy.util.slurpersupport.Node
                                var rule = eNode.attributes()["source"].toString()
                                rule = rule.substring(rule.lastIndexOf('.') + 1)
                                errors.add(mapOf(
                                    "line" to eNode.attributes()["line"].toString(),
                                    "column" to eNode.attributes()["column"].toString(),
                                    "message" to eNode.attributes()["message"].toString(),
                                    "source" to rule,
                                    "severity" to eNode.attributes()["severity"].toString()
                                ))
                            }
                            files.add(mapOf(
                                "name" to fNode.attributes()["name"].toString(),
                                "errors" to errors
                            ))
                        }
                    }
                    val model = mapOf(
                        "project" to project,
                        "files" to files
                    )
                    htmlReportFile.writer(Charsets.UTF_8).use { wrt ->
                        engine.createTemplateByPath("checkstyle-report.groovy").make(model).writeTo(wrt)
                    }
                }
            }
        }
        finalizedBy("${name}Report")
    }

    configure<FindBugsExtension> {
        // continue build despite findbug warnings
        ignoreFailures = true
        sourceSets = listOf(sourceSets["main"])
    }
    tasks.withType<FindBugs> {
        effort = "max"
        reports {
            xml.isEnabled = false
            html.isEnabled = true
        }
    }
}

apply(from = "gradle/jacoco/jacoco.gradle")
//apply(from = "gradle/binarycompatibility.gradle")
