package pl.dawidkulpa.miogiapiccohome.ble.encryption;

import android.util.Base64;

public class BLELNConnCtx {
    private static final String TAG="BLELNConnCtx";
    public enum State {New, Initialised, WaitingForCert, ChallengeResponseCli ,ChallengeResponseSer, Authorised, AuthFailed}

    private byte[] friendsPubKey64= null;
    private byte[] friendsMac6= null;
    private byte[] friendTestNonce48= null;
    private final BLELNSessionEnc sessionEnc;
    private State state;

    public BLELNConnCtx(){
        sessionEnc= new BLELNSessionEnc();
        state= State.New;
    }

    public void setState(State newState){
        state= newState;
    }

    public State getState(){
        return state;
    }

    public void setFriendsCertData(byte[] macAddress6, byte[] publicKey64){
        friendsMac6= macAddress6;
        friendsPubKey64= publicKey64;
    }

    public void generateTestNonce(){
        friendTestNonce48= BLELNEncryption.randomBytes(48);
    }

    public byte[] getTestNonce(){
        return friendTestNonce48;
    }

    public String getTestNonceBase64(){
        if(friendTestNonce48!=null)
            return Base64.encodeToString(friendTestNonce48, Base64.NO_WRAP);
        else
            return "";
    }

    public boolean verifyChallengeResponseAnswer(byte[] nonceSign64){
        return BLELNEncryption.verifySign_ECDSA_P256(friendTestNonce48, nonceSign64,
                friendsPubKey64);
    }

    public void makeSessionKey(){
        sessionEnc.makeMyKeys();
    }

    public void deriveFriendsKey(byte[] keyexPacket) {
        sessionEnc.deriveFriendsKey(keyexPacket);
        state= State.WaitingForCert;
    }

    public byte[] createMyKeyExMessage(){
        return sessionEnc.createMyKeyExMessage();
    }

    public byte[] encrypt(String msg) {
        return sessionEnc.encrypt(msg);
    }

    public String decrypt(byte[] packet){
        return sessionEnc.decrypt(packet);
    }

    public void delete(){
        sessionEnc.delete();
        friendsPubKey64= null;
        friendsMac6= null;
        friendTestNonce48= null;
    }
}
