
#include "comm.hpp"

namespace comm {

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

} // comm
