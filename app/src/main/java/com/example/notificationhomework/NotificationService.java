package com.example.notificationhomework;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import java.util.Date;


public class NotificationService extends Service {
    public NotificationService() {
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        int hours = intent.getExtras().getInt(Const.INTENT_KEY_HOURS);
        int minutes = intent.getExtras().getInt(Const.INTENT_KEY_MINUTES);

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationChannel notificationChannel = new NotificationChannel(Const.NOTIFICATION_CHANNEL_ID, Const.NOTIFICATION_CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT);
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(notificationChannel);
        }

        Intent prepareIntent = new Intent(getApplicationContext(), MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), 0, prepareIntent, 0);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext(), Const.NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setContentIntent(pendingIntent)
                .setContentTitle("Notification")
                .setContentText("Notification will be created at " +  String.format("%02d:%02d", hours, minutes))
                .setLargeIcon(BitmapFactory.decodeResource(getResources(), android.R.drawable.ic_lock_idle_alarm));

        Date currentDate = new Date(System.currentTimeMillis());
        int cHours = currentDate.getHours();
        int cMinutes = currentDate.getMinutes();
        boolean isAlarmTimeInNextDay;
        int milli = 0;
        if(hours < cHours) {
            isAlarmTimeInNextDay = true;
            if(minutes > cMinutes) {
                milli = 3600000 * 24 - (cHours - hours) * 3600000 + (minutes - cMinutes) * 60000;
            }
            if(minutes == cMinutes) {
                milli = 3600000 * 24 - (cHours - hours) * 3600000;
            }
            if(minutes < cMinutes) {
                milli = 3600000 * 24 - (cHours - hours) * 3600000 - (cMinutes - minutes) * 60000;
            }
        }
        if(hours == cHours) {
            if(minutes <= cMinutes) {
                isAlarmTimeInNextDay = true;
                milli = 3600000 * 24 - (cMinutes - minutes) * 60000;
            }
            else {
                isAlarmTimeInNextDay = false;
                milli = (minutes - cMinutes) * 60000;
            }
        }
        if(hours > cHours) {
            isAlarmTimeInNextDay = false;
            if(minutes > cMinutes) {
                milli = (hours - cHours) * 3600000 + minutes * 60000;
            }
            if(minutes==cMinutes) {
                milli = (hours - cHours) * 3600000;
            }
            if(minutes<cMinutes) {
                milli = (60 - cMinutes) * 60000 + (hours - cHours) * 3600000 + minutes * 60000;
            }
        }
        int hhours = milli / 1000 / 3600;
        int mminutes = milli / 1000 % 3600 / 60;
        Toast.makeText(getApplicationContext(), String.format("Notification will be shown at %d hours and %d minutes", hhours, mminutes), Toast.LENGTH_LONG).show();

        startForeground(Const.NOTIFICATION_FOREGROUND_SERVICE_ID, builder.build());


        class SleepTask extends AsyncTask<Integer, Void, Void> {
            @Override
            protected Void doInBackground(Integer... integers) {
                int millsSleep = integers[0];
                try {
                    Thread.sleep(millsSleep);
                }
                catch (InterruptedException e) {
                    e.printStackTrace();
                }
                return null;
            }

            @Override
            protected void onPostExecute(Void unused) {
                super.onPostExecute(unused);
                NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext(), Const.NOTIFICATION_CHANNEL_ID)
                        .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                        .setLargeIcon(BitmapFactory.decodeResource(getResources(), android.R.drawable.ic_lock_idle_alarm))
                        .setContentTitle("Your notification is going to work")
                        .setContentText("Notification has finished its work.");
                NotificationManagerCompat notificationManagerCompat = NotificationManagerCompat.from(getApplicationContext());
                notificationManagerCompat.notify(Const.NOTIFICATION_RESULT_ID, builder.build());

                ActivityLifecycleMonitor activityLifecycleMonitor = (ActivityLifecycleMonitor) getApplicationContext();
                if(activityLifecycleMonitor.isAppForeground()) {
                    SharedPreferences.Editor editor = getSharedPreferences(Const.SH_PREFERENCES_NAME, MODE_PRIVATE).edit();
                    editor.putInt(Const.SH_PREFERENCES_KEY_MODE, Const.MODE_SLEEP);
                    editor.commit();

                    Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    stopForeground(0);
                }
                else {
                    stopSelf();
                }
            }
        }

        SleepTask sleepTask = new SleepTask();
        sleepTask.execute(milli);
        return START_NOT_STICKY;
    }
}