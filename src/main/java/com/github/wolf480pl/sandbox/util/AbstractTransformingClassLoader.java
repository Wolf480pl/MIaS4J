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
package com.github.wolf480pl.sandbox.util;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.JarURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.security.CodeSigner;
import java.security.CodeSource;
import java.security.PermissionCollection;
import java.security.SecureClassLoader;
import java.util.Enumeration;
import java.util.jar.Attributes;
import java.util.jar.Attributes.Name;
import java.util.jar.Manifest;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * WARNING: This classloader does NOT guarantee correctness of the CodeSource and ProtectionDomain of the loaded classes
 */
public abstract class AbstractTransformingClassLoader extends SecureClassLoader {
    private static final Method getPermsMeth;
    private static final Logger LOG = LoggerFactory.getLogger(AbstractTransformingClassLoader.class);

    private final SecureClassLoader backend;

    static {
        ClassLoader.registerAsParallelCapable();
        Method meth = null;
        try {
            meth = SecureClassLoader.class.getDeclaredMethod("getPermissions", CodeSource.class);
            meth.setAccessible(true);
        } catch (NoSuchMethodException | SecurityException e) {
            LOG.error("Couldn't get Method object for SecureClassLoader.getPermissions()", e);
        }
        getPermsMeth = meth;
    }

    public AbstractTransformingClassLoader(SecureClassLoader backend) {
        this.backend = backend;
    }

    public AbstractTransformingClassLoader(SecureClassLoader backend, ClassLoader parent) {
        super(parent);
        this.backend = backend;
    }

    protected abstract byte[] transform(String name, byte[] bytes, CodeSource cs);

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        final String iname = toInternalName(name);
        final String rname = iname + ".class";
        URL res = backend.getResource(rname);
        if (res == null) {
            throw new ClassNotFoundException(name + ": no " + rname);
        }

        URLConnection conn;
        InputStream is;
        try {
            conn = res.openConnection();
            is = conn.getInputStream();
        } catch (IOException e) {
            throw new ClassNotFoundException(name, e);
        }

        byte[] bytes;
        try {
            bytes = IOUtils.toByteArray(is);
        } catch (IOException e) {
            throw new ClassNotFoundException(name, e);
        }

        CodeSource cs = getCodeSourceAndDefinePackage(conn, rname, name);

        bytes = transform(name, bytes, cs);

        Class<?> c = defineClass(name, bytes, 0, bytes.length, cs);
        return c;
    }

    protected CodeSource getCodeSourceAndDefinePackage(URLConnection resourceConn, String resourceName, String className) {
        URL codeSourceURL = null;
        CodeSigner[] signers = null;
        Manifest man = null;

        if (resourceConn instanceof JarURLConnection) {
            codeSourceURL = ((JarURLConnection) resourceConn).getJarFileURL();
            try {
                man = ((JarURLConnection) resourceConn).getManifest();
            } catch (IOException e) {
                LOG.warn("Couldn't get jar manifest", e);
            }
            try {
                signers = ((JarURLConnection) resourceConn).getJarEntry().getCodeSigners();
            } catch (IOException e) {
                LOG.warn("Couldn't get jar signers", e);
            }
        }

        boolean guessedCodeSource = false;
        if (codeSourceURL == null) {
            codeSourceURL = guessCodeSourceURL(resourceName, resourceConn.getURL());
            guessedCodeSource = true;
        }
        CodeSource cs = new CodeSource(codeSourceURL, signers);

        // If we guess wrong code source URL, we don't want to accidentally seal a package with that URL.
        definePackageIfNotExists(className, man, guessedCodeSource ? null : codeSourceURL);

        // TODO: Do we really want to assign codesource permissions and protection domain based on a guessed CodeSource?
        return cs;
    }

    protected Package definePackageIfNotExists(String className, Manifest man, URL codeSourceURL) {
        int lastDot = className.lastIndexOf('.');
        if (lastDot != -1) {
            String pkgName = className.substring(0, lastDot);
            if (getPackage(pkgName) == null) {
                if (man != null) {
                    return definePackage(pkgName, man, codeSourceURL);
                } else {
                    return definePackage(pkgName, null, null, null, null, null, null, null);
                }
            }
        }
        return null;
    }

    protected Package definePackage(String name, Manifest man, URL codeSourceURL) {
        String path = toInternalName(name) + "/";
        Attributes attr = man.getAttributes(path);
        String specTitle = null;
        String specVersion = null;
        String specVendor = null;
        String implTitle = null;
        String implVersion = null;
        String implVendor = null;
        String sealed = null;

        if (attr != null) {
            specTitle = attr.getValue(Name.SPECIFICATION_TITLE);
            specVersion = attr.getValue(Name.SPECIFICATION_VERSION);
            specVendor = attr.getValue(Name.SPECIFICATION_VENDOR);
            implTitle = attr.getValue(Name.IMPLEMENTATION_TITLE);
            implVersion = attr.getValue(Name.IMPLEMENTATION_VERSION);
            implVendor = attr.getValue(Name.IMPLEMENTATION_VENDOR);
            sealed = attr.getValue(Name.SEALED);
        }

        attr = man.getMainAttributes();
        if (attr != null) {
            if (specTitle != null) {
                specTitle = attr.getValue(Name.SPECIFICATION_TITLE);
            }
            if (specVersion != null) {
                specVersion = attr.getValue(Name.SPECIFICATION_VERSION);
            }
            if (specVendor != null) {
                specVendor = attr.getValue(Name.SPECIFICATION_VENDOR);
            }
            if (implTitle != null) {
                implTitle = attr.getValue(Name.IMPLEMENTATION_TITLE);
            }
            if (implVersion != null) {
                implVersion = attr.getValue(Name.IMPLEMENTATION_VERSION);
            }
            if (implVendor != null) {
                implVendor = attr.getValue(Name.IMPLEMENTATION_VENDOR);
            }
            if (sealed != null) {
                sealed = attr.getValue(Name.SEALED);
            }
        }

        return definePackage(name, specTitle, specVersion, specVendor, implTitle, implVersion, implVendor, sealed.equalsIgnoreCase("true") ? codeSourceURL : null);

    }

    @Override
    protected URL findResource(String name) {
        return backend.getResource(name);

    }

    @Override
    protected Enumeration<URL> findResources(String name) throws IOException {
        return backend.getResources(name);

    }

    @Override
    protected PermissionCollection getPermissions(CodeSource codesource) {
        PermissionCollection pc = super.getPermissions(codesource); // Let SecureClassLoader do its checks, and also use it's return value as a fallback
        if (getPermsMeth != null) {
            try {
                return (PermissionCollection) getPermsMeth.invoke(backend, codesource);
            } catch (IllegalAccessException | InvocationTargetException e) {
                if (e instanceof InvocationTargetException && e.getCause() instanceof RuntimeException) {
                    throw (RuntimeException) e.getCause();
                }
                LOG.error("Exception in call to backend's getPermissions(CodeSource)", e);
            }
        }
        return pc;
    }

    public static String toInternalName(String binaryName) {
        return binaryName.replace('.', '/');
    }

    public static URL guessCodeSourceURL(String resourcePath, URL resourceURL) {
        // FIXME: Find a better way to do this
        @SuppressWarnings("restriction")
        String escaped = sun.net.www.ParseUtil.encodePath(resourcePath, false);
        String path = resourceURL.getPath();
        if (!path.endsWith(escaped)) {
            // Umm... whadda we do now? Maybe let's fallback to full resource URL.
            LOG.warn("Resource URL path \"" + path + "\" doesn't end with escaped resource path \"" + escaped + "\" for resource \"" + resourcePath + "\"");
            return resourceURL;
        }
        path = path.substring(0, path.length() - escaped.length());
        if (path.endsWith("!/")) { // JAR
            path = path.substring(0, path.length() - 2);
        }
        try {
            URI uri = resourceURL.toURI();
            return new URI(uri.getScheme(), uri.getUserInfo(), uri.getHost(), uri.getPort(), path, uri.getQuery(), uri.getFragment()).toURL();
        } catch (MalformedURLException | URISyntaxException e) {
            // Umm... whadda we do now? Maybe let's fallback to full resource URL.
            LOG.warn("Couldn't assemble CodeSource URL with modified path", e);
            return resourceURL;
        }
    }

}
