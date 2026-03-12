dependencies {
    add("compile", "javax.servlet:servlet-api:2.4") 
    // Note: 'provided' scope needs to be implemented if needed
    add("compile", "javax.servlet:jsp-api:2.0") 
    // Note: 'provided' scope needs to be implemented if needed
    add("compile", rootProject)
    add("testCompile", "jmock:jmock:${rootProject.extra["jmockVersion"]}")
    // needed for MarkupBuilder
    add("compile", project(":groovy-xml"))
    // needed by TemplateServlet
    add("compile", project(":groovy-templates"))
    // Commented out - circular dependency: add("testCompile", (rootProject as org.gradle.api.plugins.ExtensionAware).extensions.getByType<org.gradle.api.tasks.SourceSetContainer>()["test"].runtimeClasspath)
    add("testCompile", project(":groovy-test"))
    // for compilation, dependency is not necessary because the classes are loaded using Class.forName
    add("testCompile", project(":groovy-json"))
}
