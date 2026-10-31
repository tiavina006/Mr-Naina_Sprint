package com.sprint.framework.file;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class MaFile {
    private final String fileName;
    private final String fileType;
    private final byte[] data;
    private final long size;
    private final String defaultDirectory;

    public MaFile(String fileName, String fileType, byte[] data) {
        this(fileName, fileType, data, null);
    }

    public MaFile(String fileName, String fileType, byte[] data, String defaultDirectory) {
        this.fileName = fileName;
        this.fileType = fileType;
        this.data = data;
        this.size = data == null ? 0 : data.length;
        this.defaultDirectory = defaultDirectory;
    }

    public String getFileName() {
        return fileName;
    }

    public String getFileType() {
        return fileType;
    }

    public byte[] getData() {
        return data;
    }

    public long getSize() {
        return size;
    }

    public String getDefaultDirectory() {
        return defaultDirectory;
    }

    public void save() throws IOException {
        if (defaultDirectory == null) {
            throw new IOException("Aucun repertoire de stockage configure (upload.path)");
        }
        saveTo(defaultDirectory);
    }

    public void saveTo(String directory) throws IOException {
        Path dir = Paths.get(directory);
        if (!Files.exists(dir)) {
            Files.createDirectories(dir);
        }
        Path target = dir.resolve(fileName);
        Files.write(target, data);
    }

    public static byte[] readAllBytes(InputStream input) throws IOException {
        java.io.ByteArrayOutputStream buffer = new java.io.ByteArrayOutputStream();
        byte[] chunk = new byte[8192];
        int read;
        while ((read = input.read(chunk)) != -1) {
            buffer.write(chunk, 0, read);
        }
        return buffer.toByteArray();
    }
}
