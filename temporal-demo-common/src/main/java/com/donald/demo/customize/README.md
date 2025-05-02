# Additional info on setup for private link.

Some example code for Java worker to connect using apikeys.

```
// Create a Metadata object with the Temporal namespace header key.
Metadata.Key<String> TEMPORAL_NAMESPACE_HEADER_KEY = Metadata.Key.of("temporal-namespace", Metadata.ASCII_STRING_MARSHALLER);
Metadata metadata = new Metadata();
metadata.put(TEMPORAL_NAMESPACE_HEADER_KEY, "payroll-dev.hke7h");

WorkflowServiceStubsOptions.Builder stubOptions =
WorkflowServiceStubsOptions.newBuilder()
      .setChannelInitializer(
                  (channel) -> {
                                channel.intercept(MetadataUtils.newAttachHeadersInterceptor(metadata));
                                })
      .addGrpcMetadataProvider(
                  new AuthorizationGrpcMetadataProvider(
                               () ->  "Bearer " + "APIKEY"))
      .setTarget("eu-central-1.aws.api.temporal.io:7233");
try {
     stubOptions.setSslContext(
     SimpleSslContextBuilder.noKeyOrCertChain().setUseInsecureTrustManager(false).build());
     } catch (SSLException e) {
        throw new RuntimeException(e);
                }
WorkflowServiceStubs service = WorkflowServiceStubs.newServiceStubs(stubOptions.build());

```