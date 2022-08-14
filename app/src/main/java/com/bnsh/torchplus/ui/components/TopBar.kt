package com.bnsh.torchplus.ui.components

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.bnsh.torchplus.MainActivity
import com.bnsh.torchplus.R

@SuppressLint("WrongConstant")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopBar() {
    SmallTopAppBar(
        title = { Text(stringResource(R.string.app_name)) },
        actions = {
            IconButton(onClick = {
                MainActivity.activity.startActivity(
                    Intent(
                        Intent.ACTION_VIEW, Uri.parse("https://github.com/bennett-sh/torchplus")
                    )
                )
            }) {
                Icon(Icons.Outlined.Code, "View source code")
            }
        }
    )
}
