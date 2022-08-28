/***********************************************************************
 * Copyright (c) 2017 Milan Jaitner                                   *
 * Distributed under the MIT software license, see the accompanying    *
 * file COPYING or https://www.opensource.org/licenses/mit-license.php.*
 ***********************************************************************/

package bt.doorcontrol.utils;

import android.content.Context;
import android.content.SharedPreferences;

import bt.doorcontrol.BuildConfig;

public class Prefs
{
    public static boolean getBoolean(Context ctx, String key)
    {
        return getBoolean(ctx, key, false);
    }

    public static boolean getBoolean(Context ctx, String key, boolean def)
    {
        if (ctx == null || key == null)
            return false;

        SharedPreferences settings = ctx.getSharedPreferences(BuildConfig.APPLICATION_ID, 0);
        return settings.getBoolean(key, def);
    }

    public static void setBoolean(Context ctx, String key, boolean value)
    {
        if (ctx == null)
            return;

        SharedPreferences settings = ctx.getSharedPreferences(BuildConfig.APPLICATION_ID, 0);

        SharedPreferences.Editor editor = settings.edit();
        editor.putBoolean(key, value);
        editor.commit();
    }

    public static String getString(Context ctx, String key, String def)
    {
        if (ctx == null || key == null)
            return null;

        SharedPreferences settings = ctx.getSharedPreferences(BuildConfig.APPLICATION_ID, 0);
        return settings.getString(key, def);
    }

    public static void setString(Context ctx, String key, String value)
    {
        if (ctx == null)
            return;

        SharedPreferences settings = ctx.getSharedPreferences(BuildConfig.APPLICATION_ID, 0);

        SharedPreferences.Editor editor = settings.edit();
        editor.putString(key, value);
        editor.commit();
    }

    public static void setFloat(Context ctx, String key, float value)
    {
        if (ctx == null)
            return;

        SharedPreferences settings = ctx.getSharedPreferences(BuildConfig.APPLICATION_ID, 0);

        SharedPreferences.Editor editor = settings.edit();
        editor.putFloat(key, value);
        editor.commit();
    }

    public static float getFloat(Context ctx, String key)
    {
        if (ctx == null || key == null)
            return 0f;

        SharedPreferences settings = ctx.getSharedPreferences(BuildConfig.APPLICATION_ID, 0);
        return settings.getFloat(key, 0f);
    }
}
