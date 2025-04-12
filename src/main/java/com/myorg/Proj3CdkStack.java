package com.myorg;

import java.util.List;
import java.util.Map;

import software.amazon.awscdk.RemovalPolicy;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.apigateway.IntegrationResponse;
import software.amazon.awscdk.services.apigateway.LambdaIntegration;
import software.amazon.awscdk.services.apigateway.MethodOptions;
import software.amazon.awscdk.services.apigateway.MethodResponse;
import software.amazon.awscdk.services.apigateway.MockIntegration;
import software.amazon.awscdk.services.apigateway.PassthroughBehavior;
import software.amazon.awscdk.services.apigateway.Resource;
import software.amazon.awscdk.services.apigateway.RestApi;
import software.amazon.awscdk.services.lambda.Code;
import software.amazon.awscdk.services.lambda.Function;
import software.amazon.awscdk.services.lambda.Runtime;
import software.amazon.awscdk.services.s3.Bucket;
import software.constructs.Construct;

public class Proj3CdkStack extends Stack {
    public Proj3CdkStack(final Construct scope, final String id) {
        this(scope, id, null);
    }

    public Proj3CdkStack(final Construct scope, final String id, final StackProps props) {
        super(scope, id, props);

        // 1. S3 Bucket
        Bucket bucket = Bucket.Builder.create(this, "Proj3ImagesBucket")
            .bucketName("proj3-images-varun123") // must be globally unique
            .removalPolicy(RemovalPolicy.DESTROY)
            .autoDeleteObjects(true)
            .build();

        // 2. Lambda Function
        Function uploadLambda = Function.Builder.create(this, "Proj3UploadLambda")
            .runtime(Runtime.PYTHON_3_12)
            .code(Code.fromAsset("lambda"))
            .handler("lambda_function.lambda_handler") // filename.function_name
            .environment(Map.of(
                 "BUCKET_NAME", bucket.getBucketName()
            ))
            .build();


        // 3. Permissions: Lambda can read/write to S3
        bucket.grantReadWrite(uploadLambda);

        // 4. API Gateway
        RestApi api = RestApi.Builder.create(this, "Proj3Api")
            .restApiName("Proj3 Service")
            .build();

        Resource uploadResource = api.getRoot().addResource("upload");

        // POST /upload → Lambda
        uploadResource.addMethod("POST", new LambdaIntegration(uploadLambda));

        // 5. CORS for /upload
        uploadResource.addMethod("OPTIONS", MockIntegration.Builder.create()
            .integrationResponses(List.of(
                IntegrationResponse.builder()
                    .statusCode("200")
                    .responseParameters(Map.of(
                        "method.response.header.Access-Control-Allow-Headers", "'Content-Type'",
                        "method.response.header.Access-Control-Allow-Origin", "'*'",
                        "method.response.header.Access-Control-Allow-Methods", "'OPTIONS,POST'"
                    ))
                    .build()))
            .passthroughBehavior(PassthroughBehavior.NEVER)
            .requestTemplates(Map.of("application/json", "{\"statusCode\": 200}"))
            .build(),
            MethodOptions.builder()
                .methodResponses(List.of(MethodResponse.builder()
                    .statusCode("200")
                    .responseParameters(Map.of(
                        "method.response.header.Access-Control-Allow-Headers", true,
                        "method.response.header.Access-Control-Allow-Origin", true,
                        "method.response.header.Access-Control-Allow-Methods", true
                    ))
                    .build()))
                .build());
    }
}
