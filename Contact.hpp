
#ifndef COMM_CONTACT_HPP
#define COMM_CONTACT_HPP

namespace comm {

class Contact {
public:
    Contact(const Address& a);
    Contact(const Contact& orig);
    virtual ~Contact();
    
    virtual void do_event(Buffer&) {};
    virtual void do_data(Buffer&) {};
    virtual void stat(stat_t) {}
    
    const Address address;
    Connector * comm;
   
protected:
    volatile bool connected;
    volatile bool running;
    
private:
    mask_t sent_mask;
    mask_t rec_mask;
    
    unsigned char * conn_buffer;
    
    sf::Mutex conn_mutex;
    sf::Thread conn_thread;
    
    void conn_run();
    
    friend class Connector;
};

} // comm

#endif // COMM_CONTACT_HPP
