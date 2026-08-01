package eu.faircode.netguard;

/*
    This file is part of NetGuard.

    NetGuard is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    NetGuard is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with NetGuard.  If not, see <http://www.gnu.org/licenses/>.

    Copyright 2015-2026 by Marcel Bokhorst (M66B)
*/

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Application;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.graphics.Insets;
import androidx.core.view.OnApplyWindowInsetsListener;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.preference.PreferenceManager;

public class ApplicationEx extends Application {
    private static final String TAG = "NetGuard.App";

    private Thread.UncaughtExceptionHandler mPrevHandler;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "Create version=" + Util.getSelfVersionName(this) + "/" + Util.getSelfVersionCode(this));

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            createNotificationChannels();

        mPrevHandler = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread thread, Throwable ex) {
                if (Util.ownFault(ApplicationEx.this, ex)
                        && Util.isPlayStoreInstall(ApplicationEx.this)) {
                    Log.e(TAG, ex.toString() + "\n" + Log.getStackTraceString(ex));
                    mPrevHandler.uncaughtException(thread, ex);
                } else {
                    Log.w(TAG, ex.toString() + "\n" + Log.getStackTraceString(ex));
                    System.exit(1);
                }
            }
        });

        registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks() {
            @Override
            public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle savedInstanceState) {
                TypedValue tv = new TypedValue();
                activity.getTheme().resolveAttribute(R.attr.colorPrimaryDark, tv, true);
                int colorPrimaryDark = tv.data;

                Window window = activity.getWindow();
                ViewGroup decor = (ViewGroup) window.getDecorView();

                decor.setBackgroundColor(colorPrimaryDark);

                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.VANILLA_ICE_CREAM)
                    window.setStatusBarColor(colorPrimaryDark);

                View statusBarBackground = new View(activity);
                statusBarBackground.setBackgroundColor(colorPrimaryDark);
                statusBarBackground.setClickable(false);
                statusBarBackground.setFocusable(false);
                statusBarBackground.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);

                FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0);
                params.gravity = Gravity.TOP;

                decor.addView(statusBarBackground, params);

                ViewCompat.setOnApplyWindowInsetsListener(statusBarBackground, new OnApplyWindowInsetsListener() {
                    @NonNull
                    @Override
                    public WindowInsetsCompat onApplyWindowInsets(@NonNull View v, @NonNull WindowInsetsCompat insets) {
                        Insets bars = insets.getInsets(WindowInsetsCompat.Type.statusBars() | WindowInsetsCompat.Type.displayCutout());
                        ViewGroup.LayoutParams lparams = v.getLayoutParams();
                        if (lparams.height != bars.top) {
                            lparams.height = bars.top;
                            v.setLayoutParams(lparams);
                        }
                        return insets;
                    }
                });

                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(activity);
                boolean dark = prefs.getBoolean("dark_theme", false);

                View content = activity.findViewById(android.R.id.content);
                content.setBackgroundColor(dark ? Color.parseColor("#ff121212") : Color.WHITE);

                ViewCompat.setOnApplyWindowInsetsListener(content, new OnApplyWindowInsetsListener() {
                    @NonNull
                    @Override
                    public WindowInsetsCompat onApplyWindowInsets(@NonNull View v, @NonNull WindowInsetsCompat insets) {
                        Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
                        v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
                        return insets;
                    }
                });
            }

            @Override
            public void onActivityStarted(@NonNull Activity activity) {
            }

            @Override
            public void onActivityResumed(@NonNull Activity activity) {
            }

            @Override
            public void onActivityPaused(@NonNull Activity activity) {
            }

            @Override
            public void onActivityStopped(@NonNull Activity activity) {
            }

            @Override
            public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) {
            }

            @Override
            public void onActivityDestroyed(@NonNull Activity activity) {
            }
        });
    }

    @TargetApi(Build.VERSION_CODES.O)
    private void createNotificationChannels() {
        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        NotificationChannel foreground = new NotificationChannel("foreground", getString(R.string.channel_foreground), NotificationManager.IMPORTANCE_MIN);
        foreground.setSound(null, Notification.AUDIO_ATTRIBUTES_DEFAULT);
        nm.createNotificationChannel(foreground);

        NotificationChannel notify = new NotificationChannel("notify", getString(R.string.channel_notify), NotificationManager.IMPORTANCE_DEFAULT);
        notify.setSound(null, Notification.AUDIO_ATTRIBUTES_DEFAULT);
        notify.setBypassDnd(true);
        nm.createNotificationChannel(notify);

        NotificationChannel access = new NotificationChannel("access", getString(R.string.channel_access), NotificationManager.IMPORTANCE_DEFAULT);
        access.setSound(null, Notification.AUDIO_ATTRIBUTES_DEFAULT);
        access.setBypassDnd(true);
        nm.createNotificationChannel(access);

        NotificationChannel malware = new NotificationChannel("malware", getString(R.string.setting_malware), NotificationManager.IMPORTANCE_HIGH);
        malware.setSound(null, Notification.AUDIO_ATTRIBUTES_DEFAULT);
        malware.setBypassDnd(true);
        nm.createNotificationChannel(malware);
    }
}
