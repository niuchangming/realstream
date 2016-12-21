name := """RealStream"""

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayJava)

scalaVersion := "2.11.7"
fork in run := false

libraryDependencies ++= Seq(
  javaJdbc,
  cache,
  javaWs,
  javaJpa,
  "mysql" % "mysql-connector-java" % "5.1.38",
  "org.hibernate" % "hibernate-entitymanager" % "5.1.0.Final",
  "com.tokbox" % "opentok-server-sdk" % "2.3.2",
  "com.auth0" % "java-jwt" % "2.2.1",
  "com.monitorjbl" % "json-view" % "0.12",
  "com.amazonaws" % "aws-java-sdk-s3" % "1.11.57",
  "net.coobird" % "thumbnailator" % "[0.4, 0.5)"
)

// Running Play in development mode while using JPA will work fine, 
// but in order to deploy the application you will need to add this to your build.sbt file.
// PlayKeys.externalizeResources := false

EclipseKeys.preTasks := Seq(compile in Compile)
EclipseKeys.projectFlavor := EclipseProjectFlavor.Java
EclipseKeys.createSrc := EclipseCreateSrc.ValueSet(EclipseCreateSrc.ManagedClasses, EclipseCreateSrc.ManagedResources) 
