Scala TODO List
==============
## Stack
- [Http4s](http://http4s.org/) 
- [Circe](https://circe.github.io/circe/)
- [Doobie](https://github.com/tpolecat/doobie)
- [Cats](https://typelevel.org/cats/) 
- [Circe Config](https://github.com/circe/circe-config)

## Getting Started

Be aware that this project targets **Java 11**.

Start up sbt:

```bash
sbt --java-home {your.java.11.location}
```

Once sbt has loaded, you can start up the application

```sbtshell
> ~reStart
```

This uses revolver, which is a great way to develop and test the application.  Doing things this way the application
will be automatically rebuilt when you make code changes

To stop the app in sbt, hit the `Enter` key and then type:

```sbtshell
> reStop
```

# Acknowledgments

https://github.com/pauljamescleary/scala-pet-store was used as the remplate