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
package com.github.wolf480pl.sandbox.java8.rewrite;

import org.objectweb.asm.Type;

import com.github.wolf480pl.sandbox.core.InvocationType;
import com.github.wolf480pl.sandbox.core.rewrite.RewriteAbortException;
import com.github.wolf480pl.sandbox.core.rewrite.RewritePolicy;

public class WrapperDynamicRewritePolicy extends AbstractDynamicRewritePolicy {
    private final RewritePolicy policy;

    public WrapperDynamicRewritePolicy(RewritePolicy policy) {
        this.policy = policy;
    }

    @Override
    public boolean shouldIntercept(Type caller, InvocationType type, Type owner, String name, Type desc) throws RewriteAbortException {
        return policy.shouldIntercept(caller, type, owner, name, desc);
    }

}
