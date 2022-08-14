@file:Suppress("unused")

package com.bnsh.torchplus.service

import android.annotation.SuppressLint
import android.app.StatusBarManager
import android.content.ComponentName
import android.content.Context
import android.graphics.drawable.Icon
import android.hardware.camera2.CameraManager
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.annotation.RequiresApi
import com.bnsh.torchplus.MainActivity
import com.bnsh.torchplus.R
import com.bnsh.torchplus.updateTorch

class TorchPlusTileService : TileService() {

    companion object {
        private const val TAG = "TorchPlusTileService"

        lateinit var cameraMgr: CameraManager

        @RequiresApi(33)
        @SuppressLint("WrongConstant")
        fun requestPlacement() {
            val ctx = MainActivity.activity

            (ctx.getSystemService(Context.STATUS_BAR_SERVICE) as StatusBarManager)
                .requestAddTileService(
                    ComponentName(
                        ctx,
                        TorchPlusTileService::class.java
                    ),
                    ctx.resources.getString(R.string.qs_tile_name),
                    Icon.createWithResource(
                        ctx,
                        R.drawable.ic_qs_tile
                    ), {}, {}
                )
        }
    }


    override fun onCreate() {
        super.onCreate()

        MainActivity.torchEnabled = false

        cameraMgr = getSystemService(Context.CAMERA_SERVICE) as CameraManager
    }


    private fun updateState() {
        qsTile.state =
            if(MainActivity.torchEnabled) Tile.STATE_ACTIVE
            else if(!MainActivity.compatible) Tile.STATE_UNAVAILABLE
            else Tile.STATE_INACTIVE
        qsTile.updateTile()
    }


    override fun onClick() {

        updateTorch(
            cameraMgr,
            cameraMgr.cameraIdList[0],
            !MainActivity.torchEnabled,
            this
        )

        MainActivity.torchEnabled = !MainActivity.torchEnabled

        updateState()
    }
}