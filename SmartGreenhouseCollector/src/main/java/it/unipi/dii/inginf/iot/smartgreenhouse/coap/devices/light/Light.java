package it.unipi.dii.inginf.iot.smartgreenhouse.coap.devices.light;

import it.unipi.dii.inginf.iot.smartgreenhouse.LightColor;
import org.eclipse.californium.core.CoapClient;
import org.eclipse.californium.core.CoapHandler;
import org.eclipse.californium.core.CoapResponse;
import org.eclipse.californium.core.coap.MediaTypeRegistry;

import java.util.ArrayList;
import java.util.List;

public class Light {
    private List<CoapClient> clientLightList = new ArrayList<>();
    private int light_intensity = 0;

    public void registerLight(String ip) {
        System.out.print("[REGISTRATION] The light: [" + ip + "] is now registered");
        CoapClient newClientLight = new CoapClient("coap://[" + ip + "]/light");
        clientLightList.add(newClientLight);
    }

    public void unregisterLight(String ip) {
        for(int i = 0; i< clientLightList.size(); i++) {
            if(clientLightList.get(i).getURI().equals(ip)) {
                clientLightList.remove(i);
            }
        }
    }

    public int getLightIntensity() {
        return light_intensity;
    }

    public void changeLightIntensity(LightColor color) {
        if(clientLightList == null)
            return;

        switch (color.name()){
            case "RED":
                light_intensity = 0;
                break;
            case "YELLOW":
                light_intensity = 1;
                break;
            case "GREEN":
                light_intensity = 2;
                break;
        }

        String msg = "intensity=" + light_intensity;
        for(CoapClient clientLight: clientLightList) {
            clientLight.put(new CoapHandler() {
                @Override
                public void onLoad(CoapResponse coapResponse) {
                    if (!coapResponse.isSuccess())
                        System.out.print("[ERROR] Light intensity: PUT request unsuccessful");
                }

                @Override
                public void onError() {
                    System.err.print("[ERROR] Light intensity " + clientLight.getURI() + "]");
                }
            }, msg, MediaTypeRegistry.TEXT_PLAIN);
        }
    }
}
