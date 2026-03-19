package com.android.server.autofill.ui;

import android.content.ComponentName;
import android.content.Context;
import android.content.IntentSender;
import android.graphics.drawable.Drawable;
import android.metrics.LogMaker;
import android.net.util.NetworkConstants;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.service.autofill.Dataset;
import android.service.autofill.FillResponse;
import android.service.autofill.SaveInfo;
import android.service.autofill.ValueFinder;
import android.text.TextUtils;
import android.util.Slog;
import android.view.KeyEvent;
import android.view.autofill.AutofillId;
import android.view.autofill.IAutofillWindowPresenter;
import android.widget.Toast;
import com.android.internal.logging.MetricsLogger;
import com.android.server.UiThread;
import com.android.server.autofill.Helper;
import com.android.server.autofill.ui.FillUi;
import com.android.server.autofill.ui.SaveUi;
import java.io.PrintWriter;

public final class AutoFillUI {
    private static final String TAG = "AutofillUI";
    private AutoFillUiCallback mCallback;
    private final Context mContext;
    private FillUi mFillUi;
    private final Handler mHandler = UiThread.getHandler();
    private final MetricsLogger mMetricsLogger = new MetricsLogger();
    private final OverlayControl mOverlayControl;
    private SaveUi mSaveUi;

    public interface AutoFillUiCallback {
        void authenticate(int i, int i2, IntentSender intentSender, Bundle bundle);

        void cancelSave();

        void dispatchUnhandledKey(AutofillId autofillId, KeyEvent keyEvent);

        void fill(int i, int i2, Dataset dataset);

        void requestHideFillUi(AutofillId autofillId);

        void requestShowFillUi(AutofillId autofillId, int i, int i2, IAutofillWindowPresenter iAutofillWindowPresenter);

        void save();

        void startIntentSender(IntentSender intentSender);
    }

    public AutoFillUI(Context context) {
        this.mContext = context;
        this.mOverlayControl = new OverlayControl(context);
    }

    public void setCallback(final AutoFillUiCallback autoFillUiCallback) {
        this.mHandler.post(new Runnable() {
            @Override
            public final void run() {
                AutoFillUI.lambda$setCallback$0(this.f$0, autoFillUiCallback);
            }
        });
    }

    public static void lambda$setCallback$0(AutoFillUI autoFillUI, AutoFillUiCallback autoFillUiCallback) {
        if (autoFillUI.mCallback != autoFillUiCallback) {
            if (autoFillUI.mCallback != null) {
                autoFillUI.hideAllUiThread(autoFillUI.mCallback);
            }
            autoFillUI.mCallback = autoFillUiCallback;
        }
    }

    public void clearCallback(final AutoFillUiCallback autoFillUiCallback) {
        this.mHandler.post(new Runnable() {
            @Override
            public final void run() {
                AutoFillUI.lambda$clearCallback$1(this.f$0, autoFillUiCallback);
            }
        });
    }

    public static void lambda$clearCallback$1(AutoFillUI autoFillUI, AutoFillUiCallback autoFillUiCallback) {
        if (autoFillUI.mCallback == autoFillUiCallback) {
            autoFillUI.hideAllUiThread(autoFillUiCallback);
            autoFillUI.mCallback = null;
        }
    }

    public void showError(int i, AutoFillUiCallback autoFillUiCallback) {
        showError(this.mContext.getString(i), autoFillUiCallback);
    }

    public void showError(final CharSequence charSequence, final AutoFillUiCallback autoFillUiCallback) {
        Slog.w(TAG, "showError(): " + ((Object) charSequence));
        this.mHandler.post(new Runnable() {
            @Override
            public final void run() {
                AutoFillUI.lambda$showError$2(this.f$0, autoFillUiCallback, charSequence);
            }
        });
    }

    public static void lambda$showError$2(AutoFillUI autoFillUI, AutoFillUiCallback autoFillUiCallback, CharSequence charSequence) {
        if (autoFillUI.mCallback != autoFillUiCallback) {
            return;
        }
        autoFillUI.hideAllUiThread(autoFillUiCallback);
        if (!TextUtils.isEmpty(charSequence)) {
            Toast.makeText(autoFillUI.mContext, charSequence, 1).show();
        }
    }

    public void hideFillUi(final AutoFillUiCallback autoFillUiCallback) {
        this.mHandler.post(new Runnable() {
            @Override
            public final void run() {
                this.f$0.hideFillUiUiThread(autoFillUiCallback, true);
            }
        });
    }

    public void filterFillUi(final String str, final AutoFillUiCallback autoFillUiCallback) {
        this.mHandler.post(new Runnable() {
            @Override
            public final void run() {
                AutoFillUI.lambda$filterFillUi$4(this.f$0, autoFillUiCallback, str);
            }
        });
    }

    public static void lambda$filterFillUi$4(AutoFillUI autoFillUI, AutoFillUiCallback autoFillUiCallback, String str) {
        if (autoFillUiCallback == autoFillUI.mCallback && autoFillUI.mFillUi != null) {
            autoFillUI.mFillUi.setFilterText(str);
        }
    }

    public void showFillUi(AutofillId autofillId, final FillResponse fillResponse, final String str, String str2, ComponentName componentName, final CharSequence charSequence, final Drawable drawable, final AutoFillUiCallback autoFillUiCallback, int i, boolean z) {
        final AutofillId autofillId2;
        int length;
        if (Helper.sDebug) {
            if (str != null) {
                length = str.length();
            } else {
                length = 0;
            }
            StringBuilder sb = new StringBuilder();
            sb.append("showFillUi(): id=");
            autofillId2 = autofillId;
            sb.append(autofillId2);
            sb.append(", filter=");
            sb.append(length);
            sb.append(" chars");
            Slog.d(TAG, sb.toString());
        } else {
            autofillId2 = autofillId;
        }
        final LogMaker logMakerAddTaggedData = Helper.newLogMaker(910, componentName, str2, i, z).addTaggedData(911, Integer.valueOf(str == null ? 0 : str.length())).addTaggedData(909, Integer.valueOf(fillResponse.getDatasets() != null ? fillResponse.getDatasets().size() : 0));
        this.mHandler.post(new Runnable() {
            @Override
            public final void run() {
                AutoFillUI.lambda$showFillUi$5(this.f$0, autoFillUiCallback, fillResponse, autofillId2, str, charSequence, drawable, logMakerAddTaggedData);
            }
        });
    }

    public static void lambda$showFillUi$5(AutoFillUI autoFillUI, final AutoFillUiCallback autoFillUiCallback, final FillResponse fillResponse, final AutofillId autofillId, String str, CharSequence charSequence, Drawable drawable, final LogMaker logMaker) {
        if (autoFillUiCallback != autoFillUI.mCallback) {
            return;
        }
        autoFillUI.hideAllUiThread(autoFillUiCallback);
        autoFillUI.mFillUi = new FillUi(autoFillUI.mContext, fillResponse, autofillId, str, autoFillUI.mOverlayControl, charSequence, drawable, new FillUi.Callback() {
            @Override
            public void onResponsePicked(FillResponse fillResponse2) {
                logMaker.setType(3);
                AutoFillUI.this.hideFillUiUiThread(autoFillUiCallback, true);
                if (AutoFillUI.this.mCallback != null) {
                    AutoFillUI.this.mCallback.authenticate(fillResponse2.getRequestId(), NetworkConstants.ARP_HWTYPE_RESERVED_HI, fillResponse2.getAuthentication(), fillResponse2.getClientState());
                }
            }

            @Override
            public void onDatasetPicked(Dataset dataset) {
                logMaker.setType(4);
                AutoFillUI.this.hideFillUiUiThread(autoFillUiCallback, true);
                if (AutoFillUI.this.mCallback != null) {
                    AutoFillUI.this.mCallback.fill(fillResponse.getRequestId(), fillResponse.getDatasets().indexOf(dataset), dataset);
                }
            }

            @Override
            public void onCanceled() {
                logMaker.setType(5);
                AutoFillUI.this.hideFillUiUiThread(autoFillUiCallback, true);
            }

            @Override
            public void onDestroy() {
                if (logMaker.getType() == 0) {
                    logMaker.setType(2);
                }
                AutoFillUI.this.mMetricsLogger.write(logMaker);
            }

            @Override
            public void requestShowFillUi(int i, int i2, IAutofillWindowPresenter iAutofillWindowPresenter) {
                if (AutoFillUI.this.mCallback != null) {
                    AutoFillUI.this.mCallback.requestShowFillUi(autofillId, i, i2, iAutofillWindowPresenter);
                }
            }

            @Override
            public void requestHideFillUi() {
                if (AutoFillUI.this.mCallback != null) {
                    AutoFillUI.this.mCallback.requestHideFillUi(autofillId);
                }
            }

            @Override
            public void startIntentSender(IntentSender intentSender) {
                if (AutoFillUI.this.mCallback != null) {
                    AutoFillUI.this.mCallback.startIntentSender(intentSender);
                }
            }

            @Override
            public void dispatchUnhandledKey(KeyEvent keyEvent) {
                if (AutoFillUI.this.mCallback != null) {
                    AutoFillUI.this.mCallback.dispatchUnhandledKey(autofillId, keyEvent);
                }
            }
        });
    }

    public void showSaveUi(final CharSequence charSequence, final Drawable drawable, final String str, SaveInfo saveInfo, final ValueFinder valueFinder, final ComponentName componentName, final AutoFillUiCallback autoFillUiCallback, final PendingUi pendingUi, final boolean z) {
        SaveInfo saveInfo2;
        if (Helper.sVerbose) {
            StringBuilder sb = new StringBuilder();
            sb.append("showSaveUi() for ");
            sb.append(componentName.toShortString());
            sb.append(": ");
            saveInfo2 = saveInfo;
            sb.append(saveInfo2);
            Slog.v(TAG, sb.toString());
        } else {
            saveInfo2 = saveInfo;
        }
        final LogMaker logMakerAddTaggedData = Helper.newLogMaker(916, componentName, str, pendingUi.sessionId, z).addTaggedData(917, Integer.valueOf((saveInfo.getRequiredIds() == null ? 0 : saveInfo.getRequiredIds().length) + 0 + (saveInfo.getOptionalIds() != null ? saveInfo.getOptionalIds().length : 0)));
        final SaveInfo saveInfo3 = saveInfo2;
        this.mHandler.post(new Runnable() {
            @Override
            public final void run() {
                AutoFillUI.lambda$showSaveUi$6(this.f$0, autoFillUiCallback, pendingUi, charSequence, drawable, str, componentName, saveInfo3, valueFinder, logMakerAddTaggedData, z);
            }
        });
    }

    public static void lambda$showSaveUi$6(AutoFillUI autoFillUI, AutoFillUiCallback autoFillUiCallback, final PendingUi pendingUi, CharSequence charSequence, Drawable drawable, String str, ComponentName componentName, SaveInfo saveInfo, ValueFinder valueFinder, final LogMaker logMaker, boolean z) {
        if (autoFillUiCallback != autoFillUI.mCallback) {
            return;
        }
        autoFillUI.hideAllUiThread(autoFillUiCallback);
        autoFillUI.mSaveUi = new SaveUi(autoFillUI.mContext, pendingUi, charSequence, drawable, str, componentName, saveInfo, valueFinder, autoFillUI.mOverlayControl, new SaveUi.OnSaveListener() {
            @Override
            public void onSave() {
                logMaker.setType(4);
                AutoFillUI.this.hideSaveUiUiThread(AutoFillUI.this.mCallback);
                if (AutoFillUI.this.mCallback != null) {
                    AutoFillUI.this.mCallback.save();
                }
                AutoFillUI.this.destroySaveUiUiThread(pendingUi, true);
            }

            @Override
            public void onCancel(IntentSender intentSender) {
                logMaker.setType(5);
                AutoFillUI.this.hideSaveUiUiThread(AutoFillUI.this.mCallback);
                if (intentSender != null) {
                    try {
                        intentSender.sendIntent(AutoFillUI.this.mContext, 0, null, null, null);
                    } catch (IntentSender.SendIntentException e) {
                        Slog.e(AutoFillUI.TAG, "Error starting negative action listener: " + intentSender, e);
                    }
                }
                if (AutoFillUI.this.mCallback != null) {
                    AutoFillUI.this.mCallback.cancelSave();
                }
                AutoFillUI.this.destroySaveUiUiThread(pendingUi, true);
            }

            @Override
            public void onDestroy() {
                if (logMaker.getType() == 0) {
                    logMaker.setType(2);
                    if (AutoFillUI.this.mCallback != null) {
                        AutoFillUI.this.mCallback.cancelSave();
                    }
                }
                AutoFillUI.this.mMetricsLogger.write(logMaker);
            }
        }, z);
    }

    public void onPendingSaveUi(final int i, final IBinder iBinder) {
        this.mHandler.post(new Runnable() {
            @Override
            public final void run() {
                AutoFillUI.lambda$onPendingSaveUi$7(this.f$0, i, iBinder);
            }
        });
    }

    public static void lambda$onPendingSaveUi$7(AutoFillUI autoFillUI, int i, IBinder iBinder) {
        if (autoFillUI.mSaveUi != null) {
            autoFillUI.mSaveUi.onPendingUi(i, iBinder);
            return;
        }
        Slog.w(TAG, "onPendingSaveUi(" + i + "): no save ui");
    }

    public void hideAll(final AutoFillUiCallback autoFillUiCallback) {
        this.mHandler.post(new Runnable() {
            @Override
            public final void run() {
                this.f$0.hideAllUiThread(autoFillUiCallback);
            }
        });
    }

    public void destroyAll(final PendingUi pendingUi, final AutoFillUiCallback autoFillUiCallback, final boolean z) {
        this.mHandler.post(new Runnable() {
            @Override
            public final void run() {
                this.f$0.destroyAllUiThread(pendingUi, autoFillUiCallback, z);
            }
        });
    }

    public void dump(PrintWriter printWriter) {
        printWriter.println("Autofill UI");
        if (this.mFillUi != null) {
            printWriter.print("  ");
            printWriter.println("showsFillUi: true");
            this.mFillUi.dump(printWriter, "    ");
        } else {
            printWriter.print("  ");
            printWriter.println("showsFillUi: false");
        }
        if (this.mSaveUi != null) {
            printWriter.print("  ");
            printWriter.println("showsSaveUi: true");
            this.mSaveUi.dump(printWriter, "    ");
        } else {
            printWriter.print("  ");
            printWriter.println("showsSaveUi: false");
        }
    }

    private void hideFillUiUiThread(AutoFillUiCallback autoFillUiCallback, boolean z) {
        if (this.mFillUi != null) {
            if (autoFillUiCallback == null || autoFillUiCallback == this.mCallback) {
                this.mFillUi.destroy(z);
                this.mFillUi = null;
            }
        }
    }

    private PendingUi hideSaveUiUiThread(AutoFillUiCallback autoFillUiCallback) {
        if (Helper.sVerbose) {
            Slog.v(TAG, "hideSaveUiUiThread(): mSaveUi=" + this.mSaveUi + ", callback=" + autoFillUiCallback + ", mCallback=" + this.mCallback);
        }
        if (this.mSaveUi == null) {
            return null;
        }
        if (autoFillUiCallback == null || autoFillUiCallback == this.mCallback) {
            return this.mSaveUi.hide();
        }
        return null;
    }

    private void destroySaveUiUiThread(PendingUi pendingUi, boolean z) {
        if (this.mSaveUi == null) {
            if (Helper.sDebug) {
                Slog.d(TAG, "destroySaveUiUiThread(): already destroyed");
                return;
            }
            return;
        }
        if (Helper.sDebug) {
            Slog.d(TAG, "destroySaveUiUiThread(): " + pendingUi);
        }
        this.mSaveUi.destroy();
        this.mSaveUi = null;
        if (pendingUi != null && z) {
            try {
                if (Helper.sDebug) {
                    Slog.d(TAG, "destroySaveUiUiThread(): notifying client");
                }
                pendingUi.client.setSaveUiState(pendingUi.sessionId, false);
            } catch (RemoteException e) {
                Slog.e(TAG, "Error notifying client to set save UI state to hidden: " + e);
            }
        }
    }

    private void destroyAllUiThread(PendingUi pendingUi, AutoFillUiCallback autoFillUiCallback, boolean z) {
        hideFillUiUiThread(autoFillUiCallback, z);
        destroySaveUiUiThread(pendingUi, z);
    }

    private void hideAllUiThread(AutoFillUiCallback autoFillUiCallback) {
        hideFillUiUiThread(autoFillUiCallback, true);
        PendingUi pendingUiHideSaveUiUiThread = hideSaveUiUiThread(autoFillUiCallback);
        if (pendingUiHideSaveUiUiThread != null && pendingUiHideSaveUiUiThread.getState() == 4) {
            if (Helper.sDebug) {
                Slog.d(TAG, "hideAllUiThread(): destroying Save UI because pending restoration is finished");
            }
            destroySaveUiUiThread(pendingUiHideSaveUiUiThread, true);
        }
    }
}
