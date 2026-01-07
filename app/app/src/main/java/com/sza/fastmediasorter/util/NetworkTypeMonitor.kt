package com.sza.fastmediasorter.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.core.content.getSystemService
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Monitor for detecting current network connection type.
 * 
 * Used to warn users when accessing network resources over mobile data.
 */
@Singleton
class NetworkTypeMonitor @Inject constructor(
    @ApplicationContext private val context: Context
) {

    /**
     * Get current network connection type.
     * 
     * @return Current NetworkConnectionType
     */
    fun getCurrentNetworkType(): NetworkConnectionType {
        val connectivityManager = context.getSystemService<ConnectivityManager>()
            ?: return NetworkConnectionType.NONE

        val network = connectivityManager.activeNetwork
        if (network == null) {
            Timber.d("No active network")
            return NetworkConnectionType.NONE
        }

        val capabilities = connectivityManager.getNetworkCapabilities(network)
        if (capabilities == null) {
            Timber.d("No network capabilities")
            return NetworkConnectionType.NONE
        }

        return when {
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> {
                Timber.d("Network type: WiFi")
                NetworkConnectionType.WIFI
            }
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> {
                Timber.d("Network type: Ethernet")
                NetworkConnectionType.ETHERNET
            }
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> {
                Timber.d("Network type: Mobile Data")
                NetworkConnectionType.MOBILE
            }
            else -> {
                Timber.d("Network type: Other")
                NetworkConnectionType.OTHER
            }
        }
    }

    /**
     * Check if currently connected via WiFi or Ethernet (free connections).
     * 
     * @return true if WiFi or Ethernet, false otherwise
     */
    fun isConnectedToFreeNetwork(): Boolean {
        val type = getCurrentNetworkType()
        return type == NetworkConnectionType.WIFI || type == NetworkConnectionType.ETHERNET
    }

    /**
     * Check if currently connected via mobile data (potentially costly).
     * 
     * @return true if mobile data, false otherwise
     */
    fun isConnectedToMobileData(): Boolean {
        return getCurrentNetworkType() == NetworkConnectionType.MOBILE
    }

    /**
     * Check if any network is available.
     * 
     * @return true if connected, false otherwise
     */
    fun isNetworkAvailable(): Boolean {
        return getCurrentNetworkType() != NetworkConnectionType.NONE
    }
}

/**
 * Enum representing network connection types.
 */
enum class NetworkConnectionType {
    /** No network connection */
    NONE,
    
    /** WiFi connection */
    WIFI,
    
    /** Ethernet (wired) connection */
    ETHERNET,
    
    /** Mobile data (cellular) connection */
    MOBILE,
    
    /** Other connection type */
    OTHER
}
