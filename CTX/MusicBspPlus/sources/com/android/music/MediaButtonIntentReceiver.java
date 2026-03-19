package com.android.music;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.os.UserHandle;
import android.view.KeyEvent;
import java.util.Iterator;
import java.util.List;

public class MediaButtonIntentReceiver extends BroadcastReceiver {
    private static Intent s_intent;
    private static Context save_context;
    private static long mLastClickTime = 0;
    private static boolean mDown = false;
    private static boolean mLaunched = false;
    private static Handler sHandler = new Handler() {
        @Override
        public void handleMessage(Message message) {
            switch (message.what) {
                case 1:
                    if (!MediaButtonIntentReceiver.mLaunched) {
                        Context context = (Context) message.obj;
                        Intent intent = new Intent();
                        intent.putExtra("autoshuffle", "true");
                        intent.setClass(context, MusicBrowserActivity.class);
                        intent.setFlags(335544320);
                        context.startActivity(intent);
                        boolean unused = MediaButtonIntentReceiver.mLaunched = true;
                    }
                    break;
                case 2:
                    MusicLogUtils.v("MediaButtonIntentReceiver", "MSG_DELAY_AVRCP_PLAY");
                    ((Context) message.obj).startServiceAsUser(MediaButtonIntentReceiver.s_intent, UserHandle.CURRENT);
                    break;
            }
        }
    };

    @Override
    public void onReceive(Context context, Intent intent) {
        KeyEvent keyEvent;
        String action = intent.getAction();
        MusicLogUtils.d("MediaButtonIntentReceiver", "intentAction " + action);
        if ("android.media.AUDIO_BECOMING_NOISY".equals(action)) {
            if (isMusicServiceRunning(context)) {
                Intent intent2 = new Intent(context, (Class<?>) MediaPlaybackService.class);
                intent2.setAction("com.android.music.musicservicecommand");
                intent2.putExtra("command", "pause");
                context.startServiceAsUser(intent2, UserHandle.CURRENT);
                return;
            }
            return;
        }
        if (!"android.intent.action.MEDIA_BUTTON".equals(action) || (keyEvent = (KeyEvent) intent.getParcelableExtra("android.intent.extra.KEY_EVENT")) == null) {
            return;
        }
        int keyCode = keyEvent.getKeyCode();
        int action2 = keyEvent.getAction();
        long eventTime = keyEvent.getEventTime();
        long j = eventTime - mLastClickTime;
        if (action2 == 0) {
            sHandler.removeMessages(2);
        }
        String str = null;
        if (keyCode != 79) {
            switch (keyCode) {
                case 85:
                    str = "togglepause";
                    break;
                case 86:
                    str = "stop";
                    break;
                case 87:
                    str = "next";
                    break;
                case 88:
                    str = "previous";
                    break;
                case 89:
                    str = "rewind";
                    break;
                case 90:
                    str = "forward";
                    break;
                default:
                    switch (keyCode) {
                        case 126:
                            str = "play";
                            break;
                        case 127:
                            str = "pause";
                            break;
                    }
                    break;
            }
        }
        MusicLogUtils.e("MediaButtonIntentReceiver", str + "key event" + action2);
        if (str != null) {
            if (action2 == 0) {
                if (mDown) {
                    if (("togglepause".equals(str) || "play".equals(str)) && mLastClickTime != 0 && eventTime - mLastClickTime > 1000) {
                        sHandler.sendMessage(sHandler.obtainMessage(1, context));
                    }
                    if (("forward".equals(str) || "rewind".equals(str)) && mLastClickTime != 0 && j > 500) {
                        sendToStartService(context, str, j);
                        mLastClickTime = eventTime;
                    }
                } else if (keyEvent.getRepeatCount() == 0) {
                    Intent intent3 = new Intent(context, (Class<?>) MediaPlaybackService.class);
                    intent3.setAction("com.android.music.musicservicecommand");
                    if (keyCode == 79 && eventTime - mLastClickTime < 300) {
                        intent3.putExtra("command", "next");
                        context.startServiceAsUser(intent3, UserHandle.CURRENT);
                        mLastClickTime = 0L;
                    } else if ("forward".equals(str) || "rewind".equals(str)) {
                        mLastClickTime = eventTime;
                    } else {
                        intent3.putExtra("command", str);
                        if ("play".equals(str)) {
                            s_intent = intent3;
                            save_context = context;
                            sHandler.sendMessageDelayed(sHandler.obtainMessage(2, context), 200L);
                        } else {
                            MusicLogUtils.d("MediaButtonIntentReceiver", str + ",UserHandle.CURRENT: " + UserHandle.CURRENT);
                            context.startServiceAsUser(intent3, UserHandle.CURRENT);
                        }
                        mLastClickTime = eventTime;
                    }
                    mLaunched = false;
                    mDown = true;
                }
            } else {
                if ("forward".equals(str) || "rewind".equals(str)) {
                    sendToStartService(context, "endscan", j);
                    mLastClickTime = 0L;
                }
                sHandler.removeMessages(1);
                mDown = false;
            }
            if (isOrderedBroadcast()) {
                abortBroadcast();
            }
        }
    }

    public void sendToStartService(Context context, String str, long j) {
        Intent intent = new Intent(context, (Class<?>) MediaPlaybackService.class);
        intent.setAction("com.android.music.musicservicecommand");
        intent.putExtra("command", str);
        intent.putExtra("deltatime", j);
        MusicLogUtils.d("MediaButtonIntentReceiver", "sendToStartService,UserHandle.CURRENT: " + UserHandle.CURRENT);
        context.startServiceAsUser(intent, UserHandle.CURRENT);
    }

    private boolean isMusicServiceRunning(Context context) {
        boolean z;
        List<ActivityManager.RunningServiceInfo> runningServices = ((ActivityManager) context.getSystemService("activity")).getRunningServices(100);
        Iterator<ActivityManager.RunningServiceInfo> it = runningServices.iterator();
        while (true) {
            if (it.hasNext()) {
                if (MediaPlaybackService.class.getName().equals(it.next().service.getClassName())) {
                    z = true;
                    break;
                }
            } else {
                z = false;
                break;
            }
        }
        MusicLogUtils.d("MediaButtonIntentReceiver", "isMusicServiceRunning " + z + ", Runing service num is " + runningServices.size());
        return z;
    }
}
