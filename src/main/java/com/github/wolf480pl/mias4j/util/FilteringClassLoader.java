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

public class FilteringClassLoader extends ClassLoader {
    private final Filter filter;

    public FilteringClassLoader(Filter filter) {
        this.filter = filter;
    }

    public FilteringClassLoader(Filter filter, ClassLoader parent) {
        super(parent);
        this.filter = filter;
    }

    @Override
    public Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        if (filter.canLoad(name)) {
            return super.loadClass(name, resolve);
        }
        throw new ClassNotFoundException("" + name + " - filtered out."); // will not NPE for null name
    }

    public static interface Filter {
        boolean canLoad(String className);
    }

}
