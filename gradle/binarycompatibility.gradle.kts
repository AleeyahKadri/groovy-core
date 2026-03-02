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
import java.lang.reflect.Modifier

buildscript {
    // this block should not be necessary, but for some reason it fails without!
    repositories {
        mavenCentral()
    }

    dependencies {
        classpath("me.champeau.gradle:japicmp-gradle-plugin:0.1.1")
    }
}

val checkBinaryCompatibility by tasks.registering {
    description = "Generates binary compatibility reports"
}
tasks.named("check") {
    dependsOn(checkBinaryCompatibility)
}

if (JavaVersion.current().isJava7Compatible) {
    allprojects {
        apply(plugin = "me.champeau.gradle.japicmp")
    }

    val referenceMinorVersion = "2.4.2"

    val prettyPrint: (Any?) -> String = { classOrMethod ->
        val obj = (classOrMethod as groovy.lang.GroovyObject).getProperty("get") as Any?
        if (obj != null) {
            val mods = (obj as java.lang.reflect.AccessibleObject).let {
                when (it) {
                    is java.lang.reflect.Method -> Modifier.toString(it.modifiers)
                    is java.lang.reflect.Constructor<*> -> Modifier.toString(it.modifiers)
                    else -> ""
                }
            }
            val longName = (obj as Any).let {
                (it as groovy.lang.GroovyObject).getProperty("longName") as String? ?: ""
            }
            "$mods $longName"
        } else {
            ""
        }
    }

    val reportGenerator: (MutableMap<String, Any?>) -> (Any) -> Unit = { model ->
        { outputProcessor ->
            val processor = outputProcessor as groovy.lang.Closure<*>

            val skipClass: (Any) -> Boolean = { c ->
                val fqn = (c as groovy.lang.GroovyObject).getProperty("fullyQualifiedName") as String
                fqn.matches(Regex(".*\\$[0-9]+$")) || // skip AIC
                        fqn.startsWith("org.codehaus.groovy.runtime.dgm$") ||
                        fqn.contains("_closure")
            }
            val skipMethod: (Any, Any) -> Boolean = { c, m ->
                skipClass(c) || (m as groovy.lang.GroovyObject).getProperty("name").toString().matches(Regex("access\\$[0-9]+"))
            }
            val violations = mutableMapOf<String, MutableMap<String, MutableList<String>>>()
                .withDefault { mutableMapOf<String, MutableList<String>>().withDefault { mutableListOf() } }

            processor.setDelegate(object {
                fun removedConstructor(c: Any, m: Any) {
                    if (!skipMethod(c, m)) {
                        val oldCtor = (m as groovy.lang.GroovyObject).getProperty("oldConstructor")
                        val ctorObj = (oldCtor as groovy.lang.GroovyObject).getProperty("get")
                        val mods = if (ctorObj is java.lang.reflect.Constructor<*>) ctorObj.modifiers else 0
                        val level = if (Modifier.isPrivate(mods)) "info" else "error"
                        val fqn = (c as groovy.lang.GroovyObject).getProperty("fullyQualifiedName") as String
                        violations.getOrPut(fqn) { mutableMapOf<String, MutableList<String>>().withDefault { mutableListOf() } }
                            .getOrPut(level) { mutableListOf() }
                            .add("Constructor ${prettyPrint(oldCtor)} has been removed")
                    }
                }

                fun removedMethod(c: Any, m: Any) {
                    if (!skipMethod(c, m)) {
                        val name = (m as groovy.lang.GroovyObject).getProperty("name") as String
                        val level = if (name.startsWith("super$")) "warning" else "error"
                        val fqn = (c as groovy.lang.GroovyObject).getProperty("fullyQualifiedName") as String
                        violations.getOrPut(fqn) { mutableMapOf<String, MutableList<String>>().withDefault { mutableListOf() } }
                            .getOrPut(level) { mutableListOf() }
                            .add("Method $name has been removed")
                    }
                }

                fun removedClass(c: Any) {
                    if (!skipClass(c)) {
                        val fqn = (c as groovy.lang.GroovyObject).getProperty("fullyQualifiedName") as String
                        violations.getOrPut(fqn) { mutableMapOf<String, MutableList<String>>().withDefault { mutableListOf() } }
                            .getOrPut("error") { mutableListOf() }
                            .add("Class has been removed")
                    }
                }

                fun modifiedMethod(c: Any, m: Any) {
                    if (!skipMethod(c, m)) {
                        val name = (m as groovy.lang.GroovyObject).getProperty("name") as String
                        val oldMethod = (m as groovy.lang.GroovyObject).getProperty("oldMethod")
                        val newMethod = (m as groovy.lang.GroovyObject).getProperty("newMethod")
                        val fqn = (c as groovy.lang.GroovyObject).getProperty("fullyQualifiedName") as String
                        violations.getOrPut(fqn) { mutableMapOf<String, MutableList<String>>().withDefault { mutableListOf() } }
                            .getOrPut("warning") { mutableListOf() }
                            .add("""<p>Method $name has been modified</p>
<p>From <pre>${prettyPrint(oldMethod)}</pre> to <pre>${prettyPrint(newMethod)}</pre></p>""")
                    }
                }

                fun modifiedConstructor(c: Any, m: Any) {
                    if (!skipMethod(c, m)) {
                        val name = (m as groovy.lang.GroovyObject).getProperty("name") as String
                        val oldCtor = (m as groovy.lang.GroovyObject).getProperty("oldConstructor")
                        val newCtor = (m as groovy.lang.GroovyObject).getProperty("newConstructor")
                        val fqn = (c as groovy.lang.GroovyObject).getProperty("fullyQualifiedName") as String
                        violations.getOrPut(fqn) { mutableMapOf<String, MutableList<String>>().withDefault { mutableListOf() } }
                            .getOrPut("warning") { mutableListOf() }
                            .add("""<p>Constructor $name has been modified</p>
<p>From <pre>${prettyPrint(oldCtor)}</pre> to <pre>${prettyPrint(newCtor)}</pre></p>""")
                    }
                }

                fun modifiedClass(c: Any) {
                    if (!skipClass(c)) {
                        val binaryCompatible = (c as groovy.lang.GroovyObject).getProperty("binaryCompatible") as Boolean
                        val level = if (binaryCompatible) "info" else "error"
                        val fqn = (c as groovy.lang.GroovyObject).getProperty("fullyQualifiedName") as String
                        val message = "Class $fqn has been modified"
                        violations.getOrPut(fqn) { mutableMapOf<String, MutableList<String>>().withDefault { mutableListOf() } }
                            .getOrPut(level) { mutableListOf() }
                            .add(message)
                    }
                }

                fun newClass(c: Any) {
                    if (!skipClass(c)) {
                        val fqn = (c as groovy.lang.GroovyObject).getProperty("fullyQualifiedName") as String
                        violations.getOrPut(fqn) { mutableMapOf<String, MutableList<String>>().withDefault { mutableListOf() } }
                            .getOrPut("info") { mutableListOf() }
                            .add("Class has been added")
                    }
                }

                fun newMethod(c: Any, m: Any) {
                    if (!skipMethod(c, m)) {
                        val name = (m as groovy.lang.GroovyObject).getProperty("name") as String
                        val newMethod = (m as groovy.lang.GroovyObject).getProperty("newMethod")
                        val fqn = (c as groovy.lang.GroovyObject).getProperty("fullyQualifiedName") as String
                        violations.getOrPut(fqn) { mutableMapOf<String, MutableList<String>>().withDefault { mutableListOf() } }
                            .getOrPut("info") { mutableListOf() }
                            .add("""<p>Method $name has been added</p>
<p>Signature: <pre>${prettyPrint(newMethod)}</pre></p>""")
                    }
                }

                fun after() {
                    model["violations"] = violations
                }
            })
        }
    }

    // using a global engine for all tasks in order to increase performance
    val configDir = file("${rootProject.projectDir}/config/binarycompatibility")
    val templateFile = "binarycompat-report.groovy"
    val templateConfiguration = TemplateConfiguration().apply {
        isAutoIndent = true
        isAutoNewLine = true
    }
    val engine = MarkupTemplateEngine(this::class.java.classLoader, configDir, templateConfiguration)

    val japicmpAll by tasks.registering(me.champeau.gradle.ArtifactJapicmpTask::class) {
        dependsOn(tasks.named("jarAll"))
        baseline = "org.codehaus.groovy:groovy-all:$referenceMinorVersion@jar"
        to = tasks.named<Jar>("jarAll").get().archiveFile.get().asFile
        accessModifier = "protected"
        onlyModified = true
        failOnModification = false
        txtOutputFile = file("${layout.buildDirectory.get()}/reports/japi.txt")

        doFirst {
            classpath = allprojects.flatMap { it.configurations["japicmp"].files }.toSet()
        }

        val htmlReportFile = file("${layout.buildDirectory.get()}/reports/binary-compat-${project.name}-all.html")
        inputs.file(file("$configDir/$templateFile"))
        inputs.file(templateFile)
        outputs.file(htmlReportFile)

        val model = mutableMapOf<String, Any?>(
            "title" to "Binary compatibility report for ${project.name}",
            "project" to project,
            "baseline" to baseline,
            "archive" to to.name
        )
        outputProcessor(reportGenerator(model))

        doLast {
            htmlReportFile.writer().use { wrt ->
                engine.createTemplateByPath(templateFile).make(model).writeTo(wrt)
            }
        }
    }

    allprojects {

        dependencies {
            add("japicmp", files(rootProject.tasks.named<Jar>("jar").get().archiveFile))
        }

        val japicmp by tasks.registering(me.champeau.gradle.ArtifactJapicmpTask::class) {
            dependsOn(tasks.named("replaceJarWithJarJar"))
            baseline = "org.codehaus.groovy:${project.name}:$referenceMinorVersion@jar"
            to = tasks.named<Jar>("jar").get().archiveFile.get().asFile
            accessModifier = "protected"
            onlyModified = true
            failOnModification = false
            txtOutputFile = file("${layout.buildDirectory.get()}/reports/japi.txt")

            val htmlReportFile = file("${layout.buildDirectory.get()}/reports/binary-compat-${project.name}.html")
            inputs.file(file("$configDir/$templateFile"))
            inputs.file(templateFile)
            outputs.file(htmlReportFile)

            val model = mutableMapOf<String, Any?>(
                "title" to "Binary compatibility report for ${project.name}",
                "project" to project,
                "baseline" to baseline,
                "archive" to to.name
            )
            outputProcessor(reportGenerator(model))

            doLast {
                htmlReportFile.writer().use { wrt ->
                    engine.createTemplateByPath(templateFile).make(model).writeTo(wrt)
                }
            }
        }
    }

    allprojects {
        tasks.withType(me.champeau.gradle.ArtifactJapicmpTask::class.java).configureEach {
            rootProject.tasks.named("checkBinaryCompatibility").get().dependsOn(this)
        }
    }
}
