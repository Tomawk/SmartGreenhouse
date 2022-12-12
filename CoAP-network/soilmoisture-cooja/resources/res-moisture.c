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

#define LOG_MODULE "moisture-sensor"
#define LOG_LEVEL LOG_LEVEL_APP

static void moisture_get_handler(coap_message_t *request, coap_message_t *response, uint8_t *buffer, uint16_t preferred_size, int32_t *offset);
static void moisture_event_handler(void);

EVENT_RESOURCE(res_moisture,
	"title=\"Moisture sensor\";obs",
	moisture_get_handler,
        NULL,
        NULL,
        NULL,
	 moisture_event_handler);


 
static double moisture_value = 1.5; //default value 

static bool update_moisture() { // simulate the behavior of the real sensor

	//SE IL PESO DEL TERRENO BAGNATO DIVISO IL TERRENO ASCIUTTO È NEI SEGUENTI RANGE
	//DA 1 A 1.3 È ASCIUTTO E PERCIÒ RICHIEDE IRRIGAZIONE
	//DA 1.3 A 1.7 E' NEL RANGE GIUSTO
	//DA 1.7 O PIU SPENGERE IRRIGAZIONE

	//NEL TEMPO SE IL SISTEMA DI IRRIGAZIONE E' SPENTO ALLORA IL MOISTURE DIMINUIRÀ SCENDENDO PIAN PIANO A 1

	bool updated = false;
	double old_moisture_value = moisture_value;

    srand(time(NULL));

	if(irrigation_on) {	// Se il sistema di irrigazione è acceso il moisture aumenterà indefinitivamente (?)
		moisture_value = old_moisture_value + 0.1;
		
	} else { //irrigation off
		if(old_moisture_value != 1) moisture_value = old_moisture_value - 0.1; //non può scendere sotto 1s
		else updated = true;
	}

	if(old_moisture_value != moisture_value)
		updated = true;

	return updated;
}

static void moisture_event_handler(void) {
	if (update_moisture()) { // if the value is changed
		if(moisture_value >=1 && moisture_value <= 1.3 && irrigation_on == false) {// va attivata l'irrigazione, terreno troppo secco
			LOG_INFO("Soil moisture is too dry! moisture_value: %f, please turn the irrigation ON\n",moisture_value);
			coap_notify_observers(&res_moisture);
		}else if(moisture_value >=1.7 && irrigation_on == true){
			LOG_INFO("Soil moisture is too wet! moisture_value: %f, please turn the irrigation OFF\n",moisture_value);
			coap_notify_observers(&res_moisture);
		}else {
			LOG_INFO("Soil Moisture value: %f \n", moisture_value);
			// Notify all the observers
    		coap_notify_observers(&res_moisture);
		}
	}
}


static void moisture_get_handler(coap_message_t *request, coap_message_t *response, uint8_t *buffer, uint16_t preferred_size, int32_t *offset) {
  	char message[64];
  	int length = 64;

  	snprintf(message, length, "{\"node_id\": %d, \"moisture_value\": %f}", node_id, moisture_value); //JSON format

  	size_t len = strlen(message);
  	memcpy(buffer, (const void *) message, len);

  	coap_set_header_content_format(response, TEXT_PLAIN);
  	coap_set_header_etag(response, (uint8_t *)&len, 1);
  	coap_set_payload(response, buffer, len);
}