package com.android.internal.telephony;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Registrant;
import android.os.RegistrantList;
import android.os.RemoteException;
import android.telephony.Rlog;
import android.util.LocalLog;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.telephony.IOnSubscriptionsChangedListener;
import java.io.FileDescriptor;
import java.io.PrintWriter;

public class SubscriptionMonitor {
    private static final String LOG_TAG = "SubscriptionMonitor";
    private static final int MAX_LOGLINES = 100;
    private static final boolean VDBG = true;
    private final Context mContext;
    private int mDefaultDataPhoneId;
    private final RegistrantList[] mDefaultDataSubChangedRegistrants;
    private int mDefaultDataSubId;
    private final BroadcastReceiver mDefaultDataSubscriptionChangedReceiver;
    private final LocalLog mLocalLog;
    private final Object mLock;
    private final int[] mPhoneSubId;
    private final SubscriptionController mSubscriptionController;
    private final IOnSubscriptionsChangedListener mSubscriptionsChangedListener;
    private final RegistrantList[] mSubscriptionsChangedRegistrants;

    public SubscriptionMonitor(ITelephonyRegistry iTelephonyRegistry, Context context, SubscriptionController subscriptionController, int i) {
        this.mLock = new Object();
        this.mLocalLog = new LocalLog(100);
        this.mSubscriptionsChangedListener = new IOnSubscriptionsChangedListener.Stub() {
            public void onSubscriptionsChanged() {
                synchronized (SubscriptionMonitor.this.mLock) {
                    int i2 = -1;
                    for (int i3 = 0; i3 < SubscriptionMonitor.this.mPhoneSubId.length; i3++) {
                        int subIdUsingPhoneId = SubscriptionMonitor.this.mSubscriptionController.getSubIdUsingPhoneId(i3);
                        int i4 = SubscriptionMonitor.this.mPhoneSubId[i3];
                        if (i4 == subIdUsingPhoneId) {
                            if (subIdUsingPhoneId != SubscriptionMonitor.this.mDefaultDataSubId) {
                            }
                        } else {
                            SubscriptionMonitor.this.log("Phone[" + i3 + "] subId changed " + i4 + "->" + subIdUsingPhoneId + ", " + SubscriptionMonitor.this.mSubscriptionsChangedRegistrants[i3].size() + " registrants");
                            SubscriptionMonitor.this.mPhoneSubId[i3] = subIdUsingPhoneId;
                            SubscriptionMonitor.this.mSubscriptionsChangedRegistrants[i3].notifyRegistrants();
                            if (SubscriptionMonitor.this.mDefaultDataSubId != -1) {
                                if (subIdUsingPhoneId == SubscriptionMonitor.this.mDefaultDataSubId || i4 == SubscriptionMonitor.this.mDefaultDataSubId) {
                                    SubscriptionMonitor.this.log("mDefaultDataSubId = " + SubscriptionMonitor.this.mDefaultDataSubId + ", " + SubscriptionMonitor.this.mDefaultDataSubChangedRegistrants[i3].size() + " registrants");
                                    SubscriptionMonitor.this.mDefaultDataSubChangedRegistrants[i3].notifyRegistrants();
                                }
                                if (subIdUsingPhoneId != SubscriptionMonitor.this.mDefaultDataSubId) {
                                    i2 = i3;
                                }
                            }
                        }
                    }
                    SubscriptionMonitor.this.mDefaultDataPhoneId = i2;
                }
            }
        };
        this.mDefaultDataSubscriptionChangedReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context2, Intent intent) {
                int size;
                int defaultDataSubId = SubscriptionMonitor.this.mSubscriptionController.getDefaultDataSubId();
                synchronized (SubscriptionMonitor.this.mLock) {
                    if (SubscriptionMonitor.this.mDefaultDataSubId != defaultDataSubId) {
                        SubscriptionMonitor.this.log("Default changed " + SubscriptionMonitor.this.mDefaultDataSubId + "->" + defaultDataSubId);
                        int unused = SubscriptionMonitor.this.mDefaultDataSubId;
                        int i2 = SubscriptionMonitor.this.mDefaultDataPhoneId;
                        SubscriptionMonitor.this.mDefaultDataSubId = defaultDataSubId;
                        int phoneId = SubscriptionMonitor.this.mSubscriptionController.getPhoneId(-1);
                        int size2 = 0;
                        if (defaultDataSubId != -1) {
                            int i3 = 0;
                            while (true) {
                                if (i3 >= SubscriptionMonitor.this.mPhoneSubId.length) {
                                    break;
                                }
                                if (SubscriptionMonitor.this.mPhoneSubId[i3] != defaultDataSubId) {
                                    i3++;
                                } else {
                                    SubscriptionMonitor.this.log("newDefaultDataPhoneId=" + i3);
                                    phoneId = i3;
                                    break;
                                }
                            }
                        }
                        if (phoneId != i2) {
                            SubscriptionMonitor subscriptionMonitor = SubscriptionMonitor.this;
                            StringBuilder sb = new StringBuilder();
                            sb.append("Default phoneId changed ");
                            sb.append(i2);
                            sb.append("->");
                            sb.append(phoneId);
                            sb.append(", ");
                            if (!SubscriptionMonitor.this.invalidPhoneId(i2)) {
                                size = SubscriptionMonitor.this.mDefaultDataSubChangedRegistrants[i2].size();
                            } else {
                                size = 0;
                            }
                            sb.append(size);
                            sb.append(",");
                            if (!SubscriptionMonitor.this.invalidPhoneId(phoneId)) {
                                size2 = SubscriptionMonitor.this.mDefaultDataSubChangedRegistrants[phoneId].size();
                            }
                            sb.append(size2);
                            sb.append(" registrants");
                            subscriptionMonitor.log(sb.toString());
                            SubscriptionMonitor.this.mDefaultDataPhoneId = phoneId;
                            if (!SubscriptionMonitor.this.invalidPhoneId(i2)) {
                                SubscriptionMonitor.this.mDefaultDataSubChangedRegistrants[i2].notifyRegistrants();
                            }
                            if (!SubscriptionMonitor.this.invalidPhoneId(phoneId)) {
                                SubscriptionMonitor.this.mDefaultDataSubChangedRegistrants[phoneId].notifyRegistrants();
                            }
                        }
                    }
                }
            }
        };
        try {
            iTelephonyRegistry.addOnSubscriptionsChangedListener(context.getOpPackageName(), this.mSubscriptionsChangedListener);
        } catch (RemoteException e) {
        }
        this.mSubscriptionController = subscriptionController;
        this.mContext = context;
        this.mSubscriptionsChangedRegistrants = new RegistrantList[i];
        this.mDefaultDataSubChangedRegistrants = new RegistrantList[i];
        this.mPhoneSubId = new int[i];
        this.mDefaultDataSubId = this.mSubscriptionController.getDefaultDataSubId();
        this.mDefaultDataPhoneId = this.mSubscriptionController.getPhoneId(this.mDefaultDataSubId);
        for (int i2 = 0; i2 < i; i2++) {
            this.mSubscriptionsChangedRegistrants[i2] = new RegistrantList();
            this.mDefaultDataSubChangedRegistrants[i2] = new RegistrantList();
            this.mPhoneSubId[i2] = this.mSubscriptionController.getSubIdUsingPhoneId(i2);
        }
        this.mContext.registerReceiver(this.mDefaultDataSubscriptionChangedReceiver, new IntentFilter("android.intent.action.ACTION_DEFAULT_DATA_SUBSCRIPTION_CHANGED"));
    }

    @VisibleForTesting
    public SubscriptionMonitor() {
        this.mLock = new Object();
        this.mLocalLog = new LocalLog(100);
        this.mSubscriptionsChangedListener = new IOnSubscriptionsChangedListener.Stub() {
            public void onSubscriptionsChanged() {
                synchronized (SubscriptionMonitor.this.mLock) {
                    int i2 = -1;
                    for (int i3 = 0; i3 < SubscriptionMonitor.this.mPhoneSubId.length; i3++) {
                        int subIdUsingPhoneId = SubscriptionMonitor.this.mSubscriptionController.getSubIdUsingPhoneId(i3);
                        int i4 = SubscriptionMonitor.this.mPhoneSubId[i3];
                        if (i4 == subIdUsingPhoneId) {
                            if (subIdUsingPhoneId != SubscriptionMonitor.this.mDefaultDataSubId) {
                            }
                        } else {
                            SubscriptionMonitor.this.log("Phone[" + i3 + "] subId changed " + i4 + "->" + subIdUsingPhoneId + ", " + SubscriptionMonitor.this.mSubscriptionsChangedRegistrants[i3].size() + " registrants");
                            SubscriptionMonitor.this.mPhoneSubId[i3] = subIdUsingPhoneId;
                            SubscriptionMonitor.this.mSubscriptionsChangedRegistrants[i3].notifyRegistrants();
                            if (SubscriptionMonitor.this.mDefaultDataSubId != -1) {
                                if (subIdUsingPhoneId == SubscriptionMonitor.this.mDefaultDataSubId || i4 == SubscriptionMonitor.this.mDefaultDataSubId) {
                                    SubscriptionMonitor.this.log("mDefaultDataSubId = " + SubscriptionMonitor.this.mDefaultDataSubId + ", " + SubscriptionMonitor.this.mDefaultDataSubChangedRegistrants[i3].size() + " registrants");
                                    SubscriptionMonitor.this.mDefaultDataSubChangedRegistrants[i3].notifyRegistrants();
                                }
                                if (subIdUsingPhoneId != SubscriptionMonitor.this.mDefaultDataSubId) {
                                    i2 = i3;
                                }
                            }
                        }
                    }
                    SubscriptionMonitor.this.mDefaultDataPhoneId = i2;
                }
            }
        };
        this.mDefaultDataSubscriptionChangedReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context2, Intent intent) {
                int size;
                int defaultDataSubId = SubscriptionMonitor.this.mSubscriptionController.getDefaultDataSubId();
                synchronized (SubscriptionMonitor.this.mLock) {
                    if (SubscriptionMonitor.this.mDefaultDataSubId != defaultDataSubId) {
                        SubscriptionMonitor.this.log("Default changed " + SubscriptionMonitor.this.mDefaultDataSubId + "->" + defaultDataSubId);
                        int unused = SubscriptionMonitor.this.mDefaultDataSubId;
                        int i2 = SubscriptionMonitor.this.mDefaultDataPhoneId;
                        SubscriptionMonitor.this.mDefaultDataSubId = defaultDataSubId;
                        int phoneId = SubscriptionMonitor.this.mSubscriptionController.getPhoneId(-1);
                        int size2 = 0;
                        if (defaultDataSubId != -1) {
                            int i3 = 0;
                            while (true) {
                                if (i3 >= SubscriptionMonitor.this.mPhoneSubId.length) {
                                    break;
                                }
                                if (SubscriptionMonitor.this.mPhoneSubId[i3] != defaultDataSubId) {
                                    i3++;
                                } else {
                                    SubscriptionMonitor.this.log("newDefaultDataPhoneId=" + i3);
                                    phoneId = i3;
                                    break;
                                }
                            }
                        }
                        if (phoneId != i2) {
                            SubscriptionMonitor subscriptionMonitor = SubscriptionMonitor.this;
                            StringBuilder sb = new StringBuilder();
                            sb.append("Default phoneId changed ");
                            sb.append(i2);
                            sb.append("->");
                            sb.append(phoneId);
                            sb.append(", ");
                            if (!SubscriptionMonitor.this.invalidPhoneId(i2)) {
                                size = SubscriptionMonitor.this.mDefaultDataSubChangedRegistrants[i2].size();
                            } else {
                                size = 0;
                            }
                            sb.append(size);
                            sb.append(",");
                            if (!SubscriptionMonitor.this.invalidPhoneId(phoneId)) {
                                size2 = SubscriptionMonitor.this.mDefaultDataSubChangedRegistrants[phoneId].size();
                            }
                            sb.append(size2);
                            sb.append(" registrants");
                            subscriptionMonitor.log(sb.toString());
                            SubscriptionMonitor.this.mDefaultDataPhoneId = phoneId;
                            if (!SubscriptionMonitor.this.invalidPhoneId(i2)) {
                                SubscriptionMonitor.this.mDefaultDataSubChangedRegistrants[i2].notifyRegistrants();
                            }
                            if (!SubscriptionMonitor.this.invalidPhoneId(phoneId)) {
                                SubscriptionMonitor.this.mDefaultDataSubChangedRegistrants[phoneId].notifyRegistrants();
                            }
                        }
                    }
                }
            }
        };
        this.mSubscriptionsChangedRegistrants = null;
        this.mDefaultDataSubChangedRegistrants = null;
        this.mSubscriptionController = null;
        this.mContext = null;
        this.mPhoneSubId = null;
    }

    public void registerForSubscriptionChanged(int i, Handler handler, int i2, Object obj) {
        if (invalidPhoneId(i)) {
            throw new IllegalArgumentException("Invalid PhoneId");
        }
        Registrant registrant = new Registrant(handler, i2, obj);
        this.mSubscriptionsChangedRegistrants[i].add(registrant);
        registrant.notifyRegistrant();
    }

    public void unregisterForSubscriptionChanged(int i, Handler handler) {
        if (invalidPhoneId(i)) {
            throw new IllegalArgumentException("Invalid PhoneId");
        }
        this.mSubscriptionsChangedRegistrants[i].remove(handler);
    }

    public void registerForDefaultDataSubscriptionChanged(int i, Handler handler, int i2, Object obj) {
        if (invalidPhoneId(i)) {
            throw new IllegalArgumentException("Invalid PhoneId");
        }
        Registrant registrant = new Registrant(handler, i2, obj);
        this.mDefaultDataSubChangedRegistrants[i].add(registrant);
        registrant.notifyRegistrant();
    }

    public void unregisterForDefaultDataSubscriptionChanged(int i, Handler handler) {
        if (invalidPhoneId(i)) {
            throw new IllegalArgumentException("Invalid PhoneId");
        }
        this.mDefaultDataSubChangedRegistrants[i].remove(handler);
    }

    private boolean invalidPhoneId(int i) {
        return i < 0 || i >= this.mPhoneSubId.length;
    }

    private void log(String str) {
        Rlog.d(LOG_TAG, str);
        this.mLocalLog.log(str);
    }

    public void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
        synchronized (this.mLock) {
            this.mLocalLog.dump(fileDescriptor, printWriter, strArr);
        }
    }
}
