package com.mediatek.common;

import android.os.SystemClock;
import android.util.Log;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class PluginLoader {
    private static final String TAG = "MPlugin";
    private static File folder;
    private static String tmp;
    private static PluginInfo PluginInfoObj = null;
    public static Map<String, PluginInfo> InterfaceMap = new HashMap();
    public static HashMap<File, String> sApkCache = new HashMap<>();
    public static HashMap<String, PluginInfo> PackageMap = new HashMap<>();
    private static String mOperator = MPluginGuard.getSysProp("persist.operator.optr", "");
    private static String usp_support = MPluginGuard.getSysProp("ro.mtk_carrierexpress_pack", "no");
    private static String usp_apks_path = "usp/usp-apks-path-";

    public static PluginInfo getValue(String str) {
        return InterfaceMap.get(str);
    }

    public static boolean getContainsKey(String str) {
        return InterfaceMap.containsKey(str);
    }

    public static void preloadPluginInfo() {
        ArrayList<File> arrayList = new ArrayList();
        if (usp_support.equals("no")) {
            File file = new File("/vendor/plugin/");
            if (file.exists()) {
                arrayList.addAll(listFiles(file, ".mpinfo"));
            }
            File file2 = new File("/custom/plugin/");
            if (file2.exists()) {
                arrayList.addAll(listFiles(file2, ".mpinfo"));
            }
        } else if (!mOperator.equals("") && mOperator.length() > 0) {
            loadUspPlugin(arrayList, "/custom/");
            loadUspPlugin(arrayList, "/vendor/");
        } else {
            File file3 = new File("/custom/plugin/FwkPlugin/");
            if (file3.exists()) {
                arrayList.addAll(listFiles(file3, ".mpinfo"));
            }
            File file4 = new File("/vendor/plugin/FwkPlugin");
            if (file4.exists()) {
                arrayList.addAll(listFiles(file4, ".mpinfo"));
            }
        }
        Log.d(TAG, "preloadCertificate() -- Start : " + SystemClock.uptimeMillis());
        Log.d(TAG, "preloadCertificate() -- End : " + SystemClock.uptimeMillis());
        Log.d(TAG, "precheckFwkPlugin() -- Start : " + SystemClock.uptimeMillis());
        File file5 = !new File(new StringBuilder().append("/vendor/plugin/").append(mOperator).append("FwkPlugin").toString()).exists() ? null : new File("/vendor/plugin/" + mOperator + "FwkPlugin/" + mOperator + "FwkPlugin.apk");
        if (new File("/custom/plugin/" + mOperator + "FwkPlugin").exists()) {
            file5 = new File("/custom/plugin/" + mOperator + "FwkPlugin/" + mOperator + "FwkPlugin.apk");
        }
        if (file5 != null) {
            if (!usp_support.equals("no") && mOperator.length() > 0) {
                MPluginGuard.checkAuthorizedApk(file5, "com.mediatek.fwk." + mOperator + ".plugin", true);
            } else {
                MPluginGuard.checkAuthorizedApk(file5, "com.mediatek.fwk.plugin", true);
            }
        }
        Log.d(TAG, "precheckFwkPlugin() -- End : " + SystemClock.uptimeMillis());
        for (File file6 : arrayList) {
            Log.d(TAG, file6.getName() + " exist");
            try {
                FileInputStream fileInputStream = new FileInputStream(file6);
                Log.d(TAG, "Load" + file6.getPath() + " sucessfully");
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(fileInputStream));
                String str = null;
                String[] strArr = null;
                while (true) {
                    String line = bufferedReader.readLine();
                    if (line == null) {
                        break;
                    }
                    String[] strArrSplit = line.split("\\|");
                    if (strArrSplit == null) {
                        strArr = strArrSplit;
                    } else {
                        Log.d(TAG, "Put interface : " + strArrSplit[0].trim() + " into InterfaceMap");
                        String str2 = str;
                        for (int i = 0; i < strArrSplit.length; i++) {
                            String path = file6.getPath();
                            str2 = path.substring(0, path.lastIndexOf(".")) + ".apk";
                            InterfaceMap.put(strArrSplit[0].trim(), new PluginInfo(strArrSplit[1].trim(), strArrSplit[2].trim(), str2));
                        }
                        str = str2;
                        strArr = strArrSplit;
                    }
                }
                if (strArr != null) {
                    Log.d(TAG, "Put interface : " + strArr[1].trim() + " into PackageMap");
                    PackageMap.put(strArr[1].trim(), new PluginInfo(strArr[1].trim(), strArr[2].trim(), str));
                }
                fileInputStream.close();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e2) {
                e2.printStackTrace();
            }
        }
        Log.d(TAG, "preloadPluginInfo() -- End : " + SystemClock.uptimeMillis());
    }

    private static List<File> listFiles(File file, String str) {
        ArrayList arrayList = new ArrayList();
        for (File file2 : file.listFiles()) {
            if (file2.isDirectory()) {
                arrayList.addAll(listFiles(file2, str));
            } else {
                String name = file2.getName();
                int iLastIndexOf = name.lastIndexOf(".");
                if (iLastIndexOf >= 0 && name.substring(iLastIndexOf).equals(str)) {
                    arrayList.add(file2);
                    Log.d(TAG, "preload -- mpinfo : " + name);
                }
            }
        }
        return arrayList;
    }

    private static void loadUspPlugin(List<File> list, String str) {
        File file = new File(str + usp_apks_path + mOperator + ".txt");
        if (!file.exists()) {
            return;
        }
        try {
            FileInputStream fileInputStream = new FileInputStream(file);
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(fileInputStream));
            while (true) {
                String line = bufferedReader.readLine();
                if (line != null) {
                    Log.d(TAG, str + "usp_apks_path" + mOperator + " folder :" + line);
                    File file2 = new File(line);
                    if (file2.exists()) {
                        list.addAll(listFiles(file2, ".mpinfo"));
                    }
                } else {
                    fileInputStream.close();
                    return;
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e2) {
            e2.printStackTrace();
        }
    }
}
