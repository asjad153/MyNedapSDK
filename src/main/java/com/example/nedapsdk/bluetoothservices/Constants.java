package com.example.nedapsdk.bluetoothservices;

import java.util.UUID;

public class Constants {
    public static final long SCAN_PERIOD = 1000;

    public static final int CONNECTING_STATUS = 0;
    public static final int CONNECTED_STATUS = 1;
    public static final int DISCONNECTED_STATUS = -1;
    public static final int CONNECTION_LOST_STATUS = -2;

    public static final int MESSAGE_READ = 2;

    public final static String ACTION_CONNECTED = "pk.mohammadadnan.esgsmartapp.ACTION_CONNECTED";
    public final static String ACTION_DISCONNECTED = "pk.mohammadadnan.esgsmartapp.ACTION_DISCONNECTED";
    public final static String ACTION_SCAN = "pk.mohammadadnan.esgsmartapp.ACTION_SCAN";
    public final static String ACTION_MESSAGE = "pk.mohammadadnan.esgsmartapp.ACTION_MESSAGE";

    public final static String EXTRAS_MESSAGE = "pk.mohammadadnan.esgsmartapp.EXTRAS_MESSAGE";

    public static final UUID MACE_SERVICE = UUID.fromString("87b1de8d-e7cb-4ea8-a8e4-290209522c83");

    public static final UUID MACE_ID = UUID.fromString("e68a5c09-aef8-4447-8f10-f3339898dee9"); //ID from App
    public static final UUID MACE_RX = UUID.fromString("540810c2-d573-11e5-ab30-625662870761"); //RX to App
    public static final UUID MACE_TX = UUID.fromString("54080bd6-d573-11e5-ab30-625662870761"); //TX from App

//    public static final UUID MACE_SERVICE = UUID.fromString("00002523-1212-efde-2523-785feabcd223");
//
//    public static final UUID MACE_ID = UUID.fromString("00002527-1212-efde-2523-785feabcd223"); //ID from App
//    public static final UUID MACE_RX = UUID.fromString("00002524-1212-efde-2523-785feabcd223"); //RX to App
//    public static final UUID MACE_TX = UUID.fromString("00002525-1212-efde-2523-785feabcd223"); //TX from App

    public static final UUID NOTIFICATION_DESCRIPTOR_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

    public static final byte AUTH_CHALLENGE = 0x41;
    private static final byte AUTH_RESPONSE = 0x42;
    public static final byte AUTH_FINISH = 0x43;

}
