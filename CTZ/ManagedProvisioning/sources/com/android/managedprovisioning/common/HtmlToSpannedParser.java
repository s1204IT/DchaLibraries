package com.android.managedprovisioning.common;

import android.content.Intent;
import android.text.Html;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.URLSpan;
import com.android.internal.util.Preconditions;

public class HtmlToSpannedParser {
    private final ClickableSpanFactory mClickableSpanFactory;
    private final UrlIntentFactory mUrlIntentFactory;

    public interface UrlIntentFactory {
        Intent create(String str);
    }

    public HtmlToSpannedParser(ClickableSpanFactory clickableSpanFactory, UrlIntentFactory urlIntentFactory) {
        this.mClickableSpanFactory = (ClickableSpanFactory) Preconditions.checkNotNull(clickableSpanFactory);
        this.mUrlIntentFactory = (UrlIntentFactory) Preconditions.checkNotNull(urlIntentFactory);
    }

    public Spanned parseHtml(String str) {
        Spanned spannedFromHtml = Html.fromHtml((String) Preconditions.checkStringNotEmpty(str), 63);
        if (spannedFromHtml == null) {
            return null;
        }
        SpannableStringBuilder spannableStringBuilder = new SpannableStringBuilder(spannedFromHtml);
        for (URLSpan uRLSpan : (URLSpan[]) spannableStringBuilder.getSpans(0, spannableStringBuilder.length(), URLSpan.class)) {
            Intent intentCreate = this.mUrlIntentFactory.create(uRLSpan.getURL());
            if (intentCreate != null) {
                spannableStringBuilder.setSpan(this.mClickableSpanFactory.create(intentCreate), spannableStringBuilder.getSpanStart(uRLSpan), spannableStringBuilder.getSpanEnd(uRLSpan), 33);
                spannableStringBuilder.removeSpan(uRLSpan);
            }
        }
        return spannableStringBuilder;
    }
}
