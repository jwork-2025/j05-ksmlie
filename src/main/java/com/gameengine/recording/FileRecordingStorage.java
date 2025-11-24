package com.gameengine.recording;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class FileRecordingStorage implements RecordingStorage {
    private final File dir;
    private BufferedWriter writer;
    private File currentFile;

    public FileRecordingStorage() {
        this.dir = new File("recordings");
        if (!dir.exists())
            dir.mkdirs();
    }

    @Override
    public void openForWrite(String name) throws Exception {
        String safe = name.replaceAll("[^a-zA-Z0-9_-]", "_");
        this.currentFile = new File(dir, safe + ".jsonl");
        this.writer = new BufferedWriter(new FileWriter(currentFile, StandardCharsets.UTF_8, false));
    }

    @Override
    public void appendLine(String line) throws Exception {
        if (writer == null)
            throw new IllegalStateException("writer not opened");
        writer.write(line);
        writer.write("\n");
    }

    @Override
    public void flush() throws Exception {
        if (writer != null)
            writer.flush();
    }

    @Override
    public void closeWrite() throws Exception {
        if (writer != null) {
            try {
                writer.flush();
            } catch (Exception ignored) {
            }
            try {
                writer.close();
            } catch (Exception ignored) {
            }
            writer = null;
        }
        currentFile = null;
    }

    @Override
    public List<String> listRecordings() throws Exception {
        File[] files = dir.listFiles((d, n) -> n.endsWith(".jsonl"));
        if (files == null)
            return Collections.emptyList();
        List<String> names = new ArrayList<>();
        for (File f : files)
            names.add(f.getName());
        return names;
    }

    @Override
    public List<String> readAll(String name) throws Exception {
        File f = name.endsWith(".jsonl") ? new File(dir, name) : new File(dir, name + ".jsonl");
        if (!f.exists())
            return Collections.emptyList();
        List<String> lines = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(f, StandardCharsets.UTF_8))) {
            String s;
            while ((s = br.readLine()) != null)
                lines.add(s);
        }
        return lines;
    }
}