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
package com.github.wolf480pl.sandbox.core.rewrite;

import static org.objectweb.asm.Type.getType;

import java.lang.invoke.CallSite;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.AnalyzerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.wolf480pl.sandbox.core.InvocationType;
import com.github.wolf480pl.sandbox.core.runtime.Bootstraps;
import com.github.wolf480pl.sandbox.util.SequenceMethodVisitor;
import com.github.wolf480pl.sandbox.util.WrappedCheckedException;

public class SandboxAdapter extends ClassVisitor {
    public static final String INIT = "<init>";
    protected static final Logger LOG = LoggerFactory.getLogger(SandboxAdapter.class);

    private final RewritePolicy policy;
    private Type clazz;

    public SandboxAdapter(ClassVisitor cv) {
        this(cv, RewritePolicy.ALWAYS_INTERCEPT);
    }

    public SandboxAdapter(ClassVisitor cv, RewritePolicy policy) {
        super(Opcodes.ASM5, cv);
        this.policy = policy;
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        this.clazz = Type.getObjectType(name);
        super.visit(version, access, name, signature, superName, interfaces);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        MethodAdapter ma = new MethodAdapter(cv.visitMethod(access, name, desc, signature, exceptions), policy, clazz, name.equals(INIT));
        AnalyzerAdapter analyzer = new AnalyzerAdapter(clazz.getInternalName(), access, name, desc, ma);
        ma.setAnalyzer(analyzer);
        return analyzer;
    }

    public static class MethodAdapter extends SequenceMethodVisitor {
        public static final String WRAPINVOKE_NAME = Bootstraps.WRAPINVOKE_NAME;
        public static final String WRAPINVOKE_DESC = Type.getMethodDescriptor(getType(CallSite.class), getType(MethodHandles.Lookup.class), getType(String.class), getType(MethodType.class),
                Type.INT_TYPE, getType(String.class), getType(MethodType.class));

        public static final String WRAPCONSTRUCTOR_NAME = Bootstraps.WRAPCONSTRUCTOR_NAME;
        public static final String WRAPCONSTRUCTOR_DESC = Type.getMethodDescriptor(getType(CallSite.class), getType(MethodHandles.Lookup.class), getType(String.class), getType(MethodType.class),
                getType(String.class), getType(MethodType.class));

        public static final String WRAPHANDLE_NAME = Bootstraps.WRAPHANDLE_NAME;
        public static final String WRAPHANDLE_DESC = Type.getMethodDescriptor(getType(CallSite.class), getType(MethodHandles.Lookup.class), getType(String.class), getType(MethodType.class),
                Type.INT_TYPE, getType(String.class), getType(MethodType.class));

        private final RewritePolicy policy;
        private final Type clazz;
        private final boolean constructor;
        private AnalyzerAdapter analyzer;

        public MethodAdapter(MethodVisitor mv, RewritePolicy policy, Type clazz, boolean constructor) {
            super(mv);
            this.constructor = constructor;
            this.clazz = clazz;
            this.policy = policy;
        }

        public void setAnalyzer(AnalyzerAdapter analyzer) {
            this.analyzer = analyzer;
        }

        @Override
        public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {

            InvocationType invtype;
            if (opcode == Opcodes.INVOKESPECIAL && name.equals(INIT)) {
                invtype = InvocationType.INVOKENEWSPECIAL;
            } else {
                invtype = InvocationType.fromInstruction(opcode);
            }

            Type ownerType = Type.getObjectType(owner);
            Type methType = Type.getMethodType(desc);

            int argAndReturnSizes = methType.getArgumentsAndReturnSizes();
            int arg0idx = analyzer.stack.size() - (argAndReturnSizes >> 2);

            LOG.debug("methType: " + methType);
            LOG.debug("arg&ret size: " + argAndReturnSizes + " stack size: " + analyzer.stack.size() + " arg0idx: " + arg0idx);
            if (constructor && invtype == InvocationType.INVOKENEWSPECIAL) {
                if (analyzer.stack.get(arg0idx) == Opcodes.UNINITIALIZED_THIS) {
                    //TODO: Intercept these somehow, too
                    mv.visitMethodInsn(opcode, owner, name, desc, itf);
                    return;
                }
            }

            boolean should = true;
            boolean decidedOnNew = false;

            if (!decidedOnNew) {
                try {
                    should = policy.shouldIntercept(clazz, invtype, ownerType, name, methType);
                } catch (RewriteAbortException e) {
                    throw new WrappedCheckedException(e);
                }
            }
            if (!should) {
                mv.visitMethodInsn(opcode, owner, name, desc, itf);
                return;
            }

            if (invtype == InvocationType.INVOKENEWSPECIAL) {
                Type nt = Type.getMethodType(ownerType, methType.getArgumentTypes());
                desc = nt.getDescriptor();

                /*mv.visitInvokeDynamicInsn(name, desc,
                        new Handle(Opcodes.H_INVOKESTATIC, Type.getInternalName(Bootstraps.class), WRAPINVOKE_NAME, WRAPINVOKE_DESC),
                        invtype.id(), ownerType.getClassName(), methType);*/
                mv.visitInvokeDynamicInsn("init", desc, new Handle(Opcodes.H_INVOKESTATIC, Type.getInternalName(Bootstraps.class), WRAPCONSTRUCTOR_NAME, WRAPCONSTRUCTOR_DESC),
                        ownerType.getClassName(), methType);

                // stack: ... uninitialized returned
                int local = analyzer.locals.size();
                mv.visitVarInsn(Opcodes.ASTORE, local);
                // stack: ... uninitialized
                mv.visitInsn(Opcodes.POP);
                // stack: ...

                Label uninitialized = (Label) analyzer.stack.get(arg0idx);
                int currentStack = arg0idx - 1;

                // remove all occurrences of the uninitialized reference from stack...
                while (currentStack >= 0 && analyzer.stack.get(currentStack) == uninitialized) {
                    mv.visitInsn(Opcodes.POP);
                    --currentStack;
                }
                int lowestStack = currentStack;

                int deepestRef = -1;
                for (int i = 0; i < lowestStack; ++i) {
                    if (analyzer.stack.get(i) == uninitialized) {
                        deepestRef = i;
                        break;
                    }
                }
                if (deepestRef >= 0) {
                    findReplaceInStack(uninitialized, local, local + 1, deepestRef, currentStack);
                }

                // ...and replace them with the return value of our method handle
                while (currentStack < arg0idx - 1) {
                    mv.visitVarInsn(Opcodes.ALOAD, local);
                    ++currentStack;
                }

                /*
                for (int i = 0; i < lowestStack; ++i) {
                    if (analyzer.stack.get(i) == uninitialized) {
                        // Crap... a copy of our uninitialized reference is buried somewhere in the stack... we can't fix this yet, so... I guess we crash
                        throw new UnsupportedOperationException("Couldn't rewrite constructor: An uninitialized reference to " + owner + " was buried deep in the stack");
                    }
                }
                 */

                // replace all occurrences of the uninitialized reference in locals with the return value of our metod handle
                for (int i = 0; i < analyzer.locals.size(); ++i) {
                    if (analyzer.locals.get(i) == uninitialized) {
                        mv.visitVarInsn(Opcodes.ALOAD, local);
                        mv.visitVarInsn(Opcodes.ASTORE, i);
                    }
                }

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
                    should = policy.shouldIntercept(clazz, invtype, ownerType, handle.getName(), methType);
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

        /*
        @Override
        public void visitTypeInsn(int opcode, String type) {
            if (opcode == Opcodes.NEW) {
                if (constructor) {
                    ++newsSeen;
                }
                boolean should;
                try {
                    // We don't yet know the method signature of the initializer, so we pass null as desc.
                    should = policy.shouldIntercept(clazz, InvocationType.INVOKENEWSPECIAL, Type.getObjectType(type), INIT, null);
                    constructors.put(type, should);
                } catch (RewriteAbortException e) {
                    throw new WrappedCheckedException(e);
                }
                if (should) {
                    newJustSeen = true;
                    return; // We remove it because we convert <init> to invokedynamic
                }
            }
            mv.visitTypeInsn(opcode, type);
        }
         */

        /*
        @Override
        public void visitInsn(int opcode) {
            if (newJustSeen && opcode == Opcodes.DUP) {
                // We remove it because we convert <init> to invokedynamic
            } else {
                mv.visitInsn(opcode);
            }
            newJustSeen = false;
        }

        @Override
        public void visitOtherInsn() {
            newJustSeen = false;
        }
         */

        protected void findReplaceInStack(Object find, int replaceLocal, int firstFreeLocal, int bottom, int top) {
            int loc = firstFreeLocal;
            for (int i = top; i >= bottom; --i) {
                Object type = analyzer.stack.get(i);
                if (type == find) {
                    mv.visitInsn(Opcodes.POP);
                } else if (type == Opcodes.TOP && i > 0) {
                    if (analyzer.stack.get(i - 1) == Opcodes.DOUBLE) {
                        mv.visitVarInsn(Opcodes.DSTORE, loc);
                        loc += 2;
                        --i;
                    } else if (analyzer.stack.get(i - 1) == Opcodes.LONG) {
                        mv.visitVarInsn(Opcodes.LSTORE, loc);
                        loc += 2;
                        --i;
                    } else {
                        // WTF is that?
                        throw new IllegalStateException("There was TOP on the stack without lower half");
                    }
                } else {
                    if (type == Opcodes.INTEGER) {
                        mv.visitVarInsn(Opcodes.ISTORE, loc);
                    } else if (type == Opcodes.FLOAT) {
                        mv.visitVarInsn(Opcodes.FSTORE, loc);
                    } else {
                        mv.visitVarInsn(Opcodes.ASTORE, loc);
                    }
                    ++loc;
                }
            }
            --loc;
            for (int i = bottom; i <= top; ++i) {
                Object type = analyzer.stack.get(i);
                if (type == find) {
                    mv.visitVarInsn(Opcodes.ALOAD, replaceLocal);
                } else if (type == Opcodes.DOUBLE) {
                    mv.visitVarInsn(Opcodes.DLOAD, loc);
                    loc -= 2;
                    ++i; // skip TOP
                } else if (type == Opcodes.LONG) {
                    mv.visitVarInsn(Opcodes.LSTORE, loc);
                    loc -= 2;
                    ++i; // skip TOP
                } else if (type == Opcodes.TOP) {
                    // WTF is that? If this was an upper part of DOUBLE or LONG, we would skip it by additional ++i
                    throw new IllegalStateException("There was TOP on the stack without lower half");
                } else {
                    if (type == Opcodes.INTEGER) {
                        mv.visitVarInsn(Opcodes.ILOAD, loc);
                    } else if (type == Opcodes.FLOAT) {
                        mv.visitVarInsn(Opcodes.FLOAD, loc);
                    } else {
                        mv.visitVarInsn(Opcodes.ALOAD, loc);
                    }
                    --loc;
                }
            }
        }
    }
}
