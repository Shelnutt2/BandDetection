package nu.shel.banddetection;

import android.support.test.runner.AndroidJUnit4;
import android.test.suitebuilder.annotation.SmallTest;

import org.junit.Test;
import org.junit.runner.RunWith;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

/**
 *  BandDetectionTest
 *
 * v1.0
 *
 * 2016-04-03
 *
 * This file is copyrighted By Seth Shelnutt and licensed under terms of the LGPL v2.1
 *
 * @author Seth Shelnutt
 * @since 03/04/2016
 */

@RunWith(AndroidJUnit4.class)
@SmallTest
public class BandDetectionTest {

    @Test
    public void detectionOfBand() {
        LTEBand currentBand = BandDetection.DetectBand();
        assertThat(currentBand.band,is(41));
    }

}