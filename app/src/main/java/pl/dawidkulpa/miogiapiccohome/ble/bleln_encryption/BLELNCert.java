package pl.dawidkulpa.miogiapiccohome.ble.bleln_encryption;

public class BLELNCert {
    private final int gen;
    private final byte[] mac6;
    private final byte[] pubKey64;

    public BLELNCert(int gen, byte[] mac6, byte[] pubKey64){
        this.gen= gen;
        this.mac6= mac6;
        this.pubKey64= pubKey64;
    }

    public int getGeneration(){
        return gen;
    }

    public byte[] getMac(){
        return mac6;
    }

    public byte[] getPubKey(){
        return pubKey64;
    }
}
