package org.infinispan.experimental;

import java.io.IOException;
import java.util.List;

import org.infinispan.Cache;
import org.infinispan.grpc.KeyMsg;
import org.infinispan.grpc.KeyValuePairMsg;
import org.infinispan.grpc.TopologyInfoMsg;
import org.infinispan.grpc.ValueMsg;
import org.infinispan.grpc.VoidMsg;
import org.infinispan.manager.CacheContainer;
import org.infinispan.manager.DefaultCacheManager;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.remoting.transport.Address;

import com.google.protobuf.Message;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.Descriptors.OneofDescriptor;
import com.google.protobuf.Descriptors.FieldDescriptor.JavaType;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;

public class ServerNode extends org.infinispan.grpc.CacheGrpc.CacheImplBase
{
   @Override
   public void put(KeyValuePairMsg request, StreamObserver<ValueMsg> responseObserver) {
      OneofDescriptor ood = (OneofDescriptor) KeyMsg.getDescriptor().getOneofs().toArray()[0];
      FieldDescriptor fd = request.getKey().getOneofFieldDescriptor(ood);
      Object oKey = request.getKey().getField(fd);

      OneofDescriptor oodv = (OneofDescriptor) ValueMsg.getDescriptor().getOneofs().toArray()[0];
      FieldDescriptor fdv = request.getValue().getOneofFieldDescriptor(oodv);
      Object oVal = request.getValue().getField(fdv);

      Object obj = cache.put(oKey, oVal);
      ValueMsg vmVal = setValueMsgOneOf(oodv, obj);
      responseObserver.onNext(vmVal);
      responseObserver.onCompleted();
   }

   @Override
   public void topologyGetInfo(VoidMsg request, StreamObserver<TopologyInfoMsg> responseObserver) {
      List<Address> addressList = cacheManager.getMembers();
      TopologyInfoMsg retVal = TopologyInfoMsg.getDefaultInstance();
      responseObserver.onNext(retVal);
      responseObserver.onCompleted();
   }
   
   public void topologyGetServerList()
   {
   }

   @Override
   public void get(KeyMsg request, StreamObserver<ValueMsg> responseObserver) {
      OneofDescriptor ood = (OneofDescriptor) KeyMsg.getDescriptor().getOneofs().toArray()[0];
      OneofDescriptor oodv = (OneofDescriptor) ValueMsg.getDescriptor().getOneofs().toArray()[0];
      FieldDescriptor fd = request.getOneofFieldDescriptor(ood);
      Object oKey = request.getField(fd);
      Object oVal = cache.get(oKey);
      ValueMsg vmVal = setValueMsgOneOf(oodv, oVal);
      responseObserver.onNext(vmVal);
      responseObserver.onCompleted();
   }

   private ValueMsg setValueMsgOneOf(OneofDescriptor oodv, Object obj) {
      ValueMsg vmVal = null;
      if (obj != null) {
         List<FieldDescriptor> lfd = oodv.getFields();
         for (FieldDescriptor fieldDescriptor : lfd) {
             if (fieldDescriptor.getJavaType() == JavaType.MESSAGE) {
                 Message.Builder b = ValueMsg.newBuilder().getFieldBuilder(fieldDescriptor);
                 if (b.getDefaultInstanceForType().getClass() == obj.getClass()) {
                     vmVal = ValueMsg.newBuilder().setField(fieldDescriptor, obj).build();
                     break;
                  }
             }
             else {
                 if (fieldDescriptor.getDefaultValue().getClass() == obj.getClass()) {
                     vmVal = ValueMsg.newBuilder().setField(fieldDescriptor, obj).build();
                     break;
                 }
             }
         }
      }
      if (vmVal == null) {
        vmVal = ValueMsg.newBuilder().build();
      }
      return vmVal;
   }

   private Server server;
   private static EmbeddedCacheManager cacheManager;
   private static Cache<Object,Object> cache;

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
