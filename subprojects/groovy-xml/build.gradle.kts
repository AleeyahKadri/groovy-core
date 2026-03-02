dependencies {
    compile(rootProject)
    testCompile(rootProject.the<SourceSetContainer>()["test"].runtimeClasspath)
    testCompile("xmlunit:xmlunit:${project.extra["xmlunitVersion"]}")
    testCompile(project(":groovy-test"))
}

tasks.register("moduleDescriptor", org.codehaus.groovy.gradle.WriteExtensionDescriptorTask::class) {
    extensionClasses = "org.codehaus.groovy.runtime.XmlGroovyMethods"
}

tasks.named("compileJava") {
    dependsOn("moduleDescriptor")
}
