package com.github.wolf480pl.sandbox.util;

import java.io.IOException;
import java.io.InputStream;
import java.net.JarURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.security.CodeSigner;
import java.security.CodeSource;
import java.security.SecureClassLoader;
import java.util.Enumeration;
import java.util.jar.Attributes;
import java.util.jar.Attributes.Name;
import java.util.jar.Manifest;

import org.apache.commons.io.IOUtils;

public abstract class AbstractTransformingClassLoader extends SecureClassLoader {
    private final ClassLoader backend;

    static {
        ClassLoader.registerAsParallelCapable();
    }

    public AbstractTransformingClassLoader(ClassLoader backend) {
        this.backend = backend;
    }

    public AbstractTransformingClassLoader(ClassLoader backend, ClassLoader parent) {
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
        InputStream is;
        URL codeSourceURL = null;
        CodeSigner[] signers = null;
        Manifest man = null;

        try {
            URLConnection conn = res.openConnection();
            is = conn.getInputStream();
            if (conn instanceof JarURLConnection) {
                codeSourceURL = ((JarURLConnection) conn).getJarFileURL();
                try {
                    man = ((JarURLConnection) conn).getManifest();
                } catch (IOException e) {
                    // TODO log
                }
                try {
                    signers = ((JarURLConnection) conn).getJarEntry().getCodeSigners();
                } catch (IOException e) {
                    // TODO log
                }
            }
        } catch (IOException e) {
            throw new ClassNotFoundException(name, e);
        }

        byte[] bytes;
        try {
            bytes = IOUtils.toByteArray(is);
        } catch (IOException e) {
            throw new ClassNotFoundException(name, e);
        }

        boolean guessedCodeSource = false;
        if (codeSourceURL == null) {
            codeSourceURL = guessCodeSourceURL(rname, res);
            guessedCodeSource = true;
        }
        CodeSource cs = new CodeSource(codeSourceURL, signers);
        int lastDot = name.lastIndexOf('.');
        if (lastDot != -1) {
            String pkgName = name.substring(0, lastDot);
            if (getPackage(pkgName) == null) {
                if (man != null) {
                    // If we guess wrong code source URL, we don't want to accidentally seal a package with that URL.
                    definePackage(pkgName, man, guessedCodeSource ? null : codeSourceURL);
                } else {
                    definePackage(pkgName, null, null, null, null, null, null, null);
                }
            }
        }

        bytes = transform(name, bytes, cs);

        Class<?> c = defineClass(name, bytes, 0, bytes.length, cs);
        return c;
    }

    protected Package definePackage(String name, Manifest man, URL url) {
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

        return definePackage(name, specTitle, specVersion, specVendor, implTitle, implVersion, implVendor, sealed.equalsIgnoreCase("true") ? url : null);

    }

    @Override
    protected URL findResource(String name) {
        return backend.getResource(name);

    }

    @Override
    protected Enumeration<URL> findResources(String name) throws IOException {
        return backend.getResources(name);

    }

    /*protected Package copyPackage(Package pkg, CodeSource cs) {
        return definePackage(pkg.getName(), pkg.getSpecificationTitle(), pkg.getSpecificationVersion(), pkg.getSpecificationVendor(), pkg.getImplementationTitle(), pkg.getImplementationVersion(),
                pkg.getImplementationVendor(), pkg.isSealed() ? cs.getLocation() : null);
    }*/

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
            // TODO: log this
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
            // TODO: log this
            return resourceURL;
        }
    }

}
