package coursier.test

import coursier.{ Dependency, Module, Cache }
import coursier.test.compatibility._

import scala.async.Async.{ async, await }

import utest._

object IvyLocalTests extends TestSuite {

  val tests = TestSuite{
    'coursier{
      val module = Module("com.github.alexarchambault", "coursier_2.11")
      val version = "1.0.0-SNAPSHOT"

      val extraRepo = Some(Cache.ivy2Local)

      // Assume this module (and the sub-projects it depends on) is published locally
      CentralTests.resolutionCheck(
        module, version,
        extraRepo
      )


      async {
        val res = await(CentralTests.resolve(
          Set(Dependency(module, version)),
          extraRepo = extraRepo
        ))

        val artifacts = res.artifacts.map(_.url)
        val anyJavadoc = artifacts.exists(_.contains("-javadoc"))
        val anySources = artifacts.exists(_.contains("-sources"))

        assert(!anyJavadoc)
        assert(!anySources)
      }
    }
  }

}
