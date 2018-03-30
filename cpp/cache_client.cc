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
    :
            stub_(Cache::NewStub(channel)) {
    }

    // Assembles the client's payload, sends it and presents the response back
    // from the server.
    void putValues() {

        {
            KeyValuePairMsg pair1;
            pair1.mutable_key()->set_name("k1");
            pair1.mutable_value()->set_message("v1");

            ClientContext *context = new ClientContext();
            ValueMsg reply1;
            stub_->put(context, pair1, &reply1);

            KeyValuePairMsg pair2;

            KeyMsg *kMsg = pair2.mutable_key();
            ValueMsg *kValue = pair2.mutable_value();
            ComplexKey* complexKey = kMsg->mutable_complexname();
            complexKey->set_domainid(2);
            complexKey->set_name("k2");

            ComplexValue* complexValue = kValue->mutable_complexmessage();
            complexValue->set_message("v2");
            complexValue->set_domainvalue(3.14);

            context = new ClientContext();
            ValueMsg reply2;
            stub_->put(context, pair2, &reply2);
        }

        // Create entries with c++ prefix. Used to test interop with other client
        {
            KeyValuePairMsg pair1;
            pair1.mutable_key()->set_name("c++_k1");
            pair1.mutable_value()->set_message("c++_v1");

            ClientContext *context = new ClientContext();
            ValueMsg reply1;
            stub_->put(context, pair1, &reply1);

            KeyValuePairMsg pair2;

            KeyMsg *kMsg = pair2.mutable_key();
            ValueMsg *kValue = pair2.mutable_value();
            ComplexKey* complexKey = kMsg->mutable_complexname();
            complexKey->set_domainid(2);
            complexKey->set_name("c++_k2");

            ComplexValue* complexValue = kValue->mutable_complexmessage();
            complexValue->set_message("c++_v2");
            complexValue->set_domainvalue(3.14);

            context = new ClientContext();
            ValueMsg reply2;
            stub_->put(context, pair2, &reply2);
        }

    }

    void printValue(KeyMsg key)
            {
        std::cout << "printValue: BEGIN" << std::endl;
        ValueMsg replyget;
        switch (key.KeyMsgOneOf_case())
        {
        case KeyMsg::kName:
            std::cout << "Key type is: Name value is: " << key.name() << std::endl;
            break;
        case KeyMsg::kComplexName:
            std::cout << "Key type is: ComplexName value is: " << key.complexname().name()
                    << "," << key.complexname().domainid() << std::endl;
            break;
        case KeyMsg::KEYMSGONEOF_NOT_SET:
            std::cout << "Key type is not set" << std::endl;
            break;
        default:
            std::cout << "Key type is unknown" << std::endl;
            break;
        }

        ClientContext context;
        Status status = stub_->get(&context, key, &replyget);

        if (status.ok()) {
            switch (replyget.ValueMsgOneOf_case()) {
            case ValueMsg::kMessage:
                std::cout << "Entry type is: Message value is: " << replyget.message() << std::endl;
                break;
            case ValueMsg::kComplexMessage:
                std::cout << "Entry type is: ComplexMessage value is: " << replyget.complexmessage().message()
                        << "," << replyget.complexmessage().domainvalue() << std::endl;
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
        std::cout << "printValue: END" << std::endl;
    }

private:
    std::unique_ptr<Cache::Stub> stub_;

    // Context for the client. It could be used to convey extra information to
    // the server and/or tweak certain RPC behaviors.
};

int main(int argc, char** argv) {
    // Instantiate the client. It requires a channel, out of which the actual RPCs
    // are created. This channel models a connection to an endpoint (in this case,
    // localhost at port 50051). We indicate that the channel isn't authenticated
    // (use of InsecureChannelCredentials()).
    CacheClient greeter(grpc::CreateChannel(
            "localhost:50051", grpc::InsecureChannelCredentials()));
    greeter.putValues();

    KeyMsg key;
    key.set_name("k1");

    greeter.printValue(key);

    key.mutable_complexname()->set_name("k2");
    key.mutable_complexname()->set_domainid(2);
    greeter.printValue(key);

    key.set_name("k1");
    greeter.printValue(key);

    // Try with a null entry
    key.set_name("k3");
    greeter.printValue(key);

    // Try with java entries

    key.set_name("java_k1");
    greeter.printValue(key);

    key.mutable_complexname()->set_name("java_k2");
    key.mutable_complexname()->set_domainid(2);
    greeter.printValue(key);

    return 0;
}
