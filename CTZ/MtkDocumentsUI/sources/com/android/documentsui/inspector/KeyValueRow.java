package com.android.documentsui.inspector;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.text.Selection;
import android.text.Spannable;
import android.util.AttributeSet;
import android.view.View;
import android.view.textclassifier.TextClassifier;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.android.documentsui.R;

public class KeyValueRow extends LinearLayout {
    private TextClassifier mClassifier;
    private ColorStateList mDefaultTextColor;
    private final Resources mRes;

    public KeyValueRow(Context context) {
        this(context, null);
    }

    public KeyValueRow(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, 0);
    }

    public KeyValueRow(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
        this.mRes = context.getResources();
    }

    public void setTextClassifier(TextClassifier textClassifier) {
        this.mClassifier = textClassifier;
    }

    public void setKey(CharSequence charSequence) {
        ((TextView) findViewById(R.id.table_row_key)).setText(charSequence);
    }

    public void setValue(CharSequence charSequence) {
        final TextView textView = (TextView) findViewById(R.id.table_row_value);
        textView.setText(charSequence);
        textView.setTextClassifier(this.mClassifier);
        textView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public final boolean onLongClick(View view) {
                return KeyValueRow.lambda$setValue$0(textView, view);
            }
        });
    }

    static boolean lambda$setValue$0(TextView textView, View view) {
        CharSequence text = textView.getText();
        if (text instanceof Spannable) {
            Selection.selectAll((Spannable) text);
            return false;
        }
        return false;
    }

    @Override
    public void setOnClickListener(View.OnClickListener onClickListener) {
        TextView textView = (TextView) findViewById(R.id.table_row_value);
        this.mDefaultTextColor = textView.getTextColors();
        textView.setTextColor(R.color.inspector_link);
        textView.setPaintFlags(textView.getPaintFlags() | 8);
        textView.setOnClickListener(onClickListener);
    }

    public void removeOnClickListener() {
        TextView textView = (TextView) findViewById(R.id.table_row_value);
        if (this.mDefaultTextColor != null) {
            textView.setTextColor(this.mDefaultTextColor);
        }
        textView.setPaintFlags(textView.getPaintFlags() & (-9));
        textView.setOnClickListener(null);
    }
}
