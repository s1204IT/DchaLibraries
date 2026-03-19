package com.android.car.list;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import com.android.car.list.EditTextLineItem;

public class PasswordLineItem extends EditTextLineItem<ViewHolder> {
    private boolean mShowPassword;

    @Override
    public int getType() {
        return 10;
    }

    public static class ViewHolder extends EditTextLineItem.ViewHolder {
        public final CheckBox checkbox;

        public ViewHolder(View view) {
            super(view);
            this.checkbox = (CheckBox) view.findViewById(R.id.checkbox);
        }
    }

    @Override
    public void bindViewHolder(final ViewHolder viewHolder) {
        super.setTextType(this.mShowPassword ? EditTextLineItem.TextType.VISIBLE_PASSWORD : EditTextLineItem.TextType.HIDDEN_PASSWORD);
        super.bindViewHolder(viewHolder);
        viewHolder.checkbox.setChecked(this.mShowPassword);
        viewHolder.checkbox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public final void onCheckedChanged(CompoundButton compoundButton, boolean z) {
                PasswordLineItem.lambda$bindViewHolder$0(this.f$0, viewHolder, compoundButton, z);
            }
        });
    }

    public static void lambda$bindViewHolder$0(PasswordLineItem passwordLineItem, ViewHolder viewHolder, CompoundButton compoundButton, boolean z) {
        passwordLineItem.mShowPassword = z;
        super.setTextType(passwordLineItem.mShowPassword ? EditTextLineItem.TextType.VISIBLE_PASSWORD : EditTextLineItem.TextType.HIDDEN_PASSWORD);
        viewHolder.editText.setInputType(passwordLineItem.mTextType.getValue());
    }

    @Override
    public void setTextType(EditTextLineItem.TextType textType) {
        throw new IllegalArgumentException("checkbox will automatically set TextType.");
    }

    public static RecyclerView.ViewHolder createViewHolder(ViewGroup viewGroup) {
        return new ViewHolder(LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.password_line_item, viewGroup, false));
    }
}
