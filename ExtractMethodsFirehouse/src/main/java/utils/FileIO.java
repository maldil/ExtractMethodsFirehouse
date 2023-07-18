package utils;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashSet;

public class FileIO {
    public static String readStringFromFile(String inputFile) {
        Path path = Paths.get(inputFile);


        if (Files.notExists(path)) {
            System.err.println("The file " + inputFile + " does not exist.");
        }
        try {
            BufferedInputStream in = new BufferedInputStream(new FileInputStream(inputFile));
            byte[] bytes = new byte[(int) new File(inputFile).length()];
            in.read(bytes);
            in.close();
            return new String(bytes);
        } catch (Exception e) {
            //e.printStackTrace();
            return null;
        }
    }

    public static void deleteDFile(String directoryPath) {
        Path directory = Paths.get(directoryPath);
        if (Files.exists(directory)) {
            try {
                Files.walk(directory)
                        .sorted((path1, path2) -> -path1.compareTo(path2))
                        .forEach(path -> {
                            try {
                                Files.delete(path);
                            } catch (IOException e) {
                                System.out.println(e);
                            }
                        });

            } catch (IOException e) {
                System.out.println(e);
            }
        }


    }

    public static String getMD5Encoding(String fileName) {
        byte[] thedigest = null;
        try {
            byte[] bytesOfMessage = fileName.getBytes("UTF-8");
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            thedigest = md.digest(bytesOfMessage);
        } catch (UnsupportedEncodingException | NoSuchAlgorithmException e) {
            thedigest = null;
        }
        String md5s = fileName;
        if (thedigest != null) {
            StringBuffer sbDigest = new StringBuffer();
            for (int i = 0; i < thedigest.length; ++i)
                sbDigest.append(Integer.toHexString((thedigest[i] & 0xFF) | 0x100).substring(1, 3));
            md5s = sbDigest.toString();
        }
        return md5s;
    }

    public static void writeStringToNewFile(String filePath, String content) {
        File file = new File(filePath);

        // Delete the file if it already exists
        if (file.exists()) {
            if (file.delete()) {
                System.out.println("Existing file deleted.");
            } else {
                System.out.println("Failed to delete existing file.");
            }
        }

        // Write the content to the file
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            writer.write(content);
            System.out.println("Content written to the file.");
        } catch (IOException e) {
            System.out.println("An error occurred while writing to the file: " + e.getMessage());
        }
    }

    public static void writeStringToFile(String string, String outputFile) {

        try {
            Path path = Paths.get(outputFile);
            Files.createDirectories(path.getParent());
            BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile));
            writer.write(string);
            writer.flush();
            writer.close();
        }
        catch (Exception e) {
			/*e.printStackTrace();
			System.exit(0);*/
            System.err.println(e.getMessage());
        }
    }

    public static void appendHashSetToFile(String filePath, String content) {
        try {
            File file = new File(filePath);
            if (!file.exists()) {
                file.createNewFile();
                System.out.println("File created: " + filePath);
            }

            BufferedWriter writer = new BufferedWriter(new FileWriter(file, true));

            writer.write(content);
            writer.newLine();

            writer.close();
            System.out.println("HashSet content appended to the file successfully.");
        } catch (IOException e) {
            System.err.println("Error manipulating file: " + e.getMessage());
        }
    }

}

