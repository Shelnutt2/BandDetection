package nu.shel.banddetection;

import android.util.Log;

import com.stericson.RootShell.RootShell;
import com.stericson.RootShell.execution.Command;
import com.stericson.RootTools.RootTools;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by seth on 3/31/16.
 * @author Seth Shelnutt
 */
public class Modem {

    private String TAG = "Modem";

    /**
     * Return Codes for modem class
     */
    enum returnCodes {
        ROOT_UNAVAILABLE, //Roottools can not find root
        SERIAL_INIT_OK, //Serial device detected fine
        NO_SERIAL_DEVICE //No serial device found

    }

    public String path; //Path where modem block device is located

    public returnCodes lastReturnStatus; //Last Error recorded

    public Modem() {
        findSerialDevice();
    }

    /**
     * This tests a serial device to see if it correctly responds to AT commands
     *
     * @param path Serial device to test for modem access
     * @return Boolean for if modem is usable
     */
    //TODO: This needs to be implemented
    private boolean testSerialDevice(String path){
        return path.equals("/dev/smd0");
    }

    /**
     * Finds possible serial devices. This will set the lastReturnStatus and Path (upon success)
     *
     */
     void findSerialDevice() {

         // Reset last return to signal command is running
         lastReturnStatus = null;

         // Run in a thread so we can sleep while we wait for possible ls command to finish
         new Thread(new Runnable() {
             public void run() {

                 // Check for root access
                 boolean root = RootShell.isAccessGiven();
                 if(!root) {
                     lastReturnStatus = returnCodes.ROOT_UNAVAILABLE;
                 }

                 try {
                     final List<String> mSerialDevices = new ArrayList<>();

                     try {
                         // Use SystemProperties class to get rild.libargs setting
                         Class clazz = null;
                         clazz = Class.forName("android.os.SystemProperties");
                         Method method = clazz.getDeclaredMethod("get", String.class);
                         String device = (String) method.invoke(null, "rild.libargs");

                         if (!"UNKNOWN".equals(device)) {
                             Log.d(TAG, "rild.libargs is: " + device.substring(3));
                             mSerialDevices.add(device.substring(3));
                         }
                     } catch (StringIndexOutOfBoundsException e) {
                         Log.w(TAG, e.getMessage());
                         // ignore, move on
                     }

                     // Only look in dev if we did not detect rild device or that devices is not valid
                     if (mSerialDevices.size() == 0 || !testSerialDevice(mSerialDevices.get(0))) {
                         Log.d(TAG, "looking in /dev");
                         Command listDev = new Command(0, "ls /dev/") {
                             @Override
                             public void commandOutput(int id, String line) {
                                 for (String device : line.split("\n")) {
                                     if (device.matches("^smd\\d+")) {
                                         mSerialDevices.add("/dev/" + device);
                                         Log.d(TAG, "Found modem interface: " + "/dev/" + device);
                                     }
                                 }
                                 super.commandOutput(id, line);
                             }

                             @Override
                             public void commandTerminated(int id, String reason) {
                                 super.commandTerminated(id, reason);
                             }

                             @Override
                             public void commandCompleted(int id, int exitcode) {
                                 super.commandCompleted(id, exitcode);
                             }
                         };
                         RootTools.getShell(true).add(listDev);
                         while (!listDev.isFinished()) {
                             try {
                                 // Sleep for 50 milliseconds while we wait for command to finish
                                 Thread.sleep(50);
                             } catch (InterruptedException e) {
                                 Log.e(TAG, "Unable to wait for " + listDev + " to finish. " + e);
                             }

                         }
                     }

                     // Test all possible serial devices
                     for (String mDevice : mSerialDevices) {
                         Log.d(TAG, "findSerialDevice: Possible device - " + mDevice);
                         if(testSerialDevice(mDevice)) {
                             path = mDevice;
                             lastReturnStatus = returnCodes.SERIAL_INIT_OK;
                             break;
                         } else {
                             lastReturnStatus = returnCodes.NO_SERIAL_DEVICE;
                         }
                     }
                 } catch( Exception e ) {
                     Log.e("InitSerialDevice ", e.toString());
                 }

                 // Set return status
                 if(lastReturnStatus != returnCodes.SERIAL_INIT_OK)
                    lastReturnStatus = returnCodes.NO_SERIAL_DEVICE;
             }

         }).run();
    }
}
