package com.android.systemui.media;

import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.media.AudioAttributes;
import android.media.IAudioService;
import android.media.IRingtonePlayer;
import android.media.Ringtone;
import android.net.Uri;
import android.os.Binder;
import android.os.IBinder;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.provider.MediaStore;
import android.util.Log;
import com.android.systemui.SystemUI;
import java.io.FileDescriptor;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;

/* loaded from: classes.dex */
public class RingtonePlayer extends SystemUI {
    private IAudioService mAudioService;
    private final NotificationPlayer mAsyncPlayer = new NotificationPlayer("RingtonePlayer");
    private final HashMap<IBinder, Client> mClients = new HashMap<>();
    private IRingtonePlayer mCallback = new IRingtonePlayer.Stub() { // from class: com.android.systemui.media.RingtonePlayer.1
        public void play(IBinder iBinder, Uri uri, AudioAttributes audioAttributes, float f, boolean z) throws RemoteException {
            Client client;
            synchronized (RingtonePlayer.this.mClients) {
                client = (Client) RingtonePlayer.this.mClients.get(iBinder);
                if (client == null) {
                    client = RingtonePlayer.this.new Client(iBinder, uri, Binder.getCallingUserHandle(), audioAttributes);
                    iBinder.linkToDeath(client, 0);
                    RingtonePlayer.this.mClients.put(iBinder, client);
                }
            }
            client.mRingtone.setLooping(z);
            client.mRingtone.setVolume(f);
            client.mRingtone.play();
        }

        public void stop(IBinder iBinder) {
            Client client;
            synchronized (RingtonePlayer.this.mClients) {
                client = (Client) RingtonePlayer.this.mClients.remove(iBinder);
            }
            if (client != null) {
                client.mToken.unlinkToDeath(client, 0);
                client.mRingtone.stop();
            }
        }

        public boolean isPlaying(IBinder iBinder) {
            Client client;
            synchronized (RingtonePlayer.this.mClients) {
                client = (Client) RingtonePlayer.this.mClients.get(iBinder);
            }
            if (client != null) {
                return client.mRingtone.isPlaying();
            }
            return false;
        }

        public void setPlaybackProperties(IBinder iBinder, float f, boolean z) {
            Client client;
            synchronized (RingtonePlayer.this.mClients) {
                client = (Client) RingtonePlayer.this.mClients.get(iBinder);
            }
            if (client != null) {
                client.mRingtone.setVolume(f);
                client.mRingtone.setLooping(z);
            }
        }

        public void playAsync(Uri uri, UserHandle userHandle, boolean z, AudioAttributes audioAttributes) {
            if (Binder.getCallingUid() != 1000) {
                throw new SecurityException("Async playback only available from system UID.");
            }
            if (UserHandle.ALL.equals(userHandle)) {
                userHandle = UserHandle.SYSTEM;
            }
            RingtonePlayer.this.mAsyncPlayer.play(RingtonePlayer.this.getContextForUser(userHandle), uri, z, audioAttributes);
        }

        public void stopAsync() {
            if (Binder.getCallingUid() == 1000) {
                RingtonePlayer.this.mAsyncPlayer.stop();
                return;
            }
            throw new SecurityException("Async playback only available from system UID.");
        }

        public String getTitle(Uri uri) {
            return Ringtone.getTitle(RingtonePlayer.this.getContextForUser(Binder.getCallingUserHandle()), uri, false, false);
        }

        public ParcelFileDescriptor openRingtone(Uri uri) {
            ContentResolver contentResolver = RingtonePlayer.this.getContextForUser(Binder.getCallingUserHandle()).getContentResolver();
            if (uri.toString().startsWith(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI.toString())) {
                Cursor cursorQuery = contentResolver.query(uri, new String[]{"is_ringtone", "is_alarm", "is_notification"}, null, null, null);
                Throwable th = null;
                try {
                    if (cursorQuery.moveToFirst() && (cursorQuery.getInt(0) != 0 || cursorQuery.getInt(1) != 0 || cursorQuery.getInt(2) != 0)) {
                        try {
                            ParcelFileDescriptor parcelFileDescriptorOpenFileDescriptor = contentResolver.openFileDescriptor(uri, "r");
                            if (cursorQuery != null) {
                                cursorQuery.close();
                            }
                            return parcelFileDescriptorOpenFileDescriptor;
                        } catch (IOException e) {
                            throw new SecurityException(e);
                        }
                    }
                    if (cursorQuery != null) {
                        cursorQuery.close();
                    }
                } catch (Throwable th2) {
                    if (cursorQuery != null) {
                        if (0 != 0) {
                            try {
                                cursorQuery.close();
                            } catch (Throwable th3) {
                                th.addSuppressed(th3);
                            }
                        } else {
                            cursorQuery.close();
                        }
                    }
                    throw th2;
                }
            }
            throw new SecurityException("Uri is not ringtone, alarm, or notification: " + uri);
        }
    };

    @Override // com.android.systemui.SystemUI
    public void start() {
        this.mAsyncPlayer.setUsesWakeLock(this.mContext);
        this.mAudioService = IAudioService.Stub.asInterface(ServiceManager.getService("audio"));
        try {
            this.mAudioService.setRingtonePlayer(this.mCallback);
        } catch (RemoteException e) {
            Log.e("RingtonePlayer", "Problem registering RingtonePlayer: " + e);
        }
    }

    private class Client implements IBinder.DeathRecipient {
        private final Ringtone mRingtone;
        private final IBinder mToken;

        public Client(IBinder iBinder, Uri uri, UserHandle userHandle, AudioAttributes audioAttributes) throws IllegalArgumentException {
            this.mToken = iBinder;
            this.mRingtone = new Ringtone(RingtonePlayer.this.getContextForUser(userHandle), false);
            this.mRingtone.setAudioAttributes(audioAttributes);
            this.mRingtone.setUri(uri);
        }

        @Override // android.os.IBinder.DeathRecipient
        public void binderDied() {
            synchronized (RingtonePlayer.this.mClients) {
                RingtonePlayer.this.mClients.remove(this.mToken);
            }
            this.mRingtone.stop();
        }
    }

    private Context getContextForUser(UserHandle userHandle) {
        try {
            return this.mContext.createPackageContextAsUser(this.mContext.getPackageName(), 0, userHandle);
        } catch (PackageManager.NameNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    @Override // com.android.systemui.SystemUI
    public void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
        printWriter.println("Clients:");
        synchronized (this.mClients) {
            for (Client client : this.mClients.values()) {
                printWriter.print("  mToken=");
                printWriter.print(client.mToken);
                printWriter.print(" mUri=");
                printWriter.println(client.mRingtone.getUri());
            }
        }
    }
}
