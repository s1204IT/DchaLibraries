package android.service.autofill;

import android.content.IntentSender;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.DebugUtils;
import android.view.autofill.AutofillId;
import android.view.autofill.Helper;
import com.android.internal.util.ArrayUtils;
import com.android.internal.util.Preconditions;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Arrays;

public final class SaveInfo implements Parcelable {
    public static final Parcelable.Creator<SaveInfo> CREATOR = new Parcelable.Creator<SaveInfo>() {
        @Override
        public SaveInfo createFromParcel(Parcel parcel) {
            Builder builder;
            int i = parcel.readInt();
            AutofillId[] autofillIdArr = (AutofillId[]) parcel.readParcelableArray(null, AutofillId.class);
            if (autofillIdArr != null) {
                builder = new Builder(i, autofillIdArr);
            } else {
                builder = new Builder(i);
            }
            AutofillId[] autofillIdArr2 = (AutofillId[]) parcel.readParcelableArray(null, AutofillId.class);
            if (autofillIdArr2 != null) {
                builder.setOptionalIds(autofillIdArr2);
            }
            builder.setNegativeAction(parcel.readInt(), (IntentSender) parcel.readParcelable(null));
            builder.setDescription(parcel.readCharSequence());
            CustomDescription customDescription = (CustomDescription) parcel.readParcelable(null);
            if (customDescription != null) {
                builder.setCustomDescription(customDescription);
            }
            InternalValidator internalValidator = (InternalValidator) parcel.readParcelable(null);
            if (internalValidator != null) {
                builder.setValidator(internalValidator);
            }
            InternalSanitizer[] internalSanitizerArr = (InternalSanitizer[]) parcel.readParcelableArray(null, InternalSanitizer.class);
            if (internalSanitizerArr != null) {
                for (InternalSanitizer internalSanitizer : internalSanitizerArr) {
                    builder.addSanitizer(internalSanitizer, (AutofillId[]) parcel.readParcelableArray(null, AutofillId.class));
                }
            }
            AutofillId autofillId = (AutofillId) parcel.readParcelable(null);
            if (autofillId != null) {
                builder.setTriggerId(autofillId);
            }
            builder.setFlags(parcel.readInt());
            return builder.build();
        }

        @Override
        public SaveInfo[] newArray(int i) {
            return new SaveInfo[i];
        }
    };
    public static final int FLAG_DONT_SAVE_ON_FINISH = 2;
    public static final int FLAG_SAVE_ON_ALL_VIEWS_INVISIBLE = 1;
    public static final int NEGATIVE_BUTTON_STYLE_CANCEL = 0;
    public static final int NEGATIVE_BUTTON_STYLE_REJECT = 1;
    public static final int SAVE_DATA_TYPE_ADDRESS = 2;
    public static final int SAVE_DATA_TYPE_CREDIT_CARD = 4;
    public static final int SAVE_DATA_TYPE_EMAIL_ADDRESS = 16;
    public static final int SAVE_DATA_TYPE_GENERIC = 0;
    public static final int SAVE_DATA_TYPE_PASSWORD = 1;
    public static final int SAVE_DATA_TYPE_USERNAME = 8;
    private final CustomDescription mCustomDescription;
    private final CharSequence mDescription;
    private final int mFlags;
    private final IntentSender mNegativeActionListener;
    private final int mNegativeButtonStyle;
    private final AutofillId[] mOptionalIds;
    private final AutofillId[] mRequiredIds;
    private final InternalSanitizer[] mSanitizerKeys;
    private final AutofillId[][] mSanitizerValues;
    private final AutofillId mTriggerId;
    private final int mType;
    private final InternalValidator mValidator;

    @Retention(RetentionPolicy.SOURCE)
    @interface NegativeButtonStyle {
    }

    @Retention(RetentionPolicy.SOURCE)
    @interface SaveDataType {
    }

    @Retention(RetentionPolicy.SOURCE)
    @interface SaveInfoFlags {
    }

    private SaveInfo(Builder builder) {
        this.mType = builder.mType;
        this.mNegativeButtonStyle = builder.mNegativeButtonStyle;
        this.mNegativeActionListener = builder.mNegativeActionListener;
        this.mRequiredIds = builder.mRequiredIds;
        this.mOptionalIds = builder.mOptionalIds;
        this.mDescription = builder.mDescription;
        this.mFlags = builder.mFlags;
        this.mCustomDescription = builder.mCustomDescription;
        this.mValidator = builder.mValidator;
        if (builder.mSanitizers == null) {
            this.mSanitizerKeys = null;
            this.mSanitizerValues = null;
        } else {
            int size = builder.mSanitizers.size();
            this.mSanitizerKeys = new InternalSanitizer[size];
            this.mSanitizerValues = new AutofillId[size][];
            for (int i = 0; i < size; i++) {
                this.mSanitizerKeys[i] = (InternalSanitizer) builder.mSanitizers.keyAt(i);
                this.mSanitizerValues[i] = (AutofillId[]) builder.mSanitizers.valueAt(i);
            }
        }
        this.mTriggerId = builder.mTriggerId;
    }

    public int getNegativeActionStyle() {
        return this.mNegativeButtonStyle;
    }

    public IntentSender getNegativeActionListener() {
        return this.mNegativeActionListener;
    }

    public AutofillId[] getRequiredIds() {
        return this.mRequiredIds;
    }

    public AutofillId[] getOptionalIds() {
        return this.mOptionalIds;
    }

    public int getType() {
        return this.mType;
    }

    public int getFlags() {
        return this.mFlags;
    }

    public CharSequence getDescription() {
        return this.mDescription;
    }

    public CustomDescription getCustomDescription() {
        return this.mCustomDescription;
    }

    public InternalValidator getValidator() {
        return this.mValidator;
    }

    public InternalSanitizer[] getSanitizerKeys() {
        return this.mSanitizerKeys;
    }

    public AutofillId[][] getSanitizerValues() {
        return this.mSanitizerValues;
    }

    public AutofillId getTriggerId() {
        return this.mTriggerId;
    }

    public static final class Builder {
        private CustomDescription mCustomDescription;
        private CharSequence mDescription;
        private boolean mDestroyed;
        private int mFlags;
        private IntentSender mNegativeActionListener;
        private int mNegativeButtonStyle;
        private AutofillId[] mOptionalIds;
        private final AutofillId[] mRequiredIds;
        private ArraySet<AutofillId> mSanitizerIds;
        private ArrayMap<InternalSanitizer, AutofillId[]> mSanitizers;
        private AutofillId mTriggerId;
        private final int mType;
        private InternalValidator mValidator;

        public Builder(int i, AutofillId[] autofillIdArr) {
            this.mNegativeButtonStyle = 0;
            this.mType = i;
            this.mRequiredIds = AutofillServiceHelper.assertValid(autofillIdArr);
        }

        public Builder(int i) {
            this.mNegativeButtonStyle = 0;
            this.mType = i;
            this.mRequiredIds = null;
        }

        public Builder setFlags(int i) {
            throwIfDestroyed();
            this.mFlags = Preconditions.checkFlagsArgument(i, 3);
            return this;
        }

        public Builder setOptionalIds(AutofillId[] autofillIdArr) {
            throwIfDestroyed();
            this.mOptionalIds = AutofillServiceHelper.assertValid(autofillIdArr);
            return this;
        }

        public Builder setDescription(CharSequence charSequence) {
            throwIfDestroyed();
            Preconditions.checkState(this.mCustomDescription == null, "Can call setDescription() or setCustomDescription(), but not both");
            this.mDescription = charSequence;
            return this;
        }

        public Builder setCustomDescription(CustomDescription customDescription) {
            throwIfDestroyed();
            Preconditions.checkState(this.mDescription == null, "Can call setDescription() or setCustomDescription(), but not both");
            this.mCustomDescription = customDescription;
            return this;
        }

        public Builder setNegativeAction(int i, IntentSender intentSender) {
            throwIfDestroyed();
            if (i != 0 && i != 1) {
                throw new IllegalArgumentException("Invalid style: " + i);
            }
            this.mNegativeButtonStyle = i;
            this.mNegativeActionListener = intentSender;
            return this;
        }

        public Builder setValidator(Validator validator) {
            throwIfDestroyed();
            Preconditions.checkArgument(validator instanceof InternalValidator, "not provided by Android System: " + validator);
            this.mValidator = (InternalValidator) validator;
            return this;
        }

        public Builder addSanitizer(Sanitizer sanitizer, AutofillId... autofillIdArr) {
            throwIfDestroyed();
            Preconditions.checkArgument(!ArrayUtils.isEmpty(autofillIdArr), "ids cannot be empty or null");
            Preconditions.checkArgument(sanitizer instanceof InternalSanitizer, "not provided by Android System: " + sanitizer);
            if (this.mSanitizers == null) {
                this.mSanitizers = new ArrayMap<>();
                this.mSanitizerIds = new ArraySet<>(autofillIdArr.length);
            }
            for (AutofillId autofillId : autofillIdArr) {
                Preconditions.checkArgument(!this.mSanitizerIds.contains(autofillId), "already added %s", autofillId);
                this.mSanitizerIds.add(autofillId);
            }
            this.mSanitizers.put((InternalSanitizer) sanitizer, autofillIdArr);
            return this;
        }

        public Builder setTriggerId(AutofillId autofillId) {
            throwIfDestroyed();
            this.mTriggerId = (AutofillId) Preconditions.checkNotNull(autofillId);
            return this;
        }

        public SaveInfo build() {
            throwIfDestroyed();
            Preconditions.checkState((ArrayUtils.isEmpty(this.mRequiredIds) && ArrayUtils.isEmpty(this.mOptionalIds)) ? false : true, "must have at least one required or optional id");
            this.mDestroyed = true;
            return new SaveInfo(this);
        }

        private void throwIfDestroyed() {
            if (this.mDestroyed) {
                throw new IllegalStateException("Already called #build()");
            }
        }
    }

    public String toString() {
        if (!Helper.sDebug) {
            return super.toString();
        }
        StringBuilder sb = new StringBuilder("SaveInfo: [type=");
        sb.append(DebugUtils.flagsToString(SaveInfo.class, "SAVE_DATA_TYPE_", this.mType));
        sb.append(", requiredIds=");
        sb.append(Arrays.toString(this.mRequiredIds));
        sb.append(", style=");
        sb.append(DebugUtils.flagsToString(SaveInfo.class, "NEGATIVE_BUTTON_STYLE_", this.mNegativeButtonStyle));
        if (this.mOptionalIds != null) {
            sb.append(", optionalIds=");
            sb.append(Arrays.toString(this.mOptionalIds));
        }
        if (this.mDescription != null) {
            sb.append(", description=");
            sb.append(this.mDescription);
        }
        if (this.mFlags != 0) {
            sb.append(", flags=");
            sb.append(this.mFlags);
        }
        if (this.mCustomDescription != null) {
            sb.append(", customDescription=");
            sb.append(this.mCustomDescription);
        }
        if (this.mValidator != null) {
            sb.append(", validator=");
            sb.append(this.mValidator);
        }
        if (this.mSanitizerKeys != null) {
            sb.append(", sanitizerKeys=");
            sb.append(this.mSanitizerKeys.length);
        }
        if (this.mSanitizerValues != null) {
            sb.append(", sanitizerValues=");
            sb.append(this.mSanitizerValues.length);
        }
        if (this.mTriggerId != null) {
            sb.append(", triggerId=");
            sb.append(this.mTriggerId);
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
        parcel.writeInt(this.mType);
        parcel.writeParcelableArray(this.mRequiredIds, i);
        parcel.writeParcelableArray(this.mOptionalIds, i);
        parcel.writeInt(this.mNegativeButtonStyle);
        parcel.writeParcelable(this.mNegativeActionListener, i);
        parcel.writeCharSequence(this.mDescription);
        parcel.writeParcelable(this.mCustomDescription, i);
        parcel.writeParcelable(this.mValidator, i);
        parcel.writeParcelableArray(this.mSanitizerKeys, i);
        if (this.mSanitizerKeys != null) {
            for (int i2 = 0; i2 < this.mSanitizerValues.length; i2++) {
                parcel.writeParcelableArray(this.mSanitizerValues[i2], i);
            }
        }
        parcel.writeParcelable(this.mTriggerId, i);
        parcel.writeInt(this.mFlags);
    }
}
