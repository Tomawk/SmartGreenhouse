package it.unipi.dii.inginf.iot.smartgreenhouse.coap.devices.soilph;

import it.unipi.dii.inginf.iot.smartgreenhouse.LightColor;
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

public class SoilPH {

    private List<CoapClient> clientPHSensorList = new ArrayList<>();
    private List<CoapClient> clientFertilizerList = new ArrayList<>();
    private List<CoapObserveRelation> observePHSensorList = new ArrayList<>();

    private float ph_value = 0; //between 0 and 14
    private int fertilizer_quantity = 0;
    private boolean fertilizer_on = false;
    private double ph_high_bound;
    private double ph_low_bound;

    public SoilPH() {
        ph_value = 8;
        ConfigurationParameters configurationParameters = ConfigurationParameters.getInstance();
        ph_high_bound = configurationParameters.getHighBoundSoilPH();
        ph_low_bound = configurationParameters.getLowBoundSoilPH();
    }

    public void registerSoilPH(String ip) {
        System.out.print("[REGISTRATION] The soilPH module: [" + ip + "] is now registered");
        CoapClient newClientSensorPH = new CoapClient("coap://[" + ip + "]/soilph/sensorph");
        clientPHSensorList.add(newClientSensorPH);
        CoapClient newClientFertilizer = new CoapClient("coap://[" + ip + "]/soilph/fertilizer");
        clientFertilizerList.add(newClientFertilizer);


        CoapObserveRelation newObserveSoilPH = newClientSensorPH.observe(new CoapHandler() {
            @Override
            public void onLoad(CoapResponse coapResponse) {
                String responseString = new String(coapResponse.getPayload());
                try {
                    JSONObject obj = new JSONObject(responseString);
                    int node_id = obj.getInt("node_id");
                    ph_value = (float) obj.getDouble("ph_value");
                    DBDriver.getInstance().insertPHValue(node_id,ph_value);
                } catch (Exception e) {
                    System.out.print("\n[ERROR] The SoilPH sensor gave non-significant data\n>");
                }

                if(fertilizer_on && ph_value>8 && ph_value<9) { //ph_value restablished so i must turn the fertilizer OFF
                    for (CoapClient clienFertilizer: clientFertilizerList) {
                        setFertilizerQuantity(0); //quantity set to 0 to turn off the automatic fertilizer
                    }
                    //System.out.println("Automatic Fertilizer automatically turned off");
                    fertilizer_on = false;
                }

                if(!fertilizer_on){
                    if(ph_value>ph_high_bound || ph_value<ph_low_bound){
                        for (CoapClient clienFertilizer: clientFertilizerList) {
                            setFertilizerQuantity(2); //quantity set to 2 for medium ph restablishment
                        }
                        //System.out.println("Automatic Fertilizer automatically turned on");
                        fertilizer_on = true;
                    }
                }
                }@Override
            public void onError() {
                System.err.print("\n[ERROR] SensorPH " + newClientSensorPH.getURI() + "]\n>");
            }
        });

        observePHSensorList.add(newObserveSoilPH);
    }

    public void unregisterSoilPH(String ip) {
        for (int i = 0; i < clientPHSensorList.size(); i++) {
            if (clientPHSensorList.get(i).getURI().equals(ip)) {
                clientPHSensorList.remove(i);
                clientFertilizerList.remove(i);
                observePHSensorList.get(i).proactiveCancel();
                observePHSensorList.remove(i);
            }
        }
    }

    public double getSoilPH() { //TODO RICONTROLLARE, dovrebbe essere una GET request?
        return ph_value;
    }

    public int getFertilizerQuantity(){
        return fertilizer_quantity;
    }

    public void setFertilizerQuantity(int quantity) { //in base alla quantità di fertilizzante rilasciato il ph tenderà a ristabilizzarsi piu o meno velocemente
                                                                            // Se la quantity viene posta a 0 il fertilizer viene spento
        if(clientFertilizerList == null)
            return;

        String msg = "quantity=" + quantity;
        for(CoapClient clientFertilizer: clientFertilizerList) {
            clientFertilizer.put(new CoapHandler() {
                @Override
                public void onLoad(CoapResponse coapResponse) {
                    if (!coapResponse.isSuccess())
                        System.out.print("[ERROR] Automatic Fertilizer: PUT request unsuccessful");
                    fertilizer_quantity = quantity;
                    if(fertilizer_quantity != 0) {
                        //System.out.println("Automatic Fertilizer successfully turned on");
                        fertilizer_on = true;
                    } else { // = 0
                        //System.out.println("Automatic Fertilizer successfully turned off");
                        fertilizer_on = false;
                    }
                }

                @Override
                public void onError() {
                    System.err.print("[ERROR] Automatic Fertilizer" + clientFertilizer.getURI() + "]");
                }
            }, msg, MediaTypeRegistry.TEXT_PLAIN);
        }
    }


}
