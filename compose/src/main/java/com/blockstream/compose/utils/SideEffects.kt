package com.blockstream.compose.utils

import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.app.ShareCompat
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.stack.Stack
import cafe.adriel.voyager.navigator.LocalNavigator
import com.blockstream.common.data.ErrorReport
import com.blockstream.common.data.LogoutReason
import com.blockstream.common.data.TwoFactorMethod
import com.blockstream.common.data.TwoFactorResolverData
import com.blockstream.common.data.TwoFactorSetupAction
import com.blockstream.common.events.Events
import com.blockstream.common.extensions.isNotBlank
import com.blockstream.common.extensions.twoFactorMethodsLocalized
import com.blockstream.common.models.GreenViewModel
import com.blockstream.common.navigation.NavigateDestinations
import com.blockstream.common.sideeffects.SideEffect
import com.blockstream.common.sideeffects.SideEffects
import com.blockstream.compose.LocalAppCoroutine
import com.blockstream.compose.LocalDialog
import com.blockstream.compose.LocalSnackbar
import com.blockstream.compose.R
import com.blockstream.compose.dialogs.SingleChoiceDialog
import com.blockstream.compose.dialogs.TwoFactorCodeDialog
import com.blockstream.compose.extensions.showErrorSnackbar
import com.blockstream.compose.navigation.pushUnique
import com.blockstream.compose.screens.HomeScreen
import com.blockstream.compose.screens.about.AboutScreen
import com.blockstream.compose.screens.login.LoginScreen
import com.blockstream.compose.screens.onboarding.SetupNewWalletScreen
import com.blockstream.compose.screens.onboarding.hardware.UseHardwareDeviceScreen
import com.blockstream.compose.screens.onboarding.phone.AddWalletScreen
import com.blockstream.compose.screens.onboarding.phone.EnterRecoveryPhraseScreen
import com.blockstream.compose.screens.onboarding.phone.PinScreen
import com.blockstream.compose.screens.onboarding.watchonly.WatchOnlyPolicyScreen
import com.blockstream.compose.screens.overview.WalletOverviewScreen
import com.blockstream.compose.screens.receive.ReceiveScreen
import com.blockstream.compose.screens.recovery.RecoveryCheckScreen
import com.blockstream.compose.screens.recovery.RecoveryIntroScreen
import com.blockstream.compose.screens.recovery.RecoveryPhraseScreen
import com.blockstream.compose.screens.recovery.RecoveryWordsScreen
import com.blockstream.compose.screens.send.BumpScreen
import com.blockstream.compose.screens.send.SendScreen
import com.blockstream.compose.screens.send.SweepScreen
import com.blockstream.compose.screens.settings.AppSettingsScreen
import com.blockstream.compose.screens.settings.WalletSettingsScreen
import com.blockstream.compose.sheets.AssetsAccountsBottomSheet
import com.blockstream.compose.sheets.Bip39PassphraseBottomSheet
import com.blockstream.compose.sheets.DenominationBottomSheet
import com.blockstream.compose.sheets.FeeRateBottomSheet
import com.blockstream.compose.sheets.LocalBottomSheetNavigatorM3
import com.blockstream.compose.sheets.WalletDeleteBottomSheet
import com.blockstream.compose.sheets.WalletRenameBottomSheet
import com.blockstream.compose.sideeffects.OpenDialogData
import com.blockstream.compose.sideeffects.openBrowser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

fun <Item : Screen> Stack<Item>.pushOrReplace(item: Item) {
    if(lastItemOrNull?.let { it::class == item::class } == true) {
        replace(item)
    } else {
        push(item)
    }
}

@Composable
fun HandleSideEffectDialog(viewModel: GreenViewModel, onDismiss: CoroutineScope.() -> Unit = {}, handler: CoroutineScope.(sideEffect: SideEffect) -> Unit = {}) {
    LaunchedEffect(Unit) {
        viewModel.sideEffect.onEach {
            handler.invoke(this, it)

            when (it) {
                is SideEffects.Dismiss -> {
                    onDismiss()
                }
            }
        }.collect()
    }
}

@Composable
fun HandleSideEffect(viewModel: GreenViewModel, handler: CoroutineScope.(sideEffect: SideEffect) -> Unit = {}) {
    val snackbar = LocalSnackbar.current
    val navigator = LocalNavigator.current
    val bottomSheetNavigator = LocalBottomSheetNavigatorM3.current
    val context = LocalContext.current
    val dialog = LocalDialog.current
    val appCoroutine = LocalAppCoroutine.current

    var twoFactorResolverData by remember { mutableStateOf<TwoFactorResolverData?>(null) }
    twoFactorResolverData?.also {
        it.methods?.also { methods ->
            SingleChoiceDialog(
                title = stringResource(id = R.string.id_choose_method_to_authorize_the),
                items = methods.twoFactorMethodsLocalized()
            ) {
                if(it == null){
                    viewModel._twoFactorDeferred?.completeExceptionally(Exception("id_action_canceled"))
                }else {
                    methods.getOrNull(it)?.also {
                        viewModel._twoFactorDeferred?.complete(it)
                    }
                }
                twoFactorResolverData = null
            }
        }

        it.authHandlerStatus?.also { authHandlerStatus ->
            TwoFactorCodeDialog(authHandlerStatus = authHandlerStatus){ code, isHelp ->
                if(code != null){
                    viewModel._twoFactorDeferred?.complete(code)
                }else{
                    viewModel._twoFactorDeferred?.completeExceptionally(Exception("id_action_canceled"))

                    if(isHelp == true){
                        appCoroutine.launch {
                            dialog.openDialog(OpenDialogData(
                                title = "id_are_you_not_receiving_your_2fa_code",
                                message = "id_try_again_using_another_2fa_method",
                                primaryText = if (it.enable2faCallMethod) "id_enable_2fa_call_method" else "id_try_again",
                                secondaryText = "id_contact_support",
                                onPrimary = {
                                    if (it.enable2faCallMethod){
                                        viewModel.postEvent(Events.TwoFactorSetup(
                                            greenWallet = viewModel.greenWallet,
                                            method = TwoFactorMethod.PHONE,
                                            action = TwoFactorSetupAction.SETUP,
                                            network = it.network!!,
                                            isSmsBackup = true
                                        ))
                                    }
                                },
                                onSecondary = {
                                    appCoroutine.launch {
                                        dialog.openErrorReportDialog(
                                            viewModel = viewModel,
                                            errorReport = ErrorReport.createForMultisig("Android: I am not receiving my 2FA code"),
                                            onSubmitErrorReport = { submitErrorReport ->
                                                viewModel.postEvent(submitErrorReport)
                                            }
                                        )
                                    }
                                }
                            ))
                        }
                    }
                }

                twoFactorResolverData = null
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.sideEffect.onEach {
            handler.invoke(this, it)

            when (it) {
                is SideEffects.OpenBrowser -> {
                    appCoroutine.launch {
                        openBrowser(
                            context = context,
                            dialogState = dialog,
                            isTor = viewModel.settingsManager.appSettings.tor,
                            url = it.url
                        )
                    }
                }

                is SideEffects.OpenFeeBottomSheet -> {
                    bottomSheetNavigator.show(
                        FeeRateBottomSheet(
                            greenWallet = it.greenWallet,
                            accountAsset = it.accountAsset,
                            params = it.params
                        )
                    )
                }

                is SideEffects.Snackbar -> {
                    appCoroutine.launch {
                        snackbar.showSnackbar(message = stringResourceId(context, it.text))
                    }
                }

                is SideEffects.OpenDenomination -> {
                    bottomSheetNavigator.show(
                        DenominationBottomSheet(
                            greenWallet = viewModel.greenWallet,
                            denominatedValue = it.denominatedValue
                        )
                    )
                }

                is SideEffects.ErrorSnackbar -> {
                    appCoroutine.launch {
                        snackbar.showErrorSnackbar(
                            context = context,
                            dialogState = dialog,
                            viewModel = viewModel,
                            error = it.error,
                            errorReport = it.errorReport
                        )
                    }
                }

                is SideEffects.Dialog -> {
                    appCoroutine.launch {
                        dialog.openDialog(OpenDialogData(stringResourceIdOrNull(context, it.title), stringResourceId(context, it.message)))
                    }
                }

                is SideEffects.TwoFactorResolver -> {
                    twoFactorResolverData = it.data
                }

                is SideEffects.ErrorDialog -> {
                    appCoroutine.launch {
                        dialog.openErrorDialog(it.error, it.errorReport, onErrorReport = { errorReport ->
                            appCoroutine.launch {
                                dialog.openErrorReportDialog(
                                    errorReport = errorReport,
                                    viewModel = viewModel,
                                    onSubmitErrorReport = { submitErrorReport ->
                                        viewModel.postEvent(submitErrorReport)
                                    }
                                )
                            }
                        })
                    }
                }

                is SideEffects.Share -> {
                    it.text?.also { text ->
                        val builder = ShareCompat.IntentBuilder(context)
                            .setType("text/plain")
                            .setText(text)

                        context.startActivity(
                            Intent.createChooser(
                                builder.intent,
                                context.getString(R.string.id_share)
                            )
                        )
                    }
                }

                is SideEffects.NavigateBack -> {
                    // Check if Navigator exists, else is handled by AppFragment for now
                    if(navigator != null) {
                        val error = it.error
                        if (error == null) {
                            navigator.pop()
                        } else {
                            appCoroutine.launch {
                                dialog.openErrorDialog(error, it.errorReport, onErrorReport = { errorReport ->
                                    appCoroutine.launch {
                                        dialog.openErrorReportDialog(
                                            errorReport = errorReport,
                                            viewModel = viewModel,
                                            onSubmitErrorReport = { submitErrorReport ->
                                                viewModel.postEvent(submitErrorReport)
                                            }
                                        )
                                    }
                                }) {
                                    navigator.pop()
                                }
                            }
                        }
                    }
                }

                is SideEffects.CopyToClipboard -> {
                    copyToClipboard(context = context, "Green", it.value)
                    it.message?.also {
                        appCoroutine.launch {
                            snackbar.showSnackbar(message = it)
                        }
                    }
                }

                is SideEffects.Logout -> {
                    viewModel.greenWalletOrNull?.let { greenWallet ->
                        if (greenWallet.isEphemeral || greenWallet.isHardware || it.reason == LogoutReason.USER_ACTION) {
                            navigator?.replaceAll(HomeScreen)
                        } else {
                            navigator?.replaceAll(
                                LoginScreen(
                                    greenWallet = greenWallet,
                                    isLightningShortcut = greenWallet.isLightning,
                                    autoLoginWallet = !greenWallet.isLightning
                                )
                            )
                        }
                    } ?: run {
                        navigator?.replaceAll(HomeScreen)
                    }
                }

                is SideEffects.NavigateToRoot -> {
                    navigator?.popAll()
                }

                is SideEffects.TransactionSent -> {
                    if (it.data.hasMessageOrUrl) {
                        val message = "id_message_from_recipient_s|${it.data.message ?: ""}"
                        val isUrl = it.data.url.isNotBlank()

                        dialog.openDialog(
                            OpenDialogData(
                                title = "id_success",
                                message = message,
                                primaryText = if(isUrl) "id_open" else "id_ok",
                                secondaryText = if(isUrl) "id_cancel" else null,
                                onPrimary = {
                                    if(isUrl){
                                        appCoroutine.launch {
                                            openBrowser(
                                                context = context,
                                                dialogState = dialog,
                                                isTor = viewModel.settingsManager.appSettings.tor,
                                                url = it.data.url ?: ""
                                            )
                                        }
                                    }
                                    navigator?.popAll()
                                },
                                onSecondary = {
                                    navigator?.popAll()
                                },
                                onDismiss = {
                                    navigator?.popAll()
                                }
                            ))
                    } else {
                        navigator?.popAll()
                    }
                }

                is SideEffects.NavigateTo -> {
                    when(val destination = it.destination) {
                        
                        is NavigateDestinations.RenameWallet -> {
                            bottomSheetNavigator.show(WalletRenameBottomSheet(destination.greenWallet))
                        }

                        is NavigateDestinations.AssetsAccounts -> {
                            bottomSheetNavigator.show(AssetsAccountsBottomSheet(destination.greenWallet, destination.assetsAccounts))
                        }

                        is NavigateDestinations.DeleteWallet -> {
                            bottomSheetNavigator.show(WalletDeleteBottomSheet(destination.greenWallet))
                        }

                        is NavigateDestinations.Bip39Passphrase -> {
                            bottomSheetNavigator.show(
                                Bip39PassphraseBottomSheet(
                                    greenWallet = destination.greenWallet,
                                    passphrase = destination.passphrase
                                )
                            )
                        }

                        is NavigateDestinations.WalletOverview -> {
                            navigator?.replaceAll(
                                WalletOverviewScreen(
                                    greenWallet = destination.greenWallet,
                                )
                            )
                        }

                        is NavigateDestinations.WalletSettings -> {
                            navigator?.push(
                                WalletSettingsScreen(
                                    greenWallet = destination.greenWallet
                                )
                            )
                        }

                        is NavigateDestinations.WalletLogin -> {
                            val loginScreen = LoginScreen(
                                greenWallet = destination.greenWallet,
                                isLightningShortcut = destination.isLightningShortcut,
                                autoLoginWallet = !destination.isLightningShortcut,
                                // device = null
                            )

                            navigator?.pushOrReplace(loginScreen)

//                            (requireActivity() as MainActivity).getVisibleFragment()?.also {
//                                if(it is LoginFragment && it.viewModel.greenWalletOrNull == directions.wallet && it.args.isLightningShortcut == directions.isLightningShortcut){
//                                    return
//                                }
//                            }
//
//                            navigate(
//                                NavGraphDirections.actionGlobalLoginFragment(
//                                    wallet = directions.wallet,
//                                    isLightningShortcut = directions.isLightningShortcut,
//                                    autoLoginWallet = !directions.isLightningShortcut
//                                )
//                            )
                        }

                        is NavigateDestinations.DeviceScan -> {

                        }

                        is NavigateDestinations.AppSettings -> {
                            navigator?.pushUnique(
                                AppSettingsScreen
                            )
                        }

                        is NavigateDestinations.About -> {
                            navigator?.pushUnique(
                                AboutScreen
                            )
                        }

                        is NavigateDestinations.SetupNewWallet -> {
                            navigator?.pushUnique(
                                SetupNewWalletScreen
                            )
                        }

                        is NavigateDestinations.AddWallet -> {
                            navigator?.pushUnique(
                                AddWalletScreen
                            )
                        }

                        is NavigateDestinations.UseHardwareDevice -> {
                            navigator?.pushUnique(
                                UseHardwareDeviceScreen
                            )
                        }

                        is NavigateDestinations.NewWatchOnlyWallet -> {
                            navigator?.pushUnique(
                                WatchOnlyPolicyScreen
                            )
                        }

                        is NavigateDestinations.RecoveryIntro -> {
                            navigator?.pushUnique(
                                RecoveryIntroScreen(destination.args)
                            )
                        }

                        is NavigateDestinations.RecoveryWords -> {
                            navigator?.push(
                                RecoveryWordsScreen(destination.args)
                            )
                        }

                        is NavigateDestinations.RecoveryCheck -> {
                            navigator?.popUntil { it is RecoveryIntroScreen}
                            navigator?.push(
                                RecoveryCheckScreen(destination.args)
                            )
                        }

                        is NavigateDestinations.SetPin -> {
                            navigator?.items?.firstOrNull { screen ->
                                screen is RecoveryIntroScreen
                            }?.also {
                                navigator.popUntil { screen -> screen is RecoveryIntroScreen}
                            }
                            navigator?.push(
                                PinScreen(destination.args)
                            )
                        }

                        is NavigateDestinations.EnterRecoveryPhrase -> {
                            navigator?.push(
                                EnterRecoveryPhraseScreen(destination.args)
                            )
                        }

                        is NavigateDestinations.Sweep -> {
                            navigator?.push(
                                SweepScreen(
                                    greenWallet = destination.greenWallet,
                                    privateKey = destination.privateKey,
                                    accountAsset = destination.accountAsset
                                )
                            )
                        }

                        is NavigateDestinations.Bump -> {
                            navigator?.push(
                                BumpScreen(
                                    greenWallet = destination.greenWallet,
                                    accountAsset = destination.accountAsset,
                                    transaction = destination.transaction
                                )
                            )
                        }

                        is NavigateDestinations.Send -> {
                            navigator?.push(
                                SendScreen(
                                    greenWallet = destination.greenWallet,
                                    accountAsset = destination.accountAsset
                                )
                            )
                        }

                        is NavigateDestinations.Receive -> {
                            navigator?.push(
                                ReceiveScreen(
                                    greenWallet = destination.greenWallet,
                                    accountAsset = destination.accountAsset
                                )
                            )
                        }

                        is NavigateDestinations.RecoveryPhrase -> {
                            with(destination.args) {
                                RecoveryPhraseScreen(
                                    isLightning = isLightning,
                                    providedCredentials = credentials,
                                    greenWallet = greenWallet,
                                ).also { screen ->
                                    // WalletSettings > RecoveryIntroScreen
                                    if (navigator?.lastItemOrNull is RecoveryIntroScreen) {
                                        navigator.replace(screen)
                                    } else {
                                        navigator?.push(screen)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }.collect()
    }
}