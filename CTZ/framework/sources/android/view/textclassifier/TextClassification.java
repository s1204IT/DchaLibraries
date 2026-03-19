package android.view.textclassifier;

import android.app.PendingIntent;
import android.app.RemoteAction;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.graphics.BitmapFactory;
import android.graphics.drawable.AdaptiveIconDrawable;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.Icon;
import android.os.LocaleList;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.ArrayMap;
import android.view.View;
import android.view.textclassifier.TextClassifier;
import com.android.internal.util.Preconditions;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class TextClassification implements Parcelable {
    private static final String LOG_TAG = "TextClassification";
    private static final int MAX_LEGACY_ICON_SIZE = 192;
    private final List<RemoteAction> mActions;
    private final EntityConfidence mEntityConfidence;
    private final String mId;
    private final Drawable mLegacyIcon;
    private final Intent mLegacyIntent;
    private final String mLegacyLabel;
    private final View.OnClickListener mLegacyOnClickListener;
    private final String mText;
    public static final TextClassification EMPTY = new Builder().build();
    public static final Parcelable.Creator<TextClassification> CREATOR = new Parcelable.Creator<TextClassification>() {
        @Override
        public TextClassification createFromParcel(Parcel parcel) {
            return new TextClassification(parcel);
        }

        @Override
        public TextClassification[] newArray(int i) {
            return new TextClassification[i];
        }
    };

    @Retention(RetentionPolicy.SOURCE)
    private @interface IntentType {
        public static final int ACTIVITY = 0;
        public static final int SERVICE = 1;
        public static final int UNSUPPORTED = -1;
    }

    private TextClassification(String str, Drawable drawable, String str2, Intent intent, View.OnClickListener onClickListener, List<RemoteAction> list, Map<String, Float> map, String str3) {
        this.mText = str;
        this.mLegacyIcon = drawable;
        this.mLegacyLabel = str2;
        this.mLegacyIntent = intent;
        this.mLegacyOnClickListener = onClickListener;
        this.mActions = Collections.unmodifiableList(list);
        this.mEntityConfidence = new EntityConfidence(map);
        this.mId = str3;
    }

    public String getText() {
        return this.mText;
    }

    public int getEntityCount() {
        return this.mEntityConfidence.getEntities().size();
    }

    public String getEntity(int i) {
        return this.mEntityConfidence.getEntities().get(i);
    }

    public float getConfidenceScore(String str) {
        return this.mEntityConfidence.getConfidenceScore(str);
    }

    public List<RemoteAction> getActions() {
        return this.mActions;
    }

    @Deprecated
    public Drawable getIcon() {
        return this.mLegacyIcon;
    }

    @Deprecated
    public CharSequence getLabel() {
        return this.mLegacyLabel;
    }

    @Deprecated
    public Intent getIntent() {
        return this.mLegacyIntent;
    }

    public View.OnClickListener getOnClickListener() {
        return this.mLegacyOnClickListener;
    }

    public String getId() {
        return this.mId;
    }

    public String toString() {
        return String.format(Locale.US, "TextClassification {text=%s, entities=%s, actions=%s, id=%s}", this.mText, this.mEntityConfidence, this.mActions, this.mId);
    }

    public static View.OnClickListener createIntentOnClickListener(final PendingIntent pendingIntent) {
        Preconditions.checkNotNull(pendingIntent);
        return new View.OnClickListener() {
            @Override
            public final void onClick(View view) {
                TextClassification.lambda$createIntentOnClickListener$0(pendingIntent, view);
            }
        };
    }

    static void lambda$createIntentOnClickListener$0(PendingIntent pendingIntent, View view) {
        try {
            pendingIntent.send();
        } catch (PendingIntent.CanceledException e) {
            Log.e(LOG_TAG, "Error sending PendingIntent", e);
        }
    }

    public static PendingIntent createPendingIntent(Context context, Intent intent, int i) {
        switch (getIntentType(intent, context)) {
            case 0:
                return PendingIntent.getActivity(context, i, intent, 134217728);
            case 1:
                return PendingIntent.getService(context, i, intent, 134217728);
            default:
                return null;
        }
    }

    private static int getIntentType(Intent intent, Context context) {
        Preconditions.checkArgument(context != null);
        Preconditions.checkArgument(intent != null);
        ResolveInfo resolveInfoResolveActivity = context.getPackageManager().resolveActivity(intent, 0);
        if (resolveInfoResolveActivity != null) {
            if (context.getPackageName().equals(resolveInfoResolveActivity.activityInfo.packageName)) {
                return 0;
            }
            if (resolveInfoResolveActivity.activityInfo.exported && hasPermission(context, resolveInfoResolveActivity.activityInfo.permission)) {
                return 0;
            }
        }
        ResolveInfo resolveInfoResolveService = context.getPackageManager().resolveService(intent, 0);
        if (resolveInfoResolveService != null) {
            if (context.getPackageName().equals(resolveInfoResolveService.serviceInfo.packageName)) {
                return 1;
            }
            if (resolveInfoResolveService.serviceInfo.exported && hasPermission(context, resolveInfoResolveService.serviceInfo.permission)) {
                return 1;
            }
            return -1;
        }
        return -1;
    }

    private static boolean hasPermission(Context context, String str) {
        return str == null || context.checkSelfPermission(str) == 0;
    }

    public static final class Builder {
        private List<RemoteAction> mActions = new ArrayList();
        private final Map<String, Float> mEntityConfidence = new ArrayMap();
        private String mId;
        private Drawable mLegacyIcon;
        private Intent mLegacyIntent;
        private String mLegacyLabel;
        private View.OnClickListener mLegacyOnClickListener;
        private String mText;

        public Builder setText(String str) {
            this.mText = str;
            return this;
        }

        public Builder setEntityType(String str, float f) {
            this.mEntityConfidence.put(str, Float.valueOf(f));
            return this;
        }

        public Builder addAction(RemoteAction remoteAction) {
            Preconditions.checkArgument(remoteAction != null);
            this.mActions.add(remoteAction);
            return this;
        }

        @Deprecated
        public Builder setIcon(Drawable drawable) {
            this.mLegacyIcon = drawable;
            return this;
        }

        @Deprecated
        public Builder setLabel(String str) {
            this.mLegacyLabel = str;
            return this;
        }

        @Deprecated
        public Builder setIntent(Intent intent) {
            this.mLegacyIntent = intent;
            return this;
        }

        @Deprecated
        public Builder setOnClickListener(View.OnClickListener onClickListener) {
            this.mLegacyOnClickListener = onClickListener;
            return this;
        }

        public Builder setId(String str) {
            this.mId = str;
            return this;
        }

        public TextClassification build() {
            return new TextClassification(this.mText, this.mLegacyIcon, this.mLegacyLabel, this.mLegacyIntent, this.mLegacyOnClickListener, this.mActions, this.mEntityConfidence, this.mId);
        }
    }

    public static final class Request implements Parcelable {
        public static final Parcelable.Creator<Request> CREATOR = new Parcelable.Creator<Request>() {
            @Override
            public Request createFromParcel(Parcel parcel) {
                return new Request(parcel);
            }

            @Override
            public Request[] newArray(int i) {
                return new Request[i];
            }
        };
        private final LocaleList mDefaultLocales;
        private final int mEndIndex;
        private final ZonedDateTime mReferenceTime;
        private final int mStartIndex;
        private final CharSequence mText;

        private Request(CharSequence charSequence, int i, int i2, LocaleList localeList, ZonedDateTime zonedDateTime) {
            this.mText = charSequence;
            this.mStartIndex = i;
            this.mEndIndex = i2;
            this.mDefaultLocales = localeList;
            this.mReferenceTime = zonedDateTime;
        }

        public CharSequence getText() {
            return this.mText;
        }

        public int getStartIndex() {
            return this.mStartIndex;
        }

        public int getEndIndex() {
            return this.mEndIndex;
        }

        public LocaleList getDefaultLocales() {
            return this.mDefaultLocales;
        }

        public ZonedDateTime getReferenceTime() {
            return this.mReferenceTime;
        }

        public static final class Builder {
            private LocaleList mDefaultLocales;
            private final int mEndIndex;
            private ZonedDateTime mReferenceTime;
            private final int mStartIndex;
            private final CharSequence mText;

            public Builder(CharSequence charSequence, int i, int i2) {
                TextClassifier.Utils.checkArgument(charSequence, i, i2);
                this.mText = charSequence;
                this.mStartIndex = i;
                this.mEndIndex = i2;
            }

            public Builder setDefaultLocales(LocaleList localeList) {
                this.mDefaultLocales = localeList;
                return this;
            }

            public Builder setReferenceTime(ZonedDateTime zonedDateTime) {
                this.mReferenceTime = zonedDateTime;
                return this;
            }

            public Request build() {
                return new Request(this.mText, this.mStartIndex, this.mEndIndex, this.mDefaultLocales, this.mReferenceTime);
            }
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel parcel, int i) {
            parcel.writeString(this.mText.toString());
            parcel.writeInt(this.mStartIndex);
            parcel.writeInt(this.mEndIndex);
            parcel.writeInt(this.mDefaultLocales != null ? 1 : 0);
            if (this.mDefaultLocales != null) {
                this.mDefaultLocales.writeToParcel(parcel, i);
            }
            parcel.writeInt(this.mReferenceTime != null ? 1 : 0);
            if (this.mReferenceTime != null) {
                parcel.writeString(this.mReferenceTime.toString());
            }
        }

        private Request(Parcel parcel) {
            this.mText = parcel.readString();
            this.mStartIndex = parcel.readInt();
            this.mEndIndex = parcel.readInt();
            this.mDefaultLocales = parcel.readInt() == 0 ? null : LocaleList.CREATOR.createFromParcel(parcel);
            this.mReferenceTime = parcel.readInt() != 0 ? ZonedDateTime.parse(parcel.readString()) : null;
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(this.mText);
        parcel.writeTypedList(this.mActions);
        this.mEntityConfidence.writeToParcel(parcel, i);
        parcel.writeString(this.mId);
    }

    private TextClassification(Parcel parcel) {
        this.mText = parcel.readString();
        this.mActions = parcel.createTypedArrayList(RemoteAction.CREATOR);
        if (!this.mActions.isEmpty()) {
            RemoteAction remoteAction = this.mActions.get(0);
            this.mLegacyIcon = maybeLoadDrawable(remoteAction.getIcon());
            this.mLegacyLabel = remoteAction.getTitle().toString();
            this.mLegacyOnClickListener = createIntentOnClickListener(this.mActions.get(0).getActionIntent());
        } else {
            this.mLegacyIcon = null;
            this.mLegacyLabel = null;
            this.mLegacyOnClickListener = null;
        }
        this.mLegacyIntent = null;
        this.mEntityConfidence = EntityConfidence.CREATOR.createFromParcel(parcel);
        this.mId = parcel.readString();
    }

    private static Drawable maybeLoadDrawable(Icon icon) {
        if (icon == null) {
            return null;
        }
        int type = icon.getType();
        if (type == 1) {
            return new BitmapDrawable(Resources.getSystem(), icon.getBitmap());
        }
        if (type == 3) {
            return new BitmapDrawable(Resources.getSystem(), BitmapFactory.decodeByteArray(icon.getDataBytes(), icon.getDataOffset(), icon.getDataLength()));
        }
        if (type != 5) {
            return null;
        }
        return new AdaptiveIconDrawable((Drawable) null, new BitmapDrawable(Resources.getSystem(), icon.getBitmap()));
    }

    public static final class Options {
        private LocaleList mDefaultLocales;
        private ZonedDateTime mReferenceTime;
        private final Request mRequest;
        private final TextClassificationSessionId mSessionId;

        public Options() {
            this(null, null);
        }

        private Options(TextClassificationSessionId textClassificationSessionId, Request request) {
            this.mSessionId = textClassificationSessionId;
            this.mRequest = request;
        }

        public static Options from(TextClassificationSessionId textClassificationSessionId, Request request) {
            Options options = new Options(textClassificationSessionId, request);
            options.setDefaultLocales(request.getDefaultLocales());
            options.setReferenceTime(request.getReferenceTime());
            return options;
        }

        public Options setDefaultLocales(LocaleList localeList) {
            this.mDefaultLocales = localeList;
            return this;
        }

        public Options setReferenceTime(ZonedDateTime zonedDateTime) {
            this.mReferenceTime = zonedDateTime;
            return this;
        }

        public LocaleList getDefaultLocales() {
            return this.mDefaultLocales;
        }

        public ZonedDateTime getReferenceTime() {
            return this.mReferenceTime;
        }

        public Request getRequest() {
            return this.mRequest;
        }

        public TextClassificationSessionId getSessionId() {
            return this.mSessionId;
        }
    }
}
