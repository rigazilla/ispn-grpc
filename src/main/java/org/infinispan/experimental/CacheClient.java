package org.infinispan.experimental;

import java.util.concurrent.TimeUnit;

import org.infinispan.grpc.CacheGrpc;
import org.infinispan.grpc.KeyMsg;
import org.infinispan.grpc.KeyValuePairMsg;
import org.infinispan.grpc.ValueMsg;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;

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
   
   public static void main(String[] args) throws InterruptedException {
      CacheClient client = new CacheClient("localhost", 50051);
      try {
        /* Access a service running on the local machine on port 50051 */
        String user = "world";
        if (args.length > 0) {
          user = args[0]; /* Use the arg as the name to greet if provided */
        }
        ValueMsg value= ValueMsg.newBuilder().setMessage("v1").build();
        KeyMsg key= KeyMsg.newBuilder().setName("k1").build();
        KeyValuePairMsg pair = KeyValuePairMsg.newBuilder().setValue(value).setKey(key).build();
        client.put(pair);
        ValueMsg retVal = client.get(key);
        System.out.println("Value "+retVal.getMessage()+" for key "+key.getName());
      } finally {
        client.shutdown();
      }

   }

}
