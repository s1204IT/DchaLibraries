package com.mediatek.internal.telephony.uicc;

import android.content.Context;
import android.telephony.Rlog;
import com.android.internal.telephony.Phone;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;

public final class SmsbcIccFileFetcher extends IccFileFetcherBase {
    private static final String BCSMSCFG = "ef_bcsmscfg";
    private static final String BCSMSP = "ef_bcsmsp";
    private static final String BCSMSTABLE = "ef_bcsmstable";
    private static final String TAG = "SmsbcIccFileFetcher";
    ArrayList<String> mFileList;

    public SmsbcIccFileFetcher(Context c, Phone phone) {
        super(c, phone);
        this.mFileList = new ArrayList<>();
        this.mFileList.add(BCSMSCFG);
        this.mFileList.add(BCSMSTABLE);
        this.mFileList.add(BCSMSP);
    }

    @Override
    public ArrayList<String> onGetKeys() {
        return this.mFileList;
    }

    @Override
    public IccFileRequest onGetFilePara(String key) {
        if (!key.equals(BCSMSCFG)) {
            if (!key.equals(BCSMSTABLE)) {
                if (key.equals(BCSMSP)) {
                    return new IccFileRequest(28510, 0, 2, "3F007F25", null, -1, null);
                }
                return null;
            }
            return new IccFileRequest(28509, 0, 2, "3F007F25", null, -1, null);
        }
        return new IccFileRequest(28507, 1, 2, "3F007F25", null, -1, null);
    }

    @Override
    public void onParseResult(String key, byte[] transparent, ArrayList<byte[]> linearfixed) {
        if (BCSMSCFG.equals(key)) {
            Rlog.d(TAG, "BCSMSCFG = " + Arrays.toString(transparent));
            this.mData.put(BCSMSCFG, transparent);
            return;
        }
        if (BCSMSTABLE.equals(key)) {
            Rlog.d(TAG, "BCSMSTABLE = " + linearfixed);
            if (linearfixed == null || linearfixed.size() <= 0) {
                return;
            }
            HashMap<String, byte[]> tables = new HashMap<>();
            for (int i = 0; i < linearfixed.size(); i++) {
                byte[] item = linearfixed.get(i);
                if (item != null && item.length > 0) {
                    Rlog.d(TAG, "BCSMSTABLE = " + Arrays.toString(item));
                    int status = item[0] & 1;
                    if (status == 1) {
                        tables.put(String.valueOf(i), item);
                    }
                }
            }
            this.mData.put(BCSMSTABLE, tables);
            return;
        }
        if (!BCSMSP.equals(key)) {
            return;
        }
        Rlog.d(TAG, "BCSMSP = " + linearfixed);
        if (linearfixed == null || linearfixed.size() <= 0) {
            return;
        }
        HashMap<String, byte[]> parameters = new HashMap<>();
        for (int i2 = 0; i2 < linearfixed.size(); i2++) {
            byte[] item2 = linearfixed.get(i2);
            if (item2 != null && item2.length > 0) {
                Rlog.d(TAG, "BCSMSP = " + Arrays.toString(item2));
                int select = item2[0] & 1;
                if (select == 1) {
                    parameters.put(String.valueOf(i2), item2);
                }
            }
        }
        this.mData.put(BCSMSP, parameters);
    }

    public int getBcsmsCfgFromRuim(int userServiceCategory, int userPriorityIndicator) {
        int ret = 2;
        byte[] cfg = (byte[]) this.mData.get(BCSMSCFG);
        if (cfg == null || cfg.length < 1) {
            return -1;
        }
        byte config = cfg[0];
        Rlog.d(TAG, "getBcsmsCfgFromRuim config = " + ((int) config));
        if (config != -1) {
            ret = config & 3;
        }
        if (ret != 1) {
            return ret;
        }
        HashMap<String, byte[]> tables = (HashMap) this.mData.get(BCSMSTABLE);
        HashMap<String, byte[]> parameters = (HashMap) this.mData.get(BCSMSP);
        if (tables == null || parameters == null) {
            return -1;
        }
        Iterator i$iterator = tables.keySet().iterator();
        while (true) {
            if (!i$iterator.hasNext()) {
                break;
            }
            String i = (String) i$iterator.next();
            byte[] t = tables.get(i);
            byte[] p = parameters.get(i);
            if (t != null && p != null) {
                int status = t[0] & 1;
                int select = p[0] & 1;
                Rlog.d(TAG, "getBcsmsCfgFromRuim status=" + status + " select=" + select);
                if (status == 1 && select == 1) {
                    byte ch1 = t[1];
                    byte ch2 = t[2];
                    int serviceCategory = (ch1 << 8) + ch2;
                    Rlog.d(TAG, "getBcsmsCfgFromRuim serviceCategory=" + serviceCategory);
                    Rlog.d(TAG, "userServiceCategory=" + userServiceCategory);
                    if (serviceCategory == userServiceCategory) {
                        int priority = p[1] & 3;
                        Rlog.d(TAG, "getBcsmsCfgFromRuim priority=" + priority);
                        if (userPriorityIndicator >= priority) {
                            ret = 2;
                        } else {
                            ret = 0;
                        }
                    }
                }
            }
        }
        if (ret == 1) {
            return -1;
        }
        return ret;
    }
}
