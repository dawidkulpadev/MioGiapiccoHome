package pl.dawidkulpa.miogiapiccohome.ble.bleln_encryption;

import android.util.Base64;
import android.util.Log;

import java.security.InvalidAlgorithmParameterException;
import java.security.NoSuchAlgorithmException;

import pl.dawidkulpa.miogiapiccohome.BuildConfig;

public class BLELNAuthSecrets {
    private final byte[] manuPubKey;
    private final byte[] devPrivKey;
    private final byte[] devPubKey;

    public static BLELNAuthSecrets generate(){
        BLELNAuthSecrets blelnAuthSecrets;

        try{
            BLELNEncryption.RawKeyPair kp= BLELNEncryption.generateSigningKeys();

            String manuKeyBase64 = BuildConfig.MANUFACTURER_PUBLIC_KEY;
            byte[] rawManuKey= Base64.decode(manuKeyBase64, Base64.NO_WRAP);
            Log.e("BLELNAuthSecrets", "Pub key len: "+rawManuKey.length);

            blelnAuthSecrets= new BLELNAuthSecrets(rawManuKey, kp.privateKey, kp.publicKey);
        } catch (NoSuchAlgorithmException | InvalidAlgorithmParameterException e){
            return null;
        }

        return blelnAuthSecrets;
    }

    public BLELNAuthSecrets(byte[] manuPubKey64, byte[] devPrivKey32, byte[] devPubKey64){
        manuPubKey= manuPubKey64;
        devPrivKey= devPrivKey32;
        devPubKey= devPubKey64;
    }

    public byte[] getManuPubKey() {
        return manuPubKey;
    }

    public byte[] getDevPrivKey() {
        return devPrivKey;
    }

    public byte[] getDevPubKey() {
        return devPubKey;
    }

}
