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

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import com.github.wolf480pl.sandbox.core.rewrite.RewritePolicy;

public class Rewrite {

    public static void main(String[] args) throws IOException {
        boolean bypass = false;
        if (args.length > 2) {
            bypass = args[2].equalsIgnoreCase("true");
        }
        FileInputStream fis = new FileInputStream(args[0]);
        Transformer t = bypass ? new Transformer(RewritePolicy.NEVER_INTERCEPT) : new Transformer();
        FileOutputStream fos = new FileOutputStream(args[1]);
        fos.write(t.transform("", fis));
        fis.close();
        fos.flush();
        fos.close();
    }

}
