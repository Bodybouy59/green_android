package com.blockstream.common.models.recovery

import blockstream_green.common.generated.resources.Res
import blockstream_green.common.generated.resources.id_backup_recovery_phrase
import blockstream_green.common.generated.resources.id_lightning
import com.blockstream.common.Urls
import com.blockstream.common.data.GreenWallet
import com.blockstream.common.data.NavData
import com.blockstream.common.events.Event
import com.blockstream.common.events.Events
import com.blockstream.common.extensions.ifConnected
import com.blockstream.common.extensions.previewWallet
import com.blockstream.common.gdk.data.Credentials
import com.blockstream.common.models.GreenViewModel
import com.rickclephas.kmp.nativecoroutines.NativeCoroutinesState
import com.rickclephas.kmp.observableviewmodel.MutableStateFlow
import com.rickclephas.kmp.observableviewmodel.launch
import kotlinx.coroutines.flow.MutableStateFlow
import org.jetbrains.compose.resources.getString

abstract class RecoveryPhraseViewModelAbstract(
    val isLightning: Boolean,
    greenWallet: GreenWallet?
) :
    GreenViewModel(greenWalletOrNull = greenWallet) {
    override fun screenName(): String = "RecoveryPhrase"

    @NativeCoroutinesState
    abstract val mnemonic: MutableStateFlow<String>

    @NativeCoroutinesState
    abstract val mnemonicWords: MutableStateFlow<List<String>>

    @NativeCoroutinesState
    abstract val passphrase: MutableStateFlow<String?>

    @NativeCoroutinesState
    abstract val showQR: MutableStateFlow<Boolean>
}

class RecoveryPhraseViewModel(
    isLightning: Boolean,
    providedCredentials: Credentials?,
    greenWallet: GreenWallet?
) :
    RecoveryPhraseViewModelAbstract(isLightning = isLightning, greenWallet = greenWallet) {

    val credentials: Credentials by lazy {
        providedCredentials ?: session.ifConnected {
            (if (isLightning) {
                Credentials(mnemonic = session.deriveLightningMnemonic())
            } else {
                session.getCredentials()
            })
        } ?: Credentials.empty()
    }

    override val mnemonic = MutableStateFlow(viewModelScope, credentials.mnemonic)

    override val mnemonicWords = MutableStateFlow(viewModelScope, credentials.mnemonic.split(" "))

    override val passphrase = MutableStateFlow(viewModelScope, credentials.bip39Passphrase)

    override val showQR = MutableStateFlow(viewModelScope, false)

    class LocalEvents {
        object ClickLearnMore : Events.OpenBrowser(Urls.HELP_BIP39_PASSPHRASE)
        object ShowQR : Event
    }

    init {
        viewModelScope.launch {
            _navData.value = NavData(
                title = getString(Res.string.id_backup_recovery_phrase),
                subtitle = if (isLightning) getString(Res.string.id_lightning) else null
            )
        }

        bootstrap()
    }

    override suspend fun handleEvent(event: Event) {
        super.handleEvent(event)

        if (event is LocalEvents.ShowQR) {
            showQR.value = true
        }
    }
}

class RecoveryPhraseViewModelPreview(isLightning: Boolean, providedCredentials: Credentials? = null,greenWallet: GreenWallet?) :
    RecoveryPhraseViewModelAbstract(isLightning = isLightning, greenWallet = greenWallet) {


    override val mnemonic: MutableStateFlow<String> = MutableStateFlow(DummyMnemonic)
    override val mnemonicWords: MutableStateFlow<List<String>> = MutableStateFlow(DummyMnemonic.split(" "))
    override val passphrase: MutableStateFlow<String?> = MutableStateFlow(providedCredentials?.bip39Passphrase)
    override val showQR: MutableStateFlow<Boolean> = MutableStateFlow(viewModelScope, false)

    companion object {
        val DummyMnemonic =
            "chalk verb patch cube sell west penalty fish park worry tribe tourist"
        fun preview() = RecoveryPhraseViewModelPreview(
            isLightning = false,
            greenWallet = previewWallet(isHardware = false)
        )

        fun previewBip39() = RecoveryPhraseViewModelPreview(
            isLightning = false,
            providedCredentials = Credentials(mnemonic = DummyMnemonic, bip39Passphrase = "Memorized_BIP39"),
            greenWallet = previewWallet(isHardware = false)
        )
    }
}