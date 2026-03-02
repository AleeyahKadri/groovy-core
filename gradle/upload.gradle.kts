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

val isUsingBintray = rootProject.hasProperty("bintrayUser") && rootProject.property("bintrayUser") != null &&
        rootProject.hasProperty("bintrayPassword") && rootProject.property("bintrayPassword") != null

if (isUsingBintray) {
    logger.lifecycle("Deployment environment set to Bintray")
}

val removeJarjaredDependencies: (groovy.util.Node) -> Unit = { p ->
    val dependencies = (p.get("dependencies") as List<*>)
    if (dependencies.isNotEmpty()) {
        val deps = (dependencies[0] as groovy.util.Node).children() as MutableList<groovy.util.Node>
        deps.removeAll { dep ->
            val groupId = (dep.get("groupId") as List<*>)
            val artifactId = (dep.get("artifactId") as List<*>)
            val gid = if (groupId.isNotEmpty()) (groupId[0] as groovy.util.Node).text() else null
            val aid = if (artifactId.isNotEmpty()) (artifactId[0] as groovy.util.Node).text() else null
            gid == "org.codehaus.groovy" ||
                    listOf("asm", "asm-util", "asm-analysis", "asm-tree", "asm-commons", "antlr", "commons-cli").contains(aid)
        }
    }
}

allprojects {
    apply(plugin = "maven")
    apply(from = "${rootProject.projectDir}/gradle/pomconfigurer.gradle.kts")
}

apply(from = "gradle/backports.gradle.kts")

allprojects {

    configurations {
        create("deployerJars")
    }

    if (!isUsingBintray) {
        dependencies {
            add("deployerJars", "org.apache.maven.wagon:wagon-webdav:1.0-beta-2")
        }
    }

    tasks.named<Upload>("uploadArchives") {
        repositories {
            withConvention(MavenRepositoryHandlerConvention::class) {
                mavenDeployer {
                    configuration = configurations["deployerJars"]
                    pom(project.extra["pomConfigureClosure"] as Closure<*>)

                    if (!isUsingBintray) {
                        val credentials = mapOf(
                            "userName" to System.getProperty("groovy.deploy.username"),
                            "password" to System.getProperty("groovy.deploy.password")
                        )
                        repository(
                            "id" to "codehaus.org",
                            "url" to uri("dav:https://dav.codehaus.org/repository/groovy"),
                            "authentication" to credentials
                        )
                        snapshotRepository(
                            "id" to "codehaus.org",
                            "url" to uri("dav:https://dav.codehaus.org/snapshots.repository/groovy"),
                            "authentication" to credentials
                        )
                    }
                }
            }
        }
    }

    tasks.named<Upload>("install") {
        repositories {
            withConvention(MavenRepositoryHandlerConvention::class) {
                mavenInstaller {
                    pom(project.extra["pomConfigureClosure"] as Closure<*>)
                }
            }
        }
    }

    artifacts {
        add("archives", tasks.named("jar"))
        add("archives", tasks.named("sourceJar"))
        add("archives", tasks.named("javadocJar"))
        add("archives", tasks.named("groovydocJar"))
    }

    listOf(tasks.named("uploadArchives"), tasks.named("install")).forEach { taskProvider ->
        taskProvider.configure {
            // dependency on jarAllAll should in theory be replaced with jar, jarWithIndy but
            // in practice, it is faster
            dependsOn(rootProject.tasks.named("jarAllAll"), tasks.named("sourceJar"), tasks.named("javadocJar"), tasks.named("groovydocJar"))
            doFirst {
                if ((rootProject.extra["useIndy"] as (() -> Boolean))()) {
                    throw GradleException(
                        "You cannot use uploadArchives or install task with the flag [indy] turned" +
                                " on because the build handles indy artifacts by itself in that case."
                    )
                }
                val jarTask = tasks.named<Jar>("jar").get()
                val archive = jarTask.archiveFile.get().asFile
                val indyJar = file("${archive.parent}/${archive.nameWithoutExtension}-indy.jar")
                if (indyJar.exists()) {
                    project.artifacts.add("archives", indyJar)
                }
                val grooidJar = file("${archive.parent}/${archive.nameWithoutExtension}-grooid.jar")
                if (grooidJar.exists()) {
                    project.artifacts.add("archives", grooidJar)
                }
            }
        }
    }
}

// the root project generates an alternate 'groovy-all' artifact
listOf(tasks.named("uploadArchives"), tasks.named("install")).forEach { taskProvider ->
    taskProvider.configure {
        dependsOn(tasks.named("sourceAllJar"), tasks.named("javadocAllJar"), tasks.named("groovydocAllJar"), tasks.named("distBin"))
        doFirst {
            project.artifacts.add("archives", tasks.named("jarAll").get())
            project.artifacts.add("archives", tasks.named("sourceAllJar").get())
            project.artifacts.add("archives", tasks.named("javadocAllJar").get())
            project.artifacts.add("archives", tasks.named("groovydocAllJar").get())
            project.artifacts.add("archives", tasks.named("distBin").get())
            tasks.withType<Jar>().matching { it.name.startsWith("backport") }.forEach { t ->
                project.artifacts.add("archives", t.archiveFile.get().asFile) {
                    name = t.archiveBaseName.get()
                    type = "jar"
                }
            }

            val jarAllTask = tasks.named<Jar>("jarAll").get()
            val archive = jarAllTask.archiveFile.get().asFile
            val indyJar = file("${archive.parent}/${archive.nameWithoutExtension}-indy.jar")
            if (indyJar.exists()) {
                project.artifacts.add("archives", indyJar)
            }
            val grooidJar = file("${archive.parent}/${archive.nameWithoutExtension}-grooid.jar")
            if (grooidJar.exists()) {
                project.artifacts.add("archives", grooidJar)
            }
        }
    }
}

val pomAll: MavenDeployer.() -> Unit = {
    addFilter("groovy") { artifact, file ->
        !artifact.name.contains("groovy-all") && !artifact.name.contains("groovy-binary") && !artifact.name.contains("backport")
    }
    addFilter("all") { artifact, file ->
        artifact.name.contains("groovy-all")
    }
    addFilter("binary") { artifact, file ->
        artifact.name.contains("groovy-binary")
    }
    val backports = project.extra["backports"] as Map<String, *>
    backports.forEach { (pkg, classes) ->
        addFilter("backports-$pkg") { artifact, file ->
            artifact.name == "groovy-backports-$pkg"
        }
    }

    // regular pom
    val groovypom = pom("groovy", project.extra["pomConfigureClosure"] as Closure<*>)

    // pom for 'all'
    val allpom = pom("all", project.extra["pomConfigureClosure"] as Closure<*>)
    allpom.artifactId = "groovy-all"

    // pom for binary zip
    val binarypom = pom("binary", project.extra["pomConfigureClosureWithoutTweaks"] as Closure<*>)
    binarypom.artifactId = "groovy-binary"

    // poms for backports
    backports.forEach { (pkg, classes) ->
        val id = "backports-$pkg"
        val backportPom = pom(id, project.extra["pomConfigureClosureWithoutTweaks"] as Closure<*>)
        backportPom.artifactId = "groovy-$id"
        backportPom.whenConfigured { p ->
            val deps = (p as groovy.util.Node).get("dependencies") as List<*>
            if (deps.isNotEmpty()) {
                (deps[0] as groovy.util.Node).children().clear()
            }
        }
    }

    val modules = (rootProject.extra["modules"] as () -> List<Project>)()
    modules.forEach { sp ->
        sp.tasks.named<Upload>("install").get().repositories.withConvention(MavenRepositoryHandlerConvention::class) {
            mavenInstaller {
                pom.whenConfigured { subpom ->
                    // add dependencies of other modules
                    val subDeps = ((subpom as groovy.util.Node).get("dependencies") as List<*>)
                    if (subDeps.isNotEmpty()) {
                        val allDeps = (allpom.project as groovy.util.Node).get("dependencies") as List<*>
                        if (allDeps.isNotEmpty()) {
                            (allDeps[0] as groovy.util.Node).children().addAll((subDeps[0] as groovy.util.Node).children())
                        }
                    }
                }
            }
        }
        sp.tasks.named<Upload>("uploadArchives").get().repositories.withConvention(MavenRepositoryHandlerConvention::class) {
            mavenDeployer {
                pom.whenConfigured { subpom ->
                    // add dependencies of other modules
                    val subDeps = ((subpom as groovy.util.Node).get("dependencies") as List<*>)
                    if (subDeps.isNotEmpty()) {
                        val allDeps = (allpom.project as groovy.util.Node).get("dependencies") as List<*>
                        if (allDeps.isNotEmpty()) {
                            (allDeps[0] as groovy.util.Node).children().addAll((subDeps[0] as groovy.util.Node).children())
                        }
                    }
                }
            }
        }
    }

    groovypom.whenConfigured(removeJarjaredDependencies)
    allpom.whenConfigured(removeJarjaredDependencies)

    binarypom.whenConfigured { p ->
        val deps = ((p as groovy.util.Node).get("dependencies") as List<*>)
        if (deps.isNotEmpty()) {
            (deps[0] as groovy.util.Node).children().clear()
        }
    }
}

tasks.named<Upload>("install") {
    // make sure dependencies poms are built *before* the all pom
    val modules = (rootProject.extra["modules"] as () -> List<Project>)()
    dependsOn(modules.map { it.tasks.named("install") })
    repositories {
        withConvention(MavenRepositoryHandlerConvention::class) {
            mavenInstaller(pomAll)
        }
    }
}

tasks.named<Upload>("uploadArchives") {
    // make sure dependencies poms are built *before* the all pom
    val modules = (rootProject.extra["modules"] as () -> List<Project>)()
    dependsOn(modules.map { it.tasks.named("uploadArchives") })
    repositories {
        withConvention(MavenRepositoryHandlerConvention::class) {
            mavenDeployer(pomAll)
        }
    }
}
