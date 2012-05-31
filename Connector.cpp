
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
                Contact * rec_c = get_contact(rec_a);
                
                if (!rec_c) { 
                    Buffer temp(rec_buffer,1,rec_s);
                    rec_c = contactor->make_contact(rec_a, temp);
                   
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
                Buffer temp(rec_buffer,1,block_s);
                contactor->init(temp);
                send_raw(rec_a,temp);
            } break;
           
            case JOIN_SUCCESS: {
               
            } break;
           
            case JOIN_FAILURE: {
               
            } break;
           
            case DATA: {
                Contact * rec_c = get_contact(rec_a);
                if (!rec_c) break;
                mask_t diff, mask;
                Buffer temp(rec_buffer,1,rec_s);
                
                rec_c->conn_mutex.lock();
                //TODO update connection status
                mask = rec_c->sent_mask;
                rec_c->conn_mutex.unlock();
                
                temp >> diff;
                diff ^= mask;
               
                for (int t=0; diff; diff>>=1, t++) {
                    if (diff & 1) {
                        char * data = event_buffer+t*block_s;
                        sf::Uint16 size = *reinterpret_cast<sf::Uint16*>(data-2);
                        send_event(rec_c->address,mask,data,size);
                    }
                }
                
                rec_c->do_data(temp);
            } break;
           
            case EVENT: {
                Contact * rec_c = get_contact(rec_a);
                if (!rec_c) break;
                
                rec_c->conn_mutex.lock();
                if (((rec_c->rec_mask >> rec_buffer[1]) & 1) == rec_buffer[2]) {
                    rec_c->conn_mutex.unlock();
                } else {
                    rec_c->rec_mask ^= (1 << rec_buffer[1]);
                    rec_c->conn_mutex.unlock();
                    Buffer temp(rec_buffer,header_s,rec_s);
                    rec_c->do_event(temp);                   
                }               
            } break;
           
            //letting it run as a server, not sure what will happen
            case SERVER_REQUEST: {
                *rec_buffer = SERVER_REPLY;
                Buffer(rec_buffer,1,block_s) << rec_a;
                send_raw(rec_a,rec_buffer,7);
            } break;
           
            case SERVER_REPLY: {
                if (!synched) {
                    address_mutex.lock();
                    address = Address(rec_buffer+1);
                    synched = true;
                    address_mutex.unlock();
                }
            } break;
               
            //still letting it run as a server, not sure what will happen
            case WORKAROUND_REQUEST: {
                Address temp(rec_buffer+1);
                
                *rec_buffer = WORKAROUND_FORWARD;
                Buffer(rec_buffer,1,block_s) << rec_a;
                send_raw(temp,rec_buffer,7);
            } break;
           
            case WORKAROUND_FORWARD: {
                rec_a = Address(rec_buffer+1);
                Contact * rec_c = get_contact(rec_a);
                if (!rec_c) {
                    *rec_buffer = WORKAROUND_ACK;
                    send_raw(rec_a,rec_buffer,1);
                } else {
                    *rec_buffer = JOIN;
                    Buffer temp(rec_buffer,1,block_s);
                    contactor->init(temp);
                    send_raw(rec_a,temp);
                }
            } break;
            
            case WORKAROUND_ACK: {
                Contact * rec_c = get_contact(rec_a);
                if (rec_c) {
                    *rec_buffer = JOIN;
                    Buffer temp(rec_buffer,1,block_s);
                    contactor->init(temp);
                    send_raw(rec_a,temp);
                }
            } break;
        }
    }
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
