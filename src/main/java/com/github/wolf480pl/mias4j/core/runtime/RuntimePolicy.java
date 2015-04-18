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

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles.Lookup;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public interface RuntimePolicy {

    // TODO: Maybe it should be allowed to throw something?
    MethodHandle intercept(Lookup caller, MethodHandlePrototype method);

    MethodHandle interceptSuperInitArgs(Lookup caller, MethodInfo method, MethodHandle handle);

    MethodHandle interceptSuperInitResult(Lookup caller, MethodInfo method, MethodHandle handle);

    // Useful implementations
    // TODO: Move these elsewhere
    public static class PassthruPolicy implements RuntimePolicy {
        @Override
        public MethodHandle intercept(Lookup caller, MethodHandlePrototype method) {
            try {
                return method.bake(caller);
            } catch (NoSuchMethodException | IllegalAccessException | ClassNotFoundException e) {
                // TODO: are we sure this is the correct handling?
                throw new RuntimeException(e);
            }
        }

        @Override
        public MethodHandle interceptSuperInitArgs(Lookup caller, MethodInfo method, MethodHandle handle) {
            return handle;
        }

        @Override
        public MethodHandle interceptSuperInitResult(Lookup caller, MethodInfo method, MethodHandle handle) {
            return handle;
        }
    }

    public static class LoggingPolicy implements RuntimePolicy {
        private final RuntimePolicy pol;
        private final Logger log;

        public LoggingPolicy(RuntimePolicy pol) {
            this(LoggerFactory.getLogger("LoggingPolicy"), pol);
        }

        public LoggingPolicy(Logger logger, RuntimePolicy pol) {
            this.pol = pol;
            this.log = logger;
        }

        @Override
        public MethodHandle intercept(Lookup caller, MethodHandlePrototype method) {
            log.info(caller + " wants " + method.getOwner() + "." + method.getName() + " " + method.getMethodType());
            return pol.intercept(caller, method);
        }

        @Override
        public MethodHandle interceptSuperInitArgs(Lookup caller, MethodInfo method, MethodHandle handle) {
            log.info(caller + " wants super " + method.getOwner() + "." + method.getName() + " " + method.getMethodType());
            return pol.interceptSuperInitArgs(caller, method, handle);
        }

        @Override
        public MethodHandle interceptSuperInitResult(Lookup caller, MethodInfo method, MethodHandle handle) {
            log.info(caller + " after super " + method.getOwner() + "." + method.getName() + " " + method.getMethodType());
            return pol.interceptSuperInitResult(caller, method, handle);
        }
    }
}
