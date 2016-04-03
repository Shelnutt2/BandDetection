package nu.shel.banddetection;

/**
 * Created by seth on 4/3/16.
 */

import android.util.Log;

import java.util.ArrayList;

import static nu.shel.banddetection.LTEBand.GetBandFromEarfcn;

/**
 * BandDetection
 * Created by Seth Shelnutt on 04/03/16.
 * @author Seth Shelnutt
 * @since 4/03/2016
 * This file is copyrighted By Seth Shelnutt and licensed under terms of the LGPL v2.
 */
public class BandDetection {

    private static String TAG = "BandDetection";
    public static LTEBand DetectBand(){
        Modem mModem = new Modem();
        while(mModem.lastReturnStatus == null){

        }
        ArrayList<String> output = mModem.RunModemCommand("AT\\$QCRSRP?");
        if(mModem.lastReturnStatus == Modem.returnCodes.COMMAND_SUCCESS){
            if(!output.isEmpty()){
                Log.d(TAG, "DetectBand: " + output.get(0));
                String[] splitString = output.get(0).split(",");
                if(splitString.length > 0) {
                    String earfcn = splitString[1];
                    return GetBandFromEarfcn(Double.parseDouble(earfcn));
                }
            }
        }
        return LTEBand.bands.get(0);
    }
}
