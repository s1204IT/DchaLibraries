package android.app;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.media.AudioAttributes;
import android.media.session.MediaSession;
import android.net.ProxyInfo;
import android.net.Uri;
import android.os.BadParcelableException;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.SystemClock;
import android.os.UserHandle;
import android.text.TextUtils;
import android.util.Log;
import android.util.MathUtils;
import android.widget.RemoteViews;
import com.android.internal.R;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.util.NotificationColorUtil;
import java.lang.reflect.Constructor;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class Notification implements Parcelable {
    public static final String CATEGORY_ALARM = "alarm";
    public static final String CATEGORY_CALL = "call";
    public static final String CATEGORY_EMAIL = "email";
    public static final String CATEGORY_ERROR = "err";
    public static final String CATEGORY_EVENT = "event";
    public static final String CATEGORY_MESSAGE = "msg";
    public static final String CATEGORY_PROGRESS = "progress";
    public static final String CATEGORY_PROMO = "promo";
    public static final String CATEGORY_RECOMMENDATION = "recommendation";
    public static final String CATEGORY_SERVICE = "service";
    public static final String CATEGORY_SOCIAL = "social";
    public static final String CATEGORY_STATUS = "status";
    public static final String CATEGORY_SYSTEM = "sys";
    public static final String CATEGORY_TRANSPORT = "transport";
    public static final int COLOR_DEFAULT = 0;
    public static final int DEFAULT_ALL = -1;
    public static final int DEFAULT_LIGHTS = 4;
    public static final int DEFAULT_SOUND = 1;
    public static final int DEFAULT_VIBRATE = 2;
    public static final String EXTRA_ALLOW_DURING_SETUP = "android.allowDuringSetup";
    public static final String EXTRA_AS_HEADS_UP = "headsup";
    public static final String EXTRA_BACKGROUND_IMAGE_URI = "android.backgroundImageUri";
    public static final String EXTRA_BIG_TEXT = "android.bigText";
    public static final String EXTRA_COMPACT_ACTIONS = "android.compactActions";
    public static final String EXTRA_INFO_TEXT = "android.infoText";
    public static final String EXTRA_LARGE_ICON = "android.largeIcon";
    public static final String EXTRA_LARGE_ICON_BIG = "android.largeIcon.big";
    public static final String EXTRA_MEDIA_SESSION = "android.mediaSession";
    public static final String EXTRA_ORIGINATING_USERID = "android.originatingUserId";
    public static final String EXTRA_PEOPLE = "android.people";
    public static final String EXTRA_PICTURE = "android.picture";
    public static final String EXTRA_PROGRESS = "android.progress";
    public static final String EXTRA_PROGRESS_INDETERMINATE = "android.progressIndeterminate";
    public static final String EXTRA_PROGRESS_MAX = "android.progressMax";
    public static final String EXTRA_SHOW_CHRONOMETER = "android.showChronometer";
    public static final String EXTRA_SHOW_WHEN = "android.showWhen";
    public static final String EXTRA_SMALL_ICON = "android.icon";
    public static final String EXTRA_SUB_TEXT = "android.subText";
    public static final String EXTRA_SUMMARY_TEXT = "android.summaryText";
    public static final String EXTRA_TEMPLATE = "android.template";
    public static final String EXTRA_TEXT = "android.text";
    public static final String EXTRA_TEXT_LINES = "android.textLines";
    public static final String EXTRA_TITLE = "android.title";
    public static final String EXTRA_TITLE_BIG = "android.title.big";
    public static final int FLAG_AUTO_CANCEL = 16;
    public static final int FLAG_FOREGROUND_SERVICE = 64;
    public static final int FLAG_GROUP_SUMMARY = 512;
    public static final int FLAG_HIGH_PRIORITY = 128;
    public static final int FLAG_INSISTENT = 4;
    public static final int FLAG_LOCAL_ONLY = 256;
    public static final int FLAG_NO_CLEAR = 32;
    public static final int FLAG_ONGOING_EVENT = 2;
    public static final int FLAG_ONLY_ALERT_ONCE = 8;
    public static final int FLAG_SHOW_LIGHTS = 1;
    public static final int HEADS_UP_ALLOWED = 1;
    public static final int HEADS_UP_NEVER = 0;
    public static final int HEADS_UP_REQUESTED = 2;
    public static final String INTENT_CATEGORY_NOTIFICATION_PREFERENCES = "android.intent.category.NOTIFICATION_PREFERENCES";
    private static final int MAX_CHARSEQUENCE_LENGTH = 5120;
    public static final int PRIORITY_DEFAULT = 0;
    public static final int PRIORITY_HIGH = 1;
    public static final int PRIORITY_LOW = -1;
    public static final int PRIORITY_MAX = 2;
    public static final int PRIORITY_MIN = -2;

    @Deprecated
    public static final int STREAM_DEFAULT = -1;
    private static final String TAG = "Notification";
    public static final int VISIBILITY_PRIVATE = 0;
    public static final int VISIBILITY_PUBLIC = 1;
    public static final int VISIBILITY_SECRET = -1;
    public Action[] actions;
    public AudioAttributes audioAttributes;

    @Deprecated
    public int audioStreamType;
    public RemoteViews bigContentView;
    public String category;
    public int color;
    public PendingIntent contentIntent;
    public RemoteViews contentView;
    public int defaults;
    public PendingIntent deleteIntent;
    public Bundle extras;
    public int flags;
    public PendingIntent fullScreenIntent;
    public RemoteViews headsUpContentView;
    public int icon;
    public int iconLevel;
    public Bitmap largeIcon;
    public int ledARGB;
    public int ledOffMS;
    public int ledOnMS;
    private String mGroupKey;
    private String mSortKey;
    public int number;
    public int priority;
    public Notification publicVersion;
    public Uri sound;
    public CharSequence tickerText;

    @Deprecated
    public RemoteViews tickerView;
    public long[] vibrate;
    public int visibility;
    public long when;
    public static final AudioAttributes AUDIO_ATTRIBUTES_DEFAULT = new AudioAttributes.Builder().setContentType(4).setUsage(5).build();
    public static final Parcelable.Creator<Notification> CREATOR = new Parcelable.Creator<Notification>() {
        @Override
        public Notification createFromParcel(Parcel parcel) {
            return new Notification(parcel);
        }

        @Override
        public Notification[] newArray(int size) {
            return new Notification[size];
        }
    };

    public interface Extender {
        Builder extend(Builder builder);
    }

    public String getGroup() {
        return this.mGroupKey;
    }

    public String getSortKey() {
        return this.mSortKey;
    }

    public static class Action implements Parcelable {
        public static final Parcelable.Creator<Action> CREATOR = new Parcelable.Creator<Action>() {
            @Override
            public Action createFromParcel(Parcel in) {
                return new Action(in);
            }

            @Override
            public Action[] newArray(int size) {
                return new Action[size];
            }
        };
        public PendingIntent actionIntent;
        public int icon;
        private final Bundle mExtras;
        private final RemoteInput[] mRemoteInputs;
        public CharSequence title;

        public interface Extender {
            Builder extend(Builder builder);
        }

        private Action(Parcel in) {
            this.icon = in.readInt();
            this.title = TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(in);
            if (in.readInt() == 1) {
                this.actionIntent = PendingIntent.CREATOR.createFromParcel(in);
            }
            this.mExtras = in.readBundle();
            this.mRemoteInputs = (RemoteInput[]) in.createTypedArray(RemoteInput.CREATOR);
        }

        public Action(int icon, CharSequence title, PendingIntent intent) {
            this(icon, title, intent, new Bundle(), null);
        }

        private Action(int icon, CharSequence title, PendingIntent intent, Bundle extras, RemoteInput[] remoteInputs) {
            this.icon = icon;
            this.title = title;
            this.actionIntent = intent;
            this.mExtras = extras == null ? new Bundle() : extras;
            this.mRemoteInputs = remoteInputs;
        }

        public Bundle getExtras() {
            return this.mExtras;
        }

        public RemoteInput[] getRemoteInputs() {
            return this.mRemoteInputs;
        }

        public static final class Builder {
            private final Bundle mExtras;
            private final int mIcon;
            private final PendingIntent mIntent;
            private ArrayList<RemoteInput> mRemoteInputs;
            private final CharSequence mTitle;

            public Builder(int icon, CharSequence title, PendingIntent intent) {
                this(icon, title, intent, new Bundle(), null);
            }

            public Builder(Action action) {
                this(action.icon, action.title, action.actionIntent, new Bundle(action.mExtras), action.getRemoteInputs());
            }

            private Builder(int icon, CharSequence title, PendingIntent intent, Bundle extras, RemoteInput[] remoteInputs) {
                this.mIcon = icon;
                this.mTitle = title;
                this.mIntent = intent;
                this.mExtras = extras;
                if (remoteInputs != null) {
                    this.mRemoteInputs = new ArrayList<>(remoteInputs.length);
                    Collections.addAll(this.mRemoteInputs, remoteInputs);
                }
            }

            public Builder addExtras(Bundle extras) {
                if (extras != null) {
                    this.mExtras.putAll(extras);
                }
                return this;
            }

            public Bundle getExtras() {
                return this.mExtras;
            }

            public Builder addRemoteInput(RemoteInput remoteInput) {
                if (this.mRemoteInputs == null) {
                    this.mRemoteInputs = new ArrayList<>();
                }
                this.mRemoteInputs.add(remoteInput);
                return this;
            }

            public Builder extend(Extender extender) {
                extender.extend(this);
                return this;
            }

            public Action build() {
                RemoteInput[] remoteInputs = this.mRemoteInputs != null ? (RemoteInput[]) this.mRemoteInputs.toArray(new RemoteInput[this.mRemoteInputs.size()]) : null;
                return new Action(this.mIcon, this.mTitle, this.mIntent, this.mExtras, remoteInputs);
            }
        }

        public Action m9clone() {
            return new Action(this.icon, this.title, this.actionIntent, new Bundle(this.mExtras), getRemoteInputs());
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel out, int flags) {
            out.writeInt(this.icon);
            TextUtils.writeToParcel(this.title, out, flags);
            if (this.actionIntent != null) {
                out.writeInt(1);
                this.actionIntent.writeToParcel(out, flags);
            } else {
                out.writeInt(0);
            }
            out.writeBundle(this.mExtras);
            out.writeTypedArray(this.mRemoteInputs, flags);
        }

        public static final class WearableExtender implements Extender {
            private static final int DEFAULT_FLAGS = 1;
            private static final String EXTRA_WEARABLE_EXTENSIONS = "android.wearable.EXTENSIONS";
            private static final int FLAG_AVAILABLE_OFFLINE = 1;
            private static final String KEY_CANCEL_LABEL = "cancelLabel";
            private static final String KEY_CONFIRM_LABEL = "confirmLabel";
            private static final String KEY_FLAGS = "flags";
            private static final String KEY_IN_PROGRESS_LABEL = "inProgressLabel";
            private CharSequence mCancelLabel;
            private CharSequence mConfirmLabel;
            private int mFlags;
            private CharSequence mInProgressLabel;

            public WearableExtender() {
                this.mFlags = 1;
            }

            public WearableExtender(Action action) {
                this.mFlags = 1;
                Bundle wearableBundle = action.getExtras().getBundle(EXTRA_WEARABLE_EXTENSIONS);
                if (wearableBundle != null) {
                    this.mFlags = wearableBundle.getInt("flags", 1);
                    this.mInProgressLabel = wearableBundle.getCharSequence(KEY_IN_PROGRESS_LABEL);
                    this.mConfirmLabel = wearableBundle.getCharSequence(KEY_CONFIRM_LABEL);
                    this.mCancelLabel = wearableBundle.getCharSequence(KEY_CANCEL_LABEL);
                }
            }

            @Override
            public Builder extend(Builder builder) {
                Bundle wearableBundle = new Bundle();
                if (this.mFlags != 1) {
                    wearableBundle.putInt("flags", this.mFlags);
                }
                if (this.mInProgressLabel != null) {
                    wearableBundle.putCharSequence(KEY_IN_PROGRESS_LABEL, this.mInProgressLabel);
                }
                if (this.mConfirmLabel != null) {
                    wearableBundle.putCharSequence(KEY_CONFIRM_LABEL, this.mConfirmLabel);
                }
                if (this.mCancelLabel != null) {
                    wearableBundle.putCharSequence(KEY_CANCEL_LABEL, this.mCancelLabel);
                }
                builder.getExtras().putBundle(EXTRA_WEARABLE_EXTENSIONS, wearableBundle);
                return builder;
            }

            public WearableExtender m10clone() {
                WearableExtender that = new WearableExtender();
                that.mFlags = this.mFlags;
                that.mInProgressLabel = this.mInProgressLabel;
                that.mConfirmLabel = this.mConfirmLabel;
                that.mCancelLabel = this.mCancelLabel;
                return that;
            }

            public WearableExtender setAvailableOffline(boolean availableOffline) {
                setFlag(1, availableOffline);
                return this;
            }

            public boolean isAvailableOffline() {
                return (this.mFlags & 1) != 0;
            }

            private void setFlag(int mask, boolean value) {
                if (value) {
                    this.mFlags |= mask;
                } else {
                    this.mFlags &= mask ^ (-1);
                }
            }

            public WearableExtender setInProgressLabel(CharSequence label) {
                this.mInProgressLabel = label;
                return this;
            }

            public CharSequence getInProgressLabel() {
                return this.mInProgressLabel;
            }

            public WearableExtender setConfirmLabel(CharSequence label) {
                this.mConfirmLabel = label;
                return this;
            }

            public CharSequence getConfirmLabel() {
                return this.mConfirmLabel;
            }

            public WearableExtender setCancelLabel(CharSequence label) {
                this.mCancelLabel = label;
                return this;
            }

            public CharSequence getCancelLabel() {
                return this.mCancelLabel;
            }
        }
    }

    public Notification() {
        this.audioStreamType = -1;
        this.audioAttributes = AUDIO_ATTRIBUTES_DEFAULT;
        this.color = 0;
        this.extras = new Bundle();
        this.when = System.currentTimeMillis();
        this.priority = 0;
    }

    public Notification(Context context, int icon, CharSequence tickerText, long when, CharSequence contentTitle, CharSequence contentText, Intent contentIntent) {
        this.audioStreamType = -1;
        this.audioAttributes = AUDIO_ATTRIBUTES_DEFAULT;
        this.color = 0;
        this.extras = new Bundle();
        this.when = when;
        this.icon = icon;
        this.tickerText = tickerText;
        setLatestEventInfo(context, contentTitle, contentText, PendingIntent.getActivity(context, 0, contentIntent, 0));
    }

    @Deprecated
    public Notification(int icon, CharSequence tickerText, long when) {
        this.audioStreamType = -1;
        this.audioAttributes = AUDIO_ATTRIBUTES_DEFAULT;
        this.color = 0;
        this.extras = new Bundle();
        this.icon = icon;
        this.tickerText = tickerText;
        this.when = when;
    }

    public Notification(Parcel parcel) {
        this.audioStreamType = -1;
        this.audioAttributes = AUDIO_ATTRIBUTES_DEFAULT;
        this.color = 0;
        this.extras = new Bundle();
        parcel.readInt();
        this.when = parcel.readLong();
        this.icon = parcel.readInt();
        this.number = parcel.readInt();
        if (parcel.readInt() != 0) {
            this.contentIntent = PendingIntent.CREATOR.createFromParcel(parcel);
        }
        if (parcel.readInt() != 0) {
            this.deleteIntent = PendingIntent.CREATOR.createFromParcel(parcel);
        }
        if (parcel.readInt() != 0) {
            this.tickerText = TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(parcel);
        }
        if (parcel.readInt() != 0) {
            this.tickerView = RemoteViews.CREATOR.createFromParcel(parcel);
        }
        if (parcel.readInt() != 0) {
            this.contentView = RemoteViews.CREATOR.createFromParcel(parcel);
        }
        if (parcel.readInt() != 0) {
            this.largeIcon = Bitmap.CREATOR.createFromParcel(parcel);
        }
        this.defaults = parcel.readInt();
        this.flags = parcel.readInt();
        if (parcel.readInt() != 0) {
            this.sound = Uri.CREATOR.createFromParcel(parcel);
        }
        this.audioStreamType = parcel.readInt();
        if (parcel.readInt() != 0) {
            this.audioAttributes = AudioAttributes.CREATOR.createFromParcel(parcel);
        }
        this.vibrate = parcel.createLongArray();
        this.ledARGB = parcel.readInt();
        this.ledOnMS = parcel.readInt();
        this.ledOffMS = parcel.readInt();
        this.iconLevel = parcel.readInt();
        if (parcel.readInt() != 0) {
            this.fullScreenIntent = PendingIntent.CREATOR.createFromParcel(parcel);
        }
        this.priority = parcel.readInt();
        this.category = parcel.readString();
        this.mGroupKey = parcel.readString();
        this.mSortKey = parcel.readString();
        this.extras = parcel.readBundle();
        this.actions = (Action[]) parcel.createTypedArray(Action.CREATOR);
        if (parcel.readInt() != 0) {
            this.bigContentView = RemoteViews.CREATOR.createFromParcel(parcel);
        }
        if (parcel.readInt() != 0) {
            this.headsUpContentView = RemoteViews.CREATOR.createFromParcel(parcel);
        }
        this.visibility = parcel.readInt();
        if (parcel.readInt() != 0) {
            this.publicVersion = CREATOR.createFromParcel(parcel);
        }
        this.color = parcel.readInt();
    }

    public Notification m8clone() {
        Notification that = new Notification();
        cloneInto(that, true);
        return that;
    }

    public void cloneInto(Notification that, boolean heavy) {
        that.when = this.when;
        that.icon = this.icon;
        that.number = this.number;
        that.contentIntent = this.contentIntent;
        that.deleteIntent = this.deleteIntent;
        that.fullScreenIntent = this.fullScreenIntent;
        if (this.tickerText != null) {
            that.tickerText = this.tickerText.toString();
        }
        if (heavy && this.tickerView != null) {
            that.tickerView = this.tickerView.mo11clone();
        }
        if (heavy && this.contentView != null) {
            that.contentView = this.contentView.mo11clone();
        }
        if (heavy && this.largeIcon != null) {
            that.largeIcon = Bitmap.createBitmap(this.largeIcon);
        }
        that.iconLevel = this.iconLevel;
        that.sound = this.sound;
        that.audioStreamType = this.audioStreamType;
        if (this.audioAttributes != null) {
            that.audioAttributes = new AudioAttributes.Builder(this.audioAttributes).build();
        }
        long[] vibrate = this.vibrate;
        if (vibrate != null) {
            int N = vibrate.length;
            long[] vib = new long[N];
            that.vibrate = vib;
            System.arraycopy(vibrate, 0, vib, 0, N);
        }
        that.ledARGB = this.ledARGB;
        that.ledOnMS = this.ledOnMS;
        that.ledOffMS = this.ledOffMS;
        that.defaults = this.defaults;
        that.flags = this.flags;
        that.priority = this.priority;
        that.category = this.category;
        that.mGroupKey = this.mGroupKey;
        that.mSortKey = this.mSortKey;
        if (this.extras != null) {
            try {
                that.extras = new Bundle(this.extras);
                that.extras.size();
            } catch (BadParcelableException e) {
                Log.e(TAG, "could not unparcel extras from notification: " + this, e);
                that.extras = null;
            }
        }
        if (this.actions != null) {
            that.actions = new Action[this.actions.length];
            for (int i = 0; i < this.actions.length; i++) {
                that.actions[i] = this.actions[i].m9clone();
            }
        }
        if (heavy && this.bigContentView != null) {
            that.bigContentView = this.bigContentView.mo11clone();
        }
        if (heavy && this.headsUpContentView != null) {
            that.headsUpContentView = this.headsUpContentView.mo11clone();
        }
        that.visibility = this.visibility;
        if (this.publicVersion != null) {
            that.publicVersion = new Notification();
            this.publicVersion.cloneInto(that.publicVersion, heavy);
        }
        that.color = this.color;
        if (!heavy) {
            that.lightenPayload();
        }
    }

    public final void lightenPayload() {
        this.tickerView = null;
        this.contentView = null;
        this.bigContentView = null;
        this.headsUpContentView = null;
        this.largeIcon = null;
        if (this.extras != null) {
            this.extras.remove(EXTRA_LARGE_ICON);
            this.extras.remove(EXTRA_LARGE_ICON_BIG);
            this.extras.remove(EXTRA_PICTURE);
            this.extras.remove(EXTRA_BIG_TEXT);
            this.extras.remove(Builder.EXTRA_NEEDS_REBUILD);
        }
    }

    public static CharSequence safeCharSequence(CharSequence cs) {
        if (cs != null) {
            if (cs.length() > 5120) {
                cs = cs.subSequence(0, 5120);
            }
            if (cs instanceof Parcelable) {
                Log.e(TAG, "warning: " + cs.getClass().getCanonicalName() + " instance is a custom Parcelable and not allowed in Notification");
                return cs.toString();
            }
            return cs;
        }
        return cs;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int flags) {
        parcel.writeInt(1);
        parcel.writeLong(this.when);
        parcel.writeInt(this.icon);
        parcel.writeInt(this.number);
        if (this.contentIntent != null) {
            parcel.writeInt(1);
            this.contentIntent.writeToParcel(parcel, 0);
        } else {
            parcel.writeInt(0);
        }
        if (this.deleteIntent != null) {
            parcel.writeInt(1);
            this.deleteIntent.writeToParcel(parcel, 0);
        } else {
            parcel.writeInt(0);
        }
        if (this.tickerText != null) {
            parcel.writeInt(1);
            TextUtils.writeToParcel(this.tickerText, parcel, flags);
        } else {
            parcel.writeInt(0);
        }
        if (this.tickerView != null) {
            parcel.writeInt(1);
            this.tickerView.writeToParcel(parcel, 0);
        } else {
            parcel.writeInt(0);
        }
        if (this.contentView != null) {
            parcel.writeInt(1);
            this.contentView.writeToParcel(parcel, 0);
        } else {
            parcel.writeInt(0);
        }
        if (this.largeIcon != null) {
            parcel.writeInt(1);
            this.largeIcon.writeToParcel(parcel, 0);
        } else {
            parcel.writeInt(0);
        }
        parcel.writeInt(this.defaults);
        parcel.writeInt(this.flags);
        if (this.sound != null) {
            parcel.writeInt(1);
            this.sound.writeToParcel(parcel, 0);
        } else {
            parcel.writeInt(0);
        }
        parcel.writeInt(this.audioStreamType);
        if (this.audioAttributes != null) {
            parcel.writeInt(1);
            this.audioAttributes.writeToParcel(parcel, 0);
        } else {
            parcel.writeInt(0);
        }
        parcel.writeLongArray(this.vibrate);
        parcel.writeInt(this.ledARGB);
        parcel.writeInt(this.ledOnMS);
        parcel.writeInt(this.ledOffMS);
        parcel.writeInt(this.iconLevel);
        if (this.fullScreenIntent != null) {
            parcel.writeInt(1);
            this.fullScreenIntent.writeToParcel(parcel, 0);
        } else {
            parcel.writeInt(0);
        }
        parcel.writeInt(this.priority);
        parcel.writeString(this.category);
        parcel.writeString(this.mGroupKey);
        parcel.writeString(this.mSortKey);
        parcel.writeBundle(this.extras);
        parcel.writeTypedArray(this.actions, 0);
        if (this.bigContentView != null) {
            parcel.writeInt(1);
            this.bigContentView.writeToParcel(parcel, 0);
        } else {
            parcel.writeInt(0);
        }
        if (this.headsUpContentView != null) {
            parcel.writeInt(1);
            this.headsUpContentView.writeToParcel(parcel, 0);
        } else {
            parcel.writeInt(0);
        }
        parcel.writeInt(this.visibility);
        if (this.publicVersion != null) {
            parcel.writeInt(1);
            this.publicVersion.writeToParcel(parcel, 0);
        } else {
            parcel.writeInt(0);
        }
        parcel.writeInt(this.color);
    }

    @Deprecated
    public void setLatestEventInfo(Context context, CharSequence contentTitle, CharSequence contentText, PendingIntent contentIntent) {
        Builder builder = new Builder(context);
        builder.setWhen(this.when);
        builder.setSmallIcon(this.icon);
        builder.setPriority(this.priority);
        builder.setTicker(this.tickerText);
        builder.setNumber(this.number);
        builder.setColor(this.color);
        builder.mFlags = this.flags;
        builder.setSound(this.sound, this.audioStreamType);
        builder.setDefaults(this.defaults);
        builder.setVibrate(this.vibrate);
        builder.setDeleteIntent(this.deleteIntent);
        if (contentTitle != null) {
            builder.setContentTitle(contentTitle);
        }
        if (contentText != null) {
            builder.setContentText(contentText);
        }
        builder.setContentIntent(contentIntent);
        builder.buildInto(this);
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Notification(pri=");
        sb.append(this.priority);
        sb.append(" contentView=");
        if (this.contentView != null) {
            sb.append(this.contentView.getPackage());
            sb.append("/0x");
            sb.append(Integer.toHexString(this.contentView.getLayoutId()));
        } else {
            sb.append("null");
        }
        sb.append(" vibrate=");
        if ((this.defaults & 2) != 0) {
            sb.append(PhoneConstants.APN_TYPE_DEFAULT);
        } else if (this.vibrate != null) {
            int N = this.vibrate.length - 1;
            sb.append("[");
            for (int i = 0; i < N; i++) {
                sb.append(this.vibrate[i]);
                sb.append(',');
            }
            if (N != -1) {
                sb.append(this.vibrate[N]);
            }
            sb.append("]");
        } else {
            sb.append("null");
        }
        sb.append(" sound=");
        if ((this.defaults & 1) != 0) {
            sb.append(PhoneConstants.APN_TYPE_DEFAULT);
        } else if (this.sound != null) {
            sb.append(this.sound.toString());
        } else {
            sb.append("null");
        }
        sb.append(" defaults=0x");
        sb.append(Integer.toHexString(this.defaults));
        sb.append(" flags=0x");
        sb.append(Integer.toHexString(this.flags));
        sb.append(String.format(" color=0x%08x", Integer.valueOf(this.color)));
        if (this.category != null) {
            sb.append(" category=");
            sb.append(this.category);
        }
        if (this.mGroupKey != null) {
            sb.append(" groupKey=");
            sb.append(this.mGroupKey);
        }
        if (this.mSortKey != null) {
            sb.append(" sortKey=");
            sb.append(this.mSortKey);
        }
        if (this.actions != null) {
            sb.append(" actions=");
            sb.append(this.actions.length);
        }
        sb.append(" vis=");
        sb.append(visibilityToString(this.visibility));
        if (this.publicVersion != null) {
            sb.append(" publicVersion=");
            sb.append(this.publicVersion.toString());
        }
        sb.append(")");
        return sb.toString();
    }

    public static String visibilityToString(int vis) {
        switch (vis) {
            case -1:
                return "SECRET";
            case 0:
                return "PRIVATE";
            case 1:
                return "PUBLIC";
            default:
                return "UNKNOWN(" + String.valueOf(vis) + ")";
        }
    }

    public boolean isValid() {
        return this.contentView != null || this.extras.getBoolean(Builder.EXTRA_REBUILD_CONTENT_VIEW);
    }

    public boolean isGroupSummary() {
        return (this.mGroupKey == null || (this.flags & 512) == 0) ? false : true;
    }

    public boolean isGroupChild() {
        return this.mGroupKey != null && (this.flags & 512) == 0;
    }

    public static class Builder {
        public static final String EXTRA_NEEDS_REBUILD = "android.rebuild";
        public static final String EXTRA_REBUILD_BIG_CONTENT_VIEW = "android.rebuild.bigView";
        public static final String EXTRA_REBUILD_BIG_CONTENT_VIEW_ACTION_COUNT = "android.rebuild.bigViewActionCount";
        public static final String EXTRA_REBUILD_CONTENT_VIEW = "android.rebuild.contentView";
        public static final String EXTRA_REBUILD_CONTENT_VIEW_ACTION_COUNT = "android.rebuild.contentViewActionCount";
        private static final String EXTRA_REBUILD_CONTEXT_APPLICATION_INFO = "android.rebuild.applicationInfo";
        public static final String EXTRA_REBUILD_HEADS_UP_CONTENT_VIEW = "android.rebuild.hudView";
        public static final String EXTRA_REBUILD_HEADS_UP_CONTENT_VIEW_ACTION_COUNT = "android.rebuild.hudViewActionCount";
        public static final String EXTRA_REBUILD_LARGE_ICON = "android.rebuild.largeIcon";
        private static final float LARGE_TEXT_SCALE = 1.3f;
        private static final int MAX_ACTION_BUTTONS = 3;
        private static final boolean STRIP_AND_REBUILD = true;
        private ArrayList<Action> mActions;
        private AudioAttributes mAudioAttributes;
        private int mAudioStreamType;
        private String mCategory;
        private int mColor;
        private final NotificationColorUtil mColorUtil;
        private CharSequence mContentInfo;
        private PendingIntent mContentIntent;
        private CharSequence mContentText;
        private CharSequence mContentTitle;
        private RemoteViews mContentView;
        private Context mContext;
        private int mDefaults;
        private PendingIntent mDeleteIntent;
        private Bundle mExtras;
        private int mFlags;
        private PendingIntent mFullScreenIntent;
        private String mGroupKey;
        private boolean mHasThreeLines;
        private Bitmap mLargeIcon;
        private int mLedArgb;
        private int mLedOffMs;
        private int mLedOnMs;
        private int mNumber;
        private int mOriginatingUserId;
        private ArrayList<String> mPeople;
        private int mPriority;
        private int mProgress;
        private boolean mProgressIndeterminate;
        private int mProgressMax;
        private Notification mPublicVersion;
        private Bundle mRebuildBundle;
        private Notification mRebuildNotification;
        private boolean mShowWhen;
        private int mSmallIcon;
        private int mSmallIconLevel;
        private String mSortKey;
        private Uri mSound;
        private Style mStyle;
        private CharSequence mSubText;
        private CharSequence mTickerText;
        private RemoteViews mTickerView;
        private boolean mUseChronometer;
        private long[] mVibrate;
        private int mVisibility;
        private long mWhen;

        public Builder(Context context) {
            this.mActions = new ArrayList<>(3);
            this.mShowWhen = true;
            this.mVisibility = 0;
            this.mPublicVersion = null;
            this.mColor = 0;
            this.mRebuildBundle = new Bundle();
            this.mRebuildNotification = null;
            this.mContext = context;
            this.mWhen = System.currentTimeMillis();
            this.mAudioStreamType = -1;
            this.mAudioAttributes = Notification.AUDIO_ATTRIBUTES_DEFAULT;
            this.mPriority = 0;
            this.mPeople = new ArrayList<>();
            this.mColorUtil = context.getApplicationInfo().targetSdkVersion < 21 ? NotificationColorUtil.getInstance(this.mContext) : null;
        }

        private Builder(Context context, Notification n) {
            this(context);
            this.mRebuildNotification = n;
            restoreFromNotification(n);
            Style style = null;
            Bundle extras = n.extras;
            String templateClass = extras.getString(Notification.EXTRA_TEMPLATE);
            if (!TextUtils.isEmpty(templateClass)) {
                Class<? extends Style> styleClass = getNotificationStyleClass(templateClass);
                if (styleClass == null) {
                    Log.d(Notification.TAG, "Unknown style class: " + styleClass);
                    return;
                }
                try {
                    Constructor<? extends Style> constructor = styleClass.getConstructor(new Class[0]);
                    style = constructor.newInstance(new Object[0]);
                    style.restoreFromExtras(extras);
                } catch (Throwable t) {
                    Log.e(Notification.TAG, "Could not create Style", t);
                    return;
                }
            }
            if (style != null) {
                setStyle(style);
            }
        }

        public Builder setWhen(long when) {
            this.mWhen = when;
            return this;
        }

        public Builder setShowWhen(boolean show) {
            this.mShowWhen = show;
            return this;
        }

        public Builder setUsesChronometer(boolean b) {
            this.mUseChronometer = b;
            return this;
        }

        public Builder setSmallIcon(int icon) {
            this.mSmallIcon = icon;
            return this;
        }

        public Builder setSmallIcon(int icon, int level) {
            this.mSmallIcon = icon;
            this.mSmallIconLevel = level;
            return this;
        }

        public Builder setContentTitle(CharSequence title) {
            this.mContentTitle = Notification.safeCharSequence(title);
            return this;
        }

        public Builder setContentText(CharSequence text) {
            this.mContentText = Notification.safeCharSequence(text);
            return this;
        }

        public Builder setSubText(CharSequence text) {
            this.mSubText = Notification.safeCharSequence(text);
            return this;
        }

        public Builder setNumber(int number) {
            this.mNumber = number;
            return this;
        }

        public Builder setContentInfo(CharSequence info) {
            this.mContentInfo = Notification.safeCharSequence(info);
            return this;
        }

        public Builder setProgress(int max, int progress, boolean indeterminate) {
            this.mProgressMax = max;
            this.mProgress = progress;
            this.mProgressIndeterminate = indeterminate;
            return this;
        }

        public Builder setContent(RemoteViews views) {
            this.mContentView = views;
            return this;
        }

        public Builder setContentIntent(PendingIntent intent) {
            this.mContentIntent = intent;
            return this;
        }

        public Builder setDeleteIntent(PendingIntent intent) {
            this.mDeleteIntent = intent;
            return this;
        }

        public Builder setFullScreenIntent(PendingIntent intent, boolean highPriority) {
            this.mFullScreenIntent = intent;
            setFlag(128, highPriority);
            return this;
        }

        public Builder setTicker(CharSequence tickerText) {
            this.mTickerText = Notification.safeCharSequence(tickerText);
            return this;
        }

        @Deprecated
        public Builder setTicker(CharSequence tickerText, RemoteViews views) {
            this.mTickerText = Notification.safeCharSequence(tickerText);
            this.mTickerView = views;
            return this;
        }

        public Builder setLargeIcon(Bitmap icon) {
            this.mLargeIcon = icon;
            return this;
        }

        public Builder setSound(Uri sound) {
            this.mSound = sound;
            this.mAudioAttributes = Notification.AUDIO_ATTRIBUTES_DEFAULT;
            return this;
        }

        @Deprecated
        public Builder setSound(Uri sound, int streamType) {
            this.mSound = sound;
            this.mAudioStreamType = streamType;
            return this;
        }

        public Builder setSound(Uri sound, AudioAttributes audioAttributes) {
            this.mSound = sound;
            this.mAudioAttributes = audioAttributes;
            return this;
        }

        public Builder setVibrate(long[] pattern) {
            this.mVibrate = pattern;
            return this;
        }

        public Builder setLights(int argb, int onMs, int offMs) {
            this.mLedArgb = argb;
            this.mLedOnMs = onMs;
            this.mLedOffMs = offMs;
            return this;
        }

        public Builder setOngoing(boolean ongoing) {
            setFlag(2, ongoing);
            return this;
        }

        public Builder setOnlyAlertOnce(boolean onlyAlertOnce) {
            setFlag(8, onlyAlertOnce);
            return this;
        }

        public Builder setAutoCancel(boolean autoCancel) {
            setFlag(16, autoCancel);
            return this;
        }

        public Builder setLocalOnly(boolean localOnly) {
            setFlag(256, localOnly);
            return this;
        }

        public Builder setDefaults(int defaults) {
            this.mDefaults = defaults;
            return this;
        }

        public Builder setPriority(int pri) {
            this.mPriority = pri;
            return this;
        }

        public Builder setCategory(String category) {
            this.mCategory = category;
            return this;
        }

        public Builder addPerson(String uri) {
            this.mPeople.add(uri);
            return this;
        }

        public Builder setGroup(String groupKey) {
            this.mGroupKey = groupKey;
            return this;
        }

        public Builder setGroupSummary(boolean isGroupSummary) {
            setFlag(512, isGroupSummary);
            return this;
        }

        public Builder setSortKey(String sortKey) {
            this.mSortKey = sortKey;
            return this;
        }

        public Builder addExtras(Bundle extras) {
            if (extras != null) {
                if (this.mExtras == null) {
                    this.mExtras = new Bundle(extras);
                } else {
                    this.mExtras.putAll(extras);
                }
            }
            return this;
        }

        public Builder setExtras(Bundle extras) {
            this.mExtras = extras;
            return this;
        }

        public Bundle getExtras() {
            if (this.mExtras == null) {
                this.mExtras = new Bundle();
            }
            return this.mExtras;
        }

        public Builder addAction(int icon, CharSequence title, PendingIntent intent) {
            this.mActions.add(new Action(icon, Notification.safeCharSequence(title), intent));
            return this;
        }

        public Builder addAction(Action action) {
            this.mActions.add(action);
            return this;
        }

        public Builder setStyle(Style style) {
            if (this.mStyle != style) {
                this.mStyle = style;
                if (this.mStyle != null) {
                    this.mStyle.setBuilder(this);
                }
            }
            return this;
        }

        public Builder setVisibility(int visibility) {
            this.mVisibility = visibility;
            return this;
        }

        public Builder setPublicVersion(Notification n) {
            this.mPublicVersion = n;
            return this;
        }

        public Builder extend(Extender extender) {
            extender.extend(this);
            return this;
        }

        private void setFlag(int mask, boolean value) {
            if (value) {
                this.mFlags |= mask;
            } else {
                this.mFlags &= mask ^ (-1);
            }
        }

        public Builder setColor(int argb) {
            this.mColor = argb;
            return this;
        }

        private Drawable getProfileBadgeDrawable() {
            return this.mContext.getPackageManager().getUserBadgeForDensity(new UserHandle(this.mOriginatingUserId), 0);
        }

        private Bitmap getProfileBadge() {
            Drawable badge = getProfileBadgeDrawable();
            if (badge == null) {
                return null;
            }
            int size = this.mContext.getResources().getDimensionPixelSize(R.dimen.notification_badge_size);
            Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            badge.setBounds(0, 0, size, size);
            badge.draw(canvas);
            return bitmap;
        }

        private boolean addProfileBadge(RemoteViews contentView, int resId) {
            Bitmap profileBadge = getProfileBadge();
            contentView.setViewVisibility(R.id.profile_badge_large_template, 8);
            contentView.setViewVisibility(R.id.profile_badge_line2, 8);
            contentView.setViewVisibility(R.id.profile_badge_line3, 8);
            if (profileBadge == null) {
                return false;
            }
            contentView.setImageViewBitmap(resId, profileBadge);
            contentView.setViewVisibility(resId, 0);
            if (resId == 16909129) {
                contentView.setViewVisibility(R.id.line3, 0);
            }
            return true;
        }

        private void shrinkLine3Text(RemoteViews contentView) {
            float subTextSize = this.mContext.getResources().getDimensionPixelSize(R.dimen.notification_subtext_size);
            contentView.setTextViewTextSize(R.id.text, 0, subTextSize);
        }

        private void unshrinkLine3Text(RemoteViews contentView) {
            float regularTextSize = this.mContext.getResources().getDimensionPixelSize(R.dimen.notification_text_size);
            contentView.setTextViewTextSize(R.id.text, 0, regularTextSize);
        }

        private void resetStandardTemplate(RemoteViews contentView) {
            removeLargeIconBackground(contentView);
            contentView.setViewPadding(16908294, 0, 0, 0, 0);
            contentView.setImageViewResource(16908294, 0);
            contentView.setInt(16908294, "setBackgroundResource", 0);
            contentView.setViewVisibility(R.id.right_icon, 8);
            contentView.setInt(R.id.right_icon, "setBackgroundResource", 0);
            contentView.setImageViewResource(R.id.right_icon, 0);
            contentView.setImageViewResource(16908294, 0);
            contentView.setTextViewText(16908310, null);
            contentView.setTextViewText(R.id.text, null);
            unshrinkLine3Text(contentView);
            contentView.setTextViewText(16908309, null);
            contentView.setViewVisibility(16908309, 8);
            contentView.setViewVisibility(R.id.info, 8);
            contentView.setViewVisibility(R.id.time, 8);
            contentView.setViewVisibility(R.id.line3, 8);
            contentView.setViewVisibility(R.id.overflow_divider, 8);
            contentView.setViewVisibility(16908301, 8);
            contentView.setViewVisibility(R.id.chronometer, 8);
            contentView.setViewVisibility(R.id.time, 8);
        }

        private RemoteViews applyStandardTemplate(int resId) {
            return applyStandardTemplate(resId, true);
        }

        private RemoteViews applyStandardTemplate(int resId, boolean hasProgress) {
            RemoteViews contentView = new BuilderRemoteViews(this.mContext.getApplicationInfo(), resId);
            resetStandardTemplate(contentView);
            boolean showLine3 = false;
            boolean showLine2 = false;
            boolean contentTextInLine2 = false;
            if (this.mLargeIcon != null) {
                contentView.setImageViewBitmap(16908294, this.mLargeIcon);
                processLargeLegacyIcon(this.mLargeIcon, contentView);
                contentView.setImageViewResource(R.id.right_icon, this.mSmallIcon);
                contentView.setViewVisibility(R.id.right_icon, 0);
                processSmallRightIcon(this.mSmallIcon, contentView);
            } else {
                contentView.setImageViewResource(16908294, this.mSmallIcon);
                contentView.setViewVisibility(16908294, 0);
                processSmallIconAsLarge(this.mSmallIcon, contentView);
            }
            if (this.mContentTitle != null) {
                contentView.setTextViewText(16908310, processLegacyText(this.mContentTitle));
            }
            if (this.mContentText != null) {
                contentView.setTextViewText(R.id.text, processLegacyText(this.mContentText));
                showLine3 = true;
            }
            if (this.mContentInfo != null) {
                contentView.setTextViewText(R.id.info, processLegacyText(this.mContentInfo));
                contentView.setViewVisibility(R.id.info, 0);
                showLine3 = true;
            } else if (this.mNumber > 0) {
                int tooBig = this.mContext.getResources().getInteger(17694723);
                if (this.mNumber > tooBig) {
                    contentView.setTextViewText(R.id.info, processLegacyText(this.mContext.getResources().getString(17039383)));
                } else {
                    NumberFormat f = NumberFormat.getIntegerInstance();
                    contentView.setTextViewText(R.id.info, processLegacyText(f.format(this.mNumber)));
                }
                contentView.setViewVisibility(R.id.info, 0);
                showLine3 = true;
            } else {
                contentView.setViewVisibility(R.id.info, 8);
            }
            if (this.mSubText != null) {
                contentView.setTextViewText(R.id.text, processLegacyText(this.mSubText));
                if (this.mContentText != null) {
                    contentView.setTextViewText(16908309, processLegacyText(this.mContentText));
                    contentView.setViewVisibility(16908309, 0);
                    showLine2 = true;
                    contentTextInLine2 = true;
                } else {
                    contentView.setViewVisibility(16908309, 8);
                }
            } else {
                contentView.setViewVisibility(16908309, 8);
                if (hasProgress && (this.mProgressMax != 0 || this.mProgressIndeterminate)) {
                    contentView.setViewVisibility(16908301, 0);
                    contentView.setProgressBar(16908301, this.mProgressMax, this.mProgress, this.mProgressIndeterminate);
                    contentView.setProgressBackgroundTintList(16908301, ColorStateList.valueOf(this.mContext.getResources().getColor(R.color.notification_progress_background_color)));
                    if (this.mColor != 0) {
                        ColorStateList colorStateList = ColorStateList.valueOf(this.mColor);
                        contentView.setProgressTintList(16908301, colorStateList);
                        contentView.setProgressIndeterminateTintList(16908301, colorStateList);
                    }
                    showLine2 = true;
                } else {
                    contentView.setViewVisibility(16908301, 8);
                }
            }
            if (showLine2) {
                shrinkLine3Text(contentView);
            }
            if (showsTimeOrChronometer()) {
                if (this.mUseChronometer) {
                    contentView.setViewVisibility(R.id.chronometer, 0);
                    contentView.setLong(R.id.chronometer, "setBase", this.mWhen + (SystemClock.elapsedRealtime() - System.currentTimeMillis()));
                    contentView.setBoolean(R.id.chronometer, "setStarted", true);
                } else {
                    contentView.setViewVisibility(R.id.time, 0);
                    contentView.setLong(R.id.time, "setTime", this.mWhen);
                }
            }
            contentView.setViewPadding(R.id.line1, 0, calculateTopPadding(this.mContext, this.mHasThreeLines, this.mContext.getResources().getConfiguration().fontScale), 0, 0);
            boolean addedBadge = addProfileBadge(contentView, contentTextInLine2 ? R.id.profile_badge_line2 : R.id.profile_badge_line3);
            if (addedBadge && !contentTextInLine2) {
                showLine3 = true;
            }
            contentView.setViewVisibility(R.id.line3, showLine3 ? 0 : 8);
            contentView.setViewVisibility(R.id.overflow_divider, showLine3 ? 0 : 8);
            return contentView;
        }

        private boolean showsTimeOrChronometer() {
            return this.mWhen != 0 && this.mShowWhen;
        }

        private boolean hasThreeLines() {
            boolean contentTextInLine2 = (this.mSubText == null || this.mContentText == null) ? false : true;
            boolean hasProgress = this.mStyle == null || this.mStyle.hasProgress();
            boolean badgeInLine3 = (getProfileBadgeDrawable() == null || contentTextInLine2) ? false : true;
            boolean hasLine3 = this.mContentText != null || this.mContentInfo != null || this.mNumber > 0 || badgeInLine3;
            boolean hasLine2 = !(this.mSubText == null || this.mContentText == null) || (hasProgress && this.mSubText == null && (this.mProgressMax != 0 || this.mProgressIndeterminate));
            return hasLine2 && hasLine3;
        }

        public static int calculateTopPadding(Context ctx, boolean hasThreeLines, float fontScale) {
            int padding = ctx.getResources().getDimensionPixelSize(hasThreeLines ? R.dimen.notification_top_pad_narrow : R.dimen.notification_top_pad);
            int largePadding = ctx.getResources().getDimensionPixelSize(hasThreeLines ? R.dimen.notification_top_pad_large_text_narrow : R.dimen.notification_top_pad_large_text);
            float largeFactor = (MathUtils.constrain(fontScale, 1.0f, LARGE_TEXT_SCALE) - 1.0f) / 0.29999995f;
            return Math.round(((1.0f - largeFactor) * padding) + (largePadding * largeFactor));
        }

        private void resetStandardTemplateWithActions(RemoteViews big) {
            big.setViewVisibility(R.id.actions, 8);
            big.setViewVisibility(R.id.action_divider, 8);
            big.removeAllViews(R.id.actions);
        }

        private RemoteViews applyStandardTemplateWithActions(int layoutId) {
            RemoteViews big = applyStandardTemplate(layoutId);
            resetStandardTemplateWithActions(big);
            int N = this.mActions.size();
            if (N > 0) {
                big.setViewVisibility(R.id.actions, 0);
                big.setViewVisibility(R.id.action_divider, 0);
                if (N > 3) {
                    N = 3;
                }
                for (int i = 0; i < N; i++) {
                    RemoteViews button = generateActionButton(this.mActions.get(i));
                    big.addView(R.id.actions, button);
                }
            }
            return big;
        }

        private RemoteViews makeContentView() {
            return this.mContentView != null ? this.mContentView : applyStandardTemplate(getBaseLayoutResource());
        }

        private RemoteViews makeTickerView() {
            if (this.mTickerView != null) {
                return this.mTickerView;
            }
            return null;
        }

        private RemoteViews makeBigContentView() {
            if (this.mActions.size() == 0) {
                return null;
            }
            return applyStandardTemplateWithActions(getBigBaseLayoutResource());
        }

        private RemoteViews makeHeadsUpContentView() {
            if (this.mActions.size() == 0) {
                return null;
            }
            return applyStandardTemplateWithActions(getBigBaseLayoutResource());
        }

        private RemoteViews generateActionButton(Action action) {
            boolean tombstone = action.actionIntent == null;
            RemoteViews button = new RemoteViews(this.mContext.getPackageName(), tombstone ? getActionTombstoneLayoutResource() : getActionLayoutResource());
            button.setTextViewCompoundDrawablesRelative(R.id.action0, action.icon, 0, 0, 0);
            button.setTextViewText(R.id.action0, processLegacyText(action.title));
            if (!tombstone) {
                button.setOnClickPendingIntent(R.id.action0, action.actionIntent);
            }
            button.setContentDescription(R.id.action0, action.title);
            processLegacyAction(action, button);
            return button;
        }

        private boolean isLegacy() {
            return this.mColorUtil != null;
        }

        private void processLegacyAction(Action action, RemoteViews button) {
            if (!isLegacy() || this.mColorUtil.isGrayscaleIcon(this.mContext, action.icon)) {
                button.setTextViewCompoundDrawablesRelativeColorFilter(R.id.action0, 0, this.mContext.getResources().getColor(R.color.notification_action_color_filter), PorterDuff.Mode.MULTIPLY);
            }
        }

        private CharSequence processLegacyText(CharSequence charSequence) {
            if (isLegacy()) {
                return this.mColorUtil.invertCharSequenceColors(charSequence);
            }
            return charSequence;
        }

        private void processSmallIconAsLarge(int largeIconId, RemoteViews contentView) {
            if (!isLegacy()) {
                contentView.setDrawableParameters(16908294, false, -1, -1, PorterDuff.Mode.SRC_ATOP, -1);
            }
            if (!isLegacy() || this.mColorUtil.isGrayscaleIcon(this.mContext, largeIconId)) {
                applyLargeIconBackground(contentView);
            }
        }

        private void processLargeLegacyIcon(Bitmap largeIcon, RemoteViews contentView) {
            if (isLegacy() && this.mColorUtil.isGrayscaleIcon(largeIcon)) {
                applyLargeIconBackground(contentView);
            } else {
                removeLargeIconBackground(contentView);
            }
        }

        private void applyLargeIconBackground(RemoteViews contentView) {
            contentView.setInt(16908294, "setBackgroundResource", R.drawable.notification_icon_legacy_bg);
            contentView.setDrawableParameters(16908294, true, -1, resolveColor(), PorterDuff.Mode.SRC_ATOP, -1);
            int padding = this.mContext.getResources().getDimensionPixelSize(R.dimen.notification_large_icon_circle_padding);
            contentView.setViewPadding(16908294, padding, padding, padding, padding);
        }

        private void removeLargeIconBackground(RemoteViews contentView) {
            contentView.setInt(16908294, "setBackgroundResource", 0);
        }

        private void processSmallRightIcon(int smallIconDrawableId, RemoteViews contentView) {
            if (!isLegacy()) {
                contentView.setDrawableParameters(R.id.right_icon, false, -1, -1, PorterDuff.Mode.SRC_ATOP, -1);
            }
            if (!isLegacy() || this.mColorUtil.isGrayscaleIcon(this.mContext, smallIconDrawableId)) {
                contentView.setInt(R.id.right_icon, "setBackgroundResource", R.drawable.notification_icon_legacy_bg);
                contentView.setDrawableParameters(R.id.right_icon, true, -1, resolveColor(), PorterDuff.Mode.SRC_ATOP, -1);
            }
        }

        private int sanitizeColor() {
            if (this.mColor != 0) {
                this.mColor |= -16777216;
            }
            return this.mColor;
        }

        private int resolveColor() {
            return this.mColor == 0 ? this.mContext.getResources().getColor(R.color.notification_icon_bg_color) : this.mColor;
        }

        public Notification buildUnstyled() {
            Notification n = new Notification();
            n.when = this.mWhen;
            n.icon = this.mSmallIcon;
            n.iconLevel = this.mSmallIconLevel;
            n.number = this.mNumber;
            n.color = sanitizeColor();
            setBuilderContentView(n, makeContentView());
            n.contentIntent = this.mContentIntent;
            n.deleteIntent = this.mDeleteIntent;
            n.fullScreenIntent = this.mFullScreenIntent;
            n.tickerText = this.mTickerText;
            n.tickerView = makeTickerView();
            n.largeIcon = this.mLargeIcon;
            n.sound = this.mSound;
            n.audioStreamType = this.mAudioStreamType;
            n.audioAttributes = this.mAudioAttributes;
            n.vibrate = this.mVibrate;
            n.ledARGB = this.mLedArgb;
            n.ledOnMS = this.mLedOnMs;
            n.ledOffMS = this.mLedOffMs;
            n.defaults = this.mDefaults;
            n.flags = this.mFlags;
            setBuilderBigContentView(n, makeBigContentView());
            setBuilderHeadsUpContentView(n, makeHeadsUpContentView());
            if (this.mLedOnMs != 0 || this.mLedOffMs != 0) {
                n.flags |= 1;
            }
            if ((this.mDefaults & 4) != 0) {
                n.flags |= 1;
            }
            n.category = this.mCategory;
            n.mGroupKey = this.mGroupKey;
            n.mSortKey = this.mSortKey;
            n.priority = this.mPriority;
            if (this.mActions.size() > 0) {
                n.actions = new Action[this.mActions.size()];
                this.mActions.toArray(n.actions);
            }
            n.visibility = this.mVisibility;
            if (this.mPublicVersion != null) {
                n.publicVersion = new Notification();
                this.mPublicVersion.cloneInto(n.publicVersion, true);
            }
            return n;
        }

        public void populateExtras(Bundle extras) {
            extras.putInt(Notification.EXTRA_ORIGINATING_USERID, this.mOriginatingUserId);
            extras.putParcelable(EXTRA_REBUILD_CONTEXT_APPLICATION_INFO, this.mContext.getApplicationInfo());
            extras.putCharSequence(Notification.EXTRA_TITLE, this.mContentTitle);
            extras.putCharSequence(Notification.EXTRA_TEXT, this.mContentText);
            extras.putCharSequence(Notification.EXTRA_SUB_TEXT, this.mSubText);
            extras.putCharSequence(Notification.EXTRA_INFO_TEXT, this.mContentInfo);
            extras.putInt(Notification.EXTRA_SMALL_ICON, this.mSmallIcon);
            extras.putInt(Notification.EXTRA_PROGRESS, this.mProgress);
            extras.putInt(Notification.EXTRA_PROGRESS_MAX, this.mProgressMax);
            extras.putBoolean(Notification.EXTRA_PROGRESS_INDETERMINATE, this.mProgressIndeterminate);
            extras.putBoolean(Notification.EXTRA_SHOW_CHRONOMETER, this.mUseChronometer);
            extras.putBoolean(Notification.EXTRA_SHOW_WHEN, this.mShowWhen);
            if (this.mLargeIcon != null) {
                extras.putParcelable(Notification.EXTRA_LARGE_ICON, this.mLargeIcon);
            }
            if (!this.mPeople.isEmpty()) {
                extras.putStringArray(Notification.EXTRA_PEOPLE, (String[]) this.mPeople.toArray(new String[this.mPeople.size()]));
            }
        }

        public static void stripForDelivery(Notification n) {
            String templateClass = n.extras.getString(Notification.EXTRA_TEMPLATE);
            boolean stripViews = TextUtils.isEmpty(templateClass) || getNotificationStyleClass(templateClass) != null;
            boolean isStripped = false;
            if (n.largeIcon != null && n.extras.containsKey(Notification.EXTRA_LARGE_ICON)) {
                n.largeIcon = null;
                n.extras.putBoolean(EXTRA_REBUILD_LARGE_ICON, true);
                isStripped = true;
            }
            if (stripViews && (n.contentView instanceof BuilderRemoteViews) && n.extras.getInt(EXTRA_REBUILD_CONTENT_VIEW_ACTION_COUNT, -1) == n.contentView.getSequenceNumber()) {
                n.contentView = null;
                n.extras.putBoolean(EXTRA_REBUILD_CONTENT_VIEW, true);
                n.extras.remove(EXTRA_REBUILD_CONTENT_VIEW_ACTION_COUNT);
                isStripped = true;
            }
            if (stripViews && (n.bigContentView instanceof BuilderRemoteViews) && n.extras.getInt(EXTRA_REBUILD_BIG_CONTENT_VIEW_ACTION_COUNT, -1) == n.bigContentView.getSequenceNumber()) {
                n.bigContentView = null;
                n.extras.putBoolean(EXTRA_REBUILD_BIG_CONTENT_VIEW, true);
                n.extras.remove(EXTRA_REBUILD_BIG_CONTENT_VIEW_ACTION_COUNT);
                isStripped = true;
            }
            if (stripViews && (n.headsUpContentView instanceof BuilderRemoteViews) && n.extras.getInt(EXTRA_REBUILD_HEADS_UP_CONTENT_VIEW_ACTION_COUNT, -1) == n.headsUpContentView.getSequenceNumber()) {
                n.headsUpContentView = null;
                n.extras.putBoolean(EXTRA_REBUILD_HEADS_UP_CONTENT_VIEW, true);
                n.extras.remove(EXTRA_REBUILD_HEADS_UP_CONTENT_VIEW_ACTION_COUNT);
                isStripped = true;
            }
            if (isStripped) {
                n.extras.putBoolean(EXTRA_NEEDS_REBUILD, true);
            }
        }

        public static Notification rebuild(Context context, Notification n) {
            Context builderContext;
            Bundle extras = n.extras;
            if (extras.getBoolean(EXTRA_NEEDS_REBUILD)) {
                extras.remove(EXTRA_NEEDS_REBUILD);
                ApplicationInfo applicationInfo = (ApplicationInfo) extras.getParcelable(EXTRA_REBUILD_CONTEXT_APPLICATION_INFO);
                try {
                    builderContext = context.createApplicationContext(applicationInfo, 4);
                } catch (PackageManager.NameNotFoundException e) {
                    Log.e(Notification.TAG, "ApplicationInfo " + applicationInfo + " not found");
                    builderContext = context;
                }
                Builder b = new Builder(builderContext, n);
                return b.rebuild();
            }
            return n;
        }

        private Notification rebuild() {
            if (this.mRebuildNotification == null) {
                throw new IllegalStateException("rebuild() only valid when in 'rebuild' mode.");
            }
            this.mHasThreeLines = hasThreeLines();
            Bundle extras = this.mRebuildNotification.extras;
            if (extras.getBoolean(EXTRA_REBUILD_LARGE_ICON)) {
                this.mRebuildNotification.largeIcon = (Bitmap) extras.getParcelable(Notification.EXTRA_LARGE_ICON);
            }
            extras.remove(EXTRA_REBUILD_LARGE_ICON);
            if (extras.getBoolean(EXTRA_REBUILD_CONTENT_VIEW)) {
                setBuilderContentView(this.mRebuildNotification, makeContentView());
                if (this.mStyle != null) {
                    this.mStyle.populateContentView(this.mRebuildNotification);
                }
            }
            extras.remove(EXTRA_REBUILD_CONTENT_VIEW);
            if (extras.getBoolean(EXTRA_REBUILD_BIG_CONTENT_VIEW)) {
                setBuilderBigContentView(this.mRebuildNotification, makeBigContentView());
                if (this.mStyle != null) {
                    this.mStyle.populateBigContentView(this.mRebuildNotification);
                }
            }
            extras.remove(EXTRA_REBUILD_BIG_CONTENT_VIEW);
            if (extras.getBoolean(EXTRA_REBUILD_HEADS_UP_CONTENT_VIEW)) {
                setBuilderHeadsUpContentView(this.mRebuildNotification, makeHeadsUpContentView());
                if (this.mStyle != null) {
                    this.mStyle.populateHeadsUpContentView(this.mRebuildNotification);
                }
            }
            extras.remove(EXTRA_REBUILD_HEADS_UP_CONTENT_VIEW);
            this.mHasThreeLines = false;
            return this.mRebuildNotification;
        }

        private static Class<? extends Style> getNotificationStyleClass(String templateClass) {
            Class<? extends Style>[] classes = {BigTextStyle.class, BigPictureStyle.class, InboxStyle.class, MediaStyle.class};
            for (Class<? extends Style> innerClass : classes) {
                if (templateClass.equals(innerClass.getName())) {
                    return innerClass;
                }
            }
            return null;
        }

        private void setBuilderContentView(Notification n, RemoteViews contentView) {
            n.contentView = contentView;
            if (contentView instanceof BuilderRemoteViews) {
                this.mRebuildBundle.putInt(EXTRA_REBUILD_CONTENT_VIEW_ACTION_COUNT, contentView.getSequenceNumber());
            }
        }

        private void setBuilderBigContentView(Notification n, RemoteViews bigContentView) {
            n.bigContentView = bigContentView;
            if (bigContentView instanceof BuilderRemoteViews) {
                this.mRebuildBundle.putInt(EXTRA_REBUILD_BIG_CONTENT_VIEW_ACTION_COUNT, bigContentView.getSequenceNumber());
            }
        }

        private void setBuilderHeadsUpContentView(Notification n, RemoteViews headsUpContentView) {
            n.headsUpContentView = headsUpContentView;
            if (headsUpContentView instanceof BuilderRemoteViews) {
                this.mRebuildBundle.putInt(EXTRA_REBUILD_HEADS_UP_CONTENT_VIEW_ACTION_COUNT, headsUpContentView.getSequenceNumber());
            }
        }

        private void restoreFromNotification(Notification n) {
            this.mWhen = n.when;
            this.mSmallIcon = n.icon;
            this.mSmallIconLevel = n.iconLevel;
            this.mNumber = n.number;
            this.mColor = n.color;
            this.mContentView = n.contentView;
            this.mDeleteIntent = n.deleteIntent;
            this.mFullScreenIntent = n.fullScreenIntent;
            this.mTickerText = n.tickerText;
            this.mTickerView = n.tickerView;
            this.mLargeIcon = n.largeIcon;
            this.mSound = n.sound;
            this.mAudioStreamType = n.audioStreamType;
            this.mAudioAttributes = n.audioAttributes;
            this.mVibrate = n.vibrate;
            this.mLedArgb = n.ledARGB;
            this.mLedOnMs = n.ledOnMS;
            this.mLedOffMs = n.ledOffMS;
            this.mDefaults = n.defaults;
            this.mFlags = n.flags;
            this.mCategory = n.category;
            this.mGroupKey = n.mGroupKey;
            this.mSortKey = n.mSortKey;
            this.mPriority = n.priority;
            this.mActions.clear();
            if (n.actions != null) {
                Collections.addAll(this.mActions, n.actions);
            }
            this.mVisibility = n.visibility;
            this.mPublicVersion = n.publicVersion;
            Bundle extras = n.extras;
            this.mOriginatingUserId = extras.getInt(Notification.EXTRA_ORIGINATING_USERID);
            this.mContentTitle = extras.getCharSequence(Notification.EXTRA_TITLE);
            this.mContentText = extras.getCharSequence(Notification.EXTRA_TEXT);
            this.mSubText = extras.getCharSequence(Notification.EXTRA_SUB_TEXT);
            this.mContentInfo = extras.getCharSequence(Notification.EXTRA_INFO_TEXT);
            this.mSmallIcon = extras.getInt(Notification.EXTRA_SMALL_ICON);
            this.mProgress = extras.getInt(Notification.EXTRA_PROGRESS);
            this.mProgressMax = extras.getInt(Notification.EXTRA_PROGRESS_MAX);
            this.mProgressIndeterminate = extras.getBoolean(Notification.EXTRA_PROGRESS_INDETERMINATE);
            this.mUseChronometer = extras.getBoolean(Notification.EXTRA_SHOW_CHRONOMETER);
            this.mShowWhen = extras.getBoolean(Notification.EXTRA_SHOW_WHEN);
            if (extras.containsKey(Notification.EXTRA_LARGE_ICON)) {
                this.mLargeIcon = (Bitmap) extras.getParcelable(Notification.EXTRA_LARGE_ICON);
            }
            if (extras.containsKey(Notification.EXTRA_PEOPLE)) {
                this.mPeople.clear();
                Collections.addAll(this.mPeople, extras.getStringArray(Notification.EXTRA_PEOPLE));
            }
        }

        @Deprecated
        public Notification getNotification() {
            return build();
        }

        public Notification build() {
            this.mOriginatingUserId = this.mContext.getUserId();
            this.mHasThreeLines = hasThreeLines();
            Notification n = buildUnstyled();
            if (this.mStyle != null) {
                n = this.mStyle.buildStyled(n);
            }
            if (this.mExtras != null) {
                n.extras.putAll(this.mExtras);
            }
            if (this.mRebuildBundle.size() > 0) {
                n.extras.putAll(this.mRebuildBundle);
                this.mRebuildBundle.clear();
            }
            populateExtras(n.extras);
            if (this.mStyle != null) {
                this.mStyle.addExtras(n.extras);
            }
            this.mHasThreeLines = false;
            return n;
        }

        public Notification buildInto(Notification n) {
            build().cloneInto(n, true);
            return n;
        }

        private int getBaseLayoutResource() {
            return R.layout.notification_template_material_base;
        }

        private int getBigBaseLayoutResource() {
            return R.layout.notification_template_material_big_base;
        }

        private int getBigPictureLayoutResource() {
            return R.layout.notification_template_material_big_picture;
        }

        private int getBigTextLayoutResource() {
            return R.layout.notification_template_material_big_text;
        }

        private int getInboxLayoutResource() {
            return R.layout.notification_template_material_inbox;
        }

        private int getActionLayoutResource() {
            return R.layout.notification_material_action;
        }

        private int getActionTombstoneLayoutResource() {
            return R.layout.notification_material_action_tombstone;
        }
    }

    public static abstract class Style {
        private CharSequence mBigContentTitle;
        protected Builder mBuilder;
        protected CharSequence mSummaryText = null;
        protected boolean mSummaryTextSet = false;

        protected void internalSetBigContentTitle(CharSequence title) {
            this.mBigContentTitle = title;
        }

        protected void internalSetSummaryText(CharSequence cs) {
            this.mSummaryText = cs;
            this.mSummaryTextSet = true;
        }

        public void setBuilder(Builder builder) {
            if (this.mBuilder != builder) {
                this.mBuilder = builder;
                if (this.mBuilder != null) {
                    this.mBuilder.setStyle(this);
                }
            }
        }

        protected void checkBuilder() {
            if (this.mBuilder == null) {
                throw new IllegalArgumentException("Style requires a valid Builder object");
            }
        }

        protected RemoteViews getStandardView(int layoutId) {
            checkBuilder();
            CharSequence oldBuilderContentTitle = this.mBuilder.mContentTitle;
            if (this.mBigContentTitle != null) {
                this.mBuilder.setContentTitle(this.mBigContentTitle);
            }
            RemoteViews contentView = this.mBuilder.applyStandardTemplateWithActions(layoutId);
            this.mBuilder.mContentTitle = oldBuilderContentTitle;
            if (this.mBigContentTitle != null && this.mBigContentTitle.equals(ProxyInfo.LOCAL_EXCL_LIST)) {
                contentView.setViewVisibility(R.id.line1, 8);
            } else {
                contentView.setViewVisibility(R.id.line1, 0);
            }
            CharSequence overflowText = this.mSummaryTextSet ? this.mSummaryText : this.mBuilder.mSubText;
            if (overflowText != null) {
                contentView.setTextViewText(R.id.text, this.mBuilder.processLegacyText(overflowText));
                contentView.setViewVisibility(R.id.overflow_divider, 0);
                contentView.setViewVisibility(R.id.line3, 0);
            } else {
                contentView.setTextViewText(R.id.text, ProxyInfo.LOCAL_EXCL_LIST);
                contentView.setViewVisibility(R.id.overflow_divider, 8);
                contentView.setViewVisibility(R.id.line3, 8);
            }
            return contentView;
        }

        protected void applyTopPadding(RemoteViews contentView) {
            int topPadding = Builder.calculateTopPadding(this.mBuilder.mContext, this.mBuilder.mHasThreeLines, this.mBuilder.mContext.getResources().getConfiguration().fontScale);
            contentView.setViewPadding(R.id.line1, 0, topPadding, 0, 0);
        }

        public void addExtras(Bundle extras) {
            if (this.mSummaryTextSet) {
                extras.putCharSequence(Notification.EXTRA_SUMMARY_TEXT, this.mSummaryText);
            }
            if (this.mBigContentTitle != null) {
                extras.putCharSequence(Notification.EXTRA_TITLE_BIG, this.mBigContentTitle);
            }
            extras.putString(Notification.EXTRA_TEMPLATE, getClass().getName());
        }

        protected void restoreFromExtras(Bundle extras) {
            if (extras.containsKey(Notification.EXTRA_SUMMARY_TEXT)) {
                this.mSummaryText = extras.getCharSequence(Notification.EXTRA_SUMMARY_TEXT);
                this.mSummaryTextSet = true;
            }
            if (extras.containsKey(Notification.EXTRA_TITLE_BIG)) {
                this.mBigContentTitle = extras.getCharSequence(Notification.EXTRA_TITLE_BIG);
            }
        }

        public Notification buildStyled(Notification wip) {
            populateTickerView(wip);
            populateContentView(wip);
            populateBigContentView(wip);
            populateHeadsUpContentView(wip);
            return wip;
        }

        protected void populateTickerView(Notification wip) {
        }

        protected void populateContentView(Notification wip) {
        }

        protected void populateBigContentView(Notification wip) {
        }

        protected void populateHeadsUpContentView(Notification wip) {
        }

        public Notification build() {
            checkBuilder();
            return this.mBuilder.build();
        }

        protected boolean hasProgress() {
            return true;
        }
    }

    public static class BigPictureStyle extends Style {
        private Bitmap mBigLargeIcon;
        private boolean mBigLargeIconSet = false;
        private Bitmap mPicture;

        public BigPictureStyle() {
        }

        public BigPictureStyle(Builder builder) {
            setBuilder(builder);
        }

        public BigPictureStyle setBigContentTitle(CharSequence title) {
            internalSetBigContentTitle(Notification.safeCharSequence(title));
            return this;
        }

        public BigPictureStyle setSummaryText(CharSequence cs) {
            internalSetSummaryText(Notification.safeCharSequence(cs));
            return this;
        }

        public BigPictureStyle bigPicture(Bitmap b) {
            this.mPicture = b;
            return this;
        }

        public BigPictureStyle bigLargeIcon(Bitmap b) {
            this.mBigLargeIconSet = true;
            this.mBigLargeIcon = b;
            return this;
        }

        private RemoteViews makeBigContentView() {
            Bitmap oldLargeIcon = null;
            if (this.mBigLargeIconSet) {
                oldLargeIcon = this.mBuilder.mLargeIcon;
                this.mBuilder.mLargeIcon = this.mBigLargeIcon;
            }
            RemoteViews contentView = getStandardView(this.mBuilder.getBigPictureLayoutResource());
            if (this.mBigLargeIconSet) {
                this.mBuilder.mLargeIcon = oldLargeIcon;
            }
            contentView.setImageViewBitmap(R.id.big_picture, this.mPicture);
            applyTopPadding(contentView);
            boolean twoTextLines = (this.mBuilder.mSubText == null || this.mBuilder.mContentText == null) ? false : true;
            this.mBuilder.addProfileBadge(contentView, twoTextLines ? R.id.profile_badge_line2 : R.id.profile_badge_line3);
            return contentView;
        }

        @Override
        public void addExtras(Bundle extras) {
            super.addExtras(extras);
            if (this.mBigLargeIconSet) {
                extras.putParcelable(Notification.EXTRA_LARGE_ICON_BIG, this.mBigLargeIcon);
            }
            extras.putParcelable(Notification.EXTRA_PICTURE, this.mPicture);
        }

        @Override
        protected void restoreFromExtras(Bundle extras) {
            super.restoreFromExtras(extras);
            if (extras.containsKey(Notification.EXTRA_LARGE_ICON_BIG)) {
                this.mBigLargeIconSet = true;
                this.mBigLargeIcon = (Bitmap) extras.getParcelable(Notification.EXTRA_LARGE_ICON_BIG);
            }
            this.mPicture = (Bitmap) extras.getParcelable(Notification.EXTRA_PICTURE);
        }

        @Override
        public void populateBigContentView(Notification wip) {
            this.mBuilder.setBuilderBigContentView(wip, makeBigContentView());
        }
    }

    public static class BigTextStyle extends Style {
        private static final int LINES_CONSUMED_BY_ACTIONS = 3;
        private static final int LINES_CONSUMED_BY_SUMMARY = 2;
        private static final int MAX_LINES = 13;
        private CharSequence mBigText;

        public BigTextStyle() {
        }

        public BigTextStyle(Builder builder) {
            setBuilder(builder);
        }

        public BigTextStyle setBigContentTitle(CharSequence title) {
            internalSetBigContentTitle(Notification.safeCharSequence(title));
            return this;
        }

        public BigTextStyle setSummaryText(CharSequence cs) {
            internalSetSummaryText(Notification.safeCharSequence(cs));
            return this;
        }

        public BigTextStyle bigText(CharSequence cs) {
            this.mBigText = Notification.safeCharSequence(cs);
            return this;
        }

        @Override
        public void addExtras(Bundle extras) {
            super.addExtras(extras);
            extras.putCharSequence(Notification.EXTRA_BIG_TEXT, this.mBigText);
        }

        @Override
        protected void restoreFromExtras(Bundle extras) {
            super.restoreFromExtras(extras);
            this.mBigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT);
        }

        private RemoteViews makeBigContentView() {
            CharSequence oldBuilderContentText = this.mBuilder.mContentText;
            this.mBuilder.mContentText = null;
            RemoteViews contentView = getStandardView(this.mBuilder.getBigTextLayoutResource());
            this.mBuilder.mContentText = oldBuilderContentText;
            contentView.setTextViewText(R.id.big_text, this.mBuilder.processLegacyText(this.mBigText));
            contentView.setViewVisibility(R.id.big_text, 0);
            contentView.setInt(R.id.big_text, "setMaxLines", calculateMaxLines());
            contentView.setViewVisibility(16908309, 8);
            applyTopPadding(contentView);
            this.mBuilder.shrinkLine3Text(contentView);
            this.mBuilder.addProfileBadge(contentView, R.id.profile_badge_large_template);
            return contentView;
        }

        private int calculateMaxLines() {
            int lineCount = 13;
            boolean hasActions = this.mBuilder.mActions.size() > 0;
            boolean hasSummary = (this.mSummaryTextSet ? this.mSummaryText : this.mBuilder.mSubText) != null;
            if (hasActions) {
                lineCount = 13 - 3;
            }
            if (hasSummary) {
                lineCount -= 2;
            }
            if (!this.mBuilder.mHasThreeLines) {
                return lineCount - 1;
            }
            return lineCount;
        }

        @Override
        public void populateBigContentView(Notification wip) {
            this.mBuilder.setBuilderBigContentView(wip, makeBigContentView());
        }
    }

    public static class InboxStyle extends Style {
        private ArrayList<CharSequence> mTexts = new ArrayList<>(5);

        public InboxStyle() {
        }

        public InboxStyle(Builder builder) {
            setBuilder(builder);
        }

        public InboxStyle setBigContentTitle(CharSequence title) {
            internalSetBigContentTitle(Notification.safeCharSequence(title));
            return this;
        }

        public InboxStyle setSummaryText(CharSequence cs) {
            internalSetSummaryText(Notification.safeCharSequence(cs));
            return this;
        }

        public InboxStyle addLine(CharSequence cs) {
            this.mTexts.add(Notification.safeCharSequence(cs));
            return this;
        }

        @Override
        public void addExtras(Bundle extras) {
            super.addExtras(extras);
            CharSequence[] a = new CharSequence[this.mTexts.size()];
            extras.putCharSequenceArray(Notification.EXTRA_TEXT_LINES, (CharSequence[]) this.mTexts.toArray(a));
        }

        @Override
        protected void restoreFromExtras(Bundle extras) {
            super.restoreFromExtras(extras);
            this.mTexts.clear();
            if (extras.containsKey(Notification.EXTRA_TEXT_LINES)) {
                Collections.addAll(this.mTexts, extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES));
            }
        }

        private RemoteViews makeBigContentView() {
            CharSequence oldBuilderContentText = this.mBuilder.mContentText;
            this.mBuilder.mContentText = null;
            RemoteViews contentView = getStandardView(this.mBuilder.getInboxLayoutResource());
            this.mBuilder.mContentText = oldBuilderContentText;
            contentView.setViewVisibility(16908309, 8);
            int[] rowIds = {R.id.inbox_text0, R.id.inbox_text1, R.id.inbox_text2, R.id.inbox_text3, R.id.inbox_text4, R.id.inbox_text5, R.id.inbox_text6};
            for (int rowId : rowIds) {
                contentView.setViewVisibility(rowId, 8);
            }
            boolean largeText = this.mBuilder.mContext.getResources().getConfiguration().fontScale > 1.0f;
            float subTextSize = this.mBuilder.mContext.getResources().getDimensionPixelSize(R.dimen.notification_subtext_size);
            for (int i = 0; i < this.mTexts.size() && i < rowIds.length; i++) {
                CharSequence str = this.mTexts.get(i);
                if (str != null && !str.equals(ProxyInfo.LOCAL_EXCL_LIST)) {
                    contentView.setViewVisibility(rowIds[i], 0);
                    contentView.setTextViewText(rowIds[i], this.mBuilder.processLegacyText(str));
                    if (largeText) {
                        contentView.setTextViewTextSize(rowIds[i], 0, subTextSize);
                    }
                }
            }
            contentView.setViewVisibility(R.id.inbox_end_pad, this.mTexts.size() > 0 ? 0 : 8);
            contentView.setViewVisibility(R.id.inbox_more, this.mTexts.size() > rowIds.length ? 0 : 8);
            applyTopPadding(contentView);
            this.mBuilder.shrinkLine3Text(contentView);
            this.mBuilder.addProfileBadge(contentView, R.id.profile_badge_large_template);
            return contentView;
        }

        @Override
        public void populateBigContentView(Notification wip) {
            this.mBuilder.setBuilderBigContentView(wip, makeBigContentView());
        }
    }

    public static class MediaStyle extends Style {
        static final int MAX_MEDIA_BUTTONS = 5;
        static final int MAX_MEDIA_BUTTONS_IN_COMPACT = 3;
        private int[] mActionsToShowInCompact = null;
        private MediaSession.Token mToken;

        public MediaStyle() {
        }

        public MediaStyle(Builder builder) {
            setBuilder(builder);
        }

        public MediaStyle setShowActionsInCompactView(int... actions) {
            this.mActionsToShowInCompact = actions;
            return this;
        }

        public MediaStyle setMediaSession(MediaSession.Token token) {
            this.mToken = token;
            return this;
        }

        @Override
        public Notification buildStyled(Notification wip) {
            super.buildStyled(wip);
            if (wip.category == null) {
                wip.category = Notification.CATEGORY_TRANSPORT;
            }
            return wip;
        }

        @Override
        public void populateContentView(Notification wip) {
            this.mBuilder.setBuilderContentView(wip, makeMediaContentView());
        }

        @Override
        public void populateBigContentView(Notification wip) {
            this.mBuilder.setBuilderBigContentView(wip, makeMediaBigContentView());
        }

        @Override
        public void addExtras(Bundle extras) {
            super.addExtras(extras);
            if (this.mToken != null) {
                extras.putParcelable(Notification.EXTRA_MEDIA_SESSION, this.mToken);
            }
            if (this.mActionsToShowInCompact != null) {
                extras.putIntArray(Notification.EXTRA_COMPACT_ACTIONS, this.mActionsToShowInCompact);
            }
        }

        @Override
        protected void restoreFromExtras(Bundle extras) {
            super.restoreFromExtras(extras);
            if (extras.containsKey(Notification.EXTRA_MEDIA_SESSION)) {
                this.mToken = (MediaSession.Token) extras.getParcelable(Notification.EXTRA_MEDIA_SESSION);
            }
            if (extras.containsKey(Notification.EXTRA_COMPACT_ACTIONS)) {
                this.mActionsToShowInCompact = extras.getIntArray(Notification.EXTRA_COMPACT_ACTIONS);
            }
        }

        private RemoteViews generateMediaActionButton(Action action) {
            boolean tombstone = action.actionIntent == null;
            RemoteViews button = new RemoteViews(this.mBuilder.mContext.getPackageName(), R.layout.notification_material_media_action);
            button.setImageViewResource(R.id.action0, action.icon);
            button.setDrawableParameters(R.id.action0, false, -1, -1, PorterDuff.Mode.SRC_ATOP, -1);
            if (!tombstone) {
                button.setOnClickPendingIntent(R.id.action0, action.actionIntent);
            }
            button.setContentDescription(R.id.action0, action.title);
            return button;
        }

        private RemoteViews makeMediaContentView() {
            RemoteViews view = this.mBuilder.applyStandardTemplate(R.layout.notification_template_material_media, false);
            int numActions = this.mBuilder.mActions.size();
            int N = this.mActionsToShowInCompact == null ? 0 : Math.min(this.mActionsToShowInCompact.length, 3);
            if (N > 0) {
                view.removeAllViews(R.id.media_actions);
                for (int i = 0; i < N; i++) {
                    if (i < numActions) {
                        Action action = (Action) this.mBuilder.mActions.get(this.mActionsToShowInCompact[i]);
                        RemoteViews button = generateMediaActionButton(action);
                        view.addView(R.id.media_actions, button);
                    } else {
                        throw new IllegalArgumentException(String.format("setShowActionsInCompactView: action %d out of bounds (max %d)", Integer.valueOf(i), Integer.valueOf(numActions - 1)));
                    }
                }
            }
            styleText(view);
            hideRightIcon(view);
            return view;
        }

        private RemoteViews makeMediaBigContentView() {
            int actionCount = Math.min(this.mBuilder.mActions.size(), 5);
            RemoteViews big = this.mBuilder.applyStandardTemplate(getBigLayoutResource(actionCount), false);
            if (actionCount > 0) {
                big.removeAllViews(R.id.media_actions);
                for (int i = 0; i < actionCount; i++) {
                    RemoteViews button = generateMediaActionButton((Action) this.mBuilder.mActions.get(i));
                    big.addView(R.id.media_actions, button);
                }
            }
            styleText(big);
            hideRightIcon(big);
            applyTopPadding(big);
            big.setViewVisibility(16908301, 8);
            return big;
        }

        private int getBigLayoutResource(int actionCount) {
            return actionCount <= 3 ? R.layout.notification_template_material_big_media_narrow : R.layout.notification_template_material_big_media;
        }

        private void hideRightIcon(RemoteViews contentView) {
            contentView.setViewVisibility(R.id.right_icon, 8);
        }

        private void styleText(RemoteViews contentView) {
            int primaryColor = this.mBuilder.mContext.getResources().getColor(R.color.notification_media_primary_color);
            int secondaryColor = this.mBuilder.mContext.getResources().getColor(R.color.notification_media_secondary_color);
            contentView.setTextColor(16908310, primaryColor);
            if (this.mBuilder.showsTimeOrChronometer()) {
                if (this.mBuilder.mUseChronometer) {
                    contentView.setTextColor(R.id.chronometer, secondaryColor);
                } else {
                    contentView.setTextColor(R.id.time, secondaryColor);
                }
            }
            contentView.setTextColor(16908309, secondaryColor);
            contentView.setTextColor(R.id.text, secondaryColor);
            contentView.setTextColor(R.id.info, secondaryColor);
        }

        @Override
        protected boolean hasProgress() {
            return false;
        }
    }

    public static final class WearableExtender implements Extender {
        private static final int DEFAULT_CONTENT_ICON_GRAVITY = 8388613;
        private static final int DEFAULT_FLAGS = 1;
        private static final int DEFAULT_GRAVITY = 80;
        private static final String EXTRA_WEARABLE_EXTENSIONS = "android.wearable.EXTENSIONS";
        private static final int FLAG_CONTENT_INTENT_AVAILABLE_OFFLINE = 1;
        private static final int FLAG_HINT_AVOID_BACKGROUND_CLIPPING = 16;
        private static final int FLAG_HINT_HIDE_ICON = 2;
        private static final int FLAG_HINT_SHOW_BACKGROUND_ONLY = 4;
        private static final int FLAG_START_SCROLL_BOTTOM = 8;
        private static final String KEY_ACTIONS = "actions";
        private static final String KEY_BACKGROUND = "background";
        private static final String KEY_CONTENT_ACTION_INDEX = "contentActionIndex";
        private static final String KEY_CONTENT_ICON = "contentIcon";
        private static final String KEY_CONTENT_ICON_GRAVITY = "contentIconGravity";
        private static final String KEY_CUSTOM_CONTENT_HEIGHT = "customContentHeight";
        private static final String KEY_CUSTOM_SIZE_PRESET = "customSizePreset";
        private static final String KEY_DISPLAY_INTENT = "displayIntent";
        private static final String KEY_FLAGS = "flags";
        private static final String KEY_GRAVITY = "gravity";
        private static final String KEY_HINT_SCREEN_TIMEOUT = "hintScreenTimeout";
        private static final String KEY_PAGES = "pages";
        public static final int SCREEN_TIMEOUT_LONG = -1;
        public static final int SCREEN_TIMEOUT_SHORT = 0;
        public static final int SIZE_DEFAULT = 0;
        public static final int SIZE_FULL_SCREEN = 5;
        public static final int SIZE_LARGE = 4;
        public static final int SIZE_MEDIUM = 3;
        public static final int SIZE_SMALL = 2;
        public static final int SIZE_XSMALL = 1;
        public static final int UNSET_ACTION_INDEX = -1;
        private ArrayList<Action> mActions;
        private Bitmap mBackground;
        private int mContentActionIndex;
        private int mContentIcon;
        private int mContentIconGravity;
        private int mCustomContentHeight;
        private int mCustomSizePreset;
        private PendingIntent mDisplayIntent;
        private int mFlags;
        private int mGravity;
        private int mHintScreenTimeout;
        private ArrayList<Notification> mPages;

        public WearableExtender() {
            this.mActions = new ArrayList<>();
            this.mFlags = 1;
            this.mPages = new ArrayList<>();
            this.mContentIconGravity = 8388613;
            this.mContentActionIndex = -1;
            this.mCustomSizePreset = 0;
            this.mGravity = 80;
        }

        public WearableExtender(Notification notif) {
            this.mActions = new ArrayList<>();
            this.mFlags = 1;
            this.mPages = new ArrayList<>();
            this.mContentIconGravity = 8388613;
            this.mContentActionIndex = -1;
            this.mCustomSizePreset = 0;
            this.mGravity = 80;
            Bundle wearableBundle = notif.extras.getBundle(EXTRA_WEARABLE_EXTENSIONS);
            if (wearableBundle != null) {
                List<Action> actions = wearableBundle.getParcelableArrayList(KEY_ACTIONS);
                if (actions != null) {
                    this.mActions.addAll(actions);
                }
                this.mFlags = wearableBundle.getInt("flags", 1);
                this.mDisplayIntent = (PendingIntent) wearableBundle.getParcelable(KEY_DISPLAY_INTENT);
                Notification[] pages = Notification.getNotificationArrayFromBundle(wearableBundle, KEY_PAGES);
                if (pages != null) {
                    Collections.addAll(this.mPages, pages);
                }
                this.mBackground = (Bitmap) wearableBundle.getParcelable(KEY_BACKGROUND);
                this.mContentIcon = wearableBundle.getInt(KEY_CONTENT_ICON);
                this.mContentIconGravity = wearableBundle.getInt(KEY_CONTENT_ICON_GRAVITY, 8388613);
                this.mContentActionIndex = wearableBundle.getInt(KEY_CONTENT_ACTION_INDEX, -1);
                this.mCustomSizePreset = wearableBundle.getInt(KEY_CUSTOM_SIZE_PRESET, 0);
                this.mCustomContentHeight = wearableBundle.getInt(KEY_CUSTOM_CONTENT_HEIGHT);
                this.mGravity = wearableBundle.getInt(KEY_GRAVITY, 80);
                this.mHintScreenTimeout = wearableBundle.getInt(KEY_HINT_SCREEN_TIMEOUT);
            }
        }

        @Override
        public Builder extend(Builder builder) {
            Bundle wearableBundle = new Bundle();
            if (!this.mActions.isEmpty()) {
                wearableBundle.putParcelableArrayList(KEY_ACTIONS, this.mActions);
            }
            if (this.mFlags != 1) {
                wearableBundle.putInt("flags", this.mFlags);
            }
            if (this.mDisplayIntent != null) {
                wearableBundle.putParcelable(KEY_DISPLAY_INTENT, this.mDisplayIntent);
            }
            if (!this.mPages.isEmpty()) {
                wearableBundle.putParcelableArray(KEY_PAGES, (Parcelable[]) this.mPages.toArray(new Notification[this.mPages.size()]));
            }
            if (this.mBackground != null) {
                wearableBundle.putParcelable(KEY_BACKGROUND, this.mBackground);
            }
            if (this.mContentIcon != 0) {
                wearableBundle.putInt(KEY_CONTENT_ICON, this.mContentIcon);
            }
            if (this.mContentIconGravity != 8388613) {
                wearableBundle.putInt(KEY_CONTENT_ICON_GRAVITY, this.mContentIconGravity);
            }
            if (this.mContentActionIndex != -1) {
                wearableBundle.putInt(KEY_CONTENT_ACTION_INDEX, this.mContentActionIndex);
            }
            if (this.mCustomSizePreset != 0) {
                wearableBundle.putInt(KEY_CUSTOM_SIZE_PRESET, this.mCustomSizePreset);
            }
            if (this.mCustomContentHeight != 0) {
                wearableBundle.putInt(KEY_CUSTOM_CONTENT_HEIGHT, this.mCustomContentHeight);
            }
            if (this.mGravity != 80) {
                wearableBundle.putInt(KEY_GRAVITY, this.mGravity);
            }
            if (this.mHintScreenTimeout != 0) {
                wearableBundle.putInt(KEY_HINT_SCREEN_TIMEOUT, this.mHintScreenTimeout);
            }
            builder.getExtras().putBundle(EXTRA_WEARABLE_EXTENSIONS, wearableBundle);
            return builder;
        }

        public WearableExtender m12clone() {
            WearableExtender that = new WearableExtender();
            that.mActions = new ArrayList<>(this.mActions);
            that.mFlags = this.mFlags;
            that.mDisplayIntent = this.mDisplayIntent;
            that.mPages = new ArrayList<>(this.mPages);
            that.mBackground = this.mBackground;
            that.mContentIcon = this.mContentIcon;
            that.mContentIconGravity = this.mContentIconGravity;
            that.mContentActionIndex = this.mContentActionIndex;
            that.mCustomSizePreset = this.mCustomSizePreset;
            that.mCustomContentHeight = this.mCustomContentHeight;
            that.mGravity = this.mGravity;
            that.mHintScreenTimeout = this.mHintScreenTimeout;
            return that;
        }

        public WearableExtender addAction(Action action) {
            this.mActions.add(action);
            return this;
        }

        public WearableExtender addActions(List<Action> actions) {
            this.mActions.addAll(actions);
            return this;
        }

        public WearableExtender clearActions() {
            this.mActions.clear();
            return this;
        }

        public List<Action> getActions() {
            return this.mActions;
        }

        public WearableExtender setDisplayIntent(PendingIntent intent) {
            this.mDisplayIntent = intent;
            return this;
        }

        public PendingIntent getDisplayIntent() {
            return this.mDisplayIntent;
        }

        public WearableExtender addPage(Notification page) {
            this.mPages.add(page);
            return this;
        }

        public WearableExtender addPages(List<Notification> pages) {
            this.mPages.addAll(pages);
            return this;
        }

        public WearableExtender clearPages() {
            this.mPages.clear();
            return this;
        }

        public List<Notification> getPages() {
            return this.mPages;
        }

        public WearableExtender setBackground(Bitmap background) {
            this.mBackground = background;
            return this;
        }

        public Bitmap getBackground() {
            return this.mBackground;
        }

        public WearableExtender setContentIcon(int icon) {
            this.mContentIcon = icon;
            return this;
        }

        public int getContentIcon() {
            return this.mContentIcon;
        }

        public WearableExtender setContentIconGravity(int contentIconGravity) {
            this.mContentIconGravity = contentIconGravity;
            return this;
        }

        public int getContentIconGravity() {
            return this.mContentIconGravity;
        }

        public WearableExtender setContentAction(int actionIndex) {
            this.mContentActionIndex = actionIndex;
            return this;
        }

        public int getContentAction() {
            return this.mContentActionIndex;
        }

        public WearableExtender setGravity(int gravity) {
            this.mGravity = gravity;
            return this;
        }

        public int getGravity() {
            return this.mGravity;
        }

        public WearableExtender setCustomSizePreset(int sizePreset) {
            this.mCustomSizePreset = sizePreset;
            return this;
        }

        public int getCustomSizePreset() {
            return this.mCustomSizePreset;
        }

        public WearableExtender setCustomContentHeight(int height) {
            this.mCustomContentHeight = height;
            return this;
        }

        public int getCustomContentHeight() {
            return this.mCustomContentHeight;
        }

        public WearableExtender setStartScrollBottom(boolean startScrollBottom) {
            setFlag(8, startScrollBottom);
            return this;
        }

        public boolean getStartScrollBottom() {
            return (this.mFlags & 8) != 0;
        }

        public WearableExtender setContentIntentAvailableOffline(boolean contentIntentAvailableOffline) {
            setFlag(1, contentIntentAvailableOffline);
            return this;
        }

        public boolean getContentIntentAvailableOffline() {
            return (this.mFlags & 1) != 0;
        }

        public WearableExtender setHintHideIcon(boolean hintHideIcon) {
            setFlag(2, hintHideIcon);
            return this;
        }

        public boolean getHintHideIcon() {
            return (this.mFlags & 2) != 0;
        }

        public WearableExtender setHintShowBackgroundOnly(boolean hintShowBackgroundOnly) {
            setFlag(4, hintShowBackgroundOnly);
            return this;
        }

        public boolean getHintShowBackgroundOnly() {
            return (this.mFlags & 4) != 0;
        }

        public WearableExtender setHintAvoidBackgroundClipping(boolean hintAvoidBackgroundClipping) {
            setFlag(16, hintAvoidBackgroundClipping);
            return this;
        }

        public boolean getHintAvoidBackgroundClipping() {
            return (this.mFlags & 16) != 0;
        }

        public WearableExtender setHintScreenTimeout(int timeout) {
            this.mHintScreenTimeout = timeout;
            return this;
        }

        public int getHintScreenTimeout() {
            return this.mHintScreenTimeout;
        }

        private void setFlag(int mask, boolean value) {
            if (value) {
                this.mFlags |= mask;
            } else {
                this.mFlags &= mask ^ (-1);
            }
        }
    }

    private static Notification[] getNotificationArrayFromBundle(Bundle bundle, String key) {
        Parcelable[] array = bundle.getParcelableArray(key);
        if ((array instanceof Notification[]) || array == null) {
            return (Notification[]) array;
        }
        Notification[] typedArray = (Notification[]) Arrays.copyOf(array, array.length, Notification[].class);
        bundle.putParcelableArray(key, typedArray);
        return typedArray;
    }

    private static class BuilderRemoteViews extends RemoteViews {
        public BuilderRemoteViews(Parcel parcel) {
            super(parcel);
        }

        public BuilderRemoteViews(ApplicationInfo appInfo, int layoutId) {
            super(appInfo, layoutId);
        }

        @Override
        public BuilderRemoteViews mo11clone() {
            Parcel p = Parcel.obtain();
            writeToParcel(p, 0);
            p.setDataPosition(0);
            BuilderRemoteViews brv = new BuilderRemoteViews(p);
            p.recycle();
            return brv;
        }
    }
}
