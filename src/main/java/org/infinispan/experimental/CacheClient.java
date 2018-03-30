package org.infinispan.experimental;

import java.util.concurrent.TimeUnit;


import org.infinispan.grpc.CacheGrpc;
import org.infinispan.grpc.KeyMsg;
import org.infinispan.grpc.ComplexKey;
import org.infinispan.grpc.ComplexValue;
import org.infinispan.grpc.KeyValuePairMsg;
import org.infinispan.grpc.TopologyInfoMsg;
import org.infinispan.grpc.ValueMsg;
import org.infinispan.grpc.VoidMsg;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

public class CacheClient {

   private final ManagedChannel channel;
   private final CacheGrpc.CacheBlockingStub blockingStub;


   /** Construct client connecting to HelloWorld server at {@code host:port}. */
   public CacheClient(String host, int port) {
     this(ManagedChannelBuilder.forAddress(host, port)
         // Channels are secure by default (via SSL/TLS). For the example we disable TLS to avoid
         // needing certificates.
         .usePlaintext(true)
         .build());
   }
   /** Construct client for accessing RouteGuide server using the existing channel. */
   CacheClient(ManagedChannel channel) {
     this.channel = channel;
     blockingStub = CacheGrpc.newBlockingStub(channel);
   }
   
   public void shutdown() throws InterruptedException {
      channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
   }
   
   public ValueMsg get(KeyMsg key) {
     ValueMsg response;
       response = blockingStub.get(key);
       return response;
   }

   public ValueMsg put(KeyValuePairMsg pair)
   {
      ValueMsg response;
      response = blockingStub.put(pair);
      return response;
   }

   public TopologyInfoMsg topologyGetInfo()
   {
      VoidMsg request = VoidMsg.getDefaultInstance();
      return blockingStub.topologyGetInfo(request);
   }
   
   public static void main(String[] args) throws InterruptedException {
      CacheClient client = new CacheClient("localhost", 50051);
      try {
        /* Access a service running on the local machine on port 50051 */

        ValueMsg value= ValueMsg.newBuilder().setMessage("v1").build();
        KeyMsg key= KeyMsg.newBuilder().setName("k1").build();
        KeyValuePairMsg pair = KeyValuePairMsg.newBuilder().setValue(value).setKey(key).build();
        client.put(pair);
        
        ComplexKey complexKey = ComplexKey.newBuilder().setDomainId(2).setName("k2").build();
        KeyMsg key2 = KeyMsg.newBuilder().setComplexName(complexKey).build();
        
        ComplexValue complexValue = ComplexValue.newBuilder().setMessage("v2").setDomainValue(3.14f).build();
        ValueMsg value2 = ValueMsg.newBuilder().setComplexMessage(complexValue).build();
        KeyValuePairMsg pair2 = KeyValuePairMsg.newBuilder().setValue(value2).setKey(key2).build();
        client.put(pair2);

        // Create entries with java prefix. Used to test interop with other client
        ValueMsg j_value= ValueMsg.newBuilder().setMessage("java_v1").build();
        KeyMsg j_key= KeyMsg.newBuilder().setName("java_k1").build();
        KeyValuePairMsg j_pair = KeyValuePairMsg.newBuilder().setValue(j_value).setKey(j_key).build();
        client.put(j_pair);
        
        ComplexKey j_complexKey = ComplexKey.newBuilder().setDomainId(2).setName("java_k2").build();
        KeyMsg j_key2 = KeyMsg.newBuilder().setComplexName(j_complexKey).build();
        
        ComplexValue j_complexValue = ComplexValue.newBuilder().setMessage("java_v2").setDomainValue(3.14f).build();
        ValueMsg j_value2 = ValueMsg.newBuilder().setComplexMessage(j_complexValue).build();
        KeyValuePairMsg j_pair2 = KeyValuePairMsg.newBuilder().setValue(j_value2).setKey(j_key2).build();
        client.put(j_pair2);

        
        printValue(client, key);
        
        // TODO With the topology info client must setup an appropriate routing policy
        TopologyInfoMsg topologyInfo = client.topologyGetInfo();
        printValue(client, key2);
        
        // Try with a null entry
        KeyMsg key_null= KeyMsg.newBuilder().setName("k3").build();
        printValue(client, key_null);

        // Now try to get C++ entries
        KeyMsg cpp_key= KeyMsg.newBuilder().setName("c++_k1").build();
        printValue(client, cpp_key);

        ComplexKey cpp_complexKey = ComplexKey.newBuilder().setDomainId(2).setName("c++_k2").build();
        KeyMsg cpp_key2 = KeyMsg.newBuilder().setComplexName(cpp_complexKey).build();
        ValueMsg cpp_retVal2 = client.get(cpp_key2);
        printValue(client, cpp_key2);
        
        
      } finally {
        client.shutdown();
      }

   }
   private static void printValue(CacheClient client, KeyMsg cpp_key) {
      // TODO fill all the cases
      System.out.println("printValue: BEGIN");
      switch (cpp_key.getKeyMsgOneOfCase()) {
         case COMPLEXNAME:
            System.out.println("Key type is: ComplexName, value is: "+cpp_key.getComplexName().getName()+","+ cpp_key.getComplexName().getDomainId());
            break;
         case KEYMSGONEOF_NOT_SET:
            System.out.println("Key type is not set");
            break;
         case NAME:
            System.out.println("Key type is: Name, value is "+cpp_key.getName());
            break;
         default:
            System.out.println("Key type unknown");
            break;
         
      }
      ValueMsg cpp_retVal = client.get(cpp_key);
      // TODO fill all the cases
      switch (cpp_retVal.getValueMsgOneOfCase()) {
         case COMPLEXMESSAGE:
            System.out.println("Entry type is: ComplexMessage, value is: "+cpp_retVal.getComplexMessage().getMessage()+","+ cpp_retVal.getComplexMessage().getDomainValue());
            break;
         case MESSAGE:
            System.out.println("Entry type is: Message "+cpp_retVal.getMessage()+" for key "+cpp_key.getName());
            break;
         case VALUEMSGONEOF_NOT_SET:
            System.out.println("Entry type is not set");
            break;
         default:
            System.out.println("Entry type unknown");
            break;
      }
      System.out.println("printValue: END");
      
   }

}
