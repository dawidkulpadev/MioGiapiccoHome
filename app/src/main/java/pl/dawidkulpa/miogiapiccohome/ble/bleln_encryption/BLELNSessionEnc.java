package pl.dawidkulpa.miogiapiccohome.ble.bleln_encryption;

import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PublicKey;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.ECParameterSpec;
import java.util.Objects;

import javax.crypto.Cipher;
import javax.crypto.KeyAgreement;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class BLELNSessionEnc {
    private static final byte[] AAD_HDR = "DATAv1".getBytes();

    private int epochLE;
    private int sidBE;
    private byte[] keyM2F;
    private byte[] keyF2M;
    private byte[] myPub65;
    private byte[] myNonce12;
    private ECParameterSpec ecSpec;
    KeyPair kp;

    private int txCtrC2S = 0;
    private int rxCtrS2C = 0;

    public void makeMyKeys(){
        try {
            KeyPairGenerator kpg = KeyPairGenerator.getInstance("EC");
            kpg.initialize(new ECGenParameterSpec(BLELNEncryption.CURVE));
            kp = kpg.generateKeyPair();
            ECPublicKey myPub = (ECPublicKey) kp.getPublic();
            ecSpec = myPub.getParams();
            myPub65 = BLELNEncryption.encodeUncompressed(myPub);
            myNonce12 = BLELNEncryption.randomBytes(12);
        } catch (Exception e){
            Log.d("BLELNKeys", "doKeyExchange: "+e.getMessage());
        }

    }

    public void deriveFriendsKey(byte[] keyexPacket){
        int off = 0;
        int ver = keyexPacket[off++] & 0xFF;
        if (ver != 1) throw new IllegalArgumentException("Bad version: "+ver);
        epochLE = ByteBuffer.wrap(keyexPacket, off, 4).order(ByteOrder.LITTLE_ENDIAN).getInt(); off += 4;
        byte[] psk_salt = BLELNEncryption.slice(keyexPacket, off, 32); off += 32;
        byte[] friendsPub65 = BLELNEncryption.slice(keyexPacket, off, 65); off += 65;
        byte[] friendsNonce12 = BLELNEncryption.slice(keyexPacket, off, 12);

        try{
            PublicKey srvPub = BLELNEncryption.decodeUncompressedPublic(friendsPub65, ecSpec);
            KeyAgreement ka = KeyAgreement.getInstance("ECDH");
            ka.init(kp.getPrivate());
            ka.doPhase(srvPub, true);
            byte[] shared = ka.generateSecret();
            if (shared.length != 32) {
                shared = BLELNEncryption.leftPad(shared, 32);
            }

            byte[] epochLE4 = BLELNEncryption.intToLE(epochLE);
            byte[] salt36 = BLELNEncryption.concat(psk_salt, epochLE4);

            byte[] prk = BLELNEncryption.hkdfExtract(salt36, shared);
            keyF2M = BLELNEncryption.hkdfExpand(prk, BLELNEncryption.concat(BLELNEncryption.KDF_INFO_SESS_KEY.getBytes(), myPub65, friendsPub65, myNonce12, friendsNonce12), 32);
            keyM2F = BLELNEncryption.hkdfExpand(prk, BLELNEncryption.concat(BLELNEncryption.KDF_INFO_SESS_KEY.getBytes(), friendsPub65, myPub65, friendsNonce12, myNonce12), 32);


            byte[] sid2 = BLELNEncryption.hkdfExpand(prk, BLELNEncryption.KDF_INFO_SID.getBytes(), 2);
            sidBE = ((sid2[0] & 0xFF) << 8) | (sid2[1] & 0xFF);
        } catch (Exception e){
            Log.d("BLELNKeys", "doKeyExchange: "+e.getMessage());
        }

    }

    public byte[] createMyKeyExMessage(){
        ByteBuffer rx = ByteBuffer.allocate(1 + 65 + 12);
        rx.put((byte) 1).put(myPub65).put(myNonce12);

        return rx.array();
    }

    public void delete(){
        epochLE= 0;
        sidBE= 0;
        keyM2F= null;
        keyF2M= null;
        myPub65= null;
        myNonce12= null;
        ecSpec= null;
        kp= null;
    }

    public byte[] encrypt(String msg) {
        byte[] plain= msg.getBytes(StandardCharsets.UTF_8);

        txCtrC2S++;
        byte[] ctrBE = BLELNEncryption.intToBE(txCtrC2S);
        byte[] nonce = BLELNEncryption.randomBytes(12);

        ByteBuffer b = ByteBuffer.allocate(AAD_HDR.length + 2 + 4);
        b.put(AAD_HDR);
        b.putShort((short) (sidBE & 0xFFFF));    // BE – ByteBuffer domyślnie BE
        b.order(ByteOrder.LITTLE_ENDIAN).putInt(epochLE);

        try {
            Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
            SecretKey sk = new SecretKeySpec(keyM2F, "AES");
            c.init(Cipher.ENCRYPT_MODE, sk, new GCMParameterSpec(128, nonce));
            c.updateAAD(b.array());
            byte[] cipher = c.doFinal(plain);

            int tagLen = 16;
            int ctLen = cipher.length - tagLen;
            byte[] ct = new byte[ctLen];
            byte[] tag = new byte[tagLen];
            System.arraycopy(cipher, 0, ct, 0, ctLen);
            System.arraycopy(cipher, ctLen, tag, 0, tagLen);

            // [ctr:4 BE][nonce:12][cipher][tag]
            ByteBuffer buf = ByteBuffer.allocate(4 + 12 + ctLen + 16);
            buf.put(ctrBE).put(nonce).put(ct).put(tag);
            return buf.array();
        } catch (Exception e){
            return null;
        }
    }

    public String decrypt(byte[] packet){
        if (packet.length < 4 + 12 + 16) throw new IllegalArgumentException("pkt too short");

        int ctr= ByteBuffer.wrap(packet, 0, 4).order(ByteOrder.BIG_ENDIAN).getInt();
        if (ctr <= rxCtrS2C) throw new SecurityException("replay/old ctr");
        byte[] nonce = BLELNEncryption.slice(packet, 4, 12);
        int ctLen = packet.length - (4 + 12 + 16);
        byte[] ct = BLELNEncryption.slice(packet, 4 + 12, ctLen);
        byte[] tag = BLELNEncryption.slice(packet, packet.length - 16, 16);

        ByteBuffer b = ByteBuffer.allocate(AAD_HDR.length + 2 + 4);
        b.put(AAD_HDR);
        b.putShort((short) (sidBE & 0xFFFF));    // BE – ByteBuffer domyślnie BE
        b.order(ByteOrder.LITTLE_ENDIAN).putInt(epochLE);

        try {
            Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
            SecretKey sk = new SecretKeySpec(keyF2M, "AES");
            c.init(Cipher.DECRYPT_MODE, sk, new GCMParameterSpec(128, nonce));
            c.updateAAD(b.array(), 0, b.capacity());

            byte[] both = new byte[ct.length + tag.length];
            System.arraycopy(ct, 0, both, 0, ct.length);
            System.arraycopy(tag, 0, both, ct.length, tag.length);

            byte[] plain = c.doFinal(both);
            rxCtrS2C = ctr;
            return new String(plain, StandardCharsets.UTF_8);
        } catch (Exception e){
            Log.d("decryptS2C", Objects.requireNonNull(e.getMessage()));
            return "";
        }
    }
}

