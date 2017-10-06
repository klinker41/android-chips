package com.android.ex.chips;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.text.TextUtils;
import android.text.util.Rfc822Tokenizer;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.ex.chips.Queries.Query;

/**
 * A class that inflates and binds the views in the dropdown list from
 * RecipientEditTextView.
 */
public class DropdownChipLayouter {
    /**
     * The type of adapter that is requesting a chip layout.
     */
    public enum AdapterType {
        BASE_RECIPIENT,
        RECIPIENT_ALTERNATES,
        SINGLE_RECIPIENT
    }

    private final LayoutInflater mInflater;
    private final Context mContext;
    private Query mQuery;

    public DropdownChipLayouter(LayoutInflater inflater, Context context) {
        mInflater = inflater;
        mContext = context;
    }

    public void setQuery(Query query) {
        mQuery = query;
    }


    /**
     * Layouts and binds recipient information to the view. If convertView is null, inflates a new
     * view with getItemLaytout().
     *
     * @param convertView The view to bind information to.
     * @param parent The parent to bind the view to if we inflate a new view.
     * @param entry The recipient entry to get information from.
     * @param position The position in the list.
     * @param type The adapter type that is requesting the bind.
     * @param constraint The constraint typed in the auto complete view.
     *
     * @return A view ready to be shown in the drop down list.
     */
    public View bindView(View convertView, ViewGroup parent, RecipientEntry entry, int position,
        AdapterType type, String constraint) {
        // Default to show all the information
        String displayName = entry.getDisplayName();
        String destination = entry.getDestination();
        boolean showImage = true;
        CharSequence destinationType = getDestinationType(entry);

        final View itemView = reuseOrInflateView(convertView, parent, type);

        final ViewHolder viewHolder = new ViewHolder(itemView);

        // Hide some information depending on the entry type and adapter type
        switch (type) {
            case BASE_RECIPIENT:
                if (TextUtils.isEmpty(displayName) || TextUtils.equals(displayName, destination)) {
                    displayName = destination;

                    // We only show the destination for secondary entries, so clear it only for the
                    // first level.
                    if (entry.isFirstLevel()) {
                        destination = null;
                    }
                }

                if (!entry.isFirstLevel()) {
                    displayName = null;
                    showImage = false;
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
        }

        if (displayName == null && !showImage) {
            viewHolder.destinationView.setPadding(mContext.getResources().getDimensionPixelSize(R.dimen.padding_no_picture), 0, 0, 0);
        } else {
            viewHolder.destinationView.setPadding(0, 0, 0, 0);
        }

        // Bind the information to the view
        bindTextToView(displayName, viewHolder.displayNameView);
        bindTextToView(destination, viewHolder.destinationView);
        bindTextToView("(" + destinationType + ")", viewHolder.destinationTypeView);
        bindIconToView(showImage, entry, viewHolder.imageView, type);

        return itemView;
    }

    /**
     * Returns a new view with {@link #getItemLayoutResId()}.
     */
    public View newView() {
        return mInflater.inflate(getItemLayoutResId(), null);
    }

    /**
     * Returns the same view, or inflates a new one if the given view was null.
     */
    protected View reuseOrInflateView(View convertView, ViewGroup parent, AdapterType type) {
        int itemLayout = getItemLayoutResId();
        switch (type) {
            case BASE_RECIPIENT:
            case RECIPIENT_ALTERNATES:
                break;
            case SINGLE_RECIPIENT:
                itemLayout = getAlternateItemLayoutResId();
                break;
        }
        return convertView != null ? convertView : mInflater.inflate(itemLayout, parent, false);
    }

    /**
     * Binds the text to the given text view. If the text was null, hides the text view.
     */
    protected void bindTextToView(CharSequence text, TextView view) {
        if (view == null) {
            return;
        }

        if (text != null) {
            view.setText(text);
            view.setVisibility(View.VISIBLE);
        } else {
            view.setVisibility(View.GONE);
        }
    }

    /**
     * Binds the avatar icon to the image view. If we don't want to show the image, hides the
     * image view.
     */
    protected void bindIconToView(boolean showImage, RecipientEntry entry, ImageView view,
        AdapterType type) {
        if (view == null) {
            return;
        }

        if (showImage) {
            switch (type) {
                case BASE_RECIPIENT:
                    byte[] photoBytes = entry.getPhotoBytes();
                    if (photoBytes != null && photoBytes.length > 0) {
                        final Bitmap photo = ChipsUtil.getClip(BitmapFactory.decodeByteArray(photoBytes, 0,
                            photoBytes.length));
                        view.setImageBitmap(photo);
                    } else {
                        final Bitmap photo = ContactImageCreator.getLetterPicture(mContext, entry);
                        view.setImageBitmap(photo);
                    }
                    break;
                case RECIPIENT_ALTERNATES:
                    Uri thumbnailUri = entry.getPhotoThumbnailUri();
                    if (thumbnailUri != null) {
                        // TODO: see if this needs to be done outside the main thread
                        // as it may be too slow to get immediately.
                        view.setImageURI(thumbnailUri);
                    } else {
                        final Bitmap photo = ContactImageCreator.getLetterPicture(mContext, entry);
                        view.setImageBitmap(photo);
                    }
                    break;
                case SINGLE_RECIPIENT:
                default:
                    break;
            }
            view.setVisibility(View.VISIBLE);
        } else {
            view.setVisibility(View.GONE);
        }
    }

    protected CharSequence getDestinationType(RecipientEntry entry) {
        return mQuery.getTypeLabel(mContext.getResources(), entry.getDestinationType(),
            entry.getDestinationLabel()).toString().toUpperCase();
    }

    /**
     * Returns a layout id for each item inside auto-complete list.
     *
     * Each View must contain two TextViews (for display name and destination) and one ImageView
     * (for photo). Ids for those should be available via {@link #getDisplayNameResId()},
     * {@link #getDestinationResId()}, and {@link #getPhotoResId()}.
     */
    protected int getItemLayoutResId() {
        return R.layout.chips_recipient_dropdown_item;
    }

    /**
     * Returns a layout id for each item inside alternate auto-complete list.
     *
     * Each View must contain two TextViews (for display name and destination) and one ImageView
     * (for photo). Ids for those should be available via {@link #getDisplayNameResId()},
     * {@link #getDestinationResId()}, and {@link #getPhotoResId()}.
     */
    protected int getAlternateItemLayoutResId() {
        return R.layout.chips_alternate_item;
    }

    /**
     * Returns a resource ID representing an image which should be shown when ther's no relevant
     * photo is available.
     */
    protected int getDefaultPhotoResId() {
        return R.drawable.ic_contact_picture;
    }

    /**
     * Returns an id for TextView in an item View for showing a display name. By default
     * {@link android.R.id#title} is returned.
     */
    protected int getDisplayNameResId() {
        return android.R.id.title;
    }

    /**
     * Returns an id for TextView in an item View for showing a destination
     * (an email address or a phone number).
     * By default {@link android.R.id#text1} is returned.
     */
    protected int getDestinationResId() {
        return android.R.id.text1;
    }

    /**
     * Returns an id for TextView in an item View for showing the type of the destination.
     * By default {@link android.R.id#text2} is returned.
     */
    protected int getDestinationTypeResId() {
        return android.R.id.text2;
    }

    /**
     * Returns an id for ImageView in an item View for showing photo image for a person. In default
     * {@link android.R.id#icon} is returned.
     */
    protected int getPhotoResId() {
        return android.R.id.icon;
    }

    /**
     * A holder class the view. Uses the getters in DropdownChipLayouter to find the id of the
     * corresponding views.
     */
    protected class ViewHolder {
        public final TextView displayNameView;
        public final TextView destinationView;
        public final TextView destinationTypeView;
        public final ImageView imageView;

        public ViewHolder(View view) {
            displayNameView = (TextView) view.findViewById(getDisplayNameResId());
            destinationView = (TextView) view.findViewById(getDestinationResId());
            destinationTypeView = (TextView) view.findViewById(getDestinationTypeResId());
            imageView = (ImageView) view.findViewById(getPhotoResId());
        }
    }
}
