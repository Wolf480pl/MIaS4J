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
package com.github.wolf480pl.mias4j.core.runtime;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.CodeSource;

import org.apache.commons.io.IOUtils;

import com.github.wolf480pl.mias4j.util.AbstractTransformingClassLoader;
import com.github.wolf480pl.mias4j.util.FilteringClassLoader;
import com.github.wolf480pl.mias4j.util.FilteringClassLoader.Filter;

public abstract class AbstractClassSubLoader extends AbstractTransformingClassLoader {

    public AbstractClassSubLoader(ClassLoader backend, Filter filter) {
        super(backend, new FilteringClassLoader(filter));
    }

    public AbstractClassSubLoader(ClassLoader backend, ClassLoader parent, Filter filter) {
        super(backend, new FilteringClassLoader(filter, parent));
    }

    protected Package copyPackage(Package pkg, CodeSource cs) {
        return definePackage(pkg.getName(), pkg.getSpecificationTitle(), pkg.getSpecificationVersion(), pkg.getSpecificationVendor(), pkg.getImplementationTitle(), pkg.getImplementationVersion(),
                pkg.getImplementationVendor(), pkg.isSealed() ? cs.getLocation() : null);
    }

    @Override
    protected Code findClassImpl(String className, String resName, URL res) throws ClassNotFoundException {
        InputStream is;
        try {
            is = res.openStream();
        } catch (IOException e) {
            throw new ClassNotFoundException(className, e);
        }
        byte[] bytes;
        try {
            bytes = IOUtils.toByteArray(is);
        } catch (IOException e) {
            throw new ClassNotFoundException(className, e);
        }
        Class<?> original = backend.loadClass(className);
        // TODO: Are we sure about this CodeSource?
        CodeSource cs = original.getProtectionDomain().getCodeSource();
        Package pkg = original.getPackage();
        if (pkg != null && getPackage(pkg.getName()) == null) {
            copyPackage(pkg, cs);
        }

        return new Code(bytes, cs);
    }

}