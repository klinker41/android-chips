package com.android.ex.chips;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;

import java.util.HashMap;
import java.util.Map;

public class ContactImageCreator {

    private static Map<String, Integer> nameColorMap = new HashMap<>();

    public static Bitmap getLetterPicture(Context context, RecipientEntry entry) {
        return getLetterPicture(context, entry.getDisplayName(), entry.getBackgroundColor(context));
    }

    public static Bitmap getLetterPicture(Context context, String contactName, int backgroundColor) {
        int size = DensityConverter.toDp(context, 72);

        Bitmap image;
        try {
            image = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        } catch (OutOfMemoryError e) {
            size /= 2;
            image = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        }

        Canvas canvas = new Canvas(image);
        canvas.drawColor(backgroundColor);

        Paint textPaint = new Paint();
        textPaint.setStyle(Paint.Style.FILL);
        textPaint.setColor(isColorDark(backgroundColor) ? context.getResources().getColor(android.R.color.white) :
                context.getResources().getColor(R.color.lightBackgroundTextColor));
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setAntiAlias(true);
        textPaint.setTextSize((int) (size / 1.5));

        try {
            canvas.drawText(getFirstLetter(contactName).toUpperCase(), canvas.getWidth() / 2,
                    (int) ((canvas.getHeight() / 2) - ((textPaint.descent() + textPaint.ascent()) / 2)),
                    textPaint);

        } catch (Throwable e) {
            e.printStackTrace();
        }

        return clipToCircle(image);
    }

    public static int getBackgroundColorForContact(Context context, String name) {
        if (name == null) {
            name = "a";
        }

        if (nameColorMap.containsKey(name)) {
            return nameColorMap.get(name);
        } else {
            int color = ColorUtils.getRandomMaterialColor(context).color;
            nameColorMap.put(name, color);
            return color;
        }
    }

    private static String getFirstLetter(String name) {
        if (name != null && name.length() > 0) {
            return name.substring(0, 1);
        } else {
            // default to 'a' I guess, if nothing exists.
            return "a";
        }
    }

    private static boolean isColorDark(int color) {
        double darkness = 1 - (0.299 * Color.red(color) + 0.587 * Color.green(color) + 0.114 * Color.blue(color)) / 255;
        return darkness >= 0.30;
    }

    /**
     * Clips a provided bitmap to a circle.
     */
    private static Bitmap clipToCircle(Bitmap bitmap) {
        if (bitmap == null) {
            return null;
        }

        final int width = bitmap.getWidth();
        final int height = bitmap.getHeight();
        final Bitmap outputBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);

        final Path path = new Path();
        path.addCircle(
                (float) (width / 2),
                (float) (height / 2),
                (float) Math.min(width, (height / 2)),
                Path.Direction.CCW);

        final Canvas canvas = new Canvas(outputBitmap);
        canvas.clipPath(path);
        canvas.drawBitmap(bitmap, 0, 0, null);

        bitmap.recycle();

        return outputBitmap;
    }
}
