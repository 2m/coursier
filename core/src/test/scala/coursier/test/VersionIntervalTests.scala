package coursier
package test

import coursier.core._
import utest._

object VersionIntervalTests extends TestSuite {

  val tests = TestSuite{
    'invalid{
      'basic{
        assert(VersionInterval.zero.isValid)

        val itv1 = VersionInterval(None, None, true, true)
        val itv2 = VersionInterval(None, None, false, true)
        val itv3 = VersionInterval(None, None, true, false)

        assert(!itv1.isValid)
        assert(!itv2.isValid)
        assert(!itv3.isValid)
      }
      'halfBounded{
        val itv1 = VersionInterval(Some(Version("1.2")), None, true, true)
        val itv2 = VersionInterval(Some(Version("1.2")), None, false, true)
        val itv3 = VersionInterval(None, Some(Version("1.2")), true, true)
        val itv4 = VersionInterval(None, Some(Version("1.2")), true, false)

        assert(!itv1.isValid)
        assert(!itv2.isValid)
        assert(!itv3.isValid)
        assert(!itv4.isValid)
      }
      'order{
        val itv1 = VersionInterval(Some(Version("2")), Some(Version("1")), true, true)
        val itv2 = VersionInterval(Some(Version("2")), Some(Version("1")), false, true)
        val itv3 = VersionInterval(Some(Version("2")), Some(Version("1")), true, false)
        val itv4 = VersionInterval(Some(Version("2")), Some(Version("1")), false, false)

        assert(!itv1.isValid)
        assert(!itv2.isValid)
        assert(!itv3.isValid)
        assert(!itv4.isValid)
      }
      'bound{
        val itv1 = VersionInterval(Some(Version("2")), Some(Version("2")), false, true)
        val itv2 = VersionInterval(Some(Version("2")), Some(Version("2")), true, false)
        val itv3 = VersionInterval(Some(Version("2")), Some(Version("2")), false, false)

        assert(!itv1.isValid)
        assert(!itv2.isValid)
        assert(!itv3.isValid)

        val itv4 = VersionInterval(Some(Version("2")), Some(Version("2")), true, true)
        assert(itv4.isValid)
      }
    }

    'merge{
      'basic{
        val itv0m = VersionInterval.zero.merge(VersionInterval.zero)
        assert(itv0m == Some(VersionInterval.zero))

        val itv1 = VersionInterval(Some(Version("1")), Some(Version("2")), false, true)
        val itv1m = itv1.merge(VersionInterval.zero)
        val itv1m0 = VersionInterval.zero.merge(itv1)
        assert(itv1m == Some(itv1))
        assert(itv1m0 == Some(itv1))
      }
      'noIntersec{
        val itv1 = VersionInterval(Some(Version("1")), Some(Version("2")), true, false)
        val itv2 = VersionInterval(Some(Version("3")), Some(Version("5")), false, true)
        val itvm = itv1 merge itv2
        val itvm0 = itv2 merge itv1
        assert(itvm == None)
        assert(itvm0 == None)
      }
      'noIntersecSameFrontierOpenClose{
        val itv1 = VersionInterval(Some(Version("1")), Some(Version("2")), true, false)
        val itv2 = VersionInterval(Some(Version("2")), Some(Version("4")), true, true)
        val itvm = itv1 merge itv2
        val itvm0 = itv2 merge itv1
        assert(itvm == None)
        assert(itvm0 == None)
      }
      'noIntersecSameFrontierCloseOpen{
        val itv1 = VersionInterval(Some(Version("1")), Some(Version("2")), true, true)
        val itv2 = VersionInterval(Some(Version("2")), Some(Version("4")), false, true)
        val itvm = itv1 merge itv2
        val itvm0 = itv2 merge itv1
        assert(itvm == None)
        assert(itvm0 == None)
      }
      'noIntersecSameFrontierOpenOpen{
        val itv1 = VersionInterval(Some(Version("1")), Some(Version("2")), true, false)
        val itv2 = VersionInterval(Some(Version("2")), Some(Version("4")), false, true)
        val itvm = itv1 merge itv2
        val itvm0 = itv2 merge itv1
        assert(itvm == None)
        assert(itvm0 == None)
      }
      'intersecSameFrontierCloseClose{
        val itv1 = VersionInterval(Some(Version("1")), Some(Version("2")), true, true)
        val itv2 = VersionInterval(Some(Version("2")), Some(Version("4")), true, true)
        val itvm = itv1 merge itv2
        val itvm0 = itv2 merge itv1
        val expected = VersionInterval(Some(Version("2")), Some(Version("2")), true, true)
        assert(itvm == Some(expected))
        assert(itvm0 == Some(expected))
      }
      'intersec{
        val bools = Seq(true, false)
        for (l1 <- bools; l2 <- bools; r1 <- bools; r2 <- bools) {
          val itv1 = VersionInterval(Some(Version("1")), Some(Version("3")), l1, r1)
          val itv2 = VersionInterval(Some(Version("2")), Some(Version("4")), l2, r2)
          val itvm = itv1 merge itv2
          val itvm0 = itv2 merge itv1
          val expected = VersionInterval(Some(Version("2")), Some(Version("3")), l2, r1)
          assert(itvm == Some(expected))
          assert(itvm0 == Some(expected))
        }
      }
    }

    'contains{
      val v21 = Version("2.1")
      val v22 = Version("2.2")
      val v23 = Version("2.3")
      val v24 = Version("2.4")
      val v25 = Version("2.5")
      val v26 = Version("2.6")
      val v27 = Version("2.7")
      val v28 = Version("2.8")

      'basic{
        val itv = Parse.versionInterval("[2.2,)").get
        
        assert(!itv.contains(v21))
        assert(itv.contains(v22))
        assert(itv.contains(v23))
        assert(itv.contains(v24))
      }
      'open{
        val itv = Parse.versionInterval("(2.2,)").get

        assert(!itv.contains(v21))
        assert(!itv.contains(v22))
        assert(itv.contains(v23))
        assert(itv.contains(v24))
      }
      'segment{
        val itv = Parse.versionInterval("[2.2,2.8]").get

        assert(!itv.contains(v21))
        assert(itv.contains(v22))
        assert(itv.contains(v23))
        assert(itv.contains(v24))
        assert(itv.contains(v25))
        assert(itv.contains(v26))
        assert(itv.contains(v27))
        assert(itv.contains(v28))
      }
    }

    'parse{
      'malformed{
        val s1 = "[1.1]"
        val itv1 = Parse.versionInterval(s1)
        assert(itv1 == None)

        val s2 = "(1.1)"
        val itv2 = Parse.versionInterval(s2)
        assert(itv2 == None)

        val s3 = "()"
        val itv3 = Parse.versionInterval(s3)
        assert(itv3 == None)

        val s4 = "[1.1,1.3"
        val itv4 = Parse.versionInterval(s4)
        assert(itv4 == None)

        val s5 = "1.1,1.3)"
        val itv5 = Parse.versionInterval(s5)
        assert(itv5 == None)
      }
      'basic {
        val s1 = "[1.1,1.3]"
        val itv1 = Parse.versionInterval(s1)
        assert(itv1 == Some(VersionInterval(Some(Version("1.1")), Some(Version("1.3")), true, true)))

        val s2 = "(1.1,1.3]"
        val itv2 = Parse.versionInterval(s2)
        assert(itv2 == Some(VersionInterval(Some(Version("1.1")), Some(Version("1.3")), false, true)))

        val s3 = "[1.1,1.3)"
        val itv3 = Parse.versionInterval(s3)
        assert(itv3 == Some(VersionInterval(Some(Version("1.1")), Some(Version("1.3")), true, false)))

        val s4 = "(1.1,1.3)"
        val itv4 = Parse.versionInterval(s4)
        assert(itv4 == Some(VersionInterval(Some(Version("1.1")), Some(Version("1.3")), false, false)))
      }
      'leftEmptyVersions {
        val s1 = "[,1.3]"
        val itv1 = Parse.versionInterval(s1)
        assert(itv1 == Some(VersionInterval(None, Some(Version("1.3")), true, true)))
        assert(!itv1.get.isValid)

        val s2 = "(,1.3]"
        val itv2 = Parse.versionInterval(s2)
        assert(itv2 == Some(VersionInterval(None, Some(Version("1.3")), false, true)))
        assert(itv2.get.isValid)

        val s3 = "[,1.3)"
        val itv3 = Parse.versionInterval(s3)
        assert(itv3 == Some(VersionInterval(None, Some(Version("1.3")), true, false)))
        assert(!itv3.get.isValid)

        val s4 = "(,1.3)"
        val itv4 = Parse.versionInterval(s4)
        assert(itv4 == Some(VersionInterval(None, Some(Version("1.3")), false, false)))
        assert(itv4.get.isValid)
      }
      'rightEmptyVersions {
        val s1 = "[1.3,]"
        val itv1 = Parse.versionInterval(s1)
        assert(itv1 == Some(VersionInterval(Some(Version("1.3")), None, true, true)))
        assert(!itv1.get.isValid)

        val s2 = "(1.3,]"
        val itv2 = Parse.versionInterval(s2)
        assert(itv2 == Some(VersionInterval(Some(Version("1.3")), None, false, true)))
        assert(!itv2.get.isValid)

        val s3 = "[1.3,)"
        val itv3 = Parse.versionInterval(s3)
        assert(itv3 == Some(VersionInterval(Some(Version("1.3")), None, true, false)))
        assert(itv3.get.isValid)

        val s4 = "(1.3,)"
        val itv4 = Parse.versionInterval(s4)
        assert(itv4 == Some(VersionInterval(Some(Version("1.3")), None, false, false)))
        assert(itv4.get.isValid)
      }
      'bothEmptyVersions {
        val s1 = "[,]"
        val itv1 = Parse.versionInterval(s1)
        assert(itv1 == Some(VersionInterval(None, None, true, true)))
        assert(!itv1.get.isValid)

        val s2 = "(,]"
        val itv2 = Parse.versionInterval(s2)
        assert(itv2 == Some(VersionInterval(None, None, false, true)))
        assert(!itv2.get.isValid)

        val s3 = "[,)"
        val itv3 = Parse.versionInterval(s3)
        assert(itv3 == Some(VersionInterval(None, None, true, false)))
        assert(!itv3.get.isValid)

        val s4 = "(,]"
        val itv4 = Parse.versionInterval(s4)
        assert(itv4 == Some(VersionInterval(None, None, false, true)))
        assert(!itv4.get.isValid)
      }
    }

    'repr{
      'basic {
        val s1 = "[1.1,1.3]"
        val repr1 = Parse.versionInterval(s1).map(_.repr)
        assert(repr1 == Some(s1))

        val s2 = "(1.1,1.3]"
        val repr2 = Parse.versionInterval(s2).map(_.repr)
        assert(repr2 == Some(s2))

        val s3 = "[1.1,1.3)"
        val repr3 = Parse.versionInterval(s3).map(_.repr)
        assert(repr3 == Some(s3))

        val s4 = "(1.1,1.3)"
        val repr4 = Parse.versionInterval(s4).map(_.repr)
        assert(repr4 == Some(s4))
      }
      'leftEmptyVersions {
        val s1 = "[,1.3]"
        val repr1 = Parse.versionInterval(s1).map(_.repr)
        assert(repr1 == Some(s1))

        val s2 = "(,1.3]"
        val repr2 = Parse.versionInterval(s2).map(_.repr)
        assert(repr2 == Some(s2))

        val s3 = "[,1.3)"
        val repr3 = Parse.versionInterval(s3).map(_.repr)
        assert(repr3 == Some(s3))

        val s4 = "(,1.3)"
        val repr4 = Parse.versionInterval(s4).map(_.repr)
        assert(repr4 == Some(s4))
      }
      'rightEmptyVersions {
        val s1 = "[1.3,]"
        val repr1 = Parse.versionInterval(s1).map(_.repr)
        assert(repr1 == Some(s1))

        val s2 = "(1.3,]"
        val repr2 = Parse.versionInterval(s2).map(_.repr)
        assert(repr2 == Some(s2))

        val s3 = "[1.3,)"
        val repr3 = Parse.versionInterval(s3).map(_.repr)
        assert(repr3 == Some(s3))

        val s4 = "(1.3,)"
        val repr4 = Parse.versionInterval(s4).map(_.repr)
        assert(repr4 == Some(s4))
      }
      'bothEmptyVersions {
        val s1 = "[,]"
        val repr1 = Parse.versionInterval(s1).map(_.repr)
        assert(repr1 == Some(s1))

        val s2 = "(,]"
        val repr2 = Parse.versionInterval(s2).map(_.repr)
        assert(repr2 == Some(s2))

        val s3 = "[,)"
        val repr3 = Parse.versionInterval(s3).map(_.repr)
        assert(repr3 == Some(s3))

        val s4 = "(,]"
        val repr4 = Parse.versionInterval(s4).map(_.repr)
        assert(repr4 == Some(s4))
      }
    }

    'constraint{
      'none{
        val s1 = "(,)"
        val c1 = Parse.versionInterval(s1).map(_.constraint)
        assert(c1 == Some(VersionConstraint.None))
      }
      'preferred{
        val s1 = "[1.3,)"
        val c1 = Parse.versionInterval(s1).map(_.constraint)
        assert(c1 == Some(VersionConstraint.Preferred(Parse.version("1.3").get)))
      }
      'interval{
        val s1 = "[1.3,2.4)"
        val c1 = Parse.versionInterval(s1).map(_.constraint)
        assert(c1 == Some(VersionConstraint.Interval(VersionInterval(Parse.version("1.3"), Parse.version("2.4"), true, false))))
      }
    }
  }

}
