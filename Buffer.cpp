
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

Buffer& Buffer::operator>>(float & d) {
	sf::Uint32 temp;
    *this >> temp;
	d = reinterpret_cast<float>(temp);
}

Buffer& Buffer::operator>>(double & d) {
    sf::Uint64 temp;
    *this >> temp;
	d = reinterpret_cast<double>(temp);
}

Buffer& Buffer::operator>>(char * d) {
   	while (index < limit) {	
		if (!*index) {
			index++;
			break;
		}

		*(d++) = *(index++);
	}

	*d = 0;
}

Buffer& Buffer::operator>>(sf::String & d) {
	Uint32 temp;
	d.clear();
	while (index+4 <= limit) {
		*this >> temp;
		if (!temp) break;
		d += temp;
	}
	d += 0;
}

Buffer& Buffer::operator>>(Address & a) {
    this* >> a.address;
	this* >> a.port;
}

    
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
    
Buffer& Buffer::operator<<(float d) {
    *this << reinterpret_cast<sf::Uint32>(d);
}

Buffer& Buffer::operator<<(double) {
	*this << reinterpret_cast<sf::Uint64>(d);
}

Buffer& Buffer::operator<<(const char * d) {
	while (index < limit) {	
		if (!*d) {
			*(index++) = 0;
			break;
		}

		*(d++) = *(index++);
	}
}

Buffer& Buffer::operator<<(const sf::String & d) {
	sf::String::ConstIterator i = d.begin();
	while (index+4 <= limit) {
		if (i == d.end()) {
			*this << (sf::Uint32)0;
			break;
		}
		*this << *i;
		++i;
	}
}

Buffer& Buffer::operator<<(const Address & a) {
	*this << a.address;
	*this << a.port;
}

} // comm




