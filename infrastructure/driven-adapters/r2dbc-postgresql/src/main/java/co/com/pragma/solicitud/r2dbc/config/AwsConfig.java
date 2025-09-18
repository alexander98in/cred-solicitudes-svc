package co.com.pragma.solicitud.r2dbc.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;

import java.net.URI;

@Configuration
public class AwsConfig {

    @Bean
    public AwsCredentialsProvider awsCredentialsProvider(
            @Value("${aws.access-key}") String accessKey,
            @Value("${aws.secret-key}") String secretKey
    ) {
        return StaticCredentialsProvider.create(
                AwsBasicCredentials.create(accessKey, secretKey)
        );
    }

    @Bean
    public SqsAsyncClient sqsAsyncClient(
            AwsCredentialsProvider credentialsProvider,
            @Value("${aws.region}") String region,
            @Value("${aws.sqs.endpoint:}") String endpoint
    ) {
        var builder = SqsAsyncClient.builder()
                .credentialsProvider(credentialsProvider)
                .region(Region.of(region));

        if (endpoint != null && !endpoint.isBlank()) {
            builder.endpointOverride(URI.create(endpoint));
        }
        return builder.build();
    }
}
