package de.fithud.fithudlib;

import android.app.Service;
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
import android.nfc.Tag;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
import android.util.Log;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

/**
 * Created by jandob on 11/17/14.
 */
public class FHSensorManager extends MessengerService {

    public final class Messages extends MessengerService.Messages {
        public static final int HEARTRATE_MESSAGE = 1;
        public static final int CADENCE_MESSAGE = 2;
        public static final int SPEED_MESSAGE = 3;

    }

    @Override
    void handleMessage(Message msg) {

    }

    void sendMsg(int messageType, int[] val) {
        for (int i = mClients.size() - 1; i >= 0; i--) {
            Message msg = Message.obtain(null, messageType);
            Bundle bundle = new Bundle();
            // bundle.putFloat("value", val);
            bundle.putIntArray("value", val);
            msg.setData(bundle);
            try {
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
    private int stopScanCount = 20;
    private final String H7 = "00:22:D0:3D:30:31";
    private final String CAD = "C7:9E:DF:E6:F8:D5";
    private final String SPD = "EB:03:59:83:C8:34";
    private final String HRService = "0000180d-0000-1000-8000-00805f9b34fb";
    private final String SPDCADService = "00001816-0000-1000-8000-00805f9b34fb";
    private boolean connectionInProgress = false;
    private int nrOfconnectedDevices = 0;

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

                if (mTick >= 20) {
                    stopScan();
                }
            }
        }
    };

    public void connectToAvailableDevices() {
        for (BluetoothDevice device : mBtDevicesReadyToConnect) {
            if (!mConnectedBtDevices.contains(device.getAddress())) {
                mConnectedBtDevices.add(device.getAddress());
                connectionInProgress = true;
                nrOfconnectedDevices += 1;
                Log.i(TAG, "connect to: " + device.getAddress());
                device.connectGatt(context, false, btleGattCallback);
            }
        }
        mBtDevicesReadyToConnect.clear();
    }

    private final BluetoothGattCallback btleGattCallback = new BluetoothGattCallback() {

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, final BluetoothGattCharacteristic characteristic) {
            // this will get called anytime you perform a read or write characteristic operation
            byte[] characteristicData = characteristic.getValue();
            //Log.i(TAG, "received data from characteristic:" + characteristic.getService().getUuid().toString());

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
                Log.i(TAG, "nrOfconnectedDevices: " + nrOfconnectedDevices);
                gatt.discoverServices();
            }

            if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                mConnectedBtDevices.remove(gatt.getDevice().getAddress());
                gatt.close();
                Log.i(TAG, "disconnected from " + gatt.getDevice().getAddress());
            }
        }

        @Override
        public void onServicesDiscovered(final BluetoothGatt gatt, final int status) {
            // this will get called after the client initiates a BluetoothGatt.discoverServices() call
            List<BluetoothGattService> services = gatt.getServices();
            Log.i(TAG, gatt.getDevice().getName() + " discovered " + services.size() + " services:");
            for (BluetoothGattService service : services) {
                Log.i(TAG, service.getUuid().toString());
                if (!(service.getUuid().toString().equals(HRService) || service.getUuid().toString().equals(SPDCADService))) {
                    continue;
                }

                //H7=0000180d-0000-1000-8000-00805f9b34fb
                Log.i(TAG, service.toString());
                List<BluetoothGattCharacteristic> characteristics = service.getCharacteristics();
                for (BluetoothGattCharacteristic characteristic : characteristics) {
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
            connectionInProgress = false;
        }
    };

    public void startScan() {
        Log.i(TAG, "start scanning");
        mBtAdapter.startLeScan(leScanCallback);
    }

    public void stopScan() {
        Log.i(TAG, "stop scanning");
        mBtAdapter.stopLeScan(leScanCallback);
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
            mTick++;
            Log.i(TAG, "scanning since " + Long.valueOf(mTick) + "s");
            if (mTick < 20) {
                mHandler.postDelayed(mTickRunnable, 1000);

            } else {
                Log.i(TAG, "scanning stopped");
            }
        }
    };

    @Override
    public void onDestroy() {
        Log.i(TAG, "onDestroy()");
        closeConnections();
        super.onDestroy();
    }
}
