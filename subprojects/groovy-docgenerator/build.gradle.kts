dependencies {
    add("compile", rootProject)
    add("compile", project(":groovy-templates"))
    add("testCompile", project(":groovy-test"))
    add("compile", "com.thoughtworks.qdox:qdox:${rootProject.extra["qdoxVersion"]}")
}
