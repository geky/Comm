/* 
 * File:   connector.cpp
 * Author: geky
 * 
 * Created on May 19, 2012, 6:42 AM
 */

#include "comm.hpp"
#include <climits>
#include <sstream>


namespace comm {

template<> inline void to_buffer(sf::Uint8 n, unsigned char* b) {
    *b=n;
}
    
template<> inline void to_buffer(sf::Uint16 n, unsigned char* b) {
    b[0] = n>>8;
    b[1] = n;  
}

template<> inline void to_buffer(sf::Uint32 n, unsigned char* b) {
    b[0] = n>>24;
    b[1] = n>>16;
    b[2] = n>> 8;
    b[3] = n;
}

template<> inline void to_buffer(sf::Uint64 n, unsigned char* b) {  
    b[0] = n>>56;
    b[1] = n>>48;
    b[2] = n>>40;
    b[3] = n>>32;
    b[4] = n>>24;
    b[5] = n>>16;
    b[6] = n>> 8;
    b[7] = n;
}

template<> inline sf::Uint8  from_buffer(unsigned char* b) {
    return *b;
}

template<> inline sf::Uint16 from_buffer(unsigned char* b) {
    return b[0]<<8 | b[1];
}

template<> inline sf::Uint32 from_buffer(unsigned char* b) {
    return b[0]<<24 | 
           b[1]<<16 |
           b[2]<< 8 |
           b[3];
}

template<> inline sf::Uint64 from_buffer(unsigned char* b) {
    sf::Uint64 temp =  b[0];
    temp = (temp<<8) | b[1];
    temp = (temp<<8) | b[2];
    temp = (temp<<8) | b[3];
    temp = (temp<<8) | b[4];
    temp = (temp<<8) | b[5];
    temp = (temp<<8) | b[6];
    temp = (temp<<8) | b[7];
    
    return temp;
}



Address::Address(): 
        address(),port(sf::Socket::AnyPort) {}
Address::Address(const Address& a): 
        address(a.address),port(a.port) {}
Address::Address(unsigned char* b): 
        address(from_buffer<sf::Uint64>(b)),port(from_buffer<sf::Uint16>(b)) {}
Address::Address(char a, char b, char c, char d, sf::Uint16 p): 
        address(a,b,c,d),port(p) {}
Address::Address(sf::Uint32 i, sf::Uint16 p): 
        address(i),port(p) {}
Address::Address(const sf::IpAddress& i, sf::Uint16 p): 
        address(i),port(p) {}
Address::Address(const std::string& s): 
        address(s.substr(0,s.find_last_of(":"))),
        port(strtoul(s.c_str()+s.find_last_of(":")+1,0,0)) {}

std::string Address::toString() {
    std::ostringstream ss;
    ss << address << ":" << port;
    return ss.str();
}

void Address::toBuffer(unsigned char * b) {
    to_buffer(address.toInteger(),b);
    to_buffer(port,b);
}

Address::operator bool() {
    return port!=sf::Socket::AnyPort || address!=sf::IpAddress::None;
}

bool operator==(const Address & l, const Address & r) {
    return l.address==r.address && l.port==r.port;
}

bool operator< (const Address & l, const Address & r) {
    return l.address==r.address ? 
        l.port    < r.port   :
        l.address < r.address;
}

bool operator!=(const Address & l, const Address & r) {return !(l==r);}
bool operator> (const Address & l, const Address & r) {return r<l;}
bool operator<=(const Address & l, const Address & r) {return !(r<l);}
bool operator>=(const Address & l, const Address & r) {return !(l<r);}



Contact::Contact(const Address& a):
        address(a),comm(0),
        sent_mask(0),rec_mask(0),
        connected(false),running(false),
        conn_thread(&Contact::conn_run,this) {}

Contact::Contact(const Contact& orig): 
        address(orig.address),comm(0),
        sent_mask(0),rec_mask(0),
        connected(false),running(false),
        conn_thread(&Contact::conn_run,this) {}

Contact::~Contact() {
    if (comm) comm->remove_contact(this);
}

void Contact::conn_run() {
    while (running) {
        while (connected) {
            
        }
        
        if (!connected) {
            *conn_buffer = JOIN;
            comm->pad(conn_buffer,1);
            comm->contactor->init(conn_buffer+1);
            
        }
    }
}




Connector::Connector(size_t bs, Contactor* conn, const config& conf) :
        contactor(conn), 
        buffer_size(bs<MIN_BUFFER_SIZE ? MIN_BUFFER_SIZE : bs),
        buffer_i(0),
        block_s(header_s + buffer_size),
        rec_thread(&Connector::rec_run, this),
        synch_thread(&Connector::rec_run, this),
        server_d(sf::milliseconds(conf.server_time_delay)),
        join_d(sf::milliseconds(conf.join_time_delay)),
        fast_d(sf::milliseconds(conf.fast_delay)),
        slow_d(sf::milliseconds(conf.slow_delay)),
        inc_r(conf.increase_ratio),
        dec_r(conf.decrease_ratio),
        port(conf.default_port)
        {
    
    assert(buffer_size <= MAX_BUFFER_SIZE);
    rec_buffer = new unsigned char[block_s*(mask_bits+2)];
    event_buffer = rec_buffer + block_s;
    
    for (unsigned char t=0; t<mask_bits; t++) {
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

void Connector::start() {
    socket.bind(port);
    rec_thread.launch();
    synched = true;
    synch_thread.launch();
    
    contact_mutex.lock();
    for (std::map<Address,Contact*>::iterator i = contacts.begin(); i != contacts.end(); ++i) {
        i->second->running = true;
        i->second->conn_thread.launch();
    }
    contact_mutex.unlock();
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

void Connector::remove_contact(Contact * c) {
    contact_mutex.lock();
    if (c && c->comm) {

        //TODO stuff
        contacts.erase(c->address);


        c->running = false;
        c->conn_thread.wait();

        delete[] c->conn_buffer;
        c->conn_buffer = 0;
        c->comm = 0;
    }
    contact_mutex.unlock();
}

unsigned char * Connector::make_event() {
    index_mutex.lock();
    unsigned char * index = event_buffer + buffer_i*block_s;
    buffer_i = (buffer_i+1) % mask_bits;
    index_mutex.unlock();
    pad(index,header_s);
    return index+header_s;
}

void Connector::send_event(unsigned char * b) {
    b -= header_s;
    assert(*b == EVENT);
    
    mask_t bit = 1 << b[1];
    
    contact_mutex.lock();
    for (std::map<Address,Contact*>::iterator i = contacts.begin(); i != contacts.end(); ++i) {
        Contact * c = i->second;
        c->conn_mutex.lock();
        b[2] = ((c->sent_mask ^= bit) & bit) >> b[1];
        c->conn_mutex.unlock();
        
        send_raw(c->address,b);
    }
    contact_mutex.unlock();
}

void Connector::send_event(Contact * c, unsigned char * b) {
    b -= header_s;
    assert(*b == EVENT);
    
    mask_t bit = 1 << b[1];
    
    c->conn_mutex.lock();
    b[2] = ((c->sent_mask ^= bit) & bit) >> b[1];
    c->conn_mutex.unlock();
    
    send_raw(c->address,b);
}

void Connector::send_raw(const Address& a, unsigned char * b) {
    size_t s = block_s;
    while (!b[s]) --s;
    send_raw(a,b,s);
}

void Connector::send_raw(const Address& a, unsigned char * b, size_t h) {
    send_mutex.lock();
    socket.send(b,h,a.address,a.port);
    send_mutex.unlock();
}

void Connector::synch_run() {
    while(running) {
        while(synched) {
            if(npkeepopen) {
                unsigned char buffer = KEEP_OPEN;
                send_raw(npkeepopen,&buffer,1);
            }
            sf::sleep(server_d);
        }
        
        if (!synched && npserver) {    
            unsigned char buffer = SERVER_REQUEST;
            
            for(int t=TRY_0; !synched && t<ALT_TRY_0;++t) {
                contactor->stat((stat_t)t);
                send_raw(npserver,&buffer,1);
                sf::sleep(join_d);
            }
        }
        
        if (!synched) {
            synched = true;
            contactor->stat(ALT_TRY_0);
            sf::IpAddress temp = sf::IpAddress::getPublicAddress(join_d);
            if (temp == sf::IpAddress::None) {
                contactor->stat(ALT_TRY_1);
                temp = sf::IpAddress::getLocalAddress();
            }
             
            address_mutex.lock();
            address = Address(temp,port);
            address_mutex.unlock();
            
            contactor->stat(FAILURE);
        } else {
            contactor->stat(SUCCESS);
        }
    }
}

void Connector::rec_run() {
    sf::Socket::Status stat;
    
    size_t rec_s;
    Address rec_a;
    
    while(running) {
        stat = socket.receive(rec_buffer, block_s, rec_s, rec_a.address, rec_a.port);
        if(stat != sf::Socket::Done) continue;
       
        switch(*rec_buffer) {
    
            case JOIN: {
                Contact * rec_c = get(rec_a);
                if (!rec_c) {
                    pad(rec_buffer,rec_s);
                    rec_c = contactor->make_contact(rec_a, rec_buffer+1);
                   
                    if (!rec_c) {
                        *rec_buffer = JOIN_FAILURE;
                        send_raw(rec_a,rec_buffer,1);
                        break;
                    }
                   
                    contact_mutex.lock();
                    contacts[rec_a] = rec_c;
                    contact_mutex.unlock();
                }
               
                //TODO update connection status
               
                *rec_buffer = JOIN_SUCCESS;
                pad(rec_buffer,1);
                contactor->init(rec_buffer+1);
                send_raw(rec_a,rec_buffer);
            } break;
           
            case JOIN_SUCCESS: {
               
            } break;
           
            case JOIN_FAILURE: {
               
            } break;
           
            case DATA: {
                Contact * rec_c = get(rec_a);
                if (!rec_c) break;
                
                rec_c->conn_mutex.lock();
                //TODO update connection status
                mask_t mask = rec_c->sent_mask ^ from_buffer<mask_t>(rec_buffer+1);
                rec_c->conn_mutex.unlock();
               
                for (unsigned char*e=event_buffer; mask; mask>>=1, e+=block_s) {
                    if (mask & 1) send_raw(rec_c->address,e);
                }
               
                pad(rec_buffer,rec_s);
                rec_c->do_data(rec_buffer+header_s);
            } break;
           
            case EVENT: {
                Contact * rec_c = get(rec_a);
                if (!rec_c) break;
                
                mask_t bit = 1 << rec_buffer[1];
                bool val = rec_buffer[2];
                
                rec_c->conn_mutex.lock();
                if ((rec_c->rec_mask & bit) >> rec_buffer[1] == val) {
                    rec_c->conn_mutex.unlock();
                } else {
                    rec_c->rec_mask ^= bit;
                    rec_c->conn_mutex.unlock();
                    pad(rec_buffer,rec_s);
                    rec_c->do_event(rec_buffer+header_s);                   
                }               
            } break;
           
            //letting it run as a server, not sure what will happen
            case SERVER_REQUEST: {
                *rec_buffer = SERVER_REPLY;
                rec_a.toBuffer(rec_buffer+1);
                send_raw(rec_a,rec_buffer,7);
            } break;
           
            case SERVER_REPLY: {
                if (!synched) {
                    synched = true;
                    address_mutex.lock();
                    address = Address(rec_buffer+1);
                    address_mutex.unlock();
                }
            } break;
               
            //still letting it run as a server, not sure what will happen
            case WORKAROUND_REQUEST: {
                Address temp(rec_buffer+1);
                
                *rec_buffer = WORKAROUND_FORWARD;
                rec_a.toBuffer(rec_buffer+1);
                send_raw(temp,rec_buffer,7);
            } break;
           
            case WORKAROUND_FORWARD: {
                rec_a = Address(rec_buffer+1);
                Contact * rec_c = get(rec_a);
                if (!rec_c) {
                    *rec_buffer = WORKAROUND_ACK;
                    send_raw(rec_a,rec_buffer,1);
                } else {
                    *rec_buffer = JOIN;
                    pad(rec_buffer,1);
                    contactor->init(rec_buffer+1);
                    send_raw(rec_a,rec_buffer);
                }
            } break;
            
            case WORKAROUND_ACK: {
                Contact * rec_c = get(rec_a);
                if (rec_c) {
                    *rec_buffer = JOIN;
                    pad(rec_buffer,1);
                    contactor->init(rec_buffer+1);
                    send_raw(rec_a,rec_buffer);
                }
            } break;
        }
    }
}

Contact * Connector::get(const Address & a) {
    contact_mutex.lock();
    std::map<Address,Contact*>::iterator ci = contacts.find(a);
    contact_mutex.unlock();
    
    if (ci == contacts.end()) return 0;
    return ci->second;
}

inline void Connector::pad(unsigned char * b, size_t s) {
    memset(b+s,0,block_s-s);
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

