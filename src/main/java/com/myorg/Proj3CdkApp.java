package com.myorg;

import software.amazon.awscdk.App;
import software.amazon.awscdk.Environment;
import software.amazon.awscdk.StackProps;

public class Proj3CdkApp {
    public static void main(final String[] args) {
        App app = new App();

        String account = System.getenv("CDK_DEFAULT_ACCOUNT");
        String region = System.getenv("CDK_DEFAULT_REGION");

        new Proj3CdkStack(app, "Proj3CdkStack", StackProps.builder()
            .env(Environment.builder()
                .account(account)
                .region(region)
                .build())
            .build());

        app.synth();
    }
}
