dependencies {
    add("compile", "bsf:bsf:2.4.0") {
        exclude(group = "commons-logging", module = "commons-logging")
    }
    add("compile", "commons-logging:commons-logging:1.2")
    add("compile", rootProject)
    add("testCompile", project(":groovy-test"))
}
