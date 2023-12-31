package com.example.chema.monisport;

/**
 * Created by Chema on 09/08/2017.
 */

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import zephyr.android.BioHarnessBT.*;

public class ZephyrListener extends ConnectListenerImpl{
    private static final String TAG = "ZephyrListener";
    private Handler _OldHandler;
    private Handler _aNewHandler;
    final int GP_MSG_ID = 0x20;
    final int BREATHING_MSG_ID = 0x21;
    final int ECG_MSG_ID = 0x22;
    final int RtoR_MSG_ID = 0x24;
    final int ACCEL_100mg_MSG_ID = 0x2A;
    final int SUMMARY_MSG_ID = 0x2B;


    private int GP_HANDLER_ID = 0x20;

    private final int HEART_RATE = Constantes.HEART_RATE_ZEPHYR;
    private final int RESPIRATION_RATE = Constantes.RESPIRATION_RATE_ZEPHYR;
    //private final int SKIN_TEMPERATURE = Constantes.SKIN_TEMPERATURE_ZEPHYR;
    private final int POSTURE = Constantes.POSTURE_ZEPHYR;
    private final int PEAK_ACCLERATION = Constantes.PEAK_ACCLERATION_ZEPHYR;
    private final int ACCELERATION_COMPONENT = Constantes.ACCELERATION_COMPONENT;
    /*Creating the different Objects for different types of Packets*/
    private GeneralPacketInfo GPInfo = new GeneralPacketInfo();
    private ECGPacketInfo ECGInfoPacket = new ECGPacketInfo();
    private BreathingPacketInfo BreathingInfoPacket = new  BreathingPacketInfo();
    private RtoRPacketInfo RtoRInfoPacket = new RtoRPacketInfo();
    private AccelerometerPacketInfo AccInfoPacket = new AccelerometerPacketInfo();
    private SummaryPacketInfo SummaryInfoPacket = new SummaryPacketInfo();

    private PacketTypeRequest RqPacketType = new PacketTypeRequest();
    public ZephyrListener(Handler handler,Handler _NewHandler) {
        super(handler, null);
        _OldHandler= handler;
        _aNewHandler = _NewHandler;

        // TODO Auto-generated constructor stub

    }
    public void Connected(ConnectedEvent<BTClient> eventArgs) {
        Log.d(TAG,String.format("Connected to BioHarness %s.", eventArgs.getSource().getDevice().getName()));
        RqPacketType.GP_ENABLE = true;
        RqPacketType.BREATHING_ENABLE = true;
        RqPacketType.LOGGING_ENABLE = true;

        ZephyrProtocol _protocol = new ZephyrProtocol(eventArgs.getSource().getComms(), RqPacketType);
        _protocol.addZephyrPacketEventListener(new ZephyrPacketListener() {
            public void ReceivedPacket(ZephyrPacketEvent eventArgs) {
                ZephyrPacketArgs msg = eventArgs.getPacket();
                byte CRCFailStatus;
                byte RcvdBytes;

                CRCFailStatus = msg.getCRCStatus();
                RcvdBytes = msg.getNumRvcdBytes() ;
                int MsgID = msg.getMsgID();
                byte [] DataArray = msg.getBytes();

                switch (MsgID)
                {

                    case GP_MSG_ID:

                        //***************Displaying the Heart Rate********************************
                        int HRate =  GPInfo.GetHeartRate(DataArray);
                        Message text1 = _aNewHandler.obtainMessage(HEART_RATE);
                        Bundle b1 = new Bundle();
                        b1.putString("HeartRate", String.valueOf(HRate));
                        text1.setData(b1);
                        _aNewHandler.sendMessage(text1);
                        //Log.d(TAG,"Heart Rate is "+ HRate);

                        //***************Displaying the Respiration Rate********************************
                        double RespRate = GPInfo.GetRespirationRate(DataArray);

                        text1 = _aNewHandler.obtainMessage(RESPIRATION_RATE);
                        b1.putString("RespirationRate", String.valueOf(RespRate));
                        text1.setData(b1);
                        _aNewHandler.sendMessage(text1);
                        //Log.d(TAG,"Respiration Rate is "+ RespRate);

                        //***************Displaying the Skin Temperature*******************************


                        /*double SkinTempDbl = GPInfo.GetSkinTemperature(DataArray);
                        text1 = _aNewHandler.obtainMessage(SKIN_TEMPERATURE);
                        //Bundle b1 = new Bundle();
                        b1.putString("SkinTemperature", String.valueOf(SkinTempDbl));
                        text1.setData(b1);
                        _aNewHandler.sendMessage(text1);
                        Log.d(TAG,"Skin Temperature is "+ SkinTempDbl);*/

                        //***************Displaying the Posture******************************************

                        int PostureInt = GPInfo.GetPosture(DataArray);
                        text1 = _aNewHandler.obtainMessage(POSTURE);
                        b1.putString("Posture", String.valueOf(PostureInt));
                        text1.setData(b1);
                        _aNewHandler.sendMessage(text1);
                        //Log.d(TAG,"Posture is "+ PostureInt);

                        //***************Componentes aceleración******************************************
                        text1 = _aNewHandler.obtainMessage(ACCELERATION_COMPONENT);
                        double PeakAccDbl = GPInfo.GetPeakAcceleration(DataArray);
                        double x = GPInfo.GetX_AxisAccnPeak(DataArray);
                        double y = GPInfo.GetY_AxisAccnPeak(DataArray);
                        double z = GPInfo.GetZ_AxisAccnPeak(DataArray);

                        double array1[]={x,y,z};
                        b1.putDoubleArray("Aceleracion",array1);
                        text1.setData(b1);
                        _aNewHandler.sendMessage(text1);

                        //***************Displaying the Peak Acceleration******************************************
                        text1 = _aNewHandler.obtainMessage(PEAK_ACCLERATION);
                        b1.putString("PeakAcceleration", String.valueOf(PeakAccDbl));
                        text1.setData(b1);
                        _aNewHandler.sendMessage(text1);
                        //Log.d(TAG,"Peak Acceleration is "+ PeakAccDbl);

                        byte ROGStatus = GPInfo.GetROGStatus(DataArray);
                        Log.d(TAG,"ROG Status is "+ ROGStatus);

                        break;
                    case BREATHING_MSG_ID:
					/*Do what you want. Printing Sequence Number for now*/
                        Log.d(TAG,"Breathing Packet Sequence Number is "+BreathingInfoPacket.GetSeqNum(DataArray));
                        break;
                    case ECG_MSG_ID:
					/*Do what you want. Printing Sequence Number for now*/
                        Log.d(TAG,"ECG Packet Sequence Number is "+ECGInfoPacket.GetSeqNum(DataArray));
                        break;
                    case RtoR_MSG_ID:
					/*Do what you want. Printing Sequence Number for now*/
                        Log.d(TAG,"R to R Packet Sequence Number is "+RtoRInfoPacket.GetSeqNum(DataArray));
                        break;
                    case ACCEL_100mg_MSG_ID:
					/*Do what you want. Printing Sequence Number for now*/
                        Log.d(TAG,"Accelerometry Packet Sequence Number is "+AccInfoPacket.GetSeqNum(DataArray));
                        break;
                    case SUMMARY_MSG_ID:
					/*Do what you want. Printing Sequence Number for now*/
                        Log.d(TAG,"Summary Packet Sequence Number is "+SummaryInfoPacket.GetSeqNum(DataArray));
                        break;

                }
            }
        });
    }
}
