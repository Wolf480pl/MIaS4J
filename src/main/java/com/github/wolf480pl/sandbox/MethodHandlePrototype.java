package com.github.wolf480pl.sandbox;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

//TODO: Decide if it's mutable or immutable
public class MethodHandlePrototype {
    // TODO: accessor methods...
    private InvocationType invType;
    private String owner;
    private String name;
    private MethodType methodType;

    public MethodHandlePrototype() {
        // TODO Auto-generated constructor stub
    }

    public MethodHandle bake(MethodHandles.Lookup lookup) throws NoSuchMethodException, IllegalAccessException, ClassNotFoundException {
        Class<?> ownerCls = lookup.lookupClass().getClassLoader().loadClass(owner);
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
}
