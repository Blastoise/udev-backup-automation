package io.ashutosh;


import java.io.IOException;
import java.util.List;


public class App {

    public static void main(String[] args) throws IOException, InterruptedException {

        String username = System.getenv("PCLOUD_USERNAME");
        String password = System.getenv("PCLOUD_PASSWORD");
        String srcPath = System.getProperty("srcPath");
        String destPath = System.getProperty("destPath");
        String[] assets = System.getProperty("assets").split(";");
        String telegramReceiverID = System.getProperty("telegramReceiverID");
        String botToken = System.getenv("BOT_TOKEN");

        HDDBackup hddBackup = new HDDBackup(srcPath, destPath);
        List<String> copiedFiles = hddBackup.backupAssets(assets);

        PCloudBackup pCloudBackup = PCloudBackup.getInstance(srcPath, username, password);
        pCloudBackup.backupAssets(assets);

        Telegram telegram = new Telegram(telegramReceiverID, botToken);
        telegram.sendNotification(copiedFiles);

    }
}
