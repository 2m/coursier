package coursier

import coursier.ivy.IvyRepository
import sbt.mavenint.SbtPomExtraProperties
import sbt.{ Resolver, CrossVersion, ModuleID }

object FromSbt {

  def sbtModuleIdName(
    moduleId: ModuleID,
    scalaVersion: => String,
    scalaBinaryVersion: => String
  ): String =
    sbtCrossVersionName(moduleId.name, moduleId.crossVersion, scalaVersion, scalaBinaryVersion)

  def sbtCrossVersionName(
    name: String,
    crossVersion: CrossVersion,
    scalaVersion: => String,
    scalaBinaryVersion: => String
  ): String = crossVersion match {
    case CrossVersion.Disabled => name
    case f: CrossVersion.Full => name + "_" + f.remapVersion(scalaVersion)
    case f: CrossVersion.Binary => name + "_" + f.remapVersion(scalaBinaryVersion)
  }

  def mappings(mapping: String): Seq[(String, String)] =
    mapping.split(';').flatMap { m =>
      val (froms, tos) = m.split("->", 2) match {
        case Array(from) => (from, "default(compile)")
        case Array(from, to) => (from, to)
      }

      for {
        from <- froms.split(',')
        to <- tos.split(',')
      } yield (from.trim, to.trim)
    }

  def attributes(attr: Map[String, String]): Map[String, String] =
    attr.map { case (k, v) =>
      k.stripPrefix("e:") -> v
    }.filter { case (k, _) =>
      !k.startsWith(SbtPomExtraProperties.POM_INFO_KEY_PREFIX)
    }

  def dependencies(
    module: ModuleID,
    scalaVersion: String,
    scalaBinaryVersion: String
  ): Seq[(String, Dependency)] = {

    // TODO Warn about unsupported properties in `module`

    val fullName = sbtModuleIdName(module, scalaVersion, scalaBinaryVersion)

    val dep = Dependency(
      Module(module.organization, fullName, FromSbt.attributes(module.extraDependencyAttributes)),
      module.revision,
      exclusions = module.exclusions.map { rule =>
        // FIXME Other `rule` fields are ignored here
        (rule.organization, rule.name)
      }.toSet
    )

    val mapping = module.configurations.getOrElse("compile")
    val allMappings = mappings(mapping)

    val attributes =
      if (module.explicitArtifacts.isEmpty)
        Seq(Attributes())
      else
        module.explicitArtifacts.map { a =>
          Attributes(`type` = a.extension, classifier = a.classifier.getOrElse(""))
        }

    for {
      (from, to) <- allMappings.toSeq
      attr <- attributes
    } yield from -> dep.copy(configuration = to, attributes = attr)
  }

  def project(
    projectID: ModuleID,
    allDependencies: Seq[ModuleID],
    ivyConfigurations: Map[String, Seq[String]],
    scalaVersion: String,
    scalaBinaryVersion: String
  ): Project = {

    // FIXME Ignored for now - easy to support though
    // val sbtDepOverrides = dependencyOverrides.value
    // val sbtExclusions = excludeDependencies.value

    val deps = allDependencies.flatMap(dependencies(_, scalaVersion, scalaBinaryVersion))

    Project(
      Module(
        projectID.organization,
        sbtModuleIdName(projectID, scalaVersion, scalaBinaryVersion),
        FromSbt.attributes(projectID.extraDependencyAttributes)
      ),
      projectID.revision,
      deps,
      ivyConfigurations,
      None,
      Nil,
      Map.empty,
      Nil,
      None,
      None,
      Nil,
      Info.empty
    )
  }

  def repository(resolver: Resolver, ivyProperties: Map[String, String]): Option[Repository] =
    resolver match {
      case sbt.MavenRepository(_, root) =>
        if (root.startsWith("http://") || root.startsWith("https://")) {
          val root0 = if (root.endsWith("/")) root else root + "/"
          Some(MavenRepository(root0, sbtAttrStub = true))
        } else {
          Console.err.println(s"Warning: unrecognized Maven repository protocol in $root, ignoring it")
          None
        }

      case sbt.FileRepository(_, _, patterns)
        if patterns.ivyPatterns.lengthCompare(1) == 0 &&
          patterns.ivyPatterns == patterns.artifactPatterns =>

        Some(IvyRepository(
          "file://" + patterns.ivyPatterns.head,
          changing = Some(true),
          properties = ivyProperties
        ))

      case sbt.URLRepository(_, patterns)
        if patterns.ivyPatterns.lengthCompare(1) == 0 &&
          patterns.ivyPatterns == patterns.artifactPatterns =>

        Some(IvyRepository(
          patterns.ivyPatterns.head,
          changing = None,
          properties = ivyProperties
        ))

      case other =>
        Console.err.println(s"Warning: unrecognized repository ${other.name}, ignoring it")
        None
    }

}
