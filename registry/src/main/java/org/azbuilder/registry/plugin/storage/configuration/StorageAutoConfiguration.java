package org.azbuilder.registry.plugin.storage.configuration;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import lombok.extern.slf4j.Slf4j;
import org.azbuilder.registry.configuration.OpenRegistryProperties;
import org.azbuilder.registry.plugin.storage.StorageService;
import org.azbuilder.registry.plugin.storage.aws.AwsStorageServiceImpl;
import org.azbuilder.registry.plugin.storage.aws.AwsStorageServiceProperties;
import org.azbuilder.registry.plugin.storage.azure.AzureStorageServiceImpl;
import org.azbuilder.registry.plugin.storage.azure.AzureStorageServiceProperties;
import org.azbuilder.registry.plugin.storage.local.LocalStorageServiceImpl;
import org.azbuilder.registry.service.git.GitServiceImpl;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({
        AzureStorageServiceProperties.class,
        StorageProperties.class,
        OpenRegistryProperties.class
})
@ConditionalOnMissingBean(StorageService.class)
@Slf4j
public class StorageAutoConfiguration {

    @Bean
    public StorageService terraformOutput(OpenRegistryProperties openRegistryProperties, StorageProperties storageProperties, AzureStorageServiceProperties azureStorageServiceProperties, AwsStorageServiceProperties awsStorageServiceProperties) {
        StorageService storageService = null;
        log.info("StorageType={}", storageProperties.getType());
        switch (storageProperties.getType()) {
            case AzureStorageImpl:
                BlobServiceClient blobServiceClient = new BlobServiceClientBuilder()
                        .connectionString(
                                String.format("DefaultEndpointsProtocol=https;AccountName=%s;AccountKey=%s;EndpointSuffix=core.windows.net",
                                        azureStorageServiceProperties.getAccountName(),
                                        azureStorageServiceProperties.getAccountKey())
                        ).buildClient();

                storageService = AzureStorageServiceImpl.builder()
                        .blobServiceClient(blobServiceClient)
                        .gitService(new GitServiceImpl())
                        .registryHostname(openRegistryProperties.getHostname())
                        .build();
                break;
            case AwsStorageImpl:

                AWSCredentials credentials = new BasicAWSCredentials(
                        awsStorageServiceProperties.getAccessKey(),
                        awsStorageServiceProperties.getSecretKey()
                );

                AmazonS3 s3client = AmazonS3ClientBuilder
                        .standard()
                        .withCredentials(new AWSStaticCredentialsProvider(credentials))
                        .withRegion(Regions.fromName(awsStorageServiceProperties.getRegion()))
                        .build();

                storageService = AwsStorageServiceImpl.builder()
                        .s3client(s3client)
                        .gitService(new GitServiceImpl())
                        .bucketName(awsStorageServiceProperties.getBucketName())
                        .registryHostname(openRegistryProperties.getHostname())
                        .build();
                break;
            case Local:
                storageService = new LocalStorageServiceImpl();
                break;
            default:
                storageService = null;
        }
        return storageService;
    }
}