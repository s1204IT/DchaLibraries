package com.android.server.autofill;

import android.graphics.Rect;
import android.service.autofill.FillResponse;
import android.util.DebugUtils;
import android.util.Slog;
import android.view.autofill.AutofillId;
import android.view.autofill.AutofillValue;
import java.io.PrintWriter;

final class ViewState {
    public static final int STATE_AUTOFILLED = 4;
    public static final int STATE_AUTOFILL_FAILED = 1024;
    public static final int STATE_CHANGED = 8;
    public static final int STATE_FILLABLE = 2;
    public static final int STATE_IGNORED = 128;
    public static final int STATE_INITIAL = 1;
    public static final int STATE_RESTARTED_SESSION = 256;
    public static final int STATE_STARTED_PARTITION = 32;
    public static final int STATE_STARTED_SESSION = 16;
    public static final int STATE_UNKNOWN = 0;
    public static final int STATE_URL_BAR = 512;
    public static final int STATE_WAITING_DATASET_AUTH = 64;
    private static final String TAG = "ViewState";
    public final AutofillId id;
    private AutofillValue mAutofilledValue;
    private AutofillValue mCurrentValue;
    private String mDatasetId;
    private final Listener mListener;
    private FillResponse mResponse;
    private AutofillValue mSanitizedValue;
    private final Session mSession;
    private int mState;
    private Rect mVirtualBounds;

    interface Listener {
        void onFillReady(FillResponse fillResponse, AutofillId autofillId, AutofillValue autofillValue);
    }

    ViewState(Session session, AutofillId autofillId, Listener listener, int i) {
        this.mSession = session;
        this.id = autofillId;
        this.mListener = listener;
        this.mState = i;
    }

    Rect getVirtualBounds() {
        return this.mVirtualBounds;
    }

    AutofillValue getCurrentValue() {
        return this.mCurrentValue;
    }

    void setCurrentValue(AutofillValue autofillValue) {
        this.mCurrentValue = autofillValue;
    }

    AutofillValue getAutofilledValue() {
        return this.mAutofilledValue;
    }

    void setAutofilledValue(AutofillValue autofillValue) {
        this.mAutofilledValue = autofillValue;
    }

    AutofillValue getSanitizedValue() {
        return this.mSanitizedValue;
    }

    void setSanitizedValue(AutofillValue autofillValue) {
        this.mSanitizedValue = autofillValue;
    }

    FillResponse getResponse() {
        return this.mResponse;
    }

    void setResponse(FillResponse fillResponse) {
        this.mResponse = fillResponse;
    }

    CharSequence getServiceName() {
        return this.mSession.getServiceName();
    }

    int getState() {
        return this.mState;
    }

    String getStateAsString() {
        return getStateAsString(this.mState);
    }

    static String getStateAsString(int i) {
        return DebugUtils.flagsToString(ViewState.class, "STATE_", i);
    }

    void setState(int i) {
        if (this.mState == 1) {
            this.mState = i;
        } else {
            this.mState = i | this.mState;
        }
    }

    void resetState(int i) {
        this.mState = (~i) & this.mState;
    }

    String getDatasetId() {
        return this.mDatasetId;
    }

    void setDatasetId(String str) {
        this.mDatasetId = str;
    }

    void update(AutofillValue autofillValue, Rect rect, int i) {
        if (autofillValue != null) {
            this.mCurrentValue = autofillValue;
        }
        if (rect != null) {
            this.mVirtualBounds = rect;
        }
        maybeCallOnFillReady(i);
    }

    void maybeCallOnFillReady(int i) {
        if ((this.mState & 4) != 0 && (i & 1) == 0) {
            if (Helper.sDebug) {
                Slog.d(TAG, "Ignoring UI for " + this.id + " on " + getStateAsString());
                return;
            }
            return;
        }
        if (this.mResponse != null) {
            if (this.mResponse.getDatasets() != null || this.mResponse.getAuthentication() != null) {
                this.mListener.onFillReady(this.mResponse, this.id, this.mCurrentValue);
            }
        }
    }

    public String toString() {
        StringBuilder sb = new StringBuilder("ViewState: [id=");
        sb.append(this.id);
        if (this.mDatasetId != null) {
            sb.append("datasetId:");
            sb.append(this.mDatasetId);
        }
        sb.append("state:");
        sb.append(getStateAsString());
        if (this.mCurrentValue != null) {
            sb.append("currentValue:");
            sb.append(this.mCurrentValue);
        }
        if (this.mAutofilledValue != null) {
            sb.append("autofilledValue:");
            sb.append(this.mAutofilledValue);
        }
        if (this.mSanitizedValue != null) {
            sb.append("sanitizedValue:");
            sb.append(this.mSanitizedValue);
        }
        if (this.mVirtualBounds != null) {
            sb.append("virtualBounds:");
            sb.append(this.mVirtualBounds);
        }
        return sb.toString();
    }

    void dump(String str, PrintWriter printWriter) {
        printWriter.print(str);
        printWriter.print("id:");
        printWriter.println(this.id);
        if (this.mDatasetId != null) {
            printWriter.print(str);
            printWriter.print("datasetId:");
            printWriter.println(this.mDatasetId);
        }
        printWriter.print(str);
        printWriter.print("state:");
        printWriter.println(getStateAsString());
        if (this.mResponse != null) {
            printWriter.print(str);
            printWriter.print("response id:");
            printWriter.println(this.mResponse.getRequestId());
        }
        if (this.mCurrentValue != null) {
            printWriter.print(str);
            printWriter.print("currentValue:");
            printWriter.println(this.mCurrentValue);
        }
        if (this.mAutofilledValue != null) {
            printWriter.print(str);
            printWriter.print("autofilledValue:");
            printWriter.println(this.mAutofilledValue);
        }
        if (this.mSanitizedValue != null) {
            printWriter.print(str);
            printWriter.print("sanitizedValue:");
            printWriter.println(this.mSanitizedValue);
        }
        if (this.mVirtualBounds != null) {
            printWriter.print(str);
            printWriter.print("virtualBounds:");
            printWriter.println(this.mVirtualBounds);
        }
    }
}
