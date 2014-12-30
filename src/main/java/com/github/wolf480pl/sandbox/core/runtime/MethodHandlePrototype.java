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
package com.github.wolf480pl.sandbox.core.runtime;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.invoke.MethodType;

import com.github.wolf480pl.sandbox.core.InvocationType;

public class MethodHandlePrototype implements Cloneable {
    private InvocationType invType;
    private String owner;
    private Class<?> ownerClass;
    private String name;
    private MethodType methodType;

    public MethodHandlePrototype(InvocationType invType, Class<?> owner, String name, MethodType methType) {
        this(invType, owner.getCanonicalName(), name, methType);
        this.ownerClass = owner;
    }

    public MethodHandlePrototype(InvocationType invType, String owner, String name, MethodType methType) {
        this.invType = invType;
        this.owner = owner;
        this.name = name;
        this.methodType = methType;
    }

    public static Class<?> resolveOwner(String owner, Lookup pointOfView) throws ClassNotFoundException {
        return pointOfView.lookupClass().getClassLoader().loadClass(owner);
    }

    public MethodHandle bake(Lookup lookup) throws NoSuchMethodException, IllegalAccessException, ClassNotFoundException {
        if (ownerClass == null) {
            resolveOwner(lookup);
        }
        return bake(lookup, ownerClass);
    }

    public MethodHandle bake(Lookup lookup, Class<?> ownerCls) throws NoSuchMethodException, IllegalAccessException {
        final MethodHandle handle;

        switch (invType) {
            case INVOKEINTERFACE:
            case INVOKEVIRTUAL:
                handle = lookup.findVirtual(ownerCls, name, methodType);
                break;
            case INVOKESTATIC:
                handle = lookup.findStatic(ownerCls, name, methodType);
                break;
            case INVOKESPECIAL:
                handle = lookup.findSpecial(ownerCls, name, methodType, lookup.lookupClass());
                break;
            case INVOKENEWSPECIAL:
                handle = lookup.findConstructor(ownerCls, methodType);
                break;
            default:
                throw new IllegalArgumentException("Unknown invoke type: " + invType);
        }
        return handle;
    }

    public InvocationType getInvocationType() {
        return invType;
    }

    public void setInvocationType(InvocationType invType) {
        this.invType = invType;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
        this.ownerClass = null;
    }

    public void setOwner(Class<?> owner) {
        this.ownerClass = owner;
        this.owner = owner.getCanonicalName();
    }

    public void resolveOwner(Lookup lookup) throws ClassNotFoundException {
        this.ownerClass = resolveOwner(owner, lookup);
    }

    public Class<?> getOwnerClass() {
        return ownerClass;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public MethodType getMethodType() {
        return methodType;
    }

    public MethodType getHandleMethodType() {
        if (ownerClass == null) {
            return null;
        }
        switch (invType) {
            case INVOKEINTERFACE:
            case INVOKEVIRTUAL:
            case INVOKESPECIAL:
                return methodType.insertParameterTypes(0, ownerClass);
            case INVOKESTATIC:
                return methodType;
            case INVOKENEWSPECIAL:
                return methodType.changeReturnType(ownerClass);
            default:
                throw new IllegalArgumentException("Unknown invoke type: " + invType);
        }
    }

    public void setMethodType(MethodType methodType) {
        this.methodType = methodType;
    }
}
