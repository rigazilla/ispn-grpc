/*
 *
 * Copyright 2015 gRPC authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

#include <iostream>
#include <memory>
#include <string>

#include <grpc++/grpc++.h>

#include "cache.grpc.pb.h"

using grpc::Channel;
using grpc::ClientContext;
using grpc::Status;
using ::Cache;

class CacheClient {
 public:
  CacheClient(std::shared_ptr<Channel> channel)
      : stub_(Cache::NewStub(channel)) {}

  // Assembles the client's payload, sends it and presents the response back
  // from the server.
  std::string putAndGet() {
    // Data we are sending to the server.

    KeyValuePairMsg pair;
    pair.mutable_key()->set_name("k1");
    pair.mutable_value()->set_message("v1");
    // Container for the data we expect from the server.

    // Context for the client. It could be used to convey extra information to
    // the server and/or tweak certain RPC behaviors.
    ClientContext context;

    // The actual RPC.
    ValueMsg reply;
    stub_->put(&context, pair, &reply);

    ValueMsg replyget;
    KeyMsg key;
    key.set_name("k1");
    ClientContext context1;
    Status status = stub_->get(&context1, key, &replyget);

    // Act upon its status.
    if (status.ok()) {
      return replyget.message();
    } else {
      std::cout << status.error_code() << ": " << status.error_message()
                << std::endl;
      return "RPC failed";
    }
  }

 private:
  std::unique_ptr<Cache::Stub> stub_;
};

int main(int argc, char** argv) {
  // Instantiate the client. It requires a channel, out of which the actual RPCs
  // are created. This channel models a connection to an endpoint (in this case,
  // localhost at port 50051). We indicate that the channel isn't authenticated
  // (use of InsecureChannelCredentials()).
  CacheClient greeter(grpc::CreateChannel(
      "localhost:50051", grpc::InsecureChannelCredentials()));
  std::string reply = greeter.putAndGet();
  std::cout << "Value for key k1 is "<< reply << std::endl;

  return 0;
}
