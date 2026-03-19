package com.mediatek.contacts.list.service;

import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import com.android.contacts.model.account.AccountWithDataSet;
import com.android.contacts.vcard.ProcessorBase;
import com.mediatek.contacts.ContactsApplicationEx;
import com.mediatek.contacts.util.Log;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;

public class MultiChoiceService extends Service {
    private static final Map<Integer, ProcessorBase> RUNNINGJOBMAP = new HashMap();
    private static int sCurrentJobId;
    private MyBinder mBinder;
    private final ExecutorService mExecutorService = ContactsApplicationEx.getContactsApplication().getApplicationTaskService();

    public class MyBinder extends Binder {
        public MyBinder() {
        }

        public MultiChoiceService getService() {
            return MultiChoiceService.this;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        this.mBinder = new MyBinder();
        Log.d("MultiChoiceService", "[onCreate]Multi-choice Service is being created.");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public int onStartCommand(Intent intent, int i, int i2) {
        return 2;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return this.mBinder;
    }

    private synchronized boolean tryExecute(ProcessorBase processorBase) {
        try {
            Log.d("MultiChoiceService", "[tryExecute]Executor service status: shutdown: " + this.mExecutorService.isShutdown() + ", terminated: " + this.mExecutorService.isTerminated());
            this.mExecutorService.execute(processorBase);
            RUNNINGJOBMAP.put(Integer.valueOf(sCurrentJobId), processorBase);
        } catch (RejectedExecutionException e) {
            Log.w("MultiChoiceService", "[tryExecute]Failed to excetute a job:" + e);
            return false;
        }
        return true;
    }

    public synchronized void handleDeleteRequest(List<MultiChoiceRequest> list, MultiChoiceHandlerListener multiChoiceHandlerListener) {
        sCurrentJobId++;
        Log.i("MultiChoiceService", "[handleDeleteRequest]sCurrentJobId:" + sCurrentJobId);
        if (tryExecute(new DeleteProcessor(this, multiChoiceHandlerListener, list, sCurrentJobId)) && multiChoiceHandlerListener != null) {
            multiChoiceHandlerListener.onProcessed(2, sCurrentJobId, 0, -1, list.get(0).mContactName);
        }
    }

    public synchronized void handleCopyRequest(List<MultiChoiceRequest> list, MultiChoiceHandlerListener multiChoiceHandlerListener, AccountWithDataSet accountWithDataSet, AccountWithDataSet accountWithDataSet2) {
        sCurrentJobId++;
        Log.i("MultiChoiceService", "[handleCopyRequest]sCurrentJobId:" + sCurrentJobId);
        if (tryExecute(new CopyProcessor(this, multiChoiceHandlerListener, list, sCurrentJobId, accountWithDataSet, accountWithDataSet2)) && multiChoiceHandlerListener != null) {
            multiChoiceHandlerListener.onProcessed(1, sCurrentJobId, 0, -1, list.get(0).mContactName);
        }
    }

    public synchronized void handleCancelRequest(MultiChoiceCancelRequest multiChoiceCancelRequest) {
        int i = multiChoiceCancelRequest.jobId;
        Log.i("MultiChoiceService", "[handleCancelRequest]jobId:" + i);
        ProcessorBase processorBaseRemove = RUNNINGJOBMAP.remove(Integer.valueOf(i));
        if (processorBaseRemove != null) {
            processorBaseRemove.cancel(true);
        } else {
            Log.w("MultiChoiceService", "[handleCancelRequest]" + String.format("Tried to remove unknown job (id: %d)", Integer.valueOf(i)));
            ((NotificationManager) getSystemService("notification")).cancel("MultiChoiceServiceProgress", i);
        }
        stopServiceIfAppropriate();
    }

    private synchronized void stopServiceIfAppropriate() {
        if (RUNNINGJOBMAP.size() > 0) {
            for (Map.Entry<Integer, ProcessorBase> entry : RUNNINGJOBMAP.entrySet()) {
                int iIntValue = entry.getKey().intValue();
                if (entry.getValue().isDone()) {
                    RUNNINGJOBMAP.remove(Integer.valueOf(iIntValue));
                } else {
                    Log.i("MultiChoiceService", "[stopServiceIfAppropriate]" + String.format("Found unfinished job (id: %d)", Integer.valueOf(iIntValue)));
                    return;
                }
            }
        }
        Log.i("MultiChoiceService", "[stopServiceIfAppropriate]No unfinished job. Stop this service.");
        stopSelf();
    }

    public synchronized void handleFinishNotification(int i, boolean z) {
        Log.i("MultiChoiceService", "[handleFinishNotification]jobId = " + i + ",successful = " + z);
        if (RUNNINGJOBMAP.remove(Integer.valueOf(i)) == null) {
            Log.w("MultiChoiceService", "[handleFinishNotification]" + String.format("Tried to remove unknown job (id: %d)", Integer.valueOf(i)));
        }
        stopServiceIfAppropriate();
    }

    public static synchronized boolean isProcessing(int i) {
        if (RUNNINGJOBMAP.size() <= 0) {
            Log.w("MultiChoiceService", "[isProcessing] size is <=0,return false!");
            return false;
        }
        if (RUNNINGJOBMAP.size() > 0) {
            Iterator<Map.Entry<Integer, ProcessorBase>> it = RUNNINGJOBMAP.entrySet().iterator();
            while (it.hasNext()) {
                if (it.next().getValue().getType() == i) {
                    Log.i("MultiChoiceService", "[isProcessing]return true,requestType = " + i);
                    return true;
                }
            }
        }
        return false;
    }
}
