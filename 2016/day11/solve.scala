import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
import scala.io.Source

trait Item {def kind: Int}
case class Generator(kind:Int) extends Item
case class Microchip(kind:Int) extends Item

class Elements {
  var Lim = 1
  private var elementToKind: Map[String, Int] = Map()

  def getKind(name: String): Int = {
    if (!elementToKind.contains(name)) {
      elementToKind += ((name, Lim))
      Lim *= 2
    }
    elementToKind(name)
  }
}

case class Level(ilevel:Int, generators:Int, microchips:Int) {
  def addGenerator(generator: Generator): Level =
    Level(ilevel, generators + generator.kind, microchips)

  def removeGenerator(generator: Generator): Level =
    Level(ilevel, generators & ~generator.kind, microchips)

  def addMicrochip(microchip: Microchip): Level =
    Level(ilevel, generators, microchips | microchip.kind)

  def removeMicrochip(microchip: Microchip): Level =
    Level(ilevel, generators, microchips & ~microchip.kind)

  def isEmpty: Boolean = generators == 0 && microchips == 0

  def isValid: Boolean = generators == 0 || microchips - (microchips & generators) == 0

  def items(): List[Item] = {
    var res: List[Item] = Nil
    var i = 1
    while (i <= generators || i <= microchips) {
      if ((generators & i) != 0)
        res = Generator(i) :: res
      if ((microchips & i) != 0)
        res = Microchip(i) :: res
      i *= 2
    }
    res
  }

}

case class Building(levels: Array[Level], elevator:Int) {
  override def toString: String = {
    val stLevels = levels.map(items => items.toString).mkString("\n")
    s"$stLevels\nElevator: $elevator"
  }

  def adjustLevels(levelA: Level, levelB: Level): Array[Level] = {
    val newLevels = levels.clone()
    newLevels(levelA.ilevel) = levelA
    newLevels(levelB.ilevel) = levelB
    newLevels
  }

  def move(items: List[Item], elevatorSrc: Int, elevatorDst: Int): Option[Building] = {
    if (elevatorDst < 0 || elevatorDst >= levels.length)
      None
    else {
      var levelSrc = levels(elevatorSrc)
      var levelDst = levels(elevatorDst)
      for (item <- items) {
        item match {
          case generator@Generator(_) =>
            levelSrc = levelSrc.removeGenerator(generator)
            levelDst = levelDst.addGenerator(generator)

          case microchip@Microchip(_) =>
            levelSrc = levelSrc.removeMicrochip(microchip)
            levelDst = levelDst.addMicrochip(microchip)
        }
      }
      if (levelSrc.isValid && levelDst.isValid)
        Some(Building(adjustLevels(levelSrc, levelDst), elevatorDst))
      else
        None
    }
  }

  def steps(): List[Building] = {
    val single = for {
      item <- levels(elevator).items()
      elevatorDst <- List(elevator + 1, elevator - 1)
      optionalBuilding = move(List(item), elevator, elevatorDst)
      if optionalBuilding.isDefined
    } yield optionalBuilding.get

    val double = for {
      itemA <- levels(elevator).items()
      itemB <- levels(elevator).items()
      if itemA != itemB
      elevatorDst <- List(elevator + 1, elevator - 1)
      optionalBuilding = move(List(itemA, itemB), elevator, elevatorDst)
      if optionalBuilding.isDefined
    } yield optionalBuilding.get

    single ::: double
  }

  def key(elements: Elements): BigInt =
    levels.foldLeft(BigInt(elevator)) {
      case (res, level) => res * elements.Lim * elements.Lim + (level.generators * elements.Lim + level.microchips)
    }
}

case object Day11 extends App {

  val regexMicrochip = """([^ ]*)-compatible""".r
  val regexGenerator = """([^ ]*) generator""".r

  def parse(levelDescriptions:Array[String], elements: Elements):Building = {
    val levels: ArrayBuffer[Level] = ArrayBuffer()
    for (levelDescription <- levelDescriptions) {
      val microchipKinds = regexMicrochip.findAllMatchIn(levelDescription).map(m => m.group(1))
      val generatorKinds = regexGenerator.findAllMatchIn(levelDescription).map(m => m.group(1))

      val microchips = microchipKinds.foldLeft(0) {
        case (res, element) => res | elements.getKind(element)
      }

      val generators = generatorKinds.foldLeft(0) {
        case (res, element) => res | elements.getKind(element)
      }

      levels.append(Level(levels.length, generators, microchips))
    }
    Building(levels.toArray, 0)
  }

  def solve(input:Array[String]): Int = {
    val elements = new Elements()
    val queue = mutable.Queue((0, parse(input, elements)))
    var seen: Set[BigInt] = Set()

    while (queue.nonEmpty) {
      val (distance, building) = queue.dequeue()
      val key = building.key(elements)

      if (!seen.contains(key)) {

        seen += key

        if (building.levels.forall(level => level.ilevel == building.levels.length - 1 || level.isEmpty))
          return distance

        for (buildingNew <- building.steps())
          queue.enqueue((distance + 1, buildingNew))

      }
    }
    -1
  }

  println(solve(Source.fromFile(productPrefix.toLowerCase + "/input.in").getLines().toArray))
  println(solve(Source.fromFile(productPrefix.toLowerCase + "/input2.in").getLines().toArray))
}
