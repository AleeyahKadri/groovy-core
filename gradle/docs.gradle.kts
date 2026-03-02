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

import org.gradle.api.JavaVersion
import org.gradle.api.tasks.javadoc.Javadoc

val doc by tasks.registering {
    dependsOn("javadocAll", "groovydocAll", "docGDK")
    if (JavaVersion.current().isJava7Compatible) {
        dependsOn("asciidocAll", "assembleAsciidoc")
    }
    val footer by extra { "Copyright &amp;copy; 2003-2014 The Codehaus. All rights reserved." }
    val title by extra { "Groovy ${rootProject.extra["groovyVersion"]}" }
}

if (JavaVersion.current().isJava7Compatible) {
    val assembleAsciidoc by tasks.registering(Copy::class) {
        dependsOn("asciidocAll")
        subprojects {
            from(project.tasks.named("asciidoctor"))
        }
        into("${layout.buildDirectory.get()}/asciidoc")
    }

    val asciidocAll by tasks.registering {
        dependsOn(allprojects.map { it.tasks.named("asciidoctor") })
    }
}

val javadocSpec: Javadoc.() -> Unit = {
    maxMemory = project.extra["javaDoc_mx"] as String
    options {
        this as StandardJavadocDocletOptions
        val docTask = rootProject.tasks.named("doc").get()
        val docTitle = docTask.extra["title"] as String
        val docFooter = docTask.extra["footer"] as String
        windowTitle = docTitle
        docTitle(docTitle)
        encoding = "UTF-8"
        isAuthor = true
        isVersion = true
        overview = rootProject.file("src/main/overviewj.html")
        footer = docFooter
        source = if (rootProject.extra["useIndy"] as (() -> Boolean))() { "1.7" } else { "1.6" }
        links(
            "http://docs.oracle.com/javase/8/docs/api/",
            "http://docs.oracle.com/javaee/7/api/",
            "http://commons.apache.org/proper/commons-cli/javadocs/api-release/",
            "http://junit.org/apidocs/",
            "http://docs.oracle.com/javaee/6/api/",
            "http://www.antlr2.org/javadoc/"
        )
    }
}

val groovydocSpec: GroovyDocTask.() -> Unit = {
    isUse = true
    if (project != rootProject) {
        source = project.the<SourceSetContainer>()["main"].allSource
    }
    classpath = tasks.named<Javadoc>("javadoc").get().classpath
    val docTask = rootProject.tasks.named("doc").get()
    val docTitle = docTask.extra["title"] as String
    val docFooter = docTask.extra["footer"] as String
    windowTitle = docTitle
    docTitle = docTitle
    header = docTitle
    footer = docFooter
    overview = rootProject.file("src/main/overview.html")
    isIncludePrivate = false
    link("http://docs.oracle.com/javaee/7/api/", "javax.servlet.", "javax.management.")
    link("http://docs.oracle.com/javase/8/docs/api/", "java.", "org.xml.", "javax.", "org.w3c.")
    link("http://docs.groovy-lang.org/docs/ant/api/", "org.apache.ant.", "org.apache.tools.ant.")
    link("http://junit.org/apidocs/", "org.junit.", "junit.")
    link("http://www.antlr2.org/javadoc/", "antlr.")
    link("http://commons.apache.org/proper/commons-cli/javadocs/api-release/", "org.apache.commons.cli.")
}

allprojects {
    tasks.named<Javadoc>("javadoc") {
        javadocSpec()
    }
    tasks.named<GroovyDocTask>("groovydoc") {
        groovydocSpec()
    }
}

// Root project has an extra 'all' javadoc task
val javadocAll by tasks.registering(Javadoc::class) {
    destinationDir = file("${layout.buildDirectory.get()}/alljavadoc")
    source = tasks.named<Javadoc>("javadoc").get().source
    classpath = tasks.named<Javadoc>("javadoc").get().classpath
    subprojects.forEach { sp ->
        source += sp.tasks.named<Javadoc>("javadoc").get().source
        classpath += sp.tasks.named<Javadoc>("javadoc").get().classpath
    }
    javadocSpec()
}

// Root project has an extra 'all' groovydoc task
val groovydocAll by tasks.registering(GroovyDocTask::class) {
    dependsOn(project(":groovy-groovydoc").tasks.named("classes"))
    dependsOn(project(":groovy-docgenerator").tasks.named("classes"))
    destinationDir = file("${layout.buildDirectory.get()}/allgroovydoc")
    source = tasks.named<GroovyDocTask>("groovydoc").get().source
    classpath = tasks.named<GroovyDocTask>("groovydoc").get().classpath
    groovyClasspath = tasks.named<GroovyDocTask>("groovydoc").get().groovyClasspath
    subprojects.forEach { sp ->
        source += sp.tasks.named<GroovyDocTask>("groovydoc").get().source
        classpath += sp.tasks.named<GroovyDocTask>("groovydoc").get().classpath
        groovyClasspath += sp.tasks.named<GroovyDocTask>("groovydoc").get().groovyClasspath
    }
    groovydocSpec()
}

// when docgenerator is run by the build, it requires a groovy-release-info file
// but the file is only generated by the 'jar' task, so as a workaround, we copy
// it into the docgenerator classes
val docProjectVersionInfo by tasks.registering(Copy::class) {
    destinationDir = file("${project(":groovy-docgenerator").layout.buildDirectory.get()}/classes/main")
    into("META-INF") {
        from("src/main/META-INF/groovy-release-info.properties") {
            filter(rootProject.extra["propertiesFilter"] as Map<*, *>, org.apache.tools.ant.filters.ReplaceTokens::class.java)
        }
    }
    from("subprojects/groovy-docgenerator/src/main/resources")
}

val docGDK by tasks.registering {
    dependsOn(
        project(":groovy-groovydoc").tasks.named("classes"),
        project(":groovy-docgenerator").tasks.named("classes")
    )
    // TODO don't hard-code these
    dependsOn(
        project(":groovy-sql").tasks.named("classes"),
        project(":groovy-xml").tasks.named("classes"),
        project(":groovy-swing").tasks.named("classes")
    )
    if (JavaVersion.current().isJava7Compatible) {
        dependsOn(project(":groovy-nio").tasks.named("classes"))
    }
    dependsOn(docProjectVersionInfo)
    val destinationDir by extra { "${layout.buildDirectory.get()}/html/groovy-jdk" }
    inputs.files(the<SourceSetContainer>()["tools"].runtimeClasspath)
    outputs.dir(destinationDir)
    doLast {
        try {
            ant.withGroovyBuilder {
                "java"(
                    "classname" to "org.codehaus.groovy.tools.DocGenerator",
                    "fork" to "true",
                    "failonerror" to "true",
                    "classpath" to (configurations["tools"] + tasks.named<GroovyDocTask>("groovydocAll").get().groovyClasspath).asPath,
                    "errorproperty" to "edr",
                    "outputproperty" to "odr"
                ) {
                    "arg"("value" to "-title")
                    "arg"("value" to "Groovy JDK enhancements")
                    "arg"("value" to "-link")
                    "arg"("value" to "groovy,org.codehaus.groovy=http://groovy.codehaus.org/gapi/")
                    "arg"("value" to "-link")
                    "arg"("value" to "java,org.xml,javax,org.w3c=http://docs.oracle.com/javase/7/docs/api/")
                    // either package name if in core or fully qualified path otherwise
                    "arg"("value" to "org.codehaus.groovy.runtime.DefaultGroovyMethods")
                    "arg"("value" to "org.codehaus.groovy.runtime.DefaultGroovyStaticMethods")
                    "arg"("value" to "org.codehaus.groovy.runtime.DateGroovyMethods")
                    "arg"("value" to "org.codehaus.groovy.runtime.EncodingGroovyMethods")
                    "arg"("value" to "org.codehaus.groovy.runtime.IOGroovyMethods")
                    "arg"("value" to "org.codehaus.groovy.runtime.ProcessGroovyMethods")
                    "arg"("value" to "org.codehaus.groovy.runtime.ResourceGroovyMethods")
                    "arg"("value" to "org.codehaus.groovy.runtime.SocketGroovyMethods")
                    "arg"("value" to "org.codehaus.groovy.runtime.StringGroovyMethods")
                    "arg"("value" to "org.codehaus.groovy.vmplugin.v5.PluginDefaultGroovyMethods")
                    "arg"("value" to "org.codehaus.groovy.vmplugin.v6.PluginDefaultGroovyMethods")
                    "arg"("value" to "org.codehaus.groovy.vmplugin.v6.PluginStaticGroovyMethods")
                    // TODO don't hard-code these
                    "arg"("value" to "subprojects/groovy-sql/src/main/java/org/codehaus/groovy/runtime/SqlGroovyMethods.java")
                    "arg"("value" to "subprojects/groovy-swing/src/main/java/org/codehaus/groovy/runtime/SwingGroovyMethods.java")
                    "arg"("value" to "subprojects/groovy-xml/src/main/java/org/codehaus/groovy/runtime/XmlGroovyMethods.java")
                    if (JavaVersion.current().isJava7Compatible) {
                        "arg"("value" to "subprojects/groovy-nio/src/main/java/org/codehaus/groovy/runtime/NioGroovyMethods.java")
                    }
                }
            }
        } finally {
            val odr = ant.properties["odr"]
            if (odr != null) {
                logger.info("Out: $odr")
            }
            val edr = ant.properties["edr"]
            if (edr != null) {
                logger.error("Err: $edr")
            }
        }
        copy {
            into(destinationDir)
            from("src/tools/org/codehaus/groovy/tools/groovy.ico", "src/tools/org/codehaus/groovy/tools/stylesheet.css")
        }
    }
}

// this will apply the javadoc fix tool to all generated javadocs
// we use it to make sure that the javadocs are not vulnerable independently of the JDK used to build
allprojects {
    tasks.withType<Javadoc>().configureEach {
        doLast {
            logger.lifecycle("Applying Javadoc fix tool (see http://www.kb.cert.org/vuls/id/225657) into $destinationDir")
            val javadocFix = JavadocFixTool()
            javadocFix.recursive = true
            javadocFix.doPatch = true
            javadocFix.searchAndPatch(destinationDir!!)
        }
    }
}

if (JavaVersion.current().isJava7Compatible) {
    tasks.named<Javadoc>("javadocAll") {
        options.source = "1.7"
    }
}

if (JavaVersion.current().isJava8Compatible) {
    allprojects {
        tasks.withType<Javadoc>().configureEach {
            // disable the crazy super-strict doclint tool in Java 8
            options {
                this as StandardJavadocDocletOptions
                addStringOption("Xdoclint:none", "-quiet")
            }
        }
    }
}
