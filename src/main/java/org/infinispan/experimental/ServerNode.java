package org.infinispan.experimental;

import java.io.IOException;

import org.infinispan.Cache;
import org.infinispan.grpc.KeyMsg;
import org.infinispan.grpc.KeyValuePairMsg;
import org.infinispan.grpc.ValueMsg;
import org.infinispan.manager.CacheContainer;
import org.infinispan.manager.DefaultCacheManager;
import org.infinispan.manager.EmbeddedCacheManager;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;

public class ServerNode extends org.infinispan.grpc.CacheGrpc.CacheImplBase
{
   @Override
   public void put(KeyValuePairMsg request, StreamObserver<ValueMsg> responseObserver) {
      // TODO Auto-generated method stub
      Object obj = cache.put(request.getKey(), request.getValue());
      ValueMsg vmVal = obj!=null ? ValueMsg.newBuilder((ValueMsg)obj).build() : ValueMsg.newBuilder().setMessage("null").build();
      responseObserver.onNext(vmVal);
      responseObserver.onCompleted();
   }


   private static EmbeddedCacheManager cacheManager;
   private static Cache<Object,Object> cache;

   @Override
   public void get(KeyMsg request, StreamObserver<ValueMsg> responseObserver) {
      Object oVal = cache.get(request);
      ValueMsg vmVal = ValueMsg.newBuilder((ValueMsg)oVal).build();
      responseObserver.onNext(vmVal);
      responseObserver.onCompleted();
   }

   private Server server;

   private void start() throws IOException {
     /* The port on which the server should run */
     int port = 50051;
     server = ServerBuilder.forPort(port)
         .addService(new ServerNode())
         .build()
         .start();
     Runtime.getRuntime().addShutdownHook(new Thread() {
       @Override
       public void run() {
         // Use stderr here since the logger may have been reset by its JVM shutdown hook.
         System.err.println("*** shutting down gRPC server since JVM is shutting down");
         ServerNode.this.stop();
         System.err.println("*** server shut down");
       }
     });
   }

   private void stop() {
     if (server != null) {
       server.shutdown();
     }
   }

   /**
    * Await termination on the main thread since the grpc library uses daemon threads.
    */
   private void blockUntilShutdown() throws InterruptedException {
     if (server != null) {
       server.awaitTermination();
     }
   }

   /**
    * Main launches the server from the command line.
    * @throws InterruptedException 
    */
   public static void main(String[] args) throws IOException, InterruptedException {
      // configuration values
      String xmlFileName="infinispan.xml";
      String nodeName="mycluster";
      String cacheName="local";
      
      System.setProperty("nodeName", nodeName);
      cacheManager = new DefaultCacheManager(xmlFileName);
      cache = cacheManager.getCache(cacheName);
      
      final ServerNode server = new ServerNode();
      server.start();
      server.blockUntilShutdown();
      
      
   }

}
