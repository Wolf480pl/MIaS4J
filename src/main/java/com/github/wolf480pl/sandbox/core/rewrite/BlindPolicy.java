package com.github.wolf480pl.sandbox.core.rewrite;

import org.objectweb.asm.Type;

import com.github.wolf480pl.sandbox.core.InvocationType;

public class BlindPolicy extends AbstractRewritePolicy {
    private final boolean should;
    public static final RewritePolicy ALWAYS_INTERCEPT = new BlindPolicy(true);
    public static final RewritePolicy NEVER_INTERCEPT = new BlindPolicy(false);

    public BlindPolicy(boolean should) {
        this.should = should;
    }

    @Override
    public boolean shouldIntercept(Type caller, InvocationType type, Type owner, String name, Type desc) throws RewriteAbortException {
        return should;
    }

}