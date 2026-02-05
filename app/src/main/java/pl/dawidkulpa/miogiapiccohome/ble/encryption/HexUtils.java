package pl.dawidkulpa.miogiapiccohome.ble.encryption;

public class HexUtils {
    public static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            // %02X oznacza: 2 cyfry hex, duże litery, z wiodącym zerem
            sb.append(String.format("%02X", b));
        }
        return sb.toString();
    }
}
