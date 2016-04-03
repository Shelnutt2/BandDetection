package nu.shel.banddetection;

import android.util.Log;

import com.stericson.RootShell.RootShell;
import com.stericson.RootShell.exceptions.RootDeniedException;
import com.stericson.RootShell.execution.Command;
import com.stericson.RootTools.RootTools;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeoutException;

/**
 *  Modem
 *
 * v1.0
 *
 * 2016-03-31
 *
 * This file is copyrighted By Seth Shelnutt and licensed under terms of the LGPL v2.1
 *
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
        NO_SERIAL_DEVICE, //No serial device found
        COMMAND_FAILED,
        COMMAND_SUCCESS,

    }

    private long defaultTimeout = 5000;

    public String path; //Path where modem block device is located

    public returnCodes lastReturnStatus; //Last Error recorded

    public Modem() {
        findSerialDevice();
    }

    /** Run a given command on the modem serial device, will retry on no-response
     *
     * @param command String of command to run
     * @return ArrayList of lines returned.
     */
    public ArrayList<String> RunModemCommand(final String command) {
        return RunModemCommand(command, true, defaultTimeout);
    }

    /** Run a given command on the modem serial device
     *
     * @param command String of command to run
     * @param retry Retry command on no response
     * @param timeout When should given command timeout without complete response
     * @return Arraylist of lines returned.
     */
    public ArrayList<String> RunModemCommand(final String command, final boolean retry, final long timeout) {
        if(this.path == null || this.path.isEmpty()) {
            Log.e(TAG, "RunModemCommand: Trying to run command without path! Aborting");
            return new ArrayList<>();
        }

        // Reset last return to signal command is running
        lastReturnStatus = null;

        final ArrayList<String> output = new ArrayList<>();
        RootTools.debugMode=true;

        // Run in a thread so we can sleep while we wait for possible ls command to finish
        new Thread(new Runnable() {
            public void run() {

                // Check for root access
                boolean root = RootShell.isAccessGiven();
                if(!root) {
                    lastReturnStatus = returnCodes.ROOT_UNAVAILABLE;
                }

                try {
                    // First we setup a listener
                    Log.d(TAG, "Listening for modem command output");
                    Log.d(TAG, "Running: " + "cat " + path);
                    Command listen = new Command(10, "cat " + path) {
                        @Override
                        public void commandOutput(int id, String line) {
                            Log.d(TAG, "commandOutput: " + line);
                            if(output.isEmpty() || (!output.get(output.size() -1 ).equals("OK") && !output.get(output.size() -1 ).equals("ERROR")))
                                output.add(line);
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
                    RootTools.getShell(true).add(listen);

                    if(listen.isFinished()){
                        Log.e(TAG, "RunModemCommand: Could not listen to serial device, " + path);
                    } else {
                        // Next we run the command
                        String runString = "echo -e \"" + command + "\\r\" >" + path;
                        Log.d(TAG, "Running: " + runString);
                        Command run = new Command(0, runString);
                        RootTools.getShell(true).add(run);

                        // Wait for response from modem, look for OK/ERROR or timeout
                        long currentTime = System.currentTimeMillis();
                        while ((output.isEmpty() || (!output.get(output.size() - 1).equals("OK") && !output.get(output.size() - 1).equals("ERROR"))) && System.currentTimeMillis() - currentTime < timeout) {
                            if(listen.isFinished()) {
                                Log.e(TAG, "RunModemCommand: Could not listen to serial device");
                                break;
                            }
                            try {
                                Thread.sleep(500);
                                if (retry) { //If retry is enable, run command again
                                    Log.d(TAG, "No response, rerunning command..: " + runString);
                                    run.terminate();
                                    RootTools.getShell(true).add(run);
                                    while (!run.isFinished()) {
                                        try {
                                            // Sleep for 500 milliseconds while we wait for command to finish
                                            Thread.sleep(500);
                                        } catch (InterruptedException e) {
                                            Log.e(TAG, "Unable to wait for " + runString + " to finish. " + e);
                                        }
                                    }
                                }
                            } catch (InterruptedException e) {
                                Log.e(TAG, "Unable to wait for listener to get results. " + e);
                            }
                        }
                        listen.terminate();
                    }
                } catch (RootDeniedException e) {
                    Log.e(TAG, "RunModemCommand: " + e.toString());
                } catch (TimeoutException | IOException e) {
                    Log.e(TAG, "RunModemCommand: " + command + " " + e.toString());
                }
            }

        }).run();

        if(!output.isEmpty()) {
            //The first line should equal the command run
            if (output.get(0).equals(command)) {
                Log.d(TAG, "RunModemCommand: Removing first line. " + output.get(0));
                output.remove(0);
            }
            if (output.get(0).contains(":")){
                String line = output.get(0);
                Log.d(TAG, "RunModemCommand: " + line.substring(line.indexOf(": ")));
                output.set(0, line.substring(line.indexOf(":")));
            }

            //The last line should say OK to indicate command run successfully
            if (output.get(output.size() - 1).equals("OK")) {
                output.remove(output.size() - 1);
                lastReturnStatus = returnCodes.COMMAND_SUCCESS;
            } else {
                Log.d(TAG, "RunModemCommand: Output did not end in OK");
                lastReturnStatus = returnCodes.COMMAND_FAILED;
            }
        } else {
            Log.d(TAG, "RunModemCommand: Output is empty");
            lastReturnStatus = returnCodes.COMMAND_FAILED;
        }
        return output;
    }

    /**
     * This tests a serial device to see if it correctly responds to AT commands
     *
     * @param path Serial device to test for modem access
     * @return Boolean for if modem is usable
     */
    private boolean testSerialDevice(String path){
        Log.d(TAG, "testSerialDevice: Testing " + path);
        String previousPath = this.path; // Save previous path
        this.path = path; // Set path for running test commands
        ArrayList<String> output = RunModemCommand("AT"); // AT should should an empty set
        this.path = previousPath; // Set path back to previous path
        return !output.isEmpty() && output.get(0).equals("");
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
                         Class clazz;
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
                     } else {
                         path = mSerialDevices.get(0);
                         lastReturnStatus = returnCodes.SERIAL_INIT_OK;
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
