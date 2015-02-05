/*
 * Copyright (c) 2014 Wolf480pl <wolf480@interia.pl>
 * This program is licensed under the GNU Lesser General Public License.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.github.wolf480pl.sandbox;

import java.io.FileOutputStream;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class MakeTestBuryUninitialized implements Opcodes {

    public static byte[] dump () throws Exception {

        ClassWriter cw = new ClassWriter(0);
        FieldVisitor fv;
        MethodVisitor mv;
        AnnotationVisitor av0;

        cw.visit(V1_7, ACC_PUBLIC + ACC_SUPER, "Test3", null, "java/lang/Object", null);

        {
            mv = cw.visitMethod(ACC_PUBLIC, "<init>", "(I)V", null, null);
            mv.visitCode();
            mv.visitVarInsn(ALOAD, 0);
            mv.visitLdcInsn("hey");
            mv.visitVarInsn(ILOAD, 1);
            mv.visitInsn(ICONST_1);
            mv.visitInsn(IADD);
            mv.visitMethodInsn(INVOKESTATIC, "Test3", "say", "(Ljava/lang/Object;I)I", false);
            mv.visitMethodInsn(INVOKESTATIC, "java/lang/Integer", "valueOf", "(I)Ljava/lang/Integer;", false);
            mv.visitTypeInsn(NEW, "java/util/LinkedList");
            mv.visitInsn(DUP);
            mv.visitMethodInsn(INVOKESPECIAL, "java/util/LinkedList", "<init>", "()V", false);
            mv.visitMethodInsn(INVOKESPECIAL, "Test3", "<init>", "(Ljava/lang/Integer;Ljava/util/List;)V", false);
            mv.visitLdcInsn("hey1");
            mv.visitInsn(ICONST_1);
            mv.visitTypeInsn(NEW, "java/util/Date");
            mv.visitInsn(DUP_X2);

            mv.visitMethodInsn(INVOKESPECIAL, "java/util/Date", "<init>", "()V", false);
            mv.visitInsn(POP2);

            mv.visitInsn(ICONST_0);
            mv.visitMethodInsn(INVOKESTATIC, "Test3", "say", "(Ljava/lang/Object;I)I", false);
            mv.visitInsn(POP);
            mv.visitInsn(RETURN);
            mv.visitMaxs(4, 2);
            mv.visitEnd();
        }
        {
            mv = cw.visitMethod(ACC_PUBLIC, "<init>", "(Ljava/lang/Integer;Ljava/util/List;)V", "(Ljava/lang/Integer;Ljava/util/List<Ljava/lang/Integer;>;)V", null);
            mv.visitCode();
            mv.visitVarInsn(ALOAD, 0);
            mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
            mv.visitVarInsn(ALOAD, 2);
            mv.visitVarInsn(ALOAD, 1);
            mv.visitMethodInsn(INVOKEINTERFACE, "java/util/List", "add", "(Ljava/lang/Object;)Z", true);
            mv.visitInsn(POP);
            mv.visitInsn(RETURN);
            mv.visitMaxs(2, 3);
            mv.visitEnd();
        }
        {
            mv = cw.visitMethod(ACC_PUBLIC + ACC_STATIC, "main", "([Ljava/lang/String;)V", null, null);
            mv.visitCode();
            mv.visitTypeInsn(NEW, "Test3");
            mv.visitIntInsn(BIPUSH, 10);
            mv.visitMethodInsn(INVOKESPECIAL, "Test3", "<init>", "(I)V", false);
            mv.visitFieldInsn(GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
            mv.visitLdcInsn("byebye");
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V", false);
            mv.visitInsn(RETURN);
            mv.visitMaxs(2, 1);
            mv.visitEnd();
        }
        {
            mv = cw.visitMethod(ACC_PRIVATE + ACC_STATIC, "say", "(Ljava/lang/Object;I)I", null, null);
            mv.visitCode();
            mv.visitFieldInsn(GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
            mv.visitVarInsn(ALOAD, 0);
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/Object;)V", false);
            mv.visitVarInsn(ILOAD, 1);
            mv.visitInsn(IRETURN);
            mv.visitMaxs(2, 2);
            mv.visitEnd();
        }
        cw.visitEnd();

        return cw.toByteArray();
    }

    public static void main(String[] args) throws Exception {
        FileOutputStream os = new FileOutputStream(args[0]);
        os.write(dump());
        os.flush();
        os.close();
    }
}
