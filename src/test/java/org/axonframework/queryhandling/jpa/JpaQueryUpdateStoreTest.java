package org.axonframework.queryhandling.jpa;

import demo.DemoApp;
import demo.DemoQuery;
import demo.DemoQueryResult;
import org.axonframework.messaging.responsetypes.ResponseType;
import org.axonframework.messaging.responsetypes.ResponseTypes;
import org.axonframework.queryhandling.DistributedQueryBusAutoConfig;
import org.axonframework.queryhandling.GenericSubscriptionQueryUpdateMessage;
import org.axonframework.queryhandling.SubscriptionId;
import org.axonframework.queryhandling.SubscriptionQueryUpdateMessage;
import org.axonframework.queryhandling.jpa.model.SubscriptionEntity;
import org.axonframework.serialization.Serializer;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Optional;

import static org.junit.Assert.*;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = {
        DistributedQueryBusAutoConfig.class,
        DemoApp.class
})
@ActiveProfiles("spring-test")
public class JpaQueryUpdateStoreTest {

    @Autowired
    JpaQueryUpdateStore jpaQueryUpdateStore;

    @Autowired
    Serializer messageSerializer;

    @Test
    public void testSubscription() {
        // arrange
        DemoQuery query = new DemoQuery("mockedAggId");
        SubscriptionId id = new SubscriptionId("mockedNodeId", query, messageSerializer);
        ResponseType<DemoQueryResult> initialResponseType = ResponseTypes.instanceOf(DemoQueryResult.class);
        ResponseType<DemoQueryResult> updateResponseType = ResponseTypes.instanceOf(DemoQueryResult.class);

        // act
        SubscriptionEntity subscription = jpaQueryUpdateStore.getOrCreateSubscription(
                id,
                query,
                initialResponseType,
                updateResponseType
        );
        SubscriptionEntity subscription2 = jpaQueryUpdateStore.getOrCreateSubscription(
                id,
                query,
                initialResponseType,
                updateResponseType
        );

        // assert
        assertTrue(jpaQueryUpdateStore.subscriptionExists(id));

        assertEquals(subscription, subscription2);
    }

    @Test
    public void testSubscriptionRemoval() {
        // arrange
        DemoQuery query = new DemoQuery("mockedAggId");
        SubscriptionId id = new SubscriptionId("mockedNodeId", query, messageSerializer);
        ResponseType<DemoQueryResult> initialResponseType = ResponseTypes.instanceOf(DemoQueryResult.class);
        ResponseType<DemoQueryResult> updateResponseType = ResponseTypes.instanceOf(DemoQueryResult.class);

        // act
        jpaQueryUpdateStore.getOrCreateSubscription(
                id,
                query,
                initialResponseType,
                updateResponseType
        );

        // act
        jpaQueryUpdateStore.removeSubscription(id);

        // assert
        assertFalse(jpaQueryUpdateStore.subscriptionExists(id));
    }

    @Test
    public void testUpdatePosting() {
        // arrange
        DemoQuery query = new DemoQuery("mockedAggId");
        SubscriptionId id = new SubscriptionId("mockedNodeId", query, messageSerializer);
        ResponseType<DemoQueryResult> initialResponseType = ResponseTypes.instanceOf(DemoQueryResult.class);
        ResponseType<DemoQueryResult> updateResponseType = ResponseTypes.instanceOf(DemoQueryResult.class);

        SubscriptionEntity subscription = jpaQueryUpdateStore.getOrCreateSubscription(
                id,
                query,
                initialResponseType,
                updateResponseType
        );

        GenericSubscriptionQueryUpdateMessage<DemoQueryResult> updateMessage = new GenericSubscriptionQueryUpdateMessage<>(
                DemoQueryResult.class, new DemoQueryResult("mockedAggId"));

        // act
        jpaQueryUpdateStore.postUpdate(subscription, updateMessage);

        // assert
        Optional<SubscriptionQueryUpdateMessage<DemoQueryResult>> updateOpt = jpaQueryUpdateStore.popUpdate(id);
        assertTrue(updateOpt.isPresent());
    }
}
