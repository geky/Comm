
#include "comm.hpp"

//TODO make a buffer class

namespace comm {

Buffer::Buffer():
        data(0), limit(0), index(0) {}
Buffer::Buffer(const Buffer& b):
        data(b.data), limit(b.limit), index(0) {} 
Buffer::~Buffer() {}

// Network write/read operations
Buffer& Buffer::get(char * d, size_t s) {
    assert(index+s <= limit);
    memcpy(d,index,s);
    index += s;
}
    
Buffer& Buffer::operator>>(bool & d) {
    assert(index < limit);
    d = (*(index++) != 0);
}

Buffer& Buffer::operator>>(sf::Uint8 & d) {
    assert(index < limit);
    d = *(index++);
}

Buffer& Buffer::operator>>(sf::Uint16 & d) {
    assert(index+2 <= limit);
    d  = *(index++); d<<=8;
    d |= *(index++);
}

Buffer& Buffer::operator>>(sf::Uint32 & d) {
    assert(index+4 <= limit);
    d  = *(index++); d<<=8;
    d |= *(index++); d<<=8;
    d |= *(index++); d<<=8;
    d |= *(index++);
}

Buffer& Buffer::operator>>(sf::Uint64 & d) {
    assert(index+8 <= limit);
    d  = *(index++); d<<=8;
    d |= *(index++); d<<=8;
    d |= *(index++); d<<=8;
    d |= *(index++); d<<=8;
    d |= *(index++); d<<=8;
    d |= *(index++); d<<=8;
    d |= *(index++); d<<=8;
    d |= *(index++);
}

/*Buffer& Buffer::operator>>(float & d) {
    
}

Buffer& Buffer::operator>>(double & d) {
    
}

Buffer& Buffer::operator>>(char*) {
    
}

Buffer& Buffer::operator>>(std::string&) {
    
}

Buffer& Buffer::operator>>(sf::String&) {
    
}

Buffer& Buffer::operator>>(Address&) {
    
}*/

    
Buffer& Buffer::put(const char * d, size_t s) {
    assert(index+s <= limit);
    memcpy(index,d,s);
    index += s;    
}
    
Buffer& Buffer::operator<<(bool d) {
    assert(index < limit);
    *(index++) = d;    
}

Buffer& Buffer::operator<<(sf::Uint8 d) {
    assert(index+2 <= limit);
    *(index++) = d>>8; 
    *(index++) = d;
}

Buffer& Buffer::operator<<(sf::Uint16 d) {
    assert(index+4 <= limit);
    *(index++) = d>>24; 
    *(index++) = d>>16;
    *(index++) = d>>8;
    *(index++) = d;
}

Buffer& Buffer::operator<<(sf::Uint64 d) {
    assert(index+4 <= limit);
    *(index++) = d>>56; 
    *(index++) = d>>48;
    *(index++) = d>>40;
    *(index++) = d>>32;
    *(index++) = d>>24; 
    *(index++) = d>>16;
    *(index++) = d>>8;
    *(index++) = d;
}
    
/*    Buffer& operator<<(float);
    Buffer& operator<<(double);
    Buffer& operator<<(const char*);
    Buffer& operator<<(const std::string&);
    Buffer& operator<<(const sf::String&);
    Buffer& operator<<(const Address&);
};*/

} // comm




