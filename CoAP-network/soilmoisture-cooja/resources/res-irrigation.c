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

#define LOG_MODULE "irrigation"
#define LOG_LEVEL LOG_LEVEL_APP

static void irrigation_put_handler(coap_message_t *request, coap_message_t *response, uint8_t *buffer, uint16_t preferred_size, int32_t *offset);

RESOURCE(res_irrigation,
	"title=\"Irrigation System\";rt=\"Control\"",
		NULL,
        NULL,
        irrigation_put_handler,
        NULL);
 
bool irrigation_on = false;

static void irrigation_put_handler(coap_message_t *request, coap_message_t *response, uint8_t *buffer, uint16_t preferred_size, int32_t *offset)
{
	size_t len = 0;
	const char *text = NULL;
    int put_success = 1;

	len = coap_get_post_variable(request, "mode", &text);
	if(len>0) {
		if(strcmp(text, "ON") == 0) {
			irrigation_on = true;
			leds_off(LEDS_ALL);
			leds_set(LEDS_NUM_TO_MASK(LEDS_GREEN));
			LOG_INFO("Irrigation System ON\n");
		} else if(strcmp(text, "OFF") == 0) {
			irrigation_on = false;
			leds_off(LEDS_ALL);
			leds_set(LEDS_NUM_TO_MASK(LEDS_RED));
			LOG_INFO("Irrigation System OFF\n");
		} 
		  else {
			put_success = 0;
		}
	} else {
		put_success = 0;
	}

	
	if(!put_success) {
    	coap_set_status_code(response, BAD_REQUEST_4_00);
 	}
}
