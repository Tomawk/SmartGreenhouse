package it.unipi.dii.inginf.iot.smartgreenhouse.mqtt;

import it.unipi.dii.inginf.iot.smartgreenhouse.mqtt.devices.Humidity;
import it.unipi.dii.inginf.iot.smartgreenhouse.mqtt.devices.Temperature;
import org.eclipse.paho.client.mqttv3.*;
import org.json.JSONObject;
import java.io.FileNotFoundException;
import java.io.PrintStream;

public class MQTTutility implements MqttCallback {

    private final String BROKER = "tcp://127.0.0.1:1883";
    private final String CLIENT_ID = "SmartGreenHouseCollector";
    private final int SECONDS_TO_WAIT_FOR_RECONNECTION = 5;
    private final int MAX_RECONNECTION_ITERATIONS = 10;


    // i want to redirect all the prints about the messages received and sent to the nodes to a file
    // in fact if i dont do this, the cmd will be full of messages and it will be difficult for an user to type commands

   // private PrintStream logStream = new PrintStream(new FileOutputStream("log.txt", true)); //log file APPEND
   private PrintStream logStream = new PrintStream("log.txt");

    private PrintStream console = System.out;

    private MqttClient mqttClient = null;
    private Humidity humiditySensor;
    private Temperature temperatureSensor;

    private int oldLowBoundTemperature = 0;
    private int oldHighBoundTemperature = 0;
    private int oldLowBoundHumidity = 0;
    private int oldHighBoundHumidity = 0;

    public MQTTutility() throws MqttException, FileNotFoundException {
        humiditySensor = new Humidity();
        oldHighBoundHumidity = humiditySensor.getHighBoundHumidity();
        oldLowBoundHumidity = humiditySensor.getLowBoundHumidity();
        temperatureSensor = new Temperature();
        oldHighBoundTemperature = temperatureSensor.getUpperBoundTemperature();
        oldLowBoundTemperature = temperatureSensor.getLowerBoundTemperature();
        do {
            try {
                mqttClient = new MqttClient(BROKER, CLIENT_ID);
                System.out.println("Connecting to the broker: " + BROKER + " . . . ");
                mqttClient.setCallback( this );
                connectToBroker();
            }
            catch(MqttException me)
            {
                System.out.println("I was not able to connect to the broker, Retrying ...");
            }
        }while(!mqttClient.isConnected());
    }

    /**
     * This function is used to try to connect to the broker
     */
    private void connectToBroker () throws MqttException {
        mqttClient.connect();
        mqttClient.subscribe(humiditySensor.HUMIDITY_TOPIC);
        System.out.println("Subscribed to: " + humiditySensor.HUMIDITY_TOPIC);
        mqttClient.subscribe(temperatureSensor.TEMPERATURE_TOPIC);
        System.out.println("Subscribed to: " + temperatureSensor.TEMPERATURE_TOPIC);
    }

    /**
     * Function used to publish a message
     * @param topic     topic of the message
     * @param message   message to send
     */
    public void publishMessage (final String topic, final String message)
    {
        try
        {
            mqttClient.publish(topic, new MqttMessage(message.getBytes()));
        }
        catch(MqttException e)
        {
            e.printStackTrace();
        }
    }

    @Override
    public void connectionLost(Throwable throwable) {
        System.out.println("Connection with the Broker lost!");
        // We have lost the connection, we have to try to reconnect after waiting some time
        // At each iteration we increase the time waited
        int iter = 0;
        do {
            iter++; // first iteration iter=1
            if (iter > MAX_RECONNECTION_ITERATIONS)
            {
                System.err.println("Reconnection with the broker not possible!");
                System.exit(-1);
            }
            try
            {
                Thread.sleep(SECONDS_TO_WAIT_FOR_RECONNECTION * 1000 * iter);
                System.out.println("New attempt to connect to the broker...");
                connectToBroker();
            }
            catch (MqttException | InterruptedException e)
            {
                e.printStackTrace();
            }
        } while (!this.mqttClient.isConnected());
        System.out.println("Connection with the Broker has been restored!");
    }

    @Override
    public void messageArrived(String topic, MqttMessage mqttMessage) throws Exception {

        System.setOut(logStream); //switch to log

        String payload = new String(mqttMessage.getPayload()); //json string
        if (topic.equals(humiditySensor.HUMIDITY_TOPIC)) //new message arrived for the humidity topic
        {

            if(oldHighBoundHumidity != humiditySensor.getHighBoundHumidity() || oldLowBoundHumidity != humiditySensor.getLowBoundHumidity()) {
                oldHighBoundHumidity = humiditySensor.getHighBoundHumidity();
                oldLowBoundHumidity = humiditySensor.getLowBoundHumidity();
                System.out.println("[HUMIDITY] New correct range updated to: [" + oldLowBoundHumidity + "%," + oldHighBoundHumidity + "%]");
            }

            //Extract data from the JSON Payload
            JSONObject obj = new JSONObject(payload);
            int node_id = obj.getInt("node_id");
            int humidity = obj.getInt("humidity");

            //Add data to the DB
            humiditySensor.addHumidityDB(node_id,humidity);

            if (humidity < humiditySensor.getLowBoundHumidity())
            {
                if (!humiditySensor.getLastCommand().equals("ON_HUM")) //i must turn on the humidifier if the percentage is under the threshold
                {
                    System.out.println("[HUMIDITY] Humidity percentage is too low ("+ humidity + "%), automatically turning on the Humidifier!");
                    System.out.println("[HUMIDITY] Sending ON_HUM message on the topic " + humiditySensor.HUMIDIFIER_TOPIC + " . . . ");
                    publishMessage(humiditySensor.HUMIDIFIER_TOPIC, "ON_HUM");
                    humiditySensor.setLastCommand("ON_HUM");
                }
                else
                    System.out.println("[HUMIDITY] Humidity percentage is low (" + humidity + "%), the humidifier is already ON so humidity percentage will increase!");
            }
            else if (humidity > humiditySensor.getHighBoundHumidity())
            {
                if (!humiditySensor.getLastCommand().equals("ON_DEH"))  //i must turn on the dehumidifier if the percentage is over the threshold
                {
                    System.out.println("[HUMIDITY] Humidity percentage is too high ("+ humidity + "%), automatically turning on the Dehumidifier!");
                    System.out.println("[HUMIDITY] Sending ON_DEH message on the topic " + humiditySensor.HUMIDIFIER_TOPIC + " . . . ");
                    publishMessage(humiditySensor.HUMIDIFIER_TOPIC,"ON_DEH");
                    humiditySensor.setLastCommand("ON_DEH");
                }
                else
                    System.out.println("[HUMIDITY] Humidity percentage is high (" + humidity + "%), the dehumidifier is already ON so humidity percentage will decrease!");
            }
            else //inside the threshold, turn off everything
            {
                if (!humiditySensor.getLastCommand().equals("OFF"))
                {
                    System.out.println("[HUMIDITY] Humidity percentage is in the correct range (" + humidity + "%), turning off humidifier/dehumidifier");
                    System.out.println("[HUMIDITY] Sending OFF message on the topic " + humiditySensor.HUMIDIFIER_TOPIC + " . . . ");
                    publishMessage(humiditySensor.HUMIDIFIER_TOPIC, "OFF");
                    humiditySensor.setLastCommand("OFF");
                }
                else
                {
                    System.out.println("[HUMIDITY] Humidity percentage is in the correct range: "+ humidity + "%");
                }
            }
        }
        else if (topic.equals(temperatureSensor.TEMPERATURE_TOPIC)) {

            if(oldLowBoundTemperature != temperatureSensor.getLowerBoundTemperature() || oldHighBoundTemperature != temperatureSensor.getUpperBoundTemperature()) {
                oldHighBoundTemperature = temperatureSensor.getUpperBoundTemperature();
                oldLowBoundTemperature = temperatureSensor.getLowerBoundTemperature();
                System.out.println("[TEMPERATURE] New correct range updated to: [" + oldLowBoundTemperature + "°C," + oldHighBoundTemperature + "°C]");
            }

            //Extract data from the JSON Payload
            JSONObject obj = new JSONObject(payload);
            int node_id = obj.getInt("node_id");
            int temperature = obj.getInt("temperature");

            //Add data to the DB
            temperatureSensor.addTemperatureDB(node_id, temperature);


            if (temperature < temperatureSensor.getLowerBoundTemperature()) //LOW TEMPERATURE
            {
                if (!temperatureSensor.getLastCommand().equals("ON_HEA")) //I must turn on the HEAter if the value is under the threshold
                {
                    System.out.println("[TEMPERATURE] Temperature is too low ("+ temperature + "°C), automatically turning on the Heater!");
                    System.out.println("[TEMPERATURE] Sending ON_HEA message on the topic " + temperatureSensor.AC_TOPIC + " . . . ");
                    publishMessage(temperatureSensor.AC_TOPIC, "ON_HEA");
                    temperatureSensor.setLastCommand("ON_HEA");
                } else
                    System.out.println("[TEMPERATURE] Temperature is low (" + temperature + "°C), the heater is already ON so temperature will increase!");
            } else if (temperature > temperatureSensor.getUpperBoundTemperature()) //high temperature
            {
                if (!temperatureSensor.getLastCommand().equals("ON_REF"))  //i must turn on the REFrigerator if the value is over the threshold
                {
                    System.out.println("[TEMPERATURE] Temperature is too high ("+temperature+"°C), automatically turning on the REFrigerator!");
                    System.out.println("[TEMPERATURE] Sending ON_REF message on the topic " + temperatureSensor.AC_TOPIC + " . . . ");
                    publishMessage(temperatureSensor.AC_TOPIC, "ON_REF");
                    temperatureSensor.setLastCommand("ON_REF");
                } else
                    System.out.println("[TEMPERATURE] Temperature is high ("+temperature+"°C), the heater is already ON so temperature value will decrease!");
            } else //inside the threshold, turn off everything
            {
                if (!temperatureSensor.getLastCommand().equals("OFF")) {
                    System.out.println("[TEMPERATURE] Temperature is in the correct range (" + temperature + "°C), turning off heater/refrigerator");
                    System.out.println("[TEMPERATURE] Sending OFF message on the topic " + temperatureSensor.AC_TOPIC + " . . . ");
                    publishMessage(temperatureSensor.AC_TOPIC, "OFF");
                    temperatureSensor.setLastCommand("OFF");
                } else {
                    System.out.println("[TEMPERATURE] Temperature is in the correct range: " + temperature + "°C");
                }
            }
        }
        System.setOut(console);
    }

    public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {
        System.setOut(logStream);
        System.out.println("[deliveryComplete] Message Delivered Correctly");
        System.setOut(console);
    }

    public Humidity getHumiditySensor() {
        return humiditySensor;
    }

    public Temperature getTemperatureSensor() {
        return temperatureSensor;
    }
}