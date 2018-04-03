# ispn-grpc
This project currently generates a gRPC proxy for an Infinispan EmbeddedCache object. 
Java and c++ clients are provided atm, with that user can put (k1,v1) and get(k1)->v1. That is really great, but the idea is to extend the gRPC API to something useful to develop cache or datagrid applications in every gRCP supported language.

## Java world
mvn package should do everything for you. Running the ServerNode.main and the CacheClient.main via Eclipse is a good way to go or from the command line:

- mvn exec:java -Dexec.mainClass=org.infinispan.experimental.ServerNode  # starts the server
- mvn exec:java -Dexec.mainClass=org.infinispan.experimental.CacheClient  # runs the client

## C++
You need to build protobuf and grpc for you platform, the cmake will do the rest (as long as you adapt the CMakeLists.txt) and you'll get the c++ client (cache_client executable). Run it against the java server.

This is my build chain:

- cd cpp
- mkdir build
- cd build
- gRPC_CPP_PLUGIN_EXECUTABLE=/usr/local/bin/grpc_cpp_plugin LD_LIBRARY_PATH=/opt/local/lib/ cmake .. -DCMAKE_FIND_ROOT_PATH=/opt/local
- cmake --build .

Specific settings are needed to point a specific grpc protobuf version. Try to figure out how things works for now. More info will come.


## Golang
The Go example should easily work in this way:

- go get -u google.golang.org/grpc
- install protoc compiler (3.5.0+)
- go get -u github.com/golang/protobuf/protoc-gen-go
- PATH=$GOPATH/bin/:$PATH protoc -I$PWD/src/main/proto  cache.proto --go_out=plugins=grpc:go/src/ispn/
- cd go
- GOPATH=$PWD:$GOPATH go run main.go
