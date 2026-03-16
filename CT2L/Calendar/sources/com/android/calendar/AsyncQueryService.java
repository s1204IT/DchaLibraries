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

        protected static char opToChar(int op) {
            switch (op) {
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

    public final int cancelOperation(int token) {
        return AsyncQueryServiceHelper.cancelOperation(token);
    }

    public void startQuery(int token, Object cookie, Uri uri, String[] projection, String selection, String[] selectionArgs, String orderBy) {
        AsyncQueryServiceHelper.OperationInfo info = new AsyncQueryServiceHelper.OperationInfo();
        info.op = 1;
        info.resolver = this.mContext.getContentResolver();
        info.handler = this.mHandler;
        info.token = token;
        info.cookie = cookie;
        info.uri = uri;
        info.projection = projection;
        info.selection = selection;
        info.selectionArgs = selectionArgs;
        info.orderBy = orderBy;
        AsyncQueryServiceHelper.queueOperation(this.mContext, info);
    }

    public void startInsert(int token, Object cookie, Uri uri, ContentValues initialValues, long delayMillis) {
        AsyncQueryServiceHelper.OperationInfo info = new AsyncQueryServiceHelper.OperationInfo();
        info.op = 2;
        info.resolver = this.mContext.getContentResolver();
        info.handler = this.mHandler;
        info.token = token;
        info.cookie = cookie;
        info.uri = uri;
        info.values = initialValues;
        info.delayMillis = delayMillis;
        AsyncQueryServiceHelper.queueOperation(this.mContext, info);
    }

    public void startUpdate(int token, Object cookie, Uri uri, ContentValues values, String selection, String[] selectionArgs, long delayMillis) {
        AsyncQueryServiceHelper.OperationInfo info = new AsyncQueryServiceHelper.OperationInfo();
        info.op = 3;
        info.resolver = this.mContext.getContentResolver();
        info.handler = this.mHandler;
        info.token = token;
        info.cookie = cookie;
        info.uri = uri;
        info.values = values;
        info.selection = selection;
        info.selectionArgs = selectionArgs;
        info.delayMillis = delayMillis;
        AsyncQueryServiceHelper.queueOperation(this.mContext, info);
    }

    public void startDelete(int token, Object cookie, Uri uri, String selection, String[] selectionArgs, long delayMillis) {
        AsyncQueryServiceHelper.OperationInfo info = new AsyncQueryServiceHelper.OperationInfo();
        info.op = 4;
        info.resolver = this.mContext.getContentResolver();
        info.handler = this.mHandler;
        info.token = token;
        info.cookie = cookie;
        info.uri = uri;
        info.selection = selection;
        info.selectionArgs = selectionArgs;
        info.delayMillis = delayMillis;
        AsyncQueryServiceHelper.queueOperation(this.mContext, info);
    }

    public void startBatch(int token, Object cookie, String authority, ArrayList<ContentProviderOperation> cpo, long delayMillis) {
        AsyncQueryServiceHelper.OperationInfo info = new AsyncQueryServiceHelper.OperationInfo();
        info.op = 5;
        info.resolver = this.mContext.getContentResolver();
        info.handler = this.mHandler;
        info.token = token;
        info.cookie = cookie;
        info.authority = authority;
        info.cpo = cpo;
        info.delayMillis = delayMillis;
        AsyncQueryServiceHelper.queueOperation(this.mContext, info);
    }

    protected void onQueryComplete(int token, Object cookie, Cursor cursor) {
    }

    protected void onInsertComplete(int token, Object cookie, Uri uri) {
    }

    protected void onUpdateComplete(int token, Object cookie, int result) {
    }

    protected void onDeleteComplete(int token, Object cookie, int result) {
    }

    protected void onBatchComplete(int token, Object cookie, ContentProviderResult[] results) {
    }

    @Override
    public void handleMessage(Message msg) {
        AsyncQueryServiceHelper.OperationInfo info = (AsyncQueryServiceHelper.OperationInfo) msg.obj;
        int token = msg.what;
        int op = msg.arg1;
        switch (op) {
            case 1:
                onQueryComplete(token, info.cookie, (Cursor) info.result);
                break;
            case 2:
                onInsertComplete(token, info.cookie, (Uri) info.result);
                break;
            case 3:
                onUpdateComplete(token, info.cookie, ((Integer) info.result).intValue());
                break;
            case 4:
                onDeleteComplete(token, info.cookie, ((Integer) info.result).intValue());
                break;
            case 5:
                onBatchComplete(token, info.cookie, (ContentProviderResult[]) info.result);
                break;
        }
    }

    protected void setTestHandler(Handler handler) {
        this.mHandler = handler;
    }
}
