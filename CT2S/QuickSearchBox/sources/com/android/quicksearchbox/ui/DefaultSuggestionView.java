package com.android.quicksearchbox.ui;

import android.content.Context;
import android.content.res.ColorStateList;
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

    public DefaultSuggestionView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.TAG = "QSB.DefaultSuggestionView";
    }

    public DefaultSuggestionView(Context context, AttributeSet attrs) {
        super(context, attrs);
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
    public void bindAsSuggestion(Suggestion suggestion, String userQuery) {
        CharSequence text2;
        super.bindAsSuggestion(suggestion, userQuery);
        CharSequence text1 = formatText(suggestion.getSuggestionText1(), suggestion);
        CharSequence text22 = suggestion.getSuggestionText2Url();
        if (text22 != null) {
            text2 = formatUrl(text22);
        } else {
            text2 = formatText(suggestion.getSuggestionText2(), suggestion);
        }
        if (TextUtils.isEmpty(text2)) {
            this.mText1.setSingleLine(false);
            this.mText1.setMaxLines(2);
            this.mText1.setEllipsize(TextUtils.TruncateAt.START);
        } else {
            this.mText1.setSingleLine(true);
            this.mText1.setMaxLines(1);
            this.mText1.setEllipsize(TextUtils.TruncateAt.MIDDLE);
        }
        setText1(text1);
        setText2(text2);
        this.mAsyncIcon1.set(suggestion.getSuggestionSource(), suggestion.getSuggestionIcon1());
        this.mAsyncIcon2.set(suggestion.getSuggestionSource(), suggestion.getSuggestionIcon2());
    }

    private CharSequence formatUrl(CharSequence url) {
        SpannableString text = new SpannableString(url);
        ColorStateList colors = getResources().getColorStateList(R.color.url_text);
        text.setSpan(new TextAppearanceSpan(null, 0, 0, colors, null), 0, url.length(), 33);
        return text;
    }

    private CharSequence formatText(String str, Suggestion suggestion) {
        boolean isHtml = "html".equals(suggestion.getSuggestionFormat());
        if (isHtml && looksLikeHtml(str)) {
            return Html.fromHtml(str);
        }
        return str;
    }

    private boolean looksLikeHtml(String str) {
        if (TextUtils.isEmpty(str)) {
            return false;
        }
        for (int i = str.length() - 1; i >= 0; i--) {
            char c = str.charAt(i);
            if (c == '>' || c == '&') {
                return true;
            }
        }
        return false;
    }

    private static void setViewDrawable(ImageView v, Drawable drawable) {
        v.setImageDrawable(drawable);
        if (drawable == null) {
            v.setVisibility(8);
            return;
        }
        v.setVisibility(0);
        drawable.setVisible(false, false);
        drawable.setVisible(true, false);
    }

    private class AsyncIcon {
        private String mCurrentId;
        private final ImageView mView;
        private String mWantedId;

        public AsyncIcon(ImageView view) {
            this.mView = view;
        }

        public void set(final Source source, String sourceIconId) {
            if (sourceIconId != null) {
                Uri iconUri = source.getIconUri(sourceIconId);
                final String uniqueIconId = iconUri != null ? iconUri.toString() : null;
                this.mWantedId = uniqueIconId;
                if (!TextUtils.equals(this.mWantedId, this.mCurrentId)) {
                    NowOrLater<Drawable> icon = source.getIcon(sourceIconId);
                    if (icon.haveNow()) {
                        handleNewDrawable(icon.getNow(), uniqueIconId, source);
                        return;
                    } else {
                        clearDrawable();
                        icon.getLater(new Consumer<Drawable>() {
                            @Override
                            public boolean consume(Drawable icon2) {
                                if (!TextUtils.equals(uniqueIconId, AsyncIcon.this.mWantedId)) {
                                    return false;
                                }
                                AsyncIcon.this.handleNewDrawable(icon2, uniqueIconId, source);
                                return true;
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

        private void handleNewDrawable(Drawable icon, String id, Source source) {
            if (icon == null) {
                this.mWantedId = getFallbackIconId(source);
                if (!TextUtils.equals(this.mWantedId, this.mCurrentId)) {
                    icon = getFallbackIcon(source);
                } else {
                    return;
                }
            }
            setDrawable(icon, id);
        }

        private void setDrawable(Drawable icon, String id) {
            this.mCurrentId = id;
            DefaultSuggestionView.setViewDrawable(this.mView, icon);
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
