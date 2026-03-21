package com.club360fit.app.ui.utils

import android.content.Context
import android.net.Uri

fun readBytesFromUri(context: Context, uri: Uri): ByteArray? =
    context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
