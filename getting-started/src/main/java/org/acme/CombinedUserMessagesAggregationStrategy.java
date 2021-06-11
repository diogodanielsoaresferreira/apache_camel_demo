package org.acme;

import java.util.ArrayList;
import java.util.List;
import org.apache.camel.AggregationStrategy;
import org.apache.camel.Exchange;

public class CombinedUserMessagesAggregationStrategy implements AggregationStrategy {
    @Override
    public Exchange aggregate(Exchange oldExchange, Exchange newExchange) {
        if (oldExchange == null) {
            final UserMessage body = newExchange.getIn().getBody(UserMessage.class);
            final CombinedUserMessage newEventBody = CombinedUserMessage.builder()
                .emitter(body.getEmitter())
                .text(List.of(body.getText()))
                .build();
            newExchange.getIn().setBody(newEventBody);
            return newExchange;
        }
        final UserMessage newUserMessage = newExchange.getIn().getBody(UserMessage.class);
        final CombinedUserMessage oldCombinedUserMessage = oldExchange.getIn().getBody(CombinedUserMessage.class);

        final List<String> newTest = new ArrayList<>(oldCombinedUserMessage.getText());
        newTest.add(newUserMessage.getText());

        final CombinedUserMessage newCombinedUserMessage = CombinedUserMessage.builder()
            .emitter(newUserMessage.getEmitter())
            .text(newTest)
            .build();

        oldExchange.getIn().setBody(newCombinedUserMessage);
        return oldExchange;
    }
}
