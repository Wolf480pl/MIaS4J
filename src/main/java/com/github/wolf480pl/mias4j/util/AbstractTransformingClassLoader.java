/*
 * Copyright (c) 2014 Wolf480pl <wolf480@interia.pl>
 * This program is licensed under the GNU Lesser General Public License.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.github.wolf480pl.mias4j.util;

import java.io.IOException;
import java.net.URL;
import java.security.CodeSource;
import java.security.SecureClassLoader;
import java.util.Enumeration;

public abstract class AbstractTransformingClassLoader extends SecureClassLoader {
    protected final ClassLoader backend;

    static {
        ClassLoader.registerAsParallelCapable();
    }

    public AbstractTransformingClassLoader(ClassLoader backend) {
        this.backend = backend;
    }

    public AbstractTransformingClassLoader(ClassLoader backend, ClassLoader parent) {
        super(parent);
        this.backend = backend;
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        String resName = getResourceName(name);
        URL res = backend.getResource(resName);
        if (res == null) {
            throw new ClassNotFoundException(name + ": no " + resName);
        }

        Code code = findClassImpl(name, resName, res);

        CodeSource cs = code.cs;
        byte[] bytes = code.bytes;

        bytes = transform(name, bytes, cs);

        return doDefineClass(name, bytes, 0, bytes.length, cs);
    }

    protected abstract byte[] transform(String name, byte[] bytes, CodeSource cs);

    protected abstract Code findClassImpl(String className, String resName, URL res) throws ClassNotFoundException;

    public static class Code {
        public final byte[] bytes;
        public final CodeSource cs;

        public Code(byte[] bytes, CodeSource cs) {
            this.bytes = bytes;
            this.cs = cs;
        }
    }

    protected Class<?> doDefineClass(String name, byte[] bytes, int off, int len, CodeSource cs) {
        return defineClass(name, bytes, off, len, cs);
    }

    protected String getResourceName(String className) {
        final String iname = toInternalName(className);
        return iname + ".class";
    }

    @Override
    protected URL findResource(String name) {
        return backend.getResource(name);

    }

    @Override
    protected Enumeration<URL> findResources(String name) throws IOException {
        return backend.getResources(name);

    }

    public static String toInternalName(String binaryName) {
        return binaryName.replace('.', '/');
    }

}
