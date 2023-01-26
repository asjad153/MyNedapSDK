package com.example.nedapsdk.bluetoothservices.encryption;

import android.util.Log;
import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
public class Aes128
{
    static public  final byte[] const_Rb = {
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, (byte) 0x87
    };
    public static String keyDiversification(String masterKey, String uida)
    {
        String fullKeyAHexString="";
        String dk12="";
        String M ="01"+uida;
        if(M.length() < 64) {
            String padding="8";
            for(int i=0;i<63-M.length();i++)
            {
                padding = padding+"0";
            }
            String D =M+padding;
            String keyTwo= generateKeyTwo(masterKey);
            Log.i("Key22",keyTwo);
            // Last 16 bytes xor with k2.
            byte[] xorWithK2 = xor_128(hexStringToByteArray(D.substring(32)),hexStringToByteArray(keyTwo));
            dk12= D.substring(0, 32) +byteArrayToHexString(xorWithK2);
            Log.i("dk12",dk12);
        }
        else if(M.length()==64){
            String reducedM = M.substring(0,32);
            byte[] xorWithK1 = xor_128(hexStringToByteArray(M.substring(32,64)),hexStringToByteArray(generateKeyOne(masterKey)));
            dk12 = reducedM + byteArrayToHexString(xorWithK1);
            //if no padding was used xor with k1.
        }
        else if(M.length()>64)
        {
            String reducedM = M.substring(0,64);
            String firstreducedM = reducedM.substring(0,32);
            byte[] xorWithK1 = xor_128(hexStringToByteArray(reducedM.substring(32,64)),hexStringToByteArray(generateKeyOne(masterKey)));
            dk12 = firstreducedM+byteArrayToHexString(xorWithK1);
        }
        try {
            SecretKeySpec skeySpec = new SecretKeySpec(hexStringToByteArray(masterKey), "AES");
            Cipher cipher = Cipher.getInstance("AES/CBC/NoPadding");
            cipher.init(1, skeySpec,new IvParameterSpec(new byte[]{0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0}));
            fullKeyAHexString = byteArrayToHexString(cipher.doFinal(hexStringToByteArray(dk12)));
            Log.i("Full key",fullKeyAHexString);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return fullKeyAHexString.substring(32,64);
    }
    public static String generateKeyTwo(String masterKey)
    {

        String hexString="";
        String k2="";

        String keyOne= generateKeyOne(masterKey);
        Log.i("checking k1",keyOne);
        byte[] k1Byte = hexStringToByteArray(keyOne);
        if(((k1Byte[0] & 0x80) == 0))
        {

            hexString=  byteArrayToHexString(leftshift_onebit(k1Byte));
            k2 = hexString;
        }
        else
        {
            byte[] leftShiftDataK2= leftshift_onebit(k1Byte);
            byte[] result= xor_128(leftShiftDataK2, const_Rb);
            k2= byteArrayToHexString(result);
        }
        return k2;
    }
    public static String generateKeyOne(String masterKey)
    {
        String const_Zero ="00000000000000000000000000000000";
        String k1 ="";
        byte [] L = AES_128(hexStringToByteArray(masterKey),hexStringToByteArray(const_Zero) );
        if(((L[0] & 0x80) == 0)) {
            byte [] leftShift = leftshift_onebit(L);
            k1 = byteArrayToHexString(leftShift);
        }
        else {
            byte [] leftShift = leftshift_onebit(L);
            byte[] XOR = xor_128(leftShift,hexStringToByteArray(const_Zero));
            k1=byteArrayToHexString(XOR);
        }
        return k1;
    }
    static public  byte[] AES_128(byte[] key, byte[] input) {
        byte[]  fullKeyahexString={};
        try {
            SecretKeySpec skeySpec = new SecretKeySpec(key, "AES");
            Cipher cipher = Cipher.getInstance("AES/CBC/NoPadding");
            cipher.init(1, skeySpec,new IvParameterSpec(new byte[]{0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0}));
            fullKeyahexString =cipher.doFinal(input);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return fullKeyahexString;
    }
    public static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for(int i = 0; i < len; i += 2) {
            data[i / 2] = (byte)((Character.digit(s.charAt(i), 16) << 4) + Character.digit(s.charAt(i + 1), 16));
        }
        return data;
    }
    public static String byteArrayToHexString(byte[] buf) {
        if (buf == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (byte b : buf) {
            sb.append(String.format("%02X ", b & 0xFF));
        }
        return sb.toString().trim().replace(" ", "");
    }
    static public  byte[] xor_128(byte[] a, byte[] b) {
        byte[] out=new byte[16];
        for (int i = 0; i < a.length; i++) {
            out[i] = (byte) (a[i] ^ b[i]);
        }
        return out;
    }
    static public  byte[] leftshift_onebit(byte[] input) {
        byte[] output = new byte[16];
        byte overflow = 0;
        for (int i = 15; i >= 0; i--) {
            output[i] = (byte) (input[i] << 1);
            output[i] |= overflow;
            overflow = (byte) (input[i] < 0 ? 1 : 0);
        }
        return output;
    }
    public static byte[] decrypt(byte[] raw, byte[] encrypted) throws Exception {
        SecretKeySpec skeySpec = new SecretKeySpec(raw, "AES");
        Cipher cipher = Cipher.getInstance("AES/ECB/NoPadding ");
        cipher.init(Cipher.DECRYPT_MODE, skeySpec);
        return cipher.doFinal(encrypted);
    }
    public static byte[] encrypt(byte[] raw, byte[] clear) throws Exception {
        SecretKeySpec skeySpec = new SecretKeySpec(raw, "AES");
        Cipher cipher = Cipher.getInstance("AES/ECB/NoPadding ");
        cipher.init(Cipher.ENCRYPT_MODE, skeySpec);
        return cipher.doFinal(clear);
    }
}