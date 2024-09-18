package io.ashutosh;

import org.openqa.selenium.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.function.BiPredicate;
import java.util.stream.IntStream;
import java.util.stream.LongStream;

public class Utils {
    private static final Logger logger = LoggerFactory.getLogger(Utils.class);

    public static boolean fileTimeComparator(FileTime first, FileTime second) {
        ZonedDateTime zdt1 = ZonedDateTime.ofInstant(first.toInstant(), ZoneId.of("Asia/Kolkata")).truncatedTo(ChronoUnit.SECONDS);
        ZonedDateTime zdt2 = ZonedDateTime.ofInstant(second.toInstant(), ZoneId.of("Asia/Kolkata")).truncatedTo(ChronoUnit.SECONDS);
        return zdt1.compareTo(zdt2) != 0;
    }

    public static void jsClick(WebDriver webDriver, WebElement element) {
        JavascriptExecutor javascriptExecutor = (JavascriptExecutor) webDriver;
        javascriptExecutor.executeScript("arguments[0].click();", element);
    }

    public static BiPredicate<WebDriver, By> elementPresent(boolean displayed) {
        return (webDriver, locator) -> {
            try {
                WebElement webElement = webDriver.findElement(locator);
                if (displayed) return webElement.isDisplayed();
                return true;
            } catch (NoSuchElementException | StaleElementReferenceException ex) {
                return false;
            }
        };
    }

    public static BiPredicate<WebDriver, By> uploadComplete(Long cntOfAssets) {
        return (webDriver, locator) -> {
            try {
                WebElement webElement = webDriver.findElement(locator);
                return Long.valueOf(webElement.getText()).equals(cntOfAssets);
            } catch (NoSuchElementException ex) {
                return false;
            }
        };
    }

    public static boolean generateVideoFromFrames(String frameNamePrefix, String tmpDir, String outputDir, String videoFileName) {
        ProcessBuilder processBuilder = new ProcessBuilder(
                "ffmpeg",
                "-y",
                "-loglevel", "quiet",
                "-framerate", "30",
                "-i", tmpDir + "/" + frameNamePrefix + "%d.png",
                "-c:v", "libx264",
                "-vf", "pad=ceil(iw/2)*2:ceil(ih/2)*2",
                "-pix_fmt", "yuv420p",
                outputDir + "/" + videoFileName);
        processBuilder.redirectErrorStream(true);
        try {
            Process process = processBuilder.start();
            return process.waitFor() == 0;
        } catch (IOException e) {
            logger.error("Something went wrong while starting the process to generate video from frames", e);
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            logger.error("Something went wrong while waiting for the video generation process to complete", e);
            throw new RuntimeException(e);
        }
    }

    public static void deleteFrames(String frameNamePrefix, String tmpDir, Long maxFrameCount) {
        LongStream.range(0, maxFrameCount).forEach(frameNum -> {
            try {
                Files.deleteIfExists(Paths.get(tmpDir).toAbsolutePath().resolve(frameNamePrefix + frameNum + ".png"));
            } catch (IOException e) {
                logger.error("Enable to delete frames", e);
                throw new RuntimeException(e);
            }
        });
    }

    public static String messageTextBuilder(List<String> copiedFiles, List<String> failedPaths, boolean isFailed) {
        StringBuilder text = new StringBuilder();

        if (isFailed) text.append("<b>Backup Failed</b> ❌\n");
        else if (!failedPaths.isEmpty()) text.append("<b>Backup Partially Successful</b> ✅\n");
        else text.append("<b>Backup Successful</b> ✅\n");

        // For failed:
        //          1. List of files copied if present or nothing
        //          2. Failed Paths
        // For partial successful:
        //          1. List of files copied if present or all files are up to date
        //          2. Failed Paths
        // For successful:
        //          1. List of files copied if present or all files are up to date

        if (!copiedFiles.isEmpty()) {
            text.append("\n");
            text.append("<b><i>List of files that were backed up in Hard Disk:</i></b>\n");
            IntStream.range(0, copiedFiles.size()).forEach(idx -> text.append(idx + 1).append(". ").append(copiedFiles.get(idx)).append("\n"));
        }

        if (copiedFiles.isEmpty() && !isFailed) {
            text.append("\n");
            text.append("<b><i>All files are already up to date!</i></b>\n");
        }

        if (!failedPaths.isEmpty()) {
            text.append("\n");
            text.append("<b><i>List of paths that require attention:</i></b>\n");
            IntStream.range(0, failedPaths.size()).forEach(idx -> text.append(idx + 1).append(". ").append(failedPaths.get(idx)).append("\n"));
        }
        return text.toString();
    }

}
