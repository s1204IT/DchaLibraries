package com.android.music;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.view.KeyEvent;

public class MediaButtonIntentReceiver extends BroadcastReceiver {
    private static long mLastClickTime = 0;
    private static boolean mDown = false;
    private static boolean mLaunched = false;
    private static Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    if (!MediaButtonIntentReceiver.mLaunched) {
                        Context context = (Context) msg.obj;
                        Intent i = new Intent();
                        i.putExtra("autoshuffle", "true");
                        i.setClass(context, MusicBrowserActivity.class);
                        i.setFlags(335544320);
                        context.startActivity(i);
                        boolean unused = MediaButtonIntentReceiver.mLaunched = true;
                    }
                    break;
            }
        }
    };

    @Override
    public void onReceive(Context context, Intent intent) {
        KeyEvent event;
        String intentAction = intent.getAction();
        if ("android.media.AUDIO_BECOMING_NOISY".equals(intentAction)) {
            Intent i = new Intent(context, (Class<?>) MediaPlaybackService.class);
            i.setAction("com.android.music.musicservicecommand");
            i.putExtra("command", "pause");
            context.startService(i);
            return;
        }
        if ("android.intent.action.MEDIA_BUTTON".equals(intentAction) && (event = (KeyEvent) intent.getParcelableExtra("android.intent.extra.KEY_EVENT")) != null) {
            int keycode = event.getKeyCode();
            int action = event.getAction();
            long eventtime = event.getEventTime();
            String command = null;
            switch (keycode) {
                case 79:
                case 85:
                    command = "togglepause";
                    break;
                case 86:
                    command = "stop";
                    break;
                case 87:
                    command = "next";
                    break;
                case 88:
                    command = "previous";
                    break;
                case 89:
                    command = "rewind";
                    break;
                case 126:
                    command = "play";
                    break;
                case 127:
                    command = "pause";
                    break;
            }
            if (command != null) {
                if (action == 0) {
                    if (mDown) {
                        if (("togglepause".equals(command) || "play".equals(command)) && mLastClickTime != 0 && eventtime - mLastClickTime > 1000) {
                            mHandler.sendMessage(mHandler.obtainMessage(1, context));
                        }
                    } else if (event.getRepeatCount() == 0) {
                        Intent i2 = new Intent(context, (Class<?>) MediaPlaybackService.class);
                        i2.setAction("com.android.music.musicservicecommand");
                        if (keycode == 79 && eventtime - mLastClickTime < 600) {
                            i2.putExtra("command", "next");
                            context.startService(i2);
                            mLastClickTime = 0L;
                        } else {
                            i2.putExtra("command", command);
                            context.startService(i2);
                            mLastClickTime = eventtime;
                        }
                        mLaunched = false;
                        mDown = true;
                    }
                } else {
                    if (event.isLongPress()) {
                        mHandler.sendMessage(mHandler.obtainMessage(1, context));
                    }
                    mDown = false;
                }
                if (isOrderedBroadcast()) {
                    abortBroadcast();
                }
            }
        }
    }
}
