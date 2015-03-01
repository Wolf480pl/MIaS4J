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
package com.github.wolf480pl.sandbox;

import java.security.CodeSource;
import java.security.SecureClassLoader;

import com.github.wolf480pl.sandbox.core.runtime.BMClassLoader;
import com.github.wolf480pl.sandbox.core.runtime.RuntimePolicy.LoggingPolicy;
import com.github.wolf480pl.sandbox.core.runtime.RuntimePolicy.PassthruPolicy;
import com.github.wolf480pl.sandbox.util.AbstractTransformingClassLoader;

public class SandboxClassLoader1 extends AbstractTransformingClassLoader {
    private final Transformer transformer;
    private final BMClassLoader bmLoader;

    public SandboxClassLoader1(Transformer transformer, SecureClassLoader backend) {
        super(backend);
        this.transformer = transformer;
        this.bmLoader = new BMClassLoader(getParent());
        bmLoader.setRuntimePolicy(new LoggingPolicy(new PassthruPolicy()));
    }

    public SandboxClassLoader1(Transformer transformer, SecureClassLoader backend, ClassLoader parent) {
        super(backend, parent);
        this.transformer = transformer;
        this.bmLoader = new BMClassLoader(getParent());
        bmLoader.setRuntimePolicy(new LoggingPolicy(new PassthruPolicy()));
    }

    @Override
    protected byte[] transform(String name, byte[] bytes, CodeSource cs) {
        return transformer.transfrom(name, bytes);
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        if (name.equalsIgnoreCase(BMClassLoader.BNAME)) {
            Class<?> c = findLoadedClass(name);
            if (c == null) {
                c = bmLoader.loadClass(name);
            }
            if (resolve) {
                resolveClass(c);
            }
            return c;
        } else {
            return super.loadClass(name, resolve);
        }
    }

}
