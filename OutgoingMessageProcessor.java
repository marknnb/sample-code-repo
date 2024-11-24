@Component
public class OutgoingMessageProcessor implements Processor {
  @Override
  public void process(Exchange exchange) throws Exception {
      // Get the message
      String message = exchange.getIn().getBody(String.class);

      // Process for outgoing (example transformation)
      JsonNode jsonNode = new ObjectMapper().readTree(message);
      // Add outgoing specific transformation
      exchange.getIn().setBody(jsonNode.toString());
      exchange.getIn().setHeader("sentTimestamp", System.currentTimeMillis());
  }
}
