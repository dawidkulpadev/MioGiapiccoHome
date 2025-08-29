package pl.dawidkulpa.miogiapiccohome.ble;

import android.util.Log;

import javax.crypto.Cipher;
import javax.crypto.KeyAgreement;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.security.*;
        import java.security.interfaces.ECPublicKey;
import java.security.spec.*;
import java.util.Objects;

public final class BLELNKeys {

    // ---- Stałe protokołu ----
    private static final String CURVE = "secp256r1"; // prime256v1
    private static final String HMAC = "HmacSHA256";
    private static final String KDF_INFO_SESS = "BLEv1|sess";
    private static final String KDF_INFO_K_C2S = "BLEv1|sessKey_c2s";
    private static final String KDF_INFO_K_S2C = "BLEv1|sessKey_s2c";
    private static final String KDF_INFO_SID = "BLEv1|sid";
    private static final byte[] AAD_HDR = "DATAv1".getBytes(); // stały prefix AAD

    private static final SecureRandom RNG = new SecureRandom();

    public static void hexDump(String label, byte[] data){
        StringBuilder log=new StringBuilder();
        log.append(label).append(": ");
        for(int i=0; i<data.length; i++){
            log.append(String.format("%02X", data[i]));
            if(i<data.length-1)
                log.append(" ");
        }
        Log.d("hexDump",log.toString());
    }

    // ======= Sesja klienta =======
    public static final class Session {
        public final int epochLE;               // epoch (LE w AAD)
        public final int sidBE;                 // 0..65535
        public final byte[] keyC2S;             // 32B AES-256-GCM
        public final byte[] keyS2C;             // 32B AES-256-GCM
        public final byte[] cliPub65;           // 0x04|X|Y
        public final byte[] cliNonce12;         // 12B
        public final byte[] keyexRxPacket;      // [ver=1|cliPub|cliNonce] – wyślij do KEYEX_RX

        // anty-replay (opcjonalnie używaj)
        private int txCtrC2S = 0;
        private int rxCtrS2C = 0;

        private Session(int epochLE, int sidBE, byte[] keyC2S, byte[] keyS2C,
                        byte[] cliPub65, byte[] cliNonce12, byte[] keyexRxPacket) {
            this.epochLE = epochLE;
            this.sidBE = sidBE;
            this.keyC2S = keyC2S;
            this.keyS2C = keyS2C;
            this.cliPub65 = cliPub65;
            this.cliNonce12 = cliNonce12;
            this.keyexRxPacket = keyexRxPacket;
        }

        public void printKeys(){
            hexDump("s2c", keyS2C);
            hexDump("c2s", keyC2S);
        }

        public byte[] encryptC2S(String msg) {
            byte[] plain= msg.getBytes(StandardCharsets.UTF_8);

            txCtrC2S++;
            byte[] ctrBE = intToBE(txCtrC2S);
            byte[] nonce = randomBytes(12);
            byte[] aad = makeAad(sidBE, epochLE);

            try {
                Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
                SecretKey sk = new SecretKeySpec(keyC2S, "AES");
                c.init(Cipher.ENCRYPT_MODE, sk, new GCMParameterSpec(128, nonce));
                c.updateAAD(aad);
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

        public String decryptS2C(byte[] packet){
            if (packet.length < 4 + 12 + 16) throw new IllegalArgumentException("pkt too short");
            int ctr = beToInt(packet, 0);
            if (ctr <= rxCtrS2C) throw new SecurityException("replay/old ctr");
            byte[] nonce = slice(packet, 4, 12);
            int ctLen = packet.length - (4 + 12 + 16);
            byte[] ct = slice(packet, 4 + 12, ctLen);
            byte[] tag = slice(packet, packet.length - 16, 16);

            byte[] aad = makeAad(sidBE, epochLE);

            try {
                Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
                SecretKey sk = new SecretKeySpec(keyS2C, "AES");
                c.init(Cipher.DECRYPT_MODE, sk, new GCMParameterSpec(128, nonce));
                c.updateAAD(aad);

                // Android oczekuje [cipher|tag] razem
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

        public String decryptC2S(byte[] packet){
            if (packet.length < 4 + 12 + 16) throw new IllegalArgumentException("pkt too short");
            int ctr = beToInt(packet, 0);
            //if (ctr <= rxCtrS2C) throw new SecurityException("replay/old ctr");
            byte[] nonce = slice(packet, 4, 12);
            int ctLen = packet.length - (4 + 12 + 16);
            byte[] ct = slice(packet, 4 + 12, ctLen);
            byte[] tag = slice(packet, packet.length - 16, 16);

            byte[] aad = makeAad(sidBE, epochLE);

            try {
                Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
                SecretKey sk = new SecretKeySpec(keyC2S, "AES");
                c.init(Cipher.DECRYPT_MODE, sk, new GCMParameterSpec(128, nonce));
                c.updateAAD(aad);

                // Android oczekuje [cipher|tag] razem
                byte[] both = new byte[ct.length + tag.length];
                System.arraycopy(ct, 0, both, 0, ct.length);
                System.arraycopy(tag, 0, both, ct.length, tag.length);

                byte[] plain = c.doFinal(both);
                //rxCtrS2C = ctr;
                return new String(plain, StandardCharsets.UTF_8);
            } catch (Exception e){
                Log.d("decryptS2C", Objects.requireNonNull(e.getMessage()));
                return "";
            }
        }
    }

    public static Session doKeyExchange(byte[] keyexTxPayloadFromServer){
        // KEYEX_TX = [ver=1][epoch:4 LE][salt:32][srvPub:65][srvNonce:12]
        if (keyexTxPayloadFromServer == null || keyexTxPayloadFromServer.length != 1 + 4 + 32 + 65 + 12){
            Log.d("BLELNKeys", "doKeyExchange: Bad KEYEX_TX length");
            return null;
        }

        int off = 0;
        int ver = keyexTxPayloadFromServer[off++] & 0xFF;
        if (ver != 1) throw new IllegalArgumentException("Bad version");
        int epochLE = leToInt(keyexTxPayloadFromServer, off); off += 4;
        byte[] salt32 = slice(keyexTxPayloadFromServer, off, 32); off += 32;
        byte[] srvPub65 = slice(keyexTxPayloadFromServer, off, 65); off += 65;
        byte[] srvNonce12 = slice(keyexTxPayloadFromServer, off, 12); off += 12;

        try {
            KeyPairGenerator kpg = KeyPairGenerator.getInstance("EC");
            kpg.initialize(new ECGenParameterSpec(CURVE));
            KeyPair kp = kpg.generateKeyPair();
            ECPublicKey cliPub = (ECPublicKey) kp.getPublic();
            ECParameterSpec ecSpec = cliPub.getParams();
            byte[] cliPub65 = encodeUncompressed(cliPub);
            byte[] cliNonce12 = randomBytes(12);

            // KEYEX_RX: [ver=1][cliPub:65][cliNonce:12]
            ByteBuffer rx = ByteBuffer.allocate(1 + 65 + 12);
            rx.put((byte) 1).put(cliPub65).put(cliNonce12);
            byte[] keyexRxPacket = rx.array();

            PublicKey srvPub = decodeUncompressedPublic(srvPub65, ecSpec);
            KeyAgreement ka = KeyAgreement.getInstance("ECDH");
            ka.init(kp.getPrivate());
            ka.doPhase(srvPub, true);
            byte[] shared = ka.generateSecret();               // powinno mieć 32B dla P-256
            if (shared.length != 32) {
                // niektóre implementacje zwracają fixed len – dla P-256 oczekujemy 32
                shared = leftPad(shared, 32);
            }

            byte[] epochLE4 = intToLE(epochLE);
            byte[] salt36 = concat(salt32, epochLE4);

            byte[] prk = hkdfExtract(salt36, shared);
            byte[] kC2S = hkdfExpand(prk, concat(KDF_INFO_K_C2S.getBytes(), srvPub65, cliPub65, srvNonce12, cliNonce12), 32);
            byte[] kS2C = hkdfExpand(prk, concat(KDF_INFO_K_S2C.getBytes(), srvPub65, cliPub65, srvNonce12, cliNonce12), 32);

            byte[] sid2 = hkdfExpand(prk, KDF_INFO_SID.getBytes(), 2);
            int sidBE = ((sid2[0] & 0xFF) << 8) | (sid2[1] & 0xFF);

            return new Session(epochLE, sidBE, kC2S, kS2C, cliPub65, cliNonce12, keyexRxPacket);
        } catch (Exception e){
            Log.d("BLELNKeys", "doKeyExchange: "+e.getMessage());
            return null;
        }
    }

    // ======= Pomocnicze: AAD, HKDF, EC punkt, bajty =======

    private static byte[] makeAad(int sidBE, int epochLE) {
        ByteBuffer b = ByteBuffer.allocate(AAD_HDR.length + 2 + 4);
        b.put(AAD_HDR);
        b.putShort((short) (sidBE & 0xFFFF));    // BE – ByteBuffer domyślnie BE
        b.order(ByteOrder.LITTLE_ENDIAN).putInt(epochLE);
        return b.array();
    }

    private static byte[] hkdfExtract(byte[] salt, byte[] ikm) throws Exception {
        Mac mac = Mac.getInstance(HMAC);
        SecretKey sk = new SecretKeySpec(salt, "RAW");
        mac.init(sk);
        return mac.doFinal(ikm); // PRK
    }

    private static byte[] hkdfExpand(byte[] prk, byte[] info, int outLen) throws Exception {
        Mac mac = Mac.getInstance(HMAC);
        mac.init(new SecretKeySpec(prk, "RAW"));
        int hashLen = 32;
        int n = (int) Math.ceil((double) outLen / hashLen);
        byte[] out = new byte[outLen];
        byte[] T = new byte[0];
        int pos = 0;
        for (int i = 1; i <= n; i++) {
            mac.reset();
            mac.update(T);
            mac.update(info);
            mac.update((byte) i);
            T = mac.doFinal();
            int toCopy = Math.min(hashLen, outLen - pos);
            System.arraycopy(T, 0, out, pos, toCopy);
            pos += toCopy;
        }
        return out;
    }

    private static PublicKey decodeUncompressedPublic(byte[] pub65, ECParameterSpec params) throws Exception {
        if (pub65.length < 1 || pub65[0] != 0x04)
            throw new IllegalArgumentException("Bad uncompressed point (no 0x04)");
        int coordLen = (pub65.length - 1) / 2;
        byte[] x = new byte[coordLen];
        byte[] y = new byte[coordLen];
        System.arraycopy(pub65, 1, x, 0, coordLen);
        System.arraycopy(pub65, 1 + coordLen, y, 0, coordLen);
        ECPoint w = new ECPoint(new BigInteger(1, x), new BigInteger(1, y));
        ECPublicKeySpec spec = new ECPublicKeySpec(w, params);
        return KeyFactory.getInstance("EC").generatePublic(spec);
    }

    private static byte[] encodeUncompressed(ECPublicKey pub) {
        ECPoint w = pub.getW();
        byte[] x = leftPad(w.getAffineX().toByteArray(), 32);
        byte[] y = leftPad(w.getAffineY().toByteArray(), 32);
        byte[] out = new byte[65];
        out[0] = 0x04;
        System.arraycopy(x, 0, out, 1, 32);
        System.arraycopy(y, 0, out, 33, 32);
        return out;
    }

    private static byte[] intToLE(int v) {
        return ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(v).array();
    }

    private static byte[] intToBE(int v) {
        return ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN).putInt(v).array();
    }

    private static int leToInt(byte[] a, int off) {
        return ByteBuffer.wrap(a, off, 4).order(ByteOrder.LITTLE_ENDIAN).getInt();
    }

    private static int beToInt(byte[] a, int off) {
        return ByteBuffer.wrap(a, off, 4).order(ByteOrder.BIG_ENDIAN).getInt();
    }

    private static byte[] randomBytes(int n) {
        byte[] b = new byte[n];
        RNG.nextBytes(b);
        return b;
    }

    private static byte[] slice(byte[] a, int off, int len) {
        byte[] out = new byte[len];
        System.arraycopy(a, off, out, 0, len);
        return out;
    }

    private static byte[] leftPad(byte[] in, int len) {
        if (in.length == len) return in;
        byte[] out = new byte[len];
        // jeśli za długie (np. 33B przez znak BigInteger), utnij z lewej
        if (in.length > len) {
            System.arraycopy(in, in.length - len, out, 0, len);
        } else {
            System.arraycopy(in, 0, out, len - in.length, in.length);
        }
        return out;
    }

    private static byte[] concat(byte[]... parts) {
        int tot = 0;
        for (byte[] p : parts) tot += p.length;
        byte[] out = new byte[tot];
        int o = 0;
        for (byte[] p : parts) {
            System.arraycopy(p, 0, out, o, p.length);
            o += p.length;
        }
        return out;
    }
}