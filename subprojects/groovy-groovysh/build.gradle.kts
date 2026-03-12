dependencies {
    add("compile", rootProject)
    add("compile", project(":groovy-console"))
    add("testCompile", project(":groovy-test"))
    add("compile", "jline:jline:${rootProject.extra["jlineVersion"]}") {
        exclude(group = "junit", module = "junit")
    }
}
