package doobie.contrib.h2

import doobie.contrib.h2.h2types._
import doobie.imports._
import doobie.util.update._
import doobie.util.query._

import java.net.InetAddress
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger

import org.specs2.mutable.Specification

import scalaz.concurrent.Task
import scalaz.\/-

// Establish that we can read various types. It's not very comprehensive as a test, bit it's a start.
object h2typesspec extends Specification {

  val xa = DriverManagerTransactor[Task](
    "org.h2.Driver",                     
    "jdbc:h2:mem:ch3;DB_CLOSE_DELAY=-1",  
    "sa", ""                              
  )

  def inOut[A: Atom](col: String, a: A) =
    for {
      _  <- Update0(s"CREATE LOCAL TEMPORARY TABLE TEST (value $col)", None).run
      _  <- sql"INSERT INTO TEST VALUES ($a)".update.run
      a0 <- sql"SELECT value FROM TEST".query[A].unique
    } yield (a0)

  def testInOut[A](col: String, a: A)(implicit m: Meta[A]) = 
    s"Mapping for $col as ${m.scalaType}" >> {
      s"write+read $col as ${m.scalaType}" in { 
        inOut(col, a).transact(xa).attemptRun must_== \/-(a)
      }
      s"write+read $col as Option[${m.scalaType}] (Some)" in { 
        inOut[Option[A]](col, Some(a)).transact(xa).attemptRun must_== \/-(Some(a))
      }
      s"write+read $col as Option[${m.scalaType}] (None)" in { 
        inOut[Option[A]](col, None).transact(xa).attemptRun must_== \/-(None)
      }
    }

  def skip(col: String, msg: String = "not yet implemented") =
    s"Mapping for $col" >> {
      "PENDING:" in pending(msg)
    }

  testInOut[Int]("INT", 123)
  testInOut[Boolean]("BOOLEAN", true)
  testInOut[Byte]("TINYINT",  123)
  testInOut[Short]("SMALLINT", 123)
  testInOut[Long]("BIGINT", 123)
  testInOut[BigDecimal]("DECIMAL", 123.45)
  testInOut[java.sql.Time]("TIME", new java.sql.Time(3,4,5))
  testInOut[java.sql.Date]("DATE", new java.sql.Date(4,5,6))
  testInOut[java.sql.Timestamp]("TIMESTAMP", new java.sql.Timestamp(System.currentTimeMillis))
  testInOut[List[Byte]]("BINARY", BigInt("DEADBEEF",16).toByteArray.toList) 
  skip("OTHER")
  testInOut[String]("VARCHAR", "abc")
  testInOut[String]("CHAR(3)", "abc")
  skip("BLOB")
  skip("CLOB")
  testInOut[UUID]("UUID", UUID.randomUUID)
  testInOut[List[Int]]("ARRAY", List(1,2,3))
  testInOut[List[String]]("ARRAY", List("foo", "bar"))
  skip("GEOMETRY")

}

