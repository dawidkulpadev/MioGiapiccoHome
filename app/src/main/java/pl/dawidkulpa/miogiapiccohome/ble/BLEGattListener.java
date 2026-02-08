package pl.dawidkulpa.miogiapiccohome.ble;

public interface BLEGattListener {
    void onConnectionStateChange(boolean isConnected);
    void onServicesDiscovered();
    void onCharacteristicValueReceived(String uuid, byte[] data);
    void onCharacteristicChanged(String uuid, byte[] value);
    void onCharacteristicWriteFinished(String uuid);
    void onDescriptorWriteFinished(String uuid);
}