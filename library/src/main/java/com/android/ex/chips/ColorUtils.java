package com.android.ex.chips;

import android.content.Context;
import android.graphics.Color;
import android.os.Build;
import android.support.annotation.ColorInt;
import android.support.annotation.ColorRes;

public class ColorUtils {

    public static ColorSet getRandomMaterialColor(Context context) {
        int num = (int) (Math.random() * (19 + 1));

        switch (num) {
            case 0:
                return ColorSet.RED(context);
            case 1:
                return ColorSet.PINK(context);
            case 2:
                return ColorSet.PURPLE(context);
            case 3:
                return ColorSet.DEEP_PURPLE(context);
            case 4:
                return ColorSet.INDIGO(context);
            case 5:
                return ColorSet.BLUE(context);
            case 6:
                return ColorSet.LIGHT_BLUE(context);
            case 7:
                return ColorSet.CYAN(context);
            case 8:
                return ColorSet.GREEN(context);
            case 9:
                return ColorSet.LIGHT_GREEN(context);
            case 10:
                return ColorSet.AMBER(context);
            case 11:
                return ColorSet.ORANGE(context);
            case 12:
                return ColorSet.DEEP_ORANGE(context);
            case 13:
                return ColorSet.BROWN(context);
            case 14:
                return ColorSet.GREY(context);
            case 15:
                return ColorSet.BLUE_GREY(context);
            case 16:
                return ColorSet.TEAL(context);
            case 17:
                return ColorSet.LIME(context);
            case 18:
                return ColorSet.YELLOW(context);
            case 19:
                return ColorSet.WHITE(context);
            default:
                return ColorSet.TEAL(context);
        }
    }

    public static class ColorSet {

        public int color;
        public int colorDark;
        public int colorLight;
        public int colorAccent;

        private ColorSet(Context context, @ColorRes int color, @ColorRes int colorDark,
                         @ColorRes int colorLight, @ColorRes int colorAccent) {
            this.color = getColor(context, color);
            this.colorDark = getColor(context, colorDark);
            this.colorLight = getColor(context, colorLight);
            this.colorAccent = getColor(context, colorAccent);
        }

        private int getColor(Context context, @ColorRes int color) {
            if (context == null) {
                return Color.WHITE;
            }

            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    return context.getColor(color);
                } else {
                    return context.getResources().getColor(color);
                }
            } catch (Exception e) {
                return Color.WHITE;
            }
        }

        private static ColorSet redSet = null;
        public static ColorSet RED(Context context) {
            if (redSet == null) {
                redSet = new ColorSet(context, R.color.materialRed, R.color.materialRedDark,
                        R.color.materialRedLight, R.color.materialIndigoAccent);
            }

            return redSet;
        }

        private static ColorSet pinkSet = null;
        public static ColorSet PINK(Context context) {
            if (pinkSet == null) {
                pinkSet = new ColorSet(context, R.color.materialPink, R.color.materialPinkDark,
                        R.color.materialPinkLight, R.color.materialLimeAccent);
            }

            return pinkSet;
        }

        private static ColorSet purpleSet = null;
        public static ColorSet PURPLE(Context context) {
            if (purpleSet == null) {
                purpleSet = new ColorSet(context, R.color.materialPurple, R.color.materialPurpleDark,
                        R.color.materialPurpleLight, R.color.materialTealAccent);
            }

            return purpleSet;
        }

        private static ColorSet deepPurpleSet = null;
        public static ColorSet DEEP_PURPLE(Context context) {
            if (deepPurpleSet == null) {
                deepPurpleSet = new ColorSet(context, R.color.materialDeepPurple, R.color.materialDeepPurpleDark,
                        R.color.materialDeepPurpleLight, R.color.materialPinkAccent);
            }

            return deepPurpleSet;
        }

        private static ColorSet indigoSet = null;
        public static ColorSet INDIGO(Context context) {
            if (indigoSet == null) {
                indigoSet = new ColorSet(context, R.color.materialIndigo, R.color.materialIndigoDark,
                        R.color.materialIndigoLight, R.color.materialYellowAccent);
            }

            return indigoSet;
        }

        private static ColorSet blueSet = null;
        public static ColorSet BLUE(Context context) {
            if (blueSet == null) {
                blueSet = new ColorSet(context, R.color.materialBlue, R.color.materialBlueDark,
                        R.color.materialBlueLight, R.color.materialDeepOrangeAccent);
            }

            return blueSet;
        }

        private static ColorSet lightBlueSet = null;
        public static ColorSet LIGHT_BLUE(Context context) {
            if (lightBlueSet == null) {
                lightBlueSet = new ColorSet(context, R.color.materialLightBlue, R.color.materialLightBlueDark,
                        R.color.materialLightBlueLight, R.color.materialPurpleAccent);
            }

            return lightBlueSet;
        }

        private static ColorSet cyanSet = null;
        public static ColorSet CYAN(Context context) {
            if (cyanSet == null) {
                cyanSet = new ColorSet(context, R.color.materialCyan, R.color.materialCyanDark,
                        R.color.materialCyanLight, R.color.materialAmberAccent);
            }

            return cyanSet;
        }

        private static ColorSet tealSet = null;
        public static ColorSet TEAL(Context context) {
            if (tealSet == null) {
                tealSet = new ColorSet(context, R.color.materialTeal, R.color.materialTealDark,
                        R.color.materialTealLight, R.color.materialOrangeAccent);
            }

            return tealSet;
        }

        private static ColorSet greenSet = null;
        public static ColorSet GREEN(Context context) {
            if (greenSet == null) {
                greenSet = new ColorSet(context, R.color.materialGreen, R.color.materialGreenDark,
                        R.color.materialGreenLight, R.color.materialLightBlueAccent);
            }

            return greenSet;
        }

        private static ColorSet lightGreenSet = null;
        public static ColorSet LIGHT_GREEN(Context context) {
            if (lightGreenSet == null) {
                lightGreenSet = new ColorSet(context, R.color.materialLightGreen, R.color.materialLightGreenDark,
                        R.color.materialLightGreenLight, R.color.materialOrangeAccent);
            }

            return lightGreenSet;
        }

        private static ColorSet limeSet = null;
        public static ColorSet LIME(Context context) {
            if (limeSet == null) {
                limeSet = new ColorSet(context, R.color.materialLime, R.color.materialLimeDark,
                        R.color.materialLimeLight, R.color.materialBlueAccent);
            }

            return limeSet;
        }

        private static ColorSet yellowSet = null;
        public static ColorSet YELLOW(Context context) {
            if (yellowSet == null) {
                yellowSet = new ColorSet(context, R.color.materialYellow, R.color.materialYellowDark,
                        R.color.materialYellowLight, R.color.materialRedAccent);
            }

            return yellowSet;
        }

        private static ColorSet amberSet = null;
        public static ColorSet AMBER(Context context) {
            if (amberSet == null) {
                amberSet = new ColorSet(context, R.color.materialAmber, R.color.materialAmberDark,
                        R.color.materialAmberLight, R.color.materialCyanAccent);
            }

            return amberSet;
        }

        private static ColorSet orangeSet = null;
        public static ColorSet ORANGE(Context context) {
            if (orangeSet == null) {
                orangeSet = new ColorSet(context, R.color.materialOrange, R.color.materialOrangeDark,
                        R.color.materialOrangeLight, R.color.materialDeepPurpleAccent);
            }

            return orangeSet;
        }

        private static ColorSet deepOrangeSet = null;
        public static ColorSet DEEP_ORANGE(Context context) {
            if (deepOrangeSet == null) {
                deepOrangeSet = new ColorSet(context, R.color.materialDeepOrange, R.color.materialDeepOrangeDark,
                        R.color.materialDeepOrangeLight, R.color.materialLightGreenAccent);
            }

            return deepOrangeSet;
        }

        private static ColorSet brownSet = null;
        public static ColorSet BROWN(Context context) {
            if (brownSet == null) {
                brownSet = new ColorSet(context, R.color.materialBrown, R.color.materialBrownDark,
                        R.color.materialBrownLight, R.color.materialOrangeAccent);
            }
            return brownSet;
        }

        private static ColorSet greySet = null;
        public static ColorSet GREY(Context context) {
            if (greySet == null) {
                greySet = new ColorSet(context, R.color.materialGrey, R.color.materialGreyDark,
                        R.color.materialGreyLight, R.color.materialGreenAccent);
            }

            return greySet;
        }

        private static ColorSet blueGreySet = null;
        public static ColorSet BLUE_GREY(Context context) {
            if (blueGreySet == null) {
                blueGreySet = new ColorSet(context, R.color.materialBlueGrey, R.color.materialBlueGreyDark,
                        R.color.materialBlueGreyLight, R.color.materialRedAccent);
            }

            return blueGreySet;
        }

        private static ColorSet blackSet = null;
        public static ColorSet BLACK(Context context) {
            if (blackSet == null) {
                blackSet = new ColorSet(context, android.R.color.black, android.R.color.black,
                        android.R.color.black, R.color.materialTealAccent);
            }

            return blackSet;
        }

        private static ColorSet whiteSet = null;
        public static ColorSet WHITE(Context context) {
            if (whiteSet == null) {
                whiteSet = new ColorSet(context, android.R.color.white, R.color.materialWhiteDark,
                        android.R.color.white, R.color.materialOrangeAccent);
            }

            return whiteSet;
        }
    }
}
