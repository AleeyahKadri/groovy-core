dependencies {
    compile(rootProject)
    testCompile(rootProject.the<SourceSetContainer>()["test"].runtimeClasspath)
    testCompile(project(":groovy-test"))
}

tasks.register("moduleDescriptor", org.codehaus.groovy.gradle.WriteExtensionDescriptorTask::class) {
    extensionClasses = "org.codehaus.groovy.runtime.SwingGroovyMethods"
}

tasks.named("compileJava") {
    dependsOn("moduleDescriptor")
}

apply(from = "${rootProject.projectDir}/gradle/jacoco/jacocofix.gradle")
