@ExtendWith(MockitoExtension.class)
public class ServiceBusRouteTest extends CamelTestSupport {

  @Mock
  private OutgoingMessageProcessor outgoingProcessor;

  @Override
  protected RoutesBuilder createRouteBuilder() throws Exception {
      return new ServiceBusRoute(outgoingProcessor);
  }

  @BeforeEach
  public void setupRoute() throws Exception {
      AdviceWith.adviceWith(context, "serviceBusRoute", a -> {
          // Mock the Azure Service Bus endpoint
          a.weaveByToUri("azure-servicebus:*")
              .replace()
              .to("mock:serviceBus");
      });
  }

  @Test
  public void testSuccessfulMessageProcessing() throws Exception {
      // Get mock endpoint
      MockEndpoint mockServiceBus = getMockEndpoint("mock:serviceBus");

      // Input and processed messages
      String inputMessage = "{\"test\":\"message\"}";
      String processedMessage = "{\"test\":\"message\",\"outgoing\":true}";

      // Mock processor behavior
      when(outgoingProcessor.process(any(Exchange.class))).thenAnswer(invocation -> {
          Exchange exchange = invocation.getArgument(0);
          exchange.getIn().setBody(processedMessage);
          exchange.getIn().setHeader("sentTimestamp", System.currentTimeMillis());
      });

      // Set expectations
      mockServiceBus.expectedMessageCount(1);
      mockServiceBus.expectedBodiesReceived(processedMessage);
      mockServiceBus.expectedHeaderReceived("sentTimestamp", isA(Long.class));

      // Send test message
      template.sendBody("direct:sendToServiceBus", inputMessage);

      // Verify expectations
      MockEndpoint.assertIsSatisfied(context);
      verify(outgoingProcessor, times(1)).process(any(Exchange.class));
  }

  @Test
  public void testProcessorError() throws Exception {
      // Mock processor to throw exception
      doThrow(new RuntimeException("Outgoing processing error"))
          .when(outgoingProcessor)
          .process(any(Exchange.class));

      // Get mock endpoint
      MockEndpoint mockServiceBus = getMockEndpoint("mock:serviceBus");
      mockServiceBus.expectedMessageCount(0);

      try {
          template.sendBody("direct:sendToServiceBus", "{\"test\":\"message\"}");
          fail("Should have thrown an exception");
      } catch (Exception e) {
          assertTrue(e.getCause().getMessage().contains("Outgoing processing error"));
      }

      // Verify expectations
      MockEndpoint.assertIsSatisfied(context);
  }

  @Test
  public void testWithDifferentMessageFormats() throws Exception {
      // Mock processor to pass through messages
      when(outgoingProcessor.process(any(Exchange.class))).thenAnswer(invocation -> {
          // Do nothing, just pass through
      });

      MockEndpoint mockServiceBus = getMockEndpoint("mock:serviceBus");

      // Test with different message formats
      List<String> testMessages = Arrays.asList(
          "{\"key\":\"value\"}", // JSON
          "<root><key>value</key></root>", // XML
          "plain text message" // Plain text
      );

      mockServiceBus.expectedMessageCount(testMessages.size());
      mockServiceBus.expectedBodiesReceived(testMessages);

      for (String message : testMessages) {
          template.sendBody("direct:sendToServiceBus", message);
      }

      // Verify expectations
      MockEndpoint.assertIsSatisfied(context);
  }

  @Test
  public void testWithHeaders() throws Exception {
      MockEndpoint mockServiceBus = getMockEndpoint("mock:serviceBus");

      // Mock processor to add a header
      when(outgoingProcessor.process(any(Exchange.class))).thenAnswer(invocation -> {
          Exchange exchange = invocation.getArgument(0);
          exchange.getIn().setHeader("outgoingHeader", "outgoing");
      });

      // Set expectations
      mockServiceBus.expectedHeaderReceived("originalHeader", "original");
      mockServiceBus.expectedHeaderReceived("outgoingHeader", "outgoing");

      // Send message with headers
      Map<String, Object> headers = new HashMap<>();
      headers.put("originalHeader", "original");
      template.sendBodyAndHeaders("direct:sendToServiceBus", "test message", headers);

      // Verify expectations
      MockEndpoint.assertIsSatisfied(context);
  }

  @Test
  public void testLargeMessage() throws Exception {
      // Create a large message
      StringBuilder largeMessage = new StringBuilder();
      for (int i = 0; i < 1000; i++) {
          largeMessage.append("{\"index\":").append(i).append(",\"data\":\"test\"}\n");
      }

      // Mock processor behavior
      when(outgoingProcessor.process(any(Exchange.class))).thenAnswer(invocation -> {
          // Do nothing, just pass through
      });

      MockEndpoint mockServiceBus = getMockEndpoint("mock:serviceBus");
      mockServiceBus.expectedMessageCount(1);
      mockServiceBus.expectedBodiesReceived(largeMessage.toString());

      // Send large message
      template.sendBody("direct:sendToServiceBus", largeMessage.toString());

      // Verify expectations
      MockEndpoint.assertIsSatisfied(context);
  }

  @Override
  public boolean isUseAdviceWith() {
      return true;
  }
}
