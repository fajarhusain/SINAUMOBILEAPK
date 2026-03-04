package com.examlock.browser.receiver;

import android.content.*;
import com.examlock.browser.service.ExamGuardService;
import com.examlock.browser.utils.AppPreferences;

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            AppPreferences prefs = AppPreferences.getInstance(context);
            if (prefs.isExamActive()) {
                // Restart guard service on boot if exam was active
                Intent serviceIntent = new Intent(context, ExamGuardService.class);
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    context.startForegroundService(serviceIntent);
                } else {
                    context.startService(serviceIntent);
                }
            }
        }
    }
}
