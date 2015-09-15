package org.example;

import org.apache.camel.Exchange;
import org.apache.camel.processor.aggregate.AggregationStrategy;

/**
 * Aggregator implementation which wraps all <account> fragments into a single
 * CSV document with each <account> appearing on a separate line.
 */
public class AccountAggregator implements AggregationStrategy {

    @Override
    public Exchange aggregate(Exchange oldExchange, Exchange newExchange) {
        if (oldExchange == null) {
            return newExchange;
        }
        String originalBody = oldExchange.getIn().getBody(String.class);
        String bodyToAdd = newExchange.getIn().getBody(String.class);
        oldExchange.getIn().setBody(originalBody + bodyToAdd);
        return oldExchange;
    }
    
}