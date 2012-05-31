
#include "comm.hpp"

namespace comm {

Address::Address(): 
        address(),port(sf::Socket::AnyPort) {}
Address::Address(const Address& a): 
        address(a.address),port(a.port) {}
Address::Address(char a, char b, char c, char d, sf::Uint16 p): 
        address(a,b,c,d),port(p) {}
Address::Address(sf::Uint32 i, sf::Uint16 p): 
        address(i),port(p) {}
Address::Address(const sf::IpAddress& i, sf::Uint16 p): 
        address(i),port(p) {}

Address::Address(char* b) {
    //Buffer(b,6) >> *this;
}

Address::Address(Buffer & b) {
    //b >> *this;
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
bool operator> (const Address & l, const Address & r) {return   r<l;  }
bool operator<=(const Address & l, const Address & r) {return !(r<l); }
bool operator>=(const Address & l, const Address & r) {return !(l<r); }

Buffer& operator>>(Buffer & b, Address & a) {
    sf::Uint32 temp;
    b >> temp;
    a.address = sf::IpAddress(temp);
    b >> a.port;
}

Buffer& operator<<(Buffer & b, const Address & a) {
	b << a.address.toInteger();
	b << a.port;
}

} // comm
