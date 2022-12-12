package it.unipi.dii.inginf.iot.smartgreenhouse.mqtt.devices;


import it.unipi.dii.inginf.iot.smartgreenhouse.config.ConfigurationParameters;
import it.unipi.dii.inginf.iot.smartgreenhouse.persistence.DBDriver;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;

public class Temperature {
        public final String TEMPERATURE_TOPIC = "temperature";
        public final String AC_TOPIC = "actuator";

        private int lowerBoundTemperature;
        private int upperBoundTemperature;

        private int lastTemperature;
        private String lastCommand;

        public Temperature () throws FileNotFoundException {
            ConfigurationParameters configurationParameters = ConfigurationParameters.getInstance();
            lowerBoundTemperature = configurationParameters.getLowBoundTemperature();
            upperBoundTemperature = configurationParameters.getHighBoundTemperature();
            lastCommand = "OFF";
        }

        /**
         * Function that adds a new temperature sample into the DB
         */
        public void addTemperatureDB (int node_id, int temperature_value)
        {
            lastTemperature = temperature_value;
            DBDriver.getInstance().insertTemperature(node_id,temperature_value);
        }

        public int getLastTemperature() {
            return lastTemperature;
        }

        public int getLowerBoundTemperature() {
            return lowerBoundTemperature;
        }

        public void setLowerBoundTemperature(int lowerBoundTemperature) {
            this.lowerBoundTemperature = lowerBoundTemperature;
        }

        public int getUpperBoundTemperature() {
            return upperBoundTemperature;
        }

        public void setUpperBoundTemperature(int upperBoundTemperature) {
            this.upperBoundTemperature = upperBoundTemperature;
        }

        public String getLastCommand() {
            return lastCommand;
        }

        public void setLastCommand(String lastCommand) {
            this.lastCommand = lastCommand;
        }
}
