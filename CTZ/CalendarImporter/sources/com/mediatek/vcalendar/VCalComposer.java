package com.mediatek.vcalendar;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.net.Uri;
import android.os.MemoryFile;
import android.os.ParcelFileDescriptor;
import android.provider.CalendarContract;
import com.mediatek.vcalendar.component.Component;
import com.mediatek.vcalendar.component.ComponentFactory;
import com.mediatek.vcalendar.component.VCalendar;
import com.mediatek.vcalendar.database.DatabaseHelper;
import com.mediatek.vcalendar.utils.LogUtil;
import java.io.IOException;

public class VCalComposer {
    static final String TAG = "VCalComposer";
    private boolean mCancelRequest;
    private int mComposedCount;
    private final Context mContext;
    private DatabaseHelper mDatabaseHelper;
    private final VCalStatusChangeOperator mListener;
    private String mMemoryFileName;
    private String mSelection;
    private int mTotalCnt;

    public VCalComposer(Context context, String str, VCalStatusChangeOperator vCalStatusChangeOperator) {
        this.mCancelRequest = false;
        this.mTotalCnt = -1;
        this.mMemoryFileName = "vCalendar";
        this.mContext = context;
        this.mListener = vCalStatusChangeOperator;
        this.mSelection = str;
    }

    public void cancelCurrentCompose() {
        LogUtil.d(TAG, "cancelCurrentParse()");
        this.mCancelRequest = true;
        if (this.mListener != null) {
            this.mListener.vCalOperationCanceled(this.mComposedCount, this.mTotalCnt);
        }
    }

    public Component composeComponent(SingleComponentCursorInfo singleComponentCursorInfo) throws VCalendarException {
        if (singleComponentCursorInfo.componentType == null) {
            LogUtil.e(TAG, "composeComponent(): results of the query URI are not supported components");
            return null;
        }
        Component componentCreateComponent = ComponentFactory.createComponent(singleComponentCursorInfo.componentType, null);
        componentCreateComponent.compose(singleComponentCursorInfo);
        return componentCreateComponent;
    }

    public String getMemoryFileName() {
        return this.mMemoryFileName;
    }

    public AssetFileDescriptor getAccountsMemoryFile() {
        AssetFileDescriptor assetFileDescriptor;
        LogUtil.i(TAG, "getAccountsMemoryFile(): start to create the AccountsMemory File ");
        StringBuilder sb = new StringBuilder();
        AssetFileDescriptor assetFileDescriptor2 = null;
        if (this.mCancelRequest) {
            return null;
        }
        Uri uri = CalendarContract.Events.CONTENT_URI;
        String str = this.mSelection;
        this.mDatabaseHelper = new DatabaseHelper(this.mContext);
        if (!this.mDatabaseHelper.query(uri, null, str, null, "calendar_id")) {
            LogUtil.e(TAG, "getAccountsMemoryFile(): query from database failed");
            return null;
        }
        this.mComposedCount = 0;
        this.mTotalCnt = this.mDatabaseHelper.getComponentCount();
        if (this.mTotalCnt <= 0) {
            LogUtil.e(TAG, "getAccountsMemoryFile(): No components matched the selection : " + this.mSelection);
            return null;
        }
        long j = -1;
        while (!this.mCancelRequest && this.mDatabaseHelper.hasNextComponentInfo()) {
            SingleComponentCursorInfo nextComponentInfo = this.mDatabaseHelper.getNextComponentInfo();
            if (nextComponentInfo == null) {
                LogUtil.e(TAG, "getAccountsMemoryFile(): can NOT get neccessary information of the component");
            } else {
                long j2 = nextComponentInfo.calendarId;
                LogUtil.d(TAG, "getAccountsMemoryFile(): currentCalendarId=" + j + "; tempId=" + j2);
                if (j2 != j) {
                    if (j != -1) {
                        sb.append(VCalendar.getVCalendarTail());
                    }
                    sb.append(VCalendar.getVCalendarHead());
                    j = j2;
                }
                try {
                    Component componentComposeComponent = composeComponent(nextComponentInfo);
                    if (componentComposeComponent != null) {
                        sb.append(componentComposeComponent.toString());
                        if (this.mTotalCnt == 1) {
                            this.mMemoryFileName = componentComposeComponent.getTitle();
                        }
                        this.mComposedCount++;
                    }
                } catch (VCalendarException e) {
                    LogUtil.e(TAG, "getAccountsMemoryFile(): compose component failed", e);
                }
            }
        }
        sb.append(VCalendar.getVCalendarTail());
        byte[] bytes = sb.toString().getBytes();
        try {
            try {
                MemoryFile memoryFile = new MemoryFile("calenderAssetFile", bytes.length);
                memoryFile.writeBytes(bytes, 0, 0, bytes.length);
                assetFileDescriptor = new AssetFileDescriptor(ParcelFileDescriptor.dup(memoryFile.getFileDescriptor()), 0L, memoryFile.length());
            } catch (Throwable th) {
                return null;
            }
        } catch (IOException e2) {
            e = e2;
        } catch (IllegalArgumentException e3) {
            e = e3;
        } catch (SecurityException e4) {
            e = e4;
        }
        try {
            LogUtil.d(TAG, "getAccountsMemoryFile(): Memory file length: " + assetFileDescriptor.getLength());
            return assetFileDescriptor;
        } catch (IOException e5) {
            e = e5;
            assetFileDescriptor2 = assetFileDescriptor;
            e.printStackTrace();
            return assetFileDescriptor2;
        } catch (IllegalArgumentException e6) {
            e = e6;
            assetFileDescriptor2 = assetFileDescriptor;
            e.printStackTrace();
            return assetFileDescriptor2;
        } catch (SecurityException e7) {
            e = e7;
            assetFileDescriptor2 = assetFileDescriptor;
            e.printStackTrace();
            return assetFileDescriptor2;
        } catch (Throwable th2) {
            return assetFileDescriptor;
        }
    }

    public VCalComposer(Context context) {
        this(context, null, null);
    }

    private boolean initForSingleComponentCompose(long j) {
        if (j < 0) {
            LogUtil.e(TAG, "initForSingleComponentCompose(): the id NOT set");
            return false;
        }
        this.mSelection = "_id=" + String.valueOf(j) + " AND deleted!=1";
        Uri uri = CalendarContract.Events.CONTENT_URI;
        if (this.mDatabaseHelper == null) {
            this.mDatabaseHelper = new DatabaseHelper(this.mContext);
        }
        if (!this.mDatabaseHelper.query(uri, null, this.mSelection, null, "calendar_id")) {
            LogUtil.e(TAG, "initForSingleComponentCompose(): query from database failed");
            return false;
        }
        return true;
    }

    public String buildVEventString(long j) {
        LogUtil.i(TAG, "buildVEventString()");
        if (!initForSingleComponentCompose(j)) {
            LogUtil.e(TAG, "buildVEventString(): initialize failed");
            return null;
        }
        SingleComponentCursorInfo nextComponentInfo = this.mDatabaseHelper.getNextComponentInfo();
        if (nextComponentInfo == null) {
            LogUtil.e(TAG, "buildVEventString(): can NOT get neccessary information of the component");
            return null;
        }
        try {
            Component componentComposeComponent = composeComponent(nextComponentInfo);
            if (componentComposeComponent != null) {
                return componentComposeComponent.toString();
            }
        } catch (VCalendarException e) {
            LogUtil.e(TAG, "buildVEventString(): compose a component failed");
            e.printStackTrace();
        }
        return null;
    }

    public String getVCalHead() {
        return VCalendar.getVCalendarHead();
    }

    public String getVCalEnd() {
        return VCalendar.getVCalendarTail();
    }
}
