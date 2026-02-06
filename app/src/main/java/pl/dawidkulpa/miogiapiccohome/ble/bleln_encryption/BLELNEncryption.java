package pl.dawidkulpa.miogiapiccohome.ble.bleln_encryption;

import android.util.Log;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECParameterSpec;
import java.security.spec.ECPoint;
import java.security.spec.ECPublicKeySpec;

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import java.security.AlgorithmParameters;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.ECPrivateKeySpec;
import java.util.Arrays;

public class BLELNEncryption {
    public static final String CURVE = "secp256r1"; // prime256v1
    public static final String HMAC = "HmacSHA256";
    public static final String KDF_INFO_SESS_KEY = "BLEv1|sessKey";
    public static final String KDF_INFO_SID = "BLEv1|sid";
    private static final String SIGNATURE_ALGORITHM = "SHA256withECDSA";

    private static final SecureRandom RNG = new SecureRandom();

    public static byte[] leftPad(byte[] in, int len) {
        if (in.length == len) return in;
        byte[] out = new byte[len];
        if (in.length > len) {
            System.arraycopy(in, in.length - len, out, 0, len);
        } else {
            System.arraycopy(in, 0, out, len - in.length, in.length);
        }
        return out;
    }

    public static byte[] encodeUncompressed(ECPublicKey pub) {
        ECPoint w = pub.getW();
        byte[] x = leftPad(w.getAffineX().toByteArray(), 32);
        byte[] y = leftPad(w.getAffineY().toByteArray(), 32);
        byte[] out = new byte[65];
        out[0] = 0x04;
        System.arraycopy(x, 0, out, 1, 32);
        System.arraycopy(y, 0, out, 33, 32);
        return out;
    }

    public static PublicKey decodeUncompressedPublic(byte[] pub65, ECParameterSpec params) throws Exception {
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

    public static byte[] intToLE(int v) {
        return ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(v).array();
    }

    public static byte[] intToBE(int v) {
        return ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN).putInt(v).array();
    }

    public static byte[] concat(byte[]... parts) {
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

    public static byte[] hkdfExtract(byte[] salt, byte[] ikm) throws Exception {
        Mac mac = Mac.getInstance(HMAC);
        SecretKey sk = new SecretKeySpec(salt, "RAW");
        mac.init(sk);
        return mac.doFinal(ikm); // PRK
    }

    public static byte[] hkdfExpand(byte[] prk, byte[] info, int outLen) throws Exception {
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

    public static byte[] randomBytes(int n) {
        byte[] b = new byte[n];
        RNG.nextBytes(b);
        return b;
    }

    public static byte[] slice(byte[] a, int off, int len) {
        byte[] out = new byte[len];
        System.arraycopy(a, off, out, 0, len);
        return out;
    }

    public static boolean verifySign_ECDSA_P256(byte[] data, byte[] signatureRaw, byte[] pubKeyRaw) {
        try {
            if (data == null || signatureRaw == null || signatureRaw.length != 64 || pubKeyRaw == null) {
                return false;
            }

            ECPoint pubPoint = parsePublicKeyRaw(pubKeyRaw);
            ECParameterSpec ecSpec = getECParameterSpec();
            ECPublicKeySpec pubSpec = new ECPublicKeySpec(pubPoint, ecSpec);
            KeyFactory kf = KeyFactory.getInstance("EC");
            PublicKey publicKey = kf.generatePublic(pubSpec);

            byte[] signatureDer = rawToDerSignature(signatureRaw);

            Signature sig = Signature.getInstance(SIGNATURE_ALGORITHM);
            sig.initVerify(publicKey);
            sig.update(data);
            return sig.verify(signatureDer);

        } catch (Exception e) {
            Log.e("BLELNEncryption", "Failed veryfing signature: "+ e.getMessage());
            return false;
        }
    }


    public static byte[] signData_ECDSA_P256(byte[] data, byte[] privKeyRaw) {
        try {
            if (data == null || privKeyRaw == null || privKeyRaw.length != 32) {
                return null;
            }

            BigInteger s = new BigInteger(1, privKeyRaw);
            ECParameterSpec ecSpec = getECParameterSpec();
            ECPrivateKeySpec privSpec = new ECPrivateKeySpec(s, ecSpec);
            KeyFactory kf = KeyFactory.getInstance("EC");
            PrivateKey privateKey = kf.generatePrivate(privSpec);

            Signature sig = Signature.getInstance(SIGNATURE_ALGORITHM);
            sig.initSign(privateKey);
            sig.update(data);
            byte[] signatureDer = sig.sign();

            return derToRawSignature(signatureDer);

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private static ECParameterSpec getECParameterSpec() throws Exception {
        AlgorithmParameters params = AlgorithmParameters.getInstance("EC");
        params.init(new ECGenParameterSpec(CURVE));
        return params.getParameterSpec(ECParameterSpec.class);
    }

    private static ECPoint parsePublicKeyRaw(byte[] pubKeyRaw) {
        int start = 0;
        if (pubKeyRaw.length == 65 && pubKeyRaw[0] == 0x04) {
            start = 1;
        } else if (pubKeyRaw.length != 64) {
            throw new IllegalArgumentException("Illegal public key length");
        }

        byte[] xBytes = Arrays.copyOfRange(pubKeyRaw, start, start + 32);
        byte[] yBytes = Arrays.copyOfRange(pubKeyRaw, start + 32, start + 64);

        BigInteger x = new BigInteger(1, xBytes);
        BigInteger y = new BigInteger(1, yBytes);

        return new ECPoint(x, y);
    }

    private static byte[] rawToDerSignature(byte[] raw) throws Exception {
        byte[] r = Arrays.copyOfRange(raw, 0, 32);
        byte[] s = Arrays.copyOfRange(raw, 32, 64);

        BigInteger rBig = new BigInteger(1, r);
        BigInteger sBig = new BigInteger(1, s);

        byte[] rBytes = encodeDerInteger(rBig);
        byte[] sBytes = encodeDerInteger(sBig);

        int totalLen = rBytes.length + sBytes.length;
        byte[] der = new byte[totalLen + 2];
        der[0] = 0x30; // SEQUENCE
        der[1] = (byte) totalLen;
        System.arraycopy(rBytes, 0, der, 2, rBytes.length);
        System.arraycopy(sBytes, 0, der, 2 + rBytes.length, sBytes.length);

        return der;
    }

    private static byte[] derToRawSignature(byte[] der) throws Exception {
        int rLenIdx = 3;
        int rLen = der[rLenIdx];
        int rStart = rLenIdx + 1;

        int sLenIdx = rStart + rLen + 1;
        int sLen = der[sLenIdx];
        int sStart = sLenIdx + 1;

        byte[] rBytesRaw = Arrays.copyOfRange(der, rStart, rStart + rLen);
        byte[] sBytesRaw = Arrays.copyOfRange(der, sStart, sStart + sLen);

        BigInteger rBig = new BigInteger(1, rBytesRaw);
        BigInteger sBig = new BigInteger(1, sBytesRaw);

        byte[] output = new byte[64];
        byte[] rArr = to32Bytes(rBig);
        byte[] sArr = to32Bytes(sBig);

        System.arraycopy(rArr, 0, output, 0, 32);
        System.arraycopy(sArr, 0, output, 32, 32);

        return output;
    }

    private static byte[] encodeDerInteger(BigInteger val) {
        byte[] bytes = val.toByteArray();
        byte[] output = new byte[bytes.length + 2];
        output[0] = 0x02; // INTEGER
        output[1] = (byte) bytes.length;
        System.arraycopy(bytes, 0, output, 2, bytes.length);
        return output;
    }

    private static byte[] to32Bytes(BigInteger val) {
        byte[] source = val.toByteArray();
        if (source.length == 32) return source;

        byte[] target = new byte[32];
        if (source.length > 32) {
            System.arraycopy(source, source.length - 32, target, 0, 32);
        } else {
            System.arraycopy(source, 0, target, 32 - source.length, source.length);
        }
        return target;
    }
}
