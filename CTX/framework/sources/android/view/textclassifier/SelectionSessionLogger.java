package android.view.textclassifier;

import android.content.Context;
import android.metrics.LogMaker;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.logging.MetricsLogger;
import com.android.internal.logging.nano.MetricsProto;
import com.android.internal.util.Preconditions;
import java.text.BreakIterator;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.StringJoiner;

public final class SelectionSessionLogger {
    static final String CLASSIFIER_ID = "androidtc";
    private static final boolean DEBUG_LOG_ENABLED = false;
    private static final int ENTITY_TYPE = 1254;
    private static final int EVENT_END = 1251;
    private static final int EVENT_START = 1250;
    private static final int INDEX = 1120;
    private static final String LOG_TAG = "SelectionSessionLogger";
    private static final int MODEL_NAME = 1256;
    private static final int PREV_EVENT_DELTA = 1118;
    private static final int SESSION_ID = 1119;
    private static final int SMART_END = 1253;
    private static final int SMART_START = 1252;
    private static final int START_EVENT_DELTA = 1117;
    private static final String UNKNOWN = "unknown";
    private static final int WIDGET_TYPE = 1255;
    private static final int WIDGET_VERSION = 1262;
    private static final String ZERO = "0";
    private final MetricsLogger mMetricsLogger;

    public SelectionSessionLogger() {
        this.mMetricsLogger = new MetricsLogger();
    }

    @VisibleForTesting
    public SelectionSessionLogger(MetricsLogger metricsLogger) {
        this.mMetricsLogger = (MetricsLogger) Preconditions.checkNotNull(metricsLogger);
    }

    public void writeEvent(SelectionEvent selectionEvent) {
        Preconditions.checkNotNull(selectionEvent);
        LogMaker logMakerAddTaggedData = new LogMaker(1100).setType(getLogType(selectionEvent)).setSubtype(getLogSubType(selectionEvent)).setPackageName(selectionEvent.getPackageName()).addTaggedData(1117, Long.valueOf(selectionEvent.getDurationSinceSessionStart())).addTaggedData(1118, Long.valueOf(selectionEvent.getDurationSincePreviousEvent())).addTaggedData(1120, Integer.valueOf(selectionEvent.getEventIndex())).addTaggedData(1255, selectionEvent.getWidgetType()).addTaggedData(1262, selectionEvent.getWidgetVersion()).addTaggedData(1256, SignatureParser.getModelName(selectionEvent.getResultId())).addTaggedData(1254, selectionEvent.getEntityType()).addTaggedData(1252, Integer.valueOf(selectionEvent.getSmartStart())).addTaggedData(1253, Integer.valueOf(selectionEvent.getSmartEnd())).addTaggedData(1250, Integer.valueOf(selectionEvent.getStart())).addTaggedData(1251, Integer.valueOf(selectionEvent.getEnd()));
        if (selectionEvent.getSessionId() != null) {
            logMakerAddTaggedData.addTaggedData(1119, selectionEvent.getSessionId().flattenToString());
        }
        this.mMetricsLogger.write(logMakerAddTaggedData);
        debugLog(logMakerAddTaggedData);
    }

    private static int getLogType(SelectionEvent selectionEvent) {
        int eventType = selectionEvent.getEventType();
        switch (eventType) {
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
                switch (eventType) {
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
                        switch (eventType) {
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

    private static int getLogSubType(SelectionEvent selectionEvent) {
        switch (selectionEvent.getInvocationMethod()) {
            case 1:
                return 1;
            case 2:
                return 2;
            default:
                return 0;
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

    private static String getLogSubTypeString(int i) {
        switch (i) {
            case 1:
                return "MANUAL";
            case 2:
                return "LINK";
            default:
                return "unknown";
        }
    }

    private static void debugLog(LogMaker logMaker) {
    }

    public static BreakIterator getTokenIterator(Locale locale) {
        return BreakIterator.getWordInstance((Locale) Preconditions.checkNotNull(locale));
    }

    public static String createId(String str, int i, int i2, Context context, int i3, List<Locale> list) {
        Preconditions.checkNotNull(str);
        Preconditions.checkNotNull(context);
        Preconditions.checkNotNull(list);
        StringJoiner stringJoiner = new StringJoiner(",");
        Iterator<Locale> it = list.iterator();
        while (it.hasNext()) {
            stringJoiner.add(it.next().toLanguageTag());
        }
        return SignatureParser.createSignature("androidtc", String.format(Locale.US, "%s_v%d", stringJoiner.toString(), Integer.valueOf(i3)), Objects.hash(str, Integer.valueOf(i), Integer.valueOf(i2), context.getPackageName()));
    }

    @VisibleForTesting
    public static final class SignatureParser {
        static String createSignature(String str, String str2, int i) {
            return String.format(Locale.US, "%s|%s|%d", str, str2, Integer.valueOf(i));
        }

        static String getClassifierId(String str) {
            int iIndexOf;
            if (str != null && (iIndexOf = str.indexOf("|")) >= 0) {
                return str.substring(0, iIndexOf);
            }
            return "";
        }

        static String getModelName(String str) {
            if (str == null) {
                return "";
            }
            int iIndexOf = str.indexOf("|") + 1;
            int iIndexOf2 = str.indexOf("|", iIndexOf);
            if (iIndexOf >= 1 && iIndexOf2 >= iIndexOf) {
                return str.substring(iIndexOf, iIndexOf2);
            }
            return "";
        }

        static int getHash(String str) {
            int iIndexOf;
            if (str == null || (iIndexOf = str.indexOf("|", str.indexOf("|"))) <= 0) {
                return 0;
            }
            return Integer.parseInt(str.substring(iIndexOf));
        }
    }
}
