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

#define LOG_MODULE "light-sensor"
#define LOG_LEVEL LOG_LEVEL_APP

static void light_get_handler(coap_message_t *request, coap_message_t *response, uint8_t *buffer, uint16_t preferred_size, int32_t *offset);
//static void light_event_handler(void);
static void light_put_handler(coap_message_t *request, coap_message_t *response, uint8_t *buffer, uint16_t preferred_size, int32_t *offset);

RESOURCE(res_light,
	"title=\"Light sensor\";obs",
	light_get_handler,
        NULL,
        light_put_handler,
        NULL);
	// light_event_handler);
 
static int light_intensity = 0;
static char intensity_str[2];
bool light_on = false;


static void light_get_handler(coap_message_t *request, coap_message_t *response, uint8_t *buffer, uint16_t preferred_size, int32_t *offset) { //TODO FORSE INUTILE
  	char message[64];
  	int length = 64;
  	snprintf(message, length, "{\"light\": %d, \"intensity\": %i}", node_id, light_intensity);

  	size_t len = strlen(message);
  	memcpy(buffer, (const void *) message, len);

  	coap_set_header_content_format(response, TEXT_PLAIN);
  	coap_set_header_etag(response, (uint8_t *)&len, 1);
  	coap_set_payload(response, buffer, len);
}

static void light_put_handler(coap_message_t *request, coap_message_t *response, uint8_t *buffer, uint16_t preferred_size, int32_t *offset)
{
	size_t len = 0;
	const char *text = NULL;
    int put_success = 1;

	len = coap_get_post_variable(request, "intensity", &text);
	memcpy(intensity_str,text,len);
	if(len>0) {
		if(strcmp(intensity_str, "0") == 0) {
			light_on = false;
			light_intensity = 0;
			leds_off(LEDS_ALL);
			leds_set(LEDS_NUM_TO_MASK(LEDS_RED));
			LOG_INFO("Lights OFF\n");
		} else if(strcmp(intensity_str, "1") == 0) {
			light_on = true;
			light_intensity = 1;
			leds_set(LEDS_NUM_TO_MASK(LEDS_YELLOW));
			LOG_INFO("Lights ON with intensity=LOW\n");
		}  else if(strcmp(intensity_str, "2") == 0) {
			light_on = true;
			light_intensity = 2;
			leds_set(LEDS_NUM_TO_MASK(LEDS_GREEN));
			LOG_INFO("Lights ON with intensity=HIGH\n");
		} else {
			put_success = 0;
		}
	} else {
		put_success = 0;
	}

	
	if(!put_success) {
    	coap_set_status_code(response, BAD_REQUEST_4_00);
 	}
}
