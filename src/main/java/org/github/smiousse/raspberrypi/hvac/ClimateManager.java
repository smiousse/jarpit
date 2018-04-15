package org.github.smiousse.raspberrypi.hvac;

import org.github.smiousse.raspberrypi.hvac.HvacControllerSetting.FanMode;

public class ClimateManager {

    public enum ClimateMode {
        HEAT, COOL, FAN, AUTO
    }

    private ClimateSetting climateSetting;

    private HvacController hvacController;
    private long currentTime = 0;

    /**
     * @param temperatureSetting
     */
    public ClimateManager(ClimateSetting climateSetting, HvacControllerSetting hvacControllerSetting) {
        super();
        this.climateSetting = climateSetting;
        this.init(hvacControllerSetting);
    }

    /**
     * @param hvacControllerSetting
     */
    private void init(HvacControllerSetting hvacControllerSetting) {
        hvacController = new HvacController(hvacControllerSetting);
    }

    /**
     * @param climateMode
     */
    public void checkClimate(ClimateMode climateMode) {

        // # This function is called if the compressor is in heat, cool or auto mode.
        // # First check the current temperature, set temperature, and threshold.

        // If the compressor is in the "stuck" period, just return.
        currentTime = System.currentTimeMillis();
        if (hvacController.isCompressorStuck(currentTime)) {
            // writeVerbose('Compressor currently stuck, so no change.');
            return;
        }

        if (climateSetting.getTemperatureMax() <= climateSetting.getTemperatureMin() || (climateSetting.getTemperatureMax()
                - climateSetting.getTemperatureMin()) < (climateSetting.getTemperatureThreshold() * 2)) {
            // writeVerbose('*** Error: Overlap between set minimum and maximum temperatures.');
            return;
        }

        // writeVerbose('Current temperature: '+str(observedTemperature)+' F');
        // writeVerbose('Set minimum temperature: '+str(setTemperatureMin)+' F');
        // writeVerbose('Set maximum temperature: '+str(setTemperatureMax)+' F');
        // writeVerbose('');

        // The A/C (and fan) should be enabled if the observed temperature is warmer than
        // the set temperature, plus the threshold

        boolean hotterThanMax = false;
        boolean coolerThanMin = false;
        //
        // Checking to see if it's warmer than the high range (ie. if the A/C should turn on)
        // If the A/C is on right now, it should stay on until it goes past the threshold
        if (hvacController.isCoolingOn() && (climateSetting
                .getTemperatureMax() < (climateSetting.getObservedTemperature() + climateSetting.getTemperatureThreshold()))) {
            hotterThanMax = true;
        }
        // //If the A/C is not on right now, it should turn on when it hits the threshold
        if (!hvacController.isCoolingOn() && (climateSetting
                .getTemperatureMax() < (climateSetting.getObservedTemperature() - climateSetting.getTemperatureThreshold()))) {
            hotterThanMax = true;
        }

        // Checking to see if it's colder than the low range (ie. if the heater should turn on)
        // If the heater is on right now, it should stay on until it goes past the threshold
        if (hvacController.isHeatingCompressorOn() && (climateSetting
                .getTemperatureMin() > (climateSetting.getObservedTemperature() - climateSetting.getTemperatureThreshold()))) {
            coolerThanMin = true;
        }

        // If the heater is not on right now, it should turn on when it hits the threshold
        if (!hvacController.isHeatingCompressorOn() && (climateSetting
                .getTemperatureMin() > (climateSetting.getObservedTemperature() + climateSetting.getTemperatureThreshold()))) {
            coolerThanMin = true;
        }

        if (hotterThanMax && coolerThanMin) {
            // writeVerbose('*** Error: Outside of both ranges somehow.');
            return;
        }

        if (!hotterThanMax && !coolerThanMin) {
            // writeVerbose('Temperature is in range, so no compressor necessary.');
            hvacController.setHeatingCompressor(false, false);
            hvacController.setCooling(false, false);
            if (FanMode.AUTO.equals(hvacController.getSetting().getFanMode())) {
                hvacController.setFan(false, false);
            }
        } else if (hotterThanMax && (ClimateMode.COOL.equals(climateMode) || ClimateMode.AUTO.equals(climateMode))) {
            // writeVerbose('Temperature is too warm and A/C is enabled, activating A/C.');
            hvacController.setFan(true, true);
            if (hvacController.isHeatingCompressorOn()) {
                hvacController.setHeatingCompressor(false, true);
            }
            if (hvacController.isHeatingElementOn()) {
                hvacController.setHeatingElement(false, true);
            }
            hvacController.setCooling(true, false);

        } else if (coolerThanMin && (ClimateMode.HEAT.equals(climateMode) || ClimateMode.AUTO.equals(climateMode))) {
            // writeVerbose('Temperature is too cold and heating is enabled, activating heater.');
            hvacController.setFan(true, true);
            if (hvacController.isCoolingOn()) {
                hvacController.setCooling(false, true);
            }
            hvacController.setHeatingCompressor(true, false);
        }
    }

}
