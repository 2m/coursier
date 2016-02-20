addSbtPlugin("org.xerial.sbt" % "sbt-pack" % "0.6.8")
addSbtPlugin("org.scala-js" % "sbt-scalajs" % "0.6.5")
addSbtPlugin("com.jsuereth" % "sbt-pgp" % "1.0.0")
addSbtPlugin("org.scoverage" % "sbt-scoverage" % "1.1.0")
addSbtPlugin("org.tpolecat" % "tut-plugin" % "0.4.0")
// temporarily disabled on this branch, until a Java 6 compatible coursier
// SBT plugin is released.
// addSbtPlugin("com.github.alexarchambault" % "coursier-sbt-plugin" % "1.0.0-M4")
addSbtPlugin("com.typesafe.sbt" % "sbt-proguard" % "0.2.2")
addSbtPlugin("com.typesafe" % "sbt-mima-plugin" % "0.1.8")
