package com.android.quicksearchbox.ui;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.text.Html;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.TextAppearanceSpan;
import android.util.AttributeSet;
import android.widget.ImageView;
import android.widget.TextView;
import com.android.quicksearchbox.R;
import com.android.quicksearchbox.Source;
import com.android.quicksearchbox.Suggestion;
import com.android.quicksearchbox.util.Consumer;
import com.android.quicksearchbox.util.NowOrLater;

public class DefaultSuggestionView extends BaseSuggestionView {
    private final String TAG;
    private AsyncIcon mAsyncIcon1;
    private AsyncIcon mAsyncIcon2;

    public DefaultSuggestionView(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
        this.TAG = "QSB.DefaultSuggestionView";
    }

    public DefaultSuggestionView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.TAG = "QSB.DefaultSuggestionView";
    }

    public DefaultSuggestionView(Context context) {
        super(context);
        this.TAG = "QSB.DefaultSuggestionView";
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        this.mText1 = (TextView) findViewById(R.id.text1);
        this.mText2 = (TextView) findViewById(R.id.text2);
        this.mAsyncIcon1 = new AsyncIcon(this.mIcon1) {
            @Override
            protected String getFallbackIconId(Source source) {
                return source.getSourceIconUri().toString();
            }

            @Override
            protected Drawable getFallbackIcon(Source source) {
                return source.getSourceIcon();
            }
        };
        this.mAsyncIcon2 = new AsyncIcon(this.mIcon2);
    }

    @Override
    public void bindAsSuggestion(Suggestion suggestion, String str) {
        CharSequence text;
        super.bindAsSuggestion(suggestion, str);
        CharSequence text2 = formatText(suggestion.getSuggestionText1(), suggestion);
        String suggestionText2Url = suggestion.getSuggestionText2Url();
        if (suggestionText2Url != null) {
            text = formatUrl(suggestionText2Url);
        } else {
            text = formatText(suggestion.getSuggestionText2(), suggestion);
        }
        if (TextUtils.isEmpty(text)) {
            this.mText1.setSingleLine(false);
            this.mText1.setMaxLines(2);
            this.mText1.setEllipsize(TextUtils.TruncateAt.START);
        } else {
            this.mText1.setSingleLine(true);
            this.mText1.setMaxLines(1);
            this.mText1.setEllipsize(TextUtils.TruncateAt.MIDDLE);
        }
        setText1(text2);
        setText2(text);
        this.mAsyncIcon1.set(suggestion.getSuggestionSource(), suggestion.getSuggestionIcon1());
        this.mAsyncIcon2.set(suggestion.getSuggestionSource(), suggestion.getSuggestionIcon2());
    }

    private CharSequence formatUrl(CharSequence charSequence) {
        SpannableString spannableString = new SpannableString(charSequence);
        spannableString.setSpan(new TextAppearanceSpan(null, 0, 0, getResources().getColorStateList(R.color.url_text), null), 0, charSequence.length(), 33);
        return spannableString;
    }

    private CharSequence formatText(String str, Suggestion suggestion) {
        if ("html".equals(suggestion.getSuggestionFormat()) && looksLikeHtml(str)) {
            return Html.fromHtml(str);
        }
        return str;
    }

    private boolean looksLikeHtml(String str) {
        if (TextUtils.isEmpty(str)) {
            return false;
        }
        for (int length = str.length() - 1; length >= 0; length--) {
            char cCharAt = str.charAt(length);
            if (cCharAt == '>' || cCharAt == '&') {
                return true;
            }
        }
        return false;
    }

    private static void setViewDrawable(ImageView imageView, Drawable drawable) {
        imageView.setImageDrawable(drawable);
        if (drawable == null) {
            imageView.setVisibility(8);
            return;
        }
        imageView.setVisibility(0);
        drawable.setVisible(false, false);
        drawable.setVisible(true, false);
    }

    private class AsyncIcon {
        private String mCurrentId;
        private final ImageView mView;
        private String mWantedId;

        public AsyncIcon(ImageView imageView) {
            this.mView = imageView;
        }

        public void set(final Source source, String str) {
            if (str != null) {
                Uri iconUri = source.getIconUri(str);
                final String string = iconUri != null ? iconUri.toString() : null;
                this.mWantedId = string;
                if (!TextUtils.equals(this.mWantedId, this.mCurrentId)) {
                    NowOrLater<Drawable> icon = source.getIcon(str);
                    if (icon.haveNow()) {
                        handleNewDrawable(icon.getNow(), string, source);
                        return;
                    } else {
                        clearDrawable();
                        icon.getLater(new Consumer<Drawable>() {
                            @Override
                            public boolean consume(Drawable drawable) {
                                if (TextUtils.equals(string, AsyncIcon.this.mWantedId)) {
                                    AsyncIcon.this.handleNewDrawable(drawable, string, source);
                                    return true;
                                }
                                return false;
                            }
                        });
                        return;
                    }
                }
                return;
            }
            this.mWantedId = null;
            handleNewDrawable(null, null, source);
        }

        private void handleNewDrawable(Drawable drawable, String str, Source source) {
            if (drawable == null) {
                this.mWantedId = getFallbackIconId(source);
                if (TextUtils.equals(this.mWantedId, this.mCurrentId)) {
                    return;
                } else {
                    drawable = getFallbackIcon(source);
                }
            }
            setDrawable(drawable, str);
        }

        private void setDrawable(Drawable drawable, String str) {
            this.mCurrentId = str;
            DefaultSuggestionView.setViewDrawable(this.mView, drawable);
        }

        private void clearDrawable() {
            this.mCurrentId = null;
            this.mView.setImageDrawable(null);
        }

        protected String getFallbackIconId(Source source) {
            return null;
        }

        protected Drawable getFallbackIcon(Source source) {
            return null;
        }
    }

    public static class Factory extends SuggestionViewInflater {
        public Factory(Context context) {
            super("default", DefaultSuggestionView.class, R.layout.suggestion, context);
        }
    }
}
