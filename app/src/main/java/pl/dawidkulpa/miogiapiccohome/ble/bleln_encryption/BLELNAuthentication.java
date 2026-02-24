package pl.dawidkulpa.miogiapiccohome.ble.bleln_encryption;

import android.util.Base64;
import android.util.Log;

import java.nio.charset.StandardCharsets;

public class BLELNAuthentication {
    BLELNCert cert;
    BLELNAuthSecrets authSecrets;

    public BLELNAuthentication(BLELNAuthSecrets myAuthSecrets, BLELNCert myCert){
        authSecrets= myAuthSecrets;
        cert= myCert;
    }

    public String getSignedCert(){
        String out="";

        out+= "2;";
        out+= cert.getUid()+";";
        out+= Base64.encodeToString(cert.getMac(), Base64.NO_WRAP)+";";
        out+= Base64.encodeToString(cert.getPubKey(), Base64.NO_WRAP)+",";
        out+= Base64.encodeToString(cert.getSignature(), Base64.NO_WRAP);

        return out;
    }

    public BLELNCert verifyCert(String friendsCert, String sign){
        int gen;
        int userId;

        byte[] signRaw= Base64.decode(sign, Base64.NO_WRAP);

        boolean r= BLELNEncryption.verifySign_ECDSA_P256(
                    friendsCert.getBytes(StandardCharsets.UTF_8),
                    signRaw,
                    authSecrets.getManuPubKey());

        if(r){
            String[] certSplit= friendsCert.split(";");

            try {
                gen = Integer.parseInt(certSplit[0]);
                userId= Integer.parseInt(certSplit[1]);
            } catch (NumberFormatException e){
                return null;
            }


            byte[] fMac= Base64.decode(certSplit[2], Base64.NO_WRAP);
            byte[] fPubKey= Base64.decode(certSplit[3], Base64.NO_WRAP);
            byte[] fCerSign= Base64.decode(sign, Base64.NO_WRAP);

            if(fMac.length==6 && fPubKey.length==64){
                return new BLELNCert(gen, fMac, fPubKey, userId, fCerSign);
            }
        }

        return null;
    }

    public byte[] signData(byte[] d){
        return BLELNEncryption.signData_ECDSA_P256(d, authSecrets.getDevPrivKey());
    }

    public int getUserId(){
        return cert.getUid();
    }
}
