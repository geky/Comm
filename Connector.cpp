
#include "comm.hpp"

namespace comm {

Connector::Connector(size_t bs, Contactor* conn, const config& conf) :
        contactor(conn), 
        buffer_size(bs<MIN_BUFFER_SIZE ? MIN_BUFFER_SIZE : bs),
        event_i(0),
        block_s(header_s + buffer_size),
        
        rec_thread(&Connector::rec_run, this),
        synch_thread(&Connector::rec_run, this),
        
        server_d(sf::milliseconds(conf.server_time_delay)),
        join_d(sf::milliseconds(conf.join_time_delay)),
        fast_d(sf::milliseconds(conf.fast_delay)),
        slow_d(sf::milliseconds(conf.slow_delay)),
		res_d(sf::milliseconds(conf.resolution)),
        inc_r(conf.increase_ratio),
        dec_r(conf.decrease_ratio),
        port(conf.default_port) {
    
    assert(buffer_size <= MAX_BUFFER_SIZE);
    
    //[rec, e0_s, e0, e1_s, e1...]
    rec_buffer = new char[(2+block_s)*mask_bits + block_s];
    event_buffer = rec_buffer + block_s + 2;
    
    for (int t=0; t<mask_bits; t++) {
        event_buffer[t*block_s-2] = 0;
        event_buffer[t*block_s-1] = 0;
        event_buffer[t*block_s  ] = EVENT;
        event_buffer[t*block_s+1] = t;
    }
}

Address Connector::get_address() {
    Address temp;
    address_mutex.lock();
    temp = address;
    address_mutex.unlock();
    return temp;
}

stat_t Connector::start() {
    if (socket.bind(port) != sf::Socket::Done) return FAILURE;
    
    synched = true;
    rec_thread.launch();
    synch_thread.launch();
    
    contact_mutex.lock();
    for (std::map<Address,Contact*>::iterator i = contacts.begin(); i != contacts.end(); ++i) {
        i->second->running = true;
        i->second->conn_thread.launch();
    }
    contact_mutex.unlock();
    return SUCCESS;
}

void Connector::end() {
    running = false;
    
    contact_mutex.lock();
    for (std::map<Address,Contact*>::iterator i = contacts.begin(); i != contacts.end(); ++i) {
        i->second->running = false;
    }
    contact_mutex.unlock();
}

void Connector::synch(const Address& a) {synch(a,a);}
void Connector::synch(const Address& a, const Address& k) {
    npserver = a;
    npkeepopen = k;
    synched = false;
}

void Connector::add_contact(Contact * c) {
    contact_mutex.lock();
    if (c && !c->comm) {
    
        //TODO stuff
        contacts[c->address] = c;

        c->comm = this;
        c->conn_buffer = new unsigned char[block_s];

        c->running = true;
        c->conn_thread.launch();
    }
    contact_mutex.unlock();
}

Contact * Connector::get_contact(const Address & a) {
    contact_mutex.lock();
    std::map<Address,Contact*>::iterator ci = contacts.find(a);
    contact_mutex.unlock();
    
    if (ci == contacts.end()) return 0;
    return ci->second;
}

void Connector::remove_contact(Contact * c) {
    contact_mutex.lock();
    if (c && c->comm) {

        //TODO stuff
        contacts.erase(c->address);

        c->running = false;
        c->conn_thread.wait(); //TODO this may take up to 1000ms, alternative?

        delete[] c->conn_buffer;
        c->conn_buffer = 0;
        c->comm = 0;
    }
    contact_mutex.unlock();
}

Buffer Connector::make_event() {
    index_mutex.lock();
    Buffer b(event_buffer+(event_i++)*block_s, block_s);
    b.data[-2] = b.data[-1] = 0;
    b.index += header_s;
    event_i %= mask_bits;
    index_mutex.unlock();
    return b;
}

void Connector::send_event(const Address & a, mask_t m, char * b, size_t l) {
    assert(*b == EVENT);
    b[2] = (m >> b[1]) & 1;
    send_raw(a,b,l);
}

void Connector::send_event(Contact * c, const Buffer & b) {
    mask_t mask;
    size_t size = b.limit-b.index;
    
    c->conn_mutex.lock();
    mask = (c->sent_mask ^= (1<<b.data[1]));
    c->conn_mutex.unlock();
    
    *reinterpret_cast<sf::Uint16*>(b.data-2) = size;
    send_event(c->address, mask, b.data, size);
}

void Connector::send_event(const Buffer & b) {
    mask_t temp;    
    contact_mutex.lock();
    for (std::map<Address,Contact*>::iterator i = contacts.begin(); i != contacts.end(); ++i) {
        send_event(i->second,b);
    }
    contact_mutex.unlock();
}

void Connector::send_raw(const Address& a, const Buffer& b) {
    send_mutex.lock();
    socket.send(b.data,b.limit-b.index,a.address,a.port);
    send_mutex.unlock();
}

void Connector::send_raw(const Address& a, void * b, size_t h) {
    send_mutex.lock();
    socket.send(b,h,a.address,a.port);
    send_mutex.unlock();
}

Connector::~Connector() {
    running = false;
    delete[] rec_buffer;
    
    contact_mutex.lock();
    for (std::map<Address,Contact*>::iterator i = contacts.begin(); i != contacts.end(); ++i) {
        remove_contact(i->second);
    }
    contact_mutex.unlock();
}

} // comm

