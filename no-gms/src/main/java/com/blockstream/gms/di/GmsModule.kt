package com.blockstream.gms.di

import com.blockstream.base.GooglePlay
import com.blockstream.common.ZendeskSdk
import com.blockstream.common.fcm.Firebase
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Module
import org.koin.dsl.module

@Module
@ComponentScan("com.blockstream.green")
class GmsModule

val gmsModule = module {
    single {
        GooglePlay()
    }
    single {
        ZendeskSdk()
    }
    single {
        Firebase()
    }
}