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

/*
 * This file describes artifacts which will be distributed in separate jars, intended to be used with
 * <b>older</b> versions of Groovy for binary compatibility with classes compiled with newer versions of groovy
 */

val backports = mapOf(
    "compat23" to listOf("org/codehaus/groovy/runtime/typehandling/ShortTypeHandling.class")
)

val backportJarsTask = tasks.register("backportJars") {
    group = "Backports"
    description = "Generates backports jars"
}

tasks.named("dist").configure {
    dependsOn(backportJarsTask)
}
tasks.named("install").configure {
    dependsOn(backportJarsTask)
}
tasks.named("uploadArchives").configure {
    dependsOn(backportJarsTask)
}

backports.forEach { (pkg, classList) ->
    val backportJar = tasks.register<Jar>("backport${pkg}Jar") {
        group = "Backports"
        dependsOn("jarAll")
        
        from(zipTree(tasks.named<Jar>("jar").get().archiveFile))
        include(classList)
        archiveBaseName.set("groovy-backports-$pkg")
    }
    
    // the following two jars are empty. No wonder, Maven Central *requires* a javadoc and sources classifier
    // it's stupid in our case, because we don't have such, but we have no choice
    val javadocJar = tasks.register<Jar>("backport${pkg}JavadocJar") {
        group = "Backports"
        dependsOn("jarAll")
        
        archiveBaseName.set("groovy-backports-$pkg")
        archiveClassifier.set("javadoc")
    }
    
    val sourcesJar = tasks.register<Jar>("backport${pkg}SourcesJar") {
        group = "Backports"
        dependsOn("jarAll")
        
        archiveBaseName.set("groovy-backports-$pkg")
        archiveClassifier.set("sources")
    }
    
    backportJarsTask.configure {
        dependsOn(backportJar, javadocJar, sourcesJar)
    }
}
