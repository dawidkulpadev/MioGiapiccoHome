package pl.dawidkulpa.miogiapiccohome.ble;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.util.Log;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;


/**
 *
 * Config exchange
 * <p>
 * $CONFIG,[method],[var],[value] - Manipulate config vars
 *  - method: GET - get var value from device
 *            SET - set var value on device
 *            VAL - server response with variables value
 *            SETOK - set command response
 * - var:     variable name
 *              wssid
 *              wpsk
 *              pcklk
 *              tzone
 *              uid
 *              mac
 * - value:   applicable only for SET method. Value to be set for var
 * <p>
 * $WIFIL,[SSID_1],[SSID_2],....,[SSID_n] - WiFis list
 *  - SSID_n: n WiFi SSID
 *
 */

public class BLEConfigurerBLELNCharacteristics extends BLEConfigurerCharacteristics{
    private static final int TIMEOUT_ENABLE_NOTIFICATIONS_TIME      = 2000;
    private static final int TIMEOUT_KEY_EXCHANGE_TIME              = 4000;
    private static final int TIMEOUT_READ_CONFIG_TIME               = 4000;
    private static final int TIMEOUT_WRITE_CONFIG_TIME              = 4000;

    private static final String BLE_CHAR_UUID_KEY_TX      = "ef7cb0fc-53a4-4062-bb0e-25443e3a1f5d";
    private static final String BLE_CHAR_UUID_KEY_RX      = "345ac506-c96e-45c6-a418-56a2ef2d6072";
    private static final String BLE_CHAR_UUID_DATA_TX     = "b675ddff-679e-458d-9960-939d8bb03572";
    private static final String BLE_CHAR_UUID_DATA_RX     = "566f9eb0-a95e-4c18-bc45-79bd396389af";

    // Bluetooth characteristics (null before discovered)
    private BluetoothGattCharacteristic keyTxChar =null;
    private BluetoothGattCharacteristic keyRxChar =null;
    private BluetoothGattCharacteristic dataTxChar =null;
    private BluetoothGattCharacteristic dataRxChar =null;

    private enum State {Init, EnablingNotifications, KeyExchangeProcess, ReadingConfig, Ready, WritingConfig, ConfigWritten}
    private State state;

    private BLELNKeys.Session blelnSession;

    BLEConfigurerBLELNCharacteristics(BLEGattService bleService, ActionsListener listener) {
        super(bleService, listener);
        state= State.Init;
    }

    public void onTimeout(){
        if(state==State.EnablingNotifications || state==State.KeyExchangeProcess || state==State.ReadingConfig)
            actionsListener.onError(ErrorCode.SyncFailed);
        else if(state==State.WritingConfig){
            actionsListener.onError(ErrorCode.WriteFailed);
        }
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
                    Log.e("Characteristics dicover", "KeyTX discovered");
                    keyTxChar = gattCharacteristic;
                    break;
                case BLE_CHAR_UUID_KEY_RX:
                    Log.e("Characteristics dicover", "KeyRX discovered");
                    keyRxChar = gattCharacteristic;
                    break;
                case BLE_CHAR_UUID_DATA_TX:
                    Log.e("Characteristics dicover", "DataTX discovered");
                    dataTxChar = gattCharacteristic;
                    break;
                case BLE_CHAR_UUID_DATA_RX:
                    Log.e("Characteristics dicover", "DataRX discovered");
                    dataRxChar = gattCharacteristic;
                    break;
            }
        }

        // Check if every characteristic was loaded
        return (keyTxChar !=null && keyRxChar !=null &&
                dataTxChar !=null && dataRxChar !=null);
    }

    static private List<String> splitCsvRespectingQuotes(String s) {
        List<String> out = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        boolean inQuotes = false;
        boolean startedWithQuote = false;

        int n = s.length();
        for (int i = 0; i < n; i++) {
            char ch = s.charAt(i);

            if (inQuotes) {
                if (ch == '"') {
                    boolean nextIsSepOrEnd = (i + 1 == n) || (s.charAt(i + 1) == ',');
                    if (nextIsSepOrEnd) {
                        inQuotes = false;
                    } else {
                        cur.append('"');
                    }
                } else {
                    cur.append(ch);
                }
            } else {
                if (ch == ',') {
                    out.add(cur.toString());
                    cur.setLength(0);
                    startedWithQuote = false;
                } else if (ch == '"' && cur.length() == 0) {
                    inQuotes = true;
                    startedWithQuote = true;
                } else {
                    cur.append(ch);
                }
            }
        }

        if (inQuotes && startedWithQuote) {
            cur.insert(0, '"');
        }
        out.add(cur.toString());

        return out;
    }


    @Override
    void startWrite() {
        state= State.WritingConfig;
        timeoutWatchdog.start(TIMEOUT_WRITE_CONFIG_TIME, this::onTimeout);
        if(!sendSetConfigCmd("wssid","\""+configWifiSSID+"\"")){
            actionsListener.onError(ErrorCode.WriteFailed);
        }
    }

    @Override
    void startSync() {
        timeoutWatchdog.start(TIMEOUT_ENABLE_NOTIFICATIONS_TIME, this::onTimeout);
        state= State.EnablingNotifications;
        bleService.allowNotificationsFor(dataTxChar);
    }

    @Override
    boolean preparedAndReady() {
        return state==State.Ready;
    }

    @Override
    void onPreparingDataAvailable(String uuid, byte[] data) {
    }

    @Override
    void onPreparingDescriptorUpdate(String uuid) {
        if(uuid.equals(BLE_CHAR_UUID_DATA_TX)){
            bleService.allowNotificationsFor(keyTxChar);
        } else if(uuid.equals(BLE_CHAR_UUID_KEY_TX)) {
            timeoutWatchdog.stop();
            state= State.KeyExchangeProcess;
            timeoutWatchdog.start(TIMEOUT_KEY_EXCHANGE_TIME, this::onTimeout);
        }
    }

    boolean sendGetConfigCmd(String var){
        byte[] cmd= blelnSession.encryptC2S("$CONFIG,GET,"+var);

        if(cmd!=null){
            dataRxChar.setValue(cmd);
            bleService.writeCharacteristic(dataRxChar);
            return true;
        }

        return false;
    }

    boolean sendSetConfigCmd(String var, String value){
        byte[] cmd= blelnSession.encryptC2S("$CONFIG,SET,"+var+","+value);

        if(cmd!=null){
            dataRxChar.setValue(cmd);
            bleService.writeCharacteristic(dataRxChar);
            return true;
        }

        return false;
    }

    void sendRebootCmd(){
        byte[] cmd= blelnSession.encryptC2S("$REBOOT");
        if(cmd!=null){
            dataRxChar.setValue(cmd);
            bleService.writeCharacteristic(dataRxChar);
        }
    }

    @Override
    void onPreparingNotify(String uuid, byte[] data) {
        if(Objects.equals(uuid, BLE_CHAR_UUID_KEY_TX)){
            blelnSession = BLELNKeys.doKeyExchange(data);
            if(blelnSession!=null) {
                keyRxChar.setValue(blelnSession.keyexRxPacket);
                bleService.writeCharacteristic(keyRxChar);
                Log.e("KeyRX Characteristics", "Key write started");
            } else {
                actionsListener.onError(ErrorCode.SyncFailed);
            }
        } else if(Objects.equals(uuid, BLE_CHAR_UUID_DATA_TX)){
            if(state==State.KeyExchangeProcess){
                timeoutWatchdog.stop();
                String msg= blelnSession.decryptS2C(data);
                if(msg.equals("$HDSH,OK")){
                    state= State.ReadingConfig;
                    timeoutWatchdog.start(TIMEOUT_READ_CONFIG_TIME, this::onTimeout);
                    if(!sendGetConfigCmd("wssid"))
                        actionsListener.onError(ErrorCode.SyncFailed);
                } else {
                    actionsListener.onError(ErrorCode.SyncFailed);
                }
            } else if (state == State.ReadingConfig) {
                List<String> msg= splitCsvRespectingQuotes(blelnSession.decryptS2C(data));
                if(msg.size()>=3 && msg.get(0).equals("$CONFIG") && msg.get(1).equals("VAL")){
                    String val="";
                    if(msg.size()==4){
                        val= msg.get(3);
                    }
                    switch (msg.get(2)) {
                        case "wssid":
                            Log.d("BLELNCharacteristics", "WiFi SSID read");
                            configWifiSSID = val;
                            if(!sendGetConfigCmd("pcklk")){
                                actionsListener.onError(ErrorCode.SyncFailed);
                                return;
                            }
                            break;
                        case "pcklk":
                            Log.d("BLELNCharacteristics", "Picklock read");
                            configPicklock = val;
                            if(!sendGetConfigCmd("tzone")){
                                actionsListener.onError(ErrorCode.SyncFailed);
                                return;
                            }
                            break;
                        case "tzone":
                            Log.d("BLELNCharacteristics", "Timezone read");
                            configWifiSSID = val;
                            if(!sendGetConfigCmd("mac")){
                                actionsListener.onError(ErrorCode.SyncFailed);
                                return;
                            }
                            break;
                        case "mac":
                            timeoutWatchdog.stop();
                            Log.d("BLELNCharacteristics", "MAC read");
                            configMAC = val;
                            state = State.Ready;

                            break;
                        default:
                            actionsListener.onError(ErrorCode.SyncFailed);
                            break;
                    }
                }
            }
        }
    }

    @Override
    void onReadyNotify(String uuid, byte[] data) {
       List<String> msg= splitCsvRespectingQuotes(blelnSession.decryptS2C(data));

       if(Objects.equals(msg.get(0), "$WIFIL")){
           StringBuilder wifisList= new StringBuilder();
           for(int i=1; i<msg.size(); i++){
               wifisList.append(msg.get(i));
               if(i< msg.size()-1) wifisList.append(",");
           }
           actionsListener.onRefresh(wifisList.toString());
       }
    }

    @Override
    void onReadyDataAvailable(String uuid, byte[] data) {}

    @Override
    void onWritingWriteComplete(String uuid) {}

    @Override
    void onWritingNotify(String uuid, byte[] data) {
        Log.e("MSG", new String(data, StandardCharsets.UTF_8));
        if(state==State.WritingConfig){
            if(uuid.equals(BLE_CHAR_UUID_DATA_TX)){
                List<String> msg= splitCsvRespectingQuotes(blelnSession.decryptS2C(data));
                if(msg.size()>=3 && msg.get(0).equals("$CONFIG") && msg.get(1).equals("SETOK")){
                    switch (msg.get(2)) {
                        case "wssid":
                            Log.d("BLELNCharacteristics", "WiFi SSID written");
                            if(!sendSetConfigCmd("wpsk", "\""+configWifiPSK+"\"")){
                                actionsListener.onError(ErrorCode.WriteFailed);
                                return;
                            }
                            break;
                        case "wpsk":
                            Log.d("BLELNCharacteristics", "WiFi PSK written");
                            if(!sendSetConfigCmd("pcklk", configPicklock)){
                                actionsListener.onError(ErrorCode.WriteFailed);
                                return;
                            }
                            break;
                        case "pcklk":
                            Log.d("BLELNCharacteristics", "Picklock written");
                            if(!sendSetConfigCmd("tzone", configTimezone)){
                                actionsListener.onError(ErrorCode.WriteFailed);
                                return;
                            }
                            break;
                        case "tzone":
                            Log.d("BLELNCharacteristics", "Timezone read");
                            if(!sendSetConfigCmd("uid", configUID)){
                                actionsListener.onError(ErrorCode.WriteFailed);
                                return;
                            }
                            break;
                        case "uid":
                            sendRebootCmd();
                            timeoutWatchdog.stop();
                            state = State.ConfigWritten;
                            break;
                        default:
                            actionsListener.onError(ErrorCode.SyncFailed);
                            break;
                    }
                }
            }
        }
    }

    @Override
    boolean writingComplete() {
        return state==State.ConfigWritten;
    }

    @Override
    void restart() {
        finish();
        state = State.Init;
    }

    @Override
    void finish() {
        timeoutWatchdog.stop();
        keyTxChar =null;
        keyRxChar =null;
        dataTxChar =null;
        dataRxChar =null;
        blelnSession= null;
    }
}
