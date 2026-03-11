package android.support.v7.media;

import android.content.IntentFilter;
import android.content.IntentSender;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public final class MediaRouteDescriptor {
    final Bundle mBundle;
    List<IntentFilter> mControlFilters;

    MediaRouteDescriptor(Bundle bundle, List<IntentFilter> controlFilters) {
        this.mBundle = bundle;
        this.mControlFilters = controlFilters;
    }

    public String getId() {
        return this.mBundle.getString("id");
    }

    public List<String> getGroupMemberIds() {
        return this.mBundle.getStringArrayList("groupMemberIds");
    }

    public String getName() {
        return this.mBundle.getString("name");
    }

    public String getDescription() {
        return this.mBundle.getString("status");
    }

    public Uri getIconUri() {
        String iconUri = this.mBundle.getString("iconUri");
        if (iconUri == null) {
            return null;
        }
        return Uri.parse(iconUri);
    }

    public boolean isEnabled() {
        return this.mBundle.getBoolean("enabled", true);
    }

    @Deprecated
    public boolean isConnecting() {
        return this.mBundle.getBoolean("connecting", false);
    }

    public int getConnectionState() {
        return this.mBundle.getInt("connectionState", 0);
    }

    public boolean canDisconnectAndKeepPlaying() {
        return this.mBundle.getBoolean("canDisconnect", false);
    }

    public IntentSender getSettingsActivity() {
        return (IntentSender) this.mBundle.getParcelable("settingsIntent");
    }

    public List<IntentFilter> getControlFilters() {
        ensureControlFilters();
        return this.mControlFilters;
    }

    void ensureControlFilters() {
        if (this.mControlFilters == null) {
            this.mControlFilters = this.mBundle.getParcelableArrayList("controlFilters");
            if (this.mControlFilters == null) {
                this.mControlFilters = Collections.emptyList();
            }
        }
    }

    public int getPlaybackType() {
        return this.mBundle.getInt("playbackType", 1);
    }

    public int getPlaybackStream() {
        return this.mBundle.getInt("playbackStream", -1);
    }

    public int getDeviceType() {
        return this.mBundle.getInt("deviceType");
    }

    public int getVolume() {
        return this.mBundle.getInt("volume");
    }

    public int getVolumeMax() {
        return this.mBundle.getInt("volumeMax");
    }

    public int getVolumeHandling() {
        return this.mBundle.getInt("volumeHandling", 0);
    }

    public int getPresentationDisplayId() {
        return this.mBundle.getInt("presentationDisplayId", -1);
    }

    public Bundle getExtras() {
        return this.mBundle.getBundle("extras");
    }

    public int getMinClientVersion() {
        return this.mBundle.getInt("minClientVersion", 1);
    }

    public int getMaxClientVersion() {
        return this.mBundle.getInt("maxClientVersion", Integer.MAX_VALUE);
    }

    public boolean isValid() {
        ensureControlFilters();
        if (TextUtils.isEmpty(getId()) || TextUtils.isEmpty(getName()) || this.mControlFilters.contains(null)) {
            return false;
        }
        return true;
    }

    public String toString() {
        return "MediaRouteDescriptor{ id=" + getId() + ", groupMemberIds=" + getGroupMemberIds() + ", name=" + getName() + ", description=" + getDescription() + ", iconUri=" + getIconUri() + ", isEnabled=" + isEnabled() + ", isConnecting=" + isConnecting() + ", connectionState=" + getConnectionState() + ", controlFilters=" + Arrays.toString(getControlFilters().toArray()) + ", playbackType=" + getPlaybackType() + ", playbackStream=" + getPlaybackStream() + ", deviceType=" + getDeviceType() + ", volume=" + getVolume() + ", volumeMax=" + getVolumeMax() + ", volumeHandling=" + getVolumeHandling() + ", presentationDisplayId=" + getPresentationDisplayId() + ", extras=" + getExtras() + ", isValid=" + isValid() + ", minClientVersion=" + getMinClientVersion() + ", maxClientVersion=" + getMaxClientVersion() + " }";
    }

    public Bundle asBundle() {
        return this.mBundle;
    }

    public static MediaRouteDescriptor fromBundle(Bundle bundle) {
        if (bundle != null) {
            return new MediaRouteDescriptor(bundle, null);
        }
        return null;
    }

    public static final class Builder {
        private final Bundle mBundle;
        private ArrayList<IntentFilter> mControlFilters;
        private ArrayList<String> mGroupMemberIds;

        public Builder(String id, String name) {
            this.mBundle = new Bundle();
            setId(id);
            setName(name);
        }

        public Builder(MediaRouteDescriptor descriptor) {
            if (descriptor == null) {
                throw new IllegalArgumentException("descriptor must not be null");
            }
            this.mBundle = new Bundle(descriptor.mBundle);
            descriptor.ensureControlFilters();
            if (!descriptor.mControlFilters.isEmpty()) {
                this.mControlFilters = new ArrayList<>(descriptor.mControlFilters);
            }
        }

        public Builder setId(String id) {
            this.mBundle.putString("id", id);
            return this;
        }

        public Builder setName(String name) {
            this.mBundle.putString("name", name);
            return this;
        }

        public Builder setDescription(String description) {
            this.mBundle.putString("status", description);
            return this;
        }

        public Builder setEnabled(boolean enabled) {
            this.mBundle.putBoolean("enabled", enabled);
            return this;
        }

        @Deprecated
        public Builder setConnecting(boolean connecting) {
            this.mBundle.putBoolean("connecting", connecting);
            return this;
        }

        public Builder addControlFilter(IntentFilter filter) {
            if (filter == null) {
                throw new IllegalArgumentException("filter must not be null");
            }
            if (this.mControlFilters == null) {
                this.mControlFilters = new ArrayList<>();
            }
            if (!this.mControlFilters.contains(filter)) {
                this.mControlFilters.add(filter);
            }
            return this;
        }

        public Builder addControlFilters(Collection<IntentFilter> filters) {
            if (filters == null) {
                throw new IllegalArgumentException("filters must not be null");
            }
            if (!filters.isEmpty()) {
                for (IntentFilter filter : filters) {
                    addControlFilter(filter);
                }
            }
            return this;
        }

        public Builder setPlaybackType(int playbackType) {
            this.mBundle.putInt("playbackType", playbackType);
            return this;
        }

        public Builder setPlaybackStream(int playbackStream) {
            this.mBundle.putInt("playbackStream", playbackStream);
            return this;
        }

        public Builder setDeviceType(int deviceType) {
            this.mBundle.putInt("deviceType", deviceType);
            return this;
        }

        public Builder setVolume(int volume) {
            this.mBundle.putInt("volume", volume);
            return this;
        }

        public Builder setVolumeMax(int volumeMax) {
            this.mBundle.putInt("volumeMax", volumeMax);
            return this;
        }

        public Builder setVolumeHandling(int volumeHandling) {
            this.mBundle.putInt("volumeHandling", volumeHandling);
            return this;
        }

        public Builder setPresentationDisplayId(int presentationDisplayId) {
            this.mBundle.putInt("presentationDisplayId", presentationDisplayId);
            return this;
        }

        public MediaRouteDescriptor build() {
            if (this.mControlFilters != null) {
                this.mBundle.putParcelableArrayList("controlFilters", this.mControlFilters);
            }
            if (this.mGroupMemberIds != null) {
                this.mBundle.putStringArrayList("groupMemberIds", this.mGroupMemberIds);
            }
            return new MediaRouteDescriptor(this.mBundle, this.mControlFilters);
        }
    }
}
