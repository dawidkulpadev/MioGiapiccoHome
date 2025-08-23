package pl.dawidkulpa.miogiapiccohome;

import android.annotation.SuppressLint;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class BluetoothLeService extends Service {
    public final static String ACTION_GATT_CONNECTED =
            "pl.dawidkulpa.bluetooth.le.ACTION_GATT_CONNECTED";
    public final static String ACTION_GATT_DISCONNECTED =
            "pl.dawidkulpa.bluetooth.le.ACTION_GATT_DISCONNECTED";
    public final static String ACTION_GATT_SERVICES_DISCOVERED =
            "pl.dawidkulpa.bluetooth.le.ACTION_GATT_SERVICES_DISCOVERED";
    public final static String ACTION_DATA_AVAILABLE =
            "pl.dawidkulpa.bluetooth.le.ACTION_DATA_AVAILABLE";
    public final static String ACTION_DATA_WRITE_COMPLETE=
            "pl.dawidkulpa.bluetooth.le.ACTION_DATA_WRITE_COMPLETE";
    public final static String ACTION_DESCR_WRITE_COMPLETE=
            "pl.dawidkulpa.bluetooth.le.ACTION_DESCR_WRITE_COMPLETE";
    public final static String ACTION_CHARACTERISTIC_CHANGED=
            "pl.dawidkulpa.bluetooth.le.ACTION_CHARACTERISTIC_CHANGED";

    private BluetoothAdapter bluetoothAdapter;
    private final Binder binder = new LocalBinder();
    private BluetoothGatt bluetoothGatt;
    private final ArrayList<BluetoothGattCharacteristic> pendingCharacteristicsRead= new ArrayList<>();
    private boolean isReadingCharacteristic=false;
    private final ArrayList<BluetoothGattCharacteristic> pendingCharacteristicsWrite= new ArrayList<>();
    private boolean isWritingCharacteristic=false;

    private final BluetoothGattCallback bluetoothGattCallback = new BluetoothGattCallback() {
        @SuppressLint("MissingPermission")
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            Log.e("BluetoothGattCallback", "Broadcast state change");
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                pendingCharacteristicsRead.clear();
                pendingCharacteristicsWrite.clear();
                isReadingCharacteristic= false;
                isWritingCharacteristic= false;
                broadcastUpdate(ACTION_GATT_CONNECTED);
                bluetoothGatt.discoverServices();
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                broadcastUpdate(ACTION_GATT_DISCONNECTED);
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED);

            } else {
                Log.e("BluetoothGattCallback", "failed onServicesDiscovered received: " + status);
            }
        }


        @SuppressLint("MissingPermission")
        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                BluetoothGattCharacteristic characteristic,
                int status) {
            Log.w("BluetoothGattCallback", "onCharacteristicRead received: " + status);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
            }

            if(!pendingCharacteristicsRead.isEmpty()){
                Log.e("BluetoothLeService", "Characteristics read queue not empty. Reading next...");
                isReadingCharacteristic= true;
                BluetoothGattCharacteristic ch= pendingCharacteristicsRead.get(0);
                pendingCharacteristicsRead.remove(0);
                bluetoothGatt.readCharacteristic(ch);
            } else {
                isReadingCharacteristic= false;
            }
        }

        @SuppressLint("MissingPermission")
        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            broadcastUpdate(ACTION_DATA_WRITE_COMPLETE, characteristic);

            if(!pendingCharacteristicsWrite.isEmpty()){
                Log.e("BluetoothLeService", "Characteristics write queue not empty. Writing next...");
                isWritingCharacteristic= true;
                BluetoothGattCharacteristic ch= pendingCharacteristicsWrite.get(0);
                pendingCharacteristicsWrite.remove(0);
                bluetoothGatt.writeCharacteristic(ch);
            } else {
                isWritingCharacteristic= false;
            }
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            if(status==BluetoothGatt.GATT_SUCCESS)
                broadcastUpdate(ACTION_DESCR_WRITE_COMPLETE);
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            broadcastUpdate(ACTION_CHARACTERISTIC_CHANGED, characteristic);
        }
    };

    public List<BluetoothGattService> getSupportedGattServices() {
        if (bluetoothGatt == null) return null;
        return bluetoothGatt.getServices();
    }

    private void broadcastUpdate(final String action) {
        final Intent intent = new Intent(action);
        sendBroadcast(intent);
    }

    private void broadcastUpdate(final String action, final BluetoothGattDescriptor descriptor) {
        final Intent intent = new Intent(action);
        intent.putExtra("uuid", descriptor.getCharacteristic().getUuid().toString());
        sendBroadcast(intent);
    }

    private void broadcastUpdate(final String action,
                                 final BluetoothGattCharacteristic characteristic) {
        final Intent intent = new Intent(action);
        intent.putExtra("uuid", characteristic.getUuid().toString());
        intent.putExtra("data", characteristic.getStringValue(0));
        sendBroadcast(intent);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    public class LocalBinder extends Binder {
        public BluetoothLeService getService() {
            return BluetoothLeService.this;
        }
    }



    public boolean initialize() {
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            Log.e("BluetoothLeService", "Unable to obtain a BluetoothAdapter.");
            return false;
        }
        return true;
    }

    @SuppressWarnings("UnusedReturnValue")
    @SuppressLint("MissingPermission")
    public boolean connect(final String address) {
        if (bluetoothAdapter == null || address == null) {
            Log.e("BluetoothLeService", "BluetoothAdapter not initialized or unspecified address.");
            return false;
        }
        try {
            final BluetoothDevice device = bluetoothAdapter.getRemoteDevice(address);
            // connect to the GATT server on the device
            bluetoothGatt = device.connectGatt(this, false, bluetoothGattCallback);
            return true;
        } catch (IllegalArgumentException exception) {
            Log.e("BluetoothLeService", "Device not found with provided address.  Unable to connect.");
            return false;
        }
    }

    @SuppressLint("MissingPermission")
    public void readCharacteristic(BluetoothGattCharacteristic characteristic) {
        if (bluetoothGatt == null) {
            Log.w("BluetoothLeService", "BluetoothGatt not initialized");
            return;
        }
        if(isReadingCharacteristic){
            pendingCharacteristicsRead.add(characteristic);
        } else {
            isReadingCharacteristic= true;
            bluetoothGatt.readCharacteristic(characteristic);
        }
    }

    @SuppressLint("MissingPermission")
    public void writeCharacteristic(BluetoothGattCharacteristic characteristic) {
        if (bluetoothGatt == null) {
            Log.w("BluetoothLeService", "BluetoothGatt not initialized");
            return;
        }

        if(isWritingCharacteristic){
            pendingCharacteristicsWrite.add(characteristic);
        } else {
            isWritingCharacteristic= true;
            bluetoothGatt.writeCharacteristic(characteristic);
        }
    }

    @SuppressLint("MissingPermission")
    public void allowNotificationsFor(BluetoothGattCharacteristic characteristic){
        UUID uuid = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
        bluetoothGatt.setCharacteristicNotification(characteristic, true);

        Log.d("AllowNotificationsFor", characteristic.getDescriptors().toString());
        BluetoothGattDescriptor descriptor = characteristic.getDescriptor(uuid);
        descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
        bluetoothGatt.writeDescriptor(descriptor);
    }

    @Override
    public boolean onUnbind(Intent intent) {
        close();
        return super.onUnbind(intent);
    }

    @SuppressLint("MissingPermission")
    public void close() {
        if (bluetoothGatt == null) {
            return;
        }
        bluetoothGatt.close();
        bluetoothGatt = null;
    }
}
