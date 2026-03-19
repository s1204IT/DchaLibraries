package com.android.calendar;

import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.database.SQLException;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
import android.os.SystemClock;
import android.support.v4.app.JobIntentService;
import android.util.Log;
import com.android.calendar.AsyncQueryService;
import com.mediatek.calendar.LogUtil;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.PriorityQueue;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

public class AsyncQueryServiceHelper extends JobIntentService {
    private static final PriorityQueue<OperationInfo> sWorkQueue = new PriorityQueue<>();
    protected Class<AsyncQueryService> mService = AsyncQueryService.class;

    protected static class OperationInfo implements Delayed {
        public String authority;
        public Object cookie;
        public ArrayList<ContentProviderOperation> cpo;
        public long delayMillis;
        public Handler handler;
        private long mScheduledTimeMillis = 0;
        public int op;
        public String orderBy;
        public String[] projection;
        public ContentResolver resolver;
        public Object result;
        public String selection;
        public String[] selectionArgs;
        public int token;
        public Uri uri;
        public ContentValues values;

        protected OperationInfo() {
        }

        void calculateScheduledTime() {
            this.mScheduledTimeMillis = SystemClock.elapsedRealtime() + this.delayMillis;
        }

        @Override
        public long getDelay(TimeUnit timeUnit) {
            return timeUnit.convert(this.mScheduledTimeMillis - SystemClock.elapsedRealtime(), TimeUnit.MILLISECONDS);
        }

        @Override
        public int compareTo(Delayed delayed) {
            OperationInfo operationInfo = (OperationInfo) delayed;
            if (this.mScheduledTimeMillis == operationInfo.mScheduledTimeMillis) {
                return 0;
            }
            if (this.mScheduledTimeMillis < operationInfo.mScheduledTimeMillis) {
                return -1;
            }
            return 1;
        }

        public String toString() {
            return "OperationInfo [\n\t token= " + this.token + ",\n\t op= " + AsyncQueryService.Operation.opToChar(this.op) + ",\n\t uri= " + this.uri + ",\n\t authority= " + this.authority + ",\n\t delayMillis= " + this.delayMillis + ",\n\t mScheduledTimeMillis= " + this.mScheduledTimeMillis + ",\n\t resolver= " + this.resolver + ",\n\t handler= " + this.handler + ",\n\t projection= " + Arrays.toString(this.projection) + ",\n\t selection= " + this.selection + ",\n\t selectionArgs= " + Arrays.toString(this.selectionArgs) + ",\n\t orderBy= " + this.orderBy + ",\n\t result= " + this.result + ",\n\t cookie= " + this.cookie + ",\n\t values= " + this.values + ",\n\t cpo= " + this.cpo + "\n]";
        }

        public boolean equivalent(AsyncQueryService.Operation operation) {
            return operation.token == this.token && operation.op == this.op;
        }
    }

    public static void queueOperation(Context context, OperationInfo operationInfo) {
        operationInfo.calculateScheduledTime();
        synchronized (sWorkQueue) {
            sWorkQueue.add(operationInfo);
            sWorkQueue.notify();
        }
        JobIntentService.enqueueWork(context, AsyncQueryServiceHelper.class, 256, new Intent(context, (Class<?>) AsyncQueryServiceHelper.class));
    }

    public static AsyncQueryService.Operation getLastCancelableOperation() {
        AsyncQueryService.Operation operation;
        synchronized (sWorkQueue) {
            long j = Long.MIN_VALUE;
            operation = null;
            for (OperationInfo operationInfo : sWorkQueue) {
                if (operationInfo.delayMillis > 0 && j < operationInfo.mScheduledTimeMillis) {
                    if (operation == null) {
                        operation = new AsyncQueryService.Operation();
                    }
                    operation.token = operationInfo.token;
                    operation.op = operationInfo.op;
                    operation.scheduledExecutionTime = operationInfo.mScheduledTimeMillis;
                    j = operationInfo.mScheduledTimeMillis;
                }
            }
        }
        return operation;
    }

    public static int cancelOperation(int i) {
        int i2;
        synchronized (sWorkQueue) {
            Iterator<OperationInfo> it = sWorkQueue.iterator();
            i2 = 0;
            while (it.hasNext()) {
                if (it.next().token == i) {
                    it.remove();
                    i2++;
                }
            }
        }
        return i2;
    }

    public AsyncQueryServiceHelper(String str) {
    }

    public AsyncQueryServiceHelper() {
    }

    @Override
    protected void onHandleWork(Intent intent) {
        OperationInfo operationInfoPoll;
        Cursor cursorQuery;
        synchronized (sWorkQueue) {
            do {
                if (sWorkQueue.size() == 0) {
                    return;
                }
                if (sWorkQueue.size() == 1) {
                    long jElapsedRealtime = sWorkQueue.peek().mScheduledTimeMillis - SystemClock.elapsedRealtime();
                    if (jElapsedRealtime > 0) {
                        try {
                            sWorkQueue.wait(jElapsedRealtime);
                        } catch (InterruptedException e) {
                        }
                    }
                }
                operationInfoPoll = sWorkQueue.poll();
            } while (operationInfoPoll == null);
            ContentResolver contentResolver = operationInfoPoll.resolver;
            if (contentResolver != null) {
                switch (operationInfoPoll.op) {
                    case 1:
                        try {
                            cursorQuery = contentResolver.query(operationInfoPoll.uri, operationInfoPoll.projection, operationInfoPoll.selection, operationInfoPoll.selectionArgs, operationInfoPoll.orderBy);
                            if (cursorQuery != null) {
                                LogUtil.oi("AsyncQuery", "the query count=" + cursorQuery.getCount());
                            } else {
                                LogUtil.oi("AsyncQuery", "the query result cursor is null");
                            }
                        } catch (Exception e2) {
                            Log.w("AsyncQuery", e2.toString());
                            cursorQuery = null;
                        }
                        operationInfoPoll.result = cursorQuery;
                        break;
                    case 2:
                        operationInfoPoll.result = contentResolver.insert(operationInfoPoll.uri, operationInfoPoll.values);
                        break;
                    case 3:
                        operationInfoPoll.result = Integer.valueOf(contentResolver.update(operationInfoPoll.uri, operationInfoPoll.values, operationInfoPoll.selection, operationInfoPoll.selectionArgs));
                        break;
                    case 4:
                        try {
                            operationInfoPoll.result = Integer.valueOf(contentResolver.delete(operationInfoPoll.uri, operationInfoPoll.selection, operationInfoPoll.selectionArgs));
                        } catch (IllegalArgumentException e3) {
                            Log.w("AsyncQuery", "Delete failed.");
                            Log.w("AsyncQuery", e3.toString());
                            operationInfoPoll.result = 0;
                        }
                        break;
                    case 5:
                        try {
                            operationInfoPoll.result = contentResolver.applyBatch(operationInfoPoll.authority, operationInfoPoll.cpo);
                        } catch (OperationApplicationException e4) {
                            Log.e("AsyncQuery", e4.toString());
                            operationInfoPoll.result = null;
                        } catch (SQLException e5) {
                            LogUtil.e("AsyncQuery", "Event might have been deleted from database" + e5.toString());
                            operationInfoPoll.result = null;
                        } catch (RemoteException e6) {
                            Log.e("AsyncQuery", e6.toString());
                            operationInfoPoll.result = null;
                        } catch (Exception e7) {
                            LogUtil.e("AsyncQuery", "Other exception been caught: " + e7.toString());
                            operationInfoPoll.result = null;
                        }
                        break;
                }
                Message messageObtainMessage = operationInfoPoll.handler.obtainMessage(operationInfoPoll.token);
                messageObtainMessage.obj = operationInfoPoll;
                messageObtainMessage.arg1 = operationInfoPoll.op;
                messageObtainMessage.sendToTarget();
            }
        }
    }

    @Override
    public void onStart(Intent intent, int i) {
        super.onStart(intent, i);
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }
}
