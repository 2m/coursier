package coursier
package test

import utest._
import scala.async.Async.{async, await}

import coursier.test.compatibility._

object ResolutionTests extends TestSuite {

  def resolve0(deps: Set[Dependency], filter: Option[Dependency => Boolean] = None) = {
    ResolutionProcess(Resolution(deps, filter = filter))
      .run(fetchSeveralFrom(repositories))
      .runF
  }

  implicit class ProjectOps(val p: Project) extends AnyVal {
    def kv: (ModuleVersion, (Repository, Project)) = p.moduleVersion -> (testRepository, p)
  }

  val projects = Seq(
    Project(Module("acme", "config"), "1.3.0"),
  
    Project(Module("acme", "play"), "2.4.0", Seq(
      Dependency(Module("acme", "play-json"), "2.4.0"))),
  
    Project(Module("acme", "play-json"), "2.4.0"),
  
    Project(Module("acme", "play"), "2.4.1",
      dependencies = Seq(
        Dependency(Module("acme", "play-json"), "${playJsonVersion}"),
        Dependency(Module("${project.groupId}", "${configName}"), "1.3.0")),
      properties = Map(
        "playJsonVersion" -> "2.4.0",
        "configName" -> "config")),
  
    Project(Module("acme", "play-extra-no-config"), "2.4.1",
      Seq(
        Dependency(Module("acme", "play"), "2.4.1",
          exclusions = Set(("acme", "config"))))),

    Project(Module("acme", "play-extra-no-config-no"), "2.4.1",
      Seq(
        Dependency(Module("acme", "play"), "2.4.1",
          exclusions = Set(("*", "config"))))),

    Project(Module("hudsucker", "mail"), "10.0",
      Seq(
        Dependency(Module("${project.groupId}", "test-util"), "${project.version}",
          scope = Scope.Test))),

    Project(Module("hudsucker", "test-util"), "10.0"),

    Project(Module("se.ikea", "parent"), "18.0",
      dependencyManagement = Seq(
        Dependency(Module("acme", "play"), "2.4.0",
          exclusions = Set(("acme", "play-json"))))),

    Project(Module("se.ikea", "billy"), "18.0",
      dependencies = Seq(
        Dependency(Module("acme", "play"), "")),
      parent = Some(Module("se.ikea", "parent"), "18.0")),

    Project(Module("org.gnome", "parent"), "7.0",
      Seq(
        Dependency(Module("org.gnu", "glib"), "13.4"))),

    Project(Module("org.gnome", "panel-legacy"), "7.0",
      dependencies = Seq(
        Dependency(Module("org.gnome", "desktop"), "${project.version}")),
      parent = Some(Module("org.gnome", "parent"), "7.0")),

    Project(Module("gov.nsa", "secure-pgp"), "10.0",
      Seq(
        Dependency(Module("gov.nsa", "crypto"), "536.89"))),

    Project(Module("com.mailapp", "mail-client"), "2.1",
      dependencies = Seq(
        Dependency(Module("gov.nsa", "secure-pgp"), "10.0",
          exclusions = Set(("*", "${crypto.name}")))),
      properties = Map("crypto.name" -> "crypto", "dummy" -> "2")),

    Project(Module("com.thoughtworks.paranamer", "paranamer-parent"), "2.6",
      dependencies = Seq(
        Dependency(Module("junit", "junit"), "")),
      dependencyManagement = Seq(
        Dependency(Module("junit", "junit"), "4.11", scope = Scope.Test))),

    Project(Module("com.thoughtworks.paranamer", "paranamer"), "2.6",
      parent = Some(Module("com.thoughtworks.paranamer", "paranamer-parent"), "2.6")),

    Project(Module("com.github.dummy", "libb"), "0.3.3",
      profiles = Seq(
        Profile("default", activeByDefault = Some(true), dependencies = Seq(
          Dependency(Module("org.escalier", "librairie-standard"), "2.11.6"))))),

    Project(Module("com.github.dummy", "libb"), "0.4.2",
      dependencies = Seq(
        Dependency(Module("org.scalaverification", "scala-verification"), "1.12.4")),
      profiles = Seq(
        Profile("default", activeByDefault = Some(true), dependencies = Seq(
          Dependency(Module("org.escalier", "librairie-standard"), "2.11.6"),
          Dependency(Module("org.scalaverification", "scala-verification"), "1.12.4", scope = Scope.Test))))),

    Project(Module("com.github.dummy", "libb"), "0.5.3",
      properties = Map("special" -> "true"),
      profiles = Seq(
        Profile("default", activation = Profile.Activation(properties = Seq("special" -> None)), dependencies = Seq(
          Dependency(Module("org.escalier", "librairie-standard"), "2.11.6"))))),

    Project(Module("com.github.dummy", "libb"), "0.5.4",
      properties = Map("special" -> "true"),
      profiles = Seq(
        Profile("default", activation = Profile.Activation(properties = Seq("special" -> Some("true"))), dependencies = Seq(
          Dependency(Module("org.escalier", "librairie-standard"), "2.11.6"))))),

    Project(Module("com.github.dummy", "libb"), "0.5.5",
      properties = Map("special" -> "true"),
      profiles = Seq(
        Profile("default", activation = Profile.Activation(properties = Seq("special" -> Some("!false"))), dependencies = Seq(
          Dependency(Module("org.escalier", "librairie-standard"), "2.11.6"))))),

    Project(Module("com.github.dummy", "libb-parent"), "0.5.6",
      properties = Map("special" -> "true")),

    Project(Module("com.github.dummy", "libb"), "0.5.6",
      parent = Some(Module("com.github.dummy", "libb-parent"), "0.5.6"),
      properties = Map("special" -> "true"),
      profiles = Seq(
        Profile("default", activation = Profile.Activation(properties = Seq("special" -> Some("!false"))), dependencies = Seq(
          Dependency(Module("org.escalier", "librairie-standard"), "2.11.6"))))),

    Project(Module("an-org", "a-name"), "1.0"),

    Project(Module("an-org", "a-lib"), "1.0",
      Seq(Dependency(Module("an-org", "a-name"), "1.0"))),

    Project(Module("an-org", "another-lib"), "1.0",
      Seq(Dependency(Module("an-org", "a-name"), "1.0"))),

    // Must bring transitively an-org:a-name, as an optional dependency
    Project(Module("an-org", "an-app"), "1.0",
      Seq(
        Dependency(Module("an-org", "a-lib"), "1.0", exclusions = Set(("an-org", "a-name"))),
        Dependency(Module("an-org", "another-lib"), "1.0", optional = true)))
  )

  val projectsMap = projects.map(p => p.moduleVersion -> p).toMap
  val testRepository: Repository = new TestRepository(projectsMap)

  val repositories = Seq[Repository](
    testRepository
  )

  val tests = TestSuite {
    'empty{
      async{
        val res = await(resolve0(
          Set.empty
        ))

        assert(res == Resolution.empty)
      }
    }
    'notFound{
      async {
        val dep = Dependency(Module("acme", "playy"), "2.4.0")
        val res = await(resolve0(
          Set(dep)
        ))

        val expected = Resolution(
          rootDependencies = Set(dep),
          dependencies = Set(dep.withCompileScope),
          errorCache = Map(dep.moduleVersion -> Seq("Not found"))
        )

        assert(res == expected)
      }
    }
    'single{
      async {
        val dep = Dependency(Module("acme", "config"), "1.3.0")
        val res = await(resolve0(
          Set(dep)
        ))

        val expected = Resolution(
          rootDependencies = Set(dep),
          dependencies = Set(dep.withCompileScope),
          projectCache = Map(dep.moduleVersion -> (testRepository, projectsMap(dep.moduleVersion)))
        )

        assert(res == expected)
      }
    }
    'oneTransitiveDependency{
      async {
        val dep = Dependency(Module("acme", "play"), "2.4.0")
        val trDep = Dependency(Module("acme", "play-json"), "2.4.0")
        val res = await(resolve0(
          Set(dep)
        ))

        val expected = Resolution(
          rootDependencies = Set(dep),
          dependencies = Set(dep.withCompileScope, trDep.withCompileScope),
          projectCache = Map(
            projectsMap(dep.moduleVersion).kv,
            projectsMap(trDep.moduleVersion).kv
          )
        )

        assert(res == expected)
      }
    }
    'twoTransitiveDependencyWithProps{
      async {
        val dep = Dependency(Module("acme", "play"), "2.4.1")
        val trDeps = Seq(
          Dependency(Module("acme", "play-json"), "2.4.0"),
          Dependency(Module("acme", "config"), "1.3.0")
        )
        val res = await(resolve0(
          Set(dep)
        ))

        val expected = Resolution(
          rootDependencies = Set(dep),
          dependencies = Set(dep.withCompileScope) ++ trDeps.map(_.withCompileScope),
          projectCache = Map(
            projectsMap(dep.moduleVersion).kv
          ) ++ trDeps.map(trDep => projectsMap(trDep.moduleVersion).kv)
        )

        assert(res == expected)
      }
    }
    'exclude{
      async {
        val dep = Dependency(Module("acme", "play-extra-no-config"), "2.4.1")
        val trDeps = Seq(
          Dependency(Module("acme", "play"), "2.4.1",
            exclusions = Set(("acme", "config"))),
          Dependency(Module("acme", "play-json"), "2.4.0",
            exclusions = Set(("acme", "config")))
        )
        val res = await(resolve0(
          Set(dep)
        ))

        val expected = Resolution(
          rootDependencies = Set(dep),
          dependencies = Set(dep.withCompileScope) ++ trDeps.map(_.withCompileScope),
          projectCache = Map(
            projectsMap(dep.moduleVersion).kv
          ) ++ trDeps.map(trDep => projectsMap(trDep.moduleVersion).kv)
        )

        assert(res == expected)
      }
    }
    'excludeOrgWildcard{
      async {
        val dep = Dependency(Module("acme", "play-extra-no-config-no"), "2.4.1")
        val trDeps = Seq(
          Dependency(Module("acme", "play"), "2.4.1",
            exclusions = Set(("*", "config"))),
          Dependency(Module("acme", "play-json"), "2.4.0",
            exclusions = Set(("*", "config")))
        )
        val res = await(resolve0(
          Set(dep)
        ))

        val expected = Resolution(
          rootDependencies = Set(dep),
          dependencies = Set(dep.withCompileScope) ++ trDeps.map(_.withCompileScope),
          projectCache = Map(
            projectsMap(dep.moduleVersion).kv
          ) ++ trDeps.map(trDep => projectsMap(trDep.moduleVersion).kv)
        )

        assert(res == expected)
      }
    }
    'filter{
      async {
        val dep = Dependency(Module("hudsucker", "mail"), "10.0")
        val res = await(resolve0(
          Set(dep),
          filter = Some(_.scope == Scope.Compile)
        )).copy(filter = None)

        val expected = Resolution(
          rootDependencies = Set(dep),
          dependencies = Set(dep.withCompileScope),
          projectCache = Map(
            projectsMap(dep.moduleVersion).kv
          )
        )

        assert(res == expected)
      }
    }
    'parentDepMgmt{
      async {
        val dep = Dependency(Module("se.ikea", "billy"), "18.0")
        val trDeps = Seq(
          Dependency(Module("acme", "play"), "2.4.0",
            exclusions = Set(("acme", "play-json")))
        )
        val res = await(resolve0(
          Set(dep),
          filter = Some(_.scope == Scope.Compile)
        )).copy(filter = None, projectCache = Map.empty)

        val expected = Resolution(
          rootDependencies = Set(dep),
          dependencies = Set(dep.withCompileScope) ++ trDeps.map(_.withCompileScope)
        )

        assert(res == expected)
      }
    }
    'parentDependencies{
      async {
        val dep = Dependency(Module("org.gnome", "panel-legacy"), "7.0")
        val trDeps = Seq(
          Dependency(Module("org.gnu", "glib"), "13.4"),
          Dependency(Module("org.gnome", "desktop"), "7.0"))
        val res = await(resolve0(
          Set(dep),
          filter = Some(_.scope == Scope.Compile)
        )).copy(filter = None, projectCache = Map.empty, errorCache = Map.empty)

        val expected = Resolution(
          rootDependencies = Set(dep),
          dependencies = Set(dep.withCompileScope) ++ trDeps.map(_.withCompileScope)
        )

        assert(res == expected)
      }
    }
    'propertiesInExclusions{
      async {
        val dep = Dependency(Module("com.mailapp", "mail-client"), "2.1")
        val trDeps = Seq(
          Dependency(Module("gov.nsa", "secure-pgp"), "10.0", exclusions = Set(("*", "crypto"))))
        val res = await(resolve0(
          Set(dep),
          filter = Some(_.scope == Scope.Compile)
        )).copy(filter = None, projectCache = Map.empty, errorCache = Map.empty)

        val expected = Resolution(
          rootDependencies = Set(dep),
          dependencies = Set(dep.withCompileScope) ++ trDeps.map(_.withCompileScope)
        )

        assert(res == expected)
      }
    }
    'depMgmtInParentDeps{
      async {
        val dep = Dependency(Module("com.thoughtworks.paranamer", "paranamer"), "2.6")
        val res = await(resolve0(
          Set(dep),
          filter = Some(_.scope == Scope.Compile)
        )).copy(filter = None, projectCache = Map.empty, errorCache = Map.empty)

        val expected = Resolution(
          rootDependencies = Set(dep),
          dependencies = Set(dep.withCompileScope)
        )

        assert(res == expected)
      }
    }
    'depsFromDefaultProfile{
      async {
        val dep = Dependency(Module("com.github.dummy", "libb"), "0.3.3")
        val trDeps = Seq(
          Dependency(Module("org.escalier", "librairie-standard"), "2.11.6"))
        val res = await(resolve0(
          Set(dep),
          filter = Some(_.scope == Scope.Compile)
        )).copy(filter = None, projectCache = Map.empty, errorCache = Map.empty)

        val expected = Resolution(
          rootDependencies = Set(dep),
          dependencies = Set(dep.withCompileScope) ++ trDeps.map(_.withCompileScope)
        )

        assert(res == expected)
      }
    }
    'depsFromPropertyActivatedProfile{
      val f =
        for (version <- Seq("0.5.3", "0.5.4", "0.5.5", "0.5.6")) yield {
          async {
            val dep = Dependency(Module("com.github.dummy", "libb"), version)
            val trDeps = Seq(
              Dependency(Module("org.escalier", "librairie-standard"), "2.11.6"))
            val res = await(resolve0(
              Set(dep),
              filter = Some(_.scope == Scope.Compile)
            )).copy(filter = None, projectCache = Map.empty, errorCache = Map.empty)

            val expected = Resolution(
              rootDependencies = Set(dep),
              dependencies = Set(dep.withCompileScope) ++ trDeps.map(_.withCompileScope)
            )

            assert(res == expected)
          }
        }

      scala.concurrent.Future.sequence(f)
    }
    'depsScopeOverrideFromProfile{
      async {
        // Like com.google.inject:guice:3.0 with org.sonatype.sisu.inject:cglib
        val dep = Dependency(Module("com.github.dummy", "libb"), "0.4.2")
        val trDeps = Seq(
          Dependency(Module("org.escalier", "librairie-standard"), "2.11.6"))
        val res = await(resolve0(
          Set(dep),
          filter = Some(_.scope == Scope.Compile)
        )).copy(filter = None, projectCache = Map.empty, errorCache = Map.empty)

        val expected = Resolution(
          rootDependencies = Set(dep),
          dependencies = Set(dep.withCompileScope) ++ trDeps.map(_.withCompileScope)
        )

        assert(res == expected)
      }
    }

    'exclusionsAndOptionalShouldGoAlong{
      async {
        val dep = Dependency(Module("an-org", "an-app"), "1.0")
        val trDeps = Seq(
          Dependency(Module("an-org", "a-lib"), "1.0", exclusions = Set(("an-org", "a-name"))),
          Dependency(Module("an-org", "another-lib"), "1.0", optional = true),
          Dependency(Module("an-org", "a-name"), "1.0", optional = true))
        val res = await(resolve0(
          Set(dep),
          filter = Some(_.scope == Scope.Compile)
        )).copy(filter = None, projectCache = Map.empty, errorCache = Map.empty)

        val expected = Resolution(
          rootDependencies = Set(dep),
          dependencies = Set(dep.withCompileScope) ++ trDeps.map(_.withCompileScope)
        )

        assert(res == expected)
      }
    }

    'exclusionsOfDependenciesFromDifferentPathsShouldNotCollide{
      async {
        val deps = Set(
          Dependency(Module("an-org", "an-app"), "1.0"),
          Dependency(Module("an-org", "a-lib"), "1.0", optional = true))
        val trDeps = Seq(
          Dependency(Module("an-org", "a-lib"), "1.0", exclusions = Set(("an-org", "a-name"))),
          Dependency(Module("an-org", "another-lib"), "1.0", optional = true),
          Dependency(Module("an-org", "a-name"), "1.0", optional = true))
        val res = await(resolve0(
          deps,
          filter = Some(_.scope == Scope.Compile)
        )).copy(filter = None, projectCache = Map.empty, errorCache = Map.empty)

        val expected = Resolution(
          rootDependencies = deps,
          dependencies = (deps ++ trDeps).map(_.withCompileScope)
        )

        assert(res == expected)
      }
    }

    'parts{
      'propertySubstitution{
        val res =
          core.Resolution.withProperties(
            Seq(Dependency(Module("a-company", "a-name"), "${a.property}")),
            Map("a.property" -> "a-version"))
        val expected = Seq(Dependency(Module("a-company", "a-name"), "a-version"))

        assert(res == expected)
      }
    }
  }

}
