dependencies {
    add("compile", rootProject)
    add("compile", "junit:junit:4.12")
    add("testRuntime", project(":groovy-ant"))
}

apply(from = file("${rootProject.projectDir}/gradle/jacoco/jacocofix.gradle"))
