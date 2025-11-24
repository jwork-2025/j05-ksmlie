package com.gameengine.recording;

import java.util.List;

public interface RecordingStorage {
    void openForWrite(String name) throws Exception;
    void appendLine(String line) throws Exception;
    void flush() throws Exception;
    void closeWrite() throws Exception;
    List<String> listRecordings() throws Exception;
    List<String> readAll(String name) throws Exception;
}