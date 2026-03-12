import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.api.tasks.compile.GroovyCompile
import org.gradle.api.tasks.bundling.Jar
import org.gradle.api.tasks.Copy
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.file.SourceDirectorySet
import org.gradle.api.tasks.SourceSet

// Helper to access Groovy source set
val SourceSet.groovy: SourceDirectorySet
    get() = extensions.getByName<SourceDirectorySet>("groovy")

buildscript {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }

    dependencies {
        // using the old "classpath" style of plugins because the new one doesn't play well with multi-modules
        // These plugins are not available in current repositories (Bintray/JCenter shut down)
        // Commenting out for now to get basic build working
        // classpath("org.asciidoctor:asciidoctor-gradle-plugin:1.5.2")
        // classpath("org.jfrog.buildinfo:build-info-extractor-gradle:3.0.3")
        //classpath("me.champeau.gradle:japicmp-gradle-plugin:0.1.1")
        //classpath("nl.javadude.gradle.plugins:license-gradle-plugin:0.11.0")
    }
}

apply(from = file("gradle/filter.gradle"))
apply(from = file("gradle/indy.gradle"))
// Commented out - requires plugins not available in current repositories
// apply(from = file("gradle/bintray.gradle"))

val javaHome = File(System.getProperty("java.home"))
logger.lifecycle("Using Java from $javaHome (version ${System.getProperty("java.version")})")

// Call indyBanner() from indy.gradle
val indyBanner = extra.properties["indyBanner"] as? groovy.lang.Closure<*>
indyBanner?.call()

// TODO use antlr plugin
//apply plugin: "antlr"

// Apply plugins to root project first
apply(plugin = "java")
apply(plugin = "groovy")

// Create legacy configurations for backward compatibility
val compile = configurations.create("compile") {
    extendsFrom(configurations["implementation"])
}
val runtime = configurations.create("runtime") {
    extendsFrom(configurations["runtimeOnly"])
}
val testCompile = configurations.create("testCompile") {
    extendsFrom(configurations["testImplementation"])
}
val testRuntime = configurations.create("testRuntime") {
    extendsFrom(configurations["testRuntimeOnly"])
}

allprojects {
    if (project != rootProject) {
        apply(plugin = "java")
        apply(plugin = "groovy")
        
        // Create legacy configurations for subprojects
        configurations.create("compile") {
            extendsFrom(configurations["implementation"])
        }
        configurations.create("runtime") {
            extendsFrom(configurations["runtimeOnly"])
        }
        configurations.create("testCompile") {
            extendsFrom(configurations["testImplementation"])
        }
        configurations.create("testRuntime") {
            extendsFrom(configurations["testRuntimeOnly"])
        }
    }

    layout.buildDirectory = file("target")
    configure<JavaPluginExtension> {
        sourceCompatibility = JavaVersion.VERSION_1_6
        targetCompatibility = JavaVersion.VERSION_1_6
    }

    group = "org.codehaus.groovy"
    version = project.property("groovyVersion") as String
    
    repositories {
        mavenCentral()
    }

    apply(from = file("${rootProject.projectDir}/gradle/indy.gradle"))
    // Commented out - requires plugins not available
    // if (JavaVersion.current().isJava7Compatible) {
    //     apply(from = file("${rootProject.projectDir}/gradle/asciidoctor.gradle"))
    // }
}

// todo: use the conventional "resources" directory for classpath resources
tasks.register<Copy>("copyResources") {
    destinationDir = file("${layout.buildDirectory.get().asFile}/classes")
    // text files requiring filtering
    into("main") {
        from("src/main")
        include("**/*.txt", "**/*.xml", "**/*.properties", "**/*.html")
        val propertiesFilter = rootProject.extra.properties["propertiesFilter"]
        filter(mapOf("tokens" to propertiesFilter), org.apache.tools.ant.filters.ReplaceTokens::class.java)
    }
    // other resources
    into("main") {
        from("src/main")
        include("**/*.png", "**/*.gif", "**/*.ico", "**/*.css")
    }
}

tasks.named("compileJava") {
    dependsOn("copyResources")
}

tasks.register<Copy>("copyTestResources") {
    from("src/test")
    into("${layout.buildDirectory.get().asFile}/classes/test")
    include("**/*.txt", "**/*.xml", "**/*.properties", "**/*.png", "**/*.html", "**/*.gif", "**/*.ico", "**/*.css")
}

tasks.named("compileTestJava") {
    dependsOn("copyTestResources")
}

tasks.register<Jar>("sourceJar") {
    archiveClassifier.set("sources")
    from("src/main")
}

// TODO: Add sourceJar task for subprojects
// subprojects {
//     pluginManager.withPlugin("java") {
//         tasks.create<Jar>("sourceJar") {
//             archiveClassifier.set("sources")
//             from(the<SourceSetContainer>()["main"].allSource)
//         }
//     }
// }

repositories {
    // todo Some repos are needed only for some configs. Declare them just for the configuration once Gradle allows this.
    maven { 
        url = uri("https://www.aQute.biz/repo")
        isAllowInsecureProtocol = true
    } // tools
    maven { url = uri("https://repository.jboss.org/nexus/content/groups/m2-release-proxy") } // examples, tools
}

// todo do we need compile and runtime scope for examples?
val compilerCompile = configurations.create("compilerCompile")
val tools = configurations.create("tools")
val examplesCompile = configurations.create("examplesCompile") {
    extendsFrom(configurations["compile"])
}
val examplesRuntime = configurations.create("examplesRuntime") {
    extendsFrom(examplesCompile)
}
val antlr = configurations.create("antlr")
val spec = configurations.create("spec")

val antVersion by extra("1.9.4")
val asmVersion by extra("5.0.3")
val antlrVersion by extra("2.7.7")
val bndVersion by extra("0.0.401")
val checkstyleVersion by extra("4.4")
val coberturaVersion by extra("1.9.4.1")
val commonsCliVersion by extra("1.2")
val commonsHttpClientVersion by extra("3.1")
val eclipseOsgiVersion by extra("3.9.1-v20140110-1610")
val gparsVersion by extra("1.2.1")
val ivyVersion by extra("2.4.0")
val jansiVersion by extra("1.11")
val jarjarVersion by extra("1.3")
val jlineVersion by extra("2.12")
val jmockVersion by extra("1.2.0")
val logbackVersion by extra("1.1.2")
val log4jVersion by extra("1.2.17")
val log4j2Version by extra("2.1")
val luceneVersion by extra("4.7.2")
val openejbVersion by extra("1.0")
val qdoxVersion by extra("1.12.1")
val slf4jVersion by extra("1.7.10")
val xmlunitVersion by extra("1.5")
val xstreamVersion by extra("1.4.7")
val spockVersion by extra("1.0-groovy-2.4")

dependencies {
    "compile"("antlr:antlr:$antlrVersion")
    "compile"("org.ow2.asm:asm:$asmVersion")
    "compile"("org.ow2.asm:asm-analysis:$asmVersion")
    "compile"("org.ow2.asm:asm-commons:$asmVersion")
    "compile"("org.ow2.asm:asm-tree:$asmVersion")
    "compile"("org.ow2.asm:asm-util:$asmVersion")

    "compile"("commons-cli:commons-cli:$commonsCliVersion")
    "compile"("org.apache.ant:ant:$antVersion")
    "compile"("com.thoughtworks.xstream:xstream:$xstreamVersion") {
        exclude(group = "xpp3", module = "xpp3_min")
        exclude(group = "junit", module = "junit")
        exclude(group = "jmock", module = "jmock")
    }
    "compile"(files("lib/openbeans-1.0.jar"))
    "compile"("org.fusesource.jansi:jansi:$jansiVersion")
    "compile"("org.apache.ivy:ivy:$ivyVersion") {
        isTransitive = false
    }
    "compile"(files("${layout.buildDirectory.get().asFile}/generated-classes"))

    "runtime"("org.codehaus.gpars:gpars:$gparsVersion") {
        exclude(group = "org.codehaus.groovy", module = "groovy-all")
    }
    "testCompile"("jmock:jmock:$jmockVersion")
    "testCompile"("jmock:jmock-cglib:$jmockVersion")
    "testCompile"("xmlunit:xmlunit:$xmlunitVersion")
    "testCompile"("ch.qos.logback:logback-classic:$logbackVersion")
    "testCompile"("log4j:log4j:$log4jVersion")
    "testCompile"("org.apache.logging.log4j:log4j-core:$log4j2Version")
    "testCompile"("org.slf4j:jcl-over-slf4j:$slf4jVersion")
    "testCompile"("com.thoughtworks.qdox:qdox:$qdoxVersion")

    tools("com.googlecode.jarjar:jarjar:$jarjarVersion")
    tools("checkstyle:checkstyle:$checkstyleVersion") {
        exclude(module = "junit")
    }

    tools("net.sourceforge.cobertura:cobertura:$coberturaVersion") {
        exclude(module = "asm")
        exclude(module = "ant")
    }
    tools("org.ow2.asm:asm-all:$asmVersion")
    tools("com.thoughtworks.qdox:qdox:$qdoxVersion")
    tools("biz.aQute:bnd:$bndVersion")

    examplesCompile(project(":groovy-test"))
    examplesCompile(project(":groovy-swing"))
    examplesCompile("org.apache.lucene:lucene-core:$luceneVersion")
    examplesCompile("org.apache.lucene:lucene-analyzers-common:$luceneVersion")
    examplesCompile("org.apache.lucene:lucene-queryparser:$luceneVersion")
    examplesCompile("org.eclipse:osgi:$eclipseOsgiVersion")
    examplesRuntime("commons-httpclient:commons-httpclient:$commonsHttpClientVersion") {
        exclude(module = "junit")
        exclude(module = "commons-logging")
        exclude(module = "commons-codec")
    }
    examplesRuntime("openejb:openejb-loader:$openejbVersion") {
        exclude(module = "log4j")
        exclude(module = "openejb-core")
        exclude(module = "geronimo-jta_1.0.1B_spec")
        exclude(module = "geronimo-servlet_2.4_spec")
        exclude(module = "geronimo-ejb_2.1_spec")
        exclude(module = "geronimo-j2ee-connector_1.5_spec")
    }

    // TODO use antlr plugin
    //antlr("antlr:antlr:$antlrVersion")
    antlr("org.apache.ant:ant-antlr:$antVersion")

    "testCompile"(project(":groovy-ant"))
    "testCompile"(project(":groovy-test"))
}

val generatedDirectory by extra("${layout.buildDirectory.get().asFile}/generated-sources")

the<SourceSetContainer>().apply {
    named("main") {
        java {
            srcDirs("src/main", "$generatedDirectory/src/main")
            val excludePatterns = mutableListOf<String>()
            fileTree("src/main/groovy/ui").matching {
                exclude("GroovyMain.java", "GroovySocketServer.java")
            }.visit {
                if (!isDirectory) {
                    excludePatterns.add("groovy/ui/$path")
                }
            }
            exclude(excludePatterns)
            if (!JavaVersion.current().isJava7Compatible) {
                exclude("**/indy/*")
                exclude("**/v7/*")
                exclude("**/vm7/*")
            }
        }
        groovy.apply {
            srcDirs("src/main", "$generatedDirectory/src/main")
            if (!JavaVersion.current().isJava7Compatible) {
                exclude("**/indy/*")
                exclude("**/v7/*")
                exclude("**/vm7/*")
            }
        }
        resources {
            srcDirs("src/main", "src/tools", "src/resources")
            include(
                "META-INF/services/*",
                "META-INF/groovy-release-info.properties",
                "groovy/grape/*.xml",
                "groovy/ui/*.properties",
                "groovy/ui/**/*.png",
                "groovy/inspect/swingui/AstBrowserProperties.groovy",
                "org/codehaus/groovy/tools/shell/**/*.properties",
                "org/codehaus/groovy/tools/shell/**/*.xml",
                "org/codehaus/groovy/tools/groovydoc/gstringTemplates/**/*.*",
                "org/codehaus/groovy/tools/groovy.ico"
            )
        }
    }
    named("test") {
        groovy.srcDirs("src/test")
        resources {
            srcDirs("src/test-resources")
        }
        java {
            destinationDirectory.set(file("${layout.buildDirectory.get().asFile}/test-classes"))
        }
    }
}

// Configure custom source sets after creation
val toolsSourceSet = the<SourceSetContainer>().create("tools")
val mainSourceSet = the<SourceSetContainer>()["main"]
toolsSourceSet.apply {
    groovy.srcDirs("src/tools")
    resources.srcDirs("src/tools")
    java.destinationDirectory.set(file("${layout.buildDirectory.get().asFile}/tools-classes"))
    compileClasspath = configurations["tools"] + mainSourceSet.runtimeClasspath
    runtimeClasspath = output + compileClasspath
}

val examplesSourceSet = the<SourceSetContainer>().create("examples")
examplesSourceSet.apply {
    groovy.srcDirs("src/examples")
    resources.srcDirs("src/examples")
    java.destinationDirectory.set(file("${layout.buildDirectory.get().asFile}/examples-classes"))
    compileClasspath = configurations["examplesRuntime"] + 
        mainSourceSet.output + 
        project(":groovy-xml").the<SourceSetContainer>()["main"].output
}

// make sure examples can be compiled, even if we don't run them
// todo: reorganize examples so that we can run them too
tasks.named("check") {
    dependsOn("examplesClasses")
}

// remove this from config once GRADLE-854 is fixed.
tasks.named("processResources") {
    doLast {
        copy {
            from("src/main") {
                include(
                    "groovy/inspect/swingui/AstBrowserProperties.groovy",
                    "org/codehaus/groovy/tools/groovydoc/gstringTemplates/GroovyDocTemplateInfo.java"
                )
            }
            into(the<SourceSetContainer>()["main"].output.classesDirs.singleFile)
        }
    }
}

tasks.register("ensureGrammars") {
    description = "Ensure all the Antlr generated files are up to date."
    val antlrDirectory = "$projectDir/src/main/org/codehaus/groovy/antlr"
    val groovyParserDirectory = "$antlrDirectory/parser"
    val javaParserDirectory = "$antlrDirectory/java"
    val genPath = "$generatedDirectory/src/main/org/codehaus/groovy/antlr"
    val groovyOutDir = "$genPath/parser"
    val javaOutDir = "$genPath/java"
    
    inputs.dir(antlrDirectory)
    outputs.dir(groovyOutDir)
    outputs.dir(javaOutDir)
    
    doFirst {
        file(groovyOutDir).mkdirs()
        file(javaOutDir).mkdirs()
        ant.withGroovyBuilder {
            "taskdef"(
                "name" to "antlr",
                "classname" to "org.apache.tools.ant.taskdefs.optional.ANTLR",
                "classpath" to configurations["antlr"].asPath
            )

            "mkdir"("dir" to groovyParserDirectory)
            "antlr"(
                "target" to "$antlrDirectory/groovy.g",
                "outputdirectory" to groovyOutDir
            ) {
                "classpath"("path" to configurations["compile"].asPath)
            }
            "antlr"(
                "target" to "$javaParserDirectory/java.g",
                "outputdirectory" to javaOutDir
            ) {
                "classpath"("path" to configurations["compile"].asPath)
            }
        }
    }
}

apply(from = file("gradle/utils.gradle"))

extra["modules"] = {
    subprojects
}

tasks.register("dgmConverter") {
    dependsOn("compileJava")
    description = "Generates DGM info file required for faster startup."
    
    doFirst {
        val classesDir = the<SourceSetContainer>()["main"].output.classesDirs.singleFile
        val classpath = files(classesDir, configurations["compile"]).asPath
        
        file("$classesDir/META-INF").mkdirs()
        // we use ant.java because Gradle is a bit "too smart" with JavaExec
        // as it will invalidate the task if classpath changes, which will
        // happen once Groovy files are compiled
        ant.withGroovyBuilder {
            "java"(
                "classname" to "org.codehaus.groovy.tools.DgmConverter",
                "classpath" to classpath
            ) {
                "arg"("value" to "--info")
                "arg"("value" to classesDir.absolutePath)
            }
        }
    }
    inputs.files(fileTree("src").include("**/*GroovyMethods.java"))
    // outputs.file configured in doFirst since we need classesDir
}

tasks.named<JavaCompile>("compileJava") {
    dependsOn("ensureGrammars", "exceptionUtils")
    options.forkOptions.memoryMaximumSize = project.property("javacMain_mx") as String
}

// Gradle classloading magic with Groovy will only work if it finds a *jar*
// on classpath. This "bootstrap jar" contains the minimal compiler, without .groovy compiled files

tasks.register("bootstrapJar") {
    dependsOn("compileJava", "dgmConverter")

    val destinationDir = file("${layout.buildDirectory.get().asFile}/bootstrap")
    val archiveName = "groovy-${version}-bootstrap.jar"
    val archivePath = file("$destinationDir/$archiveName")
    
    extra["archivePath"] = archivePath

    doLast {
        // we use ant.jar because Gradle is a bit "too smart" with JavaExec
        // as it will invalidate the task if classpath changes, which will
        // happen once Groovy files are compiled
        destinationDir.mkdirs()
        ant.withGroovyBuilder {
            "jar"(
                "destfile" to archivePath,
                "basedir" to the<SourceSetContainer>()["main"].output.classesDirs.singleFile
            )
        }
    }
    val useIndy = extra.properties["useIndy"] as? groovy.lang.Closure<*>
    inputs.property("indy", useIndy?.call() ?: false)
    inputs.files(fileTree("src"))
    outputs.file(archivePath)
}

tasks.named("compileGroovy") {
    dependsOn("bootstrapJar")
}

allprojects {

    tasks.withType<JavaCompile> {
        options.encoding = "UTF-8"
    }

    tasks.withType<GroovyCompile> {
        groovyOptions.forkOptions.memoryMaximumSize = project.property("groovycMain_mx") as String
        groovyOptions.encoding = "UTF-8"
        
        val bootstrapJar = rootProject.tasks.named("bootstrapJar")
        val bootstrapJarPath = bootstrapJar.get().extra["archivePath"] as File
        
        val compileJavaTask = rootProject.tasks.named<JavaCompile>("compileJava").get()
        groovyClasspath = files(
            compileJavaTask.classpath,
            bootstrapJarPath
        )

        classpath = classpath + groovyClasspath
    }

    val useIndy = extra.properties["useIndy"] as? groovy.lang.Closure<*>
    if (useIndy?.call() == true) {
        tasks.withType<GroovyCompile> {
            logger.info("Building ${project.name}:${name} with InvokeDynamic support activated")
            groovyOptions.optimizationOptions = mapOf("indy" to true)
            sourceCompatibility = "1.7"
            targetCompatibility = "1.7"
        }
        tasks.withType<JavaCompile> {
            sourceCompatibility = "1.7"
            targetCompatibility = "1.7"
        }
        tasks.withType<Jar> {
            archiveClassifier.set("indy")
        }
    }
}

tasks.named<GroovyCompile>("compileTestGroovy") {
    groovyOptions.forkOptions.memoryMaximumSize = project.property("groovycTest_mx") as String
}

apply(from = file("gradle/test.gradle"))
apply(from = file("gradle/groovydoc.gradle"))
// Commented out - requires asciidoctor plugin
// apply(from = file("gradle/docs.gradle"))
// Commented out - requires osgi plugin
// apply(from = file("gradle/assemble.gradle"))
// Commented out - requires additional setup
// apply(from = file("gradle/upload.gradle"))
apply(from = file("gradle/idea.gradle"))
apply(from = file("gradle/eclipse.gradle"))
// Commented out - requires findbugs plugin
// apply(from = file("gradle/quality.gradle"))
// If a local configuration file for tweaking the build is present, apply it
if (file("user.gradle").exists()) {
    apply(from = file("user.gradle"))
}

if (!JavaVersion.current().isJava7Compatible) {
    logger.lifecycle("""
    **************************************** WARNING **********************************************
    ****** You are running the build with an older JDK. NEVER try to release with 1.6.       ******
    ****** You must use a JDK 1.7+ in order to compile all features of the language.         ******
    ***********************************************************************************************
""")
}

// UNCOMMENT THE FOLLOWING TASKS IF YOU WANT TO RUN LICENSE CHECKING
//task licenseFormatCustom(type:nl.javadude.gradle.plugins.license.License) {
//    source = fileTree(dir:"src").include ("**/*.java","**/*.groovy","**/*.html","**/*.css","**/*.xml","**/*.properties","**/*.properties")
//}
//
//task licenseFormatGradle(type:nl.javadude.gradle.plugins.license.License) {
//    source = files(fileTree(dir:projectDir).include("**/*.gradle"),fileTree("buildSrc").include("**/*.groovy"))
//}
//
//licenseFormat.dependsOn licenseFormatCustom
//licenseFormat.dependsOn licenseFormatGradle
//
