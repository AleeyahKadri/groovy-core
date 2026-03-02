evaluationDependsOn(":groovy-swing")

dependencies {
    compile(rootProject)
    compile(project(":groovy-swing"))
    compile(project(":groovy-templates"))
    testCompile(project(":groovy-test"))
    testCompile(project(":groovy-swing").the<SourceSetContainer>()["test"].runtimeClasspath)
}

tasks.register<JavaExec>("console") {
    dependsOn("classes")
    mainClass.set("groovy.ui.Console")
    classpath = sourceSets["main"].runtimeClasspath
}
