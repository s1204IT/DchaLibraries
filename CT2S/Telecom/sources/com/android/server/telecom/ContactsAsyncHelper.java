package com.android.server.telecom;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import java.io.IOException;
import java.io.InputStream;

public final class ContactsAsyncHelper {
    private static final String LOG_TAG = ContactsAsyncHelper.class.getSimpleName();
    private static final Handler sResultHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message message) {
            WorkerArgs workerArgs = (WorkerArgs) message.obj;
            switch (message.arg1) {
                case 1:
                    if (workerArgs.listener != null) {
                        Log.d(this, "Notifying listener: " + workerArgs.listener.toString() + " image: " + workerArgs.displayPhotoUri + " completed", new Object[0]);
                        workerArgs.listener.onImageLoadComplete(message.what, workerArgs.photo, workerArgs.photoIcon, workerArgs.cookie);
                    }
                    break;
            }
        }
    };
    private static final Handler sThreadHandler;

    public interface OnImageLoadCompleteListener {
        void onImageLoadComplete(int i, Drawable drawable, Bitmap bitmap, Object obj);
    }

    static {
        HandlerThread handlerThread = new HandlerThread("ContactsAsyncWorker");
        handlerThread.start();
        sThreadHandler = new WorkerHandler(handlerThread.getLooper());
    }

    private ContactsAsyncHelper() {
    }

    private static final class WorkerArgs {
        public Context context;
        public Object cookie;
        public Uri displayPhotoUri;
        public OnImageLoadCompleteListener listener;
        public Drawable photo;
        public Bitmap photoIcon;

        private WorkerArgs() {
        }
    }

    private static class WorkerHandler extends Handler {
        public WorkerHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message message) throws Throwable {
            InputStream inputStreamOpenInputStream;
            WorkerArgs workerArgs = (WorkerArgs) message.obj;
            try {
                switch (message.arg1) {
                    case 1:
                        try {
                            inputStreamOpenInputStream = workerArgs.context.getContentResolver().openInputStream(workerArgs.displayPhotoUri);
                            break;
                        } catch (Exception e) {
                            Log.e(this, e, "Error opening photo input stream", new Object[0]);
                            inputStreamOpenInputStream = null;
                        }
                        try {
                            if (inputStreamOpenInputStream != null) {
                                workerArgs.photo = Drawable.createFromStream(inputStreamOpenInputStream, workerArgs.displayPhotoUri.toString());
                                workerArgs.photoIcon = getPhotoIconWhenAppropriate(workerArgs.context, workerArgs.photo);
                                Log.d(this, "Loading image: " + message.arg1 + " token: " + message.what + " image URI: " + workerArgs.displayPhotoUri, new Object[0]);
                            } else {
                                workerArgs.photo = null;
                                workerArgs.photoIcon = null;
                                Log.d(this, "Problem with image: " + message.arg1 + " token: " + message.what + " image URI: " + workerArgs.displayPhotoUri + ", using default image.", new Object[0]);
                            }
                            if (inputStreamOpenInputStream != null) {
                                try {
                                    inputStreamOpenInputStream.close();
                                } catch (IOException e2) {
                                    Log.e(this, e2, "Unable to close input stream.", new Object[0]);
                                }
                            }
                            break;
                        } catch (Throwable th) {
                            th = th;
                            if (inputStreamOpenInputStream != null) {
                                try {
                                    inputStreamOpenInputStream.close();
                                } catch (IOException e3) {
                                    Log.e(this, e3, "Unable to close input stream.", new Object[0]);
                                }
                                break;
                            }
                            throw th;
                        }
                    default:
                        Message messageObtainMessage = ContactsAsyncHelper.sResultHandler.obtainMessage(message.what);
                        messageObtainMessage.arg1 = message.arg1;
                        messageObtainMessage.obj = message.obj;
                        messageObtainMessage.sendToTarget();
                        return;
                }
            } catch (Throwable th2) {
                th = th2;
                inputStreamOpenInputStream = null;
            }
        }

        private Bitmap getPhotoIconWhenAppropriate(Context context, Drawable drawable) {
            if (!(drawable instanceof BitmapDrawable)) {
                return null;
            }
            int dimensionPixelSize = context.getResources().getDimensionPixelSize(R.dimen.notification_icon_size);
            Bitmap bitmap = ((BitmapDrawable) drawable).getBitmap();
            int width = bitmap.getWidth();
            int height = bitmap.getHeight();
            int i = width > height ? width : height;
            if (i <= dimensionPixelSize) {
                return bitmap;
            }
            float f = i / dimensionPixelSize;
            int i2 = (int) (width / f);
            int i3 = (int) (height / f);
            if (i2 <= 0 || i3 <= 0) {
                Log.w(this, "Photo icon's width or height become 0.", new Object[0]);
                return null;
            }
            return Bitmap.createScaledBitmap(bitmap, i2, i3, true);
        }
    }

    public static final void startObtainPhotoAsync(int i, Context context, Uri uri, OnImageLoadCompleteListener onImageLoadCompleteListener, Object obj) {
        ThreadUtil.checkOnMainThread();
        if (uri == null) {
            Log.wtf(LOG_TAG, "Uri is missing", new Object[0]);
            return;
        }
        WorkerArgs workerArgs = new WorkerArgs();
        workerArgs.cookie = obj;
        workerArgs.context = context;
        workerArgs.displayPhotoUri = uri;
        workerArgs.listener = onImageLoadCompleteListener;
        Message messageObtainMessage = sThreadHandler.obtainMessage(i);
        messageObtainMessage.arg1 = 1;
        messageObtainMessage.obj = workerArgs;
        Log.d(LOG_TAG, "Begin loading image: " + workerArgs.displayPhotoUri + ", displaying default image for now.", new Object[0]);
        sThreadHandler.sendMessage(messageObtainMessage);
    }
}
