val scriptingCapable = {
    try {
        Class.forName("javax.script.ScriptEngine")
        true
    } catch (e: ClassNotFoundException) {
        false
    }
}

val provided by configurations.creating

dependencies {
    if (!scriptingCapable()) {
        provided("org.livetribe:livetribe-jsr223:2.0.6")
    }
    compile(rootProject)
    testCompile(project(":groovy-test"))
}

configurations {
    compileOnly {
        extendsFrom(provided)
    }
}

tasks.register("moduleDescriptor", org.codehaus.groovy.gradle.WriteExtensionDescriptorTask::class) {
    extensionClasses = "org.codehaus.groovy.jsr223.ScriptExtensions"
    staticExtensionClasses = "org.codehaus.groovy.jsr223.ScriptStaticExtensions"
}

tasks.named("compileJava") {
    dependsOn("moduleDescriptor")
}
