package com.android.settings.password;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import com.android.settings.R;

/* loaded from: classes.dex */
public class PasswordRequirementAdapter extends RecyclerView.Adapter<PasswordRequirementViewHolder> {
    private String[] mRequirements;

    public PasswordRequirementAdapter() {
        setHasStableIds(true);
    }

    /* JADX DEBUG: Method merged with bridge method: onCreateViewHolder(Landroid/view/ViewGroup;I)Landroid/support/v7/widget/RecyclerView$ViewHolder; */
    @Override // android.support.v7.widget.RecyclerView.Adapter
    public PasswordRequirementViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        return new PasswordRequirementViewHolder(LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.password_requirement_item, viewGroup, false));
    }

    @Override // android.support.v7.widget.RecyclerView.Adapter
    public int getItemCount() {
        return this.mRequirements.length;
    }

    public void setRequirements(String[] strArr) {
        this.mRequirements = strArr;
        notifyDataSetChanged();
    }

    @Override // android.support.v7.widget.RecyclerView.Adapter
    public long getItemId(int i) {
        return this.mRequirements[i].hashCode();
    }

    /* JADX DEBUG: Method merged with bridge method: onBindViewHolder(Landroid/support/v7/widget/RecyclerView$ViewHolder;I)V */
    @Override // android.support.v7.widget.RecyclerView.Adapter
    public void onBindViewHolder(PasswordRequirementViewHolder passwordRequirementViewHolder, int i) {
        passwordRequirementViewHolder.mDescriptionText.setText(this.mRequirements[i]);
    }

    public static class PasswordRequirementViewHolder extends RecyclerView.ViewHolder {
        private TextView mDescriptionText;

        public PasswordRequirementViewHolder(View view) {
            super(view);
            this.mDescriptionText = (TextView) view;
        }
    }
}
