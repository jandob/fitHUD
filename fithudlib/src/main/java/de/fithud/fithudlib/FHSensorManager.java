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
import android.util.Log;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Created by jandob on 11/17/14.
 */
public class FHSensorManager {
    private Context context;
    private static final String TAG = FHSensorManager.class.getSimpleName();
    private Set<BluetoothDevice> mBtDevices;
    BluetoothAdapter mBtAdapter;
    //private List<UUID> mConnectableBtDevices = new ArrayList<UUID>();
    private List<String> mConnectableBtDevices = new ArrayList<String>();
    private int stopScanCount = 2;
    private final String H7 = "00:22:D0:3D:30:31";
    private final String CAD = "C7:9E:DF:E6:F8:D5";
    private final String SPD = "EB:03:59:83:C8:34";
    private final String HRService = "0000180d-0000-1000-8000-00805f9b34fb";
    private final String SPDCADService = "00001816-0000-1000-8000-00805f9b34fb";

    public final ArrayList<UpdateListener> mListeners = new ArrayList<UpdateListener>();
    public void registerListener(UpdateListener listener) {
        mListeners.add(listener);
    }

    public void unregisterListener(UpdateListener listener) {
        mListeners.remove(listener);
    }

    private void sendUpdate(String name, Float value) {
        for (int i=mListeners.size()-1; i>=0; i--) {
            mListeners.get(i).onUpdate(name, value);
        }
    }

    // end listener interface
    private BluetoothAdapter.LeScanCallback leScanCallback = new BluetoothAdapter.LeScanCallback() {
        @Override
        public void onLeScan(final BluetoothDevice device, final int rssi, final byte[] scanRecord) {
            Log.i(TAG, "found ble device: " + device.getName() + ", UUID: "+ device.getAddress());
            if (mConnectableBtDevices.contains(device.getAddress())) {
                mConnectableBtDevices.remove(device.getAddress());
                device.connectGatt(context, false, btleGattCallback);
                //mBtDevices.add(device);
                Log.i(TAG, "Gattconnected to: " + device.getName());
            } else {
                if (!mBtDevices.contains(device)){
                    Log.i(TAG , "stopcount: " + stopScanCount);
                    stopScanCount--;
                }
            }
            // stop if found (battery draining)
            if (mConnectableBtDevices.isEmpty() || stopScanCount <= 0) {
                mBtAdapter.stopLeScan(leScanCallback);
            }
            //btAdapter.stopLeScan(leScanCallback);
        }
    };

    private final BluetoothGattCallback btleGattCallback = new BluetoothGattCallback() {

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, final BluetoothGattCharacteristic characteristic) {
            // this will get called anytime you perform a read or write characteristic operation
            byte[] characteristicData = characteristic.getValue();
            Log.i(TAG, "received data from characteristic:" + characteristic.getUuid());
            //Log.i(TAG, "GattDesriptors:");
            //for (BluetoothGattDescriptor gattD : characteristic.getDescriptors()) {
            //    Log.i(TAG, gattD.getUuid().toString());
            //}
            for (byte data : characteristicData) {
              Log.i(TAG, Byte.toString(data));
            }
            sendUpdate("name", (float)characteristicData[1]);
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            Log.i(TAG,"OnDescriptionWrite staus: " + new Integer(status).toString());
            super.onDescriptorWrite(gatt, descriptor, status);
        }

        @Override
        public void onConnectionStateChange(final BluetoothGatt gatt, final int status, final int newState) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.i(TAG, "connected to device: " + gatt.getDevice().getName());
                gatt.discoverServices();
            } //else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                //T: close gatt, if device is out of range
                //Log.i(TAG, "disconnected from device, closing gatt:");
                //gatt.close();
            //}
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
                        Log.i(TAG, "enabeling notification: " + descriptor.getUuid().toString());
                        descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                        gatt.writeDescriptor(descriptor);
                    }
                }
            }
        }
    };

    public void closeConnections() {
        mBtAdapter.disable();
    }
    public FHSensorManager(Service mainService, Context context) {
        this.context = context;
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


        mBtDevices = mBtAdapter.getBondedDevices();
        Log.i(TAG, "bonded devices");
        for (BluetoothDevice device : mBtDevices) {
            Log.i(TAG, device.getName());
            device.connectGatt(context, false, btleGattCallback);
        }
        //UUID[] toArray = new UUID[mConnectableBtDevices.size()];
        //mConnectableBtDevices.toArray(toArray);
        mBtAdapter.startLeScan(leScanCallback);
        Log.i(TAG, "initialized");
    }


}
