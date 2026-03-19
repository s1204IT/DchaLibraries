package mediatek.telecom;

import android.telephony.PhoneNumberUtils;
import java.util.IllegalFormatException;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public class FormattedLog {
    private String mLogString;

    public enum OpType {
        OPERATION,
        NOTIFY,
        DUMP
    }

    FormattedLog(String str, Object[] objArr, AnonymousClass1 anonymousClass1) {
        this(str, objArr);
    }

    static class AnonymousClass1 {
        static final int[] $SwitchMap$mediatek$telecom$FormattedLog$OpType = new int[OpType.values().length];

        static {
            try {
                $SwitchMap$mediatek$telecom$FormattedLog$OpType[OpType.OPERATION.ordinal()] = 1;
            } catch (NoSuchFieldError e) {
            }
            try {
                $SwitchMap$mediatek$telecom$FormattedLog$OpType[OpType.NOTIFY.ordinal()] = 2;
            } catch (NoSuchFieldError e2) {
            }
            try {
                $SwitchMap$mediatek$telecom$FormattedLog$OpType[OpType.DUMP.ordinal()] = 3;
            } catch (NoSuchFieldError e3) {
            }
        }
    }

    private static String opTypeToString(OpType opType) {
        switch (AnonymousClass1.$SwitchMap$mediatek$telecom$FormattedLog$OpType[opType.ordinal()]) {
            case 1:
                return "OP";
            case MtkTelecomManager.ECT_TYPE_ASSURED:
                return "Notify";
            case 3:
                return "Dump";
            default:
                return null;
        }
    }

    public static class Builder {
        private String mAction;
        private String mCallId;
        private String mCallNumber;
        private String mCategory;
        private OpType mOpType;
        private String mServiceName;
        private LinkedHashMap<String, String> mStatusInfo = new LinkedHashMap<>();
        private StringBuilder mExtraMessage = new StringBuilder();

        public synchronized Builder setCategory(String str) {
            this.mCategory = str;
            return this;
        }

        public synchronized Builder setServiceName(String str) {
            this.mServiceName = str;
            return this;
        }

        public synchronized Builder setOpType(OpType opType) {
            this.mOpType = opType;
            return this;
        }

        public synchronized Builder setActionName(String str) {
            this.mAction = str;
            return this;
        }

        public synchronized Builder setCallNumber(String str) {
            if (str != null) {
                try {
                    if (!str.equals("conferenceCall") && !PhoneNumberUtils.isUriNumber(str)) {
                        this.mCallNumber = PhoneNumberUtils.extractNetworkPortionAlt(str);
                    } else {
                        this.mCallNumber = str;
                    }
                } catch (Throwable th) {
                    throw th;
                }
            }
            return this;
        }

        public synchronized Builder setCallId(String str) {
            this.mCallId = str;
            return this;
        }

        public synchronized Builder setStatusInfo(String str, String str2) {
            if (str != null && str2 != null) {
                if (!str.isEmpty() && !str2.isEmpty()) {
                    this.mStatusInfo.put(str, str2);
                }
            }
            return this;
        }

        public synchronized Builder resetStatusInfo(String str) {
            if (str != null) {
                if (!str.isEmpty()) {
                    this.mStatusInfo.remove(str);
                }
            }
            return this;
        }

        public synchronized Builder setExtraMessage(String str) {
            if (str != null) {
                this.mExtraMessage = new StringBuilder();
                this.mExtraMessage.append(str);
            }
            return this;
        }

        public synchronized Builder appendExtraMessage(String str) {
            if (str != null) {
                this.mExtraMessage.append(str);
            }
            return this;
        }

        public synchronized FormattedLog buildDebugMsg() {
            if (this.mCallNumber == null) {
                this.mCallNumber = "unknown";
            }
            return new FormattedLog("[Debug][%s][%s][%s][%s][%s][%s] %s", new Object[]{this.mCategory, this.mServiceName, FormattedLog.opTypeToString(this.mOpType), this.mAction, this.mCallNumber, this.mCallId, this.mExtraMessage}, null);
        }

        public synchronized FormattedLog buildDumpInfo() {
            StringBuilder sb;
            sb = new StringBuilder();
            Iterator<Map.Entry<String, String>> it = this.mStatusInfo.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<String, String> next = it.next();
                sb.append("[");
                sb.append(next.getKey());
                sb.append(":");
                sb.append(next.getValue());
                sb.append("]");
                if (it.hasNext()) {
                    sb.append(",");
                }
            }
            if (this.mCallNumber == null) {
                this.mCallNumber = "unknown";
            }
            return new FormattedLog("[Debug][%s][%s][Dump][%s][%s]-%s-%s", new Object[]{this.mCategory, this.mServiceName, this.mCallNumber, this.mCallId, sb, this.mExtraMessage}, null);
        }
    }

    private FormattedLog(String str, Object... objArr) {
        String str2;
        if (objArr != null) {
            try {
                str2 = objArr.length == 0 ? str : String.format(Locale.US, str, objArr);
            } catch (IllegalFormatException e) {
                this.mLogString = str + " (An error occurred while formatting the message.)";
                return;
            }
        }
        this.mLogString = str2;
    }

    public String toString() {
        return this.mLogString;
    }
}
