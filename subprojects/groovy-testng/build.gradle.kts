dependencies {
    add("compile", rootProject)
    add("runtime", "org.testng:testng:6.8.13") {
        // exclude "optional" beanshell even though testng's pom doesn't say optional
        exclude(group = "org.beanshell", module = "bsh")
        // and an older version of jcommander
        exclude(group = "com.beust", module = "jcommander")
    }
    add("compile", "com.beust:jcommander:1.47")
}
