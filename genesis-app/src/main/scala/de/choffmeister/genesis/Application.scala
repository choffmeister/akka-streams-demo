package de.choffmeister.genesis

import akka.actor._
import akka.stream.ActorFlowMaterializer
import akka.stream.scaladsl._

import scala.concurrent.duration._

class Application extends Bootable {
  implicit val system = ActorSystem("genesis")
  implicit val executor = system.dispatcher
  implicit val materializer = ActorFlowMaterializer()

  override def startup(args: List[String]): Unit = {
    val g = Flow() { implicit builder =>
      import FlowGraph.Implicits._

      def extension(ext: String) = Flow[File].filter(_.extension == ext)
      val log = Flow[File].map { f =>
        println(f.path)
        f
      }
      val delay = Flow[File].map { f =>
        Thread.sleep(1000)
        f
      }

      val bcast = builder.add(Broadcast[File](3))
      val merge = builder.add(Merge[File](3))

      bcast ~>                                        merge
      bcast ~> extension(".scala") ~> delay ~> log ~> merge
      bcast ~> extension(".class") ~> delay ~> log ~> merge

      (bcast.in, merge.out)
    }

    val source = FileSource(".", List(".git", ".idea", "target"))
    val done = source.via(g).runForeach(println)

    done.onComplete(_ => shutdown())
  }

  def shutdown(): Unit = {
    system.shutdown()
    system.awaitTermination(1.seconds)
  }
}

object Application extends BootableApp[Application]
