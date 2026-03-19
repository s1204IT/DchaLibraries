package com.android.documentsui.dirlist;

import android.content.Context;
import android.database.Cursor;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import com.android.documentsui.R;

final class HeaderMessageDocumentHolder extends MessageHolder {
    private final Button mButton;
    private final ImageView mIcon;
    private Message mMessage;
    private final TextView mTextView;

    public HeaderMessageDocumentHolder(Context context, ViewGroup viewGroup) {
        super(context, viewGroup, R.layout.item_doc_header_message);
        this.mIcon = (ImageView) this.itemView.findViewById(R.id.message_icon);
        this.mTextView = (TextView) this.itemView.findViewById(R.id.message_textview);
        this.mButton = (Button) this.itemView.findViewById(R.id.button_dismiss);
    }

    public void bind(Message message) {
        this.mMessage = message;
        this.mButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public final void onClick(View view) {
                this.f$0.onButtonClick(view);
            }
        });
        bind(null, null);
    }

    private void onButtonClick(View view) {
        this.mMessage.runCallback();
    }

    @Override
    public void bind(Cursor cursor, String str) {
        this.mTextView.setText(this.mMessage.getMessageString());
        this.mIcon.setImageDrawable(this.mMessage.getIcon());
        if (this.mMessage.getButtonString() != null) {
            this.mButton.setText(this.mMessage.getButtonString());
        }
    }
}
