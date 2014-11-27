package com.github.wolf480pl.sandbox;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles.Lookup;

public interface RuntimePolicy {

    // TODO: Maybe it should be allowed to throw something?
    MethodHandle intercept(Lookup caller, MethodHandlePrototype method);


    // Useful implementations
    // TODO: Move these elsewhere
    public static class PassthruPolicy implements RuntimePolicy {
        @Override
        public MethodHandle intercept(Lookup caller, MethodHandlePrototype method) {
            try {
                return method.bake(caller);
            } catch (NoSuchMethodException | IllegalAccessException | ClassNotFoundException e) {
                // TODO: are we sure this is the correct handling?
                throw new RuntimeException(e);
            }
        }
    }

    public static class LoggingPolicy implements RuntimePolicy {
        private final RuntimePolicy pol;

        public LoggingPolicy(RuntimePolicy pol) {
            this.pol = pol;
        }

        @Override
        public MethodHandle intercept(Lookup caller, MethodHandlePrototype method) {
            System.err.println(caller + " wants " + method.getOwner() + "." + method.getName() + " " + method.getMethodType());
            return pol.intercept(caller, method);
        }
    }
}
