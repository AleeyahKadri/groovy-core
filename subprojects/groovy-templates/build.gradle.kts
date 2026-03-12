import org.gradle.api.tasks.bundling.Jar

dependencies {
    add("compile", rootProject)
    add("compile", project(":groovy-xml"))
    // Commented out - circular dependency: add("testCompile", (rootProject as org.gradle.api.plugins.ExtensionAware).extensions.getByType<org.gradle.api.tasks.SourceSetContainer>()["test"].runtimeClasspath)
    add("testCompile", project(":groovy-test"))
    add("testCompile", "org.spockframework:spock-core:${rootProject.extra["spockVersion"]}") {
        exclude(module = "groovy-all")
    }
}

pluginManager.withPlugin("java") {
    afterEvaluate {
        tasks.register<Jar>("backportJar") {
            archiveAppendix.set("markup-backport")
            dependsOn("classes")
            from(provider {
                the<org.gradle.api.tasks.SourceSetContainer>()["main"].output
            })
            include("groovy/text/markup/**")
        }
    }
}
