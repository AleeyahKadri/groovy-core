import org.gradle.api.tasks.testing.Test

dependencies {
    add("compile", rootProject)
//    add("testCompile", "hsqldb:hsqldb:1.8.0.10")
    add("testCompile", "org.hsqldb:hsqldb:2.3.2:jdk5")
//    add("testCompile", "com.h2database:h2:1.3.164")
    add("testCompile", project(":groovy-test"))
}

afterEvaluate {
    // required for DataSet tests
    extensions.getByType<org.gradle.api.tasks.SourceSetContainer>()["test"].runtimeClasspath += files("src/test/groovy")
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
