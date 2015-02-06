package com.github.wolf480pl.sandbox.core.runtime;

import java.util.Arrays;
import java.util.Iterator;

public class ArgumentPack {
    private final Iterator<? extends Object> it;

    public ArgumentPack(Object... args) {
        this(Arrays.asList(args));
    }

    public ArgumentPack(Iterable<? extends Object> args) {
        this.it = args.iterator();
    }

    public static final String NEXTOBJ_NAME = "nextObject";

    public Object nextObject() {
        return it.next();
    }

    public static final String NEXTINT_NAME = "nextInt";

    public int nextInt() {
        return (Integer) it.next();
    }

    public static final String NEXTFLOAT_NAME = "nextFloat";

    public float nextFloat() {
        return (Float) it.next();
    }

    public static final String NEXTLONG_NAME = "nextLong";

    public long nextLong() {
        return (Long) it.next();
    }

    public static final String NEXTDOUBLE_NAME = "nextDouble";

    public double nextDouble() {
        return (Double) it.next();
    }

}
