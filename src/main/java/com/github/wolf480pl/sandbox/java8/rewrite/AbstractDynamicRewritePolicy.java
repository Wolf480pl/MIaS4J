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
package com.github.wolf480pl.sandbox.java8.rewrite;

import static com.github.wolf480pl.sandbox.core.rewrite.SandboxAdapter.MethodAdapter.WRAPDYNAMIC_DESC;
import static com.github.wolf480pl.sandbox.core.rewrite.SandboxAdapter.MethodAdapter.WRAPDYNAMIC_NAME;

import java.util.Arrays;
import java.util.List;

import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import com.github.wolf480pl.sandbox.core.InvocationType;
import com.github.wolf480pl.sandbox.core.rewrite.RewriteAbortException;
import com.github.wolf480pl.sandbox.core.rewrite.RewritePolicy;
import com.github.wolf480pl.sandbox.core.runtime.Bootstraps;

public abstract class AbstractDynamicRewritePolicy implements RewritePolicy {

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
        // NOTE: Varags collection for bootstrap methods works only since Java 8
        newBootstrapArgs.addAll(Arrays.asList(bootstrapArgs));

        return new Handle(Opcodes.H_INVOKESTATIC, Type.getInternalName(Bootstraps.class), WRAPDYNAMIC_NAME, WRAPDYNAMIC_DESC);
    }

}
