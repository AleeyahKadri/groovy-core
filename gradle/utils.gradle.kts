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

import org.codehaus.groovy.classgen.AnnotationVisitor
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.FieldVisitor
import org.objectweb.asm.Label
import org.objectweb.asm.MethodVisitor

import org.objectweb.asm.Opcodes.*

buildscript {
    repositories {
        mavenCentral()
    }
    dependencies {
        classpath("org.ow2.asm:asm:${project.extra["asmVersion"]}")
    }
}

/**
 * This tasks generates an utility class which allows sneaky throwing.
 */
val exceptionUtils by tasks.registering {
    val classFiles = listOf(
        "${layout.buildDirectory.get()}/generated-classes/org/codehaus/groovy/runtime/ExceptionUtils.class",
        "${tasks.named<JavaCompile>("compileJava").get().destinationDirectory.get()}/org/codehaus/groovy/runtime/ExceptionUtils.class"
    )
    outputs.files(classFiles)

    doLast {
        val cw = ClassWriter(0)
        var fv: FieldVisitor
        var mv: MethodVisitor
        var av0: AnnotationVisitor

        cw.visit(V1_5, ACC_PUBLIC + ACC_SUPER, "org/codehaus/groovy/runtime/ExceptionUtils", null, "java/lang/Object", null)

        cw.visitSource("ExceptionUtils.java", null)

        mv = cw.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null)
        mv.visitCode()
        val l0 = Label()
        mv.visitLabel(l0)
        mv.visitLineNumber(18, l0)
        mv.visitVarInsn(ALOAD, 0)
        mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false)
        mv.visitInsn(RETURN)
        val l1 = Label()
        mv.visitLabel(l1)
        mv.visitLocalVariable("this", "Lorg/codehaus/groovy/runtime/ExceptionUtils;", null, l0, l1, 0)
        mv.visitMaxs(1, 1)
        mv.visitEnd()

        mv = cw.visitMethod(ACC_PUBLIC + ACC_STATIC, "sneakyThrow", "(Ljava/lang/Throwable;)V", null, null)
        mv.visitCode()
        val l2 = Label()
        mv.visitLabel(l2)
        mv.visitLineNumber(20, l2)
        mv.visitVarInsn(ALOAD, 0)
        mv.visitInsn(ATHROW)
        val l3 = Label()
        mv.visitLabel(l3)
        mv.visitLocalVariable("e", "Ljava/lang/Throwable;", null, l2, l3, 0)
        mv.visitMaxs(1, 1)
        mv.visitEnd()

        cw.visitEnd()

        logger.lifecycle("Generating ExceptionUtils")
        classFiles.forEach { classFile ->
            val output = file(classFile)
            output.parentFile.mkdirs()
            output.outputStream().use {
                it.write(cw.toByteArray())
            }
        }
    }
}
