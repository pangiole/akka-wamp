/*
 * This file is subject to the terms and conditions defined in
 * file 'LICENSE.md', which is part of this source code package.
 */

package akka.wamp.client.japi;

import akka.actor.Actor;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.testkit.JavaTestKit;
import akka.testkit.TestActorRef;
import akka.wamp.client.Wamp;
import akka.wamp.messages.Bound;
import akka.wamp.messages.WampMessage;
import akka.wamp.router.Router;
import com.typesafe.config.ConfigFactory;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import java.util.function.Consumer;

import static akka.wamp.client.TestIdGenerators.newTestIdGenerators;
import static java.util.Arrays.asList;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.*;


/*
 * This test suite is written following the "Shared Fixture" pattern.
 *
 * Each test method leaves its fixture over the next one. Therefore
 * tests are being executed in sequence and shared state is kept as
 * class member variables (by using the Java 'static' qualifier)
 *
 * @see http://xunitpatterns.com/Chained%20Tests.html
 *
 */
// TODO Make the SBT build able to kick-off JUnit tests too!

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class JavaClientTest {
  
  static Long SIXTEEN = 16L;

  static ActorSystem system;

  static String uri;
  static Connection conn;
  static Session session;
  static Subscription subscription;


  @BeforeClass
  public static void setUp() {
    system = ActorSystem.create(
        "test",
        ConfigFactory.load("akka-test.conf").withFallback(ConfigFactory.load())
    );
    new JavaTestKit(system) {{
      Props props = Props.create(Router.class, newTestIdGenerators());
      TestActorRef<Actor> router = TestActorRef.create(system,  props);
      ActorRef manager = akka.wamp.router.Wamp.get(system).manager();
      manager.tell(WampMessage.bind(router, "default"),  getRef());
      Bound bound = (Bound) expectMsgAnyClassOf(duration("8.seconds"), Bound.class);
      uri = bound.uri().toString();
    }};

  }


  @Test
  public void test1_connect() throws Exception {
    Client client = Client.create(system);
    conn = client.connect(uri, "json").toCompletableFuture().get(SIXTEEN, SECONDS);
    assertThat(conn, notNullValue());
    assertThat(conn.format(), is("json"));
    assertThat(conn.uri().getScheme(), is("ws"));
    assertThat(conn.uri().getHost(), is("127.0.0.1"));
    // TODO assertThat(conn.uri().getPort(), greaterThan(0));
    assertThat(conn.uri().getPath(), is("/wamp"));
  }


  @Test
  public void test2_open() throws Exception {
    session = conn.open().toCompletableFuture().get(SIXTEEN, SECONDS);
    assertThat(session, notNullValue());
    assertThat(session.id(), is(1L));
  }


  @Test
  public void test3_publish_subscribe() throws Exception {
    Consumer<Event> consumer = mock(Consumer.class);
    subscription = session.subscribe("mytopic", consumer).toCompletableFuture().get(SIXTEEN, SECONDS);
    // TODO assertThat(subscription.id(), is(1L));
    assertThat(subscription.topic(), is("mytopic"));
    session.publish("mytopic", asList("paolo", 99, true));
    verify(consumer, timeout(SIXTEEN * 1000).only()).accept(any());
  }



  @Test
  public void test4_unsubscribe() throws Exception {
    Unsubscribed unsubscribed = subscription.unsubscribe().toCompletableFuture().get(SIXTEEN, SECONDS);
    assertThat(unsubscribed, notNullValue());
  }


  // TODO add more tests ...


  @Test
  public void test99_disconnect() throws Exception {
    Disconnected disconnected = conn.disconnect().toCompletableFuture().get(SIXTEEN, SECONDS);
    assertThat(disconnected, notNullValue());
  }
}
