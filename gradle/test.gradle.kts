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

import org.apache.tools.ant.taskdefs.condition.Os

allprojects {
    tasks.withType<Test> {
        if (JavaVersion.current().isJava8Compatible) {
            jvmArgs("-ea", "-Xms${project.extra["groovyJUnit_ms"]}", "-Xmx${project.extra["groovyJUnit_mx"]}")
        } else {
            jvmArgs(
                "-ea",
                "-Xms${project.extra["groovyJUnit_ms"]}",
                "-Xmx${project.extra["groovyJUnit_mx"]}",
                "-XX:PermSize=${project.extra["groovyJUnit_permSize"]}",
                "-XX:MaxPermSize=${project.extra["groovyJUnit_maxPermSize"]}"
            )
        }
        val headless = System.getProperty("java.awt.headless")
        if (headless == "true") {
            systemProperties["java.awt.headless"] = "true"
        }

        forkEvery = 50
        maxParallelForks = if (isRunningOnCI()) 1 else Runtime.getRuntime().availableProcessors()
        isScanForTestClasses = true
        ignoreFailures = false
        testLogging {
            exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
            // uncomment the following line if you need more logging
            // events("failed", "started")
        }
    }

    sourceSets {
        test {
            groovy {
                srcDir("src/spec/test")
            }
            resources {
                srcDir("src/spec/test-resources")
            }
        }
    }
}

tasks.named<Test>("test") {
    val testdb = System.getProperty("groovy.testdb.props")
    if (testdb != null) {
        systemProperties["groovy.testdb.props"] = testdb
    }
    systemProperties["apple.awt.UIElement"] = "true"
    systemProperties["javadocAssertion.src.dir"] = "./src/main"
    systemProperties["gradle.home"] = gradle.gradleHomeDir!!.path // this is needed by the security.policy

    classpath = files("src/test") + classpath
    exclude(buildExcludeFilter())
    extra["resultText"] = ""
    doLast {
        ant.withGroovyBuilder {
            "delete"(mapOf("includes" to "*.class", "dir" to "."))
        }
    }
}

fun isRunningOnCI(): Boolean {
    val path = File(".").absolutePath
    return path.contains("ci.codehaus.org") || path.contains("teamcity")
}

logger.lifecycle("Detected ${if (isRunningOnCI()) "Continuous Integration environment" else "development environment"}")

tasks.addRule("Pattern: testSingle<Name> will test **/<Name>.class") { taskName ->
    if (taskName.startsWith("testSingle")) {
        tasks.register(taskName) {
            dependsOn("test")
        }
        tasks.named<Test>("test") {
            include("**/${taskName.substring(10)}.class")
            outputs.upToDateWhen { false }
        }
    }
}

fun buildExcludeFilter(): (FileTreeElement) -> Boolean {
    val excludes = mutableListOf("GroovyTestCase", "TestSupport", "DummyTestDerivation", "LineColumnChecker")

    // temporary disabling Groovy4393Bug because it requires a specific configuration
    excludes.add("Groovy4393Bug")

    // deal with OS specific tests
    val windowsTests = listOf("ExecuteTest_Windows")
    val unixTests = listOf("ExecuteTest_LinuxSolaris")
    val osSpecificTests = (windowsTests + unixTests).toMutableSet()
    if (Os.isFamily(Os.FAMILY_WINDOWS)) {
        osSpecificTests.removeAll(windowsTests.toSet())
    } else if (Os.isFamily(Os.FAMILY_UNIX)) {
        osSpecificTests.removeAll(unixTests.toSet())
    }
    excludes.addAll(osSpecificTests)

    // temporarily disable security tests, see GRADLE-2170
    excludes.add("security")

    // if not compiled with indy support, disable indy tests
    if (!(rootProject.extra["useIndy"] as? () -> Boolean)?.invoke() ?: false) {
        excludes.addAll(listOf("indy", "Indy"))
    }

    // if no network available, disable Grapes
    if (System.getProperty("junit.network") == null) {
        excludes.add("groovy/grape/")
    }

    return { f ->
        excludes.any { f.file.path.contains(it) }
    }
}
