
#include "comm.hpp"

#define SLEEPLOCK(time,delay,state) {			  	\
	sf::Time sleeplock = (time);       				\
	while(!(state) && sleeplock >= sf::Time::Zero) {\
		sf::sleep(delay);  						  	\
		sleeplock -= (delay);			  			\
	}								  			  	\
}

#define SLEEPLOCK(time,state) SLEEPLOCK(time,res_d,state)

namespace comm {

void Contact::conn_run() {
	//simply forward control to comm
	comm.send_run(this);
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

void Connector::send_run(Contact * c) {
	//TODO
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

} // comm

