package de.tum.cit.ui;

import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
/**
 * Resolve classpath resources (files or directories) to filesystem Paths.
 * Works in IDE (exploded) and packaged JAR modes. JAR resources are copied
 * to a temp directory because you cannot write inside a JAR.
 */
public final class Resolver {

    private Resolver() {}

    private static final ClassLoader CL = MainApp.class.getClassLoader();

    /* ------------------------------------------------ FILE ------------------------------------- */

    public static Path resolveClassPathFile(String text) throws IOException {
        if (!text.startsWith("classpath:")) {
            return Paths.get(text);    // assume filesystem path
        }
        String resource = text.substring("classpath:".length());
        URL url = CL.getResource(resource);
        if (url == null) {
            throw new FileNotFoundException("Classpath resource not found: " + resource);
        }
        switch (url.getProtocol()) {
            case "file" -> {
                try {
                    return Paths.get(url.toURI());  // direct
                } catch (URISyntaxException e) {
                    throw new IOException(e);
                }
            }
            case "jar" -> {
                try (InputStream in = CL.getResourceAsStream(resource)) {
                    if (in == null) throw new FileNotFoundException(resource);
                    Path tmpDir = Files.createTempDirectory("ares-cp-file");
                    Path dest   = tmpDir.resolve(Paths.get(resource).getFileName());
                    Files.copy(in, dest, StandardCopyOption.REPLACE_EXISTING);
                    dest.toFile().deleteOnExit();
                    return dest;
                }
            }
            default -> throw new IOException("Unsupported URL protocol: " + url);
        }
    }

    /* ------------------------------------------------ DIRECTORY -------------------------------- */

    public static Path resolveClassPathDirectory(String text) throws IOException {
        if (!text.startsWith("classpath:")) {
            return Paths.get(text);
        }
        String resourceDir = text.substring("classpath:".length());
        URL url = CL.getResource(resourceDir);
        if (url == null) {
            throw new FileNotFoundException("Classpath dir not found: " + resourceDir);
        }
        switch (url.getProtocol()) {
            case "file" -> {
                try {
                    return Paths.get(url.toURI());   // direct
                } catch (URISyntaxException e) {
                    throw new IOException(e);
                }
            }
            default -> throw new IOException("Unsupported URL protocol: " + url);
        }
    }



    private static void copyJarEntry(JarFile jar, JarEntry entry,
                                     String dirPrefix, Path destRoot) {
        try {
            String rel = entry.getName().substring(dirPrefix.length());
            if (rel.isEmpty()) return;  // top dir itself
            Path dest = destRoot.resolve(rel);
            if (entry.isDirectory()) {
                Files.createDirectories(dest);
            } else {
                Files.createDirectories(dest.getParent());
                try (InputStream in = jar.getInputStream(entry)) {
                    Files.copy(in, dest, StandardCopyOption.REPLACE_EXISTING);
                }
            }
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }
}
