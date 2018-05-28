package org.infinispan.experimental;

import java.util.concurrent.TimeUnit;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

import ispn.*;

public class Sample {

   private final ManagedChannel channel;
   private final CacheGrpc.CacheBlockingStub blockingStub;


   /** Construct client connecting to HelloWorld server at {@code host:port}. */
   public Sample(String host, int port) {
     this(ManagedChannelBuilder.forAddress(host, port)
         // Channels are secure by default (via SSL/TLS). For the example we disable TLS to avoid
         // needing certificates.
         .usePlaintext(true)
         .build());
   }
   /** Construct client for accessing RouteGuide server using the existing channel. */
   Sample(ManagedChannel channel) {
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
      Sample client = new Sample("localhost", 50051);
      try {

        // Build a key/value for put
        ValueMsg value= ValueMsg.newBuilder().setMessage("v1").build();
        KeyMsg key= KeyMsg.newBuilder().setName("k1").build();        
        KeyValuePairMsg pair = KeyValuePairMsg.newBuilder().setValue(value).setKey(key).build();
        
        // Put the entry in the cache
        client.put(pair);
        
        // Build a key for get
        KeyMsg key_get= KeyMsg.newBuilder().setName("k1").build();
        
        // Get the entry from the cache
        ValueMsg retVal = client.get(key_get);
        
        // Unpack the oneof container
        switch (retVal.getValueMsgOneOfCase()) {
           case COMPLEXMESSAGE:
              System.out.println("Entry type is: ComplexMessage, value is: "+retVal.getComplexMessage().getMessage()+","+ retVal.getComplexMessage().getDomainValue());
              break;
           case MESSAGE:
              System.out.println("Entry type is: Message "+retVal.getMessage()+" for key "+key.getName());
              break;
           case VALUEMSGONEOF_NOT_SET:
              System.out.println("Entry type is not set");
              break;
           default:
              System.out.println("Entry type unknown");
              break;
        }
        
        
      } finally {
        client.shutdown();
      }

   }

}
