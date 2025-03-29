package ir.shecan.service;

import android.annotation.TargetApi;
import android.os.Build;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;

import ir.shecan.Shecan;

import ir.shecan.R;

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

    @Override
    public void onClick() {
        Tile tile = getQsTile();
        tile.setLabel(getString(R.string.quick_toggle));
        tile.setContentDescription(getString(R.string.app_name));
        tile.setState(Shecan.switchService() ? Tile.STATE_ACTIVE : Tile.STATE_INACTIVE);
        tile.updateTile();
    }

    @Override
    public void onStartListening() {
        updateTile();
    }

    private void updateTile() {
        boolean activate = ShecanVpnService.isActivated();
        Tile tile = getQsTile();
        tile.setLabel(getString(R.string.quick_toggle));
        tile.setContentDescription(getString(R.string.app_name));
        tile.setState(activate ? Tile.STATE_ACTIVE : Tile.STATE_INACTIVE);
        tile.updateTile();
    }
}
