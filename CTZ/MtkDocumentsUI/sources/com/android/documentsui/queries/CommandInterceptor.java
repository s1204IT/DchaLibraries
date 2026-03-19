package com.android.documentsui.queries;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;
import com.android.documentsui.DocumentsApplication;
import com.android.documentsui.R;
import com.android.documentsui.base.DebugFlags;
import com.android.documentsui.base.EventHandler;
import com.android.documentsui.base.Features;
import com.android.documentsui.base.SharedMinimal;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public final class CommandInterceptor implements EventHandler<String> {
    static final String COMMAND_PREFIX = ":";
    private final List<EventHandler<String[]>> mCommands = new ArrayList();
    private Features mFeatures;

    public CommandInterceptor(Features features) {
        this.mFeatures = features;
        this.mCommands.add(new EventHandler() {
            @Override
            public final boolean accept(Object obj) {
                return this.f$0.quickViewer((String[]) obj);
            }
        });
        this.mCommands.add(new EventHandler() {
            @Override
            public final boolean accept(Object obj) {
                return this.f$0.gestureScale((String[]) obj);
            }
        });
        this.mCommands.add(new EventHandler() {
            @Override
            public final boolean accept(Object obj) {
                return this.f$0.jobProgressDialog((String[]) obj);
            }
        });
        this.mCommands.add(new EventHandler() {
            @Override
            public final boolean accept(Object obj) {
                return this.f$0.archiveCreation((String[]) obj);
            }
        });
        this.mCommands.add(new EventHandler() {
            @Override
            public final boolean accept(Object obj) {
                return this.f$0.docInspector((String[]) obj);
            }
        });
        this.mCommands.add(new EventHandler() {
            @Override
            public final boolean accept(Object obj) {
                return this.f$0.docDetails((String[]) obj);
            }
        });
        this.mCommands.add(new EventHandler() {
            @Override
            public final boolean accept(Object obj) {
                return this.f$0.forcePaging((String[]) obj);
            }
        });
    }

    public void add(EventHandler<String[]> eventHandler) {
        this.mCommands.add(eventHandler);
    }

    @Override
    public boolean accept(String str) {
        if (!this.mFeatures.isDebugSupportEnabled()) {
            return false;
        }
        if (!this.mFeatures.isCommandInterceptorEnabled()) {
            if (SharedMinimal.DEBUG) {
                Log.v("CommandInterceptor", "Skipping input, command interceptor disabled.");
            }
            return false;
        }
        if (str.length() > COMMAND_PREFIX.length() && str.startsWith(COMMAND_PREFIX)) {
            String[] strArrSplit = str.substring(COMMAND_PREFIX.length()).split("\\s+");
            Iterator<EventHandler<String[]>> it = this.mCommands.iterator();
            while (it.hasNext()) {
                if (it.next().accept(strArrSplit)) {
                    return true;
                }
            }
            Log.d("CommandInterceptor", "Unrecognized debug command: " + str);
        }
        return false;
    }

    private boolean quickViewer(String[] strArr) {
        if ("qv".equals(strArr[0])) {
            if (strArr.length == 2 && !TextUtils.isEmpty(strArr[1])) {
                DebugFlags.setQuickViewer(strArr[1]);
                Log.i("CommandInterceptor", "Set quick viewer to: " + strArr[1]);
                return true;
            }
            Log.w("CommandInterceptor", "Invalid command structure: " + TextUtils.join(" ", strArr));
        } else if ("deqv".equals(strArr[0])) {
            Log.i("CommandInterceptor", "Unset quick viewer");
            DebugFlags.setQuickViewer(null);
            return true;
        }
        return false;
    }

    private boolean gestureScale(String[] strArr) {
        if ("gs".equals(strArr[0])) {
            if (strArr.length == 2 && !TextUtils.isEmpty(strArr[1])) {
                boolean zAsBool = asBool(strArr[1]);
                this.mFeatures.forceFeature(R.bool.feature_gesture_scale, zAsBool);
                Log.i("CommandInterceptor", "Set gesture scale enabled to: " + zAsBool);
                return true;
            }
            Log.w("CommandInterceptor", "Invalid command structure: " + TextUtils.join(" ", strArr));
        }
        return false;
    }

    private boolean jobProgressDialog(String[] strArr) {
        if ("jpd".equals(strArr[0])) {
            if (strArr.length == 2 && !TextUtils.isEmpty(strArr[1])) {
                boolean zAsBool = asBool(strArr[1]);
                this.mFeatures.forceFeature(R.bool.feature_job_progress_dialog, zAsBool);
                Log.i("CommandInterceptor", "Set job progress dialog enabled to: " + zAsBool);
                return true;
            }
            Log.w("CommandInterceptor", "Invalid command structure: " + TextUtils.join(" ", strArr));
        }
        return false;
    }

    private boolean archiveCreation(String[] strArr) {
        if ("zip".equals(strArr[0])) {
            if (strArr.length == 2 && !TextUtils.isEmpty(strArr[1])) {
                boolean zAsBool = asBool(strArr[1]);
                this.mFeatures.forceFeature(R.bool.feature_archive_creation, zAsBool);
                Log.i("CommandInterceptor", "Set gesture scale enabled to: " + zAsBool);
                return true;
            }
            Log.w("CommandInterceptor", "Invalid command structure: " + TextUtils.join(" ", strArr));
        }
        return false;
    }

    private boolean docInspector(String[] strArr) {
        if ("inspect".equals(strArr[0])) {
            if (strArr.length == 2 && !TextUtils.isEmpty(strArr[1])) {
                boolean zAsBool = asBool(strArr[1]);
                this.mFeatures.forceFeature(R.bool.feature_inspector, zAsBool);
                Log.i("CommandInterceptor", "Set doc inspector enabled to: " + zAsBool);
                return true;
            }
            Log.w("CommandInterceptor", "Invalid command structure: " + TextUtils.join(" ", strArr));
        }
        return false;
    }

    private boolean docDetails(String[] strArr) {
        if ("docinfo".equals(strArr[0])) {
            if (strArr.length == 2 && !TextUtils.isEmpty(strArr[1])) {
                boolean zAsBool = asBool(strArr[1]);
                DebugFlags.setDocumentDetailsEnabled(zAsBool);
                Log.i("CommandInterceptor", "Set doc details enabled to: " + zAsBool);
                return true;
            }
            Log.w("CommandInterceptor", "Invalid command structure: " + TextUtils.join(" ", strArr));
        }
        return false;
    }

    private boolean forcePaging(String[] strArr) {
        if ("page".equals(strArr[0])) {
            if (strArr.length >= 2) {
                try {
                    int i = Integer.parseInt(strArr[1]);
                    int i2 = strArr.length == 3 ? Integer.parseInt(strArr[2]) : -1;
                    DebugFlags.setForcedPaging(i, i2);
                    Log.i("CommandInterceptor", "Set forced paging to offset: " + i + ", limit: " + i2);
                    return true;
                } catch (NumberFormatException e) {
                    Log.w("CommandInterceptor", "Command input does not contain valid numbers: " + TextUtils.join(" ", strArr));
                    return false;
                }
            }
            Log.w("CommandInterceptor", "Invalid command structure: " + TextUtils.join(" ", strArr));
        } else if ("deqv".equals(strArr[0])) {
            Log.i("CommandInterceptor", "Unset quick viewer");
            DebugFlags.setQuickViewer(null);
            return true;
        }
        return false;
    }

    private final boolean asBool(String str) {
        if (str == null || str.equals("0")) {
            return false;
        }
        if (str.equals("1")) {
            return true;
        }
        return Boolean.valueOf(str).booleanValue();
    }

    public static final class DumpRootsCacheHandler implements EventHandler<String[]> {
        private final Context mContext;

        public DumpRootsCacheHandler(Context context) {
            this.mContext = context;
        }

        @Override
        public boolean accept(String[] strArr) {
            if (!"dumpCache".equals(strArr[0])) {
                return false;
            }
            DocumentsApplication.getProvidersCache(this.mContext).logCache();
            return true;
        }
    }

    public static final EventHandler<String> createDebugModeFlipper(final Features features, final Runnable runnable, final CommandInterceptor commandInterceptor) {
        if (!features.isDebugSupportEnabled()) {
            return commandInterceptor;
        }
        final String str = ":wwssadadba";
        final String str2 = "up up down down left right left right b a";
        return new EventHandler<String>() {
            static final boolean $assertionsDisabled = false;

            @Override
            public boolean accept(String str3) {
                if (str.equals(str3) || str2.equals(str3)) {
                    runnable.run();
                }
                return commandInterceptor.accept(str3);
            }
        };
    }
}
