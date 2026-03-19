package com.mediatek.server.wm;

import android.util.Slog;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

class ResolutionTunerAppList {
    private static final String APP_LIST_PATH = "system/vendor/etc/resolution_tuner_app_list.xml";
    private static final String NODE_FILTERED_WINDOW = "filteredwindow";
    private static final String NODE_PACKAGE_NAME = "packagename";
    private static final String NODE_SCALE = "scale";
    private static final String TAG = "ResolutionTunerAppList";
    private static final String TAG_APP = "app";
    private static ResolutionTunerAppList sInstance;
    private ArrayList<Applic> mTunerAppCache;

    public static ResolutionTunerAppList getInstance() {
        if (sInstance == null) {
            sInstance = new ResolutionTunerAppList();
        }
        return sInstance;
    }

    public void loadTunerAppList() throws Throwable {
        File file;
        Slog.d(TAG, "loadTunerAppList + ");
        FileInputStream fileInputStream = null;
        try {
        } catch (IOException e) {
            Slog.w(TAG, "close failed..", e);
        }
        try {
            try {
                file = new File(APP_LIST_PATH);
            } catch (IOException e2) {
                e = e2;
            }
            if (!file.exists()) {
                Slog.e(TAG, "Target file doesn't exist: system/vendor/etc/resolution_tuner_app_list.xml");
                return;
            }
            FileInputStream fileInputStream2 = new FileInputStream(file);
            try {
                this.mTunerAppCache = parseAppListFile(fileInputStream2);
                fileInputStream2.close();
            } catch (IOException e3) {
                e = e3;
                fileInputStream = fileInputStream2;
                Slog.w(TAG, "IOException", e);
                if (fileInputStream != null) {
                    fileInputStream.close();
                }
                Slog.d(TAG, "loadTunerAppList - ");
            } catch (Throwable th) {
                th = th;
                fileInputStream = fileInputStream2;
                if (fileInputStream != null) {
                    try {
                        fileInputStream.close();
                    } catch (IOException e4) {
                        Slog.w(TAG, "close failed..", e4);
                    }
                }
                throw th;
            }
            Slog.d(TAG, "loadTunerAppList - ");
        } catch (Throwable th2) {
            th = th2;
        }
    }

    public boolean contains(String str, String str2) {
        if (this.mTunerAppCache != null) {
            Iterator<Applic> it = this.mTunerAppCache.iterator();
            while (it.hasNext()) {
                if (it.next().getPackageName().equals(str)) {
                    return !r1.isFiltered(str2);
                }
            }
            return false;
        }
        return false;
    }

    public float getScaleValue(String str) {
        if (this.mTunerAppCache != null) {
            for (Applic applic : this.mTunerAppCache) {
                if (applic.getPackageName().equals(str)) {
                    return applic.getScale();
                }
            }
            return 1.0f;
        }
        return 1.0f;
    }

    class Applic {
        private ArrayList<String> filteredWindows = new ArrayList<>();
        private String packageName;
        private float scale;

        Applic() {
        }

        public String getPackageName() {
            return this.packageName;
        }

        public void setPackageName(String str) {
            this.packageName = str;
        }

        public float getScale() {
            return this.scale;
        }

        public void setScale(float f) {
            this.scale = f;
        }

        public void addFilteredWindow(String str) {
            this.filteredWindows.add(str);
        }

        public boolean isFiltered(String str) {
            return this.filteredWindows.contains(str);
        }

        public String toString() {
            return "App{packageName='" + this.packageName + "', scale='" + this.scale + "', filteredWindows= " + this.filteredWindows + "'}";
        }
    }

    private ArrayList<Applic> parseAppListFile(InputStream inputStream) {
        ArrayList<Applic> arrayList = new ArrayList<>();
        try {
            NodeList elementsByTagName = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(inputStream).getElementsByTagName(TAG_APP);
            for (int i = 0; i < elementsByTagName.getLength(); i++) {
                NodeList childNodes = elementsByTagName.item(i).getChildNodes();
                Applic applic = new Applic();
                for (int i2 = 0; i2 < childNodes.getLength(); i2++) {
                    Node nodeItem = childNodes.item(i2);
                    if (nodeItem.getNodeName().equals(NODE_PACKAGE_NAME)) {
                        applic.setPackageName(nodeItem.getTextContent());
                    } else if (nodeItem.getNodeName().equals(NODE_SCALE)) {
                        applic.setScale(Float.parseFloat(nodeItem.getTextContent()));
                    } else if (nodeItem.getNodeName().startsWith(NODE_FILTERED_WINDOW)) {
                        applic.addFilteredWindow(nodeItem.getTextContent());
                    }
                }
                arrayList.add(applic);
                Slog.d(TAG, "dom2xml: " + applic);
            }
            return arrayList;
        } catch (IOException e) {
            Slog.w(TAG, "IOException", e);
            return arrayList;
        } catch (ParserConfigurationException e2) {
            Slog.w(TAG, "dom2xml ParserConfigurationException", e2);
            return arrayList;
        } catch (SAXException e3) {
            Slog.w(TAG, "dom2xml SAXException", e3);
            return arrayList;
        }
    }
}
