package pl.dawidkulpa.miogiapiccohome;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;

import java.util.List;

public class BLEConfigurerBLELNCharacteristics extends BLEConfigurerCharacteristics{
    private static final String BLE_CHAR_UUID_KEY_TX      = "345ac506-c96e-45c6-a418-56a2ef2d6072";
    private static final String BLE_CHAR_UUID_KEY_RX      = "b675ddff-679e-458d-9960-939d8bb03572";
    private static final String BLE_CHAR_UUID_DATA_TX     = "566f9eb0-a95e-4c18-bc45-79bd396389af";
    private static final String BLE_CHAR_UUID_DATA_RX     = "f6ffba4e-eea1-4728-8b1a-7789f9a22da8";

    // Bluetooth characteristics (null before discovered)
    private BluetoothGattCharacteristic keyTxChar =null;
    private BluetoothGattCharacteristic keyRxChar =null;
    private BluetoothGattCharacteristic dataTxChar =null;
    private BluetoothGattCharacteristic dataRxChar =null;

    private enum State {Init, EnablingKeyNotifications, EnablingDataNotifications, KeyExchangeProcess, Ready}
    private State state;

    BLEConfigurerBLELNCharacteristics(BluetoothLeService bleService, WiFiListListener listener) {
        super(bleService, listener);
        state= State.Init;
    }

    @Override
    boolean discoverCharacteristics(BluetoothGattService gattService) {
        keyTxChar = null;
        keyRxChar = null;
        dataRxChar = null;
        dataTxChar = null;

        List<BluetoothGattCharacteristic> gattCharacteristics =
                gattService.getCharacteristics();

        for(BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics){
            String uuid = gattCharacteristic.getUuid().toString();

            switch (uuid){
                case BLE_CHAR_UUID_KEY_TX:
                    keyTxChar = gattCharacteristic;
                    break;
                case BLE_CHAR_UUID_KEY_RX:
                    keyRxChar = gattCharacteristic;
                    break;
                case BLE_CHAR_UUID_DATA_TX:
                    dataTxChar = gattCharacteristic;
                    break;
                case BLE_CHAR_UUID_DATA_RX:
                    dataRxChar = gattCharacteristic;
                    break;
            }
        }

        // Check if every characteristic was loaded
        return (keyTxChar !=null && keyRxChar !=null &&
                dataTxChar !=null && dataRxChar !=null);
    }

    @Override
    void startWrite() {

    }

    @Override
    void startRead() {
        state= State.EnablingKeyNotifications;
        bleService.allowNotificationsFor(keyTxChar);
    }

    @Override
    boolean preparedAndReady() {
        return state==State.Ready;
    }

    @Override
    void onPreparingDataAvailable(String uuid, String data) {
        // Data received on key exchange process
    }

    @Override
    void onPreparingDescriptorUpdate() {
        if(state==State.EnablingKeyNotifications){
            state= State.EnablingDataNotifications;
            bleService.allowNotificationsFor(dataTxChar);
        } else {
            state= State.KeyExchangeProcess;
            // TODO: Start key exchange process - need to wait for keyTx notification
        }
    }

    @Override
    void onReadyCharacteristicChanged(String uuid) {

    }

    @Override
    void onReadyDataAvailable(String uuid, String data) {

    }

    @Override
    void onWritingWriteComplete(String uuid) {

    }

    @Override
    void onWritingCharacteristicChanged(String uuid, String data) {

    }

    @Override
    boolean writingComplete() {
        return false;
    }
}
