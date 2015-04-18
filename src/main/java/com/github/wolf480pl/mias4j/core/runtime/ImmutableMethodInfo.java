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

import java.lang.invoke.MethodType;

import com.github.wolf480pl.mias4j.core.InvocationType;

public class ImmutableMethodInfo implements MethodInfo {
    private final InvocationType invType;
    private final String owner;
    private final String name;
    private final MethodType methodType;

    public ImmutableMethodInfo(InvocationType invType, String owner, String name, MethodType methType) {
        this.invType = invType;
        this.owner = owner;
        this.name = name;
        this.methodType = methType;
    }

    @Override
    public InvocationType getInvocationType() {
        return invType;
    }

    @Override
    public String getOwner() {
        return owner;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public MethodType getMethodType() {
        return methodType;
    }

}
