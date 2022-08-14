package com.bnsh.torchplus.util

import android.os.Build

class Version {

    companion object {

        fun isTiramisu(): Boolean {
            return Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
        }

    }

}