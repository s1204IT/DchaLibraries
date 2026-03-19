package android.app;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.mtp.MtpConstants;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.SystemProperties;
import android.provider.Settings;
import android.util.Printer;
import com.android.internal.util.FastPrintWriter;
import java.io.StringWriter;
import java.io.Writer;

public class ApplicationErrorReport implements Parcelable {
    public static final Parcelable.Creator<ApplicationErrorReport> CREATOR = new Parcelable.Creator<ApplicationErrorReport>() {
        @Override
        public ApplicationErrorReport createFromParcel(Parcel parcel) {
            return new ApplicationErrorReport(parcel);
        }

        @Override
        public ApplicationErrorReport[] newArray(int i) {
            return new ApplicationErrorReport[i];
        }
    };
    static final String DEFAULT_ERROR_RECEIVER_PROPERTY = "ro.error.receiver.default";
    static final String SYSTEM_APPS_ERROR_RECEIVER_PROPERTY = "ro.error.receiver.system.apps";
    public static final int TYPE_ANR = 2;
    public static final int TYPE_BATTERY = 3;
    public static final int TYPE_CRASH = 1;
    public static final int TYPE_NONE = 0;
    public static final int TYPE_RUNNING_SERVICE = 5;
    public AnrInfo anrInfo;
    public BatteryInfo batteryInfo;
    public CrashInfo crashInfo;
    public String installerPackageName;
    public String packageName;
    public String processName;
    public RunningServiceInfo runningServiceInfo;
    public boolean systemApp;
    public long time;
    public int type;

    public ApplicationErrorReport() {
    }

    ApplicationErrorReport(Parcel parcel) {
        readFromParcel(parcel);
    }

    public static ComponentName getErrorReportReceiver(Context context, String str, int i) {
        String installerPackageName;
        ComponentName errorReportReceiver;
        ComponentName errorReportReceiver2;
        if (Settings.Global.getInt(context.getContentResolver(), Settings.Global.SEND_ACTION_APP_ERROR, 0) == 0) {
            return null;
        }
        PackageManager packageManager = context.getPackageManager();
        try {
            installerPackageName = packageManager.getInstallerPackageName(str);
        } catch (IllegalArgumentException e) {
            installerPackageName = null;
        }
        if (installerPackageName != null && (errorReportReceiver2 = getErrorReportReceiver(packageManager, str, installerPackageName)) != null) {
            return errorReportReceiver2;
        }
        if ((i & 1) != 0 && (errorReportReceiver = getErrorReportReceiver(packageManager, str, SystemProperties.get(SYSTEM_APPS_ERROR_RECEIVER_PROPERTY))) != null) {
            return errorReportReceiver;
        }
        return getErrorReportReceiver(packageManager, str, SystemProperties.get(DEFAULT_ERROR_RECEIVER_PROPERTY));
    }

    static ComponentName getErrorReportReceiver(PackageManager packageManager, String str, String str2) {
        if (str2 == null || str2.length() == 0 || str2.equals(str)) {
            return null;
        }
        Intent intent = new Intent(Intent.ACTION_APP_ERROR);
        intent.setPackage(str2);
        ResolveInfo resolveInfoResolveActivity = packageManager.resolveActivity(intent, 0);
        if (resolveInfoResolveActivity == null || resolveInfoResolveActivity.activityInfo == null) {
            return null;
        }
        return new ComponentName(str2, resolveInfoResolveActivity.activityInfo.name);
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(this.type);
        parcel.writeString(this.packageName);
        parcel.writeString(this.installerPackageName);
        parcel.writeString(this.processName);
        parcel.writeLong(this.time);
        parcel.writeInt(this.systemApp ? 1 : 0);
        parcel.writeInt(this.crashInfo != null ? 1 : 0);
        int i2 = this.type;
        if (i2 != 5) {
            switch (i2) {
                case 1:
                    if (this.crashInfo != null) {
                        this.crashInfo.writeToParcel(parcel, i);
                    }
                    break;
                case 2:
                    this.anrInfo.writeToParcel(parcel, i);
                    break;
                case 3:
                    this.batteryInfo.writeToParcel(parcel, i);
                    break;
            }
        }
        this.runningServiceInfo.writeToParcel(parcel, i);
    }

    public void readFromParcel(Parcel parcel) {
        this.type = parcel.readInt();
        this.packageName = parcel.readString();
        this.installerPackageName = parcel.readString();
        this.processName = parcel.readString();
        this.time = parcel.readLong();
        this.systemApp = parcel.readInt() == 1;
        boolean z = parcel.readInt() == 1;
        int i = this.type;
        if (i != 5) {
            switch (i) {
                case 1:
                    this.crashInfo = z ? new CrashInfo(parcel) : null;
                    this.anrInfo = null;
                    this.batteryInfo = null;
                    this.runningServiceInfo = null;
                    break;
                case 2:
                    this.anrInfo = new AnrInfo(parcel);
                    this.crashInfo = null;
                    this.batteryInfo = null;
                    this.runningServiceInfo = null;
                    break;
                case 3:
                    this.batteryInfo = new BatteryInfo(parcel);
                    this.anrInfo = null;
                    this.crashInfo = null;
                    this.runningServiceInfo = null;
                    break;
            }
        }
        this.batteryInfo = null;
        this.anrInfo = null;
        this.crashInfo = null;
        this.runningServiceInfo = new RunningServiceInfo(parcel);
    }

    public static class CrashInfo {
        public String exceptionClassName;
        public String exceptionMessage;
        public String stackTrace;
        public String throwClassName;
        public String throwFileName;
        public int throwLineNumber;
        public String throwMethodName;

        public CrashInfo() {
        }

        public CrashInfo(Throwable th) {
            StringWriter stringWriter = new StringWriter();
            FastPrintWriter fastPrintWriter = new FastPrintWriter((Writer) stringWriter, false, 256);
            th.printStackTrace(fastPrintWriter);
            fastPrintWriter.flush();
            this.stackTrace = sanitizeString(stringWriter.toString());
            this.exceptionMessage = th.getMessage();
            Throwable th2 = th;
            while (th.getCause() != null) {
                th = th.getCause();
                if (th.getStackTrace() != null && th.getStackTrace().length > 0) {
                    th2 = th;
                }
                String message = th.getMessage();
                if (message != null && message.length() > 0) {
                    this.exceptionMessage = message;
                }
            }
            this.exceptionClassName = th2.getClass().getName();
            if (th2.getStackTrace().length > 0) {
                StackTraceElement stackTraceElement = th2.getStackTrace()[0];
                this.throwFileName = stackTraceElement.getFileName();
                this.throwClassName = stackTraceElement.getClassName();
                this.throwMethodName = stackTraceElement.getMethodName();
                this.throwLineNumber = stackTraceElement.getLineNumber();
            } else {
                this.throwFileName = "unknown";
                this.throwClassName = "unknown";
                this.throwMethodName = "unknown";
                this.throwLineNumber = 0;
            }
            this.exceptionMessage = sanitizeString(this.exceptionMessage);
        }

        public void appendStackTrace(String str) {
            this.stackTrace = sanitizeString(this.stackTrace + str);
        }

        private String sanitizeString(String str) {
            if (str != null && str.length() > 20480) {
                String str2 = "\n[TRUNCATED " + (str.length() - MtpConstants.DEVICE_PROPERTY_UNDEFINED) + " CHARS]\n";
                StringBuilder sb = new StringBuilder(MtpConstants.DEVICE_PROPERTY_UNDEFINED + str2.length());
                sb.append(str.substring(0, 10240));
                sb.append(str2);
                sb.append(str.substring(str.length() - 10240));
                return sb.toString();
            }
            return str;
        }

        public CrashInfo(Parcel parcel) {
            this.exceptionClassName = parcel.readString();
            this.exceptionMessage = parcel.readString();
            this.throwFileName = parcel.readString();
            this.throwClassName = parcel.readString();
            this.throwMethodName = parcel.readString();
            this.throwLineNumber = parcel.readInt();
            this.stackTrace = parcel.readString();
        }

        public void writeToParcel(Parcel parcel, int i) {
            parcel.dataPosition();
            parcel.writeString(this.exceptionClassName);
            parcel.writeString(this.exceptionMessage);
            parcel.writeString(this.throwFileName);
            parcel.writeString(this.throwClassName);
            parcel.writeString(this.throwMethodName);
            parcel.writeInt(this.throwLineNumber);
            parcel.writeString(this.stackTrace);
            parcel.dataPosition();
        }

        public void dump(Printer printer, String str) {
            printer.println(str + "exceptionClassName: " + this.exceptionClassName);
            printer.println(str + "exceptionMessage: " + this.exceptionMessage);
            printer.println(str + "throwFileName: " + this.throwFileName);
            printer.println(str + "throwClassName: " + this.throwClassName);
            printer.println(str + "throwMethodName: " + this.throwMethodName);
            printer.println(str + "throwLineNumber: " + this.throwLineNumber);
            printer.println(str + "stackTrace: " + this.stackTrace);
        }
    }

    public static class ParcelableCrashInfo extends CrashInfo implements Parcelable {
        public static final Parcelable.Creator<ParcelableCrashInfo> CREATOR = new Parcelable.Creator<ParcelableCrashInfo>() {
            @Override
            public ParcelableCrashInfo createFromParcel(Parcel parcel) {
                return new ParcelableCrashInfo(parcel);
            }

            @Override
            public ParcelableCrashInfo[] newArray(int i) {
                return new ParcelableCrashInfo[i];
            }
        };

        public ParcelableCrashInfo() {
        }

        public ParcelableCrashInfo(Throwable th) {
            super(th);
        }

        public ParcelableCrashInfo(Parcel parcel) {
            super(parcel);
        }

        @Override
        public int describeContents() {
            return 0;
        }
    }

    public static class AnrInfo {
        public String activity;
        public String cause;
        public String info;

        public AnrInfo() {
        }

        public AnrInfo(Parcel parcel) {
            this.activity = parcel.readString();
            this.cause = parcel.readString();
            this.info = parcel.readString();
        }

        public void writeToParcel(Parcel parcel, int i) {
            parcel.writeString(this.activity);
            parcel.writeString(this.cause);
            parcel.writeString(this.info);
        }

        public void dump(Printer printer, String str) {
            printer.println(str + "activity: " + this.activity);
            printer.println(str + "cause: " + this.cause);
            printer.println(str + "info: " + this.info);
        }
    }

    public static class BatteryInfo {
        public String checkinDetails;
        public long durationMicros;
        public String usageDetails;
        public int usagePercent;

        public BatteryInfo() {
        }

        public BatteryInfo(Parcel parcel) {
            this.usagePercent = parcel.readInt();
            this.durationMicros = parcel.readLong();
            this.usageDetails = parcel.readString();
            this.checkinDetails = parcel.readString();
        }

        public void writeToParcel(Parcel parcel, int i) {
            parcel.writeInt(this.usagePercent);
            parcel.writeLong(this.durationMicros);
            parcel.writeString(this.usageDetails);
            parcel.writeString(this.checkinDetails);
        }

        public void dump(Printer printer, String str) {
            printer.println(str + "usagePercent: " + this.usagePercent);
            printer.println(str + "durationMicros: " + this.durationMicros);
            printer.println(str + "usageDetails: " + this.usageDetails);
            printer.println(str + "checkinDetails: " + this.checkinDetails);
        }
    }

    public static class RunningServiceInfo {
        public long durationMillis;
        public String serviceDetails;

        public RunningServiceInfo() {
        }

        public RunningServiceInfo(Parcel parcel) {
            this.durationMillis = parcel.readLong();
            this.serviceDetails = parcel.readString();
        }

        public void writeToParcel(Parcel parcel, int i) {
            parcel.writeLong(this.durationMillis);
            parcel.writeString(this.serviceDetails);
        }

        public void dump(Printer printer, String str) {
            printer.println(str + "durationMillis: " + this.durationMillis);
            printer.println(str + "serviceDetails: " + this.serviceDetails);
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public void dump(Printer printer, String str) {
        printer.println(str + "type: " + this.type);
        printer.println(str + "packageName: " + this.packageName);
        printer.println(str + "installerPackageName: " + this.installerPackageName);
        printer.println(str + "processName: " + this.processName);
        printer.println(str + "time: " + this.time);
        printer.println(str + "systemApp: " + this.systemApp);
        int i = this.type;
        if (i != 5) {
            switch (i) {
                case 1:
                    this.crashInfo.dump(printer, str);
                    break;
                case 2:
                    this.anrInfo.dump(printer, str);
                    break;
                case 3:
                    this.batteryInfo.dump(printer, str);
                    break;
            }
        }
        this.runningServiceInfo.dump(printer, str);
    }
}
