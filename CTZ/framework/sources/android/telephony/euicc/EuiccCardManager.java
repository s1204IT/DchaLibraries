package android.telephony.euicc;

import android.annotation.SystemApi;
import android.content.Context;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.service.euicc.EuiccProfileInfo;
import android.util.Log;
import com.android.internal.telephony.euicc.IAuthenticateServerCallback;
import com.android.internal.telephony.euicc.ICancelSessionCallback;
import com.android.internal.telephony.euicc.IDeleteProfileCallback;
import com.android.internal.telephony.euicc.IDisableProfileCallback;
import com.android.internal.telephony.euicc.IEuiccCardController;
import com.android.internal.telephony.euicc.IGetAllProfilesCallback;
import com.android.internal.telephony.euicc.IGetDefaultSmdpAddressCallback;
import com.android.internal.telephony.euicc.IGetEuiccChallengeCallback;
import com.android.internal.telephony.euicc.IGetEuiccInfo1Callback;
import com.android.internal.telephony.euicc.IGetEuiccInfo2Callback;
import com.android.internal.telephony.euicc.IGetProfileCallback;
import com.android.internal.telephony.euicc.IGetRulesAuthTableCallback;
import com.android.internal.telephony.euicc.IGetSmdsAddressCallback;
import com.android.internal.telephony.euicc.IListNotificationsCallback;
import com.android.internal.telephony.euicc.ILoadBoundProfilePackageCallback;
import com.android.internal.telephony.euicc.IPrepareDownloadCallback;
import com.android.internal.telephony.euicc.IRemoveNotificationFromListCallback;
import com.android.internal.telephony.euicc.IResetMemoryCallback;
import com.android.internal.telephony.euicc.IRetrieveNotificationCallback;
import com.android.internal.telephony.euicc.IRetrieveNotificationListCallback;
import com.android.internal.telephony.euicc.ISetDefaultSmdpAddressCallback;
import com.android.internal.telephony.euicc.ISetNicknameCallback;
import com.android.internal.telephony.euicc.ISwitchToProfileCallback;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.concurrent.Executor;

@SystemApi
public class EuiccCardManager {
    public static final int CANCEL_REASON_END_USER_REJECTED = 0;
    public static final int CANCEL_REASON_POSTPONED = 1;
    public static final int CANCEL_REASON_PPR_NOT_ALLOWED = 3;
    public static final int CANCEL_REASON_TIMEOUT = 2;
    public static final int RESET_OPTION_DELETE_FIELD_LOADED_TEST_PROFILES = 2;
    public static final int RESET_OPTION_DELETE_OPERATIONAL_PROFILES = 1;
    public static final int RESET_OPTION_RESET_DEFAULT_SMDP_ADDRESS = 4;
    public static final int RESULT_EUICC_NOT_FOUND = -2;
    public static final int RESULT_OK = 0;
    public static final int RESULT_UNKNOWN_ERROR = -1;
    private static final String TAG = "EuiccCardManager";
    private final Context mContext;

    @Retention(RetentionPolicy.SOURCE)
    public @interface CancelReason {
    }

    @Retention(RetentionPolicy.SOURCE)
    public @interface ResetOption {
    }

    public interface ResultCallback<T> {
        void onComplete(int i, T t);
    }

    public EuiccCardManager(Context context) {
        this.mContext = context;
    }

    private IEuiccCardController getIEuiccCardController() {
        return IEuiccCardController.Stub.asInterface(ServiceManager.getService("euicc_card_controller"));
    }

    class AnonymousClass1 extends IGetAllProfilesCallback.Stub {
        final ResultCallback val$callback;
        final Executor val$executor;

        AnonymousClass1(Executor executor, ResultCallback resultCallback) {
            this.val$executor = executor;
            this.val$callback = resultCallback;
        }

        @Override
        public void onComplete(final int i, final EuiccProfileInfo[] euiccProfileInfoArr) {
            Executor executor = this.val$executor;
            final ResultCallback resultCallback = this.val$callback;
            executor.execute(new Runnable() {
                @Override
                public final void run() {
                    resultCallback.onComplete(i, euiccProfileInfoArr);
                }
            });
        }
    }

    public void requestAllProfiles(String str, Executor executor, ResultCallback<EuiccProfileInfo[]> resultCallback) {
        try {
            getIEuiccCardController().getAllProfiles(this.mContext.getOpPackageName(), str, new AnonymousClass1(executor, resultCallback));
        } catch (RemoteException e) {
            Log.e(TAG, "Error calling getAllProfiles", e);
            throw e.rethrowFromSystemServer();
        }
    }

    class AnonymousClass2 extends IGetProfileCallback.Stub {
        final ResultCallback val$callback;
        final Executor val$executor;

        AnonymousClass2(Executor executor, ResultCallback resultCallback) {
            this.val$executor = executor;
            this.val$callback = resultCallback;
        }

        @Override
        public void onComplete(final int i, final EuiccProfileInfo euiccProfileInfo) {
            Executor executor = this.val$executor;
            final ResultCallback resultCallback = this.val$callback;
            executor.execute(new Runnable() {
                @Override
                public final void run() {
                    resultCallback.onComplete(i, euiccProfileInfo);
                }
            });
        }
    }

    public void requestProfile(String str, String str2, Executor executor, ResultCallback<EuiccProfileInfo> resultCallback) {
        try {
            getIEuiccCardController().getProfile(this.mContext.getOpPackageName(), str, str2, new AnonymousClass2(executor, resultCallback));
        } catch (RemoteException e) {
            Log.e(TAG, "Error calling getProfile", e);
            throw e.rethrowFromSystemServer();
        }
    }

    class AnonymousClass3 extends IDisableProfileCallback.Stub {
        final ResultCallback val$callback;
        final Executor val$executor;

        AnonymousClass3(Executor executor, ResultCallback resultCallback) {
            this.val$executor = executor;
            this.val$callback = resultCallback;
        }

        @Override
        public void onComplete(final int i) {
            Executor executor = this.val$executor;
            final ResultCallback resultCallback = this.val$callback;
            executor.execute(new Runnable() {
                @Override
                public final void run() {
                    resultCallback.onComplete(i, null);
                }
            });
        }
    }

    public void disableProfile(String str, String str2, boolean z, Executor executor, ResultCallback<Void> resultCallback) {
        try {
            getIEuiccCardController().disableProfile(this.mContext.getOpPackageName(), str, str2, z, new AnonymousClass3(executor, resultCallback));
        } catch (RemoteException e) {
            Log.e(TAG, "Error calling disableProfile", e);
            throw e.rethrowFromSystemServer();
        }
    }

    class AnonymousClass4 extends ISwitchToProfileCallback.Stub {
        final ResultCallback val$callback;
        final Executor val$executor;

        AnonymousClass4(Executor executor, ResultCallback resultCallback) {
            this.val$executor = executor;
            this.val$callback = resultCallback;
        }

        @Override
        public void onComplete(final int i, final EuiccProfileInfo euiccProfileInfo) {
            Executor executor = this.val$executor;
            final ResultCallback resultCallback = this.val$callback;
            executor.execute(new Runnable() {
                @Override
                public final void run() {
                    resultCallback.onComplete(i, euiccProfileInfo);
                }
            });
        }
    }

    public void switchToProfile(String str, String str2, boolean z, Executor executor, ResultCallback<EuiccProfileInfo> resultCallback) {
        try {
            getIEuiccCardController().switchToProfile(this.mContext.getOpPackageName(), str, str2, z, new AnonymousClass4(executor, resultCallback));
        } catch (RemoteException e) {
            Log.e(TAG, "Error calling switchToProfile", e);
            throw e.rethrowFromSystemServer();
        }
    }

    class AnonymousClass5 extends ISetNicknameCallback.Stub {
        final ResultCallback val$callback;
        final Executor val$executor;

        AnonymousClass5(Executor executor, ResultCallback resultCallback) {
            this.val$executor = executor;
            this.val$callback = resultCallback;
        }

        @Override
        public void onComplete(final int i) {
            Executor executor = this.val$executor;
            final ResultCallback resultCallback = this.val$callback;
            executor.execute(new Runnable() {
                @Override
                public final void run() {
                    resultCallback.onComplete(i, null);
                }
            });
        }
    }

    public void setNickname(String str, String str2, String str3, Executor executor, ResultCallback<Void> resultCallback) {
        try {
            getIEuiccCardController().setNickname(this.mContext.getOpPackageName(), str, str2, str3, new AnonymousClass5(executor, resultCallback));
        } catch (RemoteException e) {
            Log.e(TAG, "Error calling setNickname", e);
            throw e.rethrowFromSystemServer();
        }
    }

    class AnonymousClass6 extends IDeleteProfileCallback.Stub {
        final ResultCallback val$callback;
        final Executor val$executor;

        AnonymousClass6(Executor executor, ResultCallback resultCallback) {
            this.val$executor = executor;
            this.val$callback = resultCallback;
        }

        @Override
        public void onComplete(final int i) {
            Executor executor = this.val$executor;
            final ResultCallback resultCallback = this.val$callback;
            executor.execute(new Runnable() {
                @Override
                public final void run() {
                    resultCallback.onComplete(i, null);
                }
            });
        }
    }

    public void deleteProfile(String str, String str2, Executor executor, ResultCallback<Void> resultCallback) {
        try {
            getIEuiccCardController().deleteProfile(this.mContext.getOpPackageName(), str, str2, new AnonymousClass6(executor, resultCallback));
        } catch (RemoteException e) {
            Log.e(TAG, "Error calling deleteProfile", e);
            throw e.rethrowFromSystemServer();
        }
    }

    class AnonymousClass7 extends IResetMemoryCallback.Stub {
        final ResultCallback val$callback;
        final Executor val$executor;

        AnonymousClass7(Executor executor, ResultCallback resultCallback) {
            this.val$executor = executor;
            this.val$callback = resultCallback;
        }

        @Override
        public void onComplete(final int i) {
            Executor executor = this.val$executor;
            final ResultCallback resultCallback = this.val$callback;
            executor.execute(new Runnable() {
                @Override
                public final void run() {
                    resultCallback.onComplete(i, null);
                }
            });
        }
    }

    public void resetMemory(String str, int i, Executor executor, ResultCallback<Void> resultCallback) {
        try {
            getIEuiccCardController().resetMemory(this.mContext.getOpPackageName(), str, i, new AnonymousClass7(executor, resultCallback));
        } catch (RemoteException e) {
            Log.e(TAG, "Error calling resetMemory", e);
            throw e.rethrowFromSystemServer();
        }
    }

    class AnonymousClass8 extends IGetDefaultSmdpAddressCallback.Stub {
        final ResultCallback val$callback;
        final Executor val$executor;

        AnonymousClass8(Executor executor, ResultCallback resultCallback) {
            this.val$executor = executor;
            this.val$callback = resultCallback;
        }

        @Override
        public void onComplete(final int i, final String str) {
            Executor executor = this.val$executor;
            final ResultCallback resultCallback = this.val$callback;
            executor.execute(new Runnable() {
                @Override
                public final void run() {
                    resultCallback.onComplete(i, str);
                }
            });
        }
    }

    public void requestDefaultSmdpAddress(String str, Executor executor, ResultCallback<String> resultCallback) {
        try {
            getIEuiccCardController().getDefaultSmdpAddress(this.mContext.getOpPackageName(), str, new AnonymousClass8(executor, resultCallback));
        } catch (RemoteException e) {
            Log.e(TAG, "Error calling getDefaultSmdpAddress", e);
            throw e.rethrowFromSystemServer();
        }
    }

    class AnonymousClass9 extends IGetSmdsAddressCallback.Stub {
        final ResultCallback val$callback;
        final Executor val$executor;

        AnonymousClass9(Executor executor, ResultCallback resultCallback) {
            this.val$executor = executor;
            this.val$callback = resultCallback;
        }

        @Override
        public void onComplete(final int i, final String str) {
            Executor executor = this.val$executor;
            final ResultCallback resultCallback = this.val$callback;
            executor.execute(new Runnable() {
                @Override
                public final void run() {
                    resultCallback.onComplete(i, str);
                }
            });
        }
    }

    public void requestSmdsAddress(String str, Executor executor, ResultCallback<String> resultCallback) {
        try {
            getIEuiccCardController().getSmdsAddress(this.mContext.getOpPackageName(), str, new AnonymousClass9(executor, resultCallback));
        } catch (RemoteException e) {
            Log.e(TAG, "Error calling getSmdsAddress", e);
            throw e.rethrowFromSystemServer();
        }
    }

    public void setDefaultSmdpAddress(String str, String str2, Executor executor, ResultCallback<Void> resultCallback) {
        try {
            getIEuiccCardController().setDefaultSmdpAddress(this.mContext.getOpPackageName(), str, str2, new AnonymousClass10(executor, resultCallback));
        } catch (RemoteException e) {
            Log.e(TAG, "Error calling setDefaultSmdpAddress", e);
            throw e.rethrowFromSystemServer();
        }
    }

    class AnonymousClass10 extends ISetDefaultSmdpAddressCallback.Stub {
        final ResultCallback val$callback;
        final Executor val$executor;

        AnonymousClass10(Executor executor, ResultCallback resultCallback) {
            this.val$executor = executor;
            this.val$callback = resultCallback;
        }

        @Override
        public void onComplete(final int i) {
            Executor executor = this.val$executor;
            final ResultCallback resultCallback = this.val$callback;
            executor.execute(new Runnable() {
                @Override
                public final void run() {
                    resultCallback.onComplete(i, null);
                }
            });
        }
    }

    class AnonymousClass11 extends IGetRulesAuthTableCallback.Stub {
        final ResultCallback val$callback;
        final Executor val$executor;

        AnonymousClass11(Executor executor, ResultCallback resultCallback) {
            this.val$executor = executor;
            this.val$callback = resultCallback;
        }

        @Override
        public void onComplete(final int i, final EuiccRulesAuthTable euiccRulesAuthTable) {
            Executor executor = this.val$executor;
            final ResultCallback resultCallback = this.val$callback;
            executor.execute(new Runnable() {
                @Override
                public final void run() {
                    resultCallback.onComplete(i, euiccRulesAuthTable);
                }
            });
        }
    }

    public void requestRulesAuthTable(String str, Executor executor, ResultCallback<EuiccRulesAuthTable> resultCallback) {
        try {
            getIEuiccCardController().getRulesAuthTable(this.mContext.getOpPackageName(), str, new AnonymousClass11(executor, resultCallback));
        } catch (RemoteException e) {
            Log.e(TAG, "Error calling getRulesAuthTable", e);
            throw e.rethrowFromSystemServer();
        }
    }

    class AnonymousClass12 extends IGetEuiccChallengeCallback.Stub {
        final ResultCallback val$callback;
        final Executor val$executor;

        AnonymousClass12(Executor executor, ResultCallback resultCallback) {
            this.val$executor = executor;
            this.val$callback = resultCallback;
        }

        @Override
        public void onComplete(final int i, final byte[] bArr) {
            Executor executor = this.val$executor;
            final ResultCallback resultCallback = this.val$callback;
            executor.execute(new Runnable() {
                @Override
                public final void run() {
                    resultCallback.onComplete(i, bArr);
                }
            });
        }
    }

    public void requestEuiccChallenge(String str, Executor executor, ResultCallback<byte[]> resultCallback) {
        try {
            getIEuiccCardController().getEuiccChallenge(this.mContext.getOpPackageName(), str, new AnonymousClass12(executor, resultCallback));
        } catch (RemoteException e) {
            Log.e(TAG, "Error calling getEuiccChallenge", e);
            throw e.rethrowFromSystemServer();
        }
    }

    class AnonymousClass13 extends IGetEuiccInfo1Callback.Stub {
        final ResultCallback val$callback;
        final Executor val$executor;

        AnonymousClass13(Executor executor, ResultCallback resultCallback) {
            this.val$executor = executor;
            this.val$callback = resultCallback;
        }

        @Override
        public void onComplete(final int i, final byte[] bArr) {
            Executor executor = this.val$executor;
            final ResultCallback resultCallback = this.val$callback;
            executor.execute(new Runnable() {
                @Override
                public final void run() {
                    resultCallback.onComplete(i, bArr);
                }
            });
        }
    }

    public void requestEuiccInfo1(String str, Executor executor, ResultCallback<byte[]> resultCallback) {
        try {
            getIEuiccCardController().getEuiccInfo1(this.mContext.getOpPackageName(), str, new AnonymousClass13(executor, resultCallback));
        } catch (RemoteException e) {
            Log.e(TAG, "Error calling getEuiccInfo1", e);
            throw e.rethrowFromSystemServer();
        }
    }

    class AnonymousClass14 extends IGetEuiccInfo2Callback.Stub {
        final ResultCallback val$callback;
        final Executor val$executor;

        AnonymousClass14(Executor executor, ResultCallback resultCallback) {
            this.val$executor = executor;
            this.val$callback = resultCallback;
        }

        @Override
        public void onComplete(final int i, final byte[] bArr) {
            Executor executor = this.val$executor;
            final ResultCallback resultCallback = this.val$callback;
            executor.execute(new Runnable() {
                @Override
                public final void run() {
                    resultCallback.onComplete(i, bArr);
                }
            });
        }
    }

    public void requestEuiccInfo2(String str, Executor executor, ResultCallback<byte[]> resultCallback) {
        try {
            getIEuiccCardController().getEuiccInfo2(this.mContext.getOpPackageName(), str, new AnonymousClass14(executor, resultCallback));
        } catch (RemoteException e) {
            Log.e(TAG, "Error calling getEuiccInfo2", e);
            throw e.rethrowFromSystemServer();
        }
    }

    public void authenticateServer(String str, String str2, byte[] bArr, byte[] bArr2, byte[] bArr3, byte[] bArr4, Executor executor, ResultCallback<byte[]> resultCallback) {
        try {
            getIEuiccCardController().authenticateServer(this.mContext.getOpPackageName(), str, str2, bArr, bArr2, bArr3, bArr4, new AnonymousClass15(executor, resultCallback));
        } catch (RemoteException e) {
            Log.e(TAG, "Error calling authenticateServer", e);
            throw e.rethrowFromSystemServer();
        }
    }

    class AnonymousClass15 extends IAuthenticateServerCallback.Stub {
        final ResultCallback val$callback;
        final Executor val$executor;

        AnonymousClass15(Executor executor, ResultCallback resultCallback) {
            this.val$executor = executor;
            this.val$callback = resultCallback;
        }

        @Override
        public void onComplete(final int i, final byte[] bArr) {
            Executor executor = this.val$executor;
            final ResultCallback resultCallback = this.val$callback;
            executor.execute(new Runnable() {
                @Override
                public final void run() {
                    resultCallback.onComplete(i, bArr);
                }
            });
        }
    }

    public void prepareDownload(String str, byte[] bArr, byte[] bArr2, byte[] bArr3, byte[] bArr4, Executor executor, ResultCallback<byte[]> resultCallback) {
        try {
            getIEuiccCardController().prepareDownload(this.mContext.getOpPackageName(), str, bArr, bArr2, bArr3, bArr4, new AnonymousClass16(executor, resultCallback));
        } catch (RemoteException e) {
            Log.e(TAG, "Error calling prepareDownload", e);
            throw e.rethrowFromSystemServer();
        }
    }

    class AnonymousClass16 extends IPrepareDownloadCallback.Stub {
        final ResultCallback val$callback;
        final Executor val$executor;

        AnonymousClass16(Executor executor, ResultCallback resultCallback) {
            this.val$executor = executor;
            this.val$callback = resultCallback;
        }

        @Override
        public void onComplete(final int i, final byte[] bArr) {
            Executor executor = this.val$executor;
            final ResultCallback resultCallback = this.val$callback;
            executor.execute(new Runnable() {
                @Override
                public final void run() {
                    resultCallback.onComplete(i, bArr);
                }
            });
        }
    }

    public void loadBoundProfilePackage(String str, byte[] bArr, Executor executor, ResultCallback<byte[]> resultCallback) {
        try {
            getIEuiccCardController().loadBoundProfilePackage(this.mContext.getOpPackageName(), str, bArr, new AnonymousClass17(executor, resultCallback));
        } catch (RemoteException e) {
            Log.e(TAG, "Error calling loadBoundProfilePackage", e);
            throw e.rethrowFromSystemServer();
        }
    }

    class AnonymousClass17 extends ILoadBoundProfilePackageCallback.Stub {
        final ResultCallback val$callback;
        final Executor val$executor;

        AnonymousClass17(Executor executor, ResultCallback resultCallback) {
            this.val$executor = executor;
            this.val$callback = resultCallback;
        }

        @Override
        public void onComplete(final int i, final byte[] bArr) {
            Executor executor = this.val$executor;
            final ResultCallback resultCallback = this.val$callback;
            executor.execute(new Runnable() {
                @Override
                public final void run() {
                    resultCallback.onComplete(i, bArr);
                }
            });
        }
    }

    public void cancelSession(String str, byte[] bArr, int i, Executor executor, ResultCallback<byte[]> resultCallback) {
        try {
            getIEuiccCardController().cancelSession(this.mContext.getOpPackageName(), str, bArr, i, new AnonymousClass18(executor, resultCallback));
        } catch (RemoteException e) {
            Log.e(TAG, "Error calling cancelSession", e);
            throw e.rethrowFromSystemServer();
        }
    }

    class AnonymousClass18 extends ICancelSessionCallback.Stub {
        final ResultCallback val$callback;
        final Executor val$executor;

        AnonymousClass18(Executor executor, ResultCallback resultCallback) {
            this.val$executor = executor;
            this.val$callback = resultCallback;
        }

        @Override
        public void onComplete(final int i, final byte[] bArr) {
            Executor executor = this.val$executor;
            final ResultCallback resultCallback = this.val$callback;
            executor.execute(new Runnable() {
                @Override
                public final void run() {
                    resultCallback.onComplete(i, bArr);
                }
            });
        }
    }

    class AnonymousClass19 extends IListNotificationsCallback.Stub {
        final ResultCallback val$callback;
        final Executor val$executor;

        AnonymousClass19(Executor executor, ResultCallback resultCallback) {
            this.val$executor = executor;
            this.val$callback = resultCallback;
        }

        @Override
        public void onComplete(final int i, final EuiccNotification[] euiccNotificationArr) {
            Executor executor = this.val$executor;
            final ResultCallback resultCallback = this.val$callback;
            executor.execute(new Runnable() {
                @Override
                public final void run() {
                    resultCallback.onComplete(i, euiccNotificationArr);
                }
            });
        }
    }

    public void listNotifications(String str, int i, Executor executor, ResultCallback<EuiccNotification[]> resultCallback) {
        try {
            getIEuiccCardController().listNotifications(this.mContext.getOpPackageName(), str, i, new AnonymousClass19(executor, resultCallback));
        } catch (RemoteException e) {
            Log.e(TAG, "Error calling listNotifications", e);
            throw e.rethrowFromSystemServer();
        }
    }

    class AnonymousClass20 extends IRetrieveNotificationListCallback.Stub {
        final ResultCallback val$callback;
        final Executor val$executor;

        AnonymousClass20(Executor executor, ResultCallback resultCallback) {
            this.val$executor = executor;
            this.val$callback = resultCallback;
        }

        @Override
        public void onComplete(final int i, final EuiccNotification[] euiccNotificationArr) {
            Executor executor = this.val$executor;
            final ResultCallback resultCallback = this.val$callback;
            executor.execute(new Runnable() {
                @Override
                public final void run() {
                    resultCallback.onComplete(i, euiccNotificationArr);
                }
            });
        }
    }

    public void retrieveNotificationList(String str, int i, Executor executor, ResultCallback<EuiccNotification[]> resultCallback) {
        try {
            getIEuiccCardController().retrieveNotificationList(this.mContext.getOpPackageName(), str, i, new AnonymousClass20(executor, resultCallback));
        } catch (RemoteException e) {
            Log.e(TAG, "Error calling retrieveNotificationList", e);
            throw e.rethrowFromSystemServer();
        }
    }

    class AnonymousClass21 extends IRetrieveNotificationCallback.Stub {
        final ResultCallback val$callback;
        final Executor val$executor;

        AnonymousClass21(Executor executor, ResultCallback resultCallback) {
            this.val$executor = executor;
            this.val$callback = resultCallback;
        }

        @Override
        public void onComplete(final int i, final EuiccNotification euiccNotification) {
            Executor executor = this.val$executor;
            final ResultCallback resultCallback = this.val$callback;
            executor.execute(new Runnable() {
                @Override
                public final void run() {
                    resultCallback.onComplete(i, euiccNotification);
                }
            });
        }
    }

    public void retrieveNotification(String str, int i, Executor executor, ResultCallback<EuiccNotification> resultCallback) {
        try {
            getIEuiccCardController().retrieveNotification(this.mContext.getOpPackageName(), str, i, new AnonymousClass21(executor, resultCallback));
        } catch (RemoteException e) {
            Log.e(TAG, "Error calling retrieveNotification", e);
            throw e.rethrowFromSystemServer();
        }
    }

    public void removeNotificationFromList(String str, int i, Executor executor, ResultCallback<Void> resultCallback) {
        try {
            getIEuiccCardController().removeNotificationFromList(this.mContext.getOpPackageName(), str, i, new AnonymousClass22(executor, resultCallback));
        } catch (RemoteException e) {
            Log.e(TAG, "Error calling removeNotificationFromList", e);
            throw e.rethrowFromSystemServer();
        }
    }

    class AnonymousClass22 extends IRemoveNotificationFromListCallback.Stub {
        final ResultCallback val$callback;
        final Executor val$executor;

        AnonymousClass22(Executor executor, ResultCallback resultCallback) {
            this.val$executor = executor;
            this.val$callback = resultCallback;
        }

        @Override
        public void onComplete(final int i) {
            Executor executor = this.val$executor;
            final ResultCallback resultCallback = this.val$callback;
            executor.execute(new Runnable() {
                @Override
                public final void run() {
                    resultCallback.onComplete(i, null);
                }
            });
        }
    }
}
