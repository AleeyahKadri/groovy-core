dependencies {
    compile(rootProject)
//    testCompile("hsqldb:hsqldb:1.8.0.10")
    testCompile(group = "org.hsqldb", name = "hsqldb", version = "2.3.2", classifier = "jdk5")
//    testCompile("com.h2database:h2:1.3.164")
    testCompile(project(":groovy-test"))
}

// required for DataSet tests
sourceSets {
    test {
        runtimeClasspath = runtimeClasspath.plus(files("src/test/groovy"))
    }
}

// TODO move to parent build.gradle subprojects
tasks.named<Test>("test") {
    exclude("**/*TestCase.class", "**/*$*.class")
}

tasks.register("moduleDescriptor", org.codehaus.groovy.gradle.WriteExtensionDescriptorTask::class) {
    extensionClasses = "org.codehaus.groovy.runtime.SqlGroovyMethods"
}

tasks.named("compileJava") {
    dependsOn("moduleDescriptor")
}
