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

// This file contains support tools for the Gradle build
// with regards to invoke dynamic support (indy)

val indyCapable by rootProject.extra {
    var capable = true
    try {
        Class.forName("java.lang.invoke.MethodHandle")
    } catch (e: Exception) {
        capable = false
    }

    capable && !rootProject.hasProperty("skipIndy")
}

val useIndy by rootProject.extra {
    var indy = false

    // first, check if a system property activates indy support
    indy = indy || (System.getProperty("indy") != null && System.getProperty("indy").toBoolean())

    // ultimately, check if the main project has an extension property setting indy to true
    // which is the case if the build is started with -Pindy=true or during install/dist tasks
    indy = indy || (rootProject.hasProperty("indy") && rootProject.property("indy").toString().toBoolean())

    indy && (rootProject.extra["indyCapable"] as Boolean)
}

val indyBanner by rootProject.extra {
    fun() {
        if (project == rootProject && rootProject.extra["useIndy"] as Boolean) {
            logger.lifecycle("""

         DM .N${'$'}?
          ${'$'}I?7OM.
        .7+?II77MZ       ,:~~
        +I${'$'}7O${'$'}8?  .M..DMMNNMMNZ.
         ONDOMI.     7MMMMMMOO${'$'}I.
         DOM87=      ZMNM8NMI77${'$'}.
    .MNDO?${'$'}8${'$'}?       8MMNMMMN7II.
   MMMO. O${'$'}7Z.   OI8?MDNNM${'$'}${'$'}${'$'}${'$'}7OM
    M8  ZZ7${'$'}.    MMMMMDD7I77I777MMMM.
       ZZ${'$'}7${'$'}     DMM${'$'}N${'$'}ZNMZDMODNDM. DD
     .Z${'$'}${'$'}I${'$'}${'$'}       .ZI777777II778     ?D.
    8${'$'}${'$'}7I${'$'}I+         .${'$'}I7?I7II7D.       ?Z
   .O${'$'}${'$'}7I${'$'}78         N77O+??I?${'$'}.          Z
   =7${'$'}7777${'$'}7 .    .=7NZ?I${'$'}7I+${'$'}.             O
  ~:7${'$'}${'$'}7${'$'}7D+${'$'}~:=Z:=~++77Z${'$'}?IIZ~.             N
  ${'$'}Z${'$'}7O8D8=Z8I7==I~I:+~OZ887${'$'}MOI${'$'}O           .7
  :O${'$'}I+~=?:O8?I${'$'}=++=:===Z${'$'}77ZN++${'$'}+~.          Z.
   :7${'$'}78ZZZZZ=ZZ${'$'}~?==~+DD${'$'}8O${'$'}OO${'$'}7+?:.         ${'$'}
    .=~=+Z7I7?7I${'$'}+~=:~+~O~???77?~+??~         O
        +=IZ7${'$'}7OI${'$'}=Z:~:~=8I?I?+${'$'}Z8++:        N.
          =${'$'}+8ZO${'$'}${'$'}==+=~=?=8${'$'}IIIIID${'$'}ZZ.      Z

                   INDY ENABLED !
""")
        }
    }
}

if (rootProject.extra["useIndy"] as Boolean) {
    gradle.taskGraph.whenReady {
        allTasks.filter { it is JavaForkOptions }.distinct().forEach { task ->
            logger.debug("Adding indy target to project ${(task as Task).project.name} task ${task.name}")
            (task as JavaForkOptions).systemProperties["groovy.target.indy"] = true
        }
    }
}
