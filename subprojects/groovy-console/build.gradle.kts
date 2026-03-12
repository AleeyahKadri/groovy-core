import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.JavaExec

evaluationDependsOn(":groovy-swing")

dependencies {
    add("compile", rootProject)
    add("compile", project(":groovy-swing"))
    add("compile", project(":groovy-templates"))
    add("testCompile", project(":groovy-test"))
    // Note: Original had testCompile project(':groovy-swing').sourceSets.test.runtimeClasspath
    // This is simplified for Kotlin DSL to avoid cross-project sourceSet access issues
}

afterEvaluate {
    tasks.register<JavaExec>("console") {
        dependsOn("classes")
        mainClass.set("groovy.ui.Console")
        classpath(provider {
            the<SourceSetContainer>()["main"].runtimeClasspath
        })
    }
}
