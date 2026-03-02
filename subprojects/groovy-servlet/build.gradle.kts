val provided by configurations.creating

dependencies {
    provided("javax.servlet:servlet-api:2.4")
    provided("javax.servlet:jsp-api:2.0")
    compile(rootProject)
    testCompile("jmock:jmock:${project.extra["jmockVersion"]}")
    // needed for MarkupBuilder
    compile(project(":groovy-xml"))
    // needed by TemplateServlet
    compile(project(":groovy-templates"))
    testCompile(rootProject.the<SourceSetContainer>()["test"].runtimeClasspath)
    testCompile(project(":groovy-test"))
    // for compilation, dependency is not necessary because the classes are loaded using Class.forName
    testCompile(project(":groovy-json"))
}

configurations {
    compileOnly {
        extendsFrom(provided)
    }
}
