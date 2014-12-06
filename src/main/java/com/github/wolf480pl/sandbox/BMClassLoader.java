package com.github.wolf480pl.sandbox;

import java.io.IOException;
import java.io.InputStream;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.security.CodeSource;
import java.security.SecureClassLoader;

import org.apache.commons.io.IOUtils;
import org.objectweb.asm.Type;

/**
 * This classloader is a CREDENTIAL. Anyone having a reference to it can SET THE RUNTIME POLICY for anyone using Bootstraps class loaded with this classloader.
 */
public class BMClassLoader extends SecureClassLoader {
    public static final String BNAME = Bootstraps.class.getCanonicalName();
    public static final String INAME = Type.getInternalName(Bootstraps.class);
    public static final String METH_NAME = Bootstraps.SETPOLICY_NAME;
    public static final MethodType METH_TYPE = MethodType.methodType(Void.TYPE, RuntimePolicy.class);

    private MethodHandle policySetter = null;

    static {
        ClassLoader.registerAsParallelCapable();
    }

    public BMClassLoader(ClassLoader parent) {
        super(parent);
    }

    public void setRuntimePolicy(RuntimePolicy policy) {
        if (policySetter == null) {
            try {
                loadClass(BNAME, true);
            } catch (ClassNotFoundException e) {
                throw new IllegalStateException("Couldn't load Bootstraps class through ourselves", e);
            }
        }
        try {
            policySetter.invoke(policy);
        } catch (Throwable e) {
            if (e instanceof Error) {
                throw (Error) e;
            }
            if (e instanceof RuntimeException) {
                throw (RuntimeException) e;
            }
            throw new IllegalStateException("Bootstraps.setRuntimePolicy threw a checked exception", e);
        }
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        if (name.equals(BNAME)) {
            InputStream is = getParent().getResourceAsStream(INAME);
            if (is == null) {
                throw new ClassNotFoundException(name);
            }
            byte[] bytes;
            try {
                bytes = IOUtils.toByteArray(is);
            } catch (IOException e) {
                throw new ClassNotFoundException(name, e);
            }
            // TODO: Are we sure about this CodeSource?
            CodeSource cs = Bootstraps.class.getProtectionDomain().getCodeSource();
            Package pkg = Bootstraps.class.getPackage();
            if (pkg != null) {
                copyPackage(pkg, cs);
            }
            Class<?> c = defineClass(name, bytes, 0, bytes.length, cs);
            initHandle(c);
            return c;
        } else {
            throw new ClassNotFoundException(name);
        }
    }

    private void initHandle(Class<?> cls) {
        try {
            policySetter = MethodHandles.lookup().findStatic(cls, METH_NAME, METH_TYPE);
        } catch (NoSuchMethodException | IllegalAccessException e) {
            throw new IllegalStateException("Couldn't find method handle for Bootstraps.setPolicy", e);
        }
    }

    protected Package copyPackage(Package pkg, CodeSource cs) {
        return definePackage(pkg.getName(), pkg.getSpecificationTitle(), pkg.getSpecificationVersion(), pkg.getSpecificationVendor(), pkg.getImplementationTitle(), pkg.getImplementationVersion(),
                pkg.getImplementationVendor(), pkg.isSealed() ? cs.getLocation() : null);
    }

}
