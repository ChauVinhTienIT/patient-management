package com.tiencv.patientservice.grpc;

import billing.BillingRequest;
import billing.BillingResponse;
import billing.BillingServiceGrpc;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;

@Service
public class BillingServiceGrpcClient {
    private final BillingServiceGrpc.BillingServiceBlockingStub blockingStub;
    private static final Logger log = org.slf4j.LoggerFactory.getLogger(BillingServiceGrpcClient.class);

    public BillingServiceGrpcClient (
            @Value("${billing.service.address:localhost}") String serverAddress,
            @Value("${billing.service.grpc.port:9001}") int serverPort
    ) {
        log.info("Connecting to billing service via Grpc at {}:{}", serverAddress, serverPort);

        ManagedChannel channel = ManagedChannelBuilder.forAddress(serverAddress, serverPort)
                .usePlaintext()
                .build();

        this.blockingStub = BillingServiceGrpc.newBlockingStub(channel);
    }

    public BillingResponse createBillingAccount(
            String patientId,
            String patientName,
            String patientEmail
    ) {
        BillingRequest request = BillingRequest.newBuilder()
                .setPatientId(patientId)
                .setPatientName(patientName)
                .setPatientEmail(patientEmail)
                .build();

        BillingResponse response = this.blockingStub.createBillingAccount(request);
        log.info("Received response from billing service via Grpc: {}", response);

        return response;
    }
}
