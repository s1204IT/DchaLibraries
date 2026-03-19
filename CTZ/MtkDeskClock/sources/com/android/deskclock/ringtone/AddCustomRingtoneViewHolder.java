package com.android.deskclock.ringtone;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import com.android.deskclock.ItemAdapter;
import com.android.deskclock.R;

final class AddCustomRingtoneViewHolder extends ItemAdapter.ItemViewHolder<AddCustomRingtoneHolder> implements View.OnClickListener {
    static final int CLICK_ADD_NEW = Integer.MIN_VALUE;
    static final int VIEW_TYPE_ADD_NEW = Integer.MIN_VALUE;

    private AddCustomRingtoneViewHolder(View view) {
        super(view);
        view.setOnClickListener(this);
        view.findViewById(R.id.sound_image_selected).setVisibility(8);
        TextView textView = (TextView) view.findViewById(R.id.ringtone_name);
        textView.setText(view.getContext().getString(R.string.add_new_sound));
        textView.setAlpha(0.63f);
        ImageView imageView = (ImageView) view.findViewById(R.id.ringtone_image);
        imageView.setImageResource(R.drawable.ic_add_white_24dp);
        imageView.setAlpha(0.63f);
    }

    @Override
    public void onClick(View view) {
        notifyItemClicked(Integer.MIN_VALUE);
    }

    public static class Factory implements ItemAdapter.ItemViewHolder.Factory {
        private final LayoutInflater mInflater;

        Factory(LayoutInflater layoutInflater) {
            this.mInflater = layoutInflater;
        }

        @Override
        public ItemAdapter.ItemViewHolder<?> createViewHolder(ViewGroup viewGroup, int i) {
            return new AddCustomRingtoneViewHolder(this.mInflater.inflate(R.layout.ringtone_item_sound, viewGroup, false));
        }
    }
}
