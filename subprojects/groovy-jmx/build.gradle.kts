dependencies {
    compile(rootProject)
    testCompile(project(":groovy-test"))
}

tasks.named<GroovyCompile>("compileTestGroovy") {
    classpath += rootProject.the<SourceSetContainer>()["test"].output
}
