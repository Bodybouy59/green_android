package com.blockstream.compose.screens.send

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.blockstream.common.models.send.AccountExchangeViewModelPreview
import com.blockstream.compose.GreenAndroidPreview

@Composable
@Preview
fun ExchangeScreenPreview() {
    GreenAndroidPreview {
        AccountExchangeScreen(viewModel = AccountExchangeViewModelPreview.preview())
    }
}
