dependencies {
    compile(rootProject)
    compile(project(":groovy-xml"))
    testCompile(rootProject.the<SourceSetContainer>()["test"].runtimeClasspath)
    testCompile(project(":groovy-test"))
    testCompile("org.spockframework:spock-core:${project.extra["spockVersion"]}") {
        exclude(module = "groovy-all")
    }
}

tasks.register<Jar>("backportJar") {
    archiveAppendix.set("markup-backport")
    dependsOn("classes")
    from(sourceSets["main"].output)
    include("groovy/text/markup/**")
}
