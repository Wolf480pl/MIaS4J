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
