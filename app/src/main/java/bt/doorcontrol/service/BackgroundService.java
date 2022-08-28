/***********************************************************************
 * Copyright (c) 2017 Milan Jaitner                                   *
 * Distributed under the MIT software license, see the accompanying    *
 * file COPYING or https://www.opensource.org/licenses/mit-license.php.*
 ***********************************************************************/

package bt.doorcontrol.service;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;

import bt.doorcontrol.MainActivity;
import bt.doorcontrol.R;

public class BackgroundService extends Service
{
    private static final int NOTIFICATION_ID = 39;

    public BackgroundService()
    {
    }

    @Override
    public IBinder onBind(Intent intent)
    {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int i, int i1)
    {
        return START_STICKY;
    }

    @Override
    public void onCreate()
    {
        super.onCreate();

        NotificationManager notifyManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setPriority(Notification.PRIORITY_MAX)
                .setContentTitle("Garážová vrata")
                .setContentText("Otevřít / Zavřít");

        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
        intent.setAction(MainActivity.INTERACT_WITH_DOORS);

        PendingIntent pending = PendingIntent.getActivity(getApplicationContext(), 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        builder.setContentIntent(pending);

        Notification nf = builder.build();

        startForeground(NOTIFICATION_ID, nf);

        if (notifyManager != null)
            notifyManager.notify(NOTIFICATION_ID, nf);
    }

    public static void start(Context ctx)
    {
        if (ctx != null)
            ctx.startService(new Intent(ctx, BackgroundService.class));
    }
}
