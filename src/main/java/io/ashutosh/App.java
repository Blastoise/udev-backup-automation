package io.ashutosh;


import java.io.IOException;


public class App {

    public static void main(String[] args) throws IOException {

        String username = System.getenv("PCLOUD_USERNAME");
        String password = System.getenv("PCLOUD_PASSWORD");

        String srcPath = System.getProperty("srcPath");
        String destPath = System.getProperty("destPath");
        String[] assets = System.getProperty("assets").split(";");

        HDDBackup hddBackup = new HDDBackup(srcPath, destPath);
        hddBackup.backupAssets(assets);

        PCloudBackup pCloudBackup = PCloudBackup.getInstance(srcPath, username, password);
        pCloudBackup.backupAssets(assets);
    }
}
