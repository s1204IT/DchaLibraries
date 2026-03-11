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
import com.android.quicksearchbox.Source;
import com.android.quicksearchbox.Suggestion;
import com.android.quicksearchbox.util.Consumer;
import com.android.quicksearchbox.util.NowOrLater;

public class DefaultSuggestionView extends BaseSuggestionView {
    private final String TAG;
    private AsyncIcon mAsyncIcon1;
    private AsyncIcon mAsyncIcon2;

    private class AsyncIcon {
        private String mCurrentId;
        private final ImageView mView;
        private String mWantedId;
        final DefaultSuggestionView this$0;

        public AsyncIcon(DefaultSuggestionView defaultSuggestionView, ImageView imageView) {
            this.this$0 = defaultSuggestionView;
            this.mView = imageView;
        }

        private void clearDrawable() {
            this.mCurrentId = null;
            this.mView.setImageDrawable(null);
        }

        public void handleNewDrawable(Drawable drawable, String str, Source source) {
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

        protected Drawable getFallbackIcon(Source source) {
            return null;
        }

        protected String getFallbackIconId(Source source) {
            return null;
        }

        public void set(Source source, String str) {
            if (str == null) {
                this.mWantedId = null;
                handleNewDrawable(null, null, source);
                return;
            }
            Uri iconUri = source.getIconUri(str);
            String string = iconUri == null ? null : iconUri.toString();
            this.mWantedId = string;
            if (TextUtils.equals(this.mWantedId, this.mCurrentId)) {
                return;
            }
            NowOrLater<Drawable> icon = source.getIcon(str);
            if (icon.haveNow()) {
                handleNewDrawable(icon.getNow(), string, source);
            } else {
                clearDrawable();
                icon.getLater(new Consumer<Drawable>(this, string, source) {
                    final AsyncIcon this$1;
                    final Source val$source;
                    final String val$uniqueIconId;

                    {
                        this.this$1 = this;
                        this.val$uniqueIconId = string;
                        this.val$source = source;
                    }

                    @Override
                    public boolean consume(Drawable drawable) {
                        if (!TextUtils.equals(this.val$uniqueIconId, this.this$1.mWantedId)) {
                            return false;
                        }
                        this.this$1.handleNewDrawable(drawable, this.val$uniqueIconId, this.val$source);
                        return true;
                    }
                });
            }
        }
    }

    public static class Factory extends SuggestionViewInflater {
        public Factory(Context context) {
            super("default", DefaultSuggestionView.class, 2130968582, context);
        }
    }

    public DefaultSuggestionView(Context context) {
        super(context);
        this.TAG = "QSB.DefaultSuggestionView";
    }

    public DefaultSuggestionView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.TAG = "QSB.DefaultSuggestionView";
    }

    public DefaultSuggestionView(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
        this.TAG = "QSB.DefaultSuggestionView";
    }

    private CharSequence formatText(String str, Suggestion suggestion) {
        return ("html".equals(suggestion.getSuggestionFormat()) && looksLikeHtml(str)) ? Html.fromHtml(str) : str;
    }

    private CharSequence formatUrl(CharSequence charSequence) {
        SpannableString spannableString = new SpannableString(charSequence);
        spannableString.setSpan(new TextAppearanceSpan(null, 0, 0, getResources().getColorStateList(2131165186), null), 0, charSequence.length(), 33);
        return spannableString;
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

    public static void setViewDrawable(ImageView imageView, Drawable drawable) {
        imageView.setImageDrawable(drawable);
        if (drawable == null) {
            imageView.setVisibility(8);
            return;
        }
        imageView.setVisibility(0);
        drawable.setVisible(false, false);
        drawable.setVisible(true, false);
    }

    @Override
    public void bindAsSuggestion(Suggestion suggestion, String str) {
        super.bindAsSuggestion(suggestion, str);
        CharSequence text = formatText(suggestion.getSuggestionText1(), suggestion);
        String suggestionText2Url = suggestion.getSuggestionText2Url();
        CharSequence url = suggestionText2Url != null ? formatUrl(suggestionText2Url) : formatText(suggestion.getSuggestionText2(), suggestion);
        if (TextUtils.isEmpty(url)) {
            this.mText1.setSingleLine(false);
            this.mText1.setMaxLines(2);
            this.mText1.setEllipsize(TextUtils.TruncateAt.START);
        } else {
            this.mText1.setSingleLine(true);
            this.mText1.setMaxLines(1);
            this.mText1.setEllipsize(TextUtils.TruncateAt.MIDDLE);
        }
        setText1(text);
        setText2(url);
        this.mAsyncIcon1.set(suggestion.getSuggestionSource(), suggestion.getSuggestionIcon1());
        this.mAsyncIcon2.set(suggestion.getSuggestionSource(), suggestion.getSuggestionIcon2());
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        this.mText1 = (TextView) findViewById(2131689499);
        this.mText2 = (TextView) findViewById(2131689498);
        this.mAsyncIcon1 = new AsyncIcon(this, this.mIcon1) {
            final DefaultSuggestionView this$0;

            {
                this.this$0 = this;
            }

            @Override
            protected Drawable getFallbackIcon(Source source) {
                return source.getSourceIcon();
            }

            @Override
            protected String getFallbackIconId(Source source) {
                return source.getSourceIconUri().toString();
            }
        };
        this.mAsyncIcon2 = new AsyncIcon(this, this.mIcon2);
    }
}
