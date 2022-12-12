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

#define LOG_MODULE "fertilizer"
#define LOG_LEVEL LOG_LEVEL_APP

static void fertilizer_put_handler(coap_message_t *request, coap_message_t *response, uint8_t *buffer, uint16_t preferred_size, int32_t *offset);

RESOURCE(res_fertilizer,
	"title=\"Fertilizer\";rt=\"Control\"",
		NULL,
        NULL,
        fertilizer_put_handler,
        NULL);
 
int quantity;
bool fertilizer_on = false;

static void fertilizer_put_handler(coap_message_t *request, coap_message_t *response, uint8_t *buffer, uint16_t preferred_size, int32_t *offset)
{
	size_t len = 0;
	const char *text = NULL;
    int put_success = 1;

	len = coap_get_post_variable(request, "quantity", &text);
	
	if(len>0) {
		if(strcmp(text, "0") == 0) {
			fertilizer_on = false;
			quantity = 0;
			leds_set(LEDS_NUM_TO_MASK(LEDS_RED));
			LOG_INFO("Fertilizer OFF\n");
		} else if(strcmp(text, "1") == 0) {
			fertilizer_on = true;
			quantity = 1;
			leds_set(LEDS_NUM_TO_MASK(LEDS_GREEN));
			LOG_INFO("Fertilizer ON, Quantity=1\n");
		}  else if(strcmp(text, "2") == 0) {
			fertilizer_on = true;
			quantity = 2;
			leds_set(LEDS_NUM_TO_MASK(LEDS_GREEN));
			LOG_INFO("Fertilizer ON, Quantity=2\n");
		} else if(strcmp(text, "3") == 0) {
			fertilizer_on = true;
			quantity = 3;
			leds_set(LEDS_NUM_TO_MASK(LEDS_GREEN));
			LOG_INFO("Fertilizer ON, Quantity=3\n");
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
