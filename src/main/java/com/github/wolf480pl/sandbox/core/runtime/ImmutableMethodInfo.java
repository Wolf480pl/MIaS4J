package com.github.wolf480pl.sandbox.core.runtime;

import java.lang.invoke.MethodType;

import com.github.wolf480pl.sandbox.core.InvocationType;

public class ImmutableMethodInfo implements MethodInfo {
    private final InvocationType invType;
    private final String owner;
    private final String name;
    private final MethodType methodType;

    public ImmutableMethodInfo(InvocationType invType, String owner, String name, MethodType methType) {
        this.invType = invType;
        this.owner = owner;
        this.name = name;
        this.methodType = methType;
    }

    @Override
    public InvocationType getInvocationType() {
        return invType;
    }

    @Override
    public String getOwner() {
        return owner;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public MethodType getMethodType() {
        return methodType;
    }

}
