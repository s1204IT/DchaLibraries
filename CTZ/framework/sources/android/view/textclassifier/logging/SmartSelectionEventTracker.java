package android.view.textclassifier.logging;

import android.content.Context;
import android.metrics.LogMaker;
import android.util.Log;
import android.view.textclassifier.TextClassification;
import android.view.textclassifier.TextClassifier;
import android.view.textclassifier.TextSelection;
import com.android.internal.content.NativeLibraryHelper;
import com.android.internal.logging.MetricsLogger;
import com.android.internal.logging.nano.MetricsProto;
import com.android.internal.util.Preconditions;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Objects;
import java.util.UUID;

public final class SmartSelectionEventTracker {
    private static final String CUSTOM_EDITTEXT = "customedit";
    private static final String CUSTOM_TEXTVIEW = "customview";
    private static final String CUSTOM_UNSELECTABLE_TEXTVIEW = "nosel-customview";
    private static final boolean DEBUG_LOG_ENABLED = true;
    private static final String EDITTEXT = "edittext";
    private static final String EDIT_WEBVIEW = "edit-webview";
    private static final int ENTITY_TYPE = 1254;
    private static final int EVENT_END = 1251;
    private static final int EVENT_START = 1250;
    private static final int INDEX = 1120;
    private static final String LOG_TAG = "SmartSelectEventTracker";
    private static final int MODEL_NAME = 1256;
    private static final int PREV_EVENT_DELTA = 1118;
    private static final int SESSION_ID = 1119;
    private static final int SMART_END = 1253;
    private static final int SMART_START = 1252;
    private static final int START_EVENT_DELTA = 1117;
    private static final String TEXTVIEW = "textview";
    private static final String UNKNOWN = "unknown";
    private static final String UNSELECTABLE_TEXTVIEW = "nosel-textview";
    private static final String WEBVIEW = "webview";
    private static final int WIDGET_TYPE = 1255;
    private static final int WIDGET_VERSION = 1262;
    private static final String ZERO = "0";
    private final Context mContext;
    private int mIndex;
    private long mLastEventTime;
    private final MetricsLogger mMetricsLogger;
    private String mModelName;
    private int mOrigStart;
    private final int[] mPrevIndices;
    private String mSessionId;
    private long mSessionStartTime;
    private final int[] mSmartIndices;
    private boolean mSmartSelectionTriggered;
    private final int mWidgetType;
    private final String mWidgetVersion;

    @Retention(RetentionPolicy.SOURCE)
    public @interface WidgetType {
        public static final int CUSTOM_EDITTEXT = 7;
        public static final int CUSTOM_TEXTVIEW = 6;
        public static final int CUSTOM_UNSELECTABLE_TEXTVIEW = 8;
        public static final int EDITTEXT = 3;
        public static final int EDIT_WEBVIEW = 4;
        public static final int TEXTVIEW = 1;
        public static final int UNSELECTABLE_TEXTVIEW = 5;
        public static final int UNSPECIFIED = 0;
        public static final int WEBVIEW = 2;
    }

    public SmartSelectionEventTracker(Context context, int i) {
        this.mMetricsLogger = new MetricsLogger();
        this.mSmartIndices = new int[2];
        this.mPrevIndices = new int[2];
        this.mWidgetType = i;
        this.mWidgetVersion = null;
        this.mContext = (Context) Preconditions.checkNotNull(context);
    }

    public SmartSelectionEventTracker(Context context, int i, String str) {
        this.mMetricsLogger = new MetricsLogger();
        this.mSmartIndices = new int[2];
        this.mPrevIndices = new int[2];
        this.mWidgetType = i;
        this.mWidgetVersion = str;
        this.mContext = (Context) Preconditions.checkNotNull(context);
    }

    public void logEvent(SelectionEvent selectionEvent) {
        Preconditions.checkNotNull(selectionEvent);
        if (selectionEvent.mEventType != 1 && this.mSessionId == null) {
            Log.d(LOG_TAG, "Selection session not yet started. Ignoring event");
            return;
        }
        long jCurrentTimeMillis = System.currentTimeMillis();
        switch (selectionEvent.mEventType) {
            case 1:
                this.mSessionId = startNewSession();
                Preconditions.checkArgument(selectionEvent.mEnd == selectionEvent.mStart + 1);
                this.mOrigStart = selectionEvent.mStart;
                this.mSessionStartTime = jCurrentTimeMillis;
                break;
            case 2:
            case 5:
                if (this.mPrevIndices[0] == selectionEvent.mStart && this.mPrevIndices[1] == selectionEvent.mEnd) {
                    return;
                }
                break;
            case 3:
            case 4:
                this.mSmartSelectionTriggered = true;
                this.mModelName = getModelName(selectionEvent);
                this.mSmartIndices[0] = selectionEvent.mStart;
                this.mSmartIndices[1] = selectionEvent.mEnd;
                break;
        }
        writeEvent(selectionEvent, jCurrentTimeMillis);
        if (selectionEvent.isTerminal()) {
            endSession();
        }
    }

    private void writeEvent(SelectionEvent selectionEvent, long j) {
        LogMaker logMakerAddTaggedData = new LogMaker(1100).setType(getLogType(selectionEvent)).setSubtype(1).setPackageName(this.mContext.getPackageName()).addTaggedData(1117, Long.valueOf(j - this.mSessionStartTime)).addTaggedData(1118, Long.valueOf(this.mLastEventTime != 0 ? j - this.mLastEventTime : 0L)).addTaggedData(1120, Integer.valueOf(this.mIndex)).addTaggedData(1255, getWidgetTypeName()).addTaggedData(1262, this.mWidgetVersion).addTaggedData(1256, this.mModelName).addTaggedData(1254, selectionEvent.mEntityType).addTaggedData(1252, Integer.valueOf(getSmartRangeDelta(this.mSmartIndices[0]))).addTaggedData(1253, Integer.valueOf(getSmartRangeDelta(this.mSmartIndices[1]))).addTaggedData(1250, Integer.valueOf(getRangeDelta(selectionEvent.mStart))).addTaggedData(1251, Integer.valueOf(getRangeDelta(selectionEvent.mEnd))).addTaggedData(1119, this.mSessionId);
        this.mMetricsLogger.write(logMakerAddTaggedData);
        debugLog(logMakerAddTaggedData);
        this.mLastEventTime = j;
        this.mPrevIndices[0] = selectionEvent.mStart;
        this.mPrevIndices[1] = selectionEvent.mEnd;
        this.mIndex++;
    }

    private String startNewSession() {
        endSession();
        this.mSessionId = createSessionId();
        return this.mSessionId;
    }

    private void endSession() {
        this.mOrigStart = 0;
        int[] iArr = this.mSmartIndices;
        this.mSmartIndices[1] = 0;
        iArr[0] = 0;
        int[] iArr2 = this.mPrevIndices;
        this.mPrevIndices[1] = 0;
        iArr2[0] = 0;
        this.mIndex = 0;
        this.mSessionStartTime = 0L;
        this.mLastEventTime = 0L;
        this.mSmartSelectionTriggered = false;
        this.mModelName = getModelName(null);
        this.mSessionId = null;
    }

    private static int getLogType(SelectionEvent selectionEvent) {
        int i = selectionEvent.mEventType;
        switch (i) {
            case 1:
                return 1101;
            case 2:
                return MetricsProto.MetricsEvent.ACTION_TEXT_SELECTION_MODIFY;
            case 3:
                return MetricsProto.MetricsEvent.ACTION_TEXT_SELECTION_SMART_SINGLE;
            case 4:
                return MetricsProto.MetricsEvent.ACTION_TEXT_SELECTION_SMART_MULTI;
            case 5:
                return MetricsProto.MetricsEvent.ACTION_TEXT_SELECTION_AUTO;
            default:
                switch (i) {
                    case 100:
                        return MetricsProto.MetricsEvent.ACTION_TEXT_SELECTION_OVERTYPE;
                    case 101:
                        return MetricsProto.MetricsEvent.ACTION_TEXT_SELECTION_COPY;
                    case 102:
                        return MetricsProto.MetricsEvent.ACTION_TEXT_SELECTION_PASTE;
                    case 103:
                        return MetricsProto.MetricsEvent.ACTION_TEXT_SELECTION_CUT;
                    case 104:
                        return MetricsProto.MetricsEvent.ACTION_TEXT_SELECTION_SHARE;
                    case 105:
                        return MetricsProto.MetricsEvent.ACTION_TEXT_SELECTION_SMART_SHARE;
                    case 106:
                        return MetricsProto.MetricsEvent.ACTION_TEXT_SELECTION_DRAG;
                    case 107:
                        return MetricsProto.MetricsEvent.ACTION_TEXT_SELECTION_ABANDON;
                    case 108:
                        return MetricsProto.MetricsEvent.ACTION_TEXT_SELECTION_OTHER;
                    default:
                        switch (i) {
                            case 200:
                                return MetricsProto.MetricsEvent.ACTION_TEXT_SELECTION_SELECT_ALL;
                            case 201:
                                return MetricsProto.MetricsEvent.ACTION_TEXT_SELECTION_RESET;
                            default:
                                return 0;
                        }
                }
        }
    }

    private static String getLogTypeString(int i) {
        switch (i) {
            case 1101:
                return "SELECTION_STARTED";
            case MetricsProto.MetricsEvent.ACTION_TEXT_SELECTION_MODIFY:
                return "SELECTION_MODIFIED";
            case MetricsProto.MetricsEvent.ACTION_TEXT_SELECTION_SELECT_ALL:
                return "SELECT_ALL";
            case MetricsProto.MetricsEvent.ACTION_TEXT_SELECTION_RESET:
                return "RESET";
            case MetricsProto.MetricsEvent.ACTION_TEXT_SELECTION_SMART_SINGLE:
                return "SMART_SELECTION_SINGLE";
            case MetricsProto.MetricsEvent.ACTION_TEXT_SELECTION_SMART_MULTI:
                return "SMART_SELECTION_MULTI";
            case MetricsProto.MetricsEvent.ACTION_TEXT_SELECTION_AUTO:
                return "AUTO_SELECTION";
            case MetricsProto.MetricsEvent.ACTION_TEXT_SELECTION_OVERTYPE:
                return "OVERTYPE";
            case MetricsProto.MetricsEvent.ACTION_TEXT_SELECTION_COPY:
                return "COPY";
            case MetricsProto.MetricsEvent.ACTION_TEXT_SELECTION_PASTE:
                return "PASTE";
            case MetricsProto.MetricsEvent.ACTION_TEXT_SELECTION_CUT:
                return "CUT";
            case MetricsProto.MetricsEvent.ACTION_TEXT_SELECTION_SHARE:
                return "SHARE";
            case MetricsProto.MetricsEvent.ACTION_TEXT_SELECTION_SMART_SHARE:
                return "SMART_SHARE";
            case MetricsProto.MetricsEvent.ACTION_TEXT_SELECTION_DRAG:
                return "DRAG";
            case MetricsProto.MetricsEvent.ACTION_TEXT_SELECTION_ABANDON:
                return "ABANDON";
            case MetricsProto.MetricsEvent.ACTION_TEXT_SELECTION_OTHER:
                return "OTHER";
            default:
                return "unknown";
        }
    }

    private int getRangeDelta(int i) {
        return i - this.mOrigStart;
    }

    private int getSmartRangeDelta(int i) {
        if (this.mSmartSelectionTriggered) {
            return getRangeDelta(i);
        }
        return 0;
    }

    private String getWidgetTypeName() {
        switch (this.mWidgetType) {
            case 1:
                return "textview";
            case 2:
                return "webview";
            case 3:
                return "edittext";
            case 4:
                return "edit-webview";
            case 5:
                return "nosel-textview";
            case 6:
                return "customview";
            case 7:
                return "customedit";
            case 8:
                return "nosel-customview";
            default:
                return "unknown";
        }
    }

    private String getModelName(SelectionEvent selectionEvent) {
        if (selectionEvent == null) {
            return "";
        }
        return Objects.toString(selectionEvent.mVersionTag, "");
    }

    private static String createSessionId() {
        return UUID.randomUUID().toString();
    }

    private static void debugLog(LogMaker logMaker) {
        String string = Objects.toString(logMaker.getTaggedData(1255), "unknown");
        String string2 = Objects.toString(logMaker.getTaggedData(1262), "");
        if (!string2.isEmpty()) {
            string = string + NativeLibraryHelper.CLEAR_ABI_OVERRIDE + string2;
        }
        int i = Integer.parseInt(Objects.toString(logMaker.getTaggedData(1120), "0"));
        if (logMaker.getType() == 1101) {
            String string3 = Objects.toString(logMaker.getTaggedData(1119), "");
            Log.d(LOG_TAG, String.format("New selection session: %s (%s)", string, string3.substring(string3.lastIndexOf(NativeLibraryHelper.CLEAR_ABI_OVERRIDE) + 1)));
        }
        String string4 = Objects.toString(logMaker.getTaggedData(1256), "unknown");
        Log.d(LOG_TAG, String.format("%2d: %s/%s, range=%d,%d - smart_range=%d,%d (%s/%s)", Integer.valueOf(i), getLogTypeString(logMaker.getType()), Objects.toString(logMaker.getTaggedData(1254), "unknown"), Integer.valueOf(Integer.parseInt(Objects.toString(logMaker.getTaggedData(1250), "0"))), Integer.valueOf(Integer.parseInt(Objects.toString(logMaker.getTaggedData(1251), "0"))), Integer.valueOf(Integer.parseInt(Objects.toString(logMaker.getTaggedData(1252), "0"))), Integer.valueOf(Integer.parseInt(Objects.toString(logMaker.getTaggedData(1253), "0"))), string, string4));
    }

    public static final class SelectionEvent {
        private static final String NO_VERSION_TAG = "";
        public static final int OUT_OF_BOUNDS = Integer.MAX_VALUE;
        public static final int OUT_OF_BOUNDS_NEGATIVE = Integer.MIN_VALUE;
        private final int mEnd;
        private final String mEntityType;
        private int mEventType;
        private final int mStart;
        private final String mVersionTag;

        @Retention(RetentionPolicy.SOURCE)
        public @interface ActionType {
            public static final int ABANDON = 107;
            public static final int COPY = 101;
            public static final int CUT = 103;
            public static final int DRAG = 106;
            public static final int OTHER = 108;
            public static final int OVERTYPE = 100;
            public static final int PASTE = 102;
            public static final int RESET = 201;
            public static final int SELECT_ALL = 200;
            public static final int SHARE = 104;
            public static final int SMART_SHARE = 105;
        }

        @Retention(RetentionPolicy.SOURCE)
        private @interface EventType {
            public static final int AUTO_SELECTION = 5;
            public static final int SELECTION_MODIFIED = 2;
            public static final int SELECTION_STARTED = 1;
            public static final int SMART_SELECTION_MULTI = 4;
            public static final int SMART_SELECTION_SINGLE = 3;
        }

        private SelectionEvent(int i, int i2, int i3, String str, String str2) {
            Preconditions.checkArgument(i2 >= i, "end cannot be less than start");
            this.mStart = i;
            this.mEnd = i2;
            this.mEventType = i3;
            this.mEntityType = (String) Preconditions.checkNotNull(str);
            this.mVersionTag = (String) Preconditions.checkNotNull(str2);
        }

        public static SelectionEvent selectionStarted(int i) {
            return new SelectionEvent(i, i + 1, 1, "", "");
        }

        public static SelectionEvent selectionModified(int i, int i2) {
            return new SelectionEvent(i, i2, 2, "", "");
        }

        public static SelectionEvent selectionModified(int i, int i2, TextClassification textClassification) {
            String entity;
            if (textClassification.getEntityCount() > 0) {
                entity = textClassification.getEntity(0);
            } else {
                entity = "";
            }
            return new SelectionEvent(i, i2, 2, entity, getVersionInfo(textClassification.getId()));
        }

        public static SelectionEvent selectionModified(int i, int i2, TextSelection textSelection) {
            int i3;
            String entity;
            if (getSourceClassifier(textSelection.getId()).equals(TextClassifier.DEFAULT_LOG_TAG)) {
                if (i2 - i > 1) {
                    i3 = 4;
                } else {
                    i3 = 3;
                }
            } else {
                i3 = 5;
            }
            int i4 = i3;
            if (textSelection.getEntityCount() > 0) {
                entity = textSelection.getEntity(0);
            } else {
                entity = "";
            }
            return new SelectionEvent(i, i2, i4, entity, getVersionInfo(textSelection.getId()));
        }

        public static SelectionEvent selectionAction(int i, int i2, int i3) {
            return new SelectionEvent(i, i2, i3, "", "");
        }

        public static SelectionEvent selectionAction(int i, int i2, int i3, TextClassification textClassification) {
            String entity;
            if (textClassification.getEntityCount() > 0) {
                entity = textClassification.getEntity(0);
            } else {
                entity = "";
            }
            return new SelectionEvent(i, i2, i3, entity, getVersionInfo(textClassification.getId()));
        }

        private static String getVersionInfo(String str) {
            int iIndexOf = str.indexOf("|");
            int iIndexOf2 = str.indexOf("|", iIndexOf);
            if (iIndexOf >= 0 && iIndexOf2 >= iIndexOf) {
                return str.substring(iIndexOf, iIndexOf2);
            }
            return "";
        }

        private static String getSourceClassifier(String str) {
            int iIndexOf = str.indexOf("|");
            if (iIndexOf >= 0) {
                return str.substring(0, iIndexOf);
            }
            return "";
        }

        private boolean isTerminal() {
            switch (this.mEventType) {
                case 100:
                case 101:
                case 102:
                case 103:
                case 104:
                case 105:
                case 106:
                case 107:
                case 108:
                    return true;
                default:
                    return false;
            }
        }
    }
}
