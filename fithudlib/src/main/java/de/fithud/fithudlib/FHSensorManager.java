package de.fithud.fithudlib;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.hardware.SensorManager;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by jandob on 11/17/14.
 */
public class FHSensorManager extends MessengerService {

    public final class Messages extends MessengerService.Messages {
        public static final int HEARTRATE_MESSAGE = 1;
        public static final int CADENCE_MESSAGE = 2;
        public static final int SPEED_MESSAGE = 3;
        public static final int SENSOR_STATUS_MESSAGE = 4;
        public static final int ACC_RAW_MESSAGE = 5;
    }

    public final class Commands extends MessengerService.Commands {
        public static final int SEARCH_COMMAND = 1;
        public static final int WAKEUP_COMMAND = 2;
    }

    @Override
    void handleMessage(Message msg) {
        int [] command = msg.getData().getIntArray("command");

        switch (command[0]){
            case Commands.SEARCH_COMMAND:
                startScan();
                break;

            case Commands.WAKEUP_COMMAND:
                if(spdAccWake_connected == 1) {
                    sendWakeupMessage();
                }
                break;
        }
    }

    void sendMsg(int messageType, int[] val) {
        for (int i = mClients.size() - 1; i >= 0; i--) {

            try {Message msg = Message.obtain(null, messageType);
                Bundle bundle = new Bundle();
                // bundle.putFloat("value", val);
                bundle.putIntArray("value", val);
                msg.setData(bundle);
                mClients.get(i).send(msg);
            } catch (RemoteException e) {
                // The client is dead.  Remove it from the list;
                // we are going through the list from back to front
                // so this is safe to do inside the loop.
                mClients.remove(i);
            }
        }
    }

    private Context context;
    private static final String TAG = FHSensorManager.class.getSimpleName();
    private Set<BluetoothDevice> mBtDevices;
    BluetoothAdapter mBtAdapter;
    private List<BluetoothDevice> mBtDevicesReadyToConnect = new ArrayList<BluetoothDevice>();
    private List<String> mConnectableBtDevices = new ArrayList<String>();
    private List<String> mConnectedBtDevices = new ArrayList<String>();
    private BluetoothGattCharacteristic wakeupCharacteristic;
    private BluetoothGatt   wakeupGATT;
    private int stopScanCount = 20;

    // Devices:
    private final String H7 = "00:22:D0:3D:30:31";
    private final String CAD = "C7:9E:DF:E6:F8:D5";
    private final String SPD = "EB:03:59:83:C8:34";
    // Not added yet
    private final String SPD_ACC_WAKE = "02:80:E1:00:00:AA";

    // Services
    private final String HRService = "0000180d-0000-1000-8000-00805f9b34fb";
    private final String SPDCADService = "00001816-0000-1000-8000-00805f9b34fb";
    private final String ACCService =    "02366e80-cf3a-11e1-9ab4-0002a5d5c51b";
    private final String WakeupService = "42821a40-e477-11e2-82d0-0002a5d5c51b";

    // Characteristics
    private final String WakeupCharacteristicUUID = "a32e5520-e477-11e2-a9e3-0002a5d5c51b";

    private boolean connectionInProgress = false;
    private int nrOfremainingDevices = 0;

    public int speedometer_connected = 0;
    public int heartrate_connected = 0;
    public int cadence_connected = 0;
    public int spdAccWake_connected = 0;


    Timer timer;
    TimerTask timerTask;
    final Handler handler = new Handler();

    // end listener interface
    private BluetoothAdapter.LeScanCallback leScanCallback = new BluetoothAdapter.LeScanCallback() {
        @Override
        public void onLeScan(final BluetoothDevice device, final int rssi, final byte[] scanRecord) {
            //Log.d(TAG, "found ble device: " + device.getName() + ", UUID: "+ device.getAddress());
            //Log.i(TAG, mConnectableBtDevices.toString());
            if (mConnectableBtDevices.contains(device.getAddress()) && !connectionInProgress) {
                if (!mBtDevicesReadyToConnect.contains(device)) {
                    mBtDevicesReadyToConnect.add(device);
                    Log.i(TAG, "Added " + device.getAddress() + "to the connectable list");
                }
            }
        }
    };

    public void connectToAvailableDevices() {
        for (BluetoothDevice device : mBtDevicesReadyToConnect) {
            if (!mConnectedBtDevices.contains(device.getAddress())) {
                mConnectedBtDevices.add(device.getAddress());
                connectionInProgress = true;
                nrOfremainingDevices -= 1;
                Log.i(TAG,"Remaining: "+nrOfremainingDevices);
                Log.i(TAG, "connect to: " + device.getAddress());
                device.connectGatt(context, false, btleGattCallback);
                break;
            }
        }
        if ((nrOfremainingDevices == 0) && (connectionInProgress == false)){
            Log.i(TAG,"All devices connected");
            mBtDevicesReadyToConnect.clear();
        }
    }

    public void sendWakeupMessage(){
        byte[] wakeup_signal = new byte[1];
        wakeup_signal[0] = 1;
        // Save local in characterstic
        wakeupCharacteristic.setValue(wakeup_signal);
        // Send characteristic to remote device , afterwards onCharacteristicWrite is called
        wakeupGATT.writeCharacteristic(wakeupCharacteristic);
    }

    public void sendSensorStatus(){
        int sensorStatus_dataset[] = new int[4];
        sensorStatus_dataset[0] = heartrate_connected;
        sensorStatus_dataset[1] = speedometer_connected;
        sensorStatus_dataset[2] = cadence_connected;
        sensorStatus_dataset[3] = spdAccWake_connected;
        sendMsg(Messages.SENSOR_STATUS_MESSAGE, sensorStatus_dataset);
    }

    private final BluetoothGattCallback btleGattCallback = new BluetoothGattCallback() {

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicWrite(gatt, characteristic, status);
            Log.i(TAG,"Written on device with address: "+gatt.getDevice().getAddress().toString()+" on characteristic with uuid: "+characteristic.getUuid().toString());
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, final BluetoothGattCharacteristic characteristic) {
            // this will get called anytime you perform a read or write characteristic operation
            byte[] characteristicData = characteristic.getValue();
            //Log.i(TAG, "received data from characteristic:" + characteristic.getService().getUuid().toString());

            //Log.i(TAG,"Got MSG from characteristic: "+characteristic.getService().getUuid().toString());
            if (characteristic.getService().getUuid().toString().equals(SPDCADService)) {

                if ((characteristicData[0] & (1L << 1)) != 0) {
                    //Log.i(TAG, "Got message");
                    int high_crank = ((int)characteristicData[2]) & 0xff;
                    int low_crank = ((int)characteristicData[1]) & 0xff;
                    int crank_revolutions = (high_crank << 8) | low_crank;

                    int high_time = ((int)characteristicData[4]) & 0xff;
                    int low_time = ((int)characteristicData[3]) & 0xff;
                    int time_cadence = (high_time << 8) | low_time;

                    int cadence_dataset[] = new int[2];
                    cadence_dataset[0] = crank_revolutions;
                    cadence_dataset[1] = time_cadence;
                    //Log.i(TAG, "Cadence wheel: " + crank_revolutions);
                    sendMsg(Messages.CADENCE_MESSAGE, cadence_dataset);
                } else if ((characteristicData[0] & (1L << 0)) != 0) {
                    int fourth_wheel = ((int)characteristicData[4]) & 0xff;
                    int third_wheel = ((int)characteristicData[3]) & 0xff;
                    int second_wheel = ((int)characteristicData[2]) & 0xff;
                    int first_wheel = ((int)characteristicData[1]) & 0xff;
                    int wheel_revolutions = (fourth_wheel << 24) | (third_wheel << 16) | (second_wheel << 8) | (first_wheel);

                    int high_time = ((int)characteristicData[6]) & 0xff;
                    int low_time = ((int)characteristicData[5]) & 0xff;
                    int time_speed = (high_time << 8) | low_time;

                    int speed_dataset[] = new int[2];
                    speed_dataset[0] = wheel_revolutions;
                    speed_dataset[1] = time_speed;
                    sendMsg(Messages.SPEED_MESSAGE, speed_dataset);
                }
            }
            if (characteristic.getService().getUuid().toString().equals(HRService)) {
                int speed_dataset[] = new int[1];
                speed_dataset[0] = (int) characteristicData[1];
                sendMsg(Messages.HEARTRATE_MESSAGE, speed_dataset);
            }

            if(characteristic.getService().getUuid().toString().equals(ACCService)){
                Log.i(TAG,"GOT ACC");
                int high_x = ((int)characteristicData[0]) & 0xff;
                int low_x = ((int)characteristicData[1]) & 0xff;
                int acc_x = (high_x << 8) | low_x;

                int high_y = ((int)characteristicData[2]) & 0xff;
                int low_y = ((int)characteristicData[3]) & 0xff;
                int acc_y = (high_y << 8) | low_y;

                int high_z = ((int)characteristicData[4]) & 0xff;
                int low_z = ((int)characteristicData[5]) & 0xff;
                int acc_z = (high_z << 8) | low_z;

                int acc_dataset[] = new int[3];
                acc_dataset[0] = acc_x;
                acc_dataset[1] = acc_y;
                acc_dataset[2] = acc_z;
                sendMsg(Messages.ACC_RAW_MESSAGE, acc_dataset);
            }
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            Log.i(TAG, "OnDescriptionWrite staus: " + new Integer(status).toString());
            super.onDescriptorWrite(gatt, descriptor, status);
        }

        @Override
        public void onConnectionStateChange(final BluetoothGatt gatt, final int status, final int newState) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.i(TAG, "connected to device: " + gatt.getDevice().getName());

                if(gatt.getDevice().getAddress().equals(H7)){heartrate_connected = 1; };
                if(gatt.getDevice().getAddress().equals(SPD)){speedometer_connected = 1; };
                if(gatt.getDevice().getAddress().equals(CAD)){cadence_connected = 1; };
                if(gatt.getDevice().getAddress().equals(SPD_ACC_WAKE)){
                    spdAccWake_connected = 1;
                    Log.i(TAG,"ACC connected");
                }

                Log.i(TAG, "nrOfremainingDevices: " + nrOfremainingDevices);
                gatt.discoverServices();
            }

            if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                mConnectedBtDevices.remove(gatt.getDevice().getAddress());
                gatt.close();
                Log.i(TAG, "disconnected from " + gatt.getDevice().getAddress());
                if(gatt.getDevice().getAddress().equals(H7)){ heartrate_connected = 0;};
                if(gatt.getDevice().getAddress().equals(SPD)){speedometer_connected = 0; };
                if(gatt.getDevice().getAddress().equals(CAD)){cadence_connected = 0; };
                if(gatt.getDevice().getAddress().equals(SPD_ACC_WAKE)){spdAccWake_connected = 0; };
            }
        }

        @Override
        public void onServicesDiscovered(final BluetoothGatt gatt, final int status) {
            // this will get called after the client initiates a BluetoothGatt.discoverServices() call
            List<BluetoothGattService> services = gatt.getServices();
            Log.i(TAG, gatt.getDevice().getName() + " discovered " + services.size() + " services:");
            for (BluetoothGattService service : services) {
                Log.i(TAG, service.getUuid().toString());
                if (!(service.getUuid().toString().equals(HRService) || service.getUuid().toString().equals(SPDCADService) || service.getUuid().toString().equals(ACCService) || service.getUuid().toString().equals(WakeupService) )) {
                    continue;
                }

                //H7=0000180d-0000-1000-8000-00805f9b34fb
                Log.i(TAG, service.toString());
                List<BluetoothGattCharacteristic> characteristics = service.getCharacteristics();
                for (BluetoothGattCharacteristic characteristic : characteristics) {

                    if(characteristic.getUuid().toString().equals(WakeupCharacteristicUUID)){
                        wakeupCharacteristic = characteristic;
                        wakeupGATT = gatt;
                        Log.i(TAG,"Found wakeup-char");
                    }

                    final int charaProp = characteristic.getProperties();
                    if ((charaProp | BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {
                        gatt.setCharacteristicNotification(characteristic, true);
                        for (BluetoothGattDescriptor descriptor : characteristic.getDescriptors()) {
                            //find descriptor UUID that matches Client Characteristic Configuration (0x2902)
                            // and then call setValue on that descriptor

                            Log.i(TAG, "enabling notification: " + descriptor.getUuid().toString());
                            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                            gatt.writeDescriptor(descriptor);
                        }
                    }
                }
            }
            connectionInProgress = false;
            connectToAvailableDevices();
        }
    };

    public void startScan() {
        mTick = 0;
        Log.i(TAG, "start scanning");
        mBtAdapter.startLeScan(leScanCallback);
    }

    public void stopScan() {
        Log.i(TAG, "stop scanning");
        mBtAdapter.stopLeScan(leScanCallback);
        nrOfremainingDevices = mBtDevicesReadyToConnect.size();
        connectToAvailableDevices();
    }

    public void closeConnections() {
        mBtAdapter.disable();
    }

    public void onCreate() {
        context = getBaseContext();
        // not yet used.
        SensorManager sensorManager =
                (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        LocationManager locationManager =
                (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);

        BluetoothManager btManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);

        mBtAdapter = btManager.getAdapter();
        if (mBtAdapter != null && !mBtAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            //TODO
            //startActivityForResult(enableIntent,REQUEST_ENABLE_BT);
        }

        // T: Insert device UUID's to connect to.
        mConnectableBtDevices.add(H7);
        mConnectableBtDevices.add(CAD);
        mConnectableBtDevices.add(SPD);
        //mConnectableBtDevices.add(SPD_ACC_WAKE);
        startScan();
        Log.i(TAG, "initialized");

        //debug, to be removed
        mHandler.removeCallbacks(mTickRunnable);
        mHandler.post(mTickRunnable);
    }

    // generates ticks for stopping the scan
    private long mTick = 0;
    private final Handler mHandler = new Handler();
    private final Runnable mTickRunnable = new Runnable() {

        public void run() {
            if(mTick<10) {
                mTick++;
                Log.i(TAG, "scanning since " + Long.valueOf(mTick) + "s");
                if(mTick==10)
                {
                    stopScan();
                    Log.i(TAG, "scanning stopped");
                }
            }
            sendSensorStatus();
            mHandler.postDelayed(mTickRunnable, 1000);
        }
    };

    @Override
    public void onDestroy() {
        Log.i(TAG, "onDestroy()");
        closeConnections();
        super.onDestroy();
    }
}
