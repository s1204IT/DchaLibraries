package com.android.documentsui.dirlist;

import android.content.Context;
import android.database.Cursor;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import com.android.documentsui.R;

final class InflateMessageDocumentHolder extends MessageHolder {
    private ImageView mImageView;
    private Message mMessage;
    private TextView mMsgView;

    public InflateMessageDocumentHolder(Context context, ViewGroup viewGroup) {
        super(context, viewGroup, R.layout.item_doc_inflated_message);
        this.mMsgView = (TextView) this.itemView.findViewById(R.id.message);
        this.mImageView = (ImageView) this.itemView.findViewById(R.id.artwork);
    }

    public void bind(Message message) {
        this.mMessage = message;
        bind(null, null);
    }

    @Override
    public void bind(Cursor cursor, String str) {
        this.mMsgView.setText(this.mMessage.getMessageString());
        this.mImageView.setImageDrawable(this.mMessage.getIcon());
    }
}
