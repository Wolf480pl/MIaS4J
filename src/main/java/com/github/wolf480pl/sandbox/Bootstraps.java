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

import java.lang.invoke.CallSite;
import java.lang.invoke.ConstantCallSite;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

import org.objectweb.asm.Opcodes;

public class Bootstraps {

    private Bootstraps() {
    }

    public static CallSite wrapInvoke(MethodHandles.Lookup caller, String invokedName, MethodType invokedType, int opcode, String owner, MethodType originalType) throws NoSuchMethodException,
    IllegalAccessException, ClassNotFoundException {
        System.err.println(caller + " wants " + owner + "." + invokedName + " " + invokedType);
        Class<?> ownerCls = caller.lookupClass().getClassLoader().loadClass(owner);
        final MethodHandle handle;

        // TODO access checks maybe?


        switch (opcode) {
            case Opcodes.INVOKEINTERFACE:
            case Opcodes.INVOKEVIRTUAL:
                handle = caller.findVirtual(ownerCls, invokedName, originalType);
                break;
            case Opcodes.INVOKESTATIC:
                handle = caller.findStatic(ownerCls, invokedName, originalType);
                break;
            case Opcodes.INVOKESPECIAL:
                if (invokedName == "<init>") {
                    handle = caller.findConstructor(ownerCls, originalType);
                } else {
                    handle = caller.findSpecial(ownerCls, invokedName, originalType, caller.lookupClass());
                }
                break;
            default:
                throw new IllegalArgumentException("Unknown invoke opcode: " + opcode);
        }
        return new ConstantCallSite(handle);
    }

    public static CallSite wrapConstructor(MethodHandles.Lookup caller, String invokedName, MethodType invokedType, String owner, MethodType originalType) throws NoSuchMethodException,
    IllegalAccessException, ClassNotFoundException {
        return wrapInvoke(caller, "<init>", invokedType, Opcodes.INVOKESPECIAL, owner, originalType);
    }

    public static CallSite wrapInvokeDynamic(MethodHandles.Lookup caller, String invokedName, MethodType invokedType, Object... args) {
        // TODO
        return null;
    }

}
