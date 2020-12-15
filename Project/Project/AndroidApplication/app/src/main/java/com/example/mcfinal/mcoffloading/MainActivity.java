package com.example.mcfinal.mcoffloading;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.net.wifi.WifiManager;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.StrictMode;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    WifiManager wifiManager;
    WifiP2pManager wifiP2pManager;
    WifiP2pManager.Channel wifiP2pManagerChannel;
    BroadcastReceiver broadcastReceiver;
    IntentFilter intentFilter;
    Button discoveryButton;
    ListView listView;
    TextView connStatus;
    List<WifiP2pDevice> peers = new ArrayList<WifiP2pDevice>();
    String[] deviceNames;
    WifiP2pDevice[] p2pDevices;
    TextView msgBox;
    Button locationButton, sendButton, computeButton;
    FusedLocationProviderClient locationClient;
    EditText matrix1, matrix2;
    public long globalInit;
    public float beforeBattery, afterBattery;
    public float powerUsage;
    public ArrayList<String> addressMap = new ArrayList<String>();
    public HashMap<String, Object> sndRcvReg = new HashMap<>();
    public int[][] A1 = new int[4][4];
    public int[][] A2 = new int[4][4];
    public int[][] Out = new int[4][4];
    String data = null;
    static final int MESSAGE_READ = 1;
    ServerCls serverCls;
    ClientCls clientCls;
    ReceiveAndSend receiveAndSend;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        msgBox = findViewById(R.id.readMsg);
        locationClient = LocationServices.getFusedLocationProviderClient(this);
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);
        globalInit -=800;
        init();
        callListener();

        //Getting before battery level
        IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatus = registerReceiver(null, ifilter);

        int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);

        beforeBattery = level / (float)scale;
    }
    Handler handler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            switch (msg.what) {
                case MESSAGE_READ:
                    byte[] readBuff = (byte[]) msg.obj;
                    String tempMsg = new String(readBuff, 0, msg.arg1);
                    if (tempMsg.contains("\"time\"") && tempMsg.contains("\"i\"")) {
                        try {
                            slaveCalculate(tempMsg);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    } else if (tempMsg.contains("offloadDone")) {
                        try {
                            receivedRequest(tempMsg);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    } else if (tempMsg.contains("periodicResponse")) {
                        generatePeriodResponses();
                    } else {
                        try {
                            Log.i("showing message: ",tempMsg);
                            initialConnect(tempMsg);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                    break;
            }
            return true;
        }
    });
    BroadcastReceiver batteryInformation = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int batteryLevel = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0);
            Slave.getInstance().getSlaveInfoMap().put("Battery Level", batteryLevel + "%");
        }
    };
    @SuppressLint("MissingPermission")
    private void getLocation() {
        locationClient.getLastLocation().addOnSuccessListener(this, new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        if (location != null) {
                            try {
                                Geocoder geocoder = new Geocoder(MainActivity.this,
                                        Locale.getDefault());
                                List<Address> addresses = geocoder.getFromLocation(location.
                                        getLatitude(), location.getLongitude(), 1);
                                data = (new StringBuilder()).append(addresses.get(0).getLatitude()).
                                        append("\n").append(addresses.get(0).getLongitude()).append
                                        ("\n").toString();
                                Slave.getInstance().getSlaveInfoMap().put("GPS Coordinates",
                                        addresses.get(0).getLatitude() + "," + addresses.get(0).getLongitude() + "");
                                Slave.getInstance().viewMapContents();
                                createNotes(data);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                });
    }

    private void callListener() {

        discoveryButton.setOnClickListener(new View.OnClickListener() {
            @SuppressLint("MissingPermission")
            @Override
            public void onClick(View v) {
                wifiP2pManager.discoverPeers(wifiP2pManagerChannel, new WifiP2pManager.ActionListener() {
                    @SuppressLint("LongLogTag")
                    @Override
                    public void onSuccess() {
                        Log.i("Started discovery service","Peer found");
                    }
                    @SuppressLint("LongLogTag")
                    @Override
                    public void onFailure(int reason) {
                        Log.i("Started discovery service","Peer not found");
                    }
                });
            }
        });
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                conn(position);
            }
        });
        locationButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(ActivityCompat.checkSelfPermission(MainActivity.this,
                        Manifest.permission.ACCESS_FINE_LOCATION) ==
                        PackageManager.PERMISSION_GRANTED) {
                    getLocation();
                }
                else {
                    ActivityCompat.requestPermissions(MainActivity.this,
                            new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 44);
                }
                Log.i("_Get info_ clicked","Getting battery information in btLocation");
                Slave.getInstance().viewMapContents();
                MainActivity.this.registerReceiver(MainActivity.this.batteryInformation,
                        new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
            }

        });
        computeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MatrixMult help=new MatrixMult();
                A1 = help.convertStringToArray(String.valueOf(matrix1.getText()));
                A2 = help.convertStringToArray(String.valueOf(matrix2.getText()));
                try {
                    globalInit = System.currentTimeMillis();
//                    IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
//                    Intent batteryStatus = registerReceiver(null, ifilter);
//
//                    int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
//                    int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
//
//                    beforeBattery = level / (float)scale;
                    //Toast.makeText(getApplicationContext(), "Before battery is " + beforeBattery, Toast.LENGTH_SHORT).show();

                    masterCalculate();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                JSONObject jsonObj = new JSONObject(Slave.getInstance().getSlaveInfoMap());
                String msgString = jsonObj.toString();
                String msg = msgString;
                Slave.getInstance().viewMapContents();
                receiveAndSend.write(msg.getBytes());
            }
        });
    }

    private void createNotes(String content) {
        try {
            File path = getExternalFilesDir(null);
            File file = new File(path, "Location.txt");
            FileWriter writer = new FileWriter(file);
            writer.append(content);
            writer.flush();
            writer.close();
        } catch (IOException e) {
        }
    }

    @SuppressLint("MissingPermission")
    private void conn(int position) {
        final WifiP2pDevice device = p2pDevices[position];
        WifiP2pConfig config = new WifiP2pConfig();
        config.deviceAddress = device.deviceAddress;
        wifiP2pManager.connect(wifiP2pManagerChannel, config, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                Toast.makeText(getApplicationContext(), "Paired with " +
                        device.deviceName, Toast.LENGTH_SHORT).show();
                addressMap.add(device.deviceAddress);
            }
            @Override
            public void onFailure(int reason) {
                Toast.makeText(getApplicationContext(), "Unable to Pair with "
                        + device.deviceName, Toast.LENGTH_SHORT).show();
            }
        });
    }
    private void init() {
        discoveryButton = findViewById(R.id.buttonDiscover);
        listView = findViewById(R.id.peerListView);
        connStatus = findViewById(R.id.connectionStatus);
        sendButton = findViewById(R.id.sendButton);
        computeButton = findViewById(R.id.btnCompute);
        locationButton = findViewById(R.id.getButton);
        matrix1 = findViewById(R.id.Matrix1Text);
        matrix2 = findViewById(R.id.Matrix2Text);
        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        wifiP2pManager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
        assert wifiP2pManager != null;
        wifiP2pManagerChannel = wifiP2pManager.initialize(this, getMainLooper(), null);
        broadcastReceiver = new WiFiConn(wifiP2pManager, wifiP2pManagerChannel, this);
        intentFilter = new IntentFilter();
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);
    }
    WifiP2pManager.PeerListListener wifiPeersListener = new WifiP2pManager.PeerListListener() {
        @Override
        public void onPeersAvailable(WifiP2pDeviceList peerList) {
            if (!peerList.getDeviceList().equals(peers)) {
                peers.clear();
                peers.addAll(peerList.getDeviceList());
                deviceNames = new String[peerList.getDeviceList().size()];
                p2pDevices = new WifiP2pDevice[peerList.getDeviceList().size()];
                int index = 0;
                for (WifiP2pDevice device : peerList.getDeviceList()) {
                    deviceNames[index] = device.deviceName;
                    p2pDevices[index] = device;
                    index++;
                }
                ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(getApplicationContext(),
                        android.R.layout.simple_list_item_1, deviceNames);
                listView.setAdapter(arrayAdapter);
            }
            if (peers.size() == 0) {
                Toast.makeText(getApplicationContext(), "No Devices Available", Toast.LENGTH_SHORT)
                        .show();
            }
        }
    };
    WifiP2pManager.ConnectionInfoListener connInformationListener = new WifiP2pManager.ConnectionInfoListener() {
        @Override
        public void onConnectionInfoAvailable(WifiP2pInfo info) {
            final InetAddress groupOwnerAddress = info.groupOwnerAddress;
            if (info.groupFormed && info.isGroupOwner) {
                serverCls = new ServerCls();
                serverCls.start();
            } else if (info.groupFormed) {
                clientCls = new ClientCls(groupOwnerAddress);
                clientCls.start();
            }
        }
    };
    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(broadcastReceiver);
    }
    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(broadcastReceiver, intentFilter);
    }
    public class ServerCls extends Thread {
        Socket socket;
        ServerSocket serverSocket;
        @Override
        public void run() {
            try {
                serverSocket = new ServerSocket(8888);
                socket = serverSocket.accept();
                receiveAndSend = new ReceiveAndSend(socket);
                receiveAndSend.start();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    private class ReceiveAndSend extends Thread {
        private Socket socket;
        private InputStream inputStream;
        private OutputStream outputStream;
        public ReceiveAndSend(Socket skt) {
            socket = skt;
            try {
                inputStream = socket.getInputStream();
                outputStream = socket.getOutputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        @Override
        public void run() {
            byte[] buffer = new byte[1024];
            int bytes;
            while (socket != null) {
                try {
                    bytes = inputStream.read(buffer);
                    if (bytes > 0) {
                        handler.obtainMessage(MESSAGE_READ, bytes, -1, buffer).sendToTarget();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        public void write(byte[] bytes) {
            try {
                outputStream.write(bytes);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    public class ClientCls extends Thread {
        Socket socket;
        String hostAdd;
        public ClientCls(InetAddress hostAddress) {
            hostAdd = hostAddress.getHostAddress();
            socket = new Socket();
        }
        @Override
        public void run() {
            try {
                socket.connect(new InetSocketAddress(hostAdd, 8888), 500);
                receiveAndSend = new ReceiveAndSend(socket);
                receiveAndSend.start();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    @SuppressLint("MissingPermission")
    public void connIndex(int deviceIndex) {
        wifiP2pManager.discoverPeers(wifiP2pManagerChannel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
            }
            @Override
            public void onFailure(int reason) {
            }
        });
        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        WifiP2pDevice device = p2pDevices[findPos(addressMap.get(deviceIndex))];
        WifiP2pConfig config = new WifiP2pConfig();
        config.deviceAddress = device.deviceAddress;
        wifiP2pManager.connect(wifiP2pManagerChannel, config, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
            }
            @Override
            public void onFailure(int reason) {
            }
        });
    }
    public int findPos(String mac) {
        for (int i = 0; i < p2pDevices.length; i++) {
            if (p2pDevices[i].deviceAddress.equalsIgnoreCase(mac)) {
                return i;
            }
        }
        return 0;
    }
    @SuppressLint("LongLogTag")
    public void masterCalculate() throws InterruptedException {
        Toast.makeText(getApplicationContext(), "Matrix calculation sent to Slave device",
                Toast.LENGTH_SHORT).show();

        periodMonitor();
        for (int i = 0; i < addressMap.size(); i++) {
            Log.i("Inside masterCalculate for i = 0 to addressMap.size():","  i = " + i);
            try {
                HashMap<String, String> generatedMap = offloading(i);
                HashMap<String, Object> offloadRegister = new HashMap<>();
                long time = System.currentTimeMillis() + Integer.parseInt(generatedMap.get("time").toString());
                IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
                Intent batteryStatus = registerReceiver(null, ifilter);
                int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
                int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);

                afterBattery = level / (float)scale;

                //Toast.makeText(getApplicationContext(), "After battery is: " + afterBattery, Toast.LENGTH_SHORT).show();
                powerUsage = afterBattery - beforeBattery;
                Toast.makeText(getApplicationContext(), "Power Usage is : " + powerUsage, Toast.LENGTH_SHORT).show();

                offloadRegister.put("recoverBy", time);
                offloadRegister.put("i", generatedMap.get("i"));
                offloadRegister.put("j", generatedMap.get("j"));
                sndRcvReg.put(addressMap.get(i), offloadRegister);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }
    public HashMap<String, String> generateMap(int index) {
        HashMap<String, String> dataMap = new HashMap<>();
        dataMap.put("index", index + "");
        for (int i = index * 2; i < index * 2 + 4; i++) {
            dataMap.put(0 + "", MatrixMult.arrayToString(A1[0]));
            dataMap.put(1 + "", MatrixMult.arrayToString(A1[1]));
            dataMap.put(2 + "", MatrixMult.arrayToString(A1[2]));
            dataMap.put(3 + "", MatrixMult.arrayToString(A1[3]));
            dataMap.put((i + 4) + "", MatrixMult.arrayToString(MatrixMult.getTranspose(A2)[i]));
        }
        dataMap.put("i", index + "");
        dataMap.put("j", index + "");
        dataMap.put("time", (((index + 1) * 50)) + "");
        dataMap.put("mac", addressMap.get(index));
        return dataMap;
    }
    public HashMap<String, String> offloading(int index) throws JSONException {
        connIndex(index);
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        HashMap dataMap = generateMap(0);
        JSONObject jsonObj = new JSONObject(dataMap);
        String jsonString = jsonObj.toString();
        String msg = jsonString;
        receiveAndSend.write(msg.getBytes());
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return dataMap;
    }
    public void slaveCalculate(String jsonString) throws JSONException {
        Toast.makeText(getApplicationContext(), "Slave Matrix Calculation", Toast.LENGTH_SHORT).show();

        long start = System.currentTimeMillis();
        JSONObject jsonObject = new JSONObject(jsonString);
        String mac = jsonObject.getString("mac");
        addressMap.add(mac);
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        String[] iValues = {"0", "1", "2", "3"};
        String[] jValues = {};
        if (jsonObject.get("index").equals("0")) {
            jValues = new String[]{"4", "5", "6", "7"};
        } else if (jsonObject.get("index").equals("1")) {
            jValues = new String[]{"6", "7"};
        }
        HashMap<String, String> dataMap = new HashMap<>();
        for (int i = 0; i < iValues.length; i++) {
            for (int j = 0; j < jValues.length; j++) {
                dataMap.put(iValues[i] + "," + (((int) jValues[j].charAt(0)) - 4 - 48),
                        MatrixMult.arrayRCMultiply(jsonObject.get(iValues[i]) + "", jsonObject.get(jValues[j]) + ""));
            }
        }
        int time = Integer.parseInt(jsonObject.get("time") + "");
        dataMap.put("offloadDone", "true");
        dataMap.put("mac", mac);
        long end = System.currentTimeMillis();
        long difference = end - start;
        if (difference < time) {
            try {
                Thread.sleep(time - (difference));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        connIndex(0);
        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        JSONObject jsonObj = new JSONObject(dataMap);
        String returnString = jsonObj.toString();
        String msg = returnString;
        Toast.makeText(getApplicationContext(), "Matrix calculation Finished!", Toast.LENGTH_SHORT).show();
        receiveAndSend.write(msg.getBytes());
        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
    public void receivedRequest(String jsonString) throws JSONException {
        Toast.makeText(getApplicationContext(), "Matrix Result Received!", Toast.LENGTH_SHORT).show();
        JSONObject jsonObject = new JSONObject(jsonString);
        String macAddress = jsonObject.getString("mac");
        jsonObject.remove("offloadDone");
        jsonObject.remove("mac");
        Iterator<String> keysToCopyIterator = jsonObject.keys();
        List<String> keysList = new ArrayList<String>();
        while (keysToCopyIterator.hasNext()) {
            String key = (String) keysToCopyIterator.next();
            keysList.add(key);
        }
        for (int i = 0; i < keysList.size(); i++) {
            int iVal = (int) keysList.get(i).toString().charAt(0) - 48;
            int jVal = (int) keysList.get(i).toString().charAt(2) - 48;
            Out[iVal][jVal] = Integer.parseInt(jsonObject.get(keysList.get(i)).toString());
            System.out.println("Out Value - " + Out[iVal][jVal]);
        }
        sndRcvReg.remove(macAddress);
        createOutputs();
    }

    public void calculateMatrices() {
        int[][] outMat = new int[4][4];
        for(int i = 0; i< A1.length; i++) {
            for(int j = 0; j< A2.length; j++) {
                outMat[i][j] = 0;
            }
        }
        for(int i = 0; i< A1.length; i++) {
            for(int j = 0; j< A2.length; j++) {
                for(int k = 0; k< A2.length; k++) {
                    outMat[i][j] = outMat[i][j] + A1[i][k]* A2[k][j];
                }
            }
        }
    }

    public void createOutputs() {
        String row1, row2, row3, row4, est1, est2, est3, est4;
        row1 = MatrixMult.ar2str(Out[0]);
        row2 = MatrixMult.ar2str(Out[1]);
        row3 = MatrixMult.ar2str(Out[2]);
        row4 = MatrixMult.ar2str(Out[3]);
        est1 = "Distributed execution time (without failure): " + (System.currentTimeMillis() -
                globalInit) + "ms";
        long currentT = System.currentTimeMillis();
        calculateMatrices();
        est2 = "Master (standalone) execution time: " + (System.currentTimeMillis() - currentT + 1) + "ms";

        est3 = "Power consumption with distributed computing " + powerUsage;

        startActivity(new MatrixMult().navigateResultScreen(row1,row2,row3,row4,est1,est2,est3, MainActivity.this));

    }

    public void initialConnect(String tempMsg) throws JSONException {
        msgBox.setText(tempMsg);
    }
    public void periodMonitor() throws InterruptedException {
        Thread periodMonitoring = new Thread();
        periodMonitoring.start();
        while (sndRcvReg.size() > 0) {
            Thread.sleep(5000);
            Set<String> keysToCopyIterator = sndRcvReg.keySet();
            Object[] keyList = keysToCopyIterator.toArray();
            for(int i=0; i<keyList.length; i++) {
                connIndex(addressMap.indexOf(keyList[i]));
                Thread.sleep(50);
                receiveAndSend.write("periodicMonitor".getBytes());
                Thread.sleep(50);
                HashMap<String, Object> deviceMap = (HashMap<String, Object>)
                        sndRcvReg.get(String.valueOf(keyList[i]));
                if(Integer.parseInt(deviceMap.get("battery").toString()) < 20) {
                    FailureTime recovery=new FailureTime();
                    recovery.failureRecovery(String.valueOf(keyList[i]),
                            this,new MainActivity());
                }
            }
        }
    }

    public void generatePeriodResponses() {
        getLocation();
        JSONObject jsonObj = new JSONObject(Slave.getInstance().getSlaveInfoMap());
        String msg = jsonObj.toString();
        receiveAndSend.write(msg.getBytes());
    }
}