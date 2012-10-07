/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.wifidirect;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.NetworkInfo;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pManager;
import android.util.Log;

/**
 * A BroadcastReceiver that notifies of important wifi p2p events.
 */

public class WiFiDirectBroadcastReceiver extends BroadcastReceiver {

    private AppNetService netService;
    WifiP2pNetServiceListener serviceListener;

    private static final String TAG = "WiFiDirectBroadcastReceiver";
    /**
     * @param manager WifiP2pManager system service
     * @param channel Wifi p2p channel
     * @param activity activity associated with the receiver
     */
    public WiFiDirectBroadcastReceiver(AppNetService service, WifiP2pNetServiceListener listener) {
        super();
        this.netService = service;
        this.serviceListener = listener;
    }

    /*
     * (non-Javadoc)
     * @see android.content.BroadcastReceiver#onReceive(android.content.Context,
     * android.content.Intent)
     */
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION.equals(action)) {

            // UI update to indicate wifi p2p status.
            int state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1);
            if (state == WifiP2pManager.WIFI_P2P_STATE_ENABLED) {
                // Wifi Direct mode is enabled
            	netService.setIsWifiP2pEnabled(true);
            	// TODO let's go and test ...
            	netService.discoverPeers();
            } else {
            	netService.setIsWifiP2pEnabled(false);
            	netService.resetPeers();

            }
            Log.d(TAG, "P2P state changed - state:" + state);
        } else if (WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION.equals(action)) {

            // request available peers from the wifi p2p manager. This is an
            // asynchronous call and the calling activity is notified with a
            // callback on PeerListListener.onPeersAvailable()
            if (netService.isWifiP2pAviliable()) {
            	netService.requestPeers(serviceListener);
                
            }
            Log.d(TAG, "P2P peers changed");
        } else if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action)) {

            if (!netService.isWifiP2pAviliable()) {
                return;
            }

            NetworkInfo networkInfo = (NetworkInfo) intent
                    .getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO);

            if (networkInfo.isConnected()) {

                // we are connected with the other device, request connection
                // info to find group owner IP
            	netService.requestConnectionInfo(serviceListener);
            } else {
                // It's a disconnect
            	netService.resetPeers();
            	// TODO let's go and test ...
            	netService.discoverPeers();
            }
            Log.d(TAG, "P2P connection changed - networkInfo:" + networkInfo.toString());
        } else if (WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION.equals(action)) {
        	WifiP2pDevice wifiP2pDevice = (WifiP2pDevice) intent.getParcelableExtra(
                    WifiP2pManager.EXTRA_WIFI_P2P_DEVICE);
        	netService.updateThisDevice((WifiP2pDevice) intent.getParcelableExtra(
                    WifiP2pManager.EXTRA_WIFI_P2P_DEVICE));
            Log.d(TAG, "P2P this device changed - wifiP2pDevice:" + wifiP2pDevice.toString());
//        } else if (WifiP2pManager.WIFI_P2P_DISCOVERY_CHANGED_ACTION.equals(action)) { 
//        	// TODO ...
//            Broadcast intent action indicating that peer discovery has either started or stopped. 
//            One extra EXTRA_DISCOVERY_STATE indicates whether discovery has started or stopped.
//            Note that discovery will be stopped during a connection setup. 
//            If the application tries to re-initiate discovery during this time, it can fail.
            // 1. WIFI_P2P_DISCOVERY_STARTED
            // 2. WIFI_P2P_DISCOVERY_STOPPED
    	} else {
    		// 可以注意一下是否还有其他的通知！
        	Log.d(TAG, "Other P2P change action - " + action);
    	}
    }
}