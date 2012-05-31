
#ifndef COMM_BUFFER_HPP
#define COMM_BUFFER_HPP

namespace comm {

class Buffer {
public:
    Buffer();
    Buffer(const Buffer&);
    virtual ~Buffer();
    
    //use with caution
    char * data;
    char * index;
    char * limit;
    
    Buffer& get(char*, size_t);
    
    Buffer& operator>>(bool&);
    Buffer& operator>>(sf::Int8&);
    Buffer& operator>>(sf::Uint8&);
    Buffer& operator>>(sf::Int16&);
    Buffer& operator>>(sf::Uint16&);
    Buffer& operator>>(sf::Int32&);
    Buffer& operator>>(sf::Uint32&);
    Buffer& operator>>(sf::Int64&);
    Buffer& operator>>(sf::Uint64&);
    Buffer& operator>>(float&);
    Buffer& operator>>(double&);
    Buffer& operator>>(char*);
    Buffer& operator>>(sf::String&);

    Buffer& put(const char*, size_t);
    
    Buffer& operator<<(bool);
    Buffer& operator<<(sf::Int8);
    Buffer& operator<<(sf::Uint8);
    Buffer& operator<<(sf::Int16);
    Buffer& operator<<(sf::Uint16);
    Buffer& operator<<(sf::Int32);
    Buffer& operator<<(sf::Uint32);
    Buffer& operator<<(sf::Int64);
    Buffer& operator<<(sf::Uint64);
    Buffer& operator<<(float);
    Buffer& operator<<(double);
    Buffer& operator<<(const char*);
    Buffer& operator<<(const sf::String&);
};    

}

#endif // COMM_BUFFER_HPP
