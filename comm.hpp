/* 
 * File:   connector.hpp
 * Author: geky
 *
 * Created on May 19, 2012, 6:42 AM
 */

#ifndef COMM_HPP
#define	COMM_HPP

#include <cstddef>
#include <climits>
#include <set>
#include <SFML/Network.hpp>
#include <SFML/System.hpp>
#include <stdint.h>
#include <string.h>

#include <climits>
#include <sstream>




namespace comm {
    
//mask size and type
typedef sf::Uint16 mask_t;
static const size_t mask_bits = CHAR_BIT*sizeof(mask_t);
static const size_t header_s = sizeof(mask_t)+1;

template<class T> inline void to_buffer(T,unsigned char*) {}
template<class T> inline T from_buffer(unsigned char*) {}

class Address;
class Buffer;
class Contact;
class Connector;

enum id_t {
    KEEP_OPEN           = 0x00,
    JOIN                = 0x20,
    JOIN_SUCCESS        = 0x30,
    JOIN_FAILURE        = 0x31,
    DATA                = 0x40,
    EVENT               = 0x50,
    STREAM_REQUEST      = 0x52,
    SERVER_REQUEST      = 0x60,
    SERVER_REPLY        = 0x61,
    WORKAROUND_REQUEST  = 0x70,
    WORKAROUND_FORWARD  = 0x71,
    WORKAROUND_ACK      = 0x72,
};

enum stat_t {
    SUCCESS = 0,
    TRY_0, TRY_1, TRY_2, TRY_3,
    ALT_TRY_0, ALT_TRY_1, ALT_TRY_2, ALT_TRY_3,
    FAILURE,
    REJECTION
};

    
struct config { // config for comm
    int server_time_delay;
    int join_time_delay;
    int fast_delay;
    int slow_delay;
	int resolution;
    float increase_ratio;
    float decrease_ratio;
    unsigned short default_port;
    //Address npserver;
}; 

static const config default_config = {
    8000,1000,
    1000,1000,
	10,	
    0.5, 0.25,
    11110, 
    //Address() //11111
};

class Contactor {
public:
    virtual Contact * make_contact(const Address&, Buffer&) {return 0;}
    virtual void poll(Buffer&) {}
    virtual void init(Buffer&) {}
    virtual void stat(stat_t) {}
};

static Contactor default_contactor;  

} //comm

#include "Address.hpp"
#include "Buffer.hpp"
#include "Contact.hpp"
#include "Connector.hpp"

#endif	// COMM_HPP


