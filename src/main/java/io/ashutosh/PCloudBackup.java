package io.ashutosh;

import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.devtools.DevTools;
import org.openqa.selenium.devtools.v128.page.Page;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.function.BiPredicate;

public class PCloudBackup {

    private static final Logger logger = LoggerFactory.getLogger(PCloudBackup.class);
    private static PCloudBackup pCloudBackup = null;
    private WebDriver webDriver;
    private DevTools devTools;
    private final Path srcBasePath;
    private final String username;
    private final String password;

    // Prefix for the name of the frames
    private final String frameNamePrefix = "frame-";
    // Count of frames saved
    private Long seqNum = 0L;


    private PCloudBackup(String srcBasePath, String username, String password) {
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
            logger.error("Time out waiting for element with locator: {}", locator, ex);
            throw ex;
        }
    }


    private void fileUpload(Duration timeout, List<String> filePathList) {
        StringBuilder sb = new StringBuilder();
        for (String file : filePathList) sb.append(file).append("\n");
        sb.delete(sb.length() - 1, sb.length());

        logger.info("Starting file uploads");

        WebElement uploadFile = waitFor(timeout, By.xpath("//input[@type='file' and @multiple='']"), Utils.elementPresent(false));
        uploadFile.sendKeys(sb.toString());


        // Replace all files in cloud regardless of timestamp of items(pop up appears instantly):
        try {
            WebElement applyForAllCheckBox = waitFor(Duration.ofSeconds(10), By.xpath("//div[contains(@class,'InputStyled') and contains(text(),'Apply for all')]"), Utils.elementPresent(true));
            Utils.jsClick(this.webDriver, applyForAllCheckBox);

            WebElement continueBtn = waitFor(Duration.ofSeconds(10), By.xpath("//a[@color='cyan']/span[contains(text(),'Continue')]"), Utils.elementPresent(true));
            Utils.jsClick(this.webDriver, continueBtn);

        } catch (Exception e) {
            logger.info("All files being uploaded are new and doesn't exist on pCloud");
        }
    }

    // uploads one folder at a time
    private void folderUpload(Duration timeout, String folderPath) {
        logger.info("Starting folder upload for: {}", folderPath);

        WebElement uploadFile = waitFor(timeout, By.xpath("//input[@type='file' and @webkitdirectory='true']"), Utils.elementPresent(false));
        uploadFile.sendKeys(folderPath);

        // Replace folder in cloud regardless of last modified time of items:
        try {
            WebElement continueBtn = waitFor(Duration.ofSeconds(20), By.xpath("//a[@color='cyan']/span[contains(text(),'Continue')]"), Utils.elementPresent(true));
            Utils.jsClick(this.webDriver, continueBtn);
        } catch (Exception e) {
            logger.info("Folder: {} is new and doesn't exist on pCloud", folderPath);
        }
    }

    private void uploadChecker(Duration timeout, Long cntOfAssets) {
        logger.info("Started monitoring completion of file/folder uploads");

        WebElement allItemsBtn = waitFor(timeout, By.xpath("//*[@id='upload-manager-container']//label[contains(text(),'All items')]"), Utils.elementPresent(true));
        Utils.jsClick(this.webDriver, allItemsBtn);

        waitFor(timeout, By.xpath("//*[@id='upload-manager-container']//p[contains(text(),'Completed')]/following-sibling::p"), Utils.uploadComplete(cntOfAssets));
        logger.info("Successfully uploaded files and folders!!!");
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

        logger.info("Login Successful");
    }

    private void screenRecording(DevTools devTools, String tmpDir) {
        devTools.send(Page.startScreencast(
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty()));

        devTools.addListener(Page.screencastFrame(), screencastFrame -> {
            devTools.send(Page.screencastFrameAck(screencastFrame.getSessionId()));
            byte[] frameImage = Base64.getDecoder().decode(screencastFrame.getData());
            try {
                OutputStream out = new BufferedOutputStream(new FileOutputStream(
                        tmpDir + "/" + frameNamePrefix + this.seqNum + ".png"));
                out.write(frameImage);
                out.close();
                this.seqNum++;
            } catch (IOException e) {
                logger.error("Unable to save frames. Closing the backup process", e);
                throw new RuntimeException(e);
            }
        });
    }

    private void setup(String tmpDir) {
        ChromeOptions chromeOptions = new ChromeOptions();
        chromeOptions.setPageLoadStrategy(PageLoadStrategy.EAGER);
        chromeOptions.addArguments("--headless=new");
        chromeOptions.addArguments("--disable-notifications");
        chromeOptions.addArguments("--disable-infobars");

        this.webDriver = new ChromeDriver(chromeOptions);
        this.webDriver.manage().window().maximize();

        this.devTools = ((ChromeDriver) this.webDriver).getDevTools();
        this.devTools.createSession();

        // Create a folder for storing frames
        try {
            Files.createDirectories(Paths.get(tmpDir));
        } catch (IOException e) {
            logger.error("Unable to create temporary directory: {}", tmpDir, e);
            throw new RuntimeException(e);
        }
        this.screenRecording(this.devTools, tmpDir);
    }

    private void cleanUp() {
        devTools.disconnectSession();
        devTools.close();
        this.webDriver.quit();
    }

    public void backupAssets(String[] assets, String tmpDir, String outputDir, String videoFileName) {
        try {
            this.setup(tmpDir);
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
                    logger.warn("File/Folder does not exist: {}", assetPath);
                }
            }
            if (!filePathList.isEmpty()) fileUpload(Duration.ofSeconds(50), filePathList);
            folderPathList.forEach(folderPath -> folderUpload(Duration.ofSeconds(50), folderPath));
            uploadChecker(Duration.ofMinutes(20), cntofAssets);

            // To get images of the last frame better
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                logger.warn("Timer threw an exception. Ignored");
            }

            devTools.send(Page.stopScreencast());
            devTools.clearListeners();

            if (Utils.generateVideoFromFrames(frameNamePrefix, tmpDir, outputDir, videoFileName))
                logger.info("Successfully generated video from frames: {}/{}", outputDir, videoFileName);
            else
                logger.error("Something went wrong! Process generating the frames ended with status code other than 0");
        } finally {
            try{
                devTools.send(Page.stopScreencast());
                devTools.clearListeners();
            } catch(Exception e) {
                logger.info("Event listeners might be already closed");
            }
            this.cleanUp();
            Utils.deleteFrames(frameNamePrefix, tmpDir, seqNum);
        }
    }
}
