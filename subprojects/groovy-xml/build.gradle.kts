dependencies {
    add("compile", rootProject)
    // Commented out - circular dependency: add("testCompile", (rootProject as org.gradle.api.plugins.ExtensionAware).extensions.getByType<org.gradle.api.tasks.SourceSetContainer>()["test"].runtimeClasspath)
    add("testCompile", "xmlunit:xmlunit:${rootProject.extra["xmlunitVersion"]}")
    add("testCompile", project(":groovy-test"))
}

tasks.register("moduleDescriptor", org.codehaus.groovy.gradle.WriteExtensionDescriptorTask::class) {
    extensionClasses = "org.codehaus.groovy.runtime.XmlGroovyMethods"
}

tasks.named("compileJava") {
    dependsOn("moduleDescriptor")
}
