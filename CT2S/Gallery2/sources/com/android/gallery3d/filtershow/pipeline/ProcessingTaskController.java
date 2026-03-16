package com.android.gallery3d.filtershow.pipeline;

import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;
import com.android.gallery3d.filtershow.pipeline.ProcessingTask;
import java.util.HashMap;

public class ProcessingTaskController implements Handler.Callback {
    private Context mContext;
    private int mCurrentType;
    private HandlerThread mHandlerThread;
    private Handler mProcessingHandler;
    private HashMap<Integer, ProcessingTask> mTasks = new HashMap<>();
    private final Handler mResultHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            ProcessingTask task = (ProcessingTask) ProcessingTaskController.this.mTasks.get(Integer.valueOf(msg.what));
            if (task != null) {
                if (msg.arg1 == 1) {
                    task.onResult((ProcessingTask.Result) msg.obj);
                } else if (msg.arg1 == 2) {
                    task.onUpdate((ProcessingTask.Update) msg.obj);
                } else {
                    Log.w("ProcessingTaskController", "received unknown message! " + msg.arg1);
                }
            }
        }
    };

    @Override
    public boolean handleMessage(Message msg) {
        ProcessingTask task = this.mTasks.get(Integer.valueOf(msg.what));
        if (task == null) {
            return false;
        }
        task.processRequest((ProcessingTask.Request) msg.obj);
        return true;
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

    public void add(ProcessingTask task) {
        task.added(this);
        this.mTasks.put(Integer.valueOf(task.getType()), task);
    }

    public void quit() {
        this.mHandlerThread.quit();
    }
}
