package io.ashutosh;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.ashutosh.Utils.fileTimeComparator;

public class HDDBackup {

    private static final Logger logger = LoggerFactory.getLogger(HDDBackup.class);

    private final Path srcBasePath;
    private final Path destBasePath;

    public HDDBackup(String srcBasePath, String destBasePath) {
        this.srcBasePath = Paths.get(srcBasePath);
        this.destBasePath = Paths.get(destBasePath);
    }

    private List<FileInfo> getSrcPathFileList(Path path) {
        try (Stream<Path> files = Files.walk(path, Integer.MAX_VALUE)) {
            return files.map(filePath -> {
                try {
                    return new FileInfo(this.srcBasePath.relativize(filePath).toString(), Files.readAttributes(filePath, BasicFileAttributes.class));
                } catch (IOException e) {
                    // Unlikely that we'll get an exception while reading attributes
                    // But if we get we'll skip the whole path itself by throwing RuntimeException
                    logger.error("Unable to read file attributes for file/folder: {}", filePath, e);
                    throw new RuntimeException(e);
                }
            }).collect(Collectors.toList());
        } catch (IOException e) {
            logger.error("Unable to walk the file tree for the path: {}", path.toString(), e);
            throw new RuntimeException(e);
        }
    }

    private void copyNewOrAlteredFiles(Path path, List<String> copiedFiles) {

        List<FileInfo> fileInfoList = getSrcPathFileList(path);

        // For nested files/directories
        // Parent directories might not be present while copying so creating necessary directories
        Path parent = this.srcBasePath.relativize(path).getParent();
        try {
            if (parent != null) {
                Files.createDirectories(this.destBasePath.resolve(parent));
                logger.info("Created parent directory at destBasePath for the following path: {}", path);
            }
        } catch (IOException e) {
            logger.error("Unable to create parent directory {}", parent, e);
            throw new RuntimeException(e);
        }

        for (FileInfo fileInfo : fileInfoList) {
            Path srcFilePath = this.srcBasePath.resolve(fileInfo.fileName);
            Path destFilePath = this.destBasePath.resolve(fileInfo.fileName);
            if (Files.exists(destFilePath)) {
                // if file exists then compare last modified time and size
                if (fileInfo.basicFileAttr.isRegularFile()) {
                    try {
                        BasicFileAttributes destFileAttr = Files.readAttributes(destFilePath, BasicFileAttributes.class);

                        if (fileTimeComparator(destFileAttr.lastModifiedTime(), fileInfo.basicFileAttr.lastModifiedTime()) || destFileAttr.size() != fileInfo.basicFileAttr.size()) {
                            Files.copy(srcFilePath, destFilePath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES);
                            logger.info("File successfully replaced: {}", destFilePath);
                            copiedFiles.add(fileInfo.fileName);
                        } else logger.info("File exists and is up to date: {}", destFilePath);

                    } catch (IOException e) {
                        logger.error("IOException occurred while reading attributes or while copying for file: {}", destFilePath, e);
                        throw new RuntimeException(e);
                    }
                } else logger.info("Folder already exists: {}", destFilePath);

            } else {
                // If file or folder does not exist then copy that file or copy empty folder
                try {
                    Files.copy(srcFilePath, destFilePath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES);
                    logger.info("File/Folder successfully created: {}", destFilePath);
                    if (fileInfo.basicFileAttr.isRegularFile()) copiedFiles.add(fileInfo.fileName);
                } catch (IOException e) {
                    logger.error("Unable to create file/folder with the following path: {}", destFilePath, e);
                    throw new RuntimeException(e);
                }
            }
        }
    }

    public String backupAssets(String[] assets, List<String> copiedFiles, List<String> failedPaths) {
        try {
            if (!Files.exists(this.srcBasePath)) {
                logger.error("srcBasePath does not exists");
                throw new RuntimeException("srcBasePath does not exist");
            }

            Files.createDirectories(this.destBasePath);

            for (String asset : assets) {
                try {
                    copyNewOrAlteredFiles(this.srcBasePath.resolve(asset), copiedFiles);
                } catch (RuntimeException e) {
                    failedPaths.add(this.srcBasePath.resolve(asset).toString());
                    logger.info("Gracefully handled RuntimeException for path: {}", this.srcBasePath.resolve(asset));
                }
            }

            return Utils.messageTextBuilder(copiedFiles, failedPaths, failedPaths.size() == assets.length);

        } catch (IOException e) {
            String text = "<b>Backup Failed</b> ❌\n\n" +
                    "<b><i>Error occurred while creating folder " + destBasePath + "</i></b>\n";
            logger.error("Error while creating destBasePath", e);
            logger.warn("Hard Disk backup skipped");
            return text;
        }
    }
}