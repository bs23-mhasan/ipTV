package com.mhasancse15.iptv

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalContext
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.decode.GifDecoder

@Composable
fun LoadingGif(modifier: Modifier = Modifier) {
    val context = LocalContext.current.applicationContext
    val imageLoader = remember(context) {
        ImageLoader.Builder(context)
            .components { add(GifDecoder.Factory()) }
            .build()
    }

    AsyncImage(
        model = R.raw.loader,
        imageLoader = imageLoader,
        contentDescription = stringResource(R.string.loading),
        contentScale = ContentScale.Crop,
        modifier = modifier
    )
}
