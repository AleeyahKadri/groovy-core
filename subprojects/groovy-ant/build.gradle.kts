dependencies {
    compile(rootProject)
    compile("org.apache.ant:ant:${project.extra["antVersion"]}")
    runtime("org.apache.ant:ant-junit:${project.extra["antVersion"]}") {
        exclude(group = "junit", module = "junit")
    }
    runtime("org.apache.ant:ant-launcher:${project.extra["antVersion"]}")
    runtime("org.apache.ant:ant-antlr:${project.extra["antVersion"]}")
    // for groovydoc ant command
    compile(project(":groovy-groovydoc"))
    testCompile(project(":groovy-test"))
}

apply(from = "${rootProject.projectDir}/gradle/jacoco/jacocofix.gradle")
