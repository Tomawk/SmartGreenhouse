package it.unipi.dii.inginf.iot.smartgreenhouse.coap.devices;

import it.unipi.dii.inginf.iot.smartgreenhouse.LightColor;
import it.unipi.dii.inginf.iot.smartgreenhouse.coap.devices.light.Light;
import it.unipi.dii.inginf.iot.smartgreenhouse.coap.devices.soilmoisture.SoilMoisture;
import it.unipi.dii.inginf.iot.smartgreenhouse.coap.devices.soilph.SoilPH;

public class CoapDevicesHandler {
    private Light light = new Light();
    private SoilPH soilPH = new SoilPH();
    private SoilMoisture soilMoisture = new SoilMoisture();

    private static CoapDevicesHandler instance = null;

    public static CoapDevicesHandler getInstance() {
        if(instance == null)
            instance = new CoapDevicesHandler();

        return instance;
    }

    /*      REGISTER AND UNREGISTER DEVICES     */

    public void registerLight(String ip) {
        light.registerLight(ip);
    }

    public void registerSoilPH(String ip){ soilPH.registerSoilPH(ip);}
    public void registerSoilMoisture(String ip){ soilMoisture.registerSoilMoisture(ip);}

    public void unregisterLight(String ip) {
        light.unregisterLight(ip);
    }
    public void unregisterSoilPH(String ip){ soilPH.unregisterSoilPH(ip);}
    public void unregisterSoilMoisture(String ip){ soilMoisture.unregisterSoilMoisture(ip);}

    /*      GET MEASURES FROM SENSORS     */
    public int getLightIntensity() {
        return light.getLightIntensity();
    }
    public double getSoilPH(){ return  soilPH.getSoilPH();}
    public int getFertilizerQuantity(){ return soilPH.getFertilizerQuantity(); }
    public double getSoilMoisture(){return soilMoisture.getSoilMoisture();}
    /*      SET     */
    public void setLightIntensity(LightColor lightColor) {
        light.changeLightIntensity(lightColor);
    }
    public void setFertilizerQuantity(int quantity) { soilPH.setFertilizerQuantity(quantity);}
    public void switchIrrigation(String mode){ soilMoisture.switchIrrigation(mode);}

}
