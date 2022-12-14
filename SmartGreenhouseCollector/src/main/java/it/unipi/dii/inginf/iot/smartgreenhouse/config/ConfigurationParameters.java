package it.unipi.dii.inginf.iot.smartgreenhouse.config;

import com.thoughtworks.xstream.XStream;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import java.nio.file.Files;
import java.nio.file.Paths;
import com.thoughtworks.xstream.security.AnyTypePermission;

/**
 * Class used to store the configuration parameters retrieved from the config.xml
 * There is no need to modify this value, so there are only the getters methods
 */
public class ConfigurationParameters {
    private static volatile ConfigurationParameters instance;

    private String databaseIp;
    private int databasePort;
    private String databaseUsername;
    private String databasePassword;
    private String databaseName;
    private double highBoundSoilPH;
    private double lowBoundSoilPH;
    private double lowBoundMoisture;
    private double highBoundMoisture;
    private int highBoundHumidity;
    private int lowBoundHumidity;
    private int highBoundTemperature;
    private int lowBoundTemperature;

    public static ConfigurationParameters getInstance() {
        if (instance == null) {
            synchronized (ConfigurationParameters.class) {
                if (instance == null) {
                    instance = readConfigurationParameters();
                }
            }
        }
        return instance;
    }

    /**
     * This function is used to read the config.xml file
     *
     * @return ConfigurationParameters instance
     */
    private static ConfigurationParameters readConfigurationParameters() {
        if (validConfigurationParameters()) {
            XStream xs = new XStream();
	        xs.addPermission(AnyTypePermission.ANY);

            String text = null;
            try {
                text = new String(Files.readAllBytes(Paths.get("config.xml")));

            } catch (Exception e) {
                System.err.println(e.getMessage());
            }

            return (ConfigurationParameters) xs.fromXML(text);
        } else {
            System.err.println("Problems with the configuration file!");
            System.exit(1); // If I can't read the configuration file I can't continue with the program
        }
        return null;
    }

    /**
     * This function is used to validate the config.xml with the config.xsd
     *
     * @return true if config.xml is well formatted, otherwise false
     */
    private static boolean validConfigurationParameters() {
        try {
            DocumentBuilder documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            Document document = documentBuilder.parse("config.xml");
            Schema schema = schemaFactory.newSchema(new StreamSource("./config.xsd"));
            schema.newValidator().validate(new DOMSource(document));
        } catch (Exception e) {
            if (e instanceof SAXException)
                System.err.println("Validation Error: " + e.getMessage());
            else
                System.err.println(e.getMessage());

            return false;
        }
        return true;
    }

    public String getDatabaseIp() {
        return databaseIp;
    }

    public int getDatabasePort() {
        return databasePort;
    }

    public String getDatabaseUsername() {
        return databaseUsername;
    }

    public String getDatabasePassword() {
        return databasePassword;
    }

    public String getDatabaseName() {
        return databaseName;
    }

    public double getHighBoundSoilPH() {
        return highBoundSoilPH;
    }

    public double getLowBoundSoilPH() {
        return lowBoundSoilPH;
    }

    public double getLowBoundMoisture() {
        return lowBoundMoisture;
    }

    public double getHighBoundMoisture() {
        return highBoundMoisture;
    }

    public int getHighBoundHumidity() {
        return highBoundHumidity;
    }

    public int getLowBoundHumidity() {
        return lowBoundHumidity;
    }

    public int getHighBoundTemperature() {
        return highBoundTemperature;
    }

    public int getLowBoundTemperature() {
        return lowBoundTemperature;
    }

    @Override
    public String toString() {
        return "ConfigurationParameters{" +
                "databaseIp='" + databaseIp + '\'' +
                ", databasePort=" + databasePort +
                ", databaseUsername='" + databaseUsername + '\'' +
                ", databasePassword='" + databasePassword + '\'' +
                ", databaseName='" + databaseName + '\'' +
                ", highBoundSoilPH=" + highBoundSoilPH +
                ", lowBoundSoilPH=" + lowBoundSoilPH +
                ", lowBoundMoisture=" + lowBoundMoisture +
                ", highBoundMoisture=" + highBoundMoisture +
                ", highBoundHumidity=" + highBoundHumidity +
                ", lowBoundHumidity=" + lowBoundHumidity +
                ", highBoundTemperature=" + highBoundTemperature +
                ", lowBoundTemperature=" + lowBoundTemperature +
                '}';
    }
}
