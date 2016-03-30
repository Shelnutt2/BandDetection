package nu.shel.banddetection;

import junit.framework.TestCase;

import org.junit.Test;

import static nu.shel.banddetection.LTEBand.GetBandFromEarfcn;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class LTEBandTest extends TestCase{

    @Test
    public void GetBandFromEarfcn_returnsBand41() {
        assertThat(GetBandFromEarfcn(39926).band, is(41));
    }

    @Test
    public void GetBandFromEarfcn_InvalidBandLow_returnsBand0() {
        assertThat(GetBandFromEarfcn(-1).band, is(41));
    }

    @Test
    public void GetBandFromEarfcn_InvalidBandHigh_returnsBand0() {
        assertThat(GetBandFromEarfcn(55000).band, is(0));
    }

    @Test
    public void GetBandFromEarfcn_returnsBand25() {
        assertThat(GetBandFromEarfcn(8665).band, is(25));
    }
}