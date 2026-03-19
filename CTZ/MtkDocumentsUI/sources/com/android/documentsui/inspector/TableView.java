package com.android.documentsui.inspector;

import android.content.Context;
import android.content.res.Resources;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.textclassifier.TextClassifier;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.android.documentsui.R;
import com.android.documentsui.inspector.InspectorController;
import java.util.HashMap;
import java.util.Map;

public class TableView extends LinearLayout implements InspectorController.TableDisplay {
    private final TextClassifier mClassifier;
    private final LayoutInflater mInflater;
    private final Resources mRes;
    private final Map<CharSequence, KeyValueRow> mRows;
    private final Map<CharSequence, TextView> mTitles;

    public TableView(Context context) {
        this(context, null);
    }

    public TableView(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, 0);
    }

    public TableView(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
        this.mRows = new HashMap();
        this.mTitles = new HashMap();
        this.mInflater = (LayoutInflater) context.getSystemService("layout_inflater");
        this.mRes = context.getResources();
        this.mClassifier = GpsCoordinatesTextClassifier.create(context);
    }

    void setTitle(int i, boolean z) {
        putTitle(this.mContext.getResources().getString(i), z);
    }

    protected void putTitle(CharSequence charSequence, boolean z) {
        TextView textView;
        TextView textView2 = this.mTitles.get(charSequence);
        if (textView2 == null) {
            LinearLayout linearLayout = (LinearLayout) this.mInflater.inflate(R.layout.inspector_section_title, (ViewGroup) null);
            if (!z) {
                linearLayout.setDividerDrawable(null);
            }
            textView = (TextView) linearLayout.findViewById(R.id.inspector_header_title);
            addView(linearLayout);
            this.mTitles.put(charSequence, textView);
        } else {
            textView = textView2;
        }
        textView.setText(charSequence);
    }

    protected KeyValueRow createKeyValueRow(ViewGroup viewGroup) {
        KeyValueRow keyValueRow = (KeyValueRow) this.mInflater.inflate(R.layout.table_key_value_row, (ViewGroup) null);
        viewGroup.addView(keyValueRow);
        keyValueRow.setTextClassifier(this.mClassifier);
        return keyValueRow;
    }

    @Override
    public void put(int i, CharSequence charSequence) {
        put(this.mRes.getString(i), charSequence);
    }

    protected KeyValueRow put(CharSequence charSequence, CharSequence charSequence2) {
        KeyValueRow keyValueRowCreateKeyValueRow = this.mRows.get(charSequence);
        if (keyValueRowCreateKeyValueRow == null) {
            keyValueRowCreateKeyValueRow = createKeyValueRow(this);
            keyValueRowCreateKeyValueRow.setKey(charSequence);
            this.mRows.put(charSequence, keyValueRowCreateKeyValueRow);
        } else {
            keyValueRowCreateKeyValueRow.removeOnClickListener();
        }
        keyValueRowCreateKeyValueRow.setValue(charSequence2);
        keyValueRowCreateKeyValueRow.setTextClassifier(this.mClassifier);
        return keyValueRowCreateKeyValueRow;
    }

    @Override
    public void put(int i, CharSequence charSequence, View.OnClickListener onClickListener) {
        put(i, charSequence);
        this.mRows.get(this.mRes.getString(i)).setOnClickListener(onClickListener);
    }

    public boolean isEmpty() {
        return this.mRows.isEmpty();
    }

    @Override
    public void setVisible(boolean z) {
        setVisibility(z ? 0 : 8);
    }
}
