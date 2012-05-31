
#include "comm.hpp"

namespace comm {
    
        
    
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
        
        /*if (!connected) {
            *rec_buffer = JOIN;
                    Buffer temp(rec_buffer,1,block_s);
                    contactor->init(temp);
                    send_raw(rec_a,temp);
            
            for (int t=TRY_0; !connected && t<ALT_TRY_0; ++t) {
                stat((stat_t)t);
                comm->send_raw(address,conn_buffer,conn_s);
                sf::sleep(comm->join_d);
            }
        }
        
        if (!connected && comm->npserver) {
            *conn_buffer = WORKAROUND_REQUEST;
            //address.toBuffer(conn_buffer+1);
            
            for (int t=ALT_TRY_0; !connected && t<FAILURE; ++t) {
                stat((stat_t)t);
                comm->send_raw(comm->npserver,conn_buffer,7);
                sf::sleep(comm->join_d);
            }
        }
        
        stat(connected?SUCCESS:FAILURE); */
    }
}

} // comm
