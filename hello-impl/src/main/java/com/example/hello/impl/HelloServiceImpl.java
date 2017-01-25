/*
 * Copyright (C) 2016 Lightbend Inc. <http://www.lightbend.com>
 */
package com.example.hello.impl;

import akka.Done;
import akka.NotUsed;
import akka.stream.javadsl.Source;
import com.example.hello.api.GreetingMessage;
import com.example.hello.api.HelloService;
import com.example.hello.impl.HelloCommand.Hello;
import com.example.hello.impl.HelloCommand.UseGreetingMessage;
import com.lightbend.lagom.javadsl.api.ServiceCall;
import com.lightbend.lagom.javadsl.persistence.PersistentEntityRef;
import com.lightbend.lagom.javadsl.persistence.PersistentEntityRegistry;
import com.lightbend.lagom.javadsl.pubsub.PubSubRegistry;
import com.lightbend.lagom.javadsl.pubsub.TopicId;

import javax.inject.Inject;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Implementation of the HelloService.
 */
public class HelloServiceImpl implements HelloService {

    private final PersistentEntityRegistry persistentEntityRegistry;
    private final PubSubRegistry pubSub;

    @Inject
    public HelloServiceImpl(PersistentEntityRegistry persistentEntityRegistry, PubSubRegistry pubSub) {
        this.persistentEntityRegistry = persistentEntityRegistry;
        this.pubSub = pubSub;
        persistentEntityRegistry.register(HelloEntity.class);
    }

    @Override
    public ServiceCall<NotUsed, String> hello(String id) {
        return request -> {
            PersistentEntityRef<HelloCommand> ref = persistentEntityRegistry.refFor(HelloEntity.class, id);
            return ref.ask(new Hello(id, Optional.empty())).thenApply(msg -> {
                pubSub.refFor(TopicId.of(GreetingMessage.class, "hardcoded-qualifier")).publish(new GreetingMessage(msg));
                return msg;
            });
        };
    }

    @Override
    public ServiceCall<NotUsed, Source<GreetingMessage, NotUsed>> hellos() {
        return request ->
                CompletableFuture.completedFuture(
                        pubSub.refFor(TopicId.of(GreetingMessage.class, "hardcoded-qualifier")).subscriber()
                );
    }


    @Override
    public ServiceCall<GreetingMessage, Done> useGreeting(String id) {
        return request -> {
            // Look up the hello world entity for the given ID.
            PersistentEntityRef<HelloCommand> ref = persistentEntityRegistry.refFor(HelloEntity.class, id);
            // Tell the entity to use the greeting message specified.
            return ref.ask(new UseGreetingMessage(request.message));
        };

    }

}
