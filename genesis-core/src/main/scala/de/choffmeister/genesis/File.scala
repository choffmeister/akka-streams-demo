package de.choffmeister.genesis

import java.io.{File => JavaFile, FileInputStream => JavaFileInputStream}

import akka.stream.scaladsl._
import akka.util.ByteString

case class File(path: String, name: String, extension: String, content: ByteString)

object File {
  def apply(javaFile: JavaFile): File = File(
    javaFile.getPath,
    javaFile.getName,
    extension(javaFile),
    read(javaFile)
  )

  private def read(javaFile: JavaFile): ByteString = {
    val fs = new JavaFileInputStream(javaFile)
    try {
      var done = false
      var result = ByteString.empty
      val buffer = new Array[Byte](8192)
      while (!done) {
        val read = fs.read(buffer)
        if (read > 0) result ++= ByteString.fromArray(buffer, 0, read)
        else done = true
      }
      result
    } finally {
      fs.close()
    }
  }

  private def extension(javaFile: JavaFile): String = {
    val regex = """^.*(\.[^\.]*)$""".r

    javaFile.getName match {
      case regex(ext) => ext
      case _ => ""
    }
  }
}

object FileSource {
  def apply(dir: String, excludeFolders: Seq[String] = Nil): Source[File, Unit] = {
    val iterator = new Iterator[JavaFile] {
      private var queue = Vector(new JavaFile(dir))

      @scala.annotation.tailrec
      def hasNext: Boolean = {
        if (queue.isEmpty) false
        else if (queue.head.isFile) true
        else if (excludeFolders contains queue.head.getName) {
          queue = queue.tail
          hasNext
        } else {
          val current = queue.head
          val children = Option(current.listFiles).getOrElse(Array.empty).toVector
          queue = queue.tail ++ children
          hasNext
        }
      }

      def next(): JavaFile = {
        val current = queue.head
        queue = queue.tail
        current
      }
    }

    Source(() => iterator).map(File.apply)
  }
}
