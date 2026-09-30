package com.sprint.framework.util;

import java.io.File;
import java.net.JarURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public final class UtilAnalyser {
    private UtilAnalyser() {
    }

    public static Set<Class<?>> findClasses(String packageName) {
        Set<Class<?>> classes = new LinkedHashSet<Class<?>>();
        String packagePath = packageName.replace('.', '/');
        try {
            Enumeration<URL> resources = Thread.currentThread().getContextClassLoader().getResources(packagePath);
            while (resources.hasMoreElements()) {
                URL resource = resources.nextElement();
                if ("file".equals(resource.getProtocol())) {
                    scanDirectory(packageName, new File(new URI(resource.toString())), classes);
                } else if ("jar".equals(resource.getProtocol())) {
                    scanJar(packageName, ((JarURLConnection) resource.openConnection()).getJarFile(), classes);
                }
            }
            return classes;
        } catch (Exception exception) {
            throw new IllegalStateException("Impossible d'analyser le package " + packageName, exception);
        }
    }

    private static void scanDirectory(String packageName, File directory, Set<Class<?>> classes)
            throws ClassNotFoundException {
        File[] files = directory.listFiles();
        if (files == null) {
            return;
        }
        for (File file : files) {
            if (file.isDirectory()) {
                scanDirectory(packageName + "." + file.getName(), file, classes);
            } else if (file.getName().endsWith(".class") && !file.getName().contains("$")) {
                classes.add(Class.forName(packageName + '.' + file.getName().substring(0, file.getName().length() - 6)));
            }
        }
    }

    private static void scanJar(String packageName, JarFile jar, Set<Class<?>> classes)
            throws ClassNotFoundException {
        String prefix = packageName.replace('.', '/') + "/";
        Enumeration<JarEntry> entries = jar.entries();
        while (entries.hasMoreElements()) {
            String name = entries.nextElement().getName();
            if (name.startsWith(prefix) && name.endsWith(".class") && !name.contains("$")) {
                classes.add(Class.forName(name.substring(0, name.length() - 6).replace('/', '.')));
            }
        }
    }
}