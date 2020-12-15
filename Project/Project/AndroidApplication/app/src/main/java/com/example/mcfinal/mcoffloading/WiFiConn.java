package com.example.mcfinal.mcoffloading;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.NetworkInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.widget.Toast;

public class WiFiConn extends BroadcastReceiver {

    private WifiP2pManager wifiP2pManager;
    private WifiP2pManager.Channel wifiP2pManagerChannel;
    private MainActivity mainActivity;

    public WiFiConn(WifiP2pManager manager, WifiP2pManager.Channel channel, MainActivity activity) {
        super();
        this.wifiP2pManager = manager;
        this.wifiP2pManagerChannel = channel;
        this.mainActivity = activity;
    }

    @SuppressLint("MissingPermission")
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();

        if (WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION.equals(action)) {
            // Checks if Wi-Fi is enabled
            int intExtra = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1);

            if (intExtra == WifiP2pManager.WIFI_P2P_STATE_ENABLED){
                Toast.makeText(context,"WiFi Connected", Toast.LENGTH_SHORT).show();
            }
            else{
                Toast.makeText(context,"WiFi Disconnected", Toast.LENGTH_SHORT).show();
            }
        } else if (WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION.equals(action)) {
            // Gets list of current peers
            if (wifiP2pManager!=null){
                wifiP2pManager.requestPeers(wifiP2pManagerChannel,mainActivity.wifiPeersListener);
            }

        } else if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action)) {
            // Responds to new connection or disconnection
            if (wifiP2pManager==null){
                return;
            }
            NetworkInfo networkInfo = intent.getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO);

            assert networkInfo != null;
            if (networkInfo.isConnected()){
                wifiP2pManager.requestConnectionInfo(wifiP2pManagerChannel, mainActivity.connInformationListener);
            }
        }
    }
}