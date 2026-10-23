package com.parkshare.app

import android.app.Application
import com.google.firebase.FirebaseApp

class ParkshareApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Explicitly initialize Firebase
        FirebaseApp.initializeApp(this)
    }
}
