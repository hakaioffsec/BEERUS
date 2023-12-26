package io.hakaisecurity.beerus;


import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.UUID;



public class FileZipper {

    public static void main(String[] args) {
        String sourceFolderPath = "/data/data/" + args[0];
        String serverUrl = "http://"+args[1]+":"+args[2]+"/upload";
        String zipFilePath = "/data/local/tmp/" + args[0];
        String timeStamp = String.valueOf(new java.util.Date().getTime());
        String tarGzFilePath = zipFilePath+"_"+timeStamp+".tar.gz";

        try {
            Process process = Runtime.getRuntime().exec("su");
            DataOutputStream outputStream = new DataOutputStream(process.getOutputStream());
            outputStream.writeBytes("tar -czf "+tarGzFilePath+" "+sourceFolderPath+"\n");
            outputStream.writeBytes("chmod 777 "+tarGzFilePath+"\n");
            outputStream.writeBytes("exit\n");
            outputStream.flush();
            process.waitFor();

            File file = new File(tarGzFilePath);
            sendFileToServer(file, serverUrl);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void sendFileToServer(File file, String serverUrl) throws IOException {
        URL url = new URL(serverUrl);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        String boundary = UUID.randomUUID().toString();

        connection.setRequestMethod("POST");
        connection.setDoOutput(true);
        connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

        try (DataOutputStream outputStream = new DataOutputStream(connection.getOutputStream());
             FileInputStream fileInputStream = new FileInputStream(file)) {

            outputStream.writeBytes("--" + boundary + "\r\n");
            outputStream.writeBytes("Content-Disposition: form-data; name=\"file\"; filename=\"" + file.getName() + "\"\r\n");
            outputStream.writeBytes("Content-Type: application/x-gzip\r\n\r\n");

            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = fileInputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }

            outputStream.writeBytes("\r\n");
            outputStream.writeBytes("--" + boundary + "--\r\n");

            outputStream.flush();

            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                System.out.println("File sent successfully");
            } else {
                System.err.println("Failed to send the file. Response Code: " + responseCode);
            }
        } finally {
            connection.disconnect();
        }
    }
}
