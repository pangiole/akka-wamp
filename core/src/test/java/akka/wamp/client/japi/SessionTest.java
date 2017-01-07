/*
 * This file is subject to the terms and conditions defined in
 * file 'LICENSE.md', which is part of this source code package.
 */

package akka.wamp.client.japi;

import akka.actor.*;
import akka.testkit.*;
import akka.wamp.*;
import akka.wamp.messages.*;
import akka.wamp.router.*;
import org.junit.*;
import java.net.URI;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.*;

/*
 * This test suite is written following the "Shared Fixture" pattern.
 * Each test method leaves its fixture over the next one. Therefore
 * tests are being executed in sequence.
 *
 */
public class SessionTest {

  static ActorSystem system;

  static URI uri;


  @BeforeClass
  public static void setUp() {
    system = ActorSystem.create("test");
    new JavaTestKit(system) {{
      TestActorRef<Actor> router = TestActorRef.create(system, Router.props());
      ActorRef manager = Wamp.get(system).manager();
      manager.tell(WampMessage.bind(router, "default"),  getRef());
      Bound bound = (Bound) expectMsgAnyClassOf(duration("32 seconds"), Bound.class);
      uri = bound.uri();
    }};

  }

  @Test
  public void testConnect() throws Exception {
    Client client = Client.create(system);
    Connection conn = client.connect(uri, "json").toCompletableFuture().get();
    assertThat(conn.format(), is("json"));
  }

}
