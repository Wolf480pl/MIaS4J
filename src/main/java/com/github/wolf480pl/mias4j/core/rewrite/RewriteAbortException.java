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
package com.github.wolf480pl.mias4j.core.rewrite;

public class RewriteAbortException extends Exception {
    private static final long serialVersionUID = 72081484204385874L;

    public RewriteAbortException() {
    }

    public RewriteAbortException(String message) {
        super(message);
    }

    public RewriteAbortException(Throwable cause) {
        super(cause);
    }

    public RewriteAbortException(String message, Throwable cause) {
        super(message, cause);
    }
}
