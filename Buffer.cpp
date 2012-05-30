
#include "comm.hpp"
//TODO make a buffer class
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

} // comm
