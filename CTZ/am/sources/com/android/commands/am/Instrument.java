package com.android.commands.am;

import android.app.IActivityManager;
import android.app.IInstrumentationWatcher;
import android.app.IUiAutomationConnection;
import android.app.UiAutomationConnection;
import android.content.ComponentName;
import android.content.pm.IPackageManager;
import android.content.pm.InstrumentationInfo;
import android.content.pm.PackageItemInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.ServiceManager;
import android.util.AndroidException;
import android.util.proto.ProtoOutputStream;
import android.view.IWindowManager;
import com.android.commands.am.InstrumentationData;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class Instrument {
    public static final String DEFAULT_LOG_DIR = "instrument-logs";
    private static final int INSTRUMENTATION_FLAG_DISABLE_HIDDEN_API_CHECKS = 1;
    public String componentNameArg;
    private final IActivityManager mAm;
    private final IPackageManager mPm;
    public String profileFile = null;
    public boolean wait = false;
    public boolean rawMode = false;
    boolean protoStd = false;
    boolean protoFile = false;
    String logPath = null;
    public boolean noWindowAnimation = false;
    public boolean disableHiddenApiChecks = false;
    public String abi = null;
    public int userId = -2;
    public Bundle args = new Bundle();
    private final IWindowManager mWm = IWindowManager.Stub.asInterface(ServiceManager.getService("window"));

    private interface StatusReporter {
        void onError(String str, boolean z);

        void onInstrumentationFinishedLocked(ComponentName componentName, int i, Bundle bundle);

        void onInstrumentationStatusLocked(ComponentName componentName, int i, Bundle bundle);
    }

    public Instrument(IActivityManager iActivityManager, IPackageManager iPackageManager) {
        this.mAm = iActivityManager;
        this.mPm = iPackageManager;
    }

    private static Collection<String> sorted(Collection<String> collection) {
        ArrayList arrayList = new ArrayList(collection);
        Collections.sort(arrayList);
        return arrayList;
    }

    private class TextStatusReporter implements StatusReporter {
        private boolean mRawMode;

        public TextStatusReporter(boolean z) {
            this.mRawMode = z;
        }

        @Override
        public void onInstrumentationStatusLocked(ComponentName componentName, int i, Bundle bundle) {
            String string;
            if (!this.mRawMode && bundle != null) {
                string = bundle.getString("stream");
            } else {
                string = null;
            }
            if (string != null) {
                System.out.print(string);
                return;
            }
            if (bundle != null) {
                for (String str : Instrument.sorted(bundle.keySet())) {
                    System.out.println("INSTRUMENTATION_STATUS: " + str + "=" + bundle.get(str));
                }
            }
            System.out.println("INSTRUMENTATION_STATUS_CODE: " + i);
        }

        @Override
        public void onInstrumentationFinishedLocked(ComponentName componentName, int i, Bundle bundle) {
            String string;
            if (!this.mRawMode && bundle != null) {
                string = bundle.getString("stream");
            } else {
                string = null;
            }
            if (string != null) {
                System.out.println(string);
                return;
            }
            if (bundle != null) {
                for (String str : Instrument.sorted(bundle.keySet())) {
                    System.out.println("INSTRUMENTATION_RESULT: " + str + "=" + bundle.get(str));
                }
            }
            System.out.println("INSTRUMENTATION_CODE: " + i);
        }

        @Override
        public void onError(String str, boolean z) {
            if (this.mRawMode) {
                System.out.println("onError: commandError=" + z + " message=" + str);
            }
            if (!z) {
                System.out.println(str);
            }
        }
    }

    private class ProtoStatusReporter implements StatusReporter {
        private File mLog;

        ProtoStatusReporter() {
            if (Instrument.this.protoFile) {
                if (Instrument.this.logPath == null) {
                    File file = new File(Environment.getLegacyExternalStorageDirectory(), Instrument.DEFAULT_LOG_DIR);
                    if (!file.exists() && !file.mkdirs()) {
                        System.err.format("Unable to create log directory: %s\n", file.getAbsolutePath());
                        Instrument.this.protoFile = false;
                        return;
                    }
                    this.mLog = new File(file, String.format("log-%s.instrumentation_data_proto", new SimpleDateFormat("yyyyMMdd-hhmmss-SSS", Locale.US).format(new Date())));
                } else {
                    this.mLog = new File(Environment.getLegacyExternalStorageDirectory(), Instrument.this.logPath);
                    File parentFile = this.mLog.getParentFile();
                    if (!parentFile.exists() && !parentFile.mkdirs()) {
                        System.err.format("Unable to create log directory: %s\n", parentFile.getAbsolutePath());
                        Instrument.this.protoFile = false;
                        return;
                    }
                }
                if (this.mLog.exists()) {
                    this.mLog.delete();
                }
            }
        }

        @Override
        public void onInstrumentationStatusLocked(ComponentName componentName, int i, Bundle bundle) {
            ProtoOutputStream protoOutputStream = new ProtoOutputStream();
            long jStart = protoOutputStream.start(2246267895809L);
            protoOutputStream.write(1172526071811L, i);
            writeBundle(protoOutputStream, 1146756268036L, bundle);
            protoOutputStream.end(jStart);
            outputProto(protoOutputStream);
        }

        @Override
        public void onInstrumentationFinishedLocked(ComponentName componentName, int i, Bundle bundle) {
            ProtoOutputStream protoOutputStream = new ProtoOutputStream();
            long jStart = protoOutputStream.start(InstrumentationData.Session.SESSION_STATUS);
            protoOutputStream.write(InstrumentationData.SessionStatus.STATUS_CODE, 0);
            protoOutputStream.write(1172526071811L, i);
            writeBundle(protoOutputStream, 1146756268036L, bundle);
            protoOutputStream.end(jStart);
            outputProto(protoOutputStream);
        }

        @Override
        public void onError(String str, boolean z) {
            ProtoOutputStream protoOutputStream = new ProtoOutputStream();
            long jStart = protoOutputStream.start(InstrumentationData.Session.SESSION_STATUS);
            protoOutputStream.write(InstrumentationData.SessionStatus.STATUS_CODE, 1);
            protoOutputStream.write(1138166333442L, str);
            protoOutputStream.end(jStart);
            outputProto(protoOutputStream);
        }

        private void writeBundle(ProtoOutputStream protoOutputStream, long j, Bundle bundle) {
            long jStart = protoOutputStream.start(j);
            for (String str : Instrument.sorted(bundle.keySet())) {
                long jStartRepeatedObject = protoOutputStream.startRepeatedObject(2246267895809L);
                protoOutputStream.write(InstrumentationData.ResultsBundleEntry.KEY, str);
                ?? r1 = bundle.get(str);
                if (r1 instanceof String) {
                    protoOutputStream.write(1138166333442L, (String) r1);
                } else if (r1 instanceof Byte) {
                    protoOutputStream.write(1172526071811L, ((Byte) r1).intValue());
                } else if (r1 instanceof Double) {
                    protoOutputStream.write(InstrumentationData.ResultsBundleEntry.VALUE_DOUBLE, r1.doubleValue());
                } else if (r1 instanceof Float) {
                    protoOutputStream.write(InstrumentationData.ResultsBundleEntry.VALUE_FLOAT, r1.floatValue());
                } else if (r1 instanceof Integer) {
                    protoOutputStream.write(1172526071811L, r1.intValue());
                } else if (r1 instanceof Long) {
                    protoOutputStream.write(InstrumentationData.ResultsBundleEntry.VALUE_LONG, r1.longValue());
                } else if (r1 instanceof Short) {
                    protoOutputStream.write(1172526071811L, (int) r1.shortValue());
                } else if (r1 instanceof Bundle) {
                    writeBundle(protoOutputStream, InstrumentationData.ResultsBundleEntry.VALUE_BUNDLE, r1);
                } else if (r1 instanceof byte[]) {
                    protoOutputStream.write(InstrumentationData.ResultsBundleEntry.VALUE_BYTES, (byte[]) r1);
                }
                protoOutputStream.end(jStartRepeatedObject);
            }
            protoOutputStream.end(jStart);
        }

        private void outputProto(ProtoOutputStream protoOutputStream) {
            byte[] bytes = protoOutputStream.getBytes();
            if (Instrument.this.protoStd) {
                try {
                    System.out.write(bytes);
                    System.out.flush();
                } catch (IOException e) {
                    System.err.println("Error writing finished response: ");
                    e.printStackTrace(System.err);
                }
            }
            if (Instrument.this.protoFile) {
                try {
                    FileOutputStream fileOutputStream = new FileOutputStream(this.mLog, true);
                    Throwable th = null;
                    try {
                        try {
                            fileOutputStream.write(protoOutputStream.getBytes());
                            fileOutputStream.flush();
                            fileOutputStream.close();
                        } catch (Throwable th2) {
                            th = th2;
                            throw th;
                        }
                    } finally {
                    }
                } catch (IOException e2) {
                    System.err.format("Cannot write to %s:\n", this.mLog.getAbsolutePath());
                    e2.printStackTrace();
                }
            }
        }
    }

    private class InstrumentationWatcher extends IInstrumentationWatcher.Stub {
        private boolean mFinished = false;
        private final StatusReporter mReporter;

        public InstrumentationWatcher(StatusReporter statusReporter) {
            this.mReporter = statusReporter;
        }

        public void instrumentationStatus(ComponentName componentName, int i, Bundle bundle) {
            synchronized (this) {
                this.mReporter.onInstrumentationStatusLocked(componentName, i, bundle);
                notifyAll();
            }
        }

        public void instrumentationFinished(ComponentName componentName, int i, Bundle bundle) {
            synchronized (this) {
                this.mReporter.onInstrumentationFinishedLocked(componentName, i, bundle);
                this.mFinished = true;
                notifyAll();
            }
        }

        public boolean waitForFinish() {
            synchronized (this) {
                while (!this.mFinished) {
                    try {
                        if (!Instrument.this.mAm.asBinder().pingBinder()) {
                            return false;
                        }
                        wait(1000L);
                    } catch (InterruptedException e) {
                        throw new IllegalStateException(e);
                    }
                }
                return true;
            }
        }
    }

    private ComponentName parseComponentName(String str) throws Exception {
        int size;
        if (str.contains("/")) {
            ComponentName componentNameUnflattenFromString = ComponentName.unflattenFromString(str);
            if (componentNameUnflattenFromString == null) {
                throw new IllegalArgumentException("Bad component name: " + str);
            }
            return componentNameUnflattenFromString;
        }
        List list = this.mPm.queryInstrumentation((String) null, 0).getList();
        if (list != null) {
            size = list.size();
        } else {
            size = 0;
        }
        ArrayList arrayList = new ArrayList();
        for (int i = 0; i < size; i++) {
            InstrumentationInfo instrumentationInfo = (InstrumentationInfo) list.get(i);
            ComponentName componentName = new ComponentName(((PackageItemInfo) instrumentationInfo).packageName, ((PackageItemInfo) instrumentationInfo).name);
            if (str.equals(((PackageItemInfo) instrumentationInfo).packageName)) {
                arrayList.add(componentName);
            }
        }
        if (arrayList.size() == 0) {
            throw new IllegalArgumentException("No instrumentation found for: " + str);
        }
        if (arrayList.size() == 1) {
            return (ComponentName) arrayList.get(0);
        }
        StringBuilder sb = new StringBuilder();
        int size2 = arrayList.size();
        for (int i2 = 0; i2 < size2; i2++) {
            sb.append(((ComponentName) arrayList.get(i2)).flattenToString());
            sb.append(", ");
        }
        sb.setLength(sb.length() - 2);
        throw new IllegalArgumentException("Found multiple instrumentations: " + sb.toString());
    }

    public void run() throws Exception {
        Exception e;
        StatusReporter protoStatusReporter;
        InstrumentationWatcher instrumentationWatcher;
        IUiAutomationConnection uiAutomationConnection;
        Object[] objArr;
        float[] fArr = null;
        try {
            try {
                protoStatusReporter = (this.protoFile || this.protoStd) ? new ProtoStatusReporter() : this.wait ? new TextStatusReporter(this.rawMode) : null;
                if (protoStatusReporter != null) {
                    try {
                        instrumentationWatcher = new InstrumentationWatcher(protoStatusReporter);
                        uiAutomationConnection = new UiAutomationConnection();
                    } catch (Exception e2) {
                        e = e2;
                        if (protoStatusReporter != null) {
                            throw e;
                        }
                        protoStatusReporter.onError(e.getMessage(), true);
                        throw e;
                    }
                } else {
                    instrumentationWatcher = null;
                    uiAutomationConnection = null;
                }
                if (this.noWindowAnimation) {
                    float[] animationScales = this.mWm.getAnimationScales();
                    try {
                        this.mWm.setAnimationScale(0, 0.0f);
                        this.mWm.setAnimationScale(1, 0.0f);
                        this.mWm.setAnimationScale(2, 0.0f);
                        fArr = animationScales;
                    } catch (Exception e3) {
                        e = e3;
                        if (protoStatusReporter != null) {
                        }
                    } catch (Throwable th) {
                        th = th;
                        fArr = animationScales;
                        if (fArr != null) {
                            this.mWm.setAnimationScales(fArr);
                        }
                        throw th;
                    }
                }
                ComponentName componentName = parseComponentName(this.componentNameArg);
                if (this.abi != null) {
                    String[] strArr = Build.SUPPORTED_ABIS;
                    int length = strArr.length;
                    int i = 0;
                    while (true) {
                        if (i >= length) {
                            objArr = false;
                            break;
                        } else {
                            if (strArr[i].equals(this.abi)) {
                                objArr = true;
                                break;
                            }
                            i++;
                        }
                    }
                    if (objArr == false) {
                        throw new AndroidException("INSTRUMENTATION_FAILED: Unsupported instruction set " + this.abi);
                    }
                }
                if (!this.mAm.startInstrumentation(componentName, this.profileFile, this.disableHiddenApiChecks ? 1 : 0, this.args, instrumentationWatcher, uiAutomationConnection, this.userId, this.abi)) {
                    throw new AndroidException("INSTRUMENTATION_FAILED: " + componentName.flattenToString());
                }
                if (instrumentationWatcher == null || instrumentationWatcher.waitForFinish()) {
                    if (fArr != null) {
                        this.mWm.setAnimationScales(fArr);
                    }
                } else {
                    protoStatusReporter.onError("INSTRUMENTATION_ABORTED: System has crashed.", false);
                    if (fArr != null) {
                        this.mWm.setAnimationScales(fArr);
                    }
                }
            } catch (Exception e4) {
                e = e4;
                protoStatusReporter = null;
            }
        } catch (Throwable th2) {
            th = th2;
        }
    }
}
