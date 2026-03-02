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
apply(plugin = "org.asciidoctor.gradle.asciidoctor")

fun calculateDocUrl(baseUrl: String, className: String, anchor: String?): String {
    if (className == "index") return baseUrl
    return baseUrl + "?" + className.replace('.', '/') + ".html" + (if (anchor != null) "#$anchor" else "")
}

configure<org.asciidoctor.gradle.AsciidoctorExtension> {
    val groovyVersion = project.extra["groovyVersion"] as String
    val versionParts = Regex("""(\d+)\.(\d+)\.(\d+)(?:-(.+))?""").find(groovyVersion)?.groupValues
    val major = versionParts?.get(1) ?: "2"
    val minor = versionParts?.get(2) ?: "4"
    val patch = versionParts?.get(3) ?: "0"
    val flavor = versionParts?.getOrNull(4)
    
    logDocuments = true
    sourceDir = project.file("src/spec/doc")

    attributes(mapOf(
        "rootProjectDir" to rootProject.projectDir,
        "source-highlighter" to "prettify",
        "groovyversion" to groovyVersion,
        "groovy-major-version" to major,
        "groovy-minor-version" to minor,
        "groovy-patch-version" to patch,
        "groovy-full-version" to groovyVersion,
        "groovy-short-version" to "$major.$minor",
        "doctype" to "book",
        "revnumber" to groovyVersion,
        "icons" to "font",
        "toc2" to "",
        "specfolder" to "src/spec/doc",
        "linkcss" to "",
        "stylesheet" to "assets/css/style.css",
        "encoding" to "utf-8",
        "toclevels" to 10,
        "numbered" to "",
        "sectanchors" to ""
    ))

    extensions(delegateClosureOf<org.asciidoctor.gradle.AsciidoctorExtension> {
        val baseUrls = mapOf(
            "jdk" to "http://docs.oracle.com/javase/8/docs/api/index.html",
            "gjdk" to "http://docs.groovy-lang.org/${project.version}/html/groovy-jdk/index.html",
            "gapi" to "http://docs.groovy-lang.org/${project.version}/html/gapi/index.html"
        )

        baseUrls.forEach { (macroName, baseURL) ->
            // Note: This is a simplified version as the exact DSL for inline macros
            // may require additional Groovy interop
            project.logger.info("Would register inline macro: $macroName with baseURL: $baseURL")
        }
    })
}

// skip the asciidoctor task if there's no directory with asciidoc files
tasks.named("asciidoctor") {
    onlyIf { project.file("src/spec/doc").exists() }
}

val asciidoctorAssets by tasks.registering(Copy::class) {
    from(project.fileTree("src/spec/assets"))
    from(project.fileTree("src/spec/doc/assets"))
    into("${tasks.named("asciidoctor").get().property("outputDir")}/html5/assets")
    into("${rootProject.tasks.named("asciidoctor").get().property("outputDir")}/html5/assets")
}

tasks.named("asciidoctor") {
    finalizedBy(asciidoctorAssets)
}

fun adocSanityCheck(file: File, text: String, errors: MutableSet<String>) {
    val localErrors = mutableSetOf<String>()
    text.lines().forEachIndexed { i, line ->
        if (line.contains(Regex("tag:[a-zA-Z0-9]"))) {
            localErrors.add("line ${i + 1} misses semicolon. Should be tag::\n $line")
        }
        if (line.contains(Regex("end:[a-zA-Z0-9]"))) {
            localErrors.add("line ${i + 1} misses semicolon. Should be end::\n $line")
        }
        if (line.contains(Regex("(tag|end)::[^\\[\\]]$"))) {
            localErrors.add("line ${i + 1} contains incorrect tag definition (misses []):\n $line")
        }
    }
    errors.addAll(localErrors.map { "    $file, $it" })
}

fun htmlOutputSanityCheck(file: File, text: String, errors: MutableSet<String>) {
    val localErrors = mutableSetOf<String>()
    text.lines().forEachIndexed { i, line ->
        if (line.matches(Regex("^={1,5} .*"))) {
            localErrors.add("line ${i + 1} starting with asciidoctor raw markup:\n$line")
        }
        if (line.contains(Regex("""<code class=".+?"></code>"""))) {
            localErrors.add("contains empty code block, probably incorrect import of a tag.")
        }
        if (line.contains(Regex("(gapi|jdk|gjdk):(.+?)"))) {
            localErrors.add("line ${i + 1} starting with asciidoctor raw markup:\n$line")
        }
    }
    errors.addAll(localErrors.map { "    $file, $it" })
}

tasks.named("asciidoctor") {
    val errors = LinkedHashSet<String>()
    
    doFirst {
        val specTestDir = file("src/spec/test")
        if (specTestDir.exists()) {
            specTestDir.walkTopDown().forEach { file ->
                if (file.isFile) {
                    adocSanityCheck(file, file.readText(Charsets.UTF_8), errors)
                }
            }
        }
        if (errors.isNotEmpty()) {
            throw GradleException("Incorrect Asciidoctor input:\n${errors.joinToString("\n")}")
        }
    }

    doLast {
        val scripts = """<link rel="stylesheet" href="assets/css/view-example.css">
<script src='assets/js/jquery-min-2.1.1.js'></script>
<script src='assets/js/view-example.js'></script>"""

        // gapi macro expansion
        val outputDir = property("outputDir") as File
        outputDir.listFiles { file -> file.name.endsWith(".html") }?.forEach { file ->
            var text = file.readText(Charsets.UTF_8)
            text = text.replace("</head>", "$scripts</head>")
            htmlOutputSanityCheck(file, text, errors)
            file.writeText(text, Charsets.UTF_8)
        }
        if (errors.isNotEmpty()) {
            throw GradleException("Incorrect Asciidoctor output:\n${errors.joinToString("\n")}")
        }
    }
}
