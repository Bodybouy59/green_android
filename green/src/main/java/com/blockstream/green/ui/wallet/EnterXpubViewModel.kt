package com.blockstream.green.ui.wallet;

import androidx.lifecycle.*
import com.blockstream.gdk.GreenWallet
import com.blockstream.green.data.Countly
import com.blockstream.green.database.Wallet
import com.blockstream.green.database.WalletRepository
import com.blockstream.green.gdk.SessionManager
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.withContext


class EnterXpubViewModel @AssistedInject constructor(
    greenWallet: GreenWallet,
    sessionManager: SessionManager,
    walletRepository: WalletRepository,
    countly: Countly,
    @Assisted wallet: Wallet,
) : AbstractWalletViewModel(sessionManager, walletRepository, countly, wallet) {

    val xpub = MutableLiveData<String>()
    val isXpubValid = MutableLiveData(false)

    init {
        xpub
            .asFlow()
            .onEach {
                isXpubValid.value = true
                isXpubValid.value = withContext(Dispatchers.IO){
                    greenWallet.isXpubValid(it)
                }
            }
            .launchIn(viewModelScope)
    }

    @dagger.assisted.AssistedFactory
    interface AssistedFactory {
        fun create(
            wallet: Wallet
        ): EnterXpubViewModel
    }

    companion object {
        fun provideFactory(
            assistedFactory: AssistedFactory,
            wallet: Wallet
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return assistedFactory.create(wallet) as T
            }
        }
    }
}