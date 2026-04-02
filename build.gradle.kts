buildscript {
    repositories {
        jcenter()
        maven {
            name = "Bintray Asciidoctor repo"
            url = uri("http://dl.bintray.com/content/aalmiray/asciidoctor")
        }
    }

    dependencies {
        // using the old "classpath" style of plugins because the new one doesn't play well with multi-modules
        classpath("org.asciidoctor:asciidoctor-gradle-plugin:1.5.2")
        classpath("org.jfrog.buildinfo:build-info-extractor-gradle:3.0.3")
        //classpath("me.champeau.gradle:japicmp-gradle-plugin:0.1.1")
        //classpath("nl.javadude.gradle.plugins:license-gradle-plugin:0.11.0")
    }
}

apply(from = "gradle/root-build.gradle")
