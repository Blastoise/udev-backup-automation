package io.ashutosh;

import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiPredicate;

public class PCloudBackup {

    private static PCloudBackup pCloudBackup = null;
    private final WebDriver webDriver;
    private final Path srcBasePath;
    private final String username;
    private final String password;


    private PCloudBackup(String srcBasePath, String username, String password) {
        ChromeOptions chromeOptions = new ChromeOptions();
        chromeOptions.setPageLoadStrategy(PageLoadStrategy.EAGER);
        chromeOptions.addArguments("--disable-notifications");
        chromeOptions.addArguments("--disable-infobars");

        this.webDriver = new ChromeDriver(chromeOptions);
        this.webDriver.manage().window().maximize();

        this.srcBasePath = Paths.get(srcBasePath);
        this.username = username;
        this.password = password;
    }

    public static PCloudBackup getInstance(String srcBasePath, String username, String password) {
        if (pCloudBackup == null) pCloudBackup = new PCloudBackup(srcBasePath, username, password);
        return pCloudBackup;
    }

    private WebElement waitFor(Duration timeout, By locator, BiPredicate<WebDriver, By> execFn) {
        WebDriverWait wait = new WebDriverWait(this.webDriver, timeout);
        try {
            wait.until(driver -> execFn.test(driver, locator));
            return this.webDriver.findElement(locator);
        } catch (TimeoutException ex) {
            System.out.println(ex.getMessage());
            throw new TimeoutException("Time out waiting for element with locator: " + locator);
        }
    }


    private void fileUpload(Duration timeout, List<String> filePathList) {
        StringBuilder sb = new StringBuilder();
        for (String file : filePathList) sb.append(file).append("\n");
        sb.delete(sb.length() - 1, sb.length());

        WebElement uploadFile = waitFor(timeout, By.xpath("//input[@type='file' and @multiple='']"), Utils.elementPresent(false));
        uploadFile.sendKeys(sb.toString());

        // Replace all files in cloud regardless of timestamp of items(pop up appears instantly):
        try {
            WebElement applyForAllCheckBox = waitFor(timeout, By.xpath("//div[contains(@class,'InputStyled') and contains(text(),'Apply for all')]"), Utils.elementPresent(true));
            Utils.jsClick(this.webDriver, applyForAllCheckBox);

            WebElement continueBtn = waitFor(timeout, By.xpath("//a[@color='cyan']/span[contains(text(),'Continue')]"), Utils.elementPresent(true));
            Utils.jsClick(this.webDriver, continueBtn);

        } catch (Exception e) {
            System.out.println("All files are new");
        }
    }

    // uploads one folder at a time
    private void folderUpload(Duration timeout, String folderPath) {
        WebElement uploadFile = waitFor(timeout, By.xpath("//input[@type='file' and @webkitdirectory='true']"), Utils.elementPresent(false));
        uploadFile.sendKeys(folderPath);

        // Replace folder in cloud regardless of last modified time of items:
        try {
            WebElement continueBtn = waitFor(timeout, By.xpath("//a[@color='cyan']/span[contains(text(),'Continue')]"), Utils.elementPresent(true));
            Utils.jsClick(this.webDriver, continueBtn);
        } catch (Exception e) {
            System.out.println("Folder is new");
        }
    }

    private void uploadChecker(Duration timeout, Long cntOfAssets) {
        WebElement allItemsBtn = waitFor(timeout, By.xpath("//*[@id='upload-manager-container']//label[contains(text(),'All items')]"), Utils.elementPresent(true));
        Utils.jsClick(this.webDriver, allItemsBtn);

        waitFor(timeout, By.xpath("//*[@id='upload-manager-container']//p[contains(text(),'Completed')]/following-sibling::p"), Utils.uploadComplete(cntOfAssets));
        System.out.println("Successfully uploaded files and folders!!!");
    }

    private void login() {
        this.webDriver.navigate().to("https://www.pcloud.com/");

        WebElement email = waitFor(Duration.ofSeconds(50), By.name("email"), Utils.elementPresent(true));
        email.sendKeys(this.username);

        WebElement nxtButton = waitFor(Duration.ofSeconds(50), By.cssSelector("button.butt.submitbut"), Utils.elementPresent(true));
        Utils.jsClick(this.webDriver, nxtButton);

        WebElement password = waitFor(Duration.ofSeconds(50), By.name("password"), Utils.elementPresent(true));
        password.sendKeys(this.password);

        nxtButton = waitFor(Duration.ofSeconds(50), By.cssSelector("button.butt.submitbut"), Utils.elementPresent(true));
        Utils.jsClick(this.webDriver, nxtButton);
    }

    public void backupAssets(String[] assets) {
        this.login();

        List<String> filePathList = new ArrayList<>();
        List<String> folderPathList = new ArrayList<>();
        long cntofAssets = 0L;

        for (String asset : assets) {
            Path assetPath = this.srcBasePath.resolve(asset);
            if (Files.exists(assetPath)) {
                cntofAssets++;
                if (Files.isDirectory(assetPath)) folderPathList.add(assetPath.toString());
                else filePathList.add(assetPath.toString());
            } else {
                System.out.println("File/Folder does not exist: " + assetPath);
            }
        }
        fileUpload(Duration.ofSeconds(50), filePathList);
        folderPathList.forEach(folderPath -> folderUpload(Duration.ofSeconds(50), folderPath));
        uploadChecker(Duration.ofMinutes(5), cntofAssets);
    }
}
