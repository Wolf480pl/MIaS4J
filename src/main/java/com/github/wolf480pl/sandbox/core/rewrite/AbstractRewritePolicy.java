package com.github.wolf480pl.sandbox.core.rewrite;

import static com.github.wolf480pl.sandbox.core.rewrite.SandboxAdapter.MethodAdapter.WRAPDYNAMIC_DESC;
import static com.github.wolf480pl.sandbox.core.rewrite.SandboxAdapter.MethodAdapter.WRAPDYNAMIC_NAME;

import java.util.Arrays;
import java.util.List;

import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import com.github.wolf480pl.sandbox.core.InvocationType;
import com.github.wolf480pl.sandbox.core.runtime.Bootstraps;

public abstract class AbstractRewritePolicy implements RewritePolicy {

    @Override
    public Handle interceptDynamic(Type caller, String name, Type desc, Handle bootstrapMethod, Object[] bootstrapArgs, List<Object> newBootstrapArgs) throws RewriteAbortException {
        InvocationType invType = InvocationType.fromHandleOpcode(bootstrapMethod.getTag());
        Type bsmOwner = Type.getObjectType(bootstrapMethod.getOwner());
        Type bsmType = Type.getMethodType(bootstrapMethod.getDesc());

        boolean should = shouldIntercept(caller, invType, bsmOwner, bootstrapMethod.getName(), bsmType);

        if (!should) {
            return null;
        }

        newBootstrapArgs.add(invType.id());
        newBootstrapArgs.add(bsmOwner.getClassName());
        newBootstrapArgs.add(bootstrapMethod.getName());
        newBootstrapArgs.add(bsmType);
        // FIXME: Varags collection of bootstrap methods works only since Java 8
        newBootstrapArgs.addAll(Arrays.asList(bootstrapArgs));

        return new Handle(Opcodes.H_INVOKESTATIC, Type.getInternalName(Bootstraps.class), WRAPDYNAMIC_NAME, WRAPDYNAMIC_DESC);
    }

}
