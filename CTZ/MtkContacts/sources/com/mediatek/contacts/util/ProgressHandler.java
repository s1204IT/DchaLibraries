package com.mediatek.contacts.util;

import android.app.FragmentManager;
import android.os.Handler;
import android.os.Message;
import com.mediatek.contacts.widget.SimpleProgressDialogFragment;

public class ProgressHandler extends Handler {
    public void showDialog(FragmentManager fragmentManager) {
        SimpleProgressDialogFragment.show(fragmentManager);
    }

    public void dismissDialog(FragmentManager fragmentManager) {
        removeMessages(0);
        sendMessage(obtainMessage(1, fragmentManager));
    }

    @Override
    public void handleMessage(Message message) {
        Log.d("ProgressHandler", "[handleMessage]msg.what = " + message.what + ", msg.obj = " + message.obj);
        switch (message.what) {
            case 0:
                SimpleProgressDialogFragment.show((FragmentManager) message.obj);
                break;
            case 1:
                SimpleProgressDialogFragment.dismiss((FragmentManager) message.obj);
                break;
            default:
                Log.w("ProgressHandler", "[handleMessage]unexpected message: " + message.what);
                break;
        }
    }
}
