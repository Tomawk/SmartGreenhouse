package it.unipi.dii.inginf.iot.smartgreenhouse.persistence;

import it.unipi.dii.inginf.iot.smartgreenhouse.DBTablePrinter;
import it.unipi.dii.inginf.iot.smartgreenhouse.config.ConfigurationParameters;

import java.sql.*;

public class DBDriver {
    private static DBDriver instance = null;

    private static String databaseIp;
    private static int databasePort;
    private static String databaseUsername;
    private static String databasePassword;
    private static String databaseName;

    public static DBDriver getInstance() {
        if(instance == null)
            instance = new DBDriver();

        return instance;
    }

    private DBDriver() {
        ConfigurationParameters configurationParameters = ConfigurationParameters.getInstance();
        databaseIp = configurationParameters.getDatabaseIp();
        databasePort = configurationParameters.getDatabasePort();
        databaseUsername = configurationParameters.getDatabaseUsername();
        databasePassword = configurationParameters.getDatabasePassword();
        databaseName = configurationParameters.getDatabaseName();
    }

    /**
     * @return the JDBC connection to be used to communicate with MySQL Database.
     *
     * @throws SQLException  in case the connection to the database fails.
     */
    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection("jdbc:mysql://"+ databaseIp + ":" + databasePort +
                        "/" + databaseName + "?zeroDateTimeBehavior=CONVERT_TO_NULL&serverTimezone=CET",
                databaseUsername, databasePassword);
    }

    /**
     * Insert the new sample received by the Soil PH module
     */
    public void insertPHValue(int nodeid, float phvalue) { //TODO CHANGE TO JSON
        try (
                Connection connection = getConnection();
                PreparedStatement statement = connection.prepareStatement("INSERT INTO soilph (node_id, ph_value) VALUES (?, ?)")
        )
        {
            statement.setInt(1, nodeid);
            statement.setFloat(2, phvalue);
            statement.executeUpdate();
        }
        catch (final SQLException e)
        {
            e.printStackTrace();
        }
    }

    /**
     * Insert the new sample received by the SoilMoisture Protocol
     */
    public void insertSoilMoisture(int nodeid, float moisture_value) { //TODO CHANGE TO JSON
        try (
                Connection connection = getConnection();
                PreparedStatement statement = connection.prepareStatement("INSERT INTO soilmoisture (node_id, moisture_value) VALUES (?, ?)")
        )
        {
            statement.setInt(1, nodeid);
            statement.setFloat(2, moisture_value);
            statement.executeUpdate();
        }
        catch (final SQLException e)
        {
            e.printStackTrace();
        }
    }

    public void insertHumidity(int nodeid, int humidity_percentage){
        try (
                Connection connection = getConnection();
                PreparedStatement statement = connection.prepareStatement("INSERT INTO humidity (node_id, humidity_percentage) VALUES (?, ?)")
        )
        {
            statement.setInt(1, nodeid);
            statement.setInt(2, humidity_percentage);
            statement.executeUpdate();
        }
        catch (final SQLException e)
        {
            e.printStackTrace();
        }
    }

    public void insertTemperature(int nodeid, int temperature_value){
        try (
                Connection connection = getConnection();
                PreparedStatement statement = connection.prepareStatement("INSERT INTO temperature (node_id, temperature_value) VALUES (?, ?)");
        )
        {
            statement.setInt(1, nodeid);
            statement.setInt(2, temperature_value);
            statement.executeUpdate();
        }
        catch (final SQLException e)
        {
            e.printStackTrace();
        }
    }
 /*
    public void printTable(String table_name){
        try (
                Connection connection = getConnection();
                PreparedStatement statement = connection.prepareStatement("SELECT * FROM "+ table_name);

        )
        {
            System.out.println("PRINTING TABLE ** " + table_name + " **");
            ResultSet rs = statement.executeQuery();
            ResultSetMetaData rsmd = rs.getMetaData();
            int columnsNumber = rsmd.getColumnCount();

            while (rs.next()){
                for(int i = 1 ; i <= columnsNumber; i++){

                    System.out.print(rs.getString(i) + " "); //Print one element of a row

                }
                System.out.println();//Move to the next line to print the next row.
            }
        }
        catch (final SQLException e)
        {
            e.printStackTrace();
        }

    }
*/

    public void printTable(String table_name, String max_rows){
        try (
                Connection connection = getConnection();

        )
        {
            DBTablePrinter.setDefaultMaxRows(Integer.parseInt(max_rows));
            DBTablePrinter.printTable(connection, table_name);
        }
        catch (final SQLException e)
        {
            e.printStackTrace();
        }

    }
}
