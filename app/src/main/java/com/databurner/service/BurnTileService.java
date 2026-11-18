package com.databurner.service;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Icon;
import android.os.Build;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;

import androidx.annotation.RequiresApi;

import com.databurner.R;

@RequiresApi(api = Build.VERSION_CODES.N)
public class BurnTileService extends TileService {

    private SharedPreferences prefs;

    @Override
    public void onCreate() {
        super.onCreate();
        prefs = getSharedPreferences("burn_stats", MODE_PRIVATE);
    }

    @Override
    public void onStartListening() {
        super.onStartListening();
        updateTileState();
    }

    @Override
    public void onClick() {
        super.onClick();
        boolean isRunning = prefs.getBoolean("service_running", false);
        if (isRunning) {
            Intent stopIntent = new Intent(this, BurnService.class);
            stopService(stopIntent);
            prefs.edit().putBoolean("service_running", false).apply();
        } else {
            Intent startIntent = new Intent(this, BurnService.class);
            startIntent.setAction(BurnService.ACTION_START);
            startForegroundService(startIntent);
            prefs.edit().putBoolean("service_running", true).apply();
        }
        updateTileState();
    }

    private void updateTileState() {
        Tile tile = getQsTile();
        if (tile == null) return;

        boolean isRunning = prefs.getBoolean("service_running", false);
        if (isRunning) {
            tile.setState(Tile.STATE_ACTIVE);
            tile.setLabel("Burning Active");
            tile.setIcon(Icon.createWithResource(this, R.drawable.ic_zap));
        } else {
            tile.setState(Tile.STATE_INACTIVE);
            tile.setLabel("Data Burner");
            tile.setIcon(Icon.createWithResource(this, R.drawable.ic_zap));
        }
        tile.updateTile();
    }
}
