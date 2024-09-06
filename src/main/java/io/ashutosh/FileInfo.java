package io.ashutosh;

import java.nio.file.attribute.BasicFileAttributes;

public class FileInfo {

    String fileName;
    BasicFileAttributes basicFileAttr;

    public FileInfo(String fileName, BasicFileAttributes basicFileAttr) {
        this.fileName = fileName;
        this.basicFileAttr = basicFileAttr;
    }

    @Override
    public String toString() {
        return "FileInfo{" +
                "fileName='" + fileName + '\'' +
                ", basicFileAttr=" + basicFileAttr +
                '}';
    }
}
