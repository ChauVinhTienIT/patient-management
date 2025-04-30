package com.tiencv.stack;

import software.amazon.awscdk.*;
import software.amazon.awscdk.services.ec2.*;
import software.amazon.awscdk.services.ec2.InstanceType;
import software.amazon.awscdk.services.ecs.*;
import software.amazon.awscdk.services.ecs.Protocol;
import software.amazon.awscdk.services.ecs.patterns.ApplicationLoadBalancedFargateService;
import software.amazon.awscdk.services.logs.LogGroup;
import software.amazon.awscdk.services.logs.RetentionDays;
import software.amazon.awscdk.services.msk.CfnCluster;
import software.amazon.awscdk.services.rds.*;
import software.amazon.awscdk.services.route53.CfnHealthCheck;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


public class LocalStack extends Stack {
    private final Vpc vpc;
    private final Cluster ecsCluster;

    public LocalStack(final App scope, final String id, final StackProps props) {
        super(scope, id, props);
        // Create a VPC for the application
        this.vpc = creatVpc();

        // Create a database instance for the AuthService
        DatabaseInstance authServiceDb = createDatabaseInstance("AuthServiceDb", "auth_service_db");

        // Create a database instance for the PatientService
        DatabaseInstance patientServiceDb = createDatabaseInstance("PatientServiceDb", "patient_service_db");

        // Create a health check for the AuthService database
        CfnHealthCheck authServiceDbHealthCheck = createDbHealthCheck(authServiceDb, "AuthServiceDbHealthCheck");

        // Create a health check for the PatientService database
        CfnHealthCheck patientServiceDbHealthCheck = createDbHealthCheck(patientServiceDb, "PatientServiceDbHealthCheck");

        // Create a Kafka cluster
        CfnCluster mskCluster = createMskCluster();

        // Create an ECS cluster
        this.ecsCluster = createEcsCluster();

        // Create a Fargate service for the AuthService
        FargateService authService = createFargateService(
                "AuthService",
                "auth-service",
                List.of(4005),
                authServiceDb,
                Map.of("JWT_SECRET", "3db265dfa5f2b1be2188ecfaf4d01eece12c4e1bf7eaab4bb46219e118c2882a83136ad1e41891086269c300cbe491a3f50c7c223a69379c5e9456feb12d387a")
        );

        authService.getNode().addDependency(authServiceDbHealthCheck);
        authService.getNode().addDependency(authServiceDb);

        // Create a Fargate service for the BillingService
        FargateService  billingService = createFargateService(
                "BillingService",
                "billing-service",
                List.of(4001, 9001),
                null,
                null
        );

        // Create a Fargate service for the AnalysisService
        FargateService analysisService = createFargateService(
                "AnalysisService",
                "analysis-service",
                List.of(4002),
                null,
                null
        );

        analysisService.getNode().addDependency(mskCluster);

        // Create a Fargate service for the PatientService
        FargateService patientService = createFargateService(
                "PatientService",
                "patient-service",
                List.of(4003),
                patientServiceDb,
                Map.of(
                        "BILLING_SERVICE_ADDRESS", "host.docker.internal",
                        "BILLING_SERVICE_GRPC_PORT", "9001"
                )
        );

        patientService.getNode().addDependency(patientServiceDbHealthCheck);
        patientService.getNode().addDependency(patientServiceDb);
        patientService.getNode().addDependency(billingService);
        patientService.getNode().addDependency(mskCluster);

        // Create a Fargate service for the ApiGateway
        createApiGatewayService();
    }

    private Vpc creatVpc() {
        return Vpc.Builder
                .create(this, "PatientManagementVpc")
                .vpcName("PatientManagementVpc")
                .maxAzs(2) // Default is all AZs in region
                .build();
    }

    private DatabaseInstance createDatabaseInstance(String id, String dbName) {
        return DatabaseInstance.Builder
                .create(this, id)
                .engine(
                    DatabaseInstanceEngine.postgres(
                        PostgresInstanceEngineProps.builder()
                                .version(PostgresEngineVersion.VER_17_2)
                                .build()
                    )
                )
                .vpc(this.vpc)
                .instanceType(InstanceType.of(InstanceClass.BURSTABLE2, InstanceSize.MICRO))
                .allocatedStorage(20)
                .credentials(Credentials.fromGeneratedSecret("admin_user"))
                .databaseName(dbName)
                .removalPolicy(RemovalPolicy.DESTROY)
                .build();
    }

    private CfnHealthCheck createDbHealthCheck(DatabaseInstance dbInstance, String id) {
        return CfnHealthCheck.Builder
                .create(this, id)
                .healthCheckConfig(
                    CfnHealthCheck.HealthCheckConfigProperty.builder()
                            .type("HTTP")
                            .port(Token.asNumber(dbInstance.getDbInstanceEndpointPort()))
                            .ipAddress(dbInstance.getDbInstanceEndpointAddress())
                            .requestInterval(30)  // Check every 30 seconds
                            .failureThreshold(3)  // Fail after 3 consecutive failures
                            .build()
                )
                .build();
    }

    private CfnCluster createMskCluster() {
        return CfnCluster.Builder
                .create(this, "MskCluster")
                .clusterName("kafka-cluster")
                .kafkaVersion("2.8.0")
                .numberOfBrokerNodes(1)
                .brokerNodeGroupInfo(
                    CfnCluster.BrokerNodeGroupInfoProperty.builder()
                            .instanceType("kafka.m5.large")
                            .clientSubnets(
                                this.vpc.getPrivateSubnets().stream()
                                    .map(ISubnet::getSubnetId)
                                    .collect(Collectors.toList())
                            )
                            .brokerAzDistribution("DEFAULT")
                            .build()
                )
                .build();
    }

    private Cluster createEcsCluster() {
        return Cluster.Builder
                .create(this, "PatientManagementCluster")
                .clusterName("PatientManagementCluster")
                .vpc(this.vpc)
                .defaultCloudMapNamespace(
                    CloudMapNamespaceOptions.builder()
                            .name("patient-management.local")
                            .build()
                )
                .build();
    }

    private FargateService createFargateService(
            String id,
            String imageName,
            List<Integer> ports,
            DatabaseInstance dbInstance,
            Map<String, String> additionalEnvVars
    ) {
        FargateTaskDefinition taskDefinition = FargateTaskDefinition.Builder
                .create(this, id + "Task")
                .memoryLimitMiB(512)
                .cpu(256)
                .build();

        ContainerDefinitionOptions.Builder containerOptions = ContainerDefinitionOptions.builder()
                .image(ContainerImage.fromRegistry(imageName))
                .portMappings(
                    ports.stream()
                            .map(port -> PortMapping.builder()
                                    .containerPort(port)
                                    .hostPort(port)
                                    .protocol(Protocol.TCP)
                                    .build())
                            .toList()
                )
                .logging(
                    LogDriver.awsLogs(
                        AwsLogDriverProps.builder()
                                .logGroup(
                                    LogGroup.Builder.create(this, id + "LogGroup")
                                            .logGroupName("/ecs/" + imageName)
                                            .removalPolicy(RemovalPolicy.DESTROY)
                                            .retention(RetentionDays.ONE_DAY)
                                            .build())
                                .streamPrefix(imageName)
                                .build()
                    )
                );

        Map<String, String> envVars = new HashMap<>();
        envVars.put("SPRING_KAFKA_BOOTSTRAP_SERVERS", "localhost:localstack:4510, localhost:localstack:4511, localhost:localstack:4512");

        if(additionalEnvVars != null) {
            envVars.putAll(additionalEnvVars);
        }

        if(dbInstance != null) {
            envVars.put("SPRING_DATASOURCE_URL", "jdbc:postgresql://%s:%s/%s-db"
                    .formatted(
                            dbInstance.getDbInstanceEndpointAddress(),
                            dbInstance.getDbInstanceEndpointPort(),
                            imageName
                    ));
        }
        envVars.put("SPRING_DATASOURCE_USERNAME", "admin_user");

        if (dbInstance != null) {
            envVars.put("SPRING_DATASOURCE_PASSWORD", dbInstance.getSecret().secretValueFromJson("password").toString());
        }

        envVars.put("SPRING_JPA_HIBERNATE_DDL_AUTO", "update");
        envVars.put("SPRING_SQL_INIT_MODE", "always");
        envVars.put("SPRING_DATASOURCE_HIKARI_INITIALIZATION_FAIL_TIMEOUT", "60000");

        containerOptions.environment(envVars);

        taskDefinition.addContainer(
                imageName + "Container",
                containerOptions.build()
        );

        return FargateService.Builder
                .create(this, id)
                .cluster(this.ecsCluster)
                .taskDefinition(taskDefinition)
                .assignPublicIp(false)
                .serviceName(imageName)
                .build();
    }

    private void createApiGatewayService() {
        FargateTaskDefinition taskDefinition = FargateTaskDefinition.Builder
                .create(this, "ApiGatewayTaskDefinition")
                .memoryLimitMiB(512)
                .cpu(256)
                .build();

        ContainerDefinitionOptions containerOptions = ContainerDefinitionOptions.builder()
                .image(ContainerImage.fromRegistry("api-gateway"))
                .environment(Map.of(
                        "SPRING_PROFILES_ACTIVE", "prod",
                        "AUTH_SERVICE_URL", "http://host.docker.internal:4005"
                ))
                .portMappings(
                        List.of(4004).stream()
                                .map(port -> PortMapping.builder()
                                        .containerPort(port)
                                        .hostPort(port)
                                        .protocol(Protocol.TCP)
                                        .build())
                                .toList()
                )
                .logging(
                        LogDriver.awsLogs(
                                AwsLogDriverProps.builder()
                                        .logGroup(
                                                LogGroup.Builder.create(this, "ApiGatewayLogGroup")
                                                        .logGroupName("/ecs/api-gateway")
                                                        .removalPolicy(RemovalPolicy.DESTROY)
                                                        .retention(RetentionDays.ONE_DAY)
                                                        .build())
                                        .streamPrefix("api-gateway")
                                        .build()
                        )
                )
                .build();

        taskDefinition.addContainer("ApiGatewayContainer", containerOptions);

        ApplicationLoadBalancedFargateService apiGateway = ApplicationLoadBalancedFargateService.Builder
                .create(this, "ApiGatewayService")
                .cluster(this.ecsCluster)
                .serviceName("api-gateway")
                .taskDefinition(taskDefinition)
                .desiredCount(1)
                .healthCheckGracePeriod(Duration.seconds(60)) // Wait for 60 seconds before checking health
                .build();
    }

    public static void main(final String[] args) {
        App app = new App(AppProps.builder().outdir("./cdk.out").build());

        StackProps props = StackProps.builder()
                .synthesizer(new BootstraplessSynthesizer())
                .build();

        new LocalStack(app, "localstack", props);
        app.synth();

        System.out.println("App synthesizing in progress...");
    }
}
