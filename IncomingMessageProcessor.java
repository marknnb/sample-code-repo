@Component
public class IncomingMessageProcessor implements Processor {
  @Override
  public void process(Exchange exchange) throws Exception {
      // Get the incoming message
      String message = exchange.getIn().getBody(String.class);

      // Process the message (example transformation)
      JsonNode jsonNode = new ObjectMapper().readTree(message);
      // Add some transformation or validation
      exchange.getIn().setBody(jsonNode.toString());
      exchange.getIn().setHeader("processedTimestamp", System.currentTimeMillis());
  }
}
