package com.github.wolf480pl.sandbox;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.invoke.MethodType;

public class MethodHandlePrototype implements Cloneable {
    private InvocationType invType;
    private String owner;
    private String name;
    private MethodType methodType;

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
        Class<?> ownerCls = resolveOwner(owner, lookup);
        return bake(lookup, ownerCls);
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

    public void setMethodType(MethodType methodType) {
        this.methodType = methodType;
    }
}
