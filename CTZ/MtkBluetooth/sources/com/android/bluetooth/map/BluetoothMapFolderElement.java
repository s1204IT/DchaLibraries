package com.android.bluetooth.map;

import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.util.Xml;
import com.android.internal.util.FastXmlSerializer;
import com.android.internal.util.XmlUtils;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Locale;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

public class BluetoothMapFolderElement implements Comparable<BluetoothMapFolderElement> {
    private static final boolean D = BluetoothMapService.DEBUG;
    private static final String TAG = "BluetoothMapFolderElement";
    private static final boolean V = false;
    private String mName;
    private BluetoothMapFolderElement mParent;
    private long mFolderId = -1;
    private boolean mHasSmsMmsContent = false;
    private boolean mHasImContent = false;
    private boolean mHasEmailContent = false;
    private boolean mIgnore = false;
    private HashMap<String, BluetoothMapFolderElement> mSubFolders = new HashMap<>();

    public BluetoothMapFolderElement(String str, BluetoothMapFolderElement bluetoothMapFolderElement) {
        this.mParent = null;
        this.mName = str;
        this.mParent = bluetoothMapFolderElement;
    }

    public void setIngore(boolean z) {
        this.mIgnore = z;
    }

    public boolean shouldIgnore() {
        return this.mIgnore;
    }

    public String getName() {
        return this.mName;
    }

    public boolean hasSmsMmsContent() {
        return this.mHasSmsMmsContent;
    }

    public long getFolderId() {
        return this.mFolderId;
    }

    public boolean hasEmailContent() {
        return this.mHasEmailContent;
    }

    public void setFolderId(long j) {
        this.mFolderId = j;
    }

    public void setHasSmsMmsContent(boolean z) {
        this.mHasSmsMmsContent = z;
    }

    public void setHasEmailContent(boolean z) {
        this.mHasEmailContent = z;
    }

    public void setHasImContent(boolean z) {
        this.mHasImContent = z;
    }

    public boolean hasImContent() {
        return this.mHasImContent;
    }

    public BluetoothMapFolderElement getParent() {
        return this.mParent;
    }

    public String getFullPath() {
        StringBuilder sb = new StringBuilder(this.mName);
        for (BluetoothMapFolderElement parent = this.mParent; parent != null; parent = parent.getParent()) {
            if (parent.getParent() != null) {
                sb.insert(0, parent.mName + "/");
            }
        }
        return sb.toString();
    }

    public BluetoothMapFolderElement getFolderByName(String str) {
        BluetoothMapFolderElement subFolder = getRoot().getSubFolder("telecom").getSubFolder(NotificationCompat.CATEGORY_MESSAGE).getSubFolder(str);
        if (subFolder != null && subFolder.getFolderId() == -1) {
            return null;
        }
        return subFolder;
    }

    public BluetoothMapFolderElement getFolderById(long j) {
        return getFolderById(j, this);
    }

    public static BluetoothMapFolderElement getFolderById(long j, BluetoothMapFolderElement bluetoothMapFolderElement) {
        if (bluetoothMapFolderElement == null) {
            return null;
        }
        return findFolderById(j, bluetoothMapFolderElement.getRoot());
    }

    private static BluetoothMapFolderElement findFolderById(long j, BluetoothMapFolderElement bluetoothMapFolderElement) {
        if (bluetoothMapFolderElement.getFolderId() == j) {
            return bluetoothMapFolderElement;
        }
        for (BluetoothMapFolderElement bluetoothMapFolderElement2 : (BluetoothMapFolderElement[]) bluetoothMapFolderElement.mSubFolders.values().toArray(new BluetoothMapFolderElement[bluetoothMapFolderElement.mSubFolders.size()])) {
            BluetoothMapFolderElement bluetoothMapFolderElementFindFolderById = findFolderById(j, bluetoothMapFolderElement2);
            if (bluetoothMapFolderElementFindFolderById != null) {
                return bluetoothMapFolderElementFindFolderById;
            }
        }
        return null;
    }

    public BluetoothMapFolderElement getRoot() {
        BluetoothMapFolderElement parent = this;
        while (parent.getParent() != null) {
            parent = parent.getParent();
        }
        return parent;
    }

    public BluetoothMapFolderElement addFolder(String str) {
        String lowerCase = str.toLowerCase(Locale.US);
        BluetoothMapFolderElement bluetoothMapFolderElement = this.mSubFolders.get(lowerCase);
        if (bluetoothMapFolderElement == null) {
            if (D) {
                Log.i(TAG, "addFolder():" + lowerCase);
            }
            BluetoothMapFolderElement bluetoothMapFolderElement2 = new BluetoothMapFolderElement(lowerCase, this);
            this.mSubFolders.put(lowerCase, bluetoothMapFolderElement2);
            return bluetoothMapFolderElement2;
        }
        if (D) {
            Log.i(TAG, "addFolder():" + lowerCase + " already added");
            return bluetoothMapFolderElement;
        }
        return bluetoothMapFolderElement;
    }

    public BluetoothMapFolderElement addSmsMmsFolder(String str) {
        if (D) {
            Log.i(TAG, "addSmsMmsFolder()");
        }
        BluetoothMapFolderElement bluetoothMapFolderElementAddFolder = addFolder(str);
        bluetoothMapFolderElementAddFolder.setHasSmsMmsContent(true);
        return bluetoothMapFolderElementAddFolder;
    }

    public BluetoothMapFolderElement addImFolder(String str, long j) {
        if (D) {
            Log.i(TAG, "addImFolder() id = " + j);
        }
        BluetoothMapFolderElement bluetoothMapFolderElementAddFolder = addFolder(str);
        bluetoothMapFolderElementAddFolder.setHasImContent(true);
        bluetoothMapFolderElementAddFolder.setFolderId(j);
        return bluetoothMapFolderElementAddFolder;
    }

    public BluetoothMapFolderElement addEmailFolder(String str, long j) {
        BluetoothMapFolderElement bluetoothMapFolderElementAddFolder = addFolder(str);
        bluetoothMapFolderElementAddFolder.setFolderId(j);
        bluetoothMapFolderElementAddFolder.setHasEmailContent(true);
        return bluetoothMapFolderElementAddFolder;
    }

    public int getSubFolderCount() {
        return this.mSubFolders.size();
    }

    public BluetoothMapFolderElement getSubFolder(String str) {
        return this.mSubFolders.get(str.toLowerCase());
    }

    public byte[] encode(int i, int i2) throws UnsupportedEncodingException {
        StringWriter stringWriter = new StringWriter();
        FastXmlSerializer fastXmlSerializer = new FastXmlSerializer();
        BluetoothMapFolderElement[] bluetoothMapFolderElementArr = (BluetoothMapFolderElement[]) this.mSubFolders.values().toArray(new BluetoothMapFolderElement[this.mSubFolders.size()]);
        if (i > this.mSubFolders.size()) {
            throw new IllegalArgumentException("FolderListingEncode: offset > subFolders.size()");
        }
        int size = i2 + i;
        if (size > this.mSubFolders.size()) {
            size = this.mSubFolders.size();
        }
        try {
            fastXmlSerializer.setOutput(stringWriter);
            fastXmlSerializer.startDocument("UTF-8", true);
            fastXmlSerializer.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", true);
            fastXmlSerializer.startTag(null, "folder-listing");
            fastXmlSerializer.attribute(null, "version", "1.0");
            while (i < size) {
                fastXmlSerializer.startTag(null, "folder");
                fastXmlSerializer.attribute(null, "name", bluetoothMapFolderElementArr[i].getName());
                fastXmlSerializer.endTag(null, "folder");
                i++;
            }
            fastXmlSerializer.endTag(null, "folder-listing");
            fastXmlSerializer.endDocument();
            return stringWriter.toString().getBytes("UTF-8");
        } catch (IOException e) {
            if (D) {
                Log.w(TAG, e);
            }
            throw new IllegalArgumentException("error encoding folderElement");
        } catch (IllegalArgumentException e2) {
            if (D) {
                Log.w(TAG, e2);
            }
            throw new IllegalArgumentException("error encoding folderElement");
        } catch (IllegalStateException e3) {
            if (D) {
                Log.w(TAG, e3);
            }
            throw new IllegalArgumentException("error encoding folderElement");
        }
    }

    public void appendSubfolders(InputStream inputStream) throws XmlPullParserException, IOException {
        try {
            XmlPullParser xmlPullParserNewPullParser = Xml.newPullParser();
            xmlPullParserNewPullParser.setInput(inputStream, "UTF-8");
            while (true) {
                int next = xmlPullParserNewPullParser.next();
                if (next == 3 || next == 1) {
                    break;
                }
                if (xmlPullParserNewPullParser.getEventType() == 2) {
                    String name = xmlPullParserNewPullParser.getName();
                    if (!name.equalsIgnoreCase("folder-listing")) {
                        if (D) {
                            Log.i(TAG, "Unknown XML tag: " + name);
                        }
                        XmlUtils.skipCurrentTag(xmlPullParserNewPullParser);
                    }
                    readFolders(xmlPullParserNewPullParser);
                }
            }
        } finally {
            inputStream.close();
        }
    }

    public void readFolders(XmlPullParser xmlPullParser) throws XmlPullParserException, IOException {
        if (D) {
            Log.i(TAG, "readFolders(): ");
        }
        while (true) {
            int next = xmlPullParser.next();
            if (next != 3 && next != 1) {
                if (xmlPullParser.getEventType() == 2) {
                    String name = xmlPullParser.getName();
                    if (!name.trim().equalsIgnoreCase("folder")) {
                        if (D) {
                            Log.i(TAG, "Unknown XML tag: " + name);
                        }
                        XmlUtils.skipCurrentTag(xmlPullParser);
                    } else {
                        int attributeCount = xmlPullParser.getAttributeCount();
                        for (int i = 0; i < attributeCount; i++) {
                            if (xmlPullParser.getAttributeName(i).trim().equalsIgnoreCase("name")) {
                                BluetoothMapFolderElement bluetoothMapFolderElementAddFolder = addFolder(xmlPullParser.getAttributeValue(i).trim());
                                bluetoothMapFolderElementAddFolder.setHasEmailContent(this.mHasEmailContent);
                                bluetoothMapFolderElementAddFolder.setHasImContent(this.mHasImContent);
                                bluetoothMapFolderElementAddFolder.setHasSmsMmsContent(this.mHasSmsMmsContent);
                            } else if (D) {
                                Log.i(TAG, "Unknown XML attribute: " + xmlPullParser.getAttributeName(i));
                            }
                        }
                        xmlPullParser.nextTag();
                    }
                }
            } else {
                return;
            }
        }
    }

    @Override
    public int compareTo(BluetoothMapFolderElement bluetoothMapFolderElement) {
        if (bluetoothMapFolderElement == null) {
            return 1;
        }
        int iCompareToIgnoreCase = this.mName.compareToIgnoreCase(bluetoothMapFolderElement.mName);
        if (iCompareToIgnoreCase == 0) {
            iCompareToIgnoreCase = this.mSubFolders.size() - bluetoothMapFolderElement.mSubFolders.size();
            if (iCompareToIgnoreCase == 0) {
                for (BluetoothMapFolderElement bluetoothMapFolderElement2 : this.mSubFolders.values()) {
                    BluetoothMapFolderElement bluetoothMapFolderElement3 = bluetoothMapFolderElement.mSubFolders.get(bluetoothMapFolderElement2.getName());
                    if (bluetoothMapFolderElement3 == null) {
                        if (D) {
                            Log.i(TAG, bluetoothMapFolderElement2.getFullPath() + " not in another");
                        }
                        return 1;
                    }
                    int iCompareTo = bluetoothMapFolderElement2.compareTo(bluetoothMapFolderElement3);
                    if (iCompareTo == 0) {
                        iCompareToIgnoreCase = iCompareTo;
                    } else {
                        if (D) {
                            Log.i(TAG, bluetoothMapFolderElement2.getFullPath() + " filed compareTo()");
                        }
                        return iCompareTo;
                    }
                }
            } else if (D) {
                Log.i(TAG, "mSubFolders.size(): " + this.mSubFolders.size() + " another.mSubFolders.size(): " + bluetoothMapFolderElement.mSubFolders.size());
            }
        } else if (D) {
            Log.i(TAG, "mName: " + this.mName + " another.mName: " + bluetoothMapFolderElement.mName);
        }
        return iCompareToIgnoreCase;
    }
}
