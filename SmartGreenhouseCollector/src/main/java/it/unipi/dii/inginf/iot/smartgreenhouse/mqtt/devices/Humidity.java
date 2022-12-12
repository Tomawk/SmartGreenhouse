package it.unipi.dii.inginf.iot.smartgreenhouse.mqtt.devices;

import it.unipi.dii.inginf.iot.smartgreenhouse.config.ConfigurationParameters;
import it.unipi.dii.inginf.iot.smartgreenhouse.persistence.DBDriver;

import java.sql.Timestamp;
import java.util.HashMap;

public class Humidity {
    public final String HUMIDITY_TOPIC = "humidity_s";
    public final String HUMIDIFIER_TOPIC = "humidifier_act";

    private int lowBoundHumidity;
    private int highBoundHumidity;
    private String lastCommand;
    private int lastHumidity;

    public Humidity() {
        ConfigurationParameters configurationParameters = ConfigurationParameters.getInstance();
        lowBoundHumidity = configurationParameters.getLowBoundHumidity();
        highBoundHumidity = configurationParameters.getHighBoundHumidity();
        lastHumidity = 50;
        lastCommand = "OFF";
    }

    /**
     * Function that adds a new humidity record to db
     */
    public void addHumidityDB (int node_id, int humidity_percentage)
    {
        lastHumidity = humidity_percentage;
        DBDriver.getInstance().insertHumidity(node_id,humidity_percentage);
    }

    public int getLowBoundHumidity() {
        return lowBoundHumidity;
    }

    public int getHighBoundHumidity() {
        return highBoundHumidity;
    }

    public String getLastCommand() {
        return lastCommand;
    }

    public int getLastHumidity() {
        return lastHumidity;
    }

    public void setLowBoundHumidity(int lowBoundHumidity) {
        this.lowBoundHumidity = lowBoundHumidity;
    }

    public void setHighBoundHumidity(int highBoundHumidity) {
        this.highBoundHumidity = highBoundHumidity;
    }

    public void setLastCommand(String lastCommand) {
        this.lastCommand = lastCommand;
    }
}