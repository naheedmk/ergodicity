package com.ergodicity.cgate

import org.mockito.Mockito._
import org.mockito.Matchers._
import akka.actor.{FSM, Terminated, ActorSystem}
import org.scalatest.{BeforeAndAfterAll, WordSpec}
import akka.event.Logging
import akka.testkit.{ImplicitSender, TestFSMRef, TestKit}
import ru.micexrts.cgate.{State => CGState, Connection => CGConnection}
import com.ergodicity.cgate.Connection._

class ConnectionSpec extends TestKit(ActorSystem("ConnectionSpec", AkkaConfigurations.ConfigWithDetailedLogging)) with WordSpec with BeforeAndAfterAll with ImplicitSender {
  val log = Logging(system, self)

  val Host = "host"
  val Port = 4001
  val AppName = "ConnectionSpec"

  override def afterAll() {
    system.shutdown()
  }

  "Connection" must {
    "be initialized in Closed state" in {
      val cg = mock(classOf[CGConnection])
      val connection = TestFSMRef(new Connection(cg), "Connection")
      log.info("State: " + connection.stateName)
      assert(connection.stateName == Closed)
    }

    "go to Connecting status" in {
      val cg = mock(classOf[CGConnection])
      when(cg.getState).thenReturn(CGState.OPENING)

      val connection = TestFSMRef(new Connection(cg), "Connection")
      connection ! Open

      verify(cg).open(anyString())

      connection ! ConnectionState(Opening)
      assert(connection.stateName == Opening)
    }

    "go to Active state immediately" in {
      val cg = mock(classOf[CGConnection])
      when(cg.getState).thenReturn(CGState.ACTIVE)

      val connection = TestFSMRef(new Connection(cg), "Connection")
      connection ! Open

      verify(cg).open(anyString())

      connection ! ConnectionState(Active)
      assert(connection.stateName == Active)
    }

    "go to Actiove status after Connection established" in {
      val cg = mock(classOf[CGConnection])
      when(cg.getState).thenReturn(CGState.OPENING)
        .thenReturn(CGState.ACTIVE)

      val connection = TestFSMRef(new Connection(cg), "Connection")
      connection ! Open

      connection ! ConnectionState(Active)
      assert(connection.stateName == Active)
    }

    "terminate after Connection disconnected" in {
      val cg = mock(classOf[CGConnection])
      when(cg.getState).thenReturn(CGState.ACTIVE)
        .thenReturn(CGState.ERROR)

      val connection = TestFSMRef(new Connection(cg), "Connection")
      watch(connection)
      connection ! Open

      connection ! ConnectionState(Error)
      expectMsg(Terminated(connection))
    }

    "return to Closed state after Close connection sent" in {
      val cg = mock(classOf[CGConnection])
      when(cg.getState).thenReturn(CGState.ACTIVE)

      val connection = TestFSMRef(new Connection(cg), "Connection")
      watch(connection)
      connection ! Close
      assert(connection.stateName == Closed)
    }

    "terminate on FSM.StateTimeout in Opening state" in {
      val cg = mock(classOf[CGConnection])
      when(cg.getState).thenReturn(CGState.OPENING)

      val connection = TestFSMRef(new Connection(cg), "Connection")
      connection.setState(Opening)
      watch(connection)
      connection ! FSM.StateTimeout
      expectMsg(Terminated(connection))
    }
  }
}