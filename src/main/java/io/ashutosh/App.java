package io.ashutosh;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;


public class App {

    private static final Logger logger = LoggerFactory.getLogger(App.class);

    /*
        Variables related to pCloud login:
        --------------------------------------
        username: Username of pCloud account
        password: Password of pCloud account
     */

    private static final String username = System.getenv("PCLOUD_USERNAME");
    private static final String password = System.getenv("PCLOUD_PASSWORD");

    /*
        Variables related to files and folders to be backed up:
        --------------------------------------
        srcPath: Base path from where all the assets can be reached
        destPath: Path where files and folders needs to be put
        assets: Array of string containing path of files and folders relative to srcPath
     */

    private static final String srcPath = System.getProperty("srcPath");
    private static final String destPath = System.getProperty("destPath");
    private static final String[] assets = System.getProperty("assets").split(";");

    /*
        Variables related to Video Recording:
        --------------------------------------
        tmpDir: Location where the frames generated will be kept
        outputDir: Location where the video recording generated will be kept
        pCloudVideoFileName: Name of the video recording generated
     */

    private static final String tmpDir = "/tmp/Udev-Backup-Automation";
    private static final String outputDir = System.getProperty("outputDir");
    private static final String pCloudVideoFileName = "pCloud_Backup.mp4";

    /*
        Variables related to Telegram:
        --------------------------------------
        telegramReceiverID: chat_id where the messages need to be sent
        botToken: Token assigned to the bot being used
     */

    private static final String telegramReceiverID = System.getProperty("telegramReceiverID");
    private static final String botToken = System.getenv("BOT_TOKEN");

    public static void main(String[] args) {
        try {
            // List containing names of files that we were backed up
            List<String> copiedFiles = new ArrayList<>();

            // List containing paths where something went wrong and requires some attention or re-run
            List<String> failedPaths = new ArrayList<>();

            HDDBackup hddBackup = new HDDBackup(srcPath, destPath);
            String message = hddBackup.backupAssets(assets, copiedFiles, failedPaths);

            Telegram telegram = new Telegram(telegramReceiverID, botToken);
            telegram.sendMessageUpdate(message);

            try {
                PCloudBackup pCloudBackup = PCloudBackup.getInstance(srcPath, username, password);
                pCloudBackup.backupAssets(assets, tmpDir, outputDir, pCloudVideoFileName);
                telegram.sendVideoUpdate(outputDir, pCloudVideoFileName);
            } catch (RuntimeException e) {
                telegram.sendMessageUpdate("<b>Backup Failed</b> ❌\n\n" +
                        "<b><i>Check logs for what went wrong in pCloud Backup</i></b>\n");
                throw e;
            }

            // If failed path is empty and pCloud executed successfully then only send 0 exit code for further processing
            // failedPaths not empty or not being able to create destPath folder is exit code 1
            if (!failedPaths.isEmpty() || message.contains("Error occurred while creating folder")) System.exit(1);

        } catch (RuntimeException e) {
            logger.error(e.getMessage(), e);
            logger.error("Abrupt shutdown!");
            System.exit(1);
        }
    }
}
