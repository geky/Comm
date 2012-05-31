
#ifndef COMM_ADDRESS_HPP
#define COMM_ADDRESS_HPP

namespace comm {

class Address {
public:
    Address();
    Address(const Address&);
    Address(unsigned char*);
    Address(char, char, char, char, sf::Uint16);
    Address(sf::Uint32, sf::Uint16);
    Address(const sf::IpAddress&, sf::Uint16);
    Address(const std::string&);
    
    std::string toString();
    void toBuffer(unsigned char*);
    
    sf::IpAddress address;
    sf::Uint16 port;
    
    operator bool();
};

bool operator==(const Address&,const Address&);
bool operator< (const Address&,const Address&);
bool operator!=(const Address&,const Address&);
bool operator> (const Address&,const Address&);
bool operator<=(const Address&,const Address&);
bool operator>=(const Address&,const Address&);

Buffer& operator>>(Buffer&, Address&);
Buffer& operator<<(Buffer&, const Address&);

} // comm

#endif // COMM_ADDRESS_HPP
