/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.ex.chips.recipientchip;

import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.text.style.DynamicDrawableSpan;
import android.text.style.ImageSpan;

import com.android.ex.chips.RecipientEntry;

/**
 * VisibleRecipientChip defines an ImageSpan that contains information relevant to a
 * particular recipient and renders a background asset to go with it.
 */
public class VisibleRecipientChip extends ImageSpan implements DrawableRecipientChip {
    private final SimpleRecipientChip mDelegate;

    public VisibleRecipientChip(final Drawable drawable, final RecipientEntry entry) {
        this(drawable, entry, DynamicDrawableSpan.ALIGN_BOTTOM);
    }

    public VisibleRecipientChip(final Drawable drawable, final RecipientEntry entry,
            final int verticalAlignment) {
        super(drawable, verticalAlignment);

        mDelegate = new SimpleRecipientChip(entry);
    }

    @Override
    public void setSelected(final boolean selected) {
        mDelegate.setSelected(selected);
    }

    @Override
    public boolean isSelected() {
        return mDelegate.isSelected();
    }

    @Override
    public CharSequence getDisplay() {
        return mDelegate.getDisplay();
    }

    @Override
    public CharSequence getValue() {
        return mDelegate.getValue();
    }

    @Override
    public long getContactId() {
        return mDelegate.getContactId();
    }

    @Override
    public Long getDirectoryId() {
        return mDelegate.getDirectoryId();
    }

    @Override
    public String getLookupKey() {
        return mDelegate.getLookupKey();
    }

    @Override
    public long getDataId() {
        return mDelegate.getDataId();
    }

    @Override
    public RecipientEntry getEntry() {
        return mDelegate.getEntry();
    }

    @Override
    public void setOriginalText(final String text) {
        mDelegate.setOriginalText(text);
    }

    @Override
    public CharSequence getOriginalText() {
        return mDelegate.getOriginalText();
    }

    @Override
    public Rect getBounds() {
        return getDrawable().getBounds();
    }

    @Override
    public void draw(final Canvas canvas) {
        getDrawable().draw(canvas);
    }

    @Override
    public String toString() {
        return mDelegate.toString();
    }
}
