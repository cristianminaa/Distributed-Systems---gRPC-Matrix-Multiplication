package com.example.grpc.client.grpcclient;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;

@SpringBootApplication
public class GrpcClientApplication extends SpringBootServletInitializer {

	public static void main(String[] args) {
		SpringApplication.run(GrpcClientApplication.class, args);
		/*
		 * ManagedChannel channel = ManagedChannelBuilder.forAddress("localhost", 9090)
		 * .usePlaintext()
		 * .build();
		 * 
		 * HelloServiceGrpc.HelloServiceBlockingStub stub
		 * = HelloServiceGrpc.newBlockingStub(channel);
		 * 
		 * HelloResponse helloResponse = stub.hello(HelloRequest.newBuilder()
		 * .setFirstName("Baeldung")
		 * .setLastName("gRPC")
		 * .build());
		 * 
		 * System.out.println(helloResponse);
		 * 
		 * channel.shutdown();
		 */

		/*
		 * ManagedChannel channel = ManagedChannelBuilder.forAddress("localhost", 8080)
		 * .usePlaintext()
		 * .build();
		 * 
		 * HelloServiceGrpc.HelloServiceBlockingStub stub
		 * = HelloServiceGrpc.newBlockingStub(channel);
		 * 
		 * HelloResponse helloResponse = stub.hello(HelloRequest.newBuilder()
		 * .setFirstName("Baeldung")
		 * .setLastName("gRPC")
		 * .build());
		 * 
		 * channel.shutdown();
		 */
		// SpringApplication.run(GrpcClientApplication.class, args);
	}

}
