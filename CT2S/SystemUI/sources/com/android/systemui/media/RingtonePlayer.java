package com.android.systemui.media;

import android.content.Context;
import android.content.pm.PackageManager;
import android.media.AudioAttributes;
import android.media.IAudioService;
import android.media.IRingtonePlayer;
import android.media.Ringtone;
import android.net.Uri;
import android.os.Binder;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.util.Log;
import com.android.systemui.SystemUI;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.HashMap;

public class RingtonePlayer extends SystemUI {
    private IAudioService mAudioService;
    private final NotificationPlayer mAsyncPlayer = new NotificationPlayer("RingtonePlayer");
    private final HashMap<IBinder, Client> mClients = new HashMap<>();
    private IRingtonePlayer mCallback = new IRingtonePlayer.Stub() {
        public void play(IBinder token, Uri uri, AudioAttributes aa) throws RemoteException {
            Client client;
            synchronized (RingtonePlayer.this.mClients) {
                client = (Client) RingtonePlayer.this.mClients.get(token);
                if (client == null) {
                    UserHandle user = Binder.getCallingUserHandle();
                    client = RingtonePlayer.this.new Client(token, uri, user, aa);
                    token.linkToDeath(client, 0);
                    RingtonePlayer.this.mClients.put(token, client);
                }
            }
            client.mRingtone.play();
        }

        public void stop(IBinder token) {
            Client client;
            synchronized (RingtonePlayer.this.mClients) {
                client = (Client) RingtonePlayer.this.mClients.remove(token);
            }
            if (client != null) {
                client.mToken.unlinkToDeath(client, 0);
                client.mRingtone.stop();
            }
        }

        public boolean isPlaying(IBinder token) {
            Client client;
            synchronized (RingtonePlayer.this.mClients) {
                client = (Client) RingtonePlayer.this.mClients.get(token);
            }
            if (client != null) {
                return client.mRingtone.isPlaying();
            }
            return false;
        }

        public void playAsync(Uri uri, UserHandle user, boolean looping, AudioAttributes aa) {
            if (Binder.getCallingUid() == 1000) {
                RingtonePlayer.this.mAsyncPlayer.play(RingtonePlayer.this.getContextForUser(user), uri, looping, aa);
                return;
            }
            throw new SecurityException("Async playback only available from system UID.");
        }

        public void stopAsync() {
            if (Binder.getCallingUid() == 1000) {
                RingtonePlayer.this.mAsyncPlayer.stop();
                return;
            }
            throw new SecurityException("Async playback only available from system UID.");
        }
    };

    @Override
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

        public Client(IBinder token, Uri uri, UserHandle user, AudioAttributes aa) {
            this.mToken = token;
            this.mRingtone = new Ringtone(RingtonePlayer.this.getContextForUser(user), false);
            this.mRingtone.setAudioAttributes(aa);
            this.mRingtone.setUri(uri);
        }

        @Override
        public void binderDied() {
            synchronized (RingtonePlayer.this.mClients) {
                RingtonePlayer.this.mClients.remove(this.mToken);
            }
            this.mRingtone.stop();
        }
    }

    public Context getContextForUser(UserHandle user) {
        try {
            return this.mContext.createPackageContextAsUser(this.mContext.getPackageName(), 0, user);
        } catch (PackageManager.NameNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        pw.println("Clients:");
        synchronized (this.mClients) {
            for (Client client : this.mClients.values()) {
                pw.print("  mToken=");
                pw.print(client.mToken);
                pw.print(" mUri=");
                pw.println(client.mRingtone.getUri());
            }
        }
    }
}
