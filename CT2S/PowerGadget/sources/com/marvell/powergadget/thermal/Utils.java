package com.marvell.powergadget.thermal;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

public class Utils {
    public static String readInfo(String filePath) {
        StringBuilder result = new StringBuilder();
        File file = new File(filePath);
        BufferedReader br = null;
        if (file.exists()) {
            try {
                BufferedReader br2 = new BufferedReader(new FileReader(file));
                while (true) {
                    try {
                        String line = br2.readLine();
                        if (line == null) {
                            break;
                        }
                        result.append(line);
                    } catch (FileNotFoundException e) {
                        ex = e;
                        br = br2;
                        ex.printStackTrace();
                    } catch (IOException e2) {
                        ioEx = e2;
                        br = br2;
                        ioEx.printStackTrace();
                    }
                }
                br = br2;
            } catch (FileNotFoundException e3) {
                ex = e3;
            } catch (IOException e4) {
                ioEx = e4;
            }
        }
        if (br != null) {
            try {
                br.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
            return result.toString();
        }
        return null;
    }

    public static int readInfoInt(String path) {
        String value = readInfo(path);
        if (value != null) {
            return Integer.parseInt(value);
        }
        return -1;
    }
}
