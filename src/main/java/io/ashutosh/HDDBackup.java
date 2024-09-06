package io.ashutosh;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.stream.Collectors;

import static io.ashutosh.Utils.fileTimeComparator;

public class HDDBackup {

    private final Path srcBasePath;
    private final Path destBasePath;

    public HDDBackup(String srcBasePath, String destBasePath) throws IOException {
        this.srcBasePath = Paths.get(srcBasePath);
        Files.createDirectories(this.srcBasePath);
        this.destBasePath = Paths.get(destBasePath);
        Files.createDirectories(this.destBasePath);
    }

    private List<FileInfo> getSrcPathFileList(Path path) throws IOException {
        return Files.walk(path, Integer.MAX_VALUE).map(filePath -> {
            try {
                return new FileInfo(this.srcBasePath.relativize(filePath).toString(), Files.readAttributes(filePath, BasicFileAttributes.class));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }).collect(Collectors.toList());
    }

    private void copyNewOrAlteredFiles(Path path) throws IOException {
        List<FileInfo> fileInfoList = getSrcPathFileList(path);

        for (FileInfo fileInfo : fileInfoList) {
            Path srcFilePath = this.srcBasePath.resolve(fileInfo.fileName);
            Path destFilePath = this.destBasePath.resolve(fileInfo.fileName);
            if (Files.exists(destFilePath)) {
                // if file exists then compare last modified time and size
                if (fileInfo.basicFileAttr.isRegularFile()) {
                    BasicFileAttributes destFileAttr = Files.readAttributes(destFilePath, BasicFileAttributes.class);

                    if (fileTimeComparator(destFileAttr.lastModifiedTime(), fileInfo.basicFileAttr.lastModifiedTime()) || destFileAttr.size() != fileInfo.basicFileAttr.size()) {
                        Files.copy(srcFilePath, destFilePath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES);
                    }
                }
            } else {
                // If file or folder does not exist then copy that file or copy empty folder
                Files.copy(srcFilePath, destFilePath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES);
            }
        }
    }

    public void backupAssets(String[] assets) throws IOException {
        for (String asset: assets) copyNewOrAlteredFiles(this.srcBasePath.resolve(asset));
    }
}