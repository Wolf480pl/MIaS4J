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

import org.objectweb.asm.Type;

public interface RewritePolicy {

    boolean shouldIntercept(Type caller, InvocationType type, Type owner, String name, Type desc) throws RewriteAbortException;

    public static class BlindPolicy implements RewritePolicy {
        private final boolean should;

        public BlindPolicy(boolean should) {
            this.should = should;
        }

        @Override
        public boolean shouldIntercept(Type caller, InvocationType type, Type owner, String name, Type desc) throws RewriteAbortException {
            return should;
        }
    }

    public static final RewritePolicy ALWAYS_INTERCEPT = new BlindPolicy(true);
    public static final RewritePolicy NEVER_INTERCEPT = new BlindPolicy(false);
}
