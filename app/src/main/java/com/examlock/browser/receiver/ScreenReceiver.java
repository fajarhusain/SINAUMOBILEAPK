package com.examlock.browser.receiver;

import android.content.*;
import android.util.Log;
import com.examlock.browser.utils.*;

public class ScreenReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        AppPreferences prefs = AppPreferences.getInstance(context);
        if (!prefs.isExamActive()) return;

        if (Intent.ACTION_SCREEN_OFF.equals(intent.getAction())) {
            Log.w("ScreenReceiver", "Screen turned off during exam - triggering warning");
            SecurityUtils.playWarningAlarm(context, 95);
        }
    }
}
