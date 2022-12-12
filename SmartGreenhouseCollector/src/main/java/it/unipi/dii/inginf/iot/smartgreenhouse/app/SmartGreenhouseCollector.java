package it.unipi.dii.inginf.iot.smartgreenhouse.app;

import it.unipi.dii.inginf.iot.smartgreenhouse.LightColor;
import it.unipi.dii.inginf.iot.smartgreenhouse.coap.CoapRegistrationServer;
import it.unipi.dii.inginf.iot.smartgreenhouse.mqtt.MQTTutility;
import it.unipi.dii.inginf.iot.smartgreenhouse.mqtt.devices.Humidity;
import it.unipi.dii.inginf.iot.smartgreenhouse.mqtt.devices.Temperature;
import it.unipi.dii.inginf.iot.smartgreenhouse.persistence.DBDriver;
import org.eclipse.paho.client.mqttv3.MqttException;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.SocketException;

public class SmartGreenhouseCollector {

    public static void main(String[] args) throws SocketException, MqttException, FileNotFoundException {
        CoapRegistrationServer coapRegistrationServer = new CoapRegistrationServer();
        coapRegistrationServer.start();

        MQTTutility mqttUtility = new MQTTutility();

        printAvailableCommands();

        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(System.in));
        String command;
        String[] parts;

        while (true) {
            System.out.print("> ");
            try {
                command = bufferedReader.readLine();
                parts = command.split(" ");

                switch (parts[0]) {
                    case "!help":
                        helpFunction();
                        break;
                    case "!get_light_intensity":
                        getLightIntensity(coapRegistrationServer);
                        break;
                    case "!set_light_intensity":
                        setLightIntensity(coapRegistrationServer, parts);
                        break;
                    case "!get_soil_ph":
                        getSoilPH(coapRegistrationServer);
                        break;
                    case "!set_fertilizer_quantity":
                        setFertilizerQuantity(coapRegistrationServer, parts);
                        break;
                    case "!get_fertilizer_status":
                        getFertilizerQuantity(coapRegistrationServer);
                        break;
                    case "!switch_irrigation":
                        switchIrrigationSystem(coapRegistrationServer,parts);
                        break;
                    case "!get_soilmoisture":
                        getSoilMoisture(coapRegistrationServer);
                        break;
                    case "!get_humidity":
                        getHumidity(mqttUtility.getHumiditySensor());
                        break;
                    case "!set_humidity":
                        setHumidityThreshold(parts,mqttUtility.getHumiditySensor());
                        break;
                    case "!get_temperature":
                        getTemperature(mqttUtility.getTemperatureSensor());
                        break;
                    case "!set_temperature":
                        setTemperatureTreshold(parts,mqttUtility.getTemperatureSensor());
                        break;
                    case "!print_table":
                        printTable(parts);
                        break;
                    case "!exit":
                        System.out.println("See you soon, bye!");
                        System.exit(0);
                        break;
                    default:
                        System.out.println("Command not recognized, try again\n");
                        break;
                }
                printAvailableCommands();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static void printTable(String[] parts){
        if(parts.length != 3) {
            System.out.println("Incorrect use of the command. Please use !print_table <table_name>\n");
        } else {
            DBDriver.getInstance().printTable(parts[1], parts[2]);
        }
    }

    private static void printAvailableCommands() {
        System.out.println("***************************** SMART GREENHOUSE *****************************\n" +
                "The following commands are available:\n" +
                "1) !help <command> --> shows the details of a command\n" +
                "2) !get_light_intensity--> returns if the lights are turned on or off and also the intensity\n" +
                "3) !set_light_intensity <0,1 or 2>  --> 0 to turn off lights, 1 for yellow leds, 2 for green leds \n" +
                "4) !set_fertilizer_quantity <0,1,2 or 3>  --> 0 to turn off the fertilizer manually; 1,2 and 3 to turn on the fertilizer manually with that quantity \n" +
                "5) !get_soil_ph y--> returns the ph of the soil in the greenhouse \n" +
                "6) !get_fertilizer_status--> returns if the automatic fertilizer is turned on or off and also the quantity provided\n" +
                "7) !switch_irrigation <ON, OFF> --> switch on or off the irrigation system\n" +
                "8) !get_soilmoisture --> returns the soil moisture value in the greenhouse\n" +
                "9) !get_humidity --> returns the humidity percentage in the greenhouse\n" +
                "10) !set_humidity <low bound> <high bound>  --> change the humidity threshold\n" +
                "11) !get_temperature --> returns the temperature value in the greenhouse\n" +
                "12) !set_temperature <low bound> <high bound>  --> change the temperature threshold\n" +
                "13) !print_table <table_name> <max_rows>  --> print <max_rows> rows from the specified table from the db\n" +
                "14) !exit --> exit the program\n"
        );
    }

    private static void helpFunction() {
        System.out.println("Help function not already defined, contact the administrators\n");
    }




    private static void setLightIntensity(CoapRegistrationServer coapRegistrationServer, String[] parts) {
        if(parts.length != 2) {
            System.out.println("Incorrect use of the command. Please use !set_light_intensity <0,1 or 2>\n");
        } else {
            switch(parts[1]) {
                case "0":
                    coapRegistrationServer.setLightIntensity(LightColor.RED);
                    System.out.println("Lights successfully turned off\n");
                    break;
                case "1":
                    coapRegistrationServer.setLightIntensity(LightColor.YELLOW);
                    System.out.println("Light are turned ON with intensity=LOW\n");
                    break;
                case "2":
                    coapRegistrationServer.setLightIntensity(LightColor.GREEN);
                    System.out.println("Light are turned ON with intensity=HIGH\n");
                    break;
                default:
                    System.out.println("Invalid intensity, please use 0,1 or 2 only\n");
                    break;
            }
        }
    }

    private static void setFertilizerQuantity(CoapRegistrationServer coapRegistrationServer, String[] parts){
        if(parts.length != 2) {
            System.out.println("Incorrect use of the command. Please use !set_fertilizer_quantity <0,1,2 or 3>\n");
        } else {
            switch(parts[1]) {
                case "0":
                    coapRegistrationServer.setFertilizerQuantity(0);
                    System.out.println("Automatic Fertilizer has been successfully turned off\n");
                    break;
                case "1":
                    coapRegistrationServer.setFertilizerQuantity(1);
                    System.out.println("Automatic Fertilizer has been successfully turned on with quantity=1\n");
                    break;
                case "2":
                    coapRegistrationServer.setFertilizerQuantity(2);
                    System.out.println("Automatic Fertilizer has been successfully turned on with quantity=2\n");
                    break;
                case "3":
                    coapRegistrationServer.setFertilizerQuantity(3);
                    System.out.println("Automatic Fertilizer has been successfully turned on with quantity=3\n");
                    break;
                default:
                    System.out.println("Invalid quantity, please use 0,1,2 or 3 only\n");
                    break;
            }
        }
    }

    private static void switchIrrigationSystem(CoapRegistrationServer coapRegistrationServer, String[] parts) {
        if (parts.length != 2) {
            System.out.println("Incorrect use of the command. Please use !switch_irrigation <ON , OFF>\n");
        } else {
            switch (parts[1]) {
                case "ON":
                    coapRegistrationServer.switchIrrigation("ON");
                    break;
                case "OFF":
                    coapRegistrationServer.switchIrrigation("OFF");
                    break;
                default:
                    System.out.println("Invalid mode, please use <ON , OFF> only\n");
                    break;
            }
        }
    }

    private static void setHumidityThreshold(String[] parts, Humidity humiditySensor) {
        if(parts.length != 3) {
            System.out.println("Incorrect use of the command. Please use !set_humidity <low bound> <high bound>\n");
        } else {
            int lowBound;
            int highBound;
            try {
                lowBound = Integer.parseInt(parts[1]);
                highBound = Integer.parseInt(parts[2]);
                if (highBound < lowBound) {
                    System.out.println("ERROR: The high bound must be larger than the low bound\n");
                    return;
                }
                humiditySensor.setLowBoundHumidity(lowBound);
                humiditySensor.setHighBoundHumidity(highBound);
                System.out.println("Humidity range set correctly: [" + lowBound + "% - " + highBound + "%]\n");
            } catch(Exception e) {
                System.out.println("Please enter integer values\n");
            }
        }
    }

    private static void setTemperatureTreshold(String[] parts, Temperature temperatureSensor) {
        if(parts.length != 3) {
            System.out.println("Incorrect use of the command. Please use !set_temperature <low bound> <high bound>\n");
        } else {
            int lowBound;
            int highBound;
            try {
                lowBound = Integer.parseInt(parts[1]);
                highBound = Integer.parseInt(parts[2]);
                if (highBound < lowBound) {
                    System.out.println("ERROR: The high bound must be larger than the low bound\n");
                    return;
                }
                temperatureSensor.setLowerBoundTemperature(lowBound);
                temperatureSensor.setUpperBoundTemperature(highBound);
                System.out.println("Temperature range set correctly: [" + lowBound + "°C - " + highBound + "°C]\n");
            } catch(Exception e) {
                System.out.println("Please enter integer values\n");
            }
        }
    }


    /* GET FUNCTIONS */

    private static void getLightIntensity(CoapRegistrationServer coapRegistrationServer) {
        int intensity = coapRegistrationServer.getLightIntensity();
        switch(intensity){
            case 0:
                System.out.println("The lights in the GreenHouse are OFF");
                break;
            case 1:
                System.out.println("The lights in the GreenHouse are ON with intensity=LOW");
                break;
            case 2:
                System.out.println("The lights in the GreenHouse are ON with intensity=HIGH");
                break;

        }
    }

    public static void getFertilizerQuantity(CoapRegistrationServer coapRegistrationServer){
        int quantity = coapRegistrationServer.getFertilizerQuantity();
        switch(quantity){
            case 0:
                System.out.println("The Automatic Fertilizer in the GreenHouse is OFF");
                break;
            case 1:
                System.out.println("The Automatic Fertilizer in the GreenHouse is ON with quantity=1");
                break;
            case 2:
                System.out.println("The Automatic Fertilizer in the GreenHouse is ON with quantity=2");
                break;
            case 3:
                System.out.println("The Automatic Fertilizer in the GreenHouse is ON with quantity=3");
                break;
        }
    }

    public static void getSoilPH(CoapRegistrationServer coapRegistrationServer) {
        double soilPH = coapRegistrationServer.getSoilPH();
        System.out.println("The Soil PH inside the Greenhouse is set to " + soilPH+"\n");
    }

    public static void getSoilMoisture(CoapRegistrationServer coapRegistrationServer){
        double soilMoisture = coapRegistrationServer.getSoilMoisture();
        System.out.println("The Soil Moisture inside the Greenhouse is set to " + soilMoisture+"\n");
    }

    private static void getHumidity(Humidity humiditySensor) {
        int humidity = humiditySensor.getLastHumidity();
        System.out.println("The humidity percentage in the greenhouse is " + humidity + "%\n");
    }

    private static void getTemperature(Temperature temperatureSensor) {
        int temperature = temperatureSensor.getLastTemperature();
        System.out.println("The temperature in the greenhouse is " + temperature + "°C\n");
    }

}
