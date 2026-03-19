package com.mediatek.widget;

import android.R;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.mediatek.widget.AccountViewAdapter;

public class AccountItemView extends LinearLayout {
    private ImageView mAccountIcon;
    private TextView mAccountName;
    private TextView mAccountNumber;

    public AccountItemView(Context context) {
        this(context, null);
    }

    public AccountItemView(Context context, AttributeSet attrs) {
        super(context, attrs);
        LayoutInflater inflator = (LayoutInflater) context.getSystemService("layout_inflater");
        View view = inflator.inflate(134676515, (ViewGroup) null);
        addView(view);
        initViewItem(view);
    }

    private void initViewItem(View view) {
        this.mAccountIcon = (ImageView) view.findViewById(R.id.icon);
        this.mAccountName = (TextView) view.findViewById(R.id.title);
        this.mAccountNumber = (TextView) view.findViewById(R.id.summary);
    }

    public void setViewItem(AccountViewAdapter.AccountElements element) {
        Drawable drawable = element.getDrawable();
        if (drawable != null) {
            setAccountIcon(drawable);
        } else {
            setAccountIcon(element.getIcon());
        }
        setAccountName(element.getName());
        setAccountNumber(element.getNumber());
    }

    public void setAccountIcon(int resId) {
        this.mAccountIcon.setImageResource(resId);
    }

    public void setAccountIcon(Drawable drawable) {
        this.mAccountIcon.setImageDrawable(drawable);
    }

    public void setAccountIcon(Bitmap bitmap) {
        this.mAccountIcon.setImageBitmap(bitmap);
    }

    public void setAccountName(String name) {
        setText(this.mAccountName, name);
    }

    public void setAccountNumber(String number) {
        setText(this.mAccountNumber, number);
    }

    private void setText(TextView view, String text) {
        if (TextUtils.isEmpty(text)) {
            view.setVisibility(8);
        } else {
            view.setText(text);
            view.setVisibility(0);
        }
    }
}
