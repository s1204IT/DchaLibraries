package com.android.bluetooth.map;

import android.util.Log;
import com.android.internal.util.FastXmlSerializer;
import java.io.IOException;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Locale;

public class BluetoothMapFolderElement {
    private static final boolean D = true;
    private static final String TAG = "BluetoothMapFolderElement";
    private static final boolean V = false;
    private String mName;
    private BluetoothMapFolderElement mParent;
    private boolean mHasSmsMmsContent = false;
    private long mEmailFolderId = -1;
    private HashMap<String, BluetoothMapFolderElement> mSubFolders = new HashMap<>();

    public BluetoothMapFolderElement(String name, BluetoothMapFolderElement parrent) {
        this.mParent = null;
        this.mName = name;
        this.mParent = parrent;
    }

    public String getName() {
        return this.mName;
    }

    public boolean hasSmsMmsContent() {
        return this.mHasSmsMmsContent;
    }

    public long getEmailFolderId() {
        return this.mEmailFolderId;
    }

    public void setEmailFolderId(long emailFolderId) {
        this.mEmailFolderId = emailFolderId;
    }

    public void setHasSmsMmsContent(boolean hasSmsMmsContent) {
        this.mHasSmsMmsContent = hasSmsMmsContent;
    }

    public BluetoothMapFolderElement getParent() {
        return this.mParent;
    }

    public String getFullPath() {
        StringBuilder sb = new StringBuilder(this.mName);
        for (BluetoothMapFolderElement current = this.mParent; current != null; current = current.getParent()) {
            if (current.getParent() != null) {
                sb.insert(0, current.mName + "/");
            }
        }
        return sb.toString();
    }

    public BluetoothMapFolderElement getEmailFolderByName(String name) {
        BluetoothMapFolderElement folderElement = getRoot().getSubFolder("telecom").getSubFolder("msg").getSubFolder(name);
        if (folderElement != null && folderElement.getEmailFolderId() == -1) {
            return null;
        }
        return folderElement;
    }

    public BluetoothMapFolderElement getEmailFolderById(long id) {
        return getEmailFolderById(id, this);
    }

    public static BluetoothMapFolderElement getEmailFolderById(long id, BluetoothMapFolderElement folderStructure) {
        if (folderStructure == null) {
            return null;
        }
        return findEmailFolderById(id, folderStructure.getRoot());
    }

    private static BluetoothMapFolderElement findEmailFolderById(long id, BluetoothMapFolderElement folder) {
        if (folder.getEmailFolderId() != id) {
            BluetoothMapFolderElement[] arr$ = (BluetoothMapFolderElement[]) folder.mSubFolders.values().toArray(new BluetoothMapFolderElement[folder.mSubFolders.size()]);
            for (BluetoothMapFolderElement subFolder : arr$) {
                BluetoothMapFolderElement ret = findEmailFolderById(id, subFolder);
                if (ret != null) {
                    return ret;
                }
            }
            return null;
        }
        return folder;
    }

    public BluetoothMapFolderElement getRoot() {
        BluetoothMapFolderElement rootFolder = this;
        while (rootFolder.getParent() != null) {
            rootFolder = rootFolder.getParent();
        }
        return rootFolder;
    }

    public BluetoothMapFolderElement addFolder(String name) {
        String name2 = name.toLowerCase(Locale.US);
        BluetoothMapFolderElement newFolder = this.mSubFolders.get(name2);
        Log.i(TAG, "addFolder():" + name2);
        if (newFolder == null) {
            BluetoothMapFolderElement newFolder2 = new BluetoothMapFolderElement(name2, this);
            this.mSubFolders.put(name2, newFolder2);
            return newFolder2;
        }
        return newFolder;
    }

    public BluetoothMapFolderElement addSmsMmsFolder(String name) {
        String name2 = name.toLowerCase(Locale.US);
        BluetoothMapFolderElement newFolder = this.mSubFolders.get(name2);
        Log.i(TAG, "addSmsMmsFolder():" + name2);
        if (newFolder == null) {
            newFolder = new BluetoothMapFolderElement(name2, this);
            this.mSubFolders.put(name2, newFolder);
        }
        newFolder.setHasSmsMmsContent(true);
        return newFolder;
    }

    public BluetoothMapFolderElement addEmailFolder(String name, long emailFolderId) {
        String name2 = name.toLowerCase();
        BluetoothMapFolderElement newFolder = this.mSubFolders.get(name2);
        if (newFolder == null) {
            newFolder = new BluetoothMapFolderElement(name2, this);
            this.mSubFolders.put(name2, newFolder);
        }
        newFolder.setEmailFolderId(emailFolderId);
        return newFolder;
    }

    public int getSubFolderCount() {
        return this.mSubFolders.size();
    }

    public BluetoothMapFolderElement getSubFolder(String folderName) {
        return this.mSubFolders.get(folderName.toLowerCase());
    }

    public byte[] encode(int offset, int count) throws UnsupportedEncodingException {
        StringWriter sw = new StringWriter();
        FastXmlSerializer fastXmlSerializer = new FastXmlSerializer();
        BluetoothMapFolderElement[] folders = (BluetoothMapFolderElement[]) this.mSubFolders.values().toArray(new BluetoothMapFolderElement[this.mSubFolders.size()]);
        if (offset > this.mSubFolders.size()) {
            throw new IllegalArgumentException("FolderListingEncode: offset > subFolders.size()");
        }
        int stopIndex = offset + count;
        if (stopIndex > this.mSubFolders.size()) {
            stopIndex = this.mSubFolders.size();
        }
        try {
            fastXmlSerializer.setOutput(sw);
            fastXmlSerializer.startDocument("UTF-8", true);
            fastXmlSerializer.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", true);
            fastXmlSerializer.startTag(null, "folder-listing");
            fastXmlSerializer.attribute(null, "version", "1.0");
            for (int i = offset; i < stopIndex; i++) {
                fastXmlSerializer.startTag(null, "folder");
                fastXmlSerializer.attribute(null, "name", folders[i].getName());
                fastXmlSerializer.endTag(null, "folder");
            }
            fastXmlSerializer.endTag(null, "folder-listing");
            fastXmlSerializer.endDocument();
            return sw.toString().getBytes("UTF-8");
        } catch (IOException e) {
            Log.w(TAG, e);
            throw new IllegalArgumentException("error encoding folderElement");
        } catch (IllegalArgumentException e2) {
            Log.w(TAG, e2);
            throw new IllegalArgumentException("error encoding folderElement");
        } catch (IllegalStateException e3) {
            Log.w(TAG, e3);
            throw new IllegalArgumentException("error encoding folderElement");
        }
    }
}
