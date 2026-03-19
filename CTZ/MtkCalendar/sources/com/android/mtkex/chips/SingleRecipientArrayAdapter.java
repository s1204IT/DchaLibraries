package com.android.mtkex.chips;

import android.content.Context;
import android.text.util.Rfc822Token;
import android.text.util.Rfc822Tokenizer;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

class SingleRecipientArrayAdapter extends ArrayAdapter<RecipientEntry> {
    private int mLayoutId;
    private final LayoutInflater mLayoutInflater;

    public SingleRecipientArrayAdapter(Context context, int i, RecipientEntry recipientEntry) {
        super(context, i, new RecipientEntry[]{recipientEntry});
        this.mLayoutInflater = LayoutInflater.from(context);
        this.mLayoutId = i;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        if (view == null) {
            view = newView();
        }
        bindView(view, getItem(i));
        return view;
    }

    private View newView() {
        return this.mLayoutInflater.inflate(this.mLayoutId, (ViewGroup) null);
    }

    private static void bindView(View view, RecipientEntry recipientEntry) {
        TextView textView = (TextView) view.findViewById(android.R.id.title);
        ImageView imageView = (ImageView) view.findViewById(android.R.id.icon);
        textView.setText(recipientEntry.getDisplayName());
        textView.setVisibility(0);
        imageView.setVisibility(0);
        TextView textView2 = (TextView) view.findViewById(android.R.id.text1);
        Rfc822Token[] rfc822TokenArr = Rfc822Tokenizer.tokenize(recipientEntry.getDestination());
        String address = "";
        if (rfc822TokenArr != null && rfc822TokenArr.length > 0) {
            address = rfc822TokenArr[0].getAddress();
        }
        textView2.setText(address);
    }
}
