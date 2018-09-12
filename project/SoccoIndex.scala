/*
 * Copyright 2017 Spotify AB.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

object SoccoIndex {

  import java.io.{File, PrintWriter}

  val baseUrl = "https://github.com/spotify/big-data-rosetta-code/blob/master/"

  val mainPath = new File("src/main/scala/com/spotify/bdrc")

  val header =
    """<!DOCTYPE html>
      |<html>
      |  <title>Big Data Rosetta Code</title>
      |  <xmp theme="spacelab" style="display:none;">
    """.stripMargin
  val footer =
    """  </xmp>
      |  <script src="http://strapdownjs.com/v/0.2/strapdown.js"></script>
      |</html>
    """.stripMargin

  def ls(f: File): Array[File] = {
    val fs = f.listFiles()
    fs ++ fs.filter(_.isDirectory).flatMap(ls)
  }

  def isScala(f: File): Boolean = f.isFile && f.getName.endsWith(".scala")

  case class Source(file: String, section: String, title: String, url: String,
                    objects: List[String])

  // Find all source files
  private def sources = {
    val examplePattern = "^\\s*// Example:\\s*(.+)".r
    val objectPattern = "^\\s*object\\s*(\\w+)\\s*\\{?".r
    ls(mainPath)
      .filter(isScala)
      .flatMap { f =>
        val lines = scala.io.Source.fromFile(f).getLines().toList
        val e = lines.filter(_.startsWith("// Example:"))
        if (e.nonEmpty) {
          val section = f.getParent.replaceFirst(mainPath.toString, "") + "/"
          val title = examplePattern.unapplySeq(e.head).get.head
          val objects = lines
            .filter(_.startsWith("object "))
            .map(l => objectPattern.unapplySeq(l).get.head)
          Some(Source(f.getName, section, title, baseUrl + f, objects))
        } else {
          None
        }
      }
  }

  def mappings: Seq[(File, String)] = Seq(
    new File(s"target/socco/index.html") -> s"index.html"
  ) ++ sources.map(s => new File(s"target/socco/${s.file}.html") -> s"${s.file}.html")

  def generate(outFile: File): File = {
    outFile.getParentFile.mkdirs()
    val out = new PrintWriter(outFile)
    out.println(header)
    var section = ""
    sources.sortBy(s => (s.section, s.file)).foreach { s =>
      if (section != s.section) {
        section = s.section
        out.println()
        out.println("### " + section)
        out.println()
      }
      out.println(s"- [${s.file}](${s.file}.html) ([source](${s.url})) - ${s.title}")
    }
    out.println(footer)
    out.close()
    outFile
  }
}