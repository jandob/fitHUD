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
import android.hardware.SensorManager;
import android.location.LocationManager;
import android.util.Log;

import java.util.List;
import java.util.Set;

/**
 * Created by jandob on 11/17/14.
 */
public class FHSensorManager {
    private Context context;
    private static final String TAG = FHSensorManager.class.getSimpleName();
    private Set<BluetoothDevice> mBtDevices;
    public interface OnChangedListener {

        void oneartRateChanged(FHSensorManager orientationManager);


    }
    private BluetoothAdapter.LeScanCallback leScanCallback = new BluetoothAdapter.LeScanCallback() {
        @Override
        public void onLeScan(final BluetoothDevice device, final int rssi, final byte[] scanRecord) {
            Log.i(TAG, "found ble device:");
            Log.i(TAG, device.toString());
            device.connectGatt(context, false, btleGattCallback);
            // TODO stop if found (battery draining)
            //btAdapter.stopLeScan(leScanCallback);
        }
    };
    private final BluetoothGattCallback btleGattCallback = new BluetoothGattCallback() {

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, final BluetoothGattCharacteristic characteristic) {
            // this will get called anytime you perform a read or write characteristic operation
        }

        @Override
        public void onConnectionStateChange(final BluetoothGatt gatt, final int status, final int newState) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.d(TAG, "connected to device:");
                Log.d(TAG, gatt.getDevice().getName());
                gatt.discoverServices();
            }
        }

        @Override
        public void onServicesDiscovered(final BluetoothGatt gatt, final int status) {
            // this will get called after the client initiates a BluetoothGatt.discoverServices() call
            List<BluetoothGattService> services = gatt.getServices();
            Log.d(TAG, gatt.getDevice().getName());
            Log.d(TAG, "discovered " + services.size() + " services:");
            for (BluetoothGattService service : services) {
                Log.d(TAG, service.getUuid().toString());
                List<BluetoothGattCharacteristic> characteristics = service.getCharacteristics();
                for (BluetoothGattCharacteristic characteristic : characteristics) {
                    for (BluetoothGattDescriptor descriptor : characteristic.getDescriptors()) {
                        //find descriptor UUID that matches Client Characteristic Configuration (0x2902)
                        // and then call setValue on that descriptor

                        descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                        gatt.writeDescriptor(descriptor);
                    }
                }
            }
        }
    };
    public FHSensorManager(Context context,
                           SensorManager sensorManager,
                           LocationManager locationManager,
                           BluetoothManager mBtManager,
                           BluetoothAdapter btAdapter) {
        this.context = context;
        mBtDevices = btAdapter.getBondedDevices();
        for (BluetoothDevice device : mBtDevices) {
            device.connectGatt(context, false, btleGattCallback);
        }

        btAdapter.startLeScan(leScanCallback);
    }


}
