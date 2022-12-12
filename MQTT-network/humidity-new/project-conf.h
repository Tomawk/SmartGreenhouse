#ifndef PROJECT_CONF_H_
#define PROJECT_CONF_H_

/* Enable TCP */
#define UIP_CONF_TCP 1
#undef IEEE802154_CONF_PANID
#define IEEE802154_CONF_PANID 0x0009 /* my id assigned for the MQTT client is 9 */

#define LOG_LEVEL_APP LOG_LEVEL_DBG

#endif /* PROJECT_CONF_H_ */
