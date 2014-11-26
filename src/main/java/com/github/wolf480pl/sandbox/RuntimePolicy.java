package com.github.wolf480pl.sandbox;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;

public interface RuntimePolicy {

    MethodHandle intercept(MethodHandles.Lookup caller, MethodHandlePrototype method);
}
