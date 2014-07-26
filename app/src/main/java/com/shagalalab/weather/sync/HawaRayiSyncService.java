package com.shagalalab.weather.sync;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

public class HawaRayiSyncService extends Service {
    private static final Object sSyncAdapterLock = new Object();
    private static HawaRayiSyncAdapter sHawaRayiSyncAdapter = null;

    @Override
    public void onCreate() {
        Log.d("HawaRayiSyncService", "onCreate - HawaRayiSyncService");
        synchronized (sSyncAdapterLock) {
            if (sHawaRayiSyncAdapter == null) {
                sHawaRayiSyncAdapter = new HawaRayiSyncAdapter(getApplicationContext(), true);
            }
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return sHawaRayiSyncAdapter.getSyncAdapterBinder();
    }
}
