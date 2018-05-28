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
using namespace ispn;

class Sample {
public:
    Sample(std::shared_ptr<Channel> channel)
    :
            stub_(Cache::NewStub(channel)) {
    }
    void run() {
        // Build a key/value for put
        KeyValuePairMsg pair;
        pair.mutable_key()->set_name("k1");
        pair.mutable_value()->set_message("v1");
        ClientContext context;
        ValueMsg putRetVal;

        // Put the entry in the cache
        stub_->put(&context, pair, &putRetVal);

        // Build a key for get
        KeyMsg getKey;
        ValueMsg getRetVal;
        getKey.set_name("k1");
        ClientContext context1;

        // Get the entry from the cache
        Status status = stub_->get(&context1, getKey, &getRetVal);

        // Unpack the oneof container
        if (status.ok()) {
            switch (getRetVal.ValueMsgOneOf_case()) {
            case ValueMsg::kMessage:
                std::cout << "Entry type is: Message value is: " << getRetVal.message() << std::endl;
                break;
            case ValueMsg::kComplexMessage:
                std::cout << "Entry type is: ComplexMessage value is: " << getRetVal.complexmessage().message()
                        << "," << getRetVal.complexmessage().domainvalue() << std::endl;
                break;
            case ValueMsg::VALUEMSGONEOF_NOT_SET:
                std::cout << "Key type is not set" << std::endl;
                break;
            default:
                std::cout << "Key type is unknown" << std::endl;
                break;
            }
        } else {
            std::cout << status.error_code() << ": " << status.error_message()
                    << std::endl;
        }

    }

private:
    std::unique_ptr<Cache::Stub> stub_;
};

int main(int argc, char** argv) {

    Sample greeter(grpc::CreateChannel(
            "localhost:50051", grpc::InsecureChannelCredentials()));
    greeter.run();
    return 0;
}
