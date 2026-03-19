package android.media;

import android.media.update.ApiLoader;
import android.media.update.MediaSession2Provider;
import android.os.Bundle;
import java.util.Set;

public final class SessionCommandGroup2 {
    private final MediaSession2Provider.CommandGroupProvider mProvider;

    public SessionCommandGroup2() {
        this.mProvider = ApiLoader.getProvider().createMediaSession2CommandGroup(this, null);
    }

    public SessionCommandGroup2(SessionCommandGroup2 sessionCommandGroup2) {
        this.mProvider = ApiLoader.getProvider().createMediaSession2CommandGroup(this, sessionCommandGroup2);
    }

    public SessionCommandGroup2(MediaSession2Provider.CommandGroupProvider commandGroupProvider) {
        this.mProvider = commandGroupProvider;
    }

    public void addCommand(SessionCommand2 sessionCommand2) {
        this.mProvider.addCommand_impl(sessionCommand2);
    }

    public void addCommand(int i) {
    }

    public void addAllPredefinedCommands() {
        this.mProvider.addAllPredefinedCommands_impl();
    }

    public void removeCommand(SessionCommand2 sessionCommand2) {
        this.mProvider.removeCommand_impl(sessionCommand2);
    }

    public void removeCommand(int i) {
    }

    public boolean hasCommand(SessionCommand2 sessionCommand2) {
        return this.mProvider.hasCommand_impl(sessionCommand2);
    }

    public boolean hasCommand(int i) {
        return this.mProvider.hasCommand_impl(i);
    }

    public Set<SessionCommand2> getCommands() {
        return this.mProvider.getCommands_impl();
    }

    public MediaSession2Provider.CommandGroupProvider getProvider() {
        return this.mProvider;
    }

    public Bundle toBundle() {
        return this.mProvider.toBundle_impl();
    }

    public static SessionCommandGroup2 fromBundle(Bundle bundle) {
        return ApiLoader.getProvider().fromBundle_MediaSession2CommandGroup(bundle);
    }
}
