
#ifndef COMM_CONNECTOR_HPP
#define	COMM_CONNECTOR_HPP

namespace comm {

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
    
    virtual ~Connector();
    
private:
    const sf::Time server_d;
    const sf::Time join_d;
    const sf::Time fast_d;
    const sf::Time slow_d;
    const float inc_r;
    const float dec_r;
    const unsigned short port;
    
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
    
    Contact * get(const Address&);
    void pad(unsigned char*,size_t);
    size_t get_pad(unsigned char*);
    
    void send_raw(const Address&, unsigned char*, size_t);
    void synch_run();
    void rec_run();
    
    friend void Contact::conn_run();
    
    //these are here to prevent copying
    Connector(const Connector&);
    Connector& operator=(const Connector&);
};

} // comm

#endif	// COMM_CONNECTOR_HPP
