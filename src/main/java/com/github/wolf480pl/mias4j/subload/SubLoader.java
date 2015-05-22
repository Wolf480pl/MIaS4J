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
package com.github.wolf480pl.mias4j.subload;

import java.security.CodeSource;
import java.util.HashMap;
import java.util.Map;

import com.github.wolf480pl.mias4j.core.runtime.AbstractClassSubLoader;
import com.github.wolf480pl.mias4j.util.FilteringClassLoader;

/*
 *  FIXME: Don't extend BestEffortTransformingClassLoader here, because it assumes it's not safe to load unmodified class,
 *  and therefore tries to guess the CodeSource in a might-not-always-work way. Instead, extract a superclass from
 *  BMClassLaoder and make SubLoader its subclass.
 */
public class SubLoader extends AbstractClassSubLoader {
    private final Map<String, String> subLoadMap = new HashMap<>();
    private final Map<String, String> subLoadRevMap = new HashMap<>();

    public SubLoader(ClassLoader backend) {
        this(backend, new Filter());
    }

    public SubLoader(ClassLoader backend, ClassLoader parent) {
        this(backend, parent, new Filter());
    }

    // A hack to get the filter in a variable, so that we can later give it our `this` refernece
    private SubLoader(ClassLoader backend, Filter filter) {
        super(backend, filter);
        filter.loader = this;
    }

    // A hack to get the filter in a variable, so that we can later give it our `this` refernece
    private SubLoader(ClassLoader backend, ClassLoader parent, Filter filter) {
        super(backend, filter);
        filter.loader = this;
    }

    @Override
    protected String getResourceName(String className) {
        String mapName = subLoadRevMap.get(className);
        if (isSubloadWithRename(className, mapName)) {
            className = mapName;
        }
        return super.getResourceName(className);
    }

    @Override
    protected byte[] transform(String name, byte[] bytes, CodeSource cs) {
        String mapName = subLoadRevMap.get(name);
        if (isSubloadWithRename(name, mapName)) {
            // TODO
        }

        return bytes;
    }

    protected static boolean isSubloadWithRename(String className, String mapName) {
        return mapName != null && !mapName.isEmpty() && !mapName.equals(className);
    }

    public static class Filter implements FilteringClassLoader.Filter {
        private SubLoader loader;

        @Override
        public boolean canLoad(String className) {
            return !loader.subLoadRevMap.containsKey(className);
        }
    }
}
