val scriptingCapable: () -> Boolean = {
    try {
        Class.forName("javax.script.ScriptEngine")
        true
    } catch (e: ClassNotFoundException) {
        false
    }
}

dependencies {
    if (!scriptingCapable()) {
        add("compileOnly", "org.livetribe:livetribe-jsr223:2.0.6") 
        // Note: 'provided' scope needs to be implemented if needed
    }
    add("compile", rootProject)
    add("testCompile", project(":groovy-test"))
}

tasks.register("moduleDescriptor", org.codehaus.groovy.gradle.WriteExtensionDescriptorTask::class) {
    extensionClasses = "org.codehaus.groovy.jsr223.ScriptExtensions"
    staticExtensionClasses = "org.codehaus.groovy.jsr223.ScriptStaticExtensions"
}

tasks.named("compileJava") {
    dependsOn("moduleDescriptor")
}
