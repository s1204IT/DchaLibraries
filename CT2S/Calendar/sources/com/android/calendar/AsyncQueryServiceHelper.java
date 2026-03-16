package com.android.calendar;

import android.app.IntentService;
import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
import android.os.SystemClock;
import android.util.Log;
import com.android.calendar.AsyncQueryService;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.PriorityQueue;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

public class AsyncQueryServiceHelper extends IntentService {
    private static final PriorityQueue<OperationInfo> sWorkQueue = new PriorityQueue<>();
    protected Class<AsyncQueryService> mService;

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
        public long getDelay(TimeUnit unit) {
            return unit.convert(this.mScheduledTimeMillis - SystemClock.elapsedRealtime(), TimeUnit.MILLISECONDS);
        }

        @Override
        public int compareTo(Delayed another) {
            OperationInfo anotherArgs = (OperationInfo) another;
            if (this.mScheduledTimeMillis == anotherArgs.mScheduledTimeMillis) {
                return 0;
            }
            if (this.mScheduledTimeMillis < anotherArgs.mScheduledTimeMillis) {
                return -1;
            }
            return 1;
        }

        public String toString() {
            return "OperationInfo [\n\t token= " + this.token + ",\n\t op= " + AsyncQueryService.Operation.opToChar(this.op) + ",\n\t uri= " + this.uri + ",\n\t authority= " + this.authority + ",\n\t delayMillis= " + this.delayMillis + ",\n\t mScheduledTimeMillis= " + this.mScheduledTimeMillis + ",\n\t resolver= " + this.resolver + ",\n\t handler= " + this.handler + ",\n\t projection= " + Arrays.toString(this.projection) + ",\n\t selection= " + this.selection + ",\n\t selectionArgs= " + Arrays.toString(this.selectionArgs) + ",\n\t orderBy= " + this.orderBy + ",\n\t result= " + this.result + ",\n\t cookie= " + this.cookie + ",\n\t values= " + this.values + ",\n\t cpo= " + this.cpo + "\n]";
        }

        public boolean equivalent(AsyncQueryService.Operation o) {
            return o.token == this.token && o.op == this.op;
        }
    }

    public static void queueOperation(Context context, OperationInfo args) {
        args.calculateScheduledTime();
        synchronized (sWorkQueue) {
            sWorkQueue.add(args);
            sWorkQueue.notify();
        }
        context.startService(new Intent(context, (Class<?>) AsyncQueryServiceHelper.class));
    }

    public static AsyncQueryService.Operation getLastCancelableOperation() throws Throwable {
        long lastScheduleTime = Long.MIN_VALUE;
        AsyncQueryService.Operation op = null;
        synchronized (sWorkQueue) {
            try {
                Iterator<OperationInfo> it = sWorkQueue.iterator();
                while (true) {
                    try {
                        AsyncQueryService.Operation op2 = op;
                        if (it.hasNext()) {
                            OperationInfo info = it.next();
                            if (info.delayMillis <= 0 || lastScheduleTime >= info.mScheduledTimeMillis) {
                                op = op2;
                            } else {
                                op = op2 == null ? new AsyncQueryService.Operation() : op2;
                                op.token = info.token;
                                op.op = info.op;
                                op.scheduledExecutionTime = info.mScheduledTimeMillis;
                                lastScheduleTime = info.mScheduledTimeMillis;
                            }
                        } else {
                            return op2;
                        }
                    } catch (Throwable th) {
                        th = th;
                        throw th;
                    }
                }
            } catch (Throwable th2) {
                th = th2;
            }
        }
    }

    public static int cancelOperation(int token) {
        int canceled = 0;
        synchronized (sWorkQueue) {
            Iterator<OperationInfo> it = sWorkQueue.iterator();
            while (it.hasNext()) {
                if (it.next().token == token) {
                    it.remove();
                    canceled++;
                }
            }
        }
        return canceled;
    }

    public AsyncQueryServiceHelper(String name) {
        super(name);
        this.mService = AsyncQueryService.class;
    }

    public AsyncQueryServiceHelper() {
        super("AsyncQueryServiceHelper");
        this.mService = AsyncQueryService.class;
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Cursor cursor;
        synchronized (sWorkQueue) {
            while (sWorkQueue.size() != 0) {
                if (sWorkQueue.size() == 1) {
                    OperationInfo first = sWorkQueue.peek();
                    long waitTime = first.mScheduledTimeMillis - SystemClock.elapsedRealtime();
                    if (waitTime > 0) {
                        try {
                            sWorkQueue.wait(waitTime);
                        } catch (InterruptedException e) {
                        }
                    }
                }
                OperationInfo args = sWorkQueue.poll();
                if (args != null) {
                    ContentResolver resolver = args.resolver;
                    if (resolver != null) {
                        switch (args.op) {
                            case 1:
                                try {
                                    cursor = resolver.query(args.uri, args.projection, args.selection, args.selectionArgs, args.orderBy);
                                    if (cursor != null) {
                                        cursor.getCount();
                                    }
                                } catch (Exception e2) {
                                    Log.w("AsyncQuery", e2.toString());
                                    cursor = null;
                                }
                                args.result = cursor;
                                break;
                            case 2:
                                args.result = resolver.insert(args.uri, args.values);
                                break;
                            case 3:
                                args.result = Integer.valueOf(resolver.update(args.uri, args.values, args.selection, args.selectionArgs));
                                break;
                            case 4:
                                try {
                                    args.result = Integer.valueOf(resolver.delete(args.uri, args.selection, args.selectionArgs));
                                } catch (IllegalArgumentException e3) {
                                    Log.w("AsyncQuery", "Delete failed.");
                                    Log.w("AsyncQuery", e3.toString());
                                    args.result = 0;
                                }
                                break;
                            case 5:
                                try {
                                    args.result = resolver.applyBatch(args.authority, args.cpo);
                                } catch (OperationApplicationException e4) {
                                    Log.e("AsyncQuery", e4.toString());
                                    args.result = null;
                                } catch (RemoteException e5) {
                                    Log.e("AsyncQuery", e5.toString());
                                    args.result = null;
                                }
                                break;
                        }
                        Message reply = args.handler.obtainMessage(args.token);
                        reply.obj = args;
                        reply.arg1 = args.op;
                        reply.sendToTarget();
                        return;
                    }
                    return;
                }
            }
        }
    }

    @Override
    public void onStart(Intent intent, int startId) {
        super.onStart(intent, startId);
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
