package com.github.wolf480pl.sandbox;

import org.objectweb.asm.Type;

public interface RewritePolicy {

    boolean shouldIntercept(InvocationType type, Type owner, String name, Type desc) throws RewriteAbortException;
}
