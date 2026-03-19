package com.mediatek.internal.telephony;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncResult;
import android.os.Handler;
import android.os.Message;
import android.provider.Telephony;
import android.telephony.Rlog;
import com.android.internal.telephony.CommandsInterface;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.gsm.SmsCbConstants;
import com.mediatek.internal.telephony.uicc.UsimPBMemInfo;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.SortedSet;
import java.util.TreeSet;

public class CellBroadcastFwkExt {
    public static final int CB_SET_TYPE_CLOSE_ETWS_CHANNEL = 2;
    public static final int CB_SET_TYPE_NORMAL = 0;
    public static final int CB_SET_TYPE_OPEN_ETWS_CHANNEL = 1;
    private static final Uri CHANNEL_URI = Telephony.SmsCb.CbChannel.CONTENT_URI;
    private static final Uri CHANNEL_URI1 = Uri.parse("content://cb/channel1");
    private static final int EVENT_CLOSE_ETWS_CHANNEL_DONE = 3;
    private static final int EVENT_OPEN_ETWS_CHANNEL_DONE = 2;
    private static final int EVENT_QUERY_CB_CONFIG = 1;
    private static final int MAX_ETWS_NOTIFICATION = 4;
    private static final int NEXT_ACTION_NO_ACTION = 100;
    private static final int NEXT_ACTION_ONLY_ADD = 101;
    private static final int NEXT_ACTION_ONLY_REMOVE = 101;
    private static final int NEXT_ACTION_REMOVE_THEN_ADD = 102;
    private static final String TAG = "CellBroadcastFwkExt";
    private CommandsInterface mCi;
    private Context mContext;
    private ArrayList<EtwsNotification> mEtwsNotificationList;
    private Object mLock;
    private Phone mPhone;
    private int mPhoneId;
    private CellBroadcastConfigInfo mConfigInfo = null;
    private boolean mSuccess = false;
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            Rlog.d(CellBroadcastFwkExt.TAG, "receive message " + CellBroadcastFwkExt.this.idToString(msg.what));
            switch (msg.what) {
                case 1:
                    Rlog.d(CellBroadcastFwkExt.TAG, "handle EVENT_QUERY_CB_CONFIG");
                    AsyncResult ar = (AsyncResult) msg.obj;
                    if (ar.exception != null) {
                        Rlog.d(CellBroadcastFwkExt.TAG, "fail to query cb config");
                    } else {
                        CellBroadcastConfigInfo cbConfigInfo = (CellBroadcastConfigInfo) ar.result;
                        String oldChannelConfig = cbConfigInfo.channelConfigInfo;
                        CellBroadcastFwkExt.this.handleQueriedConfig(oldChannelConfig, msg.arg1, (EtwsNotification) ar.userObj);
                    }
                    break;
                case 2:
                    Rlog.d(CellBroadcastFwkExt.TAG, "handle EVENT_OPEN_ETWS_CHANNEL_DONE");
                    AsyncResult ar2 = (AsyncResult) msg.obj;
                    EtwsNotification noti = (EtwsNotification) ar2.userObj;
                    if (ar2.exception == null) {
                        Rlog.d(CellBroadcastFwkExt.TAG, "success to open cb channel " + noti.messageId);
                        int nextAction = msg.arg1;
                        if (nextAction == 101) {
                            CellBroadcastFwkExt.this.addEtwsNoti(noti);
                        } else if (nextAction == 102) {
                            CellBroadcastFwkExt.this.removeFirstEtwsNotiThenAdd(noti);
                        } else {
                            Rlog.d(CellBroadcastFwkExt.TAG, "invalid next action " + nextAction);
                        }
                        CellBroadcastFwkExt.this.updateDatabase(true);
                    } else {
                        Rlog.d(CellBroadcastFwkExt.TAG, "fail to open cb channel");
                    }
                    break;
                case 3:
                    Rlog.d(CellBroadcastFwkExt.TAG, "handle EVENT_CLOSE_ETWS_CHANNEL_DONE");
                    AsyncResult ar3 = (AsyncResult) msg.obj;
                    EtwsNotification noti2 = (EtwsNotification) ar3.userObj;
                    if (ar3.exception == null) {
                        Rlog.d(CellBroadcastFwkExt.TAG, "success to close cb channel " + noti2.messageId);
                        int nextAction2 = msg.arg1;
                        if (nextAction2 == 101) {
                            CellBroadcastFwkExt.this.removeEtwsNoti(noti2);
                        } else {
                            Rlog.d(CellBroadcastFwkExt.TAG, "invalid next action " + nextAction2);
                        }
                        CellBroadcastFwkExt.this.updateDatabase(false);
                    } else {
                        Rlog.d(CellBroadcastFwkExt.TAG, "fail to close cb channel");
                    }
                    break;
                default:
                    Rlog.d(CellBroadcastFwkExt.TAG, "unknown CB event " + msg.what);
                    break;
            }
        }
    };

    public CellBroadcastFwkExt(Phone phone) {
        this.mPhone = null;
        this.mCi = null;
        this.mContext = null;
        this.mPhoneId = 0;
        this.mLock = null;
        this.mEtwsNotificationList = null;
        if (phone == null) {
            Rlog.d(TAG, "FAIL! phone is null");
            return;
        }
        this.mPhone = phone;
        this.mCi = phone.mCi;
        this.mContext = phone.getContext();
        this.mPhoneId = phone.getPhoneId();
        this.mLock = new Object();
        this.mEtwsNotificationList = new ArrayList<>(4);
    }

    public void openEtwsChannel(EtwsNotification newEtwsNoti) {
        Rlog.d(TAG, "openEtwsChannel");
        Message response = this.mHandler.obtainMessage(1, 2, 0, newEtwsNoti);
        this.mCi.queryCellBroadcastConfigInfo(response);
    }

    public void closeEtwsChannel(EtwsNotification newEtwsNoti) {
        Rlog.d(TAG, "closeEtwsChannel");
        Message response = this.mHandler.obtainMessage(1, 3, 0, newEtwsNoti);
        this.mCi.queryCellBroadcastConfigInfo(response);
    }

    private String idToString(int id) {
        switch (id) {
            case 1:
                return "EVENT_QUERY_CB_CONFIG";
            case 2:
                return "EVENT_OPEN_ETWS_CHANNEL_DONE";
            case 3:
                return "EVENT_CLOSE_ETWS_CHANNEL_DONE";
            default:
                return "unknown message id: " + id;
        }
    }

    private SortedSet<Integer> mergeConfigList(ArrayList<Integer> oldConfigList, ArrayList<Integer> newConfigList) {
        Rlog.d(TAG, "call mergeConfigInfoList");
        SortedSet sortedConfig = new TreeSet();
        if (oldConfigList != null && oldConfigList.size() > 0) {
            Iterator i$iterator = oldConfigList.iterator();
            while (i$iterator.hasNext()) {
                int i = ((Integer) i$iterator.next()).intValue();
                sortedConfig.add(Integer.valueOf(i));
            }
        } else {
            Rlog.d(TAG, "oldConfigList is null");
        }
        if (newConfigList != null && newConfigList.size() > 0) {
            Iterator i$iterator2 = newConfigList.iterator();
            while (i$iterator2.hasNext()) {
                int i2 = ((Integer) i$iterator2.next()).intValue();
                sortedConfig.add(Integer.valueOf(i2));
            }
        } else {
            Rlog.d(TAG, "newConfigList is null");
        }
        return sortedConfig;
    }

    private SortedSet<Integer> minusConfigList(ArrayList<Integer> oldConfigList, ArrayList<Integer> newConfigList) {
        Rlog.d(TAG, "call minusConfigList");
        SortedSet sortedConfig = new TreeSet();
        if (oldConfigList == null || oldConfigList.size() == 0) {
            Rlog.d(TAG, "oldConfigList, no need to minus");
            return sortedConfig;
        }
        if (newConfigList != null && newConfigList.size() > 0) {
            Iterator i$iterator = newConfigList.iterator();
            while (i$iterator.hasNext()) {
                int i = ((Integer) i$iterator.next()).intValue();
                int j = 0;
                int n = oldConfigList.size();
                while (true) {
                    if (j >= n) {
                        break;
                    }
                    if (i != oldConfigList.get(j).intValue()) {
                        j++;
                    } else {
                        Rlog.d(TAG, "delete config: " + i);
                        oldConfigList.remove(j);
                        break;
                    }
                }
            }
        }
        Iterator i$iterator2 = oldConfigList.iterator();
        while (i$iterator2.hasNext()) {
            sortedConfig.add(Integer.valueOf(((Integer) i$iterator2.next()).intValue()));
        }
        return sortedConfig;
    }

    private ArrayList<Integer> parseConfigInfoToList(String config) {
        Rlog.d(TAG, "call parseConfigInfoToList");
        int left = 0;
        int value = 0;
        boolean meetMinus = false;
        ArrayList<Integer> ret = new ArrayList<>();
        if (config == null || config.length() == 0) {
            return ret;
        }
        if (config.length() == 1 && config.charAt(0) == ',') {
            return ret;
        }
        String temp = config + ",";
        int n = temp.length();
        for (int i = 0; i < n; i++) {
            char ch = temp.charAt(i);
            if (ch >= '0' && ch <= '9') {
                value = (value * 10) + (ch - '0');
            } else if (ch == '-') {
                meetMinus = true;
                left = value;
                value = 0;
            } else if (ch == ',') {
                if (meetMinus) {
                    int right = value;
                    for (int j = left; j <= right; j++) {
                        ret.add(Integer.valueOf(j));
                    }
                    meetMinus = false;
                } else {
                    ret.add(Integer.valueOf(value));
                }
                value = 0;
            }
        }
        return ret;
    }

    private String parseSortedSetToString(SortedSet<Integer> sortedSet) {
        Rlog.d(TAG, "call parseSortedSet");
        if (sortedSet == null || sortedSet.size() == 0) {
            Rlog.d(TAG, "sortedSet is null");
            return null;
        }
        StringBuilder ret = new StringBuilder();
        Iterator i$iterator = sortedSet.iterator();
        while (i$iterator.hasNext()) {
            int i = ((Integer) i$iterator.next()).intValue();
            ret.append(i);
            ret.append(',');
        }
        ret.deleteCharAt(ret.length() - 1);
        return ret.toString();
    }

    private void handleQueriedConfig(String config, int nextAction, EtwsNotification noti) {
        SortedSet<Integer> sortedConfig;
        Message response;
        Rlog.d(TAG, "handleQueriedConfig");
        ArrayList<Integer> oldConfigList = parseConfigInfoToList(config);
        ArrayList<Integer> newConfigList = new ArrayList<>();
        newConfigList.add(4352);
        newConfigList.add(Integer.valueOf(SmsCbConstants.MESSAGE_ID_ETWS_TSUNAMI_WARNING));
        newConfigList.add(Integer.valueOf(SmsCbConstants.MESSAGE_ID_ETWS_EARTHQUAKE_AND_TSUNAMI_WARNING));
        newConfigList.add(Integer.valueOf(SmsCbConstants.MESSAGE_ID_ETWS_TEST_MESSAGE));
        newConfigList.add(Integer.valueOf(SmsCbConstants.MESSAGE_ID_ETWS_OTHER_EMERGENCY_TYPE));
        if (nextAction == 2) {
            Rlog.d(TAG, "to open ETWS channel: " + noti.messageId);
            int size = this.mEtwsNotificationList.size();
            if (size < 4) {
                Rlog.d(TAG, "list is NOT full");
                sortedConfig = mergeConfigList(oldConfigList, newConfigList);
                response = this.mHandler.obtainMessage(nextAction, 101, 0, noti);
            } else {
                Rlog.d(TAG, "list is full");
                EtwsNotification earliestNoti = this.mEtwsNotificationList.get(0);
                int i = 0;
                while (true) {
                    if (i >= oldConfigList.size()) {
                        break;
                    }
                    int ch = oldConfigList.get(i).intValue();
                    if (ch != earliestNoti.messageId) {
                        i++;
                    } else {
                        Rlog.d(TAG, "remove channel from old config: " + earliestNoti.messageId);
                        oldConfigList.remove(i);
                        break;
                    }
                }
                sortedConfig = mergeConfigList(oldConfigList, newConfigList);
                response = this.mHandler.obtainMessage(nextAction, 102, 0, noti);
            }
            String finalConfig = parseSortedSetToString(sortedConfig);
            this.mCi.setCellBroadcastChannelConfigInfo(finalConfig, 1, response);
            return;
        }
        if (nextAction == 3) {
            Rlog.d(TAG, "to close ETWS channel: " + noti.messageId);
            SortedSet<Integer> sortedConfig2 = minusConfigList(oldConfigList, newConfigList);
            Message response2 = this.mHandler.obtainMessage(nextAction, 101, 0, noti);
            String finalConfig2 = parseSortedSetToString(sortedConfig2);
            this.mCi.setCellBroadcastChannelConfigInfo(finalConfig2, 2, response2);
            return;
        }
        Rlog.d(TAG, "invalid action: " + nextAction);
    }

    public boolean containDuplicatedEtwsNotification(EtwsNotification newEtwsNoti) {
        Rlog.d(TAG, "call containDuplicatedEtwsNotification");
        if (newEtwsNoti == null) {
            Rlog.d(TAG, "null EtwsNotification");
            return false;
        }
        for (EtwsNotification e : this.mEtwsNotificationList) {
            if (e.isDuplicatedEtws(newEtwsNoti)) {
                return true;
            }
        }
        return false;
    }

    private void addEtwsNoti(EtwsNotification noti) {
        Rlog.d(TAG, "call addEtwsNoti");
        this.mEtwsNotificationList.add(noti);
    }

    private void removeEtwsNoti(EtwsNotification noti) {
        Rlog.d(TAG, "call removeEtwsNoti");
        int count = 0;
        int i = 0;
        int n = this.mEtwsNotificationList.size();
        while (i < n) {
            EtwsNotification element = this.mEtwsNotificationList.get(i);
            if (element.messageId == noti.messageId) {
                this.mEtwsNotificationList.remove(i);
                n--;
                count++;
            } else {
                i++;
            }
        }
        Rlog.d(TAG, "remove noti " + count);
    }

    private void removeFirstEtwsNotiThenAdd(EtwsNotification noti) {
        Rlog.d(TAG, "call removeFirstEtwsNotiThenAdd");
        if (this.mEtwsNotificationList.size() >= 4) {
            this.mEtwsNotificationList.remove(0);
        }
        this.mEtwsNotificationList.add(noti);
    }

    private void updateDatabase(boolean open) {
        Rlog.d(TAG, "updateDatabase " + open);
        Uri uri = CHANNEL_URI;
        if (this.mPhoneId == 1) {
            uri = CHANNEL_URI1;
        }
        int[] Channels = {4352, SmsCbConstants.MESSAGE_ID_ETWS_TSUNAMI_WARNING, SmsCbConstants.MESSAGE_ID_ETWS_EARTHQUAKE_AND_TSUNAMI_WARNING, SmsCbConstants.MESSAGE_ID_ETWS_TEST_MESSAGE, SmsCbConstants.MESSAGE_ID_ETWS_OTHER_EMERGENCY_TYPE};
        boolean[] handled = {false, false, false, false, false};
        Cursor cursor = this.mContext.getContentResolver().query(uri, null, null, null, null);
        if (cursor != null) {
            while (cursor.moveToNext()) {
                try {
                    try {
                        int channel = cursor.getInt(cursor.getColumnIndexOrThrow("number"));
                        Rlog.d(TAG, "updateDatabase channel:" + channel);
                        if (channel >= Channels[0] && channel <= Channels[4]) {
                            int enable = cursor.getInt(cursor.getColumnIndexOrThrow(Telephony.SmsCb.CbChannel.ENABLE));
                            handled[channel - Channels[0]] = true;
                            if (enable != 1 || !open) {
                                if (enable != 0 || open) {
                                    int key = cursor.getInt(cursor.getColumnIndexOrThrow("_id"));
                                    ContentValues value = new ContentValues(1);
                                    value.put(Telephony.SmsCb.CbChannel.ENABLE, Integer.valueOf(open ? 1 : 0));
                                    this.mContext.getContentResolver().update(uri, value, "_id=" + key, null);
                                }
                            }
                        }
                    } catch (Exception ex) {
                        Rlog.e(TAG, "get channels error:", ex);
                        if (cursor == null) {
                            return;
                        }
                        cursor.close();
                        return;
                    }
                } catch (Throwable th) {
                    if (cursor != null) {
                        cursor.close();
                    }
                    throw th;
                }
            }
        }
        if (cursor != null) {
            cursor.close();
        }
        int length = handled.length;
        for (int i = 0; i < 5; i++) {
            if (!handled[i]) {
                int channel2 = i + Channels[0];
                ContentValues values = new ContentValues();
                values.put("name", UsimPBMemInfo.STRING_NOT_SET + channel2);
                values.put("number", Integer.valueOf(channel2));
                values.put(Telephony.SmsCb.CbChannel.ENABLE, Integer.valueOf(open ? 1 : 0));
                this.mContext.getContentResolver().insert(uri, values);
            }
        }
    }
}
