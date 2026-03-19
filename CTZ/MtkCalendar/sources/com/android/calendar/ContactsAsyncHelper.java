package com.android.calendar;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.provider.ContactsContract;
import android.util.Log;
import android.widget.ImageView;
import com.android.calendar.event.EditEventHelper;
import java.io.InputStream;

public class ContactsAsyncHelper extends Handler {
    private static ContactsAsyncHelper mInstance = null;
    private static Handler sThreadHandler;

    private static final class WorkerArgs {
        public Runnable callback;
        public Context context;
        public int defaultResource;
        public EditEventHelper.AttendeeItem item;
        public Object result;
        public Uri uri;
        public ImageView view;

        private WorkerArgs() {
        }
    }

    private class WorkerHandler extends Handler {
        public WorkerHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message message) {
            InputStream inputStreamOpenContactPhotoInputStream;
            WorkerArgs workerArgs = (WorkerArgs) message.obj;
            switch (message.arg1) {
                case 1:
                case 2:
                    try {
                        inputStreamOpenContactPhotoInputStream = ContactsContract.Contacts.openContactPhotoInputStream(workerArgs.context.getContentResolver(), workerArgs.uri);
                    } catch (Exception e) {
                        Log.e("ContactsAsyncHelper", "Error opening photo input stream", e);
                        inputStreamOpenContactPhotoInputStream = null;
                    }
                    if (inputStreamOpenContactPhotoInputStream != null) {
                        workerArgs.result = Drawable.createFromStream(inputStreamOpenContactPhotoInputStream, workerArgs.uri.toString());
                    } else {
                        workerArgs.result = null;
                    }
                    break;
            }
            Message messageObtainMessage = ContactsAsyncHelper.this.obtainMessage(message.what);
            messageObtainMessage.arg1 = message.arg1;
            messageObtainMessage.obj = message.obj;
            messageObtainMessage.sendToTarget();
        }
    }

    private ContactsAsyncHelper() {
        HandlerThread handlerThread = new HandlerThread("ContactsAsyncWorker");
        handlerThread.start();
        sThreadHandler = new WorkerHandler(handlerThread.getLooper());
    }

    public static final void retrieveContactPhotoAsync(Context context, EditEventHelper.AttendeeItem attendeeItem, Runnable runnable, Uri uri) {
        if (uri == null) {
            return;
        }
        WorkerArgs workerArgs = new WorkerArgs();
        workerArgs.context = context;
        workerArgs.item = attendeeItem;
        workerArgs.uri = uri;
        workerArgs.callback = runnable;
        if (mInstance == null) {
            mInstance = new ContactsAsyncHelper();
        }
        Message messageObtainMessage = sThreadHandler.obtainMessage(-1);
        messageObtainMessage.arg1 = 2;
        messageObtainMessage.obj = workerArgs;
        sThreadHandler.sendMessage(messageObtainMessage);
    }

    @Override
    public void handleMessage(Message message) {
        WorkerArgs workerArgs = (WorkerArgs) message.obj;
        switch (message.arg1) {
            case 1:
                if (workerArgs.result != null) {
                    workerArgs.view.setVisibility(0);
                    workerArgs.view.setImageDrawable((Drawable) workerArgs.result);
                } else if (workerArgs.defaultResource != -1) {
                    workerArgs.view.setVisibility(0);
                    workerArgs.view.setImageResource(workerArgs.defaultResource);
                }
                break;
            case 2:
                if (workerArgs.result != null) {
                    workerArgs.item.mBadge = (Drawable) workerArgs.result;
                    if (workerArgs.callback != null) {
                        workerArgs.callback.run();
                    }
                }
                break;
        }
    }
}
