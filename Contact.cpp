
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

} // comm

