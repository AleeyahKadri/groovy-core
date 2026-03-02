dependencies {
    compile(rootProject)
    testCompile(project(":groovy-test"))
    testCompile("org.spockframework:spock-core:${project.extra["spockVersion"]}") {
        exclude(module = "groovy-all")
    }
}

tasks.register("moduleDescriptor", org.codehaus.groovy.gradle.WriteExtensionDescriptorTask::class) {
    extensionClasses = "org.codehaus.groovy.runtime.NioGroovyMethods"
}

tasks.named("compileJava") {
    dependsOn("moduleDescriptor")
}

tasks.withType<JavaCompile> {
    sourceCompatibility = "1.7"
    targetCompatibility = "1.7"
}

tasks.withType<Javadoc> {
    options {
        (this as StandardJavadocDocletOptions).source = "1.7"
    }
}
