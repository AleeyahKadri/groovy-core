import org.gradle.api.tasks.compile.JavaCompile

dependencies {
    add("compile", rootProject)
    // Commented out - circular dependency: add("testCompile", (rootProject as org.gradle.api.plugins.ExtensionAware).extensions.getByType<org.gradle.api.tasks.SourceSetContainer>()["test"].runtimeClasspath)
    add("compile", project(":groovy-templates"))
    add("testCompile", project(":groovy-test"))
    add("testCompile", project(":groovy-ant"))
    add("testCompile", "org.apache.ant:ant-testutil:${rootProject.extra["antVersion"]}")
}

tasks.named<JavaCompile>("compileJava") {
    doLast {
        val sourceSets = the<org.gradle.api.tasks.SourceSetContainer>()
        val classesDir = sourceSets["main"].output.classesDirs.singleFile
        file("$classesDir/META-INF").mkdirs()
    }
}
