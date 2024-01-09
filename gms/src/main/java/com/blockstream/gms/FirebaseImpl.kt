package com.blockstream.gms


import com.blockstream.base.Firebase
import com.blockstream.common.fcm.FcmCommon
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.messaging.FirebaseMessaging
import mu.KLogging
import org.koin.core.component.KoinComponent

class FirebaseImpl constructor(val fcmCommon: FcmCommon) : Firebase(), KoinComponent {
    private val firebaseMessaging: FirebaseMessaging by lazy { FirebaseMessaging.getInstance() }

    override fun initialize() {
        firebaseMessaging.token.addOnCompleteListener(OnCompleteListener { task ->
            if (!task.isSuccessful) {
                logger.info { "Fetching FCM registration token failed " + task.exception }
                return@OnCompleteListener
            }

            // Get new FCM registration token
            fcmCommon.setToken(task.result)
        })
    }

    companion object : KLogging()
}
