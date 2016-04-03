package nu.shel.banddetection;

import android.support.test.runner.AndroidJUnit4;
import android.test.suitebuilder.annotation.SmallTest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

/**
 * Created by seth on 4/2/16.
 */

@RunWith(AndroidJUnit4.class)
@SmallTest
public class ModemDeviceTest {

    private Modem mModem;

    @Before
    public void createLogHistory() {
        mModem = new Modem();
    }

    @Test
    public void detectionOfModem() {
        while(mModem.lastReturnStatus == null){

        }
        assertThat(mModem.lastReturnStatus, is(Modem.returnCodes.SERIAL_INIT_OK));
        assertThat(mModem.path, is("/dev/smd0"));
    }

    @Test
    public void runCommand() {

        while(mModem.lastReturnStatus == null){

        }
        assertThat(mModem.lastReturnStatus, is(Modem.returnCodes.SERIAL_INIT_OK));
        ArrayList<String> output = mModem.RunModemCommand("AT");
        assertThat(mModem.lastReturnStatus, is(Modem.returnCodes.COMMAND_SUCCESS));
        assertThat(output.isEmpty(),is(false));
        assertThat(output.get(0),is(""));
    }

}