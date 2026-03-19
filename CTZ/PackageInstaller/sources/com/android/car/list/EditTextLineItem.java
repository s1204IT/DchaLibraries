package com.android.car.list;

import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import com.android.car.list.EditTextLineItem.ViewHolder;
import com.android.car.list.TypedPagedListAdapter;

public class EditTextLineItem<VH extends ViewHolder> extends TypedPagedListAdapter.LineItem<VH> {
    private EditText mEditText;
    private final CharSequence mInitialInputText;
    private TextChangeListener mTextChangeListener;
    protected TextType mTextType;
    private final CharSequence mTitle;

    public interface TextChangeListener {
        void textChanged(Editable editable);
    }

    public enum TextType {
        NONE(0),
        TEXT(1),
        HIDDEN_PASSWORD(129),
        VISIBLE_PASSWORD(145);

        private int mValue;

        TextType(int i) {
            this.mValue = i;
        }

        public int getValue() {
            return this.mValue;
        }
    }

    public void setTextType(TextType textType) {
        this.mTextType = textType;
    }

    @Override
    public int getType() {
        return 7;
    }

    @Override
    public void bindViewHolder(VH vh) {
        super.bindViewHolder(vh);
        vh.titleView.setText(this.mTitle);
        this.mEditText = vh.editText;
        this.mEditText.setInputType(this.mTextType.getValue());
        if (this.mInitialInputText != null) {
            this.mEditText.setText(this.mInitialInputText);
        }
        this.mEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {
            }

            @Override
            public void afterTextChanged(Editable editable) {
                if (EditTextLineItem.this.mTextChangeListener != null) {
                    EditTextLineItem.this.mTextChangeListener.textChanged(editable);
                }
            }
        });
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public final EditText editText;
        public final TextView titleView;

        public ViewHolder(View view) {
            super(view);
            this.titleView = (TextView) view.findViewById(R.id.title);
            this.editText = (EditText) view.findViewById(R.id.input);
        }
    }

    public static RecyclerView.ViewHolder createViewHolder(ViewGroup viewGroup) {
        return new ViewHolder(LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.edit_text_line_item, viewGroup, false));
    }

    @Override
    public CharSequence getDesc() {
        return null;
    }

    @Override
    public boolean isExpandable() {
        return false;
    }
}
