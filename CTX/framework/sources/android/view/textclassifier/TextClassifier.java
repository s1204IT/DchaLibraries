package android.view.textclassifier;

import android.os.LocaleList;
import android.os.Looper;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.SpannableString;
import android.text.style.URLSpan;
import android.text.util.Linkify;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.view.textclassifier.TextClassification;
import android.view.textclassifier.TextLinks;
import android.view.textclassifier.TextSelection;
import com.android.internal.util.Preconditions;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;

public interface TextClassifier {
    public static final String DEFAULT_LOG_TAG = "androidtc";
    public static final String HINT_TEXT_IS_EDITABLE = "android.text_is_editable";
    public static final String HINT_TEXT_IS_NOT_EDITABLE = "android.text_is_not_editable";
    public static final int LOCAL = 0;
    public static final TextClassifier NO_OP = new TextClassifier() {
    };
    public static final int SYSTEM = 1;
    public static final String TYPE_ADDRESS = "address";
    public static final String TYPE_DATE = "date";
    public static final String TYPE_DATE_TIME = "datetime";
    public static final String TYPE_EMAIL = "email";
    public static final String TYPE_FLIGHT_NUMBER = "flight";
    public static final String TYPE_OTHER = "other";
    public static final String TYPE_PHONE = "phone";
    public static final String TYPE_UNKNOWN = "";
    public static final String TYPE_URL = "url";
    public static final String WIDGET_TYPE_CUSTOM_EDITTEXT = "customedit";
    public static final String WIDGET_TYPE_CUSTOM_TEXTVIEW = "customview";
    public static final String WIDGET_TYPE_CUSTOM_UNSELECTABLE_TEXTVIEW = "nosel-customview";
    public static final String WIDGET_TYPE_EDITTEXT = "edittext";
    public static final String WIDGET_TYPE_EDIT_WEBVIEW = "edit-webview";
    public static final String WIDGET_TYPE_TEXTVIEW = "textview";
    public static final String WIDGET_TYPE_UNKNOWN = "unknown";
    public static final String WIDGET_TYPE_UNSELECTABLE_TEXTVIEW = "nosel-textview";
    public static final String WIDGET_TYPE_WEBVIEW = "webview";

    @Retention(RetentionPolicy.SOURCE)
    public @interface EntityType {
    }

    @Retention(RetentionPolicy.SOURCE)
    public @interface Hints {
    }

    @Retention(RetentionPolicy.SOURCE)
    public @interface TextClassifierType {
    }

    @Retention(RetentionPolicy.SOURCE)
    public @interface WidgetType {
    }

    default TextSelection suggestSelection(TextSelection.Request request) {
        Preconditions.checkNotNull(request);
        Utils.checkMainThread();
        return new TextSelection.Builder(request.getStartIndex(), request.getEndIndex()).build();
    }

    default TextSelection suggestSelection(CharSequence charSequence, int i, int i2, LocaleList localeList) {
        return suggestSelection(new TextSelection.Request.Builder(charSequence, i, i2).setDefaultLocales(localeList).build());
    }

    default TextSelection suggestSelection(CharSequence charSequence, int i, int i2, TextSelection.Options options) {
        if (options == null) {
            return suggestSelection(new TextSelection.Request.Builder(charSequence, i, i2).build());
        }
        if (options.getRequest() != null) {
            return suggestSelection(options.getRequest());
        }
        return suggestSelection(new TextSelection.Request.Builder(charSequence, i, i2).setDefaultLocales(options.getDefaultLocales()).build());
    }

    default TextClassification classifyText(TextClassification.Request request) {
        Preconditions.checkNotNull(request);
        Utils.checkMainThread();
        return TextClassification.EMPTY;
    }

    default TextClassification classifyText(CharSequence charSequence, int i, int i2, LocaleList localeList) {
        return classifyText(new TextClassification.Request.Builder(charSequence, i, i2).setDefaultLocales(localeList).build());
    }

    default TextClassification classifyText(CharSequence charSequence, int i, int i2, TextClassification.Options options) {
        if (options == null) {
            return classifyText(new TextClassification.Request.Builder(charSequence, i, i2).build());
        }
        if (options.getRequest() != null) {
            return classifyText(options.getRequest());
        }
        return classifyText(new TextClassification.Request.Builder(charSequence, i, i2).setDefaultLocales(options.getDefaultLocales()).setReferenceTime(options.getReferenceTime()).build());
    }

    default TextLinks generateLinks(TextLinks.Request request) {
        Preconditions.checkNotNull(request);
        Utils.checkMainThread();
        return new TextLinks.Builder(request.getText().toString()).build();
    }

    default TextLinks generateLinks(CharSequence charSequence, TextLinks.Options options) {
        if (options == null) {
            return generateLinks(new TextLinks.Request.Builder(charSequence).build());
        }
        if (options.getRequest() != null) {
            return generateLinks(options.getRequest());
        }
        return generateLinks(new TextLinks.Request.Builder(charSequence).setDefaultLocales(options.getDefaultLocales()).setEntityConfig(options.getEntityConfig()).build());
    }

    default int getMaxGenerateLinksTextLength() {
        return Integer.MAX_VALUE;
    }

    default void onSelectionEvent(SelectionEvent selectionEvent) {
    }

    default void destroy() {
    }

    default boolean isDestroyed() {
        return false;
    }

    public static final class EntityConfig implements Parcelable {
        public static final Parcelable.Creator<EntityConfig> CREATOR = new Parcelable.Creator<EntityConfig>() {
            @Override
            public EntityConfig createFromParcel(Parcel parcel) {
                return new EntityConfig(parcel);
            }

            @Override
            public EntityConfig[] newArray(int i) {
                return new EntityConfig[i];
            }
        };
        private final Collection<String> mExcludedEntityTypes;
        private final Collection<String> mHints;
        private final Collection<String> mIncludedEntityTypes;
        private final boolean mUseHints;

        private EntityConfig(boolean z, Collection<String> collection, Collection<String> collection2, Collection<String> collection3) {
            Collection<String> collectionUnmodifiableCollection;
            if (collection == null) {
                collectionUnmodifiableCollection = Collections.EMPTY_LIST;
            } else {
                collectionUnmodifiableCollection = Collections.unmodifiableCollection(new ArraySet(collection));
            }
            this.mHints = collectionUnmodifiableCollection;
            this.mExcludedEntityTypes = collection3 == null ? Collections.EMPTY_LIST : new ArraySet<>(collection3);
            this.mIncludedEntityTypes = collection2 == null ? Collections.EMPTY_LIST : new ArraySet<>(collection2);
            this.mUseHints = z;
        }

        public static EntityConfig createWithHints(Collection<String> collection) {
            return new EntityConfig(true, collection, null, null);
        }

        public static EntityConfig create(Collection<String> collection) {
            return createWithHints(collection);
        }

        public static EntityConfig create(Collection<String> collection, Collection<String> collection2, Collection<String> collection3) {
            return new EntityConfig(true, collection, collection2, collection3);
        }

        public static EntityConfig createWithExplicitEntityList(Collection<String> collection) {
            return new EntityConfig(false, null, collection, null);
        }

        public static EntityConfig createWithEntityList(Collection<String> collection) {
            return createWithExplicitEntityList(collection);
        }

        public Collection<String> resolveEntityListModifications(Collection<String> collection) {
            HashSet hashSet = new HashSet();
            if (this.mUseHints) {
                hashSet.addAll(collection);
            }
            hashSet.addAll(this.mIncludedEntityTypes);
            hashSet.removeAll(this.mExcludedEntityTypes);
            return hashSet;
        }

        public Collection<String> getHints() {
            return this.mHints;
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel parcel, int i) {
            parcel.writeStringList(new ArrayList(this.mHints));
            parcel.writeStringList(new ArrayList(this.mExcludedEntityTypes));
            parcel.writeStringList(new ArrayList(this.mIncludedEntityTypes));
            parcel.writeInt(this.mUseHints ? 1 : 0);
        }

        private EntityConfig(Parcel parcel) {
            this.mHints = new ArraySet(parcel.createStringArrayList());
            this.mExcludedEntityTypes = new ArraySet(parcel.createStringArrayList());
            this.mIncludedEntityTypes = new ArraySet(parcel.createStringArrayList());
            this.mUseHints = parcel.readInt() == 1;
        }
    }

    public static final class Utils {
        static void checkArgument(CharSequence charSequence, int i, int i2) {
            Preconditions.checkArgument(charSequence != null);
            Preconditions.checkArgument(i >= 0);
            Preconditions.checkArgument(i2 <= charSequence.length());
            Preconditions.checkArgument(i2 > i);
        }

        static void checkTextLength(CharSequence charSequence, int i) {
            Preconditions.checkArgumentInRange(charSequence.length(), 0, i, "text.length()");
        }

        public static TextLinks generateLegacyLinks(TextLinks.Request request) {
            String string = request.getText().toString();
            TextLinks.Builder builder = new TextLinks.Builder(string);
            Collection<String> collectionResolveEntityListModifications = request.getEntityConfig().resolveEntityListModifications(Collections.emptyList());
            if (collectionResolveEntityListModifications.contains("url")) {
                addLinks(builder, string, "url");
            }
            if (collectionResolveEntityListModifications.contains("phone")) {
                addLinks(builder, string, "phone");
            }
            if (collectionResolveEntityListModifications.contains("email")) {
                addLinks(builder, string, "email");
            }
            return builder.build();
        }

        private static void addLinks(TextLinks.Builder builder, String str, String str2) {
            SpannableString spannableString = new SpannableString(str);
            if (Linkify.addLinks(spannableString, linkMask(str2))) {
                for (URLSpan uRLSpan : (URLSpan[]) spannableString.getSpans(0, spannableString.length(), URLSpan.class)) {
                    builder.addLink(spannableString.getSpanStart(uRLSpan), spannableString.getSpanEnd(uRLSpan), entityScores(str2), uRLSpan);
                }
            }
        }

        private static int linkMask(String str) {
            byte b;
            int iHashCode = str.hashCode();
            if (iHashCode != 116079) {
                if (iHashCode != 96619420) {
                    b = (iHashCode == 106642798 && str.equals("phone")) ? (byte) 1 : (byte) -1;
                } else if (str.equals("email")) {
                    b = 2;
                }
            } else if (str.equals("url")) {
                b = 0;
            }
            switch (b) {
                case 0:
                    return 1;
                case 1:
                    return 4;
                case 2:
                    return 2;
                default:
                    return 0;
            }
        }

        private static Map<String, Float> entityScores(String str) {
            ArrayMap arrayMap = new ArrayMap();
            arrayMap.put(str, Float.valueOf(1.0f));
            return arrayMap;
        }

        static void checkMainThread() {
            if (Looper.myLooper() == Looper.getMainLooper()) {
                Log.w(TextClassifier.DEFAULT_LOG_TAG, "TextClassifier called on main thread");
            }
        }
    }
}
