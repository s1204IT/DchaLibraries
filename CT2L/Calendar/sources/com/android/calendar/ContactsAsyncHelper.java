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
        public void handleMessage(Message msg) {
            WorkerArgs args = (WorkerArgs) msg.obj;
            switch (msg.arg1) {
                case 1:
                case 2:
                    InputStream inputStream = null;
                    try {
                        inputStream = ContactsContract.Contacts.openContactPhotoInputStream(args.context.getContentResolver(), args.uri);
                    } catch (Exception e) {
                        Log.e("ContactsAsyncHelper", "Error opening photo input stream", e);
                    }
                    if (inputStream != null) {
                        args.result = Drawable.createFromStream(inputStream, args.uri.toString());
                    } else {
                        args.result = null;
                    }
                    break;
            }
            Message reply = ContactsAsyncHelper.this.obtainMessage(msg.what);
            reply.arg1 = msg.arg1;
            reply.obj = msg.obj;
            reply.sendToTarget();
        }
    }

    private ContactsAsyncHelper() {
        HandlerThread thread = new HandlerThread("ContactsAsyncWorker");
        thread.start();
        sThreadHandler = new WorkerHandler(thread.getLooper());
    }

    public static final void retrieveContactPhotoAsync(Context context, EditEventHelper.AttendeeItem item, Runnable run, Uri photoUri) {
        if (photoUri != null) {
            WorkerArgs args = new WorkerArgs();
            args.context = context;
            args.item = item;
            args.uri = photoUri;
            args.callback = run;
            if (mInstance == null) {
                mInstance = new ContactsAsyncHelper();
            }
            Message msg = sThreadHandler.obtainMessage(-1);
            msg.arg1 = 2;
            msg.obj = args;
            sThreadHandler.sendMessage(msg);
        }
    }

    @Override
    public void handleMessage(Message msg) {
        WorkerArgs args = (WorkerArgs) msg.obj;
        switch (msg.arg1) {
            case 1:
                if (args.result != null) {
                    args.view.setVisibility(0);
                    args.view.setImageDrawable((Drawable) args.result);
                } else if (args.defaultResource != -1) {
                    args.view.setVisibility(0);
                    args.view.setImageResource(args.defaultResource);
                }
                break;
            case 2:
                if (args.result != null) {
                    args.item.mBadge = (Drawable) args.result;
                    if (args.callback != null) {
                        args.callback.run();
                    }
                }
                break;
        }
    }
}
