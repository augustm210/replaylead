package com.replaylead.app

import android.app.Application
import com.revenuecat.purchases.LogLevel
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesConfiguration

class ReplayLeadApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        val apiKey = BuildConfig.REVENUECAT_API_KEY.trim()
        if (apiKey.isNotEmpty()) {
            Purchases.logLevel = if (BuildConfig.DEBUG) LogLevel.DEBUG else LogLevel.INFO
            Purchases.configure(PurchasesConfiguration.Builder(this, apiKey).build())
        }
    }
}
