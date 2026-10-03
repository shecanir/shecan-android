package ir.shecan.core.service;

import android.annotation.TargetApi;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;

import java.util.concurrent.atomic.AtomicBoolean;

import ir.shecan.R;
import ir.shecan.Shecan;
import ir.shecan.core.util.Logger;

/**
 * Shecan Project
 *
 * @author pcqpcq & iTX Technologies
 * @link https://itxtech.org
 * <p>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
@TargetApi(Build.VERSION_CODES.N)
public class ShecanTileService extends TileService {

    private static final long TILE_REFRESH_DELAY_MS = 500L;
    private static final AtomicBoolean SWITCH_IN_PROGRESS = new AtomicBoolean(false);

    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Override
    public void onClick() {
        super.onClick();

        if (!SWITCH_IN_PROGRESS.compareAndSet(false, true)) {
            return;
        }

        boolean activate = !ShecanVpnService.isActivated();
        updateTile(activate);

        Thread switchThread = new Thread(() -> {
            try {
                if (activate) {
                    Shecan.activateService(getApplicationContext());
                } else {
                    Shecan.deactivateService(getApplicationContext());
                }
            } catch (RuntimeException e) {
                Logger.logException(e);
            } finally {
                mainHandler.postDelayed(() -> {
                    updateTile();
                    SWITCH_IN_PROGRESS.set(false);
                }, TILE_REFRESH_DELAY_MS);
            }
        }, "ShecanTileSwitch");
        switchThread.start();
    }

    @Override
    public void onStartListening() {
        updateTile();
    }

    private void updateTile() {
        updateTile(ShecanVpnService.isActivated());
    }

    private void updateTile(boolean activate) {
        Tile tile = getQsTile();
        if (tile == null) {
            return;
        }

        tile.setLabel(getString(R.string.quick_toggle));
        tile.setContentDescription(getString(R.string.app_name));
        tile.setState(activate ? Tile.STATE_ACTIVE : Tile.STATE_INACTIVE);
        tile.updateTile();
    }
}
