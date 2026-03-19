package com.android.deskclock.ringtone;

import android.graphics.PorterDuff;
import android.support.v4.content.ContextCompat;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import com.android.deskclock.AnimatorUtils;
import com.android.deskclock.ItemAdapter;
import com.android.deskclock.R;
import com.android.deskclock.ThemeUtils;
import com.android.deskclock.Utils;

final class RingtoneViewHolder extends ItemAdapter.ItemViewHolder<RingtoneHolder> implements View.OnClickListener, View.OnCreateContextMenuListener {
    static final int CLICK_LONG_PRESS = -1;
    static final int CLICK_NORMAL = 0;
    static final int CLICK_NO_PERMISSIONS = -2;
    static final int VIEW_TYPE_CUSTOM_SOUND = -2131558500;
    static final int VIEW_TYPE_SYSTEM_SOUND = 2131558500;
    private final ImageView mImageView;
    private final TextView mNameView;
    private final View mSelectedView;

    private RingtoneViewHolder(View view) {
        super(view);
        view.setOnClickListener(this);
        this.mSelectedView = view.findViewById(R.id.sound_image_selected);
        this.mNameView = (TextView) view.findViewById(R.id.ringtone_name);
        this.mImageView = (ImageView) view.findViewById(R.id.ringtone_image);
    }

    @Override
    protected void onBindItemView(RingtoneHolder ringtoneHolder) {
        boolean z;
        this.mNameView.setText(ringtoneHolder.getName());
        if (ringtoneHolder.isSelected() || !ringtoneHolder.hasPermissions()) {
            z = true;
        } else {
            z = false;
        }
        this.mNameView.setAlpha(z ? 1.0f : 0.63f);
        this.mImageView.setAlpha(z ? 1.0f : 0.63f);
        this.mImageView.clearColorFilter();
        int itemViewType = getItemViewType();
        if (itemViewType == VIEW_TYPE_CUSTOM_SOUND) {
            if (!ringtoneHolder.hasPermissions()) {
                this.mImageView.setImageResource(R.drawable.ic_ringtone_not_found);
                this.mImageView.setColorFilter(ThemeUtils.resolveColor(this.itemView.getContext(), R.attr.colorAccent), PorterDuff.Mode.SRC_ATOP);
            } else {
                this.mImageView.setImageResource(R.drawable.placeholder_album_artwork);
            }
        } else if (ringtoneHolder.item == Utils.RINGTONE_SILENT) {
            this.mImageView.setImageResource(R.drawable.ic_ringtone_silent);
        } else if (ringtoneHolder.isPlaying()) {
            this.mImageView.setImageResource(R.drawable.ic_ringtone_active);
        } else {
            this.mImageView.setImageResource(R.drawable.ic_ringtone);
        }
        AnimatorUtils.startDrawableAnimation(this.mImageView);
        this.mSelectedView.setVisibility(ringtoneHolder.isSelected() ? 0 : 8);
        this.itemView.setBackgroundColor(ContextCompat.getColor(this.itemView.getContext(), ringtoneHolder.isSelected() ? R.color.white_08p : R.color.transparent));
        if (itemViewType == VIEW_TYPE_CUSTOM_SOUND) {
            this.itemView.setOnCreateContextMenuListener(this);
        }
    }

    @Override
    public void onClick(View view) {
        if (getItemHolder().hasPermissions()) {
            notifyItemClicked(0);
        } else {
            notifyItemClicked(-2);
        }
    }

    @Override
    public void onCreateContextMenu(ContextMenu contextMenu, View view, ContextMenu.ContextMenuInfo contextMenuInfo) {
        notifyItemClicked(-1);
        contextMenu.add(0, 0, 0, R.string.remove_sound);
    }

    public static class Factory implements ItemAdapter.ItemViewHolder.Factory {
        private final LayoutInflater mInflater;

        Factory(LayoutInflater layoutInflater) {
            this.mInflater = layoutInflater;
        }

        @Override
        public ItemAdapter.ItemViewHolder<?> createViewHolder(ViewGroup viewGroup, int i) {
            return new RingtoneViewHolder(this.mInflater.inflate(R.layout.ringtone_item_sound, viewGroup, false));
        }
    }
}
