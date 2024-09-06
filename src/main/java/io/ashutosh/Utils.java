package io.ashutosh;

import org.openqa.selenium.By;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.nio.file.attribute.FileTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.NoSuchElementException;
import java.util.function.BiPredicate;

public class Utils {
    public static boolean fileTimeComparator(FileTime first, FileTime second) {
        ZonedDateTime zdt1 = ZonedDateTime.ofInstant(first.toInstant(), ZoneId.of("Asia/Kolkata")).truncatedTo(ChronoUnit.SECONDS);
        ZonedDateTime zdt2 = ZonedDateTime.ofInstant(second.toInstant(), ZoneId.of("Asia/Kolkata")).truncatedTo(ChronoUnit.SECONDS);
        return zdt1.compareTo(zdt2) != 0;
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
}
