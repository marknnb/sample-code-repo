@Component
public class ServiceBusRoute extends RouteBuilder {

  private final OutgoingMessageProcessor outgoingProcessor;

  public ServiceBusRoute(OutgoingMessageProcessor outgoingProcessor) {
      this.outgoingProcessor = outgoingProcessor;
  }

  @Override
  public void configure() throws Exception {
      from("direct:sendToServiceBus")
          .routeId("serviceBusRoute")
          .process(outgoingProcessor)
          .to("azure-servicebus:queue:YOUR.QUEUE");
  }
}
