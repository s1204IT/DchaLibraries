package com.android.settings.password;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import com.android.settings.R;

public class PasswordRequirementAdapter extends RecyclerView.Adapter<PasswordRequirementViewHolder> {
    private String[] mRequirements;

    public PasswordRequirementAdapter() {
        setHasStableIds(true);
    }

    @Override
    public PasswordRequirementViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        return new PasswordRequirementViewHolder(LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.password_requirement_item, viewGroup, false));
    }

    @Override
    public int getItemCount() {
        return this.mRequirements.length;
    }

    public void setRequirements(String[] strArr) {
        this.mRequirements = strArr;
        notifyDataSetChanged();
    }

    @Override
    public long getItemId(int i) {
        return this.mRequirements[i].hashCode();
    }

    @Override
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
