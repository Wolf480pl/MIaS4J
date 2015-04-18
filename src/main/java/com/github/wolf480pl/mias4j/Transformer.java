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
package com.github.wolf480pl.mias4j;

import java.io.IOException;
import java.io.InputStream;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;

import com.github.wolf480pl.mias4j.core.rewrite.BlindPolicy;
import com.github.wolf480pl.mias4j.core.rewrite.RewritePolicy;
import com.github.wolf480pl.mias4j.core.rewrite.SandboxAdapter;
import com.github.wolf480pl.mias4j.java8.rewrite.WrapperDynamicRewritePolicy;

public class Transformer {
    private final RewritePolicy policy;

    public Transformer() {
        this(wrapIfJava8(BlindPolicy.ALWAYS_INTERCEPT));
    }

    public Transformer(RewritePolicy policy) {
        this.policy = policy;
    }

    public byte[] transfrom(String name, byte[] data) {
        return transform(name, new ClassReader(data)).toByteArray();
    }

    public byte[] transform(String name, InputStream is) throws IOException {
        return transform(name, new ClassReader(is)).toByteArray();
    }

    protected ClassWriter transform(String name, ClassReader reader) {
        ClassWriter writer = new ClassWriter(reader, ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
        ClassVisitor visitor = new SandboxAdapter(writer, policy);
        ClassVisitor checker = new org.objectweb.asm.util.CheckClassAdapter(visitor);
        reader.accept(checker, ClassReader.SKIP_DEBUG | ClassReader.EXPAND_FRAMES);
        return writer;
    }

    public static RewritePolicy wrapIfJava8(RewritePolicy policy) {
        if (System.getProperty("java.vm.specification.version").equals("1.8")) {
            // It's JVM 8
            return new WrapperDynamicRewritePolicy(policy);
        }
        return policy;
    }
}
