package org.github.smiousse.jarpit.api.sensors;

import java.math.BigDecimal;

import org.github.smiousse.jarpit.model.SensorSetting.TempSensorModel;

public interface TempSensor {

    /**
     * @return
     */
    public BigDecimal getTemperature();

    /**
     * @return
     */
    public String getInfo();

    /**
     * @return
     */
    public TempSensorModel getTempSensorModel();

    /**
     * @return
     */
    public boolean updateReadings();
}