package com.android.services.telephony.sip;

import android.content.Context;
import android.net.sip.SipProfile;
import android.text.TextUtils;
import android.util.EventLog;
import android.util.Log;
import com.android.internal.os.AtomicFile;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

class SipProfileDb {
    private Context mContext;
    private int mProfilesCount = -1;
    private String mProfilesDirectory;
    private SipPreferences mSipPreferences;

    public SipProfileDb(Context context) {
        this.mContext = context.createCredentialProtectedStorageContext();
        setupDatabase();
    }

    public void accessDEStorageForMigration() {
        this.mContext = this.mContext.createDeviceProtectedStorageContext();
        setupDatabase();
    }

    private void setupDatabase() {
        this.mProfilesDirectory = this.mContext.getFilesDir().getAbsolutePath() + "/profiles/";
        this.mSipPreferences = new SipPreferences(this.mContext);
    }

    public void deleteProfile(SipProfile sipProfile) throws IOException {
        synchronized (SipProfileDb.class) {
            File file = new File(this.mProfilesDirectory, sipProfile.getProfileName());
            if (!isChild(new File(this.mProfilesDirectory), file)) {
                throw new IOException("Invalid Profile Credentials!");
            }
            deleteProfile(file);
            if (this.mProfilesCount < 0) {
                retrieveSipProfileListInternal();
            }
        }
    }

    private void deleteProfile(File file) {
        if (file.isDirectory()) {
            for (File file2 : file.listFiles()) {
                deleteProfile(file2);
            }
        }
        file.delete();
    }

    public void cleanupUponMigration() {
        File file = new File(this.mProfilesDirectory);
        if (file.isDirectory()) {
            file.delete();
        }
        this.mSipPreferences.clearSharedPreferences();
    }

    public void saveProfile(SipProfile sipProfile) throws IOException {
        FileOutputStream fileOutputStreamStartWrite;
        ObjectOutputStream objectOutputStream;
        synchronized (SipProfileDb.class) {
            if (this.mProfilesCount < 0) {
                retrieveSipProfileListInternal();
            }
            File file = new File(this.mProfilesDirectory, sipProfile.getProfileName());
            if (!isChild(new File(this.mProfilesDirectory), file)) {
                throw new IOException("Invalid Profile Credentials!");
            }
            if (!file.exists()) {
                file.mkdirs();
            }
            AtomicFile atomicFile = new AtomicFile(new File(file, ".pobj"));
            ObjectOutputStream objectOutputStream2 = null;
            try {
                try {
                    fileOutputStreamStartWrite = atomicFile.startWrite();
                    try {
                        objectOutputStream = new ObjectOutputStream(fileOutputStreamStartWrite);
                    } catch (IOException e) {
                        e = e;
                    }
                } catch (Throwable th) {
                    th = th;
                }
            } catch (IOException e2) {
                e = e2;
                fileOutputStreamStartWrite = null;
            }
            try {
                objectOutputStream.writeObject(sipProfile);
                objectOutputStream.flush();
                atomicFile.finishWrite(fileOutputStreamStartWrite);
                objectOutputStream.close();
            } catch (IOException e3) {
                e = e3;
                objectOutputStream2 = objectOutputStream;
                atomicFile.failWrite(fileOutputStreamStartWrite);
                throw e;
            } catch (Throwable th2) {
                th = th2;
                objectOutputStream2 = objectOutputStream;
                if (objectOutputStream2 != null) {
                    objectOutputStream2.close();
                }
                throw th;
            }
        }
    }

    public List<SipProfile> retrieveSipProfileList() {
        List<SipProfile> listRetrieveSipProfileListInternal;
        synchronized (SipProfileDb.class) {
            listRetrieveSipProfileListInternal = retrieveSipProfileListInternal();
        }
        return listRetrieveSipProfileListInternal;
    }

    private List<SipProfile> retrieveSipProfileListInternal() throws Throwable {
        List<SipProfile> listSynchronizedList = Collections.synchronizedList(new ArrayList());
        String[] list = new File(this.mProfilesDirectory).list();
        if (list == null) {
            return listSynchronizedList;
        }
        for (String str : list) {
            SipProfile sipProfileRetrieveSipProfileFromName = retrieveSipProfileFromName(str);
            if (sipProfileRetrieveSipProfileFromName != null) {
                listSynchronizedList.add(sipProfileRetrieveSipProfileFromName);
            }
        }
        this.mProfilesCount = listSynchronizedList.size();
        return listSynchronizedList;
    }

    public SipProfile retrieveSipProfileFromName(String str) throws Throwable {
        if (TextUtils.isEmpty(str)) {
            return null;
        }
        File file = new File(new File(new File(this.mProfilesDirectory), str), ".pobj");
        if (file.exists()) {
            try {
                SipProfile sipProfileDeserialize = deserialize(file);
                if (sipProfileDeserialize != null) {
                    if (str.equals(sipProfileDeserialize.getProfileName())) {
                        return sipProfileDeserialize;
                    }
                }
            } catch (IOException e) {
                log("retrieveSipProfileListInternal, exception: " + e);
            }
        }
        return null;
    }

    private SipProfile deserialize(File file) throws Throwable {
        Throwable th;
        ObjectInputStream objectInputStream;
        AtomicFile atomicFile = new AtomicFile(file);
        ObjectInputStream objectInputStream2 = null;
        try {
            try {
                objectInputStream = new ObjectInputStream(atomicFile.openRead());
                try {
                    SipProfile sipProfile = (SipProfile) objectInputStream.readObject();
                    objectInputStream.close();
                    return sipProfile;
                } catch (ClassNotFoundException e) {
                    e = e;
                    log("deserialize, exception: " + e);
                    if (objectInputStream != null) {
                        objectInputStream.close();
                    }
                    return null;
                }
            } catch (Throwable th2) {
                th = th2;
                if (0 != 0) {
                    objectInputStream2.close();
                }
                throw th;
            }
        } catch (ClassNotFoundException e2) {
            e = e2;
            objectInputStream = null;
        } catch (Throwable th3) {
            th = th3;
            if (0 != 0) {
            }
            throw th;
        }
    }

    private static void log(String str) {
        Log.d("SIP", "[SipProfileDb] " + str);
    }

    private boolean isChild(File file, File file2) {
        if (file == null || file2 == null) {
            return false;
        }
        if (file.equals(file2.getAbsoluteFile().getParentFile())) {
            return true;
        }
        Log.w("SIP", "isChild, file is not a child of the base dir.");
        EventLog.writeEvent(1397638484, "31530456", -1, "");
        return false;
    }
}
