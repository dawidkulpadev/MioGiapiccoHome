package pl.dawidkulpa.miogiapiccohome.ble;

import android.annotation.SuppressLint;
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
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class BLEGattService extends Service {
    private BluetoothAdapter bluetoothAdapter;
    private final Binder binder = new LocalBinder();
    private BluetoothGatt bluetoothGatt;
    private BLEGattListener gattListener;
    private final ArrayList<BluetoothGattCharacteristic> pendingCharacteristicsRead= new ArrayList<>();
    private boolean isReadingCharacteristic=false;
    private final ArrayList<CharacteristicValuePair> pendingCharacteristicsWrite= new ArrayList<>();
    private boolean isWritingCharacteristic=false;

    private final BluetoothGattCallback bluetoothGattCallback = new BluetoothGattCallback() {
        @SuppressLint("MissingPermission")
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            Log.e("BluetoothGattCallback", "Broadcast state change");
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                gatt.requestMtu(247);
                pendingCharacteristicsRead.clear();
                pendingCharacteristicsWrite.clear();
                isReadingCharacteristic = false;
                isWritingCharacteristic = false;
            }

            if (gattListener != null) {
                gattListener.onConnectionStateChange(newState == BluetoothProfile.STATE_CONNECTED);
            }
        }

        @SuppressLint("MissingPermission")
        @Override
        public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
            Log.d("onMtuChanged", "MTU=" + mtu);
            gatt.discoverServices();
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                if(gattListener!=null){
                    gattListener.onServicesDiscovered();
                }
            } else {
                Log.e("BluetoothGattCallback", "failed onServicesDiscovered received: " + status);
            }
        }

        @SuppressLint("MissingPermission")
        @Override
        public void onCharacteristicRead(@NonNull BluetoothGatt gatt, @NonNull BluetoothGattCharacteristic characteristic, @NonNull byte[] value, int status) {
            Log.w("BluetoothGattCallback", "onCharacteristicRead received: " + status);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                if(gattListener!=null){
                    gattListener.onCharacteristicValueReceived(characteristic.getUuid().toString(), value);
                }
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
            if (gattListener != null) {
                gattListener.onCharacteristicWriteFinished(characteristic.getUuid().toString());
            }
            if(!pendingCharacteristicsWrite.isEmpty()){
                Log.e("BluetoothLeService",
                        "Characteristics write queue not empty. Writing next..."+
                                pendingCharacteristicsWrite.get(0).getCharacteristic().getUuid().toString());
                isWritingCharacteristic= true;
                CharacteristicValuePair chv= pendingCharacteristicsWrite.get(0);
                pendingCharacteristicsWrite.remove(0);
                bluetoothGatt.writeCharacteristic(chv.getCharacteristic(), chv.getValue(), BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
            } else {
                isWritingCharacteristic= false;
            }
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            if(status==BluetoothGatt.GATT_SUCCESS){
                if(gattListener!=null){
                    gattListener.onDescriptorWriteFinished(descriptor.getCharacteristic().getUuid().toString());
                }
            }
        }

        @Override
        public void onCharacteristicChanged(@NonNull BluetoothGatt gatt, @NonNull BluetoothGattCharacteristic characteristic, @NonNull byte[] value) {
            if (gattListener != null) {
                gattListener.onCharacteristicChanged(
                        characteristic.getUuid().toString(),
                        value
                );
            }
        }
    };

    public void setBleEventListener(BLEGattListener listener) {
        this.gattListener = listener;
    }

    public List<BluetoothGattService> getSupportedGattServices() {
        if (bluetoothGatt == null) return null;
        return bluetoothGatt.getServices();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    public class LocalBinder extends Binder {
        public BLEGattService getService() {
            return BLEGattService.this;
        }
    }

    public boolean initialize() {
        BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        if (bluetoothManager != null) {
            bluetoothAdapter = bluetoothManager.getAdapter();
        }

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
            BluetoothDevice device = bluetoothAdapter.getRemoteDevice(address);
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
    public void writeCharacteristic(BluetoothGattCharacteristic characteristic, byte[] value) {
        if (bluetoothGatt == null) {
            Log.w("BluetoothLeService", "BluetoothGatt not initialized");
            return;
        }

        if(isWritingCharacteristic){
            pendingCharacteristicsWrite.add(new CharacteristicValuePair(characteristic, value));
        } else {
            isWritingCharacteristic= true;
            bluetoothGatt.writeCharacteristic(characteristic, value, BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
        }
    }

    @SuppressLint("MissingPermission")
    public void allowNotificationsFor(BluetoothGattCharacteristic characteristic){
        UUID uuid = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
        bluetoothGatt.setCharacteristicNotification(characteristic, true);

        Log.d("AllowNotificationsFor", characteristic.getDescriptors().toString());
        BluetoothGattDescriptor descriptor = characteristic.getDescriptor(uuid);
        bluetoothGatt.writeDescriptor(descriptor, BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
    }

    @Override
    public boolean onUnbind(Intent intent) {
        finish();
        return super.onUnbind(intent);
    }

    @SuppressLint("MissingPermission")
    public void finish() {
        if (bluetoothGatt != null) {
            bluetoothGatt.disconnect();
            bluetoothGatt.close();
            bluetoothGatt= null;
        }

        if(bluetoothAdapter!=null){
            bluetoothAdapter= null;
        }
    }

    @SuppressLint("MissingPermission")
    public void restart() {
        if(bluetoothGatt!=null){
            bluetoothGatt.disconnect();
            bluetoothGatt.close();
            bluetoothGatt=null;
        }
    }
}
