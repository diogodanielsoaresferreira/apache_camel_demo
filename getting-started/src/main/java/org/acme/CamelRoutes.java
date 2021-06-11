package org.acme;

import org.apache.camel.builder.endpoint.EndpointRouteBuilder;

public class CamelRoutes extends EndpointRouteBuilder {

    public void configure() {

        errorHandler(deadLetterChannel("kafka:dead_letter?brokers=localhost:9092")
            .useOriginalMessage());

        from("kafka:input_topic?brokers=localhost:9092&groupId=group1")
            .unmarshal().json(UserMessage.class)
            .filter(simple("${body.type} == 'chat'"))
            .to("seda:incoming_event");

        from("seda:incoming_event?multipleConsumers=true")
            .log("${body}")
            .split(simple("${body.devices}"))
                .log("${body}")
            .end()
            .marshal().json()
            .to("kafka:output_topic?brokers=localhost:9092")
            .to("direct:store_in_file");

        from("direct:store_in_file")
            .unmarshal().json(UserMessage.class)
            .choice()
            .when(simple("${body.emitter} == 'John Doe'"))
                .marshal().json().to("file://YOUR_PATH/user-message?filename=events.json&fileExist=Append&appendChars=\\n")
            .otherwise()
                .marshal().json().to("file://YOUR_PATH/john-doe-message?filename=events.json&fileExist=Append&appendChars=\\n")
            .end();

        from("seda:incoming_event?multipleConsumers=true")
            .aggregate(simple("${body.emitter}"), new CombinedUserMessagesAggregationStrategy())
            .completionInterval(5000)
            .bean(NLPUtils.class, "createUserMessages")
            .log("${body}");

        from("timer://foo?period=5000").log("timer");

        rest("/api").get("/hello").to("direct:hello");
        from("direct:hello").transform().constant("Hello world!");

    }

}
