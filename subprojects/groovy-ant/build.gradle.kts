dependencies {
    add("compile", rootProject)
    add("compile", "org.apache.ant:ant:${rootProject.extra["antVersion"]}")
    add("runtime", "org.apache.ant:ant-junit:${rootProject.extra["antVersion"]}") {
        exclude(group = "junit", module = "junit")
    }
    add("runtime", "org.apache.ant:ant-launcher:${rootProject.extra["antVersion"]}")
    add("runtime", "org.apache.ant:ant-antlr:${rootProject.extra["antVersion"]}")
    // for groovydoc ant command
    add("compile", project(":groovy-groovydoc"))
    add("testCompile", project(":groovy-test"))
}

apply(from = file("${rootProject.projectDir}/gradle/jacoco/jacocofix.gradle"))
