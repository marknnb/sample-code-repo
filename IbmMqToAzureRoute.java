@Component
public class IbmMqToAzureRoute extends RouteBuilder {

  private final IncomingMessageProcessor incomingProcessor;

  public IbmMqToAzureRoute(IncomingMessageProcessor incomingProcessor) {
      this.incomingProcessor = incomingProcessor;
  }

  @Override
  public void configure() throws Exception {
      from("wmq:queue:YOUR.QUEUE")
          .routeId("ibmMqToAzureRoute")
          .process(incomingProcessor)
          .multicast()
          .to("azure-storage-blob://container/blob")
          .to("direct:sendToServiceBus");
  }
}
