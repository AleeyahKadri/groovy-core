import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.api.tasks.javadoc.Javadoc

dependencies {
    add("compile", rootProject)
    add("testCompile", project(":groovy-test"))
    add("testCompile", "org.spockframework:spock-core:${rootProject.extra["spockVersion"]}") {
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
        this as StandardJavadocDocletOptions
        source = "1.7"
    }
}
