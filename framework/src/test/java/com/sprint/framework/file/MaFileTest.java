package com.sprint.framework.file;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class MaFileTest {

    @Test
    public void exposesFileNameTypeDataAndSize() {
        byte[] data = "bonjour".getBytes(StandardCharsets.UTF_8);
        MaFile file = new MaFile("note.txt", "text/plain", data);

        assertEquals("note.txt", file.getFileName());
        assertEquals("text/plain", file.getFileType());
        assertArrayEquals(data, file.getData());
        assertEquals(data.length, file.getSize());
    }

    @Test
    public void savesToExplicitDirectory() throws Exception {
        Path directory = Files.createTempDirectory("sprint-upload-");
        MaFile file = new MaFile("note.txt", "text/plain", "contenu".getBytes(StandardCharsets.UTF_8));

        file.saveTo(directory.toString());

        Path saved = directory.resolve("note.txt");
        assertTrue(Files.exists(saved));
        assertArrayEquals("contenu".getBytes(StandardCharsets.UTF_8), Files.readAllBytes(saved));
    }

    @Test
    public void savesToConfiguredDefaultDirectory() throws Exception {
        Path directory = Files.createTempDirectory("sprint-default-");
        MaFile file = new MaFile("note.txt", "text/plain",
                "contenu".getBytes(StandardCharsets.UTF_8), directory.toString());

        file.save();

        assertTrue(Files.exists(directory.resolve("note.txt")));
    }

    @Test(expected = java.io.IOException.class)
    public void saveWithoutConfiguredDirectoryFails() throws Exception {
        new MaFile("note.txt", "text/plain", new byte[]{1}).save();
    }
}
