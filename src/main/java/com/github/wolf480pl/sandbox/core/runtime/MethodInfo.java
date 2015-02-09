package com.github.wolf480pl.sandbox.core.runtime;

import java.lang.invoke.MethodType;

import com.github.wolf480pl.sandbox.core.InvocationType;

public interface MethodInfo {

    InvocationType getInvocationType();

    String getOwner();

    String getName();

    MethodType getMethodType();

}