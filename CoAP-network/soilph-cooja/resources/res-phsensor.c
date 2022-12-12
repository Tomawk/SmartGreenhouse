#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <stdint.h>
#include "contiki.h"
#include "coap-engine.h"
#include "sys/log.h"
#include "dev/leds.h"
#include "sys/node-id.h"
#include <time.h>

#include "global_variables.h"

#define LOG_MODULE "ph-sensor"
#define LOG_LEVEL LOG_LEVEL_APP

static void ph_get_handler(coap_message_t *request, coap_message_t *response, uint8_t *buffer, uint16_t preferred_size, int32_t *offset);
static void ph_event_handler(void);

EVENT_RESOURCE(res_phsensor,
	"title=\"PH sensor\";obs",
	ph_get_handler,
        NULL,
        NULL,
        NULL,
	 ph_event_handler);


 
static double ph_value = 8; //default value 

static bool update_ph() { // simulate the behavior of the real sensor
	bool updated = false;
	double old_ph_level = ph_value;

    srand(time(NULL));
    //int value = 0;

	if(fertilizer_on) {	// If the fertilizer is on, depending on the quantity set previously. The ph will reach optimal value 9 fast
		switch(quantity){
			case 1:
				if(old_ph_level>9){
					ph_value = old_ph_level - 0.2;
				}else if(old_ph_level<8){
					ph_value = old_ph_level + 0.2;
				} else { //between
					updated = true;
				}
				break;
			case 2:
				if(old_ph_level>9){
					ph_value = old_ph_level - 0.4;
				}else if(old_ph_level<8){
					ph_value = old_ph_level + 0.4;
				} else {
					updated = true;
				}
				break;
			case 3:
				if(old_ph_level>9){
					ph_value = old_ph_level - 0.6;
				}else if(old_ph_level<8){
					ph_value = old_ph_level + 0.6;
				} else {
					updated = true;
				}
				break;
		}
	} else { //fertilizer off
		double value_inc = (double)rand() / (double)RAND_MAX; // a random number between 0 and 1
		double value_dec = (double)rand() / (double)RAND_MAX; // a random number between 0 and 1
		ph_value = ph_value + value_inc - value_dec;
	}

	if(old_ph_level != ph_value)
		updated = true;

	return updated;
}

static void ph_event_handler(void) {
	if (update_ph()) { // if the value is changed
		if(ph_value >8 && ph_value < 9 && fertilizer_on == true) {//il fertilizzante ha già ristabilito il ph
			LOG_INFO("Automatic fertilizer has stabilized the ph value to: %f, please turn it off\n",ph_value);
			coap_notify_observers(&res_phsensor);
		}else {
			LOG_INFO("Soil PH value: %f \n", ph_value);
			// Notify all the observers
    		coap_notify_observers(&res_phsensor);
		}
	}
}


static void ph_get_handler(coap_message_t *request, coap_message_t *response, uint8_t *buffer, uint16_t preferred_size, int32_t *offset) {
  	char message[64];
  	int length = 64;

  	snprintf(message, length, "{\"node_id\": %d, \"ph_value\": %f}", node_id, ph_value); //JSON format

  	size_t len = strlen(message);
  	memcpy(buffer, (const void *) message, len);

  	coap_set_header_content_format(response, TEXT_PLAIN);
  	coap_set_header_etag(response, (uint8_t *)&len, 1);
  	coap_set_payload(response, buffer, len);
}