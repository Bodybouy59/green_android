package com.blockstream.compose.screens.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.copy
import blockstream_green.common.generated.resources.eye
import blockstream_green.common.generated.resources.id_enabled_1s
import blockstream_green.common.generated.resources.id_extended_public_key
import blockstream_green.common.generated.resources.id_extended_public_keys
import blockstream_green.common.generated.resources.id_multisig
import blockstream_green.common.generated.resources.id_output_descriptors
import blockstream_green.common.generated.resources.id_set_up_watchonly_credentials
import blockstream_green.common.generated.resources.id_singlesig
import blockstream_green.common.generated.resources.id_tip_you_can_use_the
import blockstream_green.common.generated.resources.key_multisig
import blockstream_green.common.generated.resources.qr_code
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import com.blockstream.common.Parcelable
import com.blockstream.common.Parcelize
import com.blockstream.common.data.GreenWallet
import com.blockstream.common.extensions.isNotBlank
import com.blockstream.common.models.settings.WatchOnlyViewModel
import com.blockstream.common.models.settings.WatchOnlyViewModelAbstract
import com.blockstream.common.models.settings.WatchOnlyViewModelPreview
import com.blockstream.common.navigation.NavigateDestinations
import com.blockstream.compose.GreenPreview
import com.blockstream.compose.components.GreenButton
import com.blockstream.compose.components.GreenButtonColor
import com.blockstream.compose.components.GreenButtonType
import com.blockstream.compose.components.GreenColumn
import com.blockstream.compose.components.GreenRow
import com.blockstream.compose.extensions.icon
import com.blockstream.compose.managers.LocalPlatformManager
import com.blockstream.compose.theme.MonospaceFont
import com.blockstream.compose.theme.bodyMedium
import com.blockstream.compose.theme.titleMedium
import com.blockstream.compose.theme.titleSmall
import com.blockstream.compose.theme.whiteMedium
import com.blockstream.compose.utils.AppBar
import com.blockstream.compose.utils.HandleSideEffect
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.core.parameter.parametersOf

@Parcelize
data class WatchOnlyScreen(
    val greenWallet: GreenWallet
) : Screen, Parcelable {
    @Composable
    override fun Content() {
        val viewModel = koinScreenModel<WatchOnlyViewModel> {
            parametersOf(greenWallet)
        }

        val navData by viewModel.navData.collectAsStateWithLifecycle()

        AppBar(navData)

        WatchOnlyScreen(viewModel = viewModel)
    }
}

@Composable
fun WatchOnlyScreen(
    viewModel: WatchOnlyViewModelAbstract
) {
    val scope = rememberCoroutineScope()
    val platformManager = LocalPlatformManager.current

    val richWatchOnly by viewModel.richWatchOnly.collectAsStateWithLifecycle()
    val multisigWatchOnly by viewModel.multisigWatchOnly.collectAsStateWithLifecycle()
    val extendedPublicKeysAccounts by viewModel.extendedPublicKeysAccounts.collectAsStateWithLifecycle()
    val outputDescriptorsAccounts by viewModel.outputDescriptorsAccounts.collectAsStateWithLifecycle()

    HandleSideEffect(viewModel)

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {

        richWatchOnly?.also {
            item {
                GreenRow(
                    padding = 0,
                    space = 4,
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    Icon(
                        painter = painterResource(Res.drawable.eye),
                        contentDescription = null
                    )

                    Text(
                        text = "Rich Watch Only",
                        style = titleMedium,
                    )
                }
            }

            if (it.isEmpty()) {
                item {
                    GreenButton("Create RWO" , modifier = Modifier.fillMaxWidth()) {
                        viewModel.postEvent(WatchOnlyViewModel.LocalEvents.CreateRichWatchOnly)
                    }
                }
            } else {
                item {
                    Column {
                        GreenButton("Create RWO for new networks" , modifier = Modifier.fillMaxWidth(), type = GreenButtonType.OUTLINE) {
                            viewModel.postEvent(WatchOnlyViewModel.LocalEvents.CreateRichWatchOnly)
                        }

                        GreenButton("Delete RWO (${it.size})", color = GreenButtonColor.RED, modifier = Modifier.fillMaxWidth()) {
                            viewModel.postEvent(WatchOnlyViewModel.LocalEvents.DeleteRichWatchOnly)
                        }
                    }
                }
            }
        }

        if (multisigWatchOnly.isNotEmpty()) {
            item {
                GreenRow(
                    padding = 0,
                    space = 4,
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    Icon(
                        painter = painterResource(Res.drawable.key_multisig),
                        contentDescription = null
                    )

                    Text(
                        text = stringResource(Res.string.id_multisig),
                        style = titleMedium,
                    )
                }

            }

            items(multisigWatchOnly) { look ->
                Setting(
                    title = look.network?.canonicalName ?: "Network",
                    subtitle = look.username?.takeIf { it.isNotBlank() }
                        ?.let { stringResource(Res.string.id_enabled_1s, it) }
                        ?: stringResource(Res.string.id_set_up_watchonly_credentials),
                    modifier = Modifier.clickable {
                        look.network?.also { network ->
                            viewModel.postEvent(
                                NavigateDestinations.WatchOnlyCredentialsSettings(
                                    network
                                )
                            )
                        }
                    }
                )
            }
        }


        if (extendedPublicKeysAccounts.isNotEmpty() || outputDescriptorsAccounts.isNotEmpty()) {
            item {
                GreenRow(
                    padding = 0,
                    space = 4,
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    Icon(
                        painter = painterResource(Res.drawable.key_multisig),
                        contentDescription = null
                    )

                    Text(
                        text = stringResource(Res.string.id_singlesig),
                        style = titleMedium
                    )
                }
            }
        }

        if (extendedPublicKeysAccounts.isNotEmpty()) {
            item {
                Column {
                    Text(
                        text = stringResource(Res.string.id_extended_public_keys),
                        style = titleSmall,
                        modifier = Modifier.padding(top = 8.dp)
                    )

                    Text(
                        text = stringResource(Res.string.id_tip_you_can_use_the),
                        style = bodyMedium,
                        color = whiteMedium
                    )
                }
            }

            items(extendedPublicKeysAccounts) {
                Descriptor(
                    title = it.account?.name ?: "-",
                    icon = painterResource(it.account!!.network.icon()),
                    descriptor = it.extendedPubkey ?: "-",
                    onCopy = {
                        platformManager.copyToClipboard(content = it.extendedPubkey ?: "-")
                    },
                    onQr = {
                        scope.launch {
                            viewModel.postEvent(
                                NavigateDestinations.Qr(
                                    title = getString(Res.string.id_extended_public_key),
                                    subtitle = it.account?.name,
                                    data = it.extendedPubkey ?: ""
                                )
                            )
                        }
                    }
                )
            }
        }

        if (outputDescriptorsAccounts.isNotEmpty()) {
            item {
                Text(
                    text = stringResource(Res.string.id_output_descriptors),
                    style = titleSmall,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            items(outputDescriptorsAccounts) {
                Descriptor(
                    title = it.account?.name ?: "-",
                    icon = painterResource(it.account!!.network.icon()),
                    descriptor = it.outputDescriptors ?: "-",
                    onCopy = {
                        platformManager.copyToClipboard(content = it.outputDescriptors ?: "-")
                    },
                    onQr = {
                        scope.launch {
                            viewModel.postEvent(
                                NavigateDestinations.Qr(
                                    title = getString(Res.string.id_output_descriptors),
                                    subtitle = it.account?.name,
                                    data = it.outputDescriptors ?: ""
                                )
                            )
                        }
                    }
                )
            }
        }
    }
}


@Composable
fun Descriptor(
    modifier: Modifier = Modifier,
    title: String,
    icon: Painter,
    descriptor: String,
    onCopy: () -> Unit = {},
    onQr: () -> Unit = {}
) {
    Card(modifier = Modifier.then(modifier)) {
        GreenColumn(
            space = 0,
            padding = 0,
            modifier = Modifier.padding(vertical = 16.dp).padding(start = 16.dp, end = 8.dp)
        ) {

            GreenRow(
                space = 8,
                padding = 0,
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                Image(
                    painter = icon,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )

                Text(
                    text = title,
                    style = titleSmall,
                    maxLines = 1,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            GreenRow(space = 0, padding = 0, verticalAlignment = Alignment.Top) {
                SelectionContainer(modifier = Modifier.weight(1f)) {
                    Text(
                        text = descriptor,
                        style = bodyMedium,
                        color = whiteMedium,
                        fontFamily = MonospaceFont()
                    )
                }

                Row {
                    IconButton(onCopy) {
                        Icon(
                            painter = painterResource(Res.drawable.copy),
                            contentDescription = "Copy",
                            modifier = Modifier.minimumInteractiveComponentSize()
                        )
                    }
                    IconButton(onQr) {
                        Icon(
                            painter = painterResource(Res.drawable.qr_code),
                            contentDescription = "QR",
                            modifier = Modifier.minimumInteractiveComponentSize()
                        )
                    }
                }
            }
        }

    }
}

@Composable
@Preview
fun WatchOnlyScreenPreview() {
    GreenPreview {
        WatchOnlyScreen(viewModel = WatchOnlyViewModelPreview.preview())
    }
}