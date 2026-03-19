package com.mediatek.common;

import android.content.pm.PackageInfo;
import android.content.pm.Signature;
import android.os.IBinder;
import android.util.Log;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class MPluginGuard {
    private static final boolean DEBUG = true;
    static final int RESULT_AUTHORIZED = 1;
    static final int RESULT_ERROR = -2;
    static final int RESULT_UNAUTHORIZED = 0;
    static final int RESULT_UNSIGNED = -1;
    private static final String TAG = "MPluginGuard";
    private static final String sSIGNATURE_FILENAME = "mplugin_guard.xml";
    private static final String sSIGNATURE_FOLDER = "/plugin/Signatures";
    private static InitStatus sInitStatus = InitStatus.NONE;
    private static Map<String, Signature> sAuthorizedSignature = new HashMap();
    private static Object sLock = new Object();
    private static int sTotalSigNum = 0;

    enum InitStatus {
        NONE,
        RUNNING,
        SUCCESS,
        FAILED
    }

    static void init() {
        Logger.d("Start init authorized signature table");
        InitThread initThread = new InitThread();
        sInitStatus = InitStatus.RUNNING;
        initThread.start();
    }

    static int startApkCheck(File file, String str, String str2, boolean z) {
        PackageInfo packageInfo;
        Signature apkSignature;
        synchronized (sLock) {
            if (sInitStatus == InitStatus.RUNNING) {
                Logger.d("Delay checking until loading finished");
                try {
                    sLock.wait();
                } catch (InterruptedException e) {
                    Log.e(TAG, "Wating initialization failed!");
                    return RESULT_ERROR;
                }
            }
        }
        if (sInitStatus != InitStatus.SUCCESS) {
            Log.e(TAG, "Check fail due to init fail!");
            return RESULT_ERROR;
        }
        if (file == null) {
            Log.e(TAG, "Null apkFile!");
            return RESULT_ERROR;
        }
        Logger.d("checkAuthorizedApk(" + file.getName() + ")");
        PackageInfo packageInfo2 = null;
        try {
            if (!"1".equals(str) || z) {
                apkSignature = getApkSignature(file);
            } else {
                try {
                    Class<?> cls = Class.forName("android.os.UserHandle");
                    Logger.d("userHandleClass is " + cls);
                    int iIntValue = ((Integer) cls.getMethod("myUserId", new Class[0]).invoke(null, new Object[0])).intValue();
                    Logger.d("userId is " + iIntValue);
                    Method method = Class.forName("android.os.ServiceManager").getMethod("getService", String.class);
                    Logger.d("get getService method : " + method);
                    IBinder iBinder = (IBinder) method.invoke(null, "package");
                    Logger.d("invoke getServiceBinder : " + iBinder);
                    Method method2 = Class.forName("android.content.pm.IPackageManager").getMethod("getPackageInfo", String.class, Integer.TYPE, Integer.TYPE);
                    Logger.d("Class forName getPackageInfoMethod : " + method2);
                    Method method3 = Class.forName("android.content.pm.IPackageManager$Stub").getMethod("asInterface", IBinder.class);
                    Logger.d("Class forName IPackageManager$Stub : " + method3);
                    Object objInvoke = method3.invoke(null, iBinder);
                    Logger.d("invoke asInterface : " + objInvoke);
                    packageInfo = (PackageInfo) method2.invoke(objInvoke, str2, 64, Integer.valueOf(iIntValue));
                    try {
                        Logger.d("invoke info : " + packageInfo);
                    } catch (Exception e2) {
                        packageInfo2 = packageInfo;
                        e = e2;
                        e.printStackTrace();
                        packageInfo = packageInfo2;
                    }
                } catch (Exception e3) {
                    e = e3;
                }
                apkSignature = (packageInfo.signatures != null && packageInfo.signatures.length > 0) ? packageInfo.signatures[0] : null;
            }
            if (apkSignature == null) {
                Logger.d("Un-signed apk");
                return -1;
            }
            if (isAuthorizedSignature(apkSignature)) {
                Logger.d("Authorized apk");
                return 1;
            }
            Logger.d("Un-authorized apk");
            return 0;
        } catch (SecurityException e4) {
            return RESULT_ERROR;
        }
    }

    static int checkAuthorizedApk(File file, String str, boolean z) {
        return 1;
    }

    private static boolean isSignatureTableReady() {
        if (sTotalSigNum > 0 && sAuthorizedSignature.size() == sTotalSigNum) {
            return DEBUG;
        }
        return false;
    }

    private static boolean readAuthorizedSignaturesLocked() throws Throwable {
        loadSignatureFileLocked(getSignatureFile());
        return isSignatureTableReady();
    }

    private static File getSignatureFile() {
        File file = new File("/vendor/plugin/Signatures");
        if (file.exists()) {
            return new File(file, sSIGNATURE_FILENAME);
        }
        File file2 = new File("/custom/plugin/Signatures");
        if (file2.exists()) {
            return new File(file2, sSIGNATURE_FILENAME);
        }
        throw new SecurityException("getSignatureFile() failed!");
    }

    private static void loadSignatureFileLocked(File file) throws Throwable {
        FileInputStream fileInputStream;
        String nodeValue;
        Signature signature;
        String nodeValue2;
        FileInputStream fileInputStream2 = null;
        try {
            try {
                DocumentBuilder documentBuilderNewDocumentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
                fileInputStream = new FileInputStream(file);
                try {
                    NodeList childNodes = documentBuilderNewDocumentBuilder.parse(fileInputStream).getDocumentElement().getChildNodes();
                    if (childNodes != null) {
                        for (int i = 0; i < childNodes.getLength(); i++) {
                            Node nodeItem = childNodes.item(i);
                            if (nodeItem.getNodeType() == 1 && "check".equals(nodeItem.getNodeName())) {
                                sTotalSigNum++;
                                nodeValue = nodeItem.getAttributes().getNamedItem("guard").getNodeValue();
                                if (nodeValue == null) {
                                    signature = null;
                                } else {
                                    try {
                                        signature = new Signature(nodeValue);
                                    } catch (IllegalArgumentException e) {
                                        Logger.w("<check> with bad guard attribute " + e);
                                    }
                                }
                                Node firstChild = nodeItem.getFirstChild();
                                nodeValue2 = (firstChild != null && firstChild.getNodeType() == 1 && firstChild.getNodeName().equals("info")) ? firstChild.getAttributes().getNamedItem("value").getNodeValue() : null;
                            } else {
                                nodeValue = null;
                                signature = null;
                                nodeValue2 = null;
                            }
                            if (nodeValue2 != null && signature != null) {
                                Logger.d("Found authorized " + nodeValue2 + " with signature: " + nodeValue);
                                sAuthorizedSignature.put(nodeValue2, signature);
                            }
                        }
                    }
                    if (fileInputStream == null) {
                        return;
                    }
                    try {
                        fileInputStream.close();
                    } catch (IOException e2) {
                        Logger.w("Fail to close file! " + e2);
                    }
                } catch (FileNotFoundException e3) {
                    e = e3;
                    Logger.w("Open signature file failed! " + e);
                    if (fileInputStream != null) {
                        try {
                            fileInputStream.close();
                        } catch (IOException e4) {
                            Logger.w("Fail to close file! " + e4);
                        }
                    }
                } catch (IOException e5) {
                    e = e5;
                    Logger.w("Parse signature file failed! " + e);
                    if (fileInputStream != null) {
                        try {
                            fileInputStream.close();
                        } catch (IOException e6) {
                            Logger.w("Fail to close file! " + e6);
                        }
                    }
                } catch (ParserConfigurationException e7) {
                    e = e7;
                    Logger.w("Parse signature file failed! " + e);
                    if (fileInputStream != null) {
                        try {
                            fileInputStream.close();
                        } catch (IOException e8) {
                            Logger.w("Fail to close file! " + e8);
                        }
                    }
                } catch (SAXException e9) {
                    e = e9;
                    Logger.w("Parse signature file failed! " + e);
                    if (fileInputStream != null) {
                        try {
                            fileInputStream.close();
                        } catch (IOException e10) {
                            Logger.w("Fail to close file! " + e10);
                        }
                    }
                }
            } catch (Throwable th) {
                th = th;
                if (0 != 0) {
                    try {
                        fileInputStream2.close();
                    } catch (IOException e11) {
                        Logger.w("Fail to close file! " + e11);
                    }
                }
                throw th;
            }
        } catch (FileNotFoundException e12) {
            e = e12;
            fileInputStream = null;
        } catch (IOException e13) {
            e = e13;
            fileInputStream = null;
        } catch (ParserConfigurationException e14) {
            e = e14;
            fileInputStream = null;
        } catch (SAXException e15) {
            e = e15;
            fileInputStream = null;
        } catch (Throwable th2) {
            th = th2;
            if (0 != 0) {
            }
            throw th;
        }
    }

    private static Signature getApkSignature(File file) {
        try {
            Class<?> cls = Class.forName("android.content.pm.PackageParser");
            Class<?> cls2 = Class.forName("android.content.pm.PackageParser$ApkLite");
            Method method = cls.getMethod("parseApkLite", File.class, Integer.TYPE);
            method.setAccessible(DEBUG);
            Object objInvoke = method.invoke(cls.newInstance(), file, Integer.valueOf(cls.getField("PARSE_MUST_BE_APK").getInt(null) | cls.getField("PARSE_COLLECT_CERTIFICATES").getInt(null)));
            if (objInvoke == null) {
                return null;
            }
            Signature[] signatureArr = (Signature[]) cls2.getField("signatures").get(objInvoke);
            if (signatureArr != null && signatureArr.length > 0) {
                return signatureArr[0];
            }
            return null;
        } catch (Exception e) {
            throw new SecurityException("parse apk " + file.getName() + " failed! " + e);
        }
    }

    private static boolean isAuthorizedSignature(Signature signature) {
        for (Map.Entry<String, Signature> entry : sAuthorizedSignature.entrySet()) {
            String key = entry.getKey();
            if (entry.getValue().equals(signature)) {
                Logger.d("Found authorized " + key);
                return DEBUG;
            }
        }
        return false;
    }

    private static void notifyUnlock(String str) {
        synchronized (sLock) {
            sLock.notifyAll();
            Logger.d("Notify unlock: " + str);
        }
    }

    static class InitThread extends Thread {
        private static final int INIT_TIMEOUT = 10000;
        private static final long RETRY_DELAY = 500;
        private static final int RETRY_NUM = 2;

        InitThread() {
        }

        @Override
        public void run() {
            Logger.d("Start InitThread");
            SignatureLoader signatureLoader = new SignatureLoader(2, RETRY_DELAY, this);
            signatureLoader.start();
            try {
                Logger.d("Start counting timeout");
                sleep(10000L);
            } catch (InterruptedException e) {
                Log.d(MPluginGuard.TAG, "Timeout interrupted");
            }
            MPluginGuard.notifyUnlock("InitThread");
            if (!MPluginGuard.isSignatureTableReady()) {
                synchronized (MPluginGuard.sLock) {
                    Logger.d("Loading signature table timeout!");
                    InitStatus unused = MPluginGuard.sInitStatus = InitStatus.FAILED;
                    signatureLoader.interrupt();
                }
            }
        }
    }

    static class SignatureLoader extends Thread {
        private long mDelayTime;
        private int mRetryNum;
        private Thread mTimeoutThread;

        public SignatureLoader(int i, long j, Thread thread) {
            this.mRetryNum = i;
            this.mDelayTime = j;
            this.mTimeoutThread = thread;
        }

        @Override
        public void run() {
            int i = 0;
            Logger.d("Start SignatureLoader");
            while (true) {
                if (i > this.mRetryNum) {
                    break;
                }
                if (MPluginGuard.readAuthorizedSignaturesLocked()) {
                    Logger.d("Read authorized signature files done!");
                    synchronized (MPluginGuard.sLock) {
                        InitStatus unused = MPluginGuard.sInitStatus = InitStatus.SUCCESS;
                    }
                    break;
                }
                if (this.mRetryNum > 0) {
                    try {
                        Logger.w("Retry (" + i + ") loading signature after: " + this.mDelayTime + "ms");
                        sleep(this.mDelayTime);
                    } catch (InterruptedException e) {
                        Log.e(MPluginGuard.TAG, "Delay interrupted", e);
                    }
                }
                i++;
            }
        }
    }

    static class Logger {
        Logger() {
        }

        static void d(String str) {
            Log.d(MPluginGuard.TAG, str);
        }

        static void w(String str) {
            Log.w(MPluginGuard.TAG, str);
        }
    }

    static String getSysProp(String str, String str2) {
        String str3;
        String str4 = "";
        try {
            str3 = (String) Class.forName("android.os.SystemProperties").getMethod("get", String.class, String.class).invoke(null, str, str2);
        } catch (ClassNotFoundException e) {
            e = e;
        } catch (IllegalAccessException e2) {
            e = e2;
        } catch (NoSuchMethodException e3) {
            e = e3;
        } catch (InvocationTargetException e4) {
            e = e4;
        }
        try {
            Logger.d("SystemProperties " + str + " is" + str3);
            return str3;
        } catch (ClassNotFoundException e5) {
            str4 = str3;
            e = e5;
            Logger.w("Get system properties failed! " + e);
            return str4;
        } catch (IllegalAccessException e6) {
            str4 = str3;
            e = e6;
            Logger.w("Get system properties failed! " + e);
            return str4;
        } catch (NoSuchMethodException e7) {
            str4 = str3;
            e = e7;
            Logger.w("Get system properties failed! " + e);
            return str4;
        } catch (InvocationTargetException e8) {
            str4 = str3;
            e = e8;
            Logger.w("Get system properties failed! " + e);
            return str4;
        }
    }
}
