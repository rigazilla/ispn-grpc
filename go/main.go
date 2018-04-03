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
    "fmt"

	"golang.org/x/net/context"
	"google.golang.org/grpc"
	pi "ispn"
)

const (
	address     = "localhost:50051"
	defaultName = "world"
)

func print(prefix string, k *pi.KeyMsg, v *pi.ValueMsg) {
    var keyLog string;
    switch x := k.KeyMsgOneOf.(type) {
        case *pi.KeyMsg_Name:
            keyLog = fmt.Sprintf("Key[type=Name, value=%s]", x.Name)
        case *pi.KeyMsg_ComplexName:
            keyLog = fmt.Sprintf("Key[type=ComplexValue, value=%s,%d]", x.ComplexName.Name,x.ComplexName.DomainId);
        default:
            keyLog = fmt.Sprintf("Unknown value (null?)", prefix);
    }
    switch x := v.ValueMsgOneOf.(type) {
        case *pi.ValueMsg_Message:
            log.Printf("%-16s - %-40s - Entry[type=Message, value=%s]",prefix, keyLog, x.Message)
        case *pi.ValueMsg_ComplexMessage:
            log.Printf("%-16s - %-40s - Entry[type=ComplexValue, value=%s,%f]",prefix, keyLog, x.ComplexMessage.Message,x.ComplexMessage.DomainValue);
        default:
            log.Printf("%-16s - Unknown value (null?)", prefix);
    }
}

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
    keyK1 := &pi.KeyMsg{KeyMsgOneOf : &pi.KeyMsg_Name{"k1"}};
    valV1 := &pi.ValueMsg{ValueMsgOneOf : &pi.ValueMsg_Message{"v1"}};
    pair1 := &pi.KeyValuePairMsg{ Key : keyK1, Value : valV1 };
    r_put1, err_put1:= ispnClient.Put(ctx, pair1);
	if err_put1 != nil {
		log.Fatalf("Error in calling put: %v", err_put1)
	}
    print("first put", keyK1, r_put1);

    // put complex key value (k2,2),(v2,3.14) entry into the cache
    keyK2 :=  &pi.KeyMsg{KeyMsgOneOf: &pi.KeyMsg_ComplexName{ &pi.ComplexKey {"k2",2}}};
    valV2 :=  &pi.ValueMsg{ValueMsgOneOf: &pi.ValueMsg_ComplexMessage{ &pi.ComplexValue {"v2",3.14}}};
    pair2 := &pi.KeyValuePairMsg{ Key : keyK2, Value : valV2 };
    r_put2, err_put2:= ispnClient.Put(ctx, pair2);
	if err_put2 != nil {
		log.Fatalf("Error in calling put: %v", err_put2)
	}
    print("second put", keyK2, r_put2);

    // Create entries with go prefix. Used to test interop with other client
    keyGo_K1 := &pi.KeyMsg{KeyMsgOneOf : &pi.KeyMsg_Name{"go_k1"}};
    valGo_V1 := &pi.ValueMsg{ValueMsgOneOf : &pi.ValueMsg_Message{"go_v1"}};
    pairGo_1 := &pi.KeyValuePairMsg{ Key : keyGo_K1, Value : valGo_V1 };
    r_putGo_1, err_putGo_1:= ispnClient.Put(ctx, pairGo_1);
	if err_putGo_1 != nil {
		log.Fatalf("Error in calling put: %v", err_putGo_1)
	}
    print("first put go:", keyGo_K1, r_putGo_1);

    keyGo_K2 :=  &pi.KeyMsg{KeyMsgOneOf: &pi.KeyMsg_ComplexName{ &pi.ComplexKey {"go_k2",2}}};
    valGo_V2 :=  &pi.ValueMsg{ValueMsgOneOf: &pi.ValueMsg_ComplexMessage{ &pi.ComplexValue {"go_v2",3.14}}};
    pairGo_2 := &pi.KeyValuePairMsg{ Key : keyGo_K2, Value : valGo_V2 };
    r_putGo_2, err_putGo_2:= ispnClient.Put(ctx, pairGo_2);
	if err_putGo_2 != nil {
		log.Fatalf("Error in calling put: %v", err_putGo_2)
	}
    print("second put go:", keyGo_K2, r_putGo_2);

   // Try with a null entry
        key_null := &pi.KeyMsg{};
        r_val_null, err_get_null := ispnClient.Get(ctx, key_null);
	    if err_get_null != nil {
	        log.Fatalf("Error in calling put: %v", err_get_null)
	    }
        print("get null",key_null, r_val_null);

        // Now try to get C++ entries
        keyCpp_K1 := &pi.KeyMsg{KeyMsgOneOf : &pi.KeyMsg_Name{"c++_k1"}};
        r_getCpp_K1, err_getCpp_1:= ispnClient.Get(ctx, keyCpp_K1);
	    if err_getCpp_1 != nil {
	        log.Fatalf("Error in calling put: %v", err_getCpp_1)
	    }
        print("get C++", keyCpp_K1, r_getCpp_K1);

        keyCpp_K2 :=  &pi.KeyMsg{KeyMsgOneOf: &pi.KeyMsg_ComplexName{ &pi.ComplexKey {"c++_k2",2}}};
        r_getCpp_K2, err_getCpp_2:= ispnClient.Get(ctx, keyCpp_K2);
	    if err_getCpp_2 != nil {
	        log.Fatalf("Error in calling put: %v", err_getCpp_2)
	    }
        print("get C++", keyCpp_K2, r_getCpp_K2);

        // Now try to get Java entries
        keyJava_K1 := &pi.KeyMsg{KeyMsgOneOf : &pi.KeyMsg_Name{"java_k1"}};
        r_getJava_K1, err_getJava_1:= ispnClient.Get(ctx, keyJava_K1);
	    if err_getJava_1 != nil {
	        log.Fatalf("Error in calling put: %v", err_getJava_1)
	    }
        print("get Java", keyJava_K1, r_getJava_K1);

        keyJava_K2 :=  &pi.KeyMsg{KeyMsgOneOf: &pi.KeyMsg_ComplexName{ &pi.ComplexKey {"java_k2",2}}};
        r_getJava_K2, err_getJava_2:= ispnClient.Get(ctx, keyJava_K2);
	    if err_getJava_2 != nil {
	        log.Fatalf("Error in calling put: %v", err_getJava_2)
	    }
        print("get Java", keyJava_K2, r_getJava_K2);

}
