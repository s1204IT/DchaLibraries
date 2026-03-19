package com.android.server.autofill.ui;

import android.R;
import android.app.Dialog;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.graphics.drawable.Drawable;
import android.metrics.LogMaker;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.service.autofill.BatchUpdates;
import android.service.autofill.CustomDescription;
import android.service.autofill.InternalTransformation;
import android.service.autofill.InternalValidator;
import android.service.autofill.SaveInfo;
import android.service.autofill.ValueFinder;
import android.text.Html;
import android.util.ArraySet;
import android.util.Pair;
import android.util.Slog;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.RemoteViews;
import android.widget.TextView;
import com.android.internal.logging.MetricsLogger;
import com.android.server.UiThread;
import com.android.server.autofill.Helper;
import java.io.PrintWriter;
import java.util.ArrayList;

final class SaveUi {
    private static final String TAG = "AutofillSaveUi";
    private static final int THEME_ID = 16974778;
    private final boolean mCompatMode;
    private final ComponentName mComponentName;
    private boolean mDestroyed;
    private final Dialog mDialog;
    private final OneTimeListener mListener;
    private final OverlayControl mOverlayControl;
    private final PendingUi mPendingUi;
    private final String mServicePackageName;
    private final CharSequence mSubTitle;
    private final CharSequence mTitle;
    private final Handler mHandler = UiThread.getHandler();
    private final MetricsLogger mMetricsLogger = new MetricsLogger();

    public interface OnSaveListener {
        void onCancel(IntentSender intentSender);

        void onDestroy();

        void onSave();
    }

    private class OneTimeListener implements OnSaveListener {
        private boolean mDone;
        private final OnSaveListener mRealListener;

        OneTimeListener(OnSaveListener onSaveListener) {
            this.mRealListener = onSaveListener;
        }

        @Override
        public void onSave() {
            if (Helper.sDebug) {
                Slog.d(SaveUi.TAG, "OneTimeListener.onSave(): " + this.mDone);
            }
            if (this.mDone) {
                return;
            }
            this.mDone = true;
            this.mRealListener.onSave();
        }

        @Override
        public void onCancel(IntentSender intentSender) {
            if (Helper.sDebug) {
                Slog.d(SaveUi.TAG, "OneTimeListener.onCancel(): " + this.mDone);
            }
            if (this.mDone) {
                return;
            }
            this.mDone = true;
            this.mRealListener.onCancel(intentSender);
        }

        @Override
        public void onDestroy() {
            if (Helper.sDebug) {
                Slog.d(SaveUi.TAG, "OneTimeListener.onDestroy(): " + this.mDone);
            }
            if (this.mDone) {
                return;
            }
            this.mDone = true;
            this.mRealListener.onDestroy();
        }
    }

    SaveUi(Context context, PendingUi pendingUi, CharSequence charSequence, Drawable drawable, String str, ComponentName componentName, final SaveInfo saveInfo, ValueFinder valueFinder, OverlayControl overlayControl, OnSaveListener onSaveListener, boolean z) {
        this.mPendingUi = pendingUi;
        this.mListener = new OneTimeListener(onSaveListener);
        this.mOverlayControl = overlayControl;
        this.mServicePackageName = str;
        this.mComponentName = componentName;
        this.mCompatMode = z;
        ContextThemeWrapper contextThemeWrapper = new ContextThemeWrapper(context, 16974778);
        View viewInflate = LayoutInflater.from(contextThemeWrapper).inflate(R.layout.app_language_picker_locale_item, (ViewGroup) null);
        TextView textView = (TextView) viewInflate.findViewById(R.id.accessibility_performAction_description);
        ArraySet arraySet = new ArraySet(3);
        int type = saveInfo.getType();
        if ((type & 1) != 0) {
            arraySet.add(contextThemeWrapper.getString(R.string.PERSOSUBSTATE_SIM_SP_EHPLMN_ERROR));
        }
        if ((type & 2) != 0) {
            arraySet.add(contextThemeWrapper.getString(R.string.PERSOSUBSTATE_SIM_SPN_IN_PROGRESS));
        }
        if ((type & 4) != 0) {
            arraySet.add(contextThemeWrapper.getString(R.string.PERSOSUBSTATE_SIM_SPN_SUCCESS));
        }
        if ((type & 8) != 0) {
            arraySet.add(contextThemeWrapper.getString(R.string.PERSOSUBSTATE_SIM_SP_EHPLMN_IN_PROGRESS));
        }
        if ((type & 16) != 0) {
            arraySet.add(contextThemeWrapper.getString(R.string.PERSOSUBSTATE_SIM_SP_EHPLMN_ENTRY));
        }
        switch (arraySet.size()) {
            case 1:
                this.mTitle = Html.fromHtml(contextThemeWrapper.getString(R.string.PERSOSUBSTATE_SIM_SPN_ERROR, arraySet.valueAt(0), charSequence), 0);
                break;
            case 2:
                this.mTitle = Html.fromHtml(contextThemeWrapper.getString(R.string.PERSOSUBSTATE_SIM_SIM_SUCCESS, arraySet.valueAt(0), arraySet.valueAt(1), charSequence), 0);
                break;
            case 3:
                this.mTitle = Html.fromHtml(contextThemeWrapper.getString(R.string.PERSOSUBSTATE_SIM_SPN_ENTRY, arraySet.valueAt(0), arraySet.valueAt(1), arraySet.valueAt(2), charSequence), 0);
                break;
            default:
                this.mTitle = Html.fromHtml(contextThemeWrapper.getString(R.string.PERSOSUBSTATE_SIM_SIM_PUK_SUCCESS, charSequence), 0);
                break;
        }
        textView.setText(this.mTitle);
        setServiceIcon(contextThemeWrapper, viewInflate, drawable);
        if (applyCustomDescription(contextThemeWrapper, viewInflate, valueFinder, saveInfo)) {
            this.mSubTitle = null;
            if (Helper.sDebug) {
                Slog.d(TAG, "on constructor: applied custom description");
            }
        } else {
            this.mSubTitle = saveInfo.getDescription();
            if (this.mSubTitle != null) {
                writeLog(1131, type);
                ViewGroup viewGroup = (ViewGroup) viewInflate.findViewById(R.id.accessibility_controlScreen_icon);
                TextView textView2 = new TextView(contextThemeWrapper);
                textView2.setText(this.mSubTitle);
                viewGroup.addView(textView2, new ViewGroup.LayoutParams(-1, -2));
                viewGroup.setVisibility(0);
            }
            if (Helper.sDebug) {
                Slog.d(TAG, "on constructor: title=" + ((Object) this.mTitle) + ", subTitle=" + ((Object) this.mSubTitle));
            }
        }
        TextView textView3 = (TextView) viewInflate.findViewById(R.id.accessibility_magnification_thumbnail_view);
        if (saveInfo.getNegativeActionStyle() == 1) {
            textView3.setText(R.string.lockscreen_failed_attempts_almost_glogin);
        } else {
            textView3.setText(R.string.PERSOSUBSTATE_SIM_SIM_PUK_IN_PROGRESS);
        }
        textView3.setOnClickListener(new View.OnClickListener() {
            @Override
            public final void onClick(View view) {
                this.f$0.mListener.onCancel(saveInfo.getNegativeActionListener());
            }
        });
        viewInflate.findViewById(R.id.accessibility_performAction_icon).setOnClickListener(new View.OnClickListener() {
            @Override
            public final void onClick(View view) {
                this.f$0.mListener.onSave();
            }
        });
        this.mDialog = new Dialog(contextThemeWrapper, 16974778);
        this.mDialog.setContentView(viewInflate);
        this.mDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public final void onDismiss(DialogInterface dialogInterface) {
                this.f$0.mListener.onCancel(null);
            }
        });
        Window window = this.mDialog.getWindow();
        window.setType(2038);
        window.addFlags(393248);
        window.addPrivateFlags(16);
        window.setSoftInputMode(32);
        window.setGravity(81);
        window.setCloseOnTouchOutside(true);
        WindowManager.LayoutParams attributes = window.getAttributes();
        attributes.width = -1;
        attributes.accessibilityTitle = contextThemeWrapper.getString(R.string.PERSOSUBSTATE_SIM_SIM_PUK_ERROR);
        attributes.windowAnimations = R.style.Animation.LockScreen;
        show();
    }

    private boolean applyCustomDescription(Context context, View view, ValueFinder valueFinder, SaveInfo saveInfo) {
        CustomDescription customDescription = saveInfo.getCustomDescription();
        if (customDescription == null) {
            return false;
        }
        final int type = saveInfo.getType();
        writeLog(1129, type);
        RemoteViews presentation = customDescription.getPresentation();
        if (presentation == null) {
            Slog.w(TAG, "No remote view on custom description");
            return false;
        }
        ArrayList transformations = customDescription.getTransformations();
        if (transformations != null && !InternalTransformation.batchApply(valueFinder, presentation, transformations)) {
            Slog.w(TAG, "could not apply main transformations on custom description");
            return false;
        }
        RemoteViews.OnClickHandler onClickHandler = new RemoteViews.OnClickHandler() {
            public boolean onClickHandler(View view2, PendingIntent pendingIntent, Intent intent) {
                LogMaker logMakerNewLogMaker = SaveUi.this.newLogMaker(1132, type);
                if (!SaveUi.isValidLink(pendingIntent, intent)) {
                    logMakerNewLogMaker.setType(0);
                    SaveUi.this.mMetricsLogger.write(logMakerNewLogMaker);
                    return false;
                }
                if (Helper.sVerbose) {
                    Slog.v(SaveUi.TAG, "Intercepting custom description intent");
                }
                IBinder token = SaveUi.this.mPendingUi.getToken();
                intent.putExtra("android.view.autofill.extra.RESTORE_SESSION_TOKEN", token);
                try {
                    SaveUi.this.mPendingUi.client.startIntentSender(pendingIntent.getIntentSender(), intent);
                    SaveUi.this.mPendingUi.setState(2);
                    if (Helper.sDebug) {
                        Slog.d(SaveUi.TAG, "hiding UI until restored with token " + token);
                    }
                    SaveUi.this.hide();
                    logMakerNewLogMaker.setType(1);
                    SaveUi.this.mMetricsLogger.write(logMakerNewLogMaker);
                    return true;
                } catch (RemoteException e) {
                    Slog.w(SaveUi.TAG, "error triggering pending intent: " + intent);
                    logMakerNewLogMaker.setType(11);
                    SaveUi.this.mMetricsLogger.write(logMakerNewLogMaker);
                    return false;
                }
            }
        };
        try {
            presentation.setApplyTheme(16974778);
            View viewApply = presentation.apply(context, null, onClickHandler);
            ArrayList updates = customDescription.getUpdates();
            if (updates != null) {
                int size = updates.size();
                if (Helper.sDebug) {
                    Slog.d(TAG, "custom description has " + size + " batch updates");
                }
                for (int i = 0; i < size; i++) {
                    Pair pair = (Pair) updates.get(i);
                    InternalValidator internalValidator = (InternalValidator) pair.first;
                    if (internalValidator == null || !internalValidator.isValid(valueFinder)) {
                        if (Helper.sDebug) {
                            Slog.d(TAG, "Skipping batch update #" + i);
                        }
                    } else {
                        BatchUpdates batchUpdates = (BatchUpdates) pair.second;
                        RemoteViews updates2 = batchUpdates.getUpdates();
                        if (updates2 != null) {
                            if (Helper.sDebug) {
                                Slog.d(TAG, "Applying template updates for batch update #" + i);
                            }
                            updates2.reapply(context, viewApply);
                        }
                        ArrayList transformations2 = batchUpdates.getTransformations();
                        if (transformations2 == null) {
                            continue;
                        } else {
                            if (Helper.sDebug) {
                                Slog.d(TAG, "Applying child transformation for batch update #" + i + ": " + transformations2);
                            }
                            if (!InternalTransformation.batchApply(valueFinder, presentation, transformations2)) {
                                Slog.w(TAG, "Could not apply child transformation for batch update #" + i + ": " + transformations2);
                                return false;
                            }
                            presentation.reapply(context, viewApply);
                        }
                    }
                }
            }
            ViewGroup viewGroup = (ViewGroup) view.findViewById(R.id.accessibility_controlScreen_icon);
            viewGroup.addView(viewApply);
            viewGroup.setVisibility(0);
            return true;
        } catch (Exception e) {
            Slog.e(TAG, "Error applying custom description. ", e);
            return false;
        }
    }

    private void setServiceIcon(Context context, View view, Drawable drawable) {
        ImageView imageView = (ImageView) view.findViewById(R.id.accessibility_controlScreen_title);
        int dimensionPixelSize = context.getResources().getDimensionPixelSize(R.dimen.action_bar_subtitle_bottom_margin);
        int minimumWidth = drawable.getMinimumWidth();
        int minimumHeight = drawable.getMinimumHeight();
        if (minimumWidth <= dimensionPixelSize && minimumHeight <= dimensionPixelSize) {
            if (Helper.sDebug) {
                Slog.d(TAG, "Adding service icon (" + minimumWidth + "x" + minimumHeight + ") as it's less than maximum (" + dimensionPixelSize + "x" + dimensionPixelSize + ").");
            }
            imageView.setImageDrawable(drawable);
            return;
        }
        Slog.w(TAG, "Not adding service icon of size (" + minimumWidth + "x" + minimumHeight + ") because maximum is (" + dimensionPixelSize + "x" + dimensionPixelSize + ").");
        ((ViewGroup) imageView.getParent()).removeView(imageView);
    }

    private static boolean isValidLink(PendingIntent pendingIntent, Intent intent) {
        if (pendingIntent == null) {
            Slog.w(TAG, "isValidLink(): custom description without pending intent");
            return false;
        }
        if (!pendingIntent.isActivity()) {
            Slog.w(TAG, "isValidLink(): pending intent not for activity");
            return false;
        }
        if (intent == null) {
            Slog.w(TAG, "isValidLink(): no intent");
            return false;
        }
        return true;
    }

    private LogMaker newLogMaker(int i, int i2) {
        return newLogMaker(i).addTaggedData(1130, Integer.valueOf(i2));
    }

    private LogMaker newLogMaker(int i) {
        return Helper.newLogMaker(i, this.mComponentName, this.mServicePackageName, this.mPendingUi.sessionId, this.mCompatMode);
    }

    private void writeLog(int i, int i2) {
        this.mMetricsLogger.write(newLogMaker(i, i2));
    }

    void onPendingUi(int i, IBinder iBinder) {
        if (!this.mPendingUi.matches(iBinder)) {
            Slog.w(TAG, "restore(" + i + "): got token " + iBinder + " instead of " + this.mPendingUi.getToken());
            return;
        }
        LogMaker logMakerNewLogMaker = newLogMaker(1134);
        try {
            switch (i) {
                case 1:
                    logMakerNewLogMaker.setType(5);
                    if (Helper.sDebug) {
                        Slog.d(TAG, "Cancelling pending save dialog for " + iBinder);
                    }
                    hide();
                    break;
                case 2:
                    if (Helper.sDebug) {
                        Slog.d(TAG, "Restoring save dialog for " + iBinder);
                    }
                    logMakerNewLogMaker.setType(1);
                    show();
                    break;
                default:
                    logMakerNewLogMaker.setType(11);
                    Slog.w(TAG, "restore(): invalid operation " + i);
                    break;
            }
            this.mMetricsLogger.write(logMakerNewLogMaker);
            this.mPendingUi.setState(4);
        } catch (Throwable th) {
            this.mMetricsLogger.write(logMakerNewLogMaker);
            throw th;
        }
    }

    private void show() {
        Slog.i(TAG, "Showing save dialog: " + ((Object) this.mTitle));
        this.mDialog.show();
        this.mOverlayControl.hideOverlays();
    }

    PendingUi hide() {
        if (Helper.sVerbose) {
            Slog.v(TAG, "Hiding save dialog.");
        }
        try {
            this.mDialog.hide();
            this.mOverlayControl.showOverlays();
            return this.mPendingUi;
        } catch (Throwable th) {
            this.mOverlayControl.showOverlays();
            throw th;
        }
    }

    void destroy() {
        try {
            if (Helper.sDebug) {
                Slog.d(TAG, "destroy()");
            }
            throwIfDestroyed();
            this.mListener.onDestroy();
            this.mHandler.removeCallbacksAndMessages(this.mListener);
            this.mDialog.dismiss();
            this.mDestroyed = true;
        } finally {
            this.mOverlayControl.showOverlays();
        }
    }

    private void throwIfDestroyed() {
        if (this.mDestroyed) {
            throw new IllegalStateException("cannot interact with a destroyed instance");
        }
    }

    public String toString() {
        return this.mTitle == null ? "NO TITLE" : this.mTitle.toString();
    }

    void dump(PrintWriter printWriter, String str) {
        printWriter.print(str);
        printWriter.print("title: ");
        printWriter.println(this.mTitle);
        printWriter.print(str);
        printWriter.print("subtitle: ");
        printWriter.println(this.mSubTitle);
        printWriter.print(str);
        printWriter.print("pendingUi: ");
        printWriter.println(this.mPendingUi);
        printWriter.print(str);
        printWriter.print("service: ");
        printWriter.println(this.mServicePackageName);
        printWriter.print(str);
        printWriter.print("app: ");
        printWriter.println(this.mComponentName.toShortString());
        printWriter.print(str);
        printWriter.print("compat mode: ");
        printWriter.println(this.mCompatMode);
        View decorView = this.mDialog.getWindow().getDecorView();
        int[] locationOnScreen = decorView.getLocationOnScreen();
        printWriter.print(str);
        printWriter.print("coordinates: ");
        printWriter.print('(');
        printWriter.print(locationOnScreen[0]);
        printWriter.print(',');
        printWriter.print(locationOnScreen[1]);
        printWriter.print(')');
        printWriter.print('(');
        printWriter.print(locationOnScreen[0] + decorView.getWidth());
        printWriter.print(',');
        printWriter.print(locationOnScreen[1] + decorView.getHeight());
        printWriter.println(')');
        printWriter.print(str);
        printWriter.print("destroyed: ");
        printWriter.println(this.mDestroyed);
    }
}
