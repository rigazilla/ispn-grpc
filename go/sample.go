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

package main

import (
	"log"
	"time"

	"golang.org/x/net/context"
	"google.golang.org/grpc"
	pi "ispn"
)

const (
	address     = "localhost:50051"
	defaultName = "world"
)

func main() {
	// Set up a connection to the server.
	conn, err := grpc.Dial(address, grpc.WithInsecure())
	if err != nil {
		log.Fatalf("did not connect: %v", err)
	}
	defer conn.Close()
    ispnClient := pi.NewCacheClient(conn);
	ctx, cancel := context.WithTimeout(context.Background(), time.Second)
	defer cancel()

    // put k1,v1 entry into the cache
    key := &pi.KeyMsg{KeyMsgOneOf : &pi.KeyMsg_Name{"k1"}};
    value := &pi.ValueMsg{ValueMsgOneOf : &pi.ValueMsg_Message{"v1"}};
    pair := &pi.KeyValuePairMsg{ Key : key, Value : value };
    _, errPut:= ispnClient.Put(ctx, pair);
	if errPut != nil {
		log.Fatalf("Error in calling put: %v", errPut)
	}

    getKey := &pi.KeyMsg{KeyMsgOneOf : &pi.KeyMsg_Name{"k1"}};
    getRetValue, errGetRetValue:= ispnClient.Get(ctx, getKey);
	    if errGetRetValue != nil {
	        log.Fatalf("Error in calling put: %v", errGetRetValue)
	}
    switch x := getRetValue.ValueMsgOneOf.(type) {
        case *pi.ValueMsg_Message:
            log.Printf("%s - Entry[type=Message, value=%s]", getKey, x.Message)
        case *pi.ValueMsg_ComplexMessage:
            log.Printf("%s - Entry[type=ComplexValue, value=%s,%f]", getKey, x.ComplexMessage.Message,x.ComplexMessage.DomainValue);
        default:
            log.Printf("Unknown value (null?)");
    }
}
