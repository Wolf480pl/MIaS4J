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

import static org.objectweb.asm.Type.getType;

import java.lang.invoke.CallSite;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Handle;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

public class SandboxAdapter extends ClassVisitor {

    public SandboxAdapter(ClassVisitor cv) {
        super(Opcodes.ASM5, cv);
        // TODO Auto-generated constructor stub
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        return new MethodAdapter(cv.visitMethod(access, name, desc, signature, exceptions), name.equals("<init>"));
    }

    public static class MethodAdapter extends MethodVisitor {
        public static final String WRAPINVOKE_NAME = "wrapInvoke";
        public static final String WRAPINVOKE_DESC = Type.getMethodDescriptor(getType(CallSite.class), getType(MethodHandles.Lookup.class), getType(String.class), getType(MethodType.class),
                Type.INT_TYPE, getType(String.class), getType(MethodType.class));

        private boolean skip;

        public MethodAdapter(MethodVisitor mv, boolean constructor) {
            super(Opcodes.ASM5, mv);
            this.skip = constructor;
        }

        @Override
        public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
            if (skip) {
                skip = false;
                mv.visitMethodInsn(opcode, owner, name, desc, itf);
                return;
            }
            Type ownerType = Type.getObjectType(owner);
            Type methType = Type.getMethodType(desc);
            if (opcode != Opcodes.INVOKESTATIC) {
                Type[] argTypes = methType.getArgumentTypes();
                Type[] newArgTypes = new Type[argTypes.length + 1];
                newArgTypes[0] = ownerType;
                System.arraycopy(argTypes, 0, newArgTypes, 1, argTypes.length);
                Type nt = Type.getMethodType(methType.getReturnType(), newArgTypes);
                desc = nt.getDescriptor();
            }
            mv.visitInvokeDynamicInsn(name, desc,
                    new Handle(Opcodes.H_INVOKESTATIC, Type.getInternalName(Bootstraps.class), WRAPINVOKE_NAME, WRAPINVOKE_DESC),
                    opcode, ownerType.getClassName(), methType);
        }

        @Override
        public void visitInvokeDynamicInsn(String name, String desc, Handle bsm, Object... bsmArgs) {
            // TODO
            super.visitInvokeDynamicInsn(name, desc, bsm, bsmArgs);
        }

        @Override
        public void visitLdcInsn(Object cst) {
            // TODO
            super.visitLdcInsn(cst);
        }
    }

}
