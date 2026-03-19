package com.android.calendar;

import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import com.android.calendar.AsyncQueryServiceHelper;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;

public class AsyncQueryService extends Handler {
    private static AtomicInteger mUniqueToken = new AtomicInteger(0);
    private Context mContext;
    private Handler mHandler = this;

    public static class Operation {
        public int op;
        public long scheduledExecutionTime;
        public int token;

        protected static char opToChar(int i) {
            switch (i) {
                case 1:
                    return 'Q';
                case 2:
                    return 'I';
                case 3:
                    return 'U';
                case 4:
                    return 'D';
                case 5:
                    return 'B';
                default:
                    return '?';
            }
        }

        public String toString() {
            return "Operation [op=" + this.op + ", token=" + this.token + ", scheduledExecutionTime=" + this.scheduledExecutionTime + "]";
        }
    }

    public AsyncQueryService(Context context) {
        this.mContext = context;
    }

    public final int getNextToken() {
        return mUniqueToken.getAndIncrement();
    }

    public final Operation getLastCancelableOperation() {
        return AsyncQueryServiceHelper.getLastCancelableOperation();
    }

    public final int cancelOperation(int i) {
        return AsyncQueryServiceHelper.cancelOperation(i);
    }

    public void startQuery(int i, Object obj, Uri uri, String[] strArr, String str, String[] strArr2, String str2) {
        AsyncQueryServiceHelper.OperationInfo operationInfo = new AsyncQueryServiceHelper.OperationInfo();
        operationInfo.op = 1;
        operationInfo.resolver = this.mContext.getContentResolver();
        operationInfo.handler = this.mHandler;
        operationInfo.token = i;
        operationInfo.cookie = obj;
        operationInfo.uri = uri;
        operationInfo.projection = strArr;
        operationInfo.selection = str;
        operationInfo.selectionArgs = strArr2;
        operationInfo.orderBy = str2;
        AsyncQueryServiceHelper.queueOperation(this.mContext, operationInfo);
    }

    public void startInsert(int i, Object obj, Uri uri, ContentValues contentValues, long j) {
        AsyncQueryServiceHelper.OperationInfo operationInfo = new AsyncQueryServiceHelper.OperationInfo();
        operationInfo.op = 2;
        operationInfo.resolver = this.mContext.getContentResolver();
        operationInfo.handler = this.mHandler;
        operationInfo.token = i;
        operationInfo.cookie = obj;
        operationInfo.uri = uri;
        operationInfo.values = contentValues;
        operationInfo.delayMillis = j;
        AsyncQueryServiceHelper.queueOperation(this.mContext, operationInfo);
    }

    public void startUpdate(int i, Object obj, Uri uri, ContentValues contentValues, String str, String[] strArr, long j) {
        AsyncQueryServiceHelper.OperationInfo operationInfo = new AsyncQueryServiceHelper.OperationInfo();
        operationInfo.op = 3;
        operationInfo.resolver = this.mContext.getContentResolver();
        operationInfo.handler = this.mHandler;
        operationInfo.token = i;
        operationInfo.cookie = obj;
        operationInfo.uri = uri;
        operationInfo.values = contentValues;
        operationInfo.selection = str;
        operationInfo.selectionArgs = strArr;
        operationInfo.delayMillis = j;
        AsyncQueryServiceHelper.queueOperation(this.mContext, operationInfo);
    }

    public void startDelete(int i, Object obj, Uri uri, String str, String[] strArr, long j) {
        AsyncQueryServiceHelper.OperationInfo operationInfo = new AsyncQueryServiceHelper.OperationInfo();
        operationInfo.op = 4;
        operationInfo.resolver = this.mContext.getContentResolver();
        operationInfo.handler = this.mHandler;
        operationInfo.token = i;
        operationInfo.cookie = obj;
        operationInfo.uri = uri;
        operationInfo.selection = str;
        operationInfo.selectionArgs = strArr;
        operationInfo.delayMillis = j;
        AsyncQueryServiceHelper.queueOperation(this.mContext, operationInfo);
    }

    public void startBatch(int i, Object obj, String str, ArrayList<ContentProviderOperation> arrayList, long j) {
        AsyncQueryServiceHelper.OperationInfo operationInfo = new AsyncQueryServiceHelper.OperationInfo();
        operationInfo.op = 5;
        operationInfo.resolver = this.mContext.getContentResolver();
        operationInfo.handler = this.mHandler;
        operationInfo.token = i;
        operationInfo.cookie = obj;
        operationInfo.authority = str;
        operationInfo.cpo = arrayList;
        operationInfo.delayMillis = j;
        AsyncQueryServiceHelper.queueOperation(this.mContext, operationInfo);
    }

    protected void onQueryComplete(int i, Object obj, Cursor cursor) {
    }

    protected void onInsertComplete(int i, Object obj, Uri uri) {
    }

    protected void onUpdateComplete(int i, Object obj, int i2) {
    }

    protected void onDeleteComplete(int i, Object obj, int i2) {
    }

    protected void onBatchComplete(int i, Object obj, ContentProviderResult[] contentProviderResultArr) {
    }

    @Override
    public void handleMessage(Message message) {
        AsyncQueryServiceHelper.OperationInfo operationInfo = (AsyncQueryServiceHelper.OperationInfo) message.obj;
        int i = message.what;
        switch (message.arg1) {
            case 1:
                onQueryComplete(i, operationInfo.cookie, (Cursor) operationInfo.result);
                break;
            case 2:
                onInsertComplete(i, operationInfo.cookie, (Uri) operationInfo.result);
                break;
            case 3:
                onUpdateComplete(i, operationInfo.cookie, ((Integer) operationInfo.result).intValue());
                break;
            case 4:
                onDeleteComplete(i, operationInfo.cookie, ((Integer) operationInfo.result).intValue());
                break;
            case 5:
                onBatchComplete(i, operationInfo.cookie, (ContentProviderResult[]) operationInfo.result);
                break;
        }
    }

    protected void setTestHandler(Handler handler) {
        this.mHandler = handler;
    }
}
