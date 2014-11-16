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
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Handle;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import com.github.wolf480pl.sandbox.util.WrappedCheckedException;

public class SandboxAdapter extends ClassVisitor {
    private final RewritePolicy policy;

    public SandboxAdapter(ClassVisitor cv) {
        this(cv, new RewritePolicy() {
            @Override
            public boolean shouldIntercept(InvocationType type, Type owner, String name, Type desc) throws RewriteAbortException {
                return true;
            }
        });
    }

    public SandboxAdapter(ClassVisitor cv, RewritePolicy policy) {
        super(Opcodes.ASM5, cv);
        this.policy = policy;
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        return new MethodAdapter(cv.visitMethod(access, name, desc, signature, exceptions), policy, name.equals("<init>"));
    }

    public static class MethodAdapter extends MethodVisitor {
        public static final String WRAPINVOKE_NAME = "wrapInvoke";
        public static final String WRAPINVOKE_DESC = Type.getMethodDescriptor(getType(CallSite.class), getType(MethodHandles.Lookup.class), getType(String.class), getType(MethodType.class),
                Type.INT_TYPE, getType(String.class), getType(MethodType.class));

        public static final String WRAPCONSTRUCTOR_NAME = "wrapConstructor";
        public static final String WRAPCONSTRUCTOR_DESC = Type.getMethodDescriptor(getType(CallSite.class), getType(MethodHandles.Lookup.class), getType(String.class), getType(MethodType.class),
                getType(String.class), getType(MethodType.class));

        public static final String WRAPHANDLE_NAME = "wrapHandle";
        public static final String WRAPHANDLE_DESC = Type.getMethodDescriptor(getType(CallSite.class), getType(MethodHandles.Lookup.class), getType(String.class), getType(MethodType.class),
                Type.INT_TYPE, getType(String.class), getType(MethodType.class));

        private final RewritePolicy policy;
        private boolean skip;

        public MethodAdapter(MethodVisitor mv, RewritePolicy policy, boolean constructor) {
            super(Opcodes.ASM5, mv);
            this.skip = constructor;
            this.policy = policy;
        }

        @Override
        public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
            if (skip) {
                skip = false;
                mv.visitMethodInsn(opcode, owner, name, desc, itf);
                return;
            }

            InvocationType invtype;
            if (opcode == Opcodes.INVOKESPECIAL && name.equals("<init>")) {
                invtype = InvocationType.INVOKENEWSPECIAL;
            } else {
                invtype = InvocationType.fromInstruction(opcode);
            }

            Type ownerType = Type.getObjectType(owner);
            Type methType = Type.getMethodType(desc);

            try {
                if (!policy.shouldIntercept(invtype, ownerType, name, methType)) {
                    if (invtype == InvocationType.INVOKENEWSPECIAL) {
                        // We ate NEW, so now we have to give it back, since we're not rewriting the call
                        mv.visitTypeInsn(Opcodes.NEW, ownerType.getInternalName());
                    }
                    mv.visitMethodInsn(opcode, owner, name, desc, itf);
                    return;
                }
            } catch (RewriteAbortException e) {
                throw new WrappedCheckedException(e);
            }

            if (invtype == InvocationType.INVOKENEWSPECIAL) {
                Type nt = Type.getMethodType(ownerType, methType.getArgumentTypes());
                desc = nt.getDescriptor();

                /*mv.visitInvokeDynamicInsn(name, desc,
                        new Handle(Opcodes.H_INVOKESTATIC, Type.getInternalName(Bootstraps.class), WRAPINVOKE_NAME, WRAPINVOKE_DESC),
                        invtype.id(), ownerType.getClassName(), methType);*/
                mv.visitInvokeDynamicInsn("init", desc, new Handle(Opcodes.H_INVOKESTATIC, Type.getInternalName(Bootstraps.class), WRAPCONSTRUCTOR_NAME, WRAPCONSTRUCTOR_DESC),
                        ownerType.getClassName(), methType);
                return;
            }

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
                    invtype.id(), ownerType.getClassName(), methType);
        }

        @Override
        public void visitInvokeDynamicInsn(String name, String desc, Handle bsm, Object... bsmArgs) {
            // TODO
            super.visitInvokeDynamicInsn(name, desc, bsm, bsmArgs);
        }

        @Override
        public void visitLdcInsn(Object cst) {
            if (cst instanceof Handle) {
                Handle handle = (Handle) cst;
                InvocationType invtype = InvocationType.fromHandleOpcode(((Handle) cst).getTag());
                Type ownerType = Type.getObjectType(handle.getOwner());
                Type methType = Type.getMethodType(handle.getDesc());

                boolean should;
                try {
                    should = policy.shouldIntercept(invtype, ownerType, handle.getName(), methType);
                } catch (RewriteAbortException e) {
                    throw new WrappedCheckedException(e);
                }
                if (should) {

                    mv.visitInvokeDynamicInsn(handle.getName(), Type.getMethodDescriptor(Type.getType(MethodHandle.class)), new Handle(Opcodes.H_INVOKESTATIC, Type.getInternalName(Bootstraps.class),
                            WRAPHANDLE_NAME, WRAPHANDLE_DESC), invtype.insnOpcode, ownerType.getClassName(), methType);
                    return;
                }
            }
            mv.visitLdcInsn(cst);
        }

        @Override
        public void visitTypeInsn(int opcode, String type) {
            if (opcode == Opcodes.NEW) {
                return; // We remove it because we convert <init> to invokedynamic
            }
            mv.visitTypeInsn(opcode, type);
        }
    }
}
