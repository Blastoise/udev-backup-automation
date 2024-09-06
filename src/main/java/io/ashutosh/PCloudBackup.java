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
            throw new TimeoutException("Timed out waiting for element with locator: " + locator);
        }
    }


    private void fileUpload(Duration timeout, List<String> filePathList) {
        StringBuilder sb = new StringBuilder();
        for (String file : filePathList) {
            sb.append(file).append("\n");
        }

        sb.delete(sb.length() - 1, sb.length());

        WebElement uploadFile = waitFor(timeout, By.xpath("//input[@type='file' and @multiple='']"), Utils.elementPresent(false));
        uploadFile.sendKeys(sb.toString());

        // Replace all files in cloud regardless of timestamp of items(pop up appears instantly):
        try {
            WebElement applyForAllCheckBox = waitFor(Duration.ofSeconds(5), By.xpath("//div[contains(@class,'InputStyled') and contains(text(),'Apply for all')]"), Utils.elementPresent(true));
            applyForAllCheckBox.click();

            WebElement continueBtn = waitFor(Duration.ofSeconds(6), By.xpath("//a[@color='cyan']/span[contains(text(),'Continue')]"), Utils.elementPresent(true));
            continueBtn.click();
        } catch (Exception e) {
            System.out.println("All files are unique");
        }
    }

    // uploads one folder at a time
    private void folderUpload(Duration timeout, String folderPath, String folderName) {
        WebElement uploadFile = waitFor(timeout, By.xpath("//input[@type='file' and @webkitdirectory='true']"), Utils.elementPresent(false));
        uploadFile.sendKeys(folderPath);

        // Replace folder in cloud regardless of last modified time of items:
        try {
            waitFor(Duration.ofSeconds(10), By.xpath("//div[contains(@class,'UploadOptionsModal')]//span[text()='" + folderName + "']"), Utils.elementPresent(true));
            WebElement applyForAllCheckBox = waitFor(Duration.ofSeconds(10), By.xpath("//div[contains(@class,'InputStyled') and contains(text(),'Apply for all')]"), Utils.elementPresent(true));
            applyForAllCheckBox.click();

            WebElement continueBtn = waitFor(Duration.ofSeconds(11), By.xpath("//a[@color='cyan']/span[contains(text(),'Continue')]"), Utils.elementPresent(true));
            continueBtn.click();
        } catch (Exception e) {
            System.out.println("Folder is new");
        }
    }

    private void uploadChecker(Duration timeout, Long cntOfAssets) {
        WebElement allItemsBtn = waitFor(timeout, By.xpath("//*[@id='upload-manager-container']//label[contains(text(),'All items')]"), Utils.elementPresent(true));
        JavascriptExecutor javascriptExecutor = (JavascriptExecutor) this.webDriver;
        javascriptExecutor.executeScript("arguments[0].click();", allItemsBtn);

        waitFor(timeout, By.xpath("//*[@id='upload-manager-container']//p[contains(text(),'Completed')]/following-sibling::p"), Utils.uploadComplete(cntOfAssets));
        System.out.println("Successfully uploaded files and folders!!!");
    }

    private void login() {
        this.webDriver.navigate().to("https://www.pcloud.com/");

        // Close any advertisement pop-up
        try {
            WebElement closeModal = waitFor(Duration.ofSeconds(5), By.xpath("//div[contains(@class, 'ModalClose')]"), Utils.elementPresent(true));
            closeModal.getAttribute("innerHTML");
            closeModal.click();
        } catch (Exception e) {
            System.out.println("No advertisement to close");
        }

        WebElement email = waitFor(Duration.ofSeconds(50), By.name("email"), Utils.elementPresent(true));

        email.sendKeys(this.username);

        WebElement nxtButton = waitFor(Duration.ofSeconds(50), By.cssSelector("button.butt.submitbut"), Utils.elementPresent(true));
        nxtButton.click();


        WebElement password = waitFor(Duration.ofSeconds(50), By.name("password"), Utils.elementPresent(true));
        password.sendKeys(this.password);


        nxtButton = waitFor(Duration.ofSeconds(50), By.cssSelector("button.butt.submitbut"), Utils.elementPresent(true));
        nxtButton.click();
    }

    public void backupAssets(String[] assets) {
        this.login();

        List<String> filePathList = new ArrayList<>();
        long cntofAssets = 0L;

        for (String asset : assets) {
            Path assetPath = this.srcBasePath.resolve(asset);
            if (Files.exists(assetPath)) {
                cntofAssets++;
                if (Files.isDirectory(assetPath))
                    folderUpload(Duration.ofSeconds(50), assetPath.toString(), assetPath.getFileName().toString());
                else filePathList.add(assetPath.toString());
            } else {
                System.out.println("File/Folder does not exist: " + assetPath);
            }
        }
        fileUpload(Duration.ofSeconds(50), filePathList);
        uploadChecker(Duration.ofDays(1L), cntofAssets);
    }
}
