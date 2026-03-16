package com.android.ex.chips;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.StateListDrawable;
import android.net.Uri;
import android.text.TextUtils;
import android.text.util.Rfc822Tokenizer;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import com.android.ex.chips.Queries;

public class DropdownChipLayouter {
    private final Context mContext;
    private ChipDeleteListener mDeleteListener;
    private final LayoutInflater mInflater;
    private Queries.Query mQuery;

    public enum AdapterType {
        BASE_RECIPIENT,
        RECIPIENT_ALTERNATES,
        SINGLE_RECIPIENT
    }

    public interface ChipDeleteListener {
        void onChipDelete();
    }

    public DropdownChipLayouter(LayoutInflater inflater, Context context) {
        this.mInflater = inflater;
        this.mContext = context;
    }

    public void setQuery(Queries.Query query) {
        this.mQuery = query;
    }

    public void setDeleteListener(ChipDeleteListener listener) {
        this.mDeleteListener = listener;
    }

    public View bindView(View convertView, ViewGroup parent, RecipientEntry entry, int position, AdapterType type, String constraint) {
        return bindView(convertView, parent, entry, position, type, constraint, null);
    }

    public View bindView(View convertView, ViewGroup parent, RecipientEntry entry, int position, AdapterType type, String constraint, StateListDrawable deleteDrawable) {
        String displayName = entry.getDisplayName();
        String destination = entry.getDestination();
        boolean showImage = true;
        CharSequence destinationType = getDestinationType(entry);
        View itemView = reuseOrInflateView(convertView, parent, type);
        ViewHolder viewHolder = new ViewHolder(itemView);
        switch (type) {
            case BASE_RECIPIENT:
                if (TextUtils.isEmpty(displayName) || TextUtils.equals(displayName, destination)) {
                    displayName = destination;
                    if (entry.isFirstLevel()) {
                        destination = null;
                    }
                }
                if (!entry.isFirstLevel()) {
                    displayName = null;
                    showImage = false;
                }
                if (viewHolder.topDivider != null) {
                    viewHolder.topDivider.setVisibility(position == 0 ? 0 : 8);
                }
                break;
            case RECIPIENT_ALTERNATES:
                if (position != 0) {
                    displayName = null;
                    showImage = false;
                }
                break;
            case SINGLE_RECIPIENT:
                destination = Rfc822Tokenizer.tokenize(entry.getDestination())[0].getAddress();
                destinationType = null;
                break;
        }
        bindTextToView(displayName, viewHolder.displayNameView);
        bindTextToView(destination, viewHolder.destinationView);
        bindTextToView(destinationType, viewHolder.destinationTypeView);
        bindIconToView(showImage, entry, viewHolder.imageView, type);
        bindDrawableToDeleteView(deleteDrawable, viewHolder.deleteView);
        return itemView;
    }

    public View newView(AdapterType type) {
        return this.mInflater.inflate(getItemLayoutResId(type), (ViewGroup) null);
    }

    protected View reuseOrInflateView(View convertView, ViewGroup parent, AdapterType type) {
        int itemLayout = getItemLayoutResId(type);
        switch (type) {
            case SINGLE_RECIPIENT:
                itemLayout = getAlternateItemLayoutResId(type);
                break;
        }
        return convertView != null ? convertView : this.mInflater.inflate(itemLayout, parent, false);
    }

    protected void bindTextToView(CharSequence text, TextView view) {
        if (view != null) {
            if (text != null) {
                view.setText(text);
                view.setVisibility(0);
            } else {
                view.setVisibility(8);
            }
        }
    }

    protected void bindIconToView(boolean showImage, RecipientEntry entry, ImageView view, AdapterType type) {
        if (view != null) {
            if (showImage) {
                switch (type) {
                    case BASE_RECIPIENT:
                        byte[] photoBytes = entry.getPhotoBytes();
                        if (photoBytes != null && photoBytes.length > 0) {
                            Bitmap photo = BitmapFactory.decodeByteArray(photoBytes, 0, photoBytes.length);
                            view.setImageBitmap(photo);
                        } else {
                            view.setImageResource(getDefaultPhotoResId());
                        }
                        break;
                    case RECIPIENT_ALTERNATES:
                        Uri thumbnailUri = entry.getPhotoThumbnailUri();
                        if (thumbnailUri != null) {
                            view.setImageURI(thumbnailUri);
                        } else {
                            view.setImageResource(getDefaultPhotoResId());
                        }
                        break;
                }
                view.setVisibility(0);
                return;
            }
            view.setVisibility(8);
        }
    }

    protected void bindDrawableToDeleteView(final StateListDrawable drawable, ImageView view) {
        if (view != null) {
            if (drawable == null) {
                view.setVisibility(8);
            }
            view.setImageDrawable(drawable);
            if (this.mDeleteListener != null) {
                view.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view2) {
                        if (drawable.getCurrent() != null) {
                            DropdownChipLayouter.this.mDeleteListener.onChipDelete();
                        }
                    }
                });
            }
        }
    }

    protected CharSequence getDestinationType(RecipientEntry entry) {
        return this.mQuery.getTypeLabel(this.mContext.getResources(), entry.getDestinationType(), entry.getDestinationLabel()).toString().toUpperCase();
    }

    protected int getItemLayoutResId(AdapterType type) {
        switch (type) {
            case BASE_RECIPIENT:
                return R.layout.chips_autocomplete_recipient_dropdown_item;
            case RECIPIENT_ALTERNATES:
                return R.layout.chips_recipient_dropdown_item;
            default:
                return R.layout.chips_recipient_dropdown_item;
        }
    }

    protected int getAlternateItemLayoutResId(AdapterType type) {
        switch (type) {
            case BASE_RECIPIENT:
                return R.layout.chips_autocomplete_recipient_dropdown_item;
            case RECIPIENT_ALTERNATES:
                return R.layout.chips_recipient_dropdown_item;
            default:
                return R.layout.chips_recipient_dropdown_item;
        }
    }

    protected int getDefaultPhotoResId() {
        return R.drawable.ic_contact_picture;
    }

    protected int getDisplayNameResId() {
        return android.R.id.title;
    }

    protected int getDestinationResId() {
        return android.R.id.text1;
    }

    protected int getDestinationTypeResId() {
        return android.R.id.text2;
    }

    protected int getPhotoResId() {
        return android.R.id.icon;
    }

    protected int getDeleteResId() {
        return android.R.id.icon1;
    }

    protected class ViewHolder {
        public final ImageView deleteView;
        public final TextView destinationTypeView;
        public final TextView destinationView;
        public final TextView displayNameView;
        public final ImageView imageView;
        public final View topDivider;

        public ViewHolder(View view) {
            this.displayNameView = (TextView) view.findViewById(DropdownChipLayouter.this.getDisplayNameResId());
            this.destinationView = (TextView) view.findViewById(DropdownChipLayouter.this.getDestinationResId());
            this.destinationTypeView = (TextView) view.findViewById(DropdownChipLayouter.this.getDestinationTypeResId());
            this.imageView = (ImageView) view.findViewById(DropdownChipLayouter.this.getPhotoResId());
            this.deleteView = (ImageView) view.findViewById(DropdownChipLayouter.this.getDeleteResId());
            this.topDivider = view.findViewById(R.id.chip_autocomplete_top_divider);
        }
    }
}
