package com.android.ex.chips;

import android.content.Context;
import android.content.res.Resources;
import android.util.TypedValue;

public class DensityConverter {
    public static int toDp(Context context, int px) {
        return convert(context, px, TypedValue.COMPLEX_UNIT_DIP);
    }

    private static int convert(Context context, int amount, int conversionUnit) {
        if (amount < 0) {
            throw new IllegalArgumentException("px should not be less than zero");
        }

        Resources r = context.getResources();
        return (int) TypedValue.applyDimension(conversionUnit, amount, r.getDisplayMetrics());
    }
}
