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

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public class Wrap {

    public static void main(String[] args) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, ClassNotFoundException, MalformedURLException, URISyntaxException {
        String cp = args[0];
        String main = args[1];
        String[] newArgs = Arrays.copyOfRange(args, 2, args.length);

        List<URL> urls = new LinkedList<>();
        for (String s : cp.split(":")) {
            urls.add(new File(s).toURI().toURL());
        }

        Transformer t = null;
        t = new Transformer();
        ClassLoader ldr = new SandboxClassLoader(urls.toArray(new URL[0]), t);
        Class<?> mainClass = ldr.loadClass(main);

        Method mainMethod = mainClass.getDeclaredMethod("main", String[].class);
        mainMethod.invoke(null, (Object) newArgs);
        // MethodHandles.lookup().findStatic(mainClass, "main", MethodType.methodType(Void.class, String[].class)).invoke(newArgs);
        System.out.println("bye");
    }

}
