# ispn-grpc

This project currently generates a gRPC proxy for an Infinispan EmbeddedCache object. 
Java and c++ clients are provided atm, with that user can put (k1,v1) and get(k1)->v1. That is really great, but the idea is to extend the gRPC API to something useful to develop cache or datagrid applications in every gRCP supported language.

## Java world

mvn package should do everything for you. Running the ServerNode.main and the CacheClient.main via Eclipse is the best way go and it should works (at least it does on my lap)

## C++

You need to build protobuf and grpc for you platform, the cmake will do the rest (as long as you adapt the CMakeLists.txt) and you'll get the c++ client (cache_client executable). Run it against the java server.



