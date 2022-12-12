package it.unipi.dii.inginf.iot.smartgreenhouse.coap.devices.soilmoisture;

import it.unipi.dii.inginf.iot.smartgreenhouse.config.ConfigurationParameters;
import it.unipi.dii.inginf.iot.smartgreenhouse.persistence.DBDriver;
import org.eclipse.californium.core.CoapClient;
import org.eclipse.californium.core.CoapHandler;
import org.eclipse.californium.core.CoapObserveRelation;
import org.eclipse.californium.core.CoapResponse;
import org.eclipse.californium.core.coap.MediaTypeRegistry;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class SoilMoisture {


    private List<CoapClient> clientSoilMoistureList = new ArrayList<>();
    private List<CoapClient> clientIrrigationList = new ArrayList<>();
    private List<CoapObserveRelation> observeMoistureList = new ArrayList<>();

    private boolean irrigation_on = false;
    private double moisture_value = 0;
    private double moisture_high_bound = 0;
    private double moisture_low_bound = 0;

    public SoilMoisture() {
        moisture_value = 1.5; //default value;
        ConfigurationParameters configurationParameters = ConfigurationParameters.getInstance();
        moisture_high_bound = configurationParameters.getHighBoundMoisture();
        moisture_low_bound = configurationParameters.getLowBoundMoisture();
    }

    public void registerSoilMoisture(String ip) {
        System.out.print("[REGISTRATION] The SoilMoisture module: [" + ip + "] is now registered");
        CoapClient newClientMoisture = new CoapClient("coap://[" + ip + "]/soilmoisture/moisture");
        clientSoilMoistureList.add(newClientMoisture);
        CoapClient newClientIrrigation = new CoapClient("coap://[" + ip + "]/soilmoisture/irrigation");
        clientIrrigationList.add(newClientIrrigation);


        CoapObserveRelation newObserveMoisture = newClientMoisture.observe(new CoapHandler() {
            @Override
            public void onLoad(CoapResponse coapResponse) {
                String responseString = new String(coapResponse.getPayload());
                try {
                    JSONObject obj = new JSONObject(responseString);
                    int node_id = obj.getInt("node_id");
                    moisture_value = obj.getDouble("moisture_value");
                    DBDriver.getInstance().insertSoilMoisture(node_id,(float)moisture_value);
                } catch (Exception e) {
                    System.out.print("\n[ERROR] The Moisture sensor gave non-significant data\n>");
                }

                if(irrigation_on && moisture_value > moisture_high_bound) { //turn off the irrigation automatically
                    for (CoapClient clientIrrigation: clientIrrigationList) {
                        switchIrrigation("OFF");
                    }
                    irrigation_on = false;
                }

                if(!irrigation_on && moisture_value < moisture_low_bound){ //turn on irrigation automatically
                    for (CoapClient clientIrrigation: clientIrrigationList) {
                        switchIrrigation("ON");
                    }
                    irrigation_on = true;
                }
            }@Override
            public void onError() {
                System.err.print("\n[ERROR] SoilMoisture Sensor " + newClientMoisture.getURI() + "]\n>");
            }
        });

        observeMoistureList.add(newObserveMoisture);
    }

    public void unregisterSoilMoisture(String ip) {
        for (int i = 0; i < clientSoilMoistureList.size(); i++) {
            if (clientSoilMoistureList.get(i).getURI().equals(ip)) {
                clientSoilMoistureList.remove(i);
                observeMoistureList.get(i).proactiveCancel();
                observeMoistureList.remove(i);
            }
        }
    }

    public double getSoilMoisture() {
        return moisture_value;
    }

    public void switchIrrigation(String switch_str) { //In base alla switch str spenge o accende l'irrigation system
        if(clientSoilMoistureList == null)
            return;
        String msg = "mode=" + switch_str;
        for(CoapClient clientIrrigationPut: clientIrrigationList) {
            clientIrrigationPut.put(new CoapHandler() {
                @Override
                public void onLoad(CoapResponse coapResponse) {
                    if (!coapResponse.isSuccess()) {
                        System.out.println("[ERROR] Irrigation System: PUT request unsuccessful");
                    } else {
                        if(switch_str.equals("ON")){
                            //System.out.println("Irrigation system successfully turned on");
                            irrigation_on = true;
                        } else if (switch_str.equals("OFF")) {
                            //System.out.println("Irrigation system successfully turned off");
                            irrigation_on = false;
                        }
                    }
                }

                @Override
                public void onError() {
                    System.err.print("[ERROR] Irrigation System" + clientIrrigationPut.getURI() + "]");
                }
            }, msg, MediaTypeRegistry.TEXT_PLAIN);
        }
    }
}
