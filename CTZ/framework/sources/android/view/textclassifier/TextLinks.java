package android.view.textclassifier;

import android.os.LocaleList;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.Spannable;
import android.text.style.ClickableSpan;
import android.text.style.URLSpan;
import android.view.View;
import android.view.textclassifier.TextClassifier;
import android.view.textclassifier.TextLinksParams;
import android.widget.TextView;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.util.Preconditions;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;

public final class TextLinks implements Parcelable {
    public static final int APPLY_STRATEGY_IGNORE = 0;
    public static final int APPLY_STRATEGY_REPLACE = 1;
    public static final Parcelable.Creator<TextLinks> CREATOR = new Parcelable.Creator<TextLinks>() {
        @Override
        public TextLinks createFromParcel(Parcel parcel) {
            return new TextLinks(parcel);
        }

        @Override
        public TextLinks[] newArray(int i) {
            return new TextLinks[i];
        }
    };
    public static final int STATUS_DIFFERENT_TEXT = 3;
    public static final int STATUS_LINKS_APPLIED = 0;
    public static final int STATUS_NO_LINKS_APPLIED = 2;
    public static final int STATUS_NO_LINKS_FOUND = 1;
    private final String mFullText;
    private final List<TextLink> mLinks;

    @Retention(RetentionPolicy.SOURCE)
    public @interface ApplyStrategy {
    }

    @Retention(RetentionPolicy.SOURCE)
    public @interface Status {
    }

    private TextLinks(String str, ArrayList<TextLink> arrayList) {
        this.mFullText = str;
        this.mLinks = Collections.unmodifiableList(arrayList);
    }

    public String getText() {
        return this.mFullText;
    }

    public Collection<TextLink> getLinks() {
        return this.mLinks;
    }

    public int apply(Spannable spannable, int i, Function<TextLink, TextLinkSpan> function) {
        Preconditions.checkNotNull(spannable);
        return new TextLinksParams.Builder().setApplyStrategy(i).setSpanFactory(function).build().apply(spannable, this);
    }

    public String toString() {
        return String.format(Locale.US, "TextLinks{fullText=%s, links=%s}", this.mFullText, this.mLinks);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(this.mFullText);
        parcel.writeTypedList(this.mLinks);
    }

    private TextLinks(Parcel parcel) {
        this.mFullText = parcel.readString();
        this.mLinks = parcel.createTypedArrayList(TextLink.CREATOR);
    }

    public static final class TextLink implements Parcelable {
        public static final Parcelable.Creator<TextLink> CREATOR = new Parcelable.Creator<TextLink>() {
            @Override
            public TextLink createFromParcel(Parcel parcel) {
                return new TextLink(parcel);
            }

            @Override
            public TextLink[] newArray(int i) {
                return new TextLink[i];
            }
        };
        private final int mEnd;
        private final EntityConfidence mEntityScores;
        private final int mStart;
        final URLSpan mUrlSpan;

        TextLink(int i, int i2, Map<String, Float> map, URLSpan uRLSpan) {
            Preconditions.checkNotNull(map);
            Preconditions.checkArgument(!map.isEmpty());
            Preconditions.checkArgument(i <= i2);
            this.mStart = i;
            this.mEnd = i2;
            this.mEntityScores = new EntityConfidence(map);
            this.mUrlSpan = uRLSpan;
        }

        public int getStart() {
            return this.mStart;
        }

        public int getEnd() {
            return this.mEnd;
        }

        public int getEntityCount() {
            return this.mEntityScores.getEntities().size();
        }

        public String getEntity(int i) {
            return this.mEntityScores.getEntities().get(i);
        }

        public float getConfidenceScore(String str) {
            return this.mEntityScores.getConfidenceScore(str);
        }

        public String toString() {
            return String.format(Locale.US, "TextLink{start=%s, end=%s, entityScores=%s, urlSpan=%s}", Integer.valueOf(this.mStart), Integer.valueOf(this.mEnd), this.mEntityScores, this.mUrlSpan);
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel parcel, int i) {
            this.mEntityScores.writeToParcel(parcel, i);
            parcel.writeInt(this.mStart);
            parcel.writeInt(this.mEnd);
        }

        private TextLink(Parcel parcel) {
            this.mEntityScores = EntityConfidence.CREATOR.createFromParcel(parcel);
            this.mStart = parcel.readInt();
            this.mEnd = parcel.readInt();
            this.mUrlSpan = null;
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
        private String mCallingPackageName;
        private final LocaleList mDefaultLocales;
        private final TextClassifier.EntityConfig mEntityConfig;
        private final boolean mLegacyFallback;
        private final CharSequence mText;

        private Request(CharSequence charSequence, LocaleList localeList, TextClassifier.EntityConfig entityConfig, boolean z, String str) {
            this.mText = charSequence;
            this.mDefaultLocales = localeList;
            this.mEntityConfig = entityConfig;
            this.mLegacyFallback = z;
            this.mCallingPackageName = str;
        }

        public CharSequence getText() {
            return this.mText;
        }

        public LocaleList getDefaultLocales() {
            return this.mDefaultLocales;
        }

        public TextClassifier.EntityConfig getEntityConfig() {
            return this.mEntityConfig;
        }

        public boolean isLegacyFallback() {
            return this.mLegacyFallback;
        }

        void setCallingPackageName(String str) {
            this.mCallingPackageName = str;
        }

        public static final class Builder {
            private String mCallingPackageName;
            private LocaleList mDefaultLocales;
            private TextClassifier.EntityConfig mEntityConfig;
            private boolean mLegacyFallback = true;
            private final CharSequence mText;

            public Builder(CharSequence charSequence) {
                this.mText = (CharSequence) Preconditions.checkNotNull(charSequence);
            }

            public Builder setDefaultLocales(LocaleList localeList) {
                this.mDefaultLocales = localeList;
                return this;
            }

            public Builder setEntityConfig(TextClassifier.EntityConfig entityConfig) {
                this.mEntityConfig = entityConfig;
                return this;
            }

            public Builder setLegacyFallback(boolean z) {
                this.mLegacyFallback = z;
                return this;
            }

            public Builder setCallingPackageName(String str) {
                this.mCallingPackageName = str;
                return this;
            }

            public Request build() {
                return new Request(this.mText, this.mDefaultLocales, this.mEntityConfig, this.mLegacyFallback, this.mCallingPackageName);
            }
        }

        public String getCallingPackageName() {
            return this.mCallingPackageName;
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel parcel, int i) {
            parcel.writeString(this.mText.toString());
            parcel.writeInt(this.mDefaultLocales != null ? 1 : 0);
            if (this.mDefaultLocales != null) {
                this.mDefaultLocales.writeToParcel(parcel, i);
            }
            parcel.writeInt(this.mEntityConfig != null ? 1 : 0);
            if (this.mEntityConfig != null) {
                this.mEntityConfig.writeToParcel(parcel, i);
            }
            parcel.writeString(this.mCallingPackageName);
        }

        private Request(Parcel parcel) {
            this.mText = parcel.readString();
            this.mDefaultLocales = parcel.readInt() == 0 ? null : LocaleList.CREATOR.createFromParcel(parcel);
            this.mEntityConfig = parcel.readInt() != 0 ? TextClassifier.EntityConfig.CREATOR.createFromParcel(parcel) : null;
            this.mLegacyFallback = true;
            this.mCallingPackageName = parcel.readString();
        }
    }

    public static class TextLinkSpan extends ClickableSpan {
        public static final int INVOCATION_METHOD_KEYBOARD = 1;
        public static final int INVOCATION_METHOD_TOUCH = 0;
        public static final int INVOCATION_METHOD_UNSPECIFIED = -1;
        private final TextLink mTextLink;

        @Retention(RetentionPolicy.SOURCE)
        public @interface InvocationMethod {
        }

        public TextLinkSpan(TextLink textLink) {
            this.mTextLink = textLink;
        }

        @Override
        public void onClick(View view) {
            onClick(view, -1);
        }

        public final void onClick(View view, int i) {
            if (view instanceof TextView) {
                TextView textView = (TextView) view;
                if (TextClassificationManager.getSettings(textView.getContext()).isSmartLinkifyEnabled()) {
                    if (i == 0) {
                        textView.requestActionMode(this);
                        return;
                    } else {
                        textView.handleClick(this);
                        return;
                    }
                }
                if (this.mTextLink.mUrlSpan != null) {
                    this.mTextLink.mUrlSpan.onClick(textView);
                } else {
                    textView.handleClick(this);
                }
            }
        }

        public final TextLink getTextLink() {
            return this.mTextLink;
        }

        @VisibleForTesting(visibility = VisibleForTesting.Visibility.PRIVATE)
        public final String getUrl() {
            if (this.mTextLink.mUrlSpan != null) {
                return this.mTextLink.mUrlSpan.getURL();
            }
            return null;
        }
    }

    public static final class Builder {
        private final String mFullText;
        private final ArrayList<TextLink> mLinks = new ArrayList<>();

        public Builder(String str) {
            this.mFullText = (String) Preconditions.checkNotNull(str);
        }

        public Builder addLink(int i, int i2, Map<String, Float> map) {
            this.mLinks.add(new TextLink(i, i2, map, null));
            return this;
        }

        Builder addLink(int i, int i2, Map<String, Float> map, URLSpan uRLSpan) {
            this.mLinks.add(new TextLink(i, i2, map, uRLSpan));
            return this;
        }

        public Builder clearTextLinks() {
            this.mLinks.clear();
            return this;
        }

        public TextLinks build() {
            return new TextLinks(this.mFullText, this.mLinks);
        }
    }

    public static final class Options {
        private int mApplyStrategy;
        private String mCallingPackageName;
        private LocaleList mDefaultLocales;
        private TextClassifier.EntityConfig mEntityConfig;
        private boolean mLegacyFallback;
        private final Request mRequest;
        private final TextClassificationSessionId mSessionId;
        private Function<TextLink, TextLinkSpan> mSpanFactory;

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
            options.setEntityConfig(request.getEntityConfig());
            return options;
        }

        public static Options fromLinkMask(int i) {
            ArrayList arrayList = new ArrayList();
            if ((i & 1) != 0) {
                arrayList.add("url");
            }
            if ((i & 2) != 0) {
                arrayList.add("email");
            }
            if ((i & 4) != 0) {
                arrayList.add("phone");
            }
            if ((i & 8) != 0) {
                arrayList.add("address");
            }
            return new Options().setEntityConfig(TextClassifier.EntityConfig.createWithEntityList(arrayList));
        }

        public Options setDefaultLocales(LocaleList localeList) {
            this.mDefaultLocales = localeList;
            return this;
        }

        public Options setEntityConfig(TextClassifier.EntityConfig entityConfig) {
            this.mEntityConfig = entityConfig;
            return this;
        }

        public Options setApplyStrategy(int i) {
            checkValidApplyStrategy(i);
            this.mApplyStrategy = i;
            return this;
        }

        public Options setSpanFactory(Function<TextLink, TextLinkSpan> function) {
            this.mSpanFactory = function;
            return this;
        }

        public LocaleList getDefaultLocales() {
            return this.mDefaultLocales;
        }

        public TextClassifier.EntityConfig getEntityConfig() {
            return this.mEntityConfig;
        }

        public int getApplyStrategy() {
            return this.mApplyStrategy;
        }

        public Function<TextLink, TextLinkSpan> getSpanFactory() {
            return this.mSpanFactory;
        }

        public Request getRequest() {
            return this.mRequest;
        }

        public TextClassificationSessionId getSessionId() {
            return this.mSessionId;
        }

        private static void checkValidApplyStrategy(int i) {
            if (i != 0 && i != 1) {
                throw new IllegalArgumentException("Invalid apply strategy. See TextLinks.ApplyStrategy for options.");
            }
        }
    }
}
