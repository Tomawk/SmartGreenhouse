#include "contiki.h"
#include "net/routing/routing.h"
#include "mqtt.h"
#include "net/ipv6/uip.h"
#include "net/ipv6/uip-icmp6.h"
#include "net/ipv6/sicslowpan.h"
#include "sys/etimer.h"
#include "sys/ctimer.h"
#include "lib/sensors.h"
#include "dev/button-hal.h"
#include "dev/leds.h"
#include "os/sys/log.h"
#include "mqtt-client.h"

#include <string.h>
#include <strings.h>

#include <sys/node-id.h>
#include <time.h>

#define LOG_MODULE "tmp-sensor"
#ifdef  MQTT_CLIENT_CONF_LOG_LEVEL
#define LOG_LEVEL MQTT_CLIENT_CONF_LOG_LEVEL
#else
#define LOG_LEVEL LOG_LEVEL_DBG
#endif

/* MQTT broker address. */
#define MQTT_CLIENT_BROKER_IP_ADDR "fd00::1"

static const char *broker_ip = MQTT_CLIENT_BROKER_IP_ADDR;

// Defaukt config values
#define DEFAULT_BROKER_PORT         1883
#define DEFAULT_PUBLISH_INTERVAL    (30 * CLOCK_SECOND)
#define PUBLISH_INTERVAL	    (5 * CLOCK_SECOND)

// We assume that the broker does not require authentication

/* Various states */
static uint8_t state;

#define STATE_INIT    		0	// initial state
#define STATE_NET_OK    	1	// Network is initialized
#define STATE_CONNECTING      	2	// Connecting to MQTT broker
#define STATE_CONNECTED       	3	// Connection successful
#define STATE_SUBSCRIBED      	4	// Topics subscription done
#define STATE_DISCONNECTED    	5	// Disconnected from MQTT broker

PROCESS_NAME(processo_della_temperatura);
AUTOSTART_PROCESSES(&processo_della_temperatura);

/* Maximum TCP segment size for outgoing segments of our socket */
#define MAX_TCP_SEGMENT_SIZE    32
#define CONFIG_IP_ADDR_STR_LEN  64

/*
 * Buffers for Client ID and Topics.
 */
#define BUFFER_SIZE 64

static char client_id[BUFFER_SIZE];
static char pub_topic[BUFFER_SIZE];
static char sub_topic[BUFFER_SIZE];

// Periodic timer to check the state of the MQTT client
#define STATE_MACHINE_PERIODIC     (CLOCK_SECOND >> 1)
static struct etimer periodic_timer;

/*
 * The main MQTT buffers.
 * We will need to increase if we start publishing more data.
 */
#define APP_BUFFER_SIZE 512
static char app_buffer[APP_BUFFER_SIZE];

static struct mqtt_message *msg_ptr = 0;

static struct mqtt_connection conn;

PROCESS(processo_della_temperatura, "Temperature sensor process");

static bool increase_temperature = false;
static bool decrease_temperature = false;
static int temperature = 50; // we cannot use float value in the testbed
static int variation = 0;

// Function called for handling an incoming message
static void pub_handler(const char *topic, uint16_t topic_len, const uint8_t *chunk, uint16_t chunk_len) {
	LOG_INFO("Message received: topic='%s' (len=%u), chunk_len=%u\n", topic, topic_len, chunk_len);

	if(strcmp(topic, "actuator") == 0) {
		LOG_INFO("Received Actuator command\n");
		if(strcmp((const char*) chunk, "ON_HEA") == 0) {
			LOG_INFO("Turn ON the heater\n");
			increase_temperature = true;
			decrease_temperature = false;	
		} else if(strcmp((const char*) chunk, "ON_REF") == 0) {
			LOG_INFO("Turn ON the refrigerator\n");	
			increase_temperature = false;
			decrease_temperature = true;
		} else if(strcmp((const char*) chunk, "OFF") == 0)  {
			LOG_INFO("Turn OFF the refrigerator/heater\n");	
			increase_temperature = false;
			decrease_temperature = false;
		}	
	} else {
		LOG_ERR("Incorrect Topic!\n");
	}
}

// This function is called each time occurs a MQTT event
static void mqtt_event(struct mqtt_connection *m, mqtt_event_t event, void *data) 
{
	switch(event) 
	{
		case MQTT_EVENT_CONNECTED: 
		{
			LOG_INFO("MQTT connection acquired\n");
			state = STATE_CONNECTED;
			break;
		}
		case MQTT_EVENT_DISCONNECTED: 
		{
			printf("MQTT connection disconnected. Reason: %u\n", *((mqtt_event_t *)data));
			state = STATE_DISCONNECTED;
			process_poll(&processo_della_temperatura);
			break;
		}
		case MQTT_EVENT_PUBLISH: 
		{
			msg_ptr = data;
			pub_handler(msg_ptr->topic, strlen(msg_ptr->topic), msg_ptr->payload_chunk, msg_ptr->payload_length);
			break;
		}
		case MQTT_EVENT_SUBACK: 
		{
			#if MQTT_311
			mqtt_suback_event_t *suback_event = (mqtt_suback_event_t *)data;
			if(suback_event->success) 
			{
				LOG_INFO("Application has subscribed to the topic\n");
			} 
			else 
			{
				LOG_ERR("Application failed to subscribe to topic (ret code %x)\n", suback_event->return_code);
			}
			#else
			LOG_INFO("Application has subscribed to the topic\n");
			#endif
			break;
		}
		case MQTT_EVENT_UNSUBACK: 
		{
			LOG_INFO("Application is unsubscribed to topic successfully\n");
			break;
		}
		case MQTT_EVENT_PUBACK: 
		{
			LOG_INFO("Publishing complete.\n");
			break;
		}
		default:
			LOG_INFO("Application got a unhandled MQTT event: %i\n", event);
			break;
	}
}

static bool have_connectivity(void) {
	if(uip_ds6_get_global(ADDR_PREFERRED) == NULL || uip_ds6_defrt_choose() == NULL) {
		return false;
	}
	return true;
}

PROCESS_THREAD(processo_della_temperatura, ev, data) {

	PROCESS_BEGIN();

	mqtt_status_t status;
	char broker_address[CONFIG_IP_ADDR_STR_LEN];

	// Initialize the ClientID as MAC address
	snprintf(client_id, BUFFER_SIZE, "%02x%02x%02x%02x%02x%02x",
		     linkaddr_node_addr.u8[0], linkaddr_node_addr.u8[1],
		     linkaddr_node_addr.u8[2], linkaddr_node_addr.u8[5],
		     linkaddr_node_addr.u8[6], linkaddr_node_addr.u8[7]);

	// Broker registration					 
	mqtt_register(&conn, &processo_della_temperatura, client_id, mqtt_event, MAX_TCP_SEGMENT_SIZE);
			
	state=STATE_INIT;
				    
	// Initialize periodic timer to check the status 
	etimer_set(&periodic_timer, PUBLISH_INTERVAL);

	while(1) {
		PROCESS_YIELD();

		if((ev == PROCESS_EVENT_TIMER && data == &periodic_timer) || ev == PROCESS_EVENT_POLL) {			  			  
			if(state==STATE_INIT) {
				if(have_connectivity()==true) { 
					state = STATE_NET_OK;
				}
			}   
			if(state == STATE_NET_OK) {
				LOG_INFO("Connecting to MQTT server . . .\n"); 
			  	memcpy(broker_address, broker_ip, strlen(broker_ip));
			  
			  	mqtt_connect(&conn, broker_address, DEFAULT_BROKER_PORT,
						   (DEFAULT_PUBLISH_INTERVAL * 3) / CLOCK_SECOND,
						   MQTT_CLEAN_SESSION_ON);
			  	state = STATE_CONNECTING;
			} 
			if(state==STATE_CONNECTED) {
				// Subscribe to a topic
				strcpy(sub_topic,"actuator");
				status = mqtt_subscribe(&conn, NULL, sub_topic, MQTT_QOS_LEVEL_0);
				if(status == MQTT_STATUS_OUT_QUEUE_FULL) {
					LOG_ERR("Tried to subscribe but command queue was full!\n");
					PROCESS_EXIT();
				}
				state = STATE_SUBSCRIBED;
			}	  
			if(state == STATE_SUBSCRIBED) {	
				sprintf(pub_topic, "%s", "temperature");
				
				// simulate the behavior of the sensor				
				if (increase_temperature || decrease_temperature) { //if the heater or the refrigerator is ON then the temperature must increase/decrease ONLY depending on which device is ON
					variation = rand()%3; 	// a value in [0,2]
					temperature = (increase_temperature) ? (temperature + variation) : (temperature - variation);
				} else { //if none of them are ON, it decrease or increase randomly
						variation = (rand()%5)-2; // a value in [-2, 2]
						temperature = temperature + variation;
				}

				LOG_INFO("New value of temperature recorded: %d\n", temperature);
				
				sprintf(app_buffer, "{\"node_id\": %d, \"temperature\": %d}", node_id, temperature);
				mqtt_publish(&conn, NULL, pub_topic, (uint8_t *)app_buffer,
				strlen(app_buffer), MQTT_QOS_LEVEL_0, MQTT_RETAIN_OFF);

			} 
			else if ( state == STATE_DISCONNECTED ){
				LOG_ERR("Disconnected from MQTT broker\n");	
				state = STATE_INIT;
			}
			etimer_set(&periodic_timer, PUBLISH_INTERVAL);
		}
	}
	PROCESS_END();
}
