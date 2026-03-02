dependencies {
    compile(rootProject)
    testCompile(rootProject.the<SourceSetContainer>()["test"].runtimeClasspath)
    compile(project(":groovy-templates"))
    testCompile(project(":groovy-test"))
    testCompile(project(":groovy-ant"))
    testCompile("org.apache.ant:ant-testutil:${project.extra["antVersion"]}")
}

tasks.named<JavaCompile>("compileJava") {
    doLast {
        val classesDir = sourceSets["main"].output.classesDirs.singleFile
        file("$classesDir/META-INF").mkdirs()
    }
}
