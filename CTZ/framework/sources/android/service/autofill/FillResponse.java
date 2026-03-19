package android.service.autofill;

import android.content.IntentSender;
import android.content.pm.ParceledListSlice;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.view.autofill.AutofillId;
import android.view.autofill.Helper;
import android.widget.RemoteViews;
import com.android.internal.util.Preconditions;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class FillResponse implements Parcelable {
    public static final Parcelable.Creator<FillResponse> CREATOR = new Parcelable.Creator<FillResponse>() {
        @Override
        public FillResponse createFromParcel(Parcel parcel) {
            Builder builder = new Builder();
            ParceledListSlice parceledListSlice = (ParceledListSlice) parcel.readParcelable(null);
            List list = parceledListSlice != null ? parceledListSlice.getList() : null;
            int size = list != null ? list.size() : 0;
            for (int i = 0; i < size; i++) {
                builder.addDataset((Dataset) list.get(i));
            }
            builder.setSaveInfo((SaveInfo) parcel.readParcelable(null));
            builder.setClientState((Bundle) parcel.readParcelable(null));
            AutofillId[] autofillIdArr = (AutofillId[]) parcel.readParcelableArray(null, AutofillId.class);
            IntentSender intentSender = (IntentSender) parcel.readParcelable(null);
            RemoteViews remoteViews = (RemoteViews) parcel.readParcelable(null);
            if (autofillIdArr != null) {
                builder.setAuthentication(autofillIdArr, intentSender, remoteViews);
            }
            RemoteViews remoteViews2 = (RemoteViews) parcel.readParcelable(null);
            if (remoteViews2 != null) {
                builder.setHeader(remoteViews2);
            }
            RemoteViews remoteViews3 = (RemoteViews) parcel.readParcelable(null);
            if (remoteViews3 != null) {
                builder.setFooter(remoteViews3);
            }
            builder.setIgnoredIds((AutofillId[]) parcel.readParcelableArray(null, AutofillId.class));
            long j = parcel.readLong();
            if (j > 0) {
                builder.disableAutofill(j);
            }
            AutofillId[] autofillIdArr2 = (AutofillId[]) parcel.readParcelableArray(null, AutofillId.class);
            if (autofillIdArr2 != null) {
                builder.setFieldClassificationIds(autofillIdArr2);
            }
            builder.setFlags(parcel.readInt());
            FillResponse fillResponseBuild = builder.build();
            fillResponseBuild.setRequestId(parcel.readInt());
            return fillResponseBuild;
        }

        @Override
        public FillResponse[] newArray(int i) {
            return new FillResponse[i];
        }
    };
    public static final int FLAG_DISABLE_ACTIVITY_ONLY = 2;
    public static final int FLAG_TRACK_CONTEXT_COMMITED = 1;
    private final IntentSender mAuthentication;
    private final AutofillId[] mAuthenticationIds;
    private final Bundle mClientState;
    private final ParceledListSlice<Dataset> mDatasets;
    private final long mDisableDuration;
    private final AutofillId[] mFieldClassificationIds;
    private final int mFlags;
    private final RemoteViews mFooter;
    private final RemoteViews mHeader;
    private final AutofillId[] mIgnoredIds;
    private final RemoteViews mPresentation;
    private int mRequestId;
    private final SaveInfo mSaveInfo;

    @Retention(RetentionPolicy.SOURCE)
    @interface FillResponseFlags {
    }

    private FillResponse(Builder builder) {
        this.mDatasets = builder.mDatasets != null ? new ParceledListSlice<>(builder.mDatasets) : null;
        this.mSaveInfo = builder.mSaveInfo;
        this.mClientState = builder.mClientState;
        this.mPresentation = builder.mPresentation;
        this.mHeader = builder.mHeader;
        this.mFooter = builder.mFooter;
        this.mAuthentication = builder.mAuthentication;
        this.mAuthenticationIds = builder.mAuthenticationIds;
        this.mIgnoredIds = builder.mIgnoredIds;
        this.mDisableDuration = builder.mDisableDuration;
        this.mFieldClassificationIds = builder.mFieldClassificationIds;
        this.mFlags = builder.mFlags;
        this.mRequestId = Integer.MIN_VALUE;
    }

    public Bundle getClientState() {
        return this.mClientState;
    }

    public List<Dataset> getDatasets() {
        if (this.mDatasets != null) {
            return this.mDatasets.getList();
        }
        return null;
    }

    public SaveInfo getSaveInfo() {
        return this.mSaveInfo;
    }

    public RemoteViews getPresentation() {
        return this.mPresentation;
    }

    public RemoteViews getHeader() {
        return this.mHeader;
    }

    public RemoteViews getFooter() {
        return this.mFooter;
    }

    public IntentSender getAuthentication() {
        return this.mAuthentication;
    }

    public AutofillId[] getAuthenticationIds() {
        return this.mAuthenticationIds;
    }

    public AutofillId[] getIgnoredIds() {
        return this.mIgnoredIds;
    }

    public long getDisableDuration() {
        return this.mDisableDuration;
    }

    public AutofillId[] getFieldClassificationIds() {
        return this.mFieldClassificationIds;
    }

    public int getFlags() {
        return this.mFlags;
    }

    public void setRequestId(int i) {
        this.mRequestId = i;
    }

    public int getRequestId() {
        return this.mRequestId;
    }

    public static final class Builder {
        private IntentSender mAuthentication;
        private AutofillId[] mAuthenticationIds;
        private Bundle mClientState;
        private ArrayList<Dataset> mDatasets;
        private boolean mDestroyed;
        private long mDisableDuration;
        private AutofillId[] mFieldClassificationIds;
        private int mFlags;
        private RemoteViews mFooter;
        private RemoteViews mHeader;
        private AutofillId[] mIgnoredIds;
        private RemoteViews mPresentation;
        private SaveInfo mSaveInfo;

        public Builder setAuthentication(AutofillId[] autofillIdArr, IntentSender intentSender, RemoteViews remoteViews) {
            throwIfDestroyed();
            throwIfDisableAutofillCalled();
            if (this.mHeader != null || this.mFooter != null) {
                throw new IllegalStateException("Already called #setHeader() or #setFooter()");
            }
            if ((remoteViews == null) ^ (intentSender == null)) {
                throw new IllegalArgumentException("authentication and presentation must be both non-null or null");
            }
            this.mAuthentication = intentSender;
            this.mPresentation = remoteViews;
            this.mAuthenticationIds = AutofillServiceHelper.assertValid(autofillIdArr);
            return this;
        }

        public Builder setIgnoredIds(AutofillId... autofillIdArr) {
            throwIfDestroyed();
            this.mIgnoredIds = autofillIdArr;
            return this;
        }

        public Builder addDataset(Dataset dataset) {
            throwIfDestroyed();
            throwIfDisableAutofillCalled();
            if (dataset == null) {
                return this;
            }
            if (this.mDatasets == null) {
                this.mDatasets = new ArrayList<>();
            }
            if (!this.mDatasets.add(dataset)) {
                return this;
            }
            return this;
        }

        public Builder setSaveInfo(SaveInfo saveInfo) {
            throwIfDestroyed();
            throwIfDisableAutofillCalled();
            this.mSaveInfo = saveInfo;
            return this;
        }

        public Builder setClientState(Bundle bundle) {
            throwIfDestroyed();
            throwIfDisableAutofillCalled();
            this.mClientState = bundle;
            return this;
        }

        public Builder setFieldClassificationIds(AutofillId... autofillIdArr) {
            throwIfDestroyed();
            throwIfDisableAutofillCalled();
            Preconditions.checkArrayElementsNotNull(autofillIdArr, "ids");
            Preconditions.checkArgumentInRange(autofillIdArr.length, 1, UserData.getMaxFieldClassificationIdsSize(), "ids length");
            this.mFieldClassificationIds = autofillIdArr;
            this.mFlags |= 1;
            return this;
        }

        public Builder setFlags(int i) {
            throwIfDestroyed();
            this.mFlags = Preconditions.checkFlagsArgument(i, 3);
            return this;
        }

        public Builder disableAutofill(long j) {
            throwIfDestroyed();
            if (j <= 0) {
                throw new IllegalArgumentException("duration must be greater than 0");
            }
            if (this.mAuthentication != null || this.mDatasets != null || this.mSaveInfo != null || this.mFieldClassificationIds != null || this.mClientState != null) {
                throw new IllegalStateException("disableAutofill() must be the only method called");
            }
            this.mDisableDuration = j;
            return this;
        }

        public Builder setHeader(RemoteViews remoteViews) {
            throwIfDestroyed();
            throwIfAuthenticationCalled();
            this.mHeader = (RemoteViews) Preconditions.checkNotNull(remoteViews);
            return this;
        }

        public Builder setFooter(RemoteViews remoteViews) {
            throwIfDestroyed();
            throwIfAuthenticationCalled();
            this.mFooter = (RemoteViews) Preconditions.checkNotNull(remoteViews);
            return this;
        }

        public FillResponse build() {
            throwIfDestroyed();
            if (this.mAuthentication == null && this.mDatasets == null && this.mSaveInfo == null && this.mDisableDuration == 0 && this.mFieldClassificationIds == null && this.mClientState == null) {
                throw new IllegalStateException("need to provide: at least one DataSet, or a SaveInfo, or an authentication with a presentation, or a FieldsDetection, or a client state, or disable autofill");
            }
            if (this.mDatasets == null && (this.mHeader != null || this.mFooter != null)) {
                throw new IllegalStateException("must add at least 1 dataset when using header or footer");
            }
            this.mDestroyed = true;
            return new FillResponse(this);
        }

        private void throwIfDestroyed() {
            if (this.mDestroyed) {
                throw new IllegalStateException("Already called #build()");
            }
        }

        private void throwIfDisableAutofillCalled() {
            if (this.mDisableDuration > 0) {
                throw new IllegalStateException("Already called #disableAutofill()");
            }
        }

        private void throwIfAuthenticationCalled() {
            if (this.mAuthentication != null) {
                throw new IllegalStateException("Already called #setAuthentication()");
            }
        }
    }

    public String toString() {
        if (!Helper.sDebug || Build.TYPE.equals("user")) {
            return super.toString();
        }
        StringBuilder sb = new StringBuilder("FillResponse : [mRequestId=" + this.mRequestId);
        if (this.mDatasets != null) {
            sb.append(", datasets=");
            sb.append(this.mDatasets.getList());
        }
        if (this.mSaveInfo != null) {
            sb.append(", saveInfo=");
            sb.append(this.mSaveInfo);
        }
        if (this.mClientState != null) {
            sb.append(", hasClientState");
        }
        if (this.mPresentation != null) {
            sb.append(", hasPresentation");
        }
        if (this.mHeader != null) {
            sb.append(", hasHeader");
        }
        if (this.mFooter != null) {
            sb.append(", hasFooter");
        }
        if (this.mAuthentication != null) {
            sb.append(", hasAuthentication");
        }
        if (this.mAuthenticationIds != null) {
            sb.append(", authenticationIds=");
            sb.append(Arrays.toString(this.mAuthenticationIds));
        }
        sb.append(", disableDuration=");
        sb.append(this.mDisableDuration);
        if (this.mFlags != 0) {
            sb.append(", flags=");
            sb.append(this.mFlags);
        }
        if (this.mFieldClassificationIds != null) {
            sb.append(Arrays.toString(this.mFieldClassificationIds));
        }
        sb.append("]");
        return sb.toString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeParcelable(this.mDatasets, i);
        parcel.writeParcelable(this.mSaveInfo, i);
        parcel.writeParcelable(this.mClientState, i);
        parcel.writeParcelableArray(this.mAuthenticationIds, i);
        parcel.writeParcelable(this.mAuthentication, i);
        parcel.writeParcelable(this.mPresentation, i);
        parcel.writeParcelable(this.mHeader, i);
        parcel.writeParcelable(this.mFooter, i);
        parcel.writeParcelableArray(this.mIgnoredIds, i);
        parcel.writeLong(this.mDisableDuration);
        parcel.writeParcelableArray(this.mFieldClassificationIds, i);
        parcel.writeInt(this.mFlags);
        parcel.writeInt(this.mRequestId);
    }
}
