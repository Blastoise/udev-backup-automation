package io.ashutosh;

import org.openqa.selenium.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.NoSuchElementException;
import java.util.function.BiPredicate;
import java.util.stream.LongStream;

public class Utils {
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

    public static boolean generateVideoFromFrames(String frameNameSuffix, String videoFileName) throws IOException, InterruptedException {
        ProcessBuilder processBuilder = new ProcessBuilder(
                "ffmpeg",
                "-y",
                "-loglevel", "quiet",
                "-framerate", "15",
                "-i", frameNameSuffix+"%d.png",
                "-c:v", "libx264",
                "-pix_fmt", "yuv420p",
                videoFileName);
        processBuilder.redirectErrorStream(true);

        Process process = processBuilder.start();
        return process.waitFor()==0;
    }

    public static void deleteFrames(Long maxFrameCount) {
        LongStream.range(0, maxFrameCount).forEach(frameNum -> {
            try {
                Files.deleteIfExists(Paths.get("").toAbsolutePath().resolve("frame-" + frameNum + ".png"));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }
}
