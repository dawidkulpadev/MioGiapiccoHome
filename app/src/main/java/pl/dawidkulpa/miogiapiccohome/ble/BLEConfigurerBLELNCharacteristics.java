package pl.dawidkulpa.miogiapiccohome.ble;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.util.Base64;
import android.util.Log;

import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;

import pl.dawidkulpa.miogiapiccohome.ble.encryption.BLELNAuthentication;
import pl.dawidkulpa.miogiapiccohome.ble.encryption.BLELNCert;
import pl.dawidkulpa.miogiapiccohome.ble.encryption.BLELNConnCtx;

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
    private static final String TAG="BLEConfigurerBLELNCharacteristics";
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

    private enum State {Init, EnablingNotifications, KeyExchangeProcess, Authentication, ReadingConfig, Ready, WritingConfig, ConfigWritten}
    private State state;

    BLELNAuthentication authStore;
    BLELNConnCtx connCtx;

    BLEConfigurerBLELNCharacteristics(BLEGattService bleService, ActionsListener listener) {
        super(bleService, listener);
        state= State.Init;

        byte[] mac= HexFormat.of().parseHex("112233445566");
        byte[] certSign= HexFormat.of().parseHex("69043bd37c6d74df9792a64fc4d868868454297bcc96a7377d0a2f658e6d2cd7af47f02b607d9ecfe3ca6ac133cc673d1bbdf3c1be7e857417d43ed2ad5ce9c3");
        byte[] manuPubKey= HexFormat.of().parseHex("f1b64c144a0789f56815ac8e900a216c4a713cd066f77cbd979a1205ef7a4f6bac99ccb4f06fbd03b2032698e72c00c58b2846e56a6712d537e7167e2fd1bfe3");
        byte[] myPrivateKey= HexFormat.of().parseHex("aa67c7f5edb0690e46deaf5e2d5952bfbf702ad8535d18b15f3fc72d574c40b7");
        byte[] myPublicKey= HexFormat.of().parseHex("06f01d7befa683d3eb1f4081c44cb036015d282cc42d8fd9cb3a557d0eb6d67cfc8ed94368811ca6a0df28fd1ac5e2be8c2516ff5bc5caf62240c13ec26894a0");

        // TODO: set authStore keys and cert!!!!!!
        authStore= new BLELNAuthentication(mac, certSign, manuPubKey, myPrivateKey, myPublicKey);
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
                    Log.e(TAG, "KeyTX discovered");
                    keyTxChar = gattCharacteristic;
                    break;
                case BLE_CHAR_UUID_KEY_RX:
                    Log.e(TAG, "KeyRX discovered");
                    keyRxChar = gattCharacteristic;
                    break;
                case BLE_CHAR_UUID_DATA_TX:
                    Log.e(TAG, "DataTX discovered");
                    dataTxChar = gattCharacteristic;
                    break;
                case BLE_CHAR_UUID_DATA_RX:
                    Log.e(TAG, "DataRX discovered");
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
    void preparingStateOnValueReceived(String uuid, byte[] data) {
    }

    @Override
    void preparingStateOnDescriptorUpdate(String uuid) {
        Log.e(TAG, "Descriptor update");
        if(uuid.equals(BLE_CHAR_UUID_DATA_TX)){
            Log.e(TAG, "Notfity for DataTx enabled");
            bleService.allowNotificationsFor(keyTxChar);
        } else if(uuid.equals(BLE_CHAR_UUID_KEY_TX)) {
            timeoutWatchdog.stop();
            Log.e(TAG, "Notfity for KeyTx enabled");
            state= State.KeyExchangeProcess;
            timeoutWatchdog.start(TIMEOUT_KEY_EXCHANGE_TIME, this::onTimeout);
        }
    }

    boolean sendGetConfigCmd(String var){
        byte[] cmd= connCtx.encrypt("$CONFIG,GET,"+var);

        if(cmd!=null){
            bleService.writeCharacteristic(dataRxChar, cmd);
            return true;
        }

        return false;
    }

    boolean sendSetConfigCmd(String var, String value){
        byte[] cmd= connCtx.encrypt("$CONFIG,SET,"+var+","+value);

        if(cmd!=null){
            bleService.writeCharacteristic(dataRxChar, cmd);
            return true;
        }

        return false;
    }

    void sendRebootCmd(){
        byte[] cmd= connCtx.encrypt("$REBOOT");
        if(cmd!=null){
            bleService.writeCharacteristic(dataRxChar, cmd);
        }
    }

    @Override
    void preparingStateOnNotify(String uuid, byte[] data) {
        if(Objects.equals(uuid, BLE_CHAR_UUID_KEY_TX)){
            if (state == State.KeyExchangeProcess) {
                connCtx = new BLELNConnCtx();
                connCtx.makeSessionKey();
                try {
                    connCtx.deriveFriendsKey(data);
                } catch (IllegalArgumentException e){
                    actionsListener.onError(ErrorCode.SyncFailed);
                }

                bleService.writeCharacteristic(keyRxChar, connCtx.createMyKeyExMessage());
                state= State.Authentication;
                Log.e(TAG, "Key write started");
            } else if(state == State.Authentication){
                if(connCtx!=null){
                    if(connCtx.getState() == BLELNConnCtx.State.WaitingForCert){
                        Log.e(TAG, "Received keyTx message while WaitingForCert");
                        String plainMsg= connCtx.decrypt(data);
                        if (!plainMsg.isEmpty()) {
                            String[] parts= plainMsg.split(",");
                            if(Objects.equals(parts[0], "$CERT") && parts.length==3){
                                BLELNCert cert= authStore.verifyCert(parts[1], parts[2]);

                                if(cert != null){
                                    connCtx.setFriendsCertData(cert.getMac(), cert.getPubKey());
                                    sendCertToServer();
                                    connCtx.setState(BLELNConnCtx.State.ChallengeResponseCli);
                                } else {
                                    actionsListener.onError(ErrorCode.SyncFailed);
                                    Log.e(TAG, "WaitingForCert - invalid cert");
                                }
                            } else {
                                Log.e(TAG, "WaitingForCert - wrong message");
                                actionsListener.onError(ErrorCode.SyncFailed);
                            }
                        } else {
                            actionsListener.onError(ErrorCode.SyncFailed);
                        }
                    } else if(connCtx.getState() == BLELNConnCtx.State.ChallengeResponseCli){
                        Log.e(TAG, "Received keyTx message while ChallengeResponseCli");
                        String plainMsg= connCtx.decrypt(data);
                        if (!plainMsg.isEmpty()) {
                            String[] parts = plainMsg.split(",");
                            if (Objects.equals(parts[0], "$CHRN") && parts.length == 2) {
                                sendChallengeNonceSign(parts[1]);
                                connCtx.setState(BLELNConnCtx.State.ChallengeResponseSer);
                            } else {
                                actionsListener.onError(ErrorCode.SyncFailed);
                            }
                        } else {
                            actionsListener.onError(ErrorCode.SyncFailed);
                        }
                    } else if(connCtx.getState() == BLELNConnCtx.State.ChallengeResponseSer){
                        Log.e(TAG, "Received keyTx message while ChallengeResponseSer");

                        String plainMsg= connCtx.decrypt(data);
                        if (!plainMsg.isEmpty()) {
                            String[] parts = plainMsg.split(",");
                            if (Objects.equals(parts[0], "$CHRA") && parts.length == 2) {
                                byte[] nonceSign=  Base64.decode(parts[1], Base64.NO_WRAP);
                                if(connCtx.verifyChallengeResponseAnswer(nonceSign)){
                                    String msg="$AUOK";
                                    msg += ",1";
                                    byte[] encMsg = connCtx.encrypt(msg);
                                    if(encMsg != null) {
                                        connCtx.setState(BLELNConnCtx.State.Authorised);
                                        Log.e(TAG, "auth success");
                                        bleService.writeCharacteristic(keyRxChar, encMsg);
                                        state= State.ReadingConfig;
                                        timeoutWatchdog.start(TIMEOUT_READ_CONFIG_TIME, this::onTimeout);
                                        if(!sendGetConfigCmd("wssid"))
                                            actionsListener.onError(ErrorCode.SyncFailed);
                                    } else {
                                        Log.e(TAG, "failed encrypting cert msg");
                                    }
                                } else {
                                    Log.e(TAG, "ChallengeResponseSeri - invalid sign");
                                    actionsListener.onError(ErrorCode.SyncFailed);
                                }
                            } else {
                                actionsListener.onError(ErrorCode.SyncFailed);
                            }
                        } else {
                            actionsListener.onError(ErrorCode.SyncFailed);
                        }
                    }
                } else {
                    actionsListener.onError(ErrorCode.SyncFailed);
                }
            }
        } else if(Objects.equals(uuid, BLE_CHAR_UUID_DATA_TX)){
            if (state == State.ReadingConfig) {
                List<String> msg= splitCsvRespectingQuotes(connCtx.decrypt(data));
                if(msg.size()>=3 && msg.get(0).equals("$CONFIG") && msg.get(1).equals("VAL")){
                    String val="";
                    if(msg.size()==4){
                        val= msg.get(3);
                    }
                    switch (msg.get(2)) {
                        case "wssid":
                            Log.d(TAG, "WiFi SSID read");
                            configWifiSSID = val;
                            if(!sendGetConfigCmd("pcklk")){
                                actionsListener.onError(ErrorCode.SyncFailed);
                                return;
                            }
                            break;
                        case "pcklk":
                            Log.d(TAG, "Picklock read");
                            configPicklock = val;
                            if(!sendGetConfigCmd("tzone")){
                                actionsListener.onError(ErrorCode.SyncFailed);
                                return;
                            }
                            break;
                        case "tzone":
                            Log.d(TAG, "Timezone read");
                            configWifiSSID = val;
                            if(!sendGetConfigCmd("mac")){
                                actionsListener.onError(ErrorCode.SyncFailed);
                                return;
                            }
                            break;
                        case "mac":
                            timeoutWatchdog.stop();
                            Log.d(TAG, "MAC read");
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

    /*** Connection context not protected! */
    private void sendCertToServer() {
        String msg= "$CERT";
        msg += "," + authStore.getSignedCert();

        byte[] encMsg= connCtx.encrypt(msg);
        if(encMsg != null) {
            bleService.writeCharacteristic(keyRxChar, encMsg);
        } else {
            Log.e(TAG, "Failed encrypting cert msg");
        }
    }

    public void sendChallengeNonceSign(String nonceB64) {
        byte[] nonceRaw;             // Servers nonce raw bytes
        byte[] friendsNonceSign;     // Servers nonce sing I have created

        // Sign nonce
        nonceRaw = Base64.decode(nonceB64, Base64.NO_WRAP);
        friendsNonceSign= authStore.signData(nonceRaw);

        // Create clients nonce
        connCtx.generateTestNonce();
        String myNonceB64= connCtx.getTestNonceBase64();

        // Create BLE message
        String msg= "$CHRAN";
        String friendsNonceSignB64= Base64.encodeToString(friendsNonceSign, Base64.NO_WRAP);
        msg += "," + friendsNonceSignB64 + "," + myNonceB64;

        byte[] encMsg= connCtx.encrypt(msg);
        if(encMsg != null) {
            bleService.writeCharacteristic(keyRxChar, encMsg);
        } else {
            Log.e(TAG, "Failed encrypting cert msg");
        }
    }

    @Override
    void readyStateOnNotify(String uuid, byte[] data) {
       List<String> msg= splitCsvRespectingQuotes(connCtx.decrypt(data));

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
    void readyStateOnValueReceived(String uuid, byte[] data) {}

    @Override
    void writingStateOnWriteComplete(String uuid) {}

    @Override
    void writingStateOnNotify(String uuid, byte[] data) {
        if(state==State.WritingConfig){
            if(uuid.equals(BLE_CHAR_UUID_DATA_TX)){
                List<String> msg= splitCsvRespectingQuotes(connCtx.decrypt(data));
                if(msg.size()>=3 && msg.get(0).equals("$CONFIG") && msg.get(1).equals("SETOK")){
                    switch (msg.get(2)) {
                        case "wssid":
                            Log.d(TAG, "WiFi SSID written");
                            if(!sendSetConfigCmd("wpsk", "\""+configWifiPSK+"\"")){
                                actionsListener.onError(ErrorCode.WriteFailed);
                                return;
                            }
                            break;
                        case "wpsk":
                            Log.d(TAG, "WiFi PSK written");
                            if(!sendSetConfigCmd("pcklk", configPicklock)){
                                actionsListener.onError(ErrorCode.WriteFailed);
                                return;
                            }
                            break;
                        case "pcklk":
                            Log.d(TAG, "Picklock written");
                            if(!sendSetConfigCmd("tzone", configTimezone)){
                                actionsListener.onError(ErrorCode.WriteFailed);
                                return;
                            }
                            break;
                        case "tzone":
                            Log.d(TAG, "Timezone read");
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
        connCtx.delete();
        connCtx= null;
    }
}
