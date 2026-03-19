package com.android.internal.telephony.euicc;

import android.app.AppOpsManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ComponentInfo;
import android.os.Binder;
import android.os.Handler;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.service.euicc.EuiccProfileInfo;
import android.telephony.euicc.EuiccNotification;
import android.telephony.euicc.EuiccRulesAuthTable;
import android.text.TextUtils;
import android.util.Log;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.telephony.SubscriptionController;
import com.android.internal.telephony.euicc.IEuiccCardController;
import com.android.internal.telephony.uicc.UiccController;
import com.android.internal.telephony.uicc.UiccSlot;
import com.android.internal.telephony.uicc.euicc.EuiccCard;
import com.android.internal.telephony.uicc.euicc.EuiccCardErrorException;
import com.android.internal.telephony.uicc.euicc.async.AsyncResultCallback;
import java.io.FileDescriptor;
import java.io.PrintWriter;

public class EuiccCardController extends IEuiccCardController.Stub {
    private static final String KEY_LAST_BOOT_COUNT = "last_boot_count";
    private static final String TAG = "EuiccCardController";
    private static EuiccCardController sInstance;
    private AppOpsManager mAppOps;
    private ComponentInfo mBestComponent;
    private String mCallingPackage;
    private final Context mContext;
    private EuiccController mEuiccController;
    private Handler mEuiccMainThreadHandler;
    private SimSlotStatusChangedBroadcastReceiver mSimSlotStatusChangeReceiver;
    private UiccController mUiccController;

    private class SimSlotStatusChangedBroadcastReceiver extends BroadcastReceiver {
        private SimSlotStatusChangedBroadcastReceiver() {
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            if ("android.telephony.action.SIM_SLOT_STATUS_CHANGED".equals(intent.getAction())) {
                if (EuiccCardController.this.isEmbeddedSlotActivated()) {
                    EuiccCardController.this.mEuiccController.startOtaUpdatingIfNecessary();
                }
                EuiccCardController.this.mContext.unregisterReceiver(EuiccCardController.this.mSimSlotStatusChangeReceiver);
            }
        }
    }

    public static EuiccCardController init(Context context) {
        synchronized (EuiccCardController.class) {
            if (sInstance == null) {
                sInstance = new EuiccCardController(context);
            } else {
                Log.wtf(TAG, "init() called multiple times! sInstance = " + sInstance);
            }
        }
        return sInstance;
    }

    public static EuiccCardController get() {
        if (sInstance == null) {
            synchronized (EuiccCardController.class) {
                if (sInstance == null) {
                    throw new IllegalStateException("get() called before init()");
                }
            }
        }
        return sInstance;
    }

    private EuiccCardController(Context context) {
        this(context, new Handler(), EuiccController.get(), UiccController.getInstance());
        ServiceManager.addService("euicc_card_controller", this);
    }

    @VisibleForTesting(visibility = VisibleForTesting.Visibility.PRIVATE)
    public EuiccCardController(Context context, Handler handler, EuiccController euiccController, UiccController uiccController) {
        this.mContext = context;
        this.mAppOps = (AppOpsManager) context.getSystemService("appops");
        this.mEuiccMainThreadHandler = handler;
        this.mUiccController = uiccController;
        this.mEuiccController = euiccController;
        if (isBootUp(this.mContext)) {
            this.mSimSlotStatusChangeReceiver = new SimSlotStatusChangedBroadcastReceiver();
            this.mContext.registerReceiver(this.mSimSlotStatusChangeReceiver, new IntentFilter("android.telephony.action.SIM_SLOT_STATUS_CHANGED"));
        }
    }

    @VisibleForTesting(visibility = VisibleForTesting.Visibility.PRIVATE)
    public static boolean isBootUp(Context context) {
        int i = Settings.Global.getInt(context.getContentResolver(), "boot_count", -1);
        SharedPreferences defaultSharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        int i2 = defaultSharedPreferences.getInt(KEY_LAST_BOOT_COUNT, -1);
        if (i == -1 || i2 == -1 || i != i2) {
            defaultSharedPreferences.edit().putInt(KEY_LAST_BOOT_COUNT, i).apply();
            return true;
        }
        return false;
    }

    @VisibleForTesting(visibility = VisibleForTesting.Visibility.PRIVATE)
    public boolean isEmbeddedSlotActivated() {
        UiccSlot[] uiccSlots = this.mUiccController.getUiccSlots();
        if (uiccSlots == null) {
            return false;
        }
        for (UiccSlot uiccSlot : uiccSlots) {
            if (uiccSlot.isEuicc() && uiccSlot.isActive()) {
                return true;
            }
        }
        return false;
    }

    private void checkCallingPackage(String str) {
        this.mAppOps.checkPackage(Binder.getCallingUid(), str);
        this.mCallingPackage = str;
        this.mBestComponent = EuiccConnector.findBestComponent(this.mContext.getPackageManager());
        if (this.mBestComponent == null || !TextUtils.equals(this.mCallingPackage, this.mBestComponent.packageName)) {
            throw new SecurityException("The calling package can only be LPA.");
        }
    }

    private EuiccCard getEuiccCard(String str) {
        UiccController uiccController = UiccController.getInstance();
        int uiccSlotForCardId = uiccController.getUiccSlotForCardId(str);
        if (uiccSlotForCardId != -1 && uiccController.getUiccSlot(uiccSlotForCardId).isEuicc()) {
            return (EuiccCard) uiccController.getUiccCardForSlot(uiccSlotForCardId);
        }
        loge("EuiccCard is null. CardId : " + str);
        return null;
    }

    private int getResultCode(Throwable th) {
        if (th instanceof EuiccCardErrorException) {
            return ((EuiccCardErrorException) th).getErrorCode();
        }
        return -1;
    }

    public void getAllProfiles(String str, String str2, final IGetAllProfilesCallback iGetAllProfilesCallback) {
        checkCallingPackage(str);
        EuiccCard euiccCard = getEuiccCard(str2);
        if (euiccCard == null) {
            try {
                iGetAllProfilesCallback.onComplete(-2, (EuiccProfileInfo[]) null);
                return;
            } catch (RemoteException e) {
                loge("getAllProfiles callback failure.", e);
                return;
            }
        }
        euiccCard.getAllProfiles(new AsyncResultCallback<EuiccProfileInfo[]>() {
            @Override
            public void onResult(EuiccProfileInfo[] euiccProfileInfoArr) {
                try {
                    iGetAllProfilesCallback.onComplete(0, euiccProfileInfoArr);
                } catch (RemoteException e2) {
                    EuiccCardController.loge("getAllProfiles callback failure.", e2);
                }
            }

            @Override
            public void onException(Throwable th) {
                try {
                    EuiccCardController.loge("getAllProfiles callback onException: ", th);
                    iGetAllProfilesCallback.onComplete(EuiccCardController.this.getResultCode(th), (EuiccProfileInfo[]) null);
                } catch (RemoteException e2) {
                    EuiccCardController.loge("getAllProfiles callback failure.", e2);
                }
            }
        }, this.mEuiccMainThreadHandler);
    }

    public void getProfile(String str, String str2, String str3, final IGetProfileCallback iGetProfileCallback) {
        checkCallingPackage(str);
        EuiccCard euiccCard = getEuiccCard(str2);
        if (euiccCard == null) {
            try {
                iGetProfileCallback.onComplete(-2, (EuiccProfileInfo) null);
                return;
            } catch (RemoteException e) {
                loge("getProfile callback failure.", e);
                return;
            }
        }
        euiccCard.getProfile(str3, new AsyncResultCallback<EuiccProfileInfo>() {
            @Override
            public void onResult(EuiccProfileInfo euiccProfileInfo) {
                try {
                    iGetProfileCallback.onComplete(0, euiccProfileInfo);
                } catch (RemoteException e2) {
                    EuiccCardController.loge("getProfile callback failure.", e2);
                }
            }

            @Override
            public void onException(Throwable th) {
                try {
                    EuiccCardController.loge("getProfile callback onException: ", th);
                    iGetProfileCallback.onComplete(EuiccCardController.this.getResultCode(th), (EuiccProfileInfo) null);
                } catch (RemoteException e2) {
                    EuiccCardController.loge("getProfile callback failure.", e2);
                }
            }
        }, this.mEuiccMainThreadHandler);
    }

    public void disableProfile(String str, String str2, String str3, boolean z, final IDisableProfileCallback iDisableProfileCallback) {
        checkCallingPackage(str);
        EuiccCard euiccCard = getEuiccCard(str2);
        if (euiccCard == null) {
            try {
                iDisableProfileCallback.onComplete(-2);
                return;
            } catch (RemoteException e) {
                loge("disableProfile callback failure.", e);
                return;
            }
        }
        euiccCard.disableProfile(str3, z, new AsyncResultCallback<Void>() {
            @Override
            public void onResult(Void r2) {
                try {
                    iDisableProfileCallback.onComplete(0);
                } catch (RemoteException e2) {
                    EuiccCardController.loge("disableProfile callback failure.", e2);
                }
            }

            @Override
            public void onException(Throwable th) {
                try {
                    EuiccCardController.loge("disableProfile callback onException: ", th);
                    iDisableProfileCallback.onComplete(EuiccCardController.this.getResultCode(th));
                } catch (RemoteException e2) {
                    EuiccCardController.loge("disableProfile callback failure.", e2);
                }
            }
        }, this.mEuiccMainThreadHandler);
    }

    public void switchToProfile(String str, String str2, final String str3, final boolean z, final ISwitchToProfileCallback iSwitchToProfileCallback) {
        checkCallingPackage(str);
        final EuiccCard euiccCard = getEuiccCard(str2);
        if (euiccCard == null) {
            try {
                iSwitchToProfileCallback.onComplete(-2, (EuiccProfileInfo) null);
                return;
            } catch (RemoteException e) {
                loge("switchToProfile callback failure.", e);
                return;
            }
        }
        euiccCard.getProfile(str3, new AsyncResultCallback<EuiccProfileInfo>() {
            @Override
            public void onResult(final EuiccProfileInfo euiccProfileInfo) {
                euiccCard.switchToProfile(str3, z, new AsyncResultCallback<Void>() {
                    @Override
                    public void onResult(Void r3) {
                        try {
                            iSwitchToProfileCallback.onComplete(0, euiccProfileInfo);
                        } catch (RemoteException e2) {
                            EuiccCardController.loge("switchToProfile callback failure.", e2);
                        }
                    }

                    @Override
                    public void onException(Throwable th) {
                        try {
                            EuiccCardController.loge("switchToProfile callback onException: ", th);
                            iSwitchToProfileCallback.onComplete(EuiccCardController.this.getResultCode(th), euiccProfileInfo);
                        } catch (RemoteException e2) {
                            EuiccCardController.loge("switchToProfile callback failure.", e2);
                        }
                    }
                }, EuiccCardController.this.mEuiccMainThreadHandler);
            }

            @Override
            public void onException(Throwable th) {
                try {
                    EuiccCardController.loge("getProfile in switchToProfile callback onException: ", th);
                    iSwitchToProfileCallback.onComplete(EuiccCardController.this.getResultCode(th), (EuiccProfileInfo) null);
                } catch (RemoteException e2) {
                    EuiccCardController.loge("switchToProfile callback failure.", e2);
                }
            }
        }, this.mEuiccMainThreadHandler);
    }

    public void setNickname(String str, String str2, String str3, String str4, final ISetNicknameCallback iSetNicknameCallback) {
        checkCallingPackage(str);
        EuiccCard euiccCard = getEuiccCard(str2);
        if (euiccCard == null) {
            try {
                iSetNicknameCallback.onComplete(-2);
                return;
            } catch (RemoteException e) {
                loge("setNickname callback failure.", e);
                return;
            }
        }
        euiccCard.setNickname(str3, str4, new AsyncResultCallback<Void>() {
            @Override
            public void onResult(Void r2) {
                try {
                    iSetNicknameCallback.onComplete(0);
                } catch (RemoteException e2) {
                    EuiccCardController.loge("setNickname callback failure.", e2);
                }
            }

            @Override
            public void onException(Throwable th) {
                try {
                    EuiccCardController.loge("setNickname callback onException: ", th);
                    iSetNicknameCallback.onComplete(EuiccCardController.this.getResultCode(th));
                } catch (RemoteException e2) {
                    EuiccCardController.loge("setNickname callback failure.", e2);
                }
            }
        }, this.mEuiccMainThreadHandler);
    }

    public void deleteProfile(String str, String str2, String str3, final IDeleteProfileCallback iDeleteProfileCallback) {
        checkCallingPackage(str);
        EuiccCard euiccCard = getEuiccCard(str2);
        if (euiccCard == null) {
            try {
                iDeleteProfileCallback.onComplete(-2);
                return;
            } catch (RemoteException e) {
                loge("deleteProfile callback failure.", e);
                return;
            }
        }
        euiccCard.deleteProfile(str3, new AsyncResultCallback<Void>() {
            @Override
            public void onResult(Void r2) {
                Log.i(EuiccCardController.TAG, "Request subscription info list refresh after delete.");
                SubscriptionController.getInstance().requestEmbeddedSubscriptionInfoListRefresh();
                try {
                    iDeleteProfileCallback.onComplete(0);
                } catch (RemoteException e2) {
                    EuiccCardController.loge("deleteProfile callback failure.", e2);
                }
            }

            @Override
            public void onException(Throwable th) {
                try {
                    EuiccCardController.loge("deleteProfile callback onException: ", th);
                    iDeleteProfileCallback.onComplete(EuiccCardController.this.getResultCode(th));
                } catch (RemoteException e2) {
                    EuiccCardController.loge("deleteProfile callback failure.", e2);
                }
            }
        }, this.mEuiccMainThreadHandler);
    }

    public void resetMemory(String str, String str2, int i, final IResetMemoryCallback iResetMemoryCallback) {
        checkCallingPackage(str);
        EuiccCard euiccCard = getEuiccCard(str2);
        if (euiccCard == null) {
            try {
                iResetMemoryCallback.onComplete(-2);
                return;
            } catch (RemoteException e) {
                loge("resetMemory callback failure.", e);
                return;
            }
        }
        euiccCard.resetMemory(i, new AsyncResultCallback<Void>() {
            @Override
            public void onResult(Void r2) {
                Log.i(EuiccCardController.TAG, "Request subscription info list refresh after reset memory.");
                SubscriptionController.getInstance().requestEmbeddedSubscriptionInfoListRefresh();
                try {
                    iResetMemoryCallback.onComplete(0);
                } catch (RemoteException e2) {
                    EuiccCardController.loge("resetMemory callback failure.", e2);
                }
            }

            @Override
            public void onException(Throwable th) {
                try {
                    EuiccCardController.loge("resetMemory callback onException: ", th);
                    iResetMemoryCallback.onComplete(EuiccCardController.this.getResultCode(th));
                } catch (RemoteException e2) {
                    EuiccCardController.loge("resetMemory callback failure.", e2);
                }
            }
        }, this.mEuiccMainThreadHandler);
    }

    public void getDefaultSmdpAddress(String str, String str2, final IGetDefaultSmdpAddressCallback iGetDefaultSmdpAddressCallback) {
        checkCallingPackage(str);
        EuiccCard euiccCard = getEuiccCard(str2);
        if (euiccCard == null) {
            try {
                iGetDefaultSmdpAddressCallback.onComplete(-2, (String) null);
                return;
            } catch (RemoteException e) {
                loge("getDefaultSmdpAddress callback failure.", e);
                return;
            }
        }
        euiccCard.getDefaultSmdpAddress(new AsyncResultCallback<String>() {
            @Override
            public void onResult(String str3) {
                try {
                    iGetDefaultSmdpAddressCallback.onComplete(0, str3);
                } catch (RemoteException e2) {
                    EuiccCardController.loge("getDefaultSmdpAddress callback failure.", e2);
                }
            }

            @Override
            public void onException(Throwable th) {
                try {
                    EuiccCardController.loge("getDefaultSmdpAddress callback onException: ", th);
                    iGetDefaultSmdpAddressCallback.onComplete(EuiccCardController.this.getResultCode(th), (String) null);
                } catch (RemoteException e2) {
                    EuiccCardController.loge("getDefaultSmdpAddress callback failure.", e2);
                }
            }
        }, this.mEuiccMainThreadHandler);
    }

    public void getSmdsAddress(String str, String str2, final IGetSmdsAddressCallback iGetSmdsAddressCallback) {
        checkCallingPackage(str);
        EuiccCard euiccCard = getEuiccCard(str2);
        if (euiccCard == null) {
            try {
                iGetSmdsAddressCallback.onComplete(-2, (String) null);
                return;
            } catch (RemoteException e) {
                loge("getSmdsAddress callback failure.", e);
                return;
            }
        }
        euiccCard.getSmdsAddress(new AsyncResultCallback<String>() {
            @Override
            public void onResult(String str3) {
                try {
                    iGetSmdsAddressCallback.onComplete(0, str3);
                } catch (RemoteException e2) {
                    EuiccCardController.loge("getSmdsAddress callback failure.", e2);
                }
            }

            @Override
            public void onException(Throwable th) {
                try {
                    EuiccCardController.loge("getSmdsAddress callback onException: ", th);
                    iGetSmdsAddressCallback.onComplete(EuiccCardController.this.getResultCode(th), (String) null);
                } catch (RemoteException e2) {
                    EuiccCardController.loge("getSmdsAddress callback failure.", e2);
                }
            }
        }, this.mEuiccMainThreadHandler);
    }

    public void setDefaultSmdpAddress(String str, String str2, String str3, final ISetDefaultSmdpAddressCallback iSetDefaultSmdpAddressCallback) {
        checkCallingPackage(str);
        EuiccCard euiccCard = getEuiccCard(str2);
        if (euiccCard == null) {
            try {
                iSetDefaultSmdpAddressCallback.onComplete(-2);
                return;
            } catch (RemoteException e) {
                loge("setDefaultSmdpAddress callback failure.", e);
                return;
            }
        }
        euiccCard.setDefaultSmdpAddress(str3, new AsyncResultCallback<Void>() {
            @Override
            public void onResult(Void r2) {
                try {
                    iSetDefaultSmdpAddressCallback.onComplete(0);
                } catch (RemoteException e2) {
                    EuiccCardController.loge("setDefaultSmdpAddress callback failure.", e2);
                }
            }

            @Override
            public void onException(Throwable th) {
                try {
                    EuiccCardController.loge("setDefaultSmdpAddress callback onException: ", th);
                    iSetDefaultSmdpAddressCallback.onComplete(EuiccCardController.this.getResultCode(th));
                } catch (RemoteException e2) {
                    EuiccCardController.loge("setDefaultSmdpAddress callback failure.", e2);
                }
            }
        }, this.mEuiccMainThreadHandler);
    }

    public void getRulesAuthTable(String str, String str2, final IGetRulesAuthTableCallback iGetRulesAuthTableCallback) {
        checkCallingPackage(str);
        EuiccCard euiccCard = getEuiccCard(str2);
        if (euiccCard == null) {
            try {
                iGetRulesAuthTableCallback.onComplete(-2, (EuiccRulesAuthTable) null);
                return;
            } catch (RemoteException e) {
                loge("getRulesAuthTable callback failure.", e);
                return;
            }
        }
        euiccCard.getRulesAuthTable(new AsyncResultCallback<EuiccRulesAuthTable>() {
            @Override
            public void onResult(EuiccRulesAuthTable euiccRulesAuthTable) {
                try {
                    iGetRulesAuthTableCallback.onComplete(0, euiccRulesAuthTable);
                } catch (RemoteException e2) {
                    EuiccCardController.loge("getRulesAuthTable callback failure.", e2);
                }
            }

            @Override
            public void onException(Throwable th) {
                try {
                    EuiccCardController.loge("getRulesAuthTable callback onException: ", th);
                    iGetRulesAuthTableCallback.onComplete(EuiccCardController.this.getResultCode(th), (EuiccRulesAuthTable) null);
                } catch (RemoteException e2) {
                    EuiccCardController.loge("getRulesAuthTable callback failure.", e2);
                }
            }
        }, this.mEuiccMainThreadHandler);
    }

    public void getEuiccChallenge(String str, String str2, final IGetEuiccChallengeCallback iGetEuiccChallengeCallback) {
        checkCallingPackage(str);
        EuiccCard euiccCard = getEuiccCard(str2);
        if (euiccCard == null) {
            try {
                iGetEuiccChallengeCallback.onComplete(-2, (byte[]) null);
                return;
            } catch (RemoteException e) {
                loge("getEuiccChallenge callback failure.", e);
                return;
            }
        }
        euiccCard.getEuiccChallenge(new AsyncResultCallback<byte[]>() {
            @Override
            public void onResult(byte[] bArr) {
                try {
                    iGetEuiccChallengeCallback.onComplete(0, bArr);
                } catch (RemoteException e2) {
                    EuiccCardController.loge("getEuiccChallenge callback failure.", e2);
                }
            }

            @Override
            public void onException(Throwable th) {
                try {
                    EuiccCardController.loge("getEuiccChallenge callback onException: ", th);
                    iGetEuiccChallengeCallback.onComplete(EuiccCardController.this.getResultCode(th), (byte[]) null);
                } catch (RemoteException e2) {
                    EuiccCardController.loge("getEuiccChallenge callback failure.", e2);
                }
            }
        }, this.mEuiccMainThreadHandler);
    }

    public void getEuiccInfo1(String str, String str2, final IGetEuiccInfo1Callback iGetEuiccInfo1Callback) {
        checkCallingPackage(str);
        EuiccCard euiccCard = getEuiccCard(str2);
        if (euiccCard == null) {
            try {
                iGetEuiccInfo1Callback.onComplete(-2, (byte[]) null);
                return;
            } catch (RemoteException e) {
                loge("getEuiccInfo1 callback failure.", e);
                return;
            }
        }
        euiccCard.getEuiccInfo1(new AsyncResultCallback<byte[]>() {
            @Override
            public void onResult(byte[] bArr) {
                try {
                    iGetEuiccInfo1Callback.onComplete(0, bArr);
                } catch (RemoteException e2) {
                    EuiccCardController.loge("getEuiccInfo1 callback failure.", e2);
                }
            }

            @Override
            public void onException(Throwable th) {
                try {
                    EuiccCardController.loge("getEuiccInfo1 callback onException: ", th);
                    iGetEuiccInfo1Callback.onComplete(EuiccCardController.this.getResultCode(th), (byte[]) null);
                } catch (RemoteException e2) {
                    EuiccCardController.loge("getEuiccInfo1 callback failure.", e2);
                }
            }
        }, this.mEuiccMainThreadHandler);
    }

    public void getEuiccInfo2(String str, String str2, final IGetEuiccInfo2Callback iGetEuiccInfo2Callback) {
        checkCallingPackage(str);
        EuiccCard euiccCard = getEuiccCard(str2);
        if (euiccCard == null) {
            try {
                iGetEuiccInfo2Callback.onComplete(-2, (byte[]) null);
                return;
            } catch (RemoteException e) {
                loge("getEuiccInfo2 callback failure.", e);
                return;
            }
        }
        euiccCard.getEuiccInfo2(new AsyncResultCallback<byte[]>() {
            @Override
            public void onResult(byte[] bArr) {
                try {
                    iGetEuiccInfo2Callback.onComplete(0, bArr);
                } catch (RemoteException e2) {
                    EuiccCardController.loge("getEuiccInfo2 callback failure.", e2);
                }
            }

            @Override
            public void onException(Throwable th) {
                try {
                    EuiccCardController.loge("getEuiccInfo2 callback onException: ", th);
                    iGetEuiccInfo2Callback.onComplete(EuiccCardController.this.getResultCode(th), (byte[]) null);
                } catch (RemoteException e2) {
                    EuiccCardController.loge("getEuiccInfo2 callback failure.", e2);
                }
            }
        }, this.mEuiccMainThreadHandler);
    }

    public void authenticateServer(String str, String str2, String str3, byte[] bArr, byte[] bArr2, byte[] bArr3, byte[] bArr4, final IAuthenticateServerCallback iAuthenticateServerCallback) {
        checkCallingPackage(str);
        EuiccCard euiccCard = getEuiccCard(str2);
        if (euiccCard == null) {
            try {
                iAuthenticateServerCallback.onComplete(-2, (byte[]) null);
                return;
            } catch (RemoteException e) {
                loge("authenticateServer callback failure.", e);
                return;
            }
        }
        euiccCard.authenticateServer(str3, bArr, bArr2, bArr3, bArr4, new AsyncResultCallback<byte[]>() {
            @Override
            public void onResult(byte[] bArr5) {
                try {
                    iAuthenticateServerCallback.onComplete(0, bArr5);
                } catch (RemoteException e2) {
                    EuiccCardController.loge("authenticateServer callback failure.", e2);
                }
            }

            @Override
            public void onException(Throwable th) {
                try {
                    EuiccCardController.loge("authenticateServer callback onException: ", th);
                    iAuthenticateServerCallback.onComplete(EuiccCardController.this.getResultCode(th), (byte[]) null);
                } catch (RemoteException e2) {
                    EuiccCardController.loge("authenticateServer callback failure.", e2);
                }
            }
        }, this.mEuiccMainThreadHandler);
    }

    public void prepareDownload(String str, String str2, byte[] bArr, byte[] bArr2, byte[] bArr3, byte[] bArr4, final IPrepareDownloadCallback iPrepareDownloadCallback) {
        checkCallingPackage(str);
        EuiccCard euiccCard = getEuiccCard(str2);
        if (euiccCard == null) {
            try {
                iPrepareDownloadCallback.onComplete(-2, (byte[]) null);
                return;
            } catch (RemoteException e) {
                loge("prepareDownload callback failure.", e);
                return;
            }
        }
        euiccCard.prepareDownload(bArr, bArr2, bArr3, bArr4, new AsyncResultCallback<byte[]>() {
            @Override
            public void onResult(byte[] bArr5) {
                try {
                    iPrepareDownloadCallback.onComplete(0, bArr5);
                } catch (RemoteException e2) {
                    EuiccCardController.loge("prepareDownload callback failure.", e2);
                }
            }

            @Override
            public void onException(Throwable th) {
                try {
                    EuiccCardController.loge("prepareDownload callback onException: ", th);
                    iPrepareDownloadCallback.onComplete(EuiccCardController.this.getResultCode(th), (byte[]) null);
                } catch (RemoteException e2) {
                    EuiccCardController.loge("prepareDownload callback failure.", e2);
                }
            }
        }, this.mEuiccMainThreadHandler);
    }

    public void loadBoundProfilePackage(String str, String str2, byte[] bArr, final ILoadBoundProfilePackageCallback iLoadBoundProfilePackageCallback) {
        checkCallingPackage(str);
        EuiccCard euiccCard = getEuiccCard(str2);
        if (euiccCard == null) {
            try {
                iLoadBoundProfilePackageCallback.onComplete(-2, (byte[]) null);
                return;
            } catch (RemoteException e) {
                loge("loadBoundProfilePackage callback failure.", e);
                return;
            }
        }
        euiccCard.loadBoundProfilePackage(bArr, new AsyncResultCallback<byte[]>() {
            @Override
            public void onResult(byte[] bArr2) {
                Log.i(EuiccCardController.TAG, "Request subscription info list refresh after install.");
                SubscriptionController.getInstance().requestEmbeddedSubscriptionInfoListRefresh();
                try {
                    iLoadBoundProfilePackageCallback.onComplete(0, bArr2);
                } catch (RemoteException e2) {
                    EuiccCardController.loge("loadBoundProfilePackage callback failure.", e2);
                }
            }

            @Override
            public void onException(Throwable th) {
                try {
                    EuiccCardController.loge("loadBoundProfilePackage callback onException: ", th);
                    iLoadBoundProfilePackageCallback.onComplete(EuiccCardController.this.getResultCode(th), (byte[]) null);
                } catch (RemoteException e2) {
                    EuiccCardController.loge("loadBoundProfilePackage callback failure.", e2);
                }
            }
        }, this.mEuiccMainThreadHandler);
    }

    public void cancelSession(String str, String str2, byte[] bArr, int i, final ICancelSessionCallback iCancelSessionCallback) {
        checkCallingPackage(str);
        EuiccCard euiccCard = getEuiccCard(str2);
        if (euiccCard == null) {
            try {
                iCancelSessionCallback.onComplete(-2, (byte[]) null);
                return;
            } catch (RemoteException e) {
                loge("cancelSession callback failure.", e);
                return;
            }
        }
        euiccCard.cancelSession(bArr, i, new AsyncResultCallback<byte[]>() {
            @Override
            public void onResult(byte[] bArr2) {
                try {
                    iCancelSessionCallback.onComplete(0, bArr2);
                } catch (RemoteException e2) {
                    EuiccCardController.loge("cancelSession callback failure.", e2);
                }
            }

            @Override
            public void onException(Throwable th) {
                try {
                    EuiccCardController.loge("cancelSession callback onException: ", th);
                    iCancelSessionCallback.onComplete(EuiccCardController.this.getResultCode(th), (byte[]) null);
                } catch (RemoteException e2) {
                    EuiccCardController.loge("cancelSession callback failure.", e2);
                }
            }
        }, this.mEuiccMainThreadHandler);
    }

    public void listNotifications(String str, String str2, int i, final IListNotificationsCallback iListNotificationsCallback) {
        checkCallingPackage(str);
        EuiccCard euiccCard = getEuiccCard(str2);
        if (euiccCard == null) {
            try {
                iListNotificationsCallback.onComplete(-2, (EuiccNotification[]) null);
                return;
            } catch (RemoteException e) {
                loge("listNotifications callback failure.", e);
                return;
            }
        }
        euiccCard.listNotifications(i, new AsyncResultCallback<EuiccNotification[]>() {
            @Override
            public void onResult(EuiccNotification[] euiccNotificationArr) {
                try {
                    iListNotificationsCallback.onComplete(0, euiccNotificationArr);
                } catch (RemoteException e2) {
                    EuiccCardController.loge("listNotifications callback failure.", e2);
                }
            }

            @Override
            public void onException(Throwable th) {
                try {
                    EuiccCardController.loge("listNotifications callback onException: ", th);
                    iListNotificationsCallback.onComplete(EuiccCardController.this.getResultCode(th), (EuiccNotification[]) null);
                } catch (RemoteException e2) {
                    EuiccCardController.loge("listNotifications callback failure.", e2);
                }
            }
        }, this.mEuiccMainThreadHandler);
    }

    public void retrieveNotificationList(String str, String str2, int i, final IRetrieveNotificationListCallback iRetrieveNotificationListCallback) {
        checkCallingPackage(str);
        EuiccCard euiccCard = getEuiccCard(str2);
        if (euiccCard == null) {
            try {
                iRetrieveNotificationListCallback.onComplete(-2, (EuiccNotification[]) null);
                return;
            } catch (RemoteException e) {
                loge("retrieveNotificationList callback failure.", e);
                return;
            }
        }
        euiccCard.retrieveNotificationList(i, new AsyncResultCallback<EuiccNotification[]>() {
            @Override
            public void onResult(EuiccNotification[] euiccNotificationArr) {
                try {
                    iRetrieveNotificationListCallback.onComplete(0, euiccNotificationArr);
                } catch (RemoteException e2) {
                    EuiccCardController.loge("retrieveNotificationList callback failure.", e2);
                }
            }

            @Override
            public void onException(Throwable th) {
                try {
                    EuiccCardController.loge("retrieveNotificationList callback onException: ", th);
                    iRetrieveNotificationListCallback.onComplete(EuiccCardController.this.getResultCode(th), (EuiccNotification[]) null);
                } catch (RemoteException e2) {
                    EuiccCardController.loge("retrieveNotificationList callback failure.", e2);
                }
            }
        }, this.mEuiccMainThreadHandler);
    }

    public void retrieveNotification(String str, String str2, int i, final IRetrieveNotificationCallback iRetrieveNotificationCallback) {
        checkCallingPackage(str);
        EuiccCard euiccCard = getEuiccCard(str2);
        if (euiccCard == null) {
            try {
                iRetrieveNotificationCallback.onComplete(-2, (EuiccNotification) null);
                return;
            } catch (RemoteException e) {
                loge("retrieveNotification callback failure.", e);
                return;
            }
        }
        euiccCard.retrieveNotification(i, new AsyncResultCallback<EuiccNotification>() {
            @Override
            public void onResult(EuiccNotification euiccNotification) {
                try {
                    iRetrieveNotificationCallback.onComplete(0, euiccNotification);
                } catch (RemoteException e2) {
                    EuiccCardController.loge("retrieveNotification callback failure.", e2);
                }
            }

            @Override
            public void onException(Throwable th) {
                try {
                    EuiccCardController.loge("retrieveNotification callback onException: ", th);
                    iRetrieveNotificationCallback.onComplete(EuiccCardController.this.getResultCode(th), (EuiccNotification) null);
                } catch (RemoteException e2) {
                    EuiccCardController.loge("retrieveNotification callback failure.", e2);
                }
            }
        }, this.mEuiccMainThreadHandler);
    }

    public void removeNotificationFromList(String str, String str2, int i, final IRemoveNotificationFromListCallback iRemoveNotificationFromListCallback) {
        checkCallingPackage(str);
        EuiccCard euiccCard = getEuiccCard(str2);
        if (euiccCard == null) {
            try {
                iRemoveNotificationFromListCallback.onComplete(-2);
                return;
            } catch (RemoteException e) {
                loge("removeNotificationFromList callback failure.", e);
                return;
            }
        }
        euiccCard.removeNotificationFromList(i, new AsyncResultCallback<Void>() {
            @Override
            public void onResult(Void r2) {
                try {
                    iRemoveNotificationFromListCallback.onComplete(0);
                } catch (RemoteException e2) {
                    EuiccCardController.loge("removeNotificationFromList callback failure.", e2);
                }
            }

            @Override
            public void onException(Throwable th) {
                try {
                    EuiccCardController.loge("removeNotificationFromList callback onException: ", th);
                    iRemoveNotificationFromListCallback.onComplete(EuiccCardController.this.getResultCode(th));
                } catch (RemoteException e2) {
                    EuiccCardController.loge("removeNotificationFromList callback failure.", e2);
                }
            }
        }, this.mEuiccMainThreadHandler);
    }

    public void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.DUMP", "Requires DUMP");
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        super.dump(fileDescriptor, printWriter, strArr);
        printWriter.println("mCallingPackage=" + this.mCallingPackage);
        printWriter.println("mBestComponent=" + this.mBestComponent);
        Binder.restoreCallingIdentity(jClearCallingIdentity);
    }

    private static void loge(String str) {
        Log.e(TAG, str);
    }

    private static void loge(String str, Throwable th) {
        Log.e(TAG, str, th);
    }
}
