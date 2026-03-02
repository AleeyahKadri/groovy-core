/**
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

import groovy.util.XmlParser
import groovy.util.Node
import org.gradle.plugins.ide.idea.model.IdeaModel
import org.gradle.plugins.ide.idea.GenerateIdeaProject

fun appendNode(node: Node, text: String) {
    node.append(XmlParser().parseText(text))
}

allprojects {
    apply(plugin = "idea")

    configure<IdeaModel> {
        module.iml.withXml {
            val node = asNode()

            // remove target classes from libraries
            val moduleRoot = (node.get("component") as List<*>).find {
                (it as Node).attribute("name") == "NewModuleRootManager"
            } as Node
            val entries = (moduleRoot.get("orderEntry") as List<*>).filter { oe ->
                val oeNode = oe as Node
                val library = oeNode.get("library")
                if (library is List<*> && library.isNotEmpty()) {
                    val classes = (library[0] as Node).get("CLASSES")
                    if (classes is List<*> && classes.isNotEmpty()) {
                        val root = (classes[0] as Node).get("root")
                        if (root is List<*>) {
                            return@filter root.any { r ->
                                val url = (r as Node).attribute("url") as String?
                                url?.contains("target") == true
                            }
                        }
                    }
                }
                false
            }
            if (entries.isNotEmpty()) {
                entries.forEach { moduleRoot.remove(it) }
            }
        }
    }
}

configure<IdeaModel> {
    project {
        ipr {
            withXml {
                val node = asNode()

                // jdk, language level fix
                val pRoot = (node.get("component") as List<*>).find {
                    (it as Node).attribute("name") == "ProjectRootManager"
                } as Node
                pRoot.attributes()["languageLevel"] = "JDK_1_6"
                pRoot.attributes()["project-jdk-name"] = "1.8"

                // Use git
                val vcsConfig = (node.get("component") as List<*>).find {
                    (it as Node).attribute("name") == "VcsDirectoryMappings"
                } as Node
                val mapping = (vcsConfig.get("mapping") as List<*>)[0] as Node
                mapping.attributes()["vcs"] = "Git"

                // license header
                val copyrightManager = (node.get("component") as List<*>).find {
                    (it as Node).attribute("name") == "CopyrightManager"
                } as Node
                copyrightManager.attributes()["default"] = "ASL2"
                val copyrightList = copyrightManager.get("copyright") as List<*>
                val aslCopyright = copyrightList.find {
                    val options = (it as Node).get("option") as List<*>
                    options.any { opt ->
                        val optNode = opt as Node
                        optNode.attribute("name") == "myName" && optNode.attribute("value") == "ASL2"
                    }
                }
                if (aslCopyright == null) {
                    appendNode(copyrightManager, """
                      <copyright>
                          <option name="notice" value="Copyright 2003-${'$'}today.year the original author or authors.&#10;&#10;Licensed under the Apache License, Version 2.0 (the &quot;License&quot;);&#10;you may not use this file except in compliance with the License.&#10;You may obtain a copy of the License at&#10;&#10;     http://www.apache.org/licenses/LICENSE-2.0&#10;&#10;Unless required by applicable law or agreed to in writing, software&#10;distributed under the License is distributed on an &quot;AS IS&quot; BASIS,&#10;WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.&#10;See the License for the specific language governing permissions and&#10;limitations under the License." />
                          <option name="keyword" value="Copyright" />
                          <option name="allowReplaceKeyword" value="" />
                          <option name="myName" value="ASL2" />
                          <option name="myLocal" value="true" />
                      </copyright>
                """)
                }

                val compilerConfig = (node.get("component") as List<*>).find {
                    (it as Node).attributes()["name"] == "CompilerConfiguration"
                } as Node
                listOf(
                    "/**/org.codehaus.groovy.runtime.ExtensionModule",
                    "*.jar",
                    "test-resources:/**/*.groovy", "test-resources:/**/*.java",
                    "!?*.groovy",
                    "/groovy/inspect/swingui/AstBrowserProperties.groovy",
                    "/groovy/inspect/swingui/AstBrowserProperties.groovy",
                    "/**/org.codehaus.groovy.runtime.ExtensionModule;*.jar",
                    "/groovy/inspect/swingui/AstBrowserProperties.groovy",
                    "/**/org.codehaus.groovy.runtime.ExtensionModule",
                    "*.jar"
                ).forEach {
                    appendNode((compilerConfig.get("wildcardResourcePatterns") as List<*>)[0] as Node, "<entry name=\"$it\" />")
                }
                appendNode(compilerConfig, """<excludeFromCompile>
      <directory url="file://${'$'}PROJECT_DIR$/src/test-resources" includeSubdirectories="true" />
    </excludeFromCompile>""")
            }
        }
    }

    workspace {
        iws {
            withXml {
                val node = asNode()

                // exclude some files from stub generation
                var groovyCompilerConfig = (node.get("component") as List<*>).find {
                    (it as Node).attribute("name") == "GroovyCompilerConfiguration"
                } as Node?
                if (groovyCompilerConfig == null) {
                    node.append(XmlParser().parseText("""
                   <component name='GroovyCompilerConfiguration'>
                      <excludes></excludes>
                   </component>"""))
                    groovyCompilerConfig = (node.get("component") as List<*>).find {
                        (it as Node).attribute("name") == "GroovyCompilerConfiguration"
                    } as Node
                }
                val excludeNode = (groovyCompilerConfig.get("excludes") as List<*>)[0] as Node
                listOf(
                    "/src/test/org/codehaus/groovy/transform/DelegateTransformTest.groovy",
                    "/src/test/groovy/bugs/Groovy593_Bug.groovy"
                ).forEach { excludedFile ->
                    val url = "file://\$PROJECT_DIR\$$excludedFile"
                    val fileNodes = excludeNode.get("file") as List<*>
                    if (!fileNodes.any { (it as Node).attribute("url") == url }) {
                        excludeNode.append(XmlParser().parseText("<file url=\"$url\" />"))
                    }
                }

                // add sample configurations
                val runmanager = (node.get("component") as List<*>).find {
                    (it as Node).attribute("name") == "RunManager"
                } as Node
                appendNode(runmanager, """<configuration default="false" name="Console" type="Application" factoryName="Application">
      <extension name="coverage" enabled="false" merge="false" runner="idea">
        <pattern>
          <option name="PATTERN" value="groovy.ui.*" />
          <option name="ENABLED" value="true" />
        </pattern>
      </extension>
      <option name="MAIN_CLASS_NAME" value="groovy.ui.Console" />
      <option name="VM_PARAMETERS" />
      <option name="PROGRAM_PARAMETERS" />
      <option name="WORKING_DIRECTORY" value="${'$'}PROJECT_DIR${'$'}" />
      <option name="ALTERNATIVE_JRE_PATH_ENABLED" value="false" />
      <option name="ALTERNATIVE_JRE_PATH" />
      <option name="ENABLE_SWING_INSPECTOR" value="false" />
      <option name="ENV_VARIABLES" />
      <option name="PASS_PARENT_ENVS" value="true" />
      <module name="groovy-console" />
      <envs />
      <RunnerSettings RunnerId="Debug">
        <option name="DEBUG_PORT" value="" />
        <option name="TRANSPORT" value="0" />
        <option name="LOCAL" value="true" />
      </RunnerSettings>
      <RunnerSettings RunnerId="Profile ">
        <option name="myExternalizedOptions" />
      </RunnerSettings>
      <RunnerSettings RunnerId="Run" />
      <ConfigurationWrapper RunnerId="Debug" />
      <ConfigurationWrapper RunnerId="Run" />
      <method />
    </configuration>
""")
                appendNode(runmanager, """<configuration default="false" name="groovy.transform.stc in groovy" type="JUnit" factoryName="JUnit">
      <extension name="coverage" enabled="false" merge="false" runner="idea">
        <pattern>
          <option name="PATTERN" value="groovy.transform.stc.*" />
          <option name="ENABLED" value="true" />
        </pattern>
      </extension>
      <module name="groovy" />
      <option name="ALTERNATIVE_JRE_PATH_ENABLED" value="false" />
      <option name="ALTERNATIVE_JRE_PATH" />
      <option name="PACKAGE_NAME" value="groovy.transform.stc" />
      <option name="MAIN_CLASS_NAME" />
      <option name="METHOD_NAME" />
      <option name="TEST_OBJECT" value="package" />
      <option name="VM_PARAMETERS" />
      <option name="PARAMETERS" />
      <option name="WORKING_DIRECTORY" value="${'$'}PROJECT_DIR${'$'}" />
      <option name="ENV_VARIABLES" />
      <option name="PASS_PARENT_ENVS" value="true" />
      <option name="TEST_SEARCH_SCOPE">
        <value defaultName="moduleWithDependencies" />
      </option>
      <envs />
      <patterns />
      <RunnerSettings RunnerId="Debug">
        <option name="DEBUG_PORT" value="" />
        <option name="TRANSPORT" value="0" />
        <option name="LOCAL" value="true" />
      </RunnerSettings>
      <RunnerSettings RunnerId="Profile ">
        <option name="myExternalizedOptions" />
      </RunnerSettings>
      <RunnerSettings RunnerId="Run" />
      <ConfigurationWrapper RunnerId="Debug" />
      <ConfigurationWrapper RunnerId="Run" />
      <method />
    </configuration>
""")
            }
        }
    }

    module.iml.withXml {
        val node = asNode()

        // remove compiler classes from libraries
        val moduleRoot = (node.get("component") as List<*>).find {
            (it as Node).attribute("name") == "NewModuleRootManager"
        } as Node
        val entry = (moduleRoot.get("orderEntry") as List<*>).find { oe ->
            val oeNode = oe as Node
            val library = oeNode.get("library")
            if (library is List<*> && library.isNotEmpty()) {
                val classes = (library[0] as Node).get("CLASSES")
                if (classes is List<*> && classes.isNotEmpty()) {
                    val root = (classes[0] as Node).get("root")
                    if (root is List<*>) {
                        return@find root.any { r ->
                            val url = (r as Node).attribute("url") as String?
                            url?.contains("classes/compiler") == true
                        }
                    }
                }
            }
            false
        }
        if (entry != null) {
            moduleRoot.remove(entry)
        }

        // add an entry for groovy jar
        val jarTask = rootProject.tasks.named("jar").get() as Jar
        appendNode(moduleRoot, """
            <orderEntry type="module-library" exported="">
              <library>
                <CLASSES>
                  <root url="jar://${jarTask.archiveFile.get().asFile.absolutePath}!/"/>
                </CLASSES>
              </library>
            </orderEntry>
        """)
    }

    tasks.withType<GenerateIdeaProject>().configureEach {
        dependsOn(rootProject.tasks.named("jar"))
    }
}
