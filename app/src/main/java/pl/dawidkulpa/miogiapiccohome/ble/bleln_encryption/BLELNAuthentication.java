package pl.dawidkulpa.miogiapiccohome.ble.bleln_encryption;

import android.util.Base64;

import java.nio.charset.StandardCharsets;

public class BLELNAuthentication {
    private final byte[] myMac6;
    private final byte[] certSign64;
    private final byte[] manuPubKey64;
    private final byte[] myPrivateKey32;
    private final byte[] myPublicKey64;


    public BLELNAuthentication(byte[] mac, byte[] certSign, byte[] manuPubKey, byte[] myPrivateKey, byte[] myPublicKey){
        myMac6= mac;
        certSign64= certSign;
        manuPubKey64= manuPubKey;
        myPrivateKey32= myPrivateKey;
        myPublicKey64= myPublicKey;
    }

    public String getSignedCert(){
        String out="";

        out+= "2;";
        out+= Base64.encodeToString(myMac6, Base64.NO_WRAP)+";";
        out+= Base64.encodeToString(myPublicKey64, Base64.NO_WRAP)+",";
        out+= Base64.encodeToString(certSign64, Base64.NO_WRAP);

        return out;
    }

    public BLELNCert verifyCert(String cert, String sign){
        int gen;

        byte[] signRaw= Base64.decode(sign, Base64.NO_WRAP);

        boolean r= BLELNEncryption.verifySign_ECDSA_P256(
                    cert.getBytes(StandardCharsets.UTF_8),
                    signRaw,
                    manuPubKey64);

        if(r){
            String[] certSplit= cert.split(";");

            try {
                gen = Integer.parseInt(certSplit[0]);
            } catch (NumberFormatException e){
                return null;
            }

            byte[] fMac= Base64.decode(certSplit[1], Base64.NO_WRAP);
            byte[] fPubKey= Base64.decode(certSplit[2], Base64.NO_WRAP);

            if(fMac.length==6 && fPubKey.length==64){
                return new BLELNCert(gen, fMac, fPubKey);
            }
        }

        return null;
    }

    public byte[] signData(byte[] d){
        return BLELNEncryption.signData_ECDSA_P256(d, myPrivateKey32);
    }
}
