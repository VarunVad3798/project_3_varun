package com.myorg;

import software.amazon.awscdk.App;
import software.amazon.awscdk.Environment;
import software.amazon.awscdk.StackProps;

public class Proj3CdkApp {
    public static void main(final String[] args) {
        App app = new App();

        new Proj3CdkStack(app, "Proj3CdkStack", StackProps.builder()
            .env(Environment.builder()
                .account("481665130116")   // ✅ Your AWS Account ID
                .region("us-east-2")       // ✅ Your region (same used in GitHub secrets)
                .build())
            .build());

        app.synth();
    }
}
