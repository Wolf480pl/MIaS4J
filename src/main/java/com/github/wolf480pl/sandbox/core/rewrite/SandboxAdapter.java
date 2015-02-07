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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
import com.github.wolf480pl.sandbox.core.runtime.ArgumentPack;
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

        public static final String WRAPSUPERCONSTRUCTORARGS_NAME = Bootstraps.WRAPSUPERCONSTRUCTORARGS_NAME;
        public static final String WRAPSUPERCONSTRUCTORARGS_DESC = Type.getMethodDescriptor(getType(CallSite.class), getType(MethodHandles.Lookup.class), getType(String.class),
                getType(MethodType.class), getType(String.class), getType(MethodType.class));

        public static final String WRAPSUPERCONSTRUCTORRES_NAME = Bootstraps.WRAPSUPERCONSTRUCTORRES_NAME;
        public static final String WRAPSUPERCONSTRUCTORRES_DESC = Type.getMethodDescriptor(getType(CallSite.class), getType(MethodHandles.Lookup.class), getType(String.class),
                getType(MethodType.class), getType(String.class), getType(MethodType.class));

        public static final String WRAPHANDLE_NAME = Bootstraps.WRAPHANDLE_NAME;
        public static final String WRAPHANDLE_DESC = Type.getMethodDescriptor(getType(CallSite.class), getType(MethodHandles.Lookup.class), getType(String.class), getType(MethodType.class),
                Type.INT_TYPE, getType(String.class), getType(MethodType.class));

        public static final String[] ARGPACK_NAMES = makeArgPackNameTable();
        public static final String[] ARGPACK_DESCS = makeArgPackDescTable();

        private final RewritePolicy policy;
        private final Type clazz;
        private final boolean constructor;
        private AnalyzerAdapter analyzer;
        private Set<Label> removedNews = new HashSet<>();
        private List<Label> labels = new ArrayList<>(3);

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
            labels.clear();

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

            boolean should = true;
            boolean decidedOnNew = (invtype == InvocationType.INVOKENEWSPECIAL) && removedNews.contains(analyzer.stack.get(arg0idx));

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

            LOG.debug("methType: " + methType);
            LOG.debug("arg&ret size: " + argAndReturnSizes + " stack size: " + analyzer.stack.size() + " arg0idx: " + arg0idx);
            if (constructor && invtype == InvocationType.INVOKENEWSPECIAL) {
                if (analyzer.stack.get(arg0idx) == Opcodes.UNINITIALIZED_THIS) {
                    int thisLocal;
                    int freeLocal = analyzer.locals.size();
                    if (analyzer.locals.get(0) == Opcodes.UNINITIALIZED_THIS) {
                        thisLocal = 0;
                    } else {
                        // TODO
                        throw new UnsupportedOperationException();
                    }

                    // Filter the arguments
                    mv.visitInvokeDynamicInsn("init", desc, new Handle(Opcodes.H_INVOKESTATIC, Type.getInternalName(Bootstraps.class), WRAPSUPERCONSTRUCTORARGS_NAME, WRAPSUPERCONSTRUCTORARGS_DESC),
                            ownerType.getClassName(), methType);
                    int packLocal = freeLocal++;
                    mv.visitVarInsn(Opcodes.ASTORE, packLocal);
                    mv.visitVarInsn(Opcodes.ALOAD, thisLocal);
                    for (Type argType : methType.getArgumentTypes()) {
                        if (argType.getSort() == Type.VOID) {
                            continue;
                        }
                        mv.visitVarInsn(Opcodes.ALOAD, packLocal);
                        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, Type.getInternalName(ArgumentPack.class), ARGPACK_NAMES[argType.getSort()], ARGPACK_DESCS[argType.getSort()], false);
                    }

                    // Call the original superinitializer
                    mv.visitMethodInsn(opcode, owner, name, desc, itf);

                    // Give the policy a chance to inspect the outcome of the superinitializer call, and possibly throw something
                    mv.visitVarInsn(Opcodes.ALOAD, thisLocal);
                    mv.visitInvokeDynamicInsn("init", Type.getMethodDescriptor(Type.VOID_TYPE, ownerType), new Handle(Opcodes.H_INVOKESTATIC, Type.getInternalName(Bootstraps.class),
                            WRAPSUPERCONSTRUCTORRES_NAME, WRAPSUPERCONSTRUCTORRES_DESC), ownerType.getClassName(), methType);
                    return;
                }
            }

            if (invtype == InvocationType.INVOKENEWSPECIAL) {
                Type nt = Type.getMethodType(ownerType, methType.getArgumentTypes());
                desc = nt.getDescriptor();

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
            labels.clear();
            // TODO
            super.visitInvokeDynamicInsn(name, desc, bsm, bsmArgs);
        }

        @Override
        public void visitLdcInsn(Object cst) {
            labels.clear();
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


        @Override
        public void visitTypeInsn(int opcode, String type) {
            if (opcode == Opcodes.NEW) {
                boolean should;
                try {
                    // We don't yet know the method signature of the initializer, so we pass null as desc.
                    // The policy is supposed to return true if and only if they're sure they want to rewrite the call, no matter what constructor signature it is.
                    should = policy.shouldIntercept(clazz, InvocationType.INVOKENEWSPECIAL, Type.getObjectType(type), INIT, null);
                } catch (RewriteAbortException e) {
                    throw new WrappedCheckedException(e);
                }
                if (should) {
                    removedNews.addAll(labels);
                    // We convert <init> to invokedynamic, so we don't need to call NEW.
                    // We still need to have something in place of the uninitialized object reference, so we put null there.
                    mv.visitInsn(Opcodes.ACONST_NULL);

                    labels.clear(); // before we return
                    return;
                }
            }
            mv.visitTypeInsn(opcode, type);
            labels.clear();
        }

        @Override
        public void visitLabel(Label label) {
            labels.add(label);
            super.visitLabel(label);
        }

        @Override
        public void visitOtherInsn() {
            labels.clear();
        }

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

        public static String[] makeArgPackNameTable() {
            String[] t = new String[Type.METHOD];
            t[Type.VOID] = null;
            t[Type.BOOLEAN] = ArgumentPack.NEXTINT_NAME;
            t[Type.CHAR] = ArgumentPack.NEXTINT_NAME;
            t[Type.BYTE] = ArgumentPack.NEXTINT_NAME;
            t[Type.SHORT] = ArgumentPack.NEXTINT_NAME;
            t[Type.INT] = ArgumentPack.NEXTINT_NAME;
            t[Type.FLOAT] = ArgumentPack.NEXTFLOAT_NAME;
            t[Type.LONG] = ArgumentPack.NEXTLONG_NAME;
            t[Type.DOUBLE] = ArgumentPack.NEXTDOUBLE_NAME;
            t[Type.ARRAY] = ArgumentPack.NEXTOBJ_NAME;
            t[Type.OBJECT] = ArgumentPack.NEXTOBJ_NAME;
            return t;
        }

        public static final String ARGPACK_NEXTINT_DESC = Type.getMethodDescriptor(Type.INT_TYPE);
        public static final String ARGPACK_NEXTFLOAT_DESC = Type.getMethodDescriptor(Type.FLOAT_TYPE);
        public static final String ARGPACK_NEXTLONG_DESC = Type.getMethodDescriptor(Type.LONG_TYPE);
        public static final String ARGPACK_NEXTDOUBLE_DESC = Type.getMethodDescriptor(Type.DOUBLE_TYPE);
        public static final String ARGPACK_NEXTOBJ_DESC = Type.getMethodDescriptor(Type.getType(Object.class));

        public static String[] makeArgPackDescTable() {
            String[] t = new String[Type.METHOD];
            t[Type.VOID] = null;
            t[Type.BOOLEAN] = ARGPACK_NEXTINT_DESC;
            t[Type.CHAR] = ARGPACK_NEXTINT_DESC;
            t[Type.BYTE] = ARGPACK_NEXTINT_DESC;
            t[Type.SHORT] = ARGPACK_NEXTINT_DESC;
            t[Type.INT] = ARGPACK_NEXTINT_DESC;
            t[Type.FLOAT] = ARGPACK_NEXTFLOAT_DESC;
            t[Type.LONG] = ARGPACK_NEXTLONG_DESC;
            t[Type.DOUBLE] = ARGPACK_NEXTDOUBLE_DESC;
            t[Type.ARRAY] = ARGPACK_NEXTOBJ_DESC;
            t[Type.OBJECT] = ARGPACK_NEXTOBJ_DESC;
            return t;
        }

    }
}
