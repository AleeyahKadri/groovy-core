val subprojects = mutableListOf(
    "groovy-ant",
    "groovy-bsf",
    "groovy-console",
    "groovy-docgenerator",
    "groovy-groovydoc",
    "groovy-groovysh",
    "groovy-jmx",
    "groovy-json",
    "groovy-jsr223",
    "groovy-servlet",
    "groovy-sql",
    "groovy-swing",
    "groovy-templates",
    "groovy-test",
    "groovy-testng",
    "groovy-xml"
)

if (JavaVersion.current().isJava7Compatible) {
    subprojects.add("groovy-nio")
}

include(*subprojects.toTypedArray())

rootProject.children.forEach { prj ->
    prj.projectDir = file("$rootDir/subprojects/${prj.name}")
}

rootProject.name = "groovy" // TODO should this be groovy-core?
