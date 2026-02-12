package pl.dawidkulpa.miogiapiccohome.ble;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.util.Base64;
import android.util.Log;

import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;

import pl.dawidkulpa.miogiapiccohome.R;
import pl.dawidkulpa.miogiapiccohome.ble.bleln_encryption.BLELNAuthSecrets;
import pl.dawidkulpa.miogiapiccohome.ble.bleln_encryption.BLELNAuthentication;
import pl.dawidkulpa.miogiapiccohome.ble.bleln_encryption.BLELNCert;
import pl.dawidkulpa.miogiapiccohome.ble.bleln_encryption.BLELNConnCtx;

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

    BLEConfigurerBLELNCharacteristics(BLEGattService bleService, ActionsListener listener, BLELNCert myCert, BLELNAuthSecrets myAuthSecrets) {
        super(bleService, listener);
        state= State.Init;

        authStore= new BLELNAuthentication(myAuthSecrets, myCert);
    }

    public void onTimeout(){
        if(state==State.EnablingNotifications || state==State.KeyExchangeProcess || state==State.ReadingConfig) {
            Log.e(TAG, "Timeout on " + state.name());
            actionsListener.onError(ErrorCode.SyncFailed);
        } else if(state==State.WritingConfig){
            actionsListener.onError(ErrorCode.WriteFailed);
        }
    }

    public void sendUpdateProgress(int progress, int msgResId){
        if(actionsListener!=null){
            actionsListener.onSyncProgress(progress, msgResId);
        }
    }

    @Override
    boolean discoverCharacteristics(BluetoothGattService gattService) {
        keyTxChar = null;
        keyRxChar = null;
        dataRxChar = null;
        dataTxChar = null;

        sendUpdateProgress(10, -1);

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

        sendUpdateProgress(15, R.string.message_connect_step_gen2_encrypting_connection);

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

        if (inQuotes) {
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
                sendUpdateProgress(25, R.string.message_connect_step_gen2_authorisation);
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
                                    if(cert.getUid()==-1 || cert.getUid()==authStore.getUserId()) {
                                        connCtx.setFriendsCertData(cert.getMac(), cert.getPubKey());
                                        sendCertToServer();
                                        connCtx.setState(BLELNConnCtx.State.ChallengeResponseCli);
                                    } else {
                                        actionsListener.onError(ErrorCode.SyncFailed);
                                        Log.e(TAG, "WaitingForCert - not my device");
                                        connCtx.setState(BLELNConnCtx.State.AuthFailed);
                                    }
                                } else {
                                    connCtx.setState(BLELNConnCtx.State.AuthFailed);
                                    actionsListener.onError(ErrorCode.SyncFailed);
                                    Log.e(TAG, "WaitingForCert - invalid cert");
                                }
                            } else {
                                connCtx.setState(BLELNConnCtx.State.AuthFailed);
                                Log.e(TAG, "WaitingForCert - wrong message");
                                actionsListener.onError(ErrorCode.SyncFailed);
                            }
                            sendUpdateProgress(40, -1);
                        } else {
                            actionsListener.onError(ErrorCode.SyncFailed);
                            connCtx.setState(BLELNConnCtx.State.AuthFailed);
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
                            sendUpdateProgress(55, -1);
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
                                        if(!sendGetConfigCmd("wssid")) {
                                            Log.e(TAG, "Error sending wssid get command");
                                            actionsListener.onError(ErrorCode.SyncFailed);
                                        }
                                        sendUpdateProgress(70, R.string.message_connect_step_gen2_reading_config);
                                    } else {
                                        Log.e(TAG, "failed encrypting cert msg");
                                    }
                                } else {
                                    Log.e(TAG, "ChallengeResponseSeri - invalid sign");
                                    actionsListener.onError(ErrorCode.SyncFailed);
                                }
                            } else {
                                Log.e(TAG, "Server challenge response answer incorrect");
                                actionsListener.onError(ErrorCode.SyncFailed);
                            }
                        } else {
                            Log.e(TAG, "Server challenge response answer decrypt error");
                            actionsListener.onError(ErrorCode.SyncFailed);
                        }
                    }
                } else {
                    Log.e(TAG, "Authentication - connection context is null");
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
                                Log.e(TAG, "Error sending pcklk get command");
                                actionsListener.onError(ErrorCode.SyncFailed);
                                return;
                            }
                            sendUpdateProgress(75, -1);
                            break;
                        case "pcklk":
                            Log.d(TAG, "Picklock read");
                            configPicklock = val;
                            if(!sendGetConfigCmd("tzone")){
                                Log.e(TAG, "Error sending tzone get command");
                                actionsListener.onError(ErrorCode.SyncFailed);
                                return;
                            }
                            sendUpdateProgress(80, -1);
                            break;
                        case "tzone":
                            Log.d(TAG, "Timezone read");
                            configWifiSSID = val;
                            if(!sendGetConfigCmd("mac")){
                                Log.e(TAG, "Error sending mac get command");
                                actionsListener.onError(ErrorCode.SyncFailed);
                                return;
                            }
                            sendUpdateProgress(85, -1);
                            break;
                        case "mac":
                            Log.d(TAG, "MAC read: "+ val);
                            configMAC = val;
                            if(!sendGetConfigCmd("role")){
                                Log.e(TAG, "Error sending role get command");
                                actionsListener.onError(ErrorCode.SyncFailed);
                                return;
                            }
                            sendUpdateProgress(90, -1);
                            break;
                        case "role":
                            timeoutWatchdog.stop();
                            configRole= Integer.parseInt(val);
                            state = State.Ready;
                            sendUpdateProgress(100, -1);
                            break;
                        default:
                            Log.e(TAG, "Received unknown message: "+msg);
                            actionsListener.onError(ErrorCode.SyncFailed);
                            break;
                    }
                }
            }
        }
    }

    private void sendCertToServer() {
        String msg= "$CERT";
        msg += "," + authStore.getSignedCert();

        Log.e(TAG, "Cert: "+msg);

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
                            Log.d(TAG, "Timezone written");
                            if(!sendSetConfigCmd("uid", configUID)){
                                actionsListener.onError(ErrorCode.WriteFailed);
                                return;
                            }
                            break;
                        case "uid":
                            Log.d(TAG, "UId written");
                            if(!sendSetConfigCmd("role", String.valueOf(configRole))){
                                actionsListener.onError(ErrorCode.WriteFailed);
                                return;
                            }
                            break;
                        case "role":
                            Log.d(TAG, "Role written");
                            if(!sendSetConfigCmd("certsign", String.valueOf(configDevSignBase64))){
                                actionsListener.onError(ErrorCode.WriteFailed);
                                return;
                            }
                            break;
                        case "certsign":
                            Log.d(TAG, "certsign written");
                            sendRebootCmd();
                            timeoutWatchdog.stop();
                            state = State.ConfigWritten;
                            break;
                        default:
                            Log.e(TAG, "Unknown set command response "+msg);
                            actionsListener.onError(ErrorCode.SyncFailed);
                            break;
                    }
                }
            }
        }
    }

    @Override
    String getDevicesPubKey() {
        if(connCtx!=null){
            return Base64.encodeToString(connCtx.getPubKey(), Base64.NO_WRAP);
        }

        return "";
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
        if(connCtx!=null)
            connCtx.delete();
        connCtx= null;
    }
}
