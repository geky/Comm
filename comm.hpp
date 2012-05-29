/* 
 * File:   connector.hpp
 * Author: geky
 *
 * Created on May 19, 2012, 6:42 AM
 */

#ifndef CONNECTOR_HPP
#define	CONNECTOR_HPP

#include <cstddef>
#include <climits>
#include <set>
#include <SFML/Network.hpp>
#include <SFML/System.hpp>
#include <stdint.h>
#include <string.h>



namespace comm {

    
//mask size and type
typedef sf::Uint16 mask_t;
static const size_t mask_bits = CHAR_BIT*sizeof(mask_t);
static const size_t header_s = sizeof(mask_t)+1;

template<class T> inline void to_buffer(T,unsigned char*);
template<class T> inline T from_buffer(unsigned char*);

class Contactor;
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
    REJECTION,
    FAILURE,
    TRY_0, TRY_1, TRY_2, TRY_3,
    ALT_TRY_0, ALT_TRY_1, ALT_TRY_2, ALT_TRY_3
};


class Address {
public:
    Address();
    Address(const Address&);
    Address(unsigned char*);
    Address(char, char, char, char, sf::Uint16);
    Address(sf::Uint32, sf::Uint16);
    Address(const sf::IpAddress&, sf::Uint16);
    Address(const std::string&);
    
    std::string toString();
    void toBuffer(unsigned char*);
    
    sf::IpAddress address;
    sf::Uint16 port;
    
    operator bool();
};

    
struct config { // config for comm
    int server_time_delay;
    int join_time_delay;
    int fast_delay;
    int slow_delay;
    float increase_ratio;
    float decrease_ratio;
    sf::Uint16 default_port;
    //Address npserver;
}; 

static const config default_config = {
    8000,1000,
    1000,1000,
    0.5, 0.25,
    11110, 
    //Address() //11111
};



class Contact {
public:
    Contact(const Address& a);
    Contact(const Contact& orig);
    virtual ~Contact();
    
    virtual void do_event(unsigned char*) {};
    virtual void do_data(unsigned char*) {};
    virtual void stat(stat_t) {}
    
    const Address address;
protected:
    volatile bool connected;
    volatile bool running;
private:
    mask_t sent_mask;
    mask_t rec_mask;
    
    unsigned char * conn_buffer;
    Connector * comm;
    
    sf::Mutex conn_mutex;
    sf::Thread conn_thread;
    
    void conn_run();
    
    friend class Connector;
};





class Contactor {
public:
    virtual Contact * make_contact(const Address&, unsigned char*) {return 0;}
    virtual void poll(unsigned char *) {}
    virtual void init(unsigned char *) {}
    virtual void stat(stat_t) {}
};

static Contactor default_contactor;  




class Connector {

public:
    const size_t buffer_size;
    enum {
        MIN_BUFFER_SIZE = 4,
        MAX_BUFFER_SIZE = sf::UdpSocket::MaxDatagramSize - header_s
    };
    
    Contactor * const contactor;
    
    Connector(size_t, Contactor* = &default_contactor, const config& = default_config);
    
    Address get_address();
    
    void start();
    void end();
    
    void synch(const Address&);
    void synch(const Address&, const Address&);
    
    void add_contact(Contact*);
    void remove_contact(Contact*);
    
    unsigned char * make_event();
    void send_event(unsigned char*);
    void send_event(Contact*, unsigned char*);
    
    Contact * get(const Address&);
    void pad(unsigned char*,size_t);
    
    virtual ~Connector();
    
private:
    const sf::Time server_d;
    const sf::Time join_d;
    const sf::Time fast_d;
    const sf::Time slow_d;
    const float inc_r;
    const float dec_r;
    const sf::Uint16 port;
    
    Address npserver;
    Address npkeepopen;
    
    Address address;
    sf::Mutex address_mutex;
    
    volatile bool running;
    volatile bool synched;
    
    sf::Mutex contact_mutex;
    sf::Mutex send_mutex;
    sf::Mutex index_mutex;
    
    sf::Thread rec_thread;
    sf::Thread synch_thread;
    
    sf::UdpSocket socket;
    
    std::map<Address,Contact*> contacts;
    
    const size_t block_s;
    
    unsigned char * rec_buffer;
    
    unsigned int buffer_i;
    unsigned char * event_buffer;
    
    void send_raw(const Address&, unsigned char*);
    void send_raw(const Address&, unsigned char*, size_t);
    void synch_run();
    void rec_run();
    
    //these are here to prevent copying
    Connector(const Connector&);
    Connector& operator=(const Connector&);
};

} //comm

#endif	/* CONNECTOR_HPP */


