# ispn-grpc
This project is a POC of a client/server architecture for Infinispan. It currently generates a gRPC proxy that exposes a restrict subset of features (put/get) of an Infinispan EmbeddedCache.
Java, c++ and go client examples are provided atm and with a very basic set of features, in fact the aim is no to develop a full featured client, but rather to provide an idea of how a cross-language client can look like. So the point is keep the framework really languange independent and see if with this approach is possible to extend the gRPC API to something useful to the development of cache or datagrid applications in every gRCP supported language.

## Java world

### The server
An early version of the server implementation is provided in the ServerNode.java source file. It's just an EmbeddedCache instance wrapped (in a very naive mode) by a gRPC proxy.

### The client
Two examples of client are available:

- Sample.java is where to look for a first impression on how to use a gRPC client;
- CacheClient.java is a more complex example that wants to test the interoperability between different language clients.


mvn package should do everything for you. Running the ServerNode.main and the CacheClient.main via Eclipse is a good way to go. The command line alternative is:

- mvn exec:java -Dexec.mainClass=org.infinispan.experimental.ServerNode  # starts the server
- mvn exec:java -Dexec.mainClass=org.infinispan.experimental.CacheClient  # runs the client


## C++

- sample.cc is where to look for a first impression on how to use a gRPC client;
- cache_client.cc is a more complex example that wants to test the interoperability between different language clients.


You need to build protobuf and grpc for you platform, the cmake will do the rest (as long as you adapt the CMakeLists.txt) and you'll get the c++ client (cache_client executable). Run it against the java server.

This is my build chain:

- cd cpp
- mkdir build
- cd build
- gRPC_CPP_PLUGIN_EXECUTABLE=/usr/local/bin/grpc_cpp_plugin LD_LIBRARY_PATH=/opt/local/lib/ cmake .. -DCMAKE_FIND_ROOT_PATH=/opt/local
- cmake --build .

Specific settings are needed to point a specific grpc protobuf version. Try to figure out how things works for now. More info will come.


## Golang

- sample.go is where to look for a first impression on how to use a gRPC client;
- cache_client.cc is a more complex example that wants to test the interoperability between different language clients.


The Go example should easily work in this way:

- go get -u google.golang.org/grpc
- install protoc compiler (3.5.0+)
- go get -u github.com/golang/protobuf/protoc-gen-go
- PATH=$GOPATH/bin/:$PATH protoc -I$PWD/src/main/proto  cache_client.proto --go_out=plugins=grpc:go/src/ispn/
- cd go
- GOPATH=$PWD:$GOPATH go run main.go
