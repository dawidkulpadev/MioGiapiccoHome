package pl.dawidkulpa.miogiapiccohome.ble;

import android.bluetooth.BluetoothGattCharacteristic;

public class CharacteristicValuePair {
    private final BluetoothGattCharacteristic ch;
    private final byte[] v;

    public CharacteristicValuePair(BluetoothGattCharacteristic characteristic, byte[] value){
        ch= characteristic;
        v= value;
    }

    public BluetoothGattCharacteristic getCharacteristic(){
        return ch;
    }

    public byte[] getValue(){
        return v;
    }
}
