package com.android.gallery3d.filtershow.pipeline;

import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import com.android.gallery3d.filtershow.pipeline.ProcessingTask;
import com.mediatek.gallery3d.util.Log;
import java.util.HashMap;

public class ProcessingTaskController implements Handler.Callback {
    private Context mContext;
    private int mCurrentType;
    private HandlerThread mHandlerThread;
    private Handler mProcessingHandler;
    private HashMap<Integer, ProcessingTask> mTasks = new HashMap<>();
    private final Handler mResultHandler = new Handler() {
        @Override
        public void handleMessage(Message message) {
            ProcessingTask processingTask = (ProcessingTask) ProcessingTaskController.this.mTasks.get(Integer.valueOf(message.what));
            if (processingTask != null) {
                if (message.arg1 == 1) {
                    processingTask.onResult((ProcessingTask.Result) message.obj);
                    return;
                }
                if (message.arg1 == 2) {
                    processingTask.onUpdate((ProcessingTask.Update) message.obj);
                    return;
                }
                Log.w("ProcessingTaskController", "received unknown message! " + message.arg1);
            }
        }
    };

    @Override
    public boolean handleMessage(Message message) {
        ProcessingTask processingTask = this.mTasks.get(Integer.valueOf(message.what));
        if (processingTask != null) {
            processingTask.processRequest((ProcessingTask.Request) message.obj);
            return true;
        }
        return false;
    }

    public ProcessingTaskController(Context context) {
        this.mHandlerThread = null;
        this.mProcessingHandler = null;
        this.mContext = context;
        this.mHandlerThread = new HandlerThread("ProcessingTaskController", -2);
        this.mHandlerThread.start();
        this.mProcessingHandler = new Handler(this.mHandlerThread.getLooper(), this);
    }

    public Handler getProcessingHandler() {
        return this.mProcessingHandler;
    }

    public Handler getResultHandler() {
        return this.mResultHandler;
    }

    public int getReservedType() {
        int i = this.mCurrentType;
        this.mCurrentType = i + 1;
        return i;
    }

    public Context getContext() {
        return this.mContext;
    }

    public void add(ProcessingTask processingTask) {
        processingTask.added(this);
        this.mTasks.put(Integer.valueOf(processingTask.getType()), processingTask);
    }

    public void quit() {
        this.mHandlerThread.quit();
    }
}
