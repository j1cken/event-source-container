package com.redhat;

import java.net.URI;
import java.util.UUID;

import javax.enterprise.context.ApplicationScoped;

import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.rest.RestParamType;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import io.cloudevents.CloudEvent;
import io.cloudevents.core.builder.CloudEventBuilder;
import io.cloudevents.core.provider.EventFormatProvider;
import io.cloudevents.jackson.JsonFormat;
import io.netty.handler.codec.http.HttpMethod;

@ApplicationScoped
public class ReactiveGreetingResource extends RouteBuilder {
    
    @ConfigProperty(name = "K_SINK")
    String ceTarget;

    @Override
    public void configure() throws Exception {

        rest().get("/")  
            .param()
                .name("name")
                .type(RestParamType.query)
                .defaultValue("world")
                .description("Name who to greet")
                .endParam()      
        .to("direct:ce");
        
        from("direct:ce")
        .process(exchange -> {
            CloudEvent cloudevent = CloudEventBuilder.v1()
            .withId(UUID.randomUUID().toString())
            .withType("greeting")
            .withSource(new URI("quarkus-camel"))
            .withDataContentType("application/json")
            .withData(("{\"hello\":\"" + exchange.getIn().getHeader("name") + "\"}").getBytes())
            .build();
            exchange.getIn().setBody(EventFormatProvider.getInstance().resolveFormat(JsonFormat.CONTENT_TYPE).serialize(cloudevent));
        })
        .removeHeaders("*")
        .setHeader(Exchange.HTTP_METHOD, constant(HttpMethod.POST))
        .setHeader(Exchange.CONTENT_TYPE, constant("application/cloudevents+json"))
        .setHeader(Exchange.HTTP_URI, constant(ceTarget))
        .to("log:INFO?showHeaders=true")
        .to("vertx-http:httpUri");
    }
    
}