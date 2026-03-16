package com.android.services.telephony.sip;

import android.content.Context;
import android.net.sip.SipProfile;
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
    private int mProfilesCount = -1;
    private String mProfilesDirectory;
    private SipSharedPreferences mSipSharedPreferences;

    public SipProfileDb(Context context) {
        this.mProfilesDirectory = context.getFilesDir().getAbsolutePath() + "/profiles/";
        this.mSipSharedPreferences = new SipSharedPreferences(context);
    }

    public void deleteProfile(SipProfile p) throws IOException {
        synchronized (SipProfileDb.class) {
            File profileFile = new File(this.mProfilesDirectory, p.getProfileName());
            if (!isChild(new File(this.mProfilesDirectory), profileFile)) {
                throw new IOException("Invalid Profile Credentials!");
            }
            deleteProfile(profileFile);
            if (this.mProfilesCount < 0) {
                retrieveSipProfileListInternal();
            }
            SipSharedPreferences sipSharedPreferences = this.mSipSharedPreferences;
            int i = this.mProfilesCount - 1;
            this.mProfilesCount = i;
            sipSharedPreferences.setProfilesCount(i);
        }
    }

    private void deleteProfile(File file) {
        if (file.isDirectory()) {
            File[] arr$ = file.listFiles();
            for (File child : arr$) {
                deleteProfile(child);
            }
        }
        file.delete();
    }

    public void saveProfile(SipProfile p) throws IOException {
        ObjectOutputStream oos;
        synchronized (SipProfileDb.class) {
            if (this.mProfilesCount < 0) {
                retrieveSipProfileListInternal();
            }
            File f = new File(this.mProfilesDirectory, p.getProfileName());
            if (!isChild(new File(this.mProfilesDirectory), f)) {
                throw new IOException("Invalid Profile Credentials!");
            }
            if (!f.exists()) {
                f.mkdirs();
            }
            AtomicFile atomicFile = new AtomicFile(new File(f, ".pobj"));
            FileOutputStream fos = null;
            ObjectOutputStream oos2 = null;
            try {
                try {
                    fos = atomicFile.startWrite();
                    oos = new ObjectOutputStream(fos);
                } catch (IOException e) {
                    e = e;
                }
            } catch (Throwable th) {
                th = th;
            }
            try {
                oos.writeObject(p);
                oos.flush();
                SipSharedPreferences sipSharedPreferences = this.mSipSharedPreferences;
                int i = this.mProfilesCount + 1;
                this.mProfilesCount = i;
                sipSharedPreferences.setProfilesCount(i);
                atomicFile.finishWrite(fos);
                if (oos != null) {
                    oos.close();
                }
            } catch (IOException e2) {
                e = e2;
                oos2 = oos;
                atomicFile.failWrite(fos);
                throw e;
            } catch (Throwable th2) {
                th = th2;
                oos2 = oos;
                if (oos2 != null) {
                    oos2.close();
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
        List<SipProfile> sipProfileList = Collections.synchronizedList(new ArrayList());
        File root = new File(this.mProfilesDirectory);
        String[] dirs = root.list();
        if (dirs != null) {
            for (String dir : dirs) {
                File f = new File(new File(root, dir), ".pobj");
                if (f.exists()) {
                    try {
                        SipProfile p = deserialize(f);
                        if (p != null && dir.equals(p.getProfileName())) {
                            sipProfileList.add(p);
                        }
                    } catch (IOException e) {
                        log("retrieveSipProfileListInternal, exception: " + e);
                    }
                }
            }
            this.mProfilesCount = sipProfileList.size();
            this.mSipSharedPreferences.setProfilesCount(this.mProfilesCount);
        }
        return sipProfileList;
    }

    private SipProfile deserialize(File profileObjectFile) throws Throwable {
        SipProfile p;
        ObjectInputStream ois;
        AtomicFile atomicFile = new AtomicFile(profileObjectFile);
        ObjectInputStream ois2 = null;
        try {
            try {
                ois = new ObjectInputStream(atomicFile.openRead());
            } catch (Throwable th) {
                th = th;
            }
        } catch (ClassNotFoundException e) {
            e = e;
        }
        try {
            p = (SipProfile) ois.readObject();
            if (ois != null) {
                ois.close();
            }
            ois2 = ois;
        } catch (ClassNotFoundException e2) {
            e = e2;
            ois2 = ois;
            log("deserialize, exception: " + e);
            if (ois2 != null) {
                ois2.close();
            }
            p = null;
        } catch (Throwable th2) {
            th = th2;
            ois2 = ois;
            if (ois2 != null) {
                ois2.close();
            }
            throw th;
        }
        return p;
    }

    private static void log(String msg) {
        Log.d("SIP", "[SipProfileDb] " + msg);
    }

    private boolean isChild(File base, File file) {
        if (base == null || file == null) {
            return false;
        }
        if (base.equals(file.getAbsoluteFile().getParentFile())) {
            return true;
        }
        Log.w("SIP", "isChild, file is not a child of the base dir.");
        EventLog.writeEvent(1397638484, "31530456", -1, "");
        return false;
    }
}
