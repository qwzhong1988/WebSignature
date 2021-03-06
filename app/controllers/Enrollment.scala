package controllers

import play.api._
import play.api.mvc._
import play.api.libs.json._
import play.api.libs.functional.syntax._
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.concurrent._

import models.SignatureModel
import models.User
import be.ac.ulg.montefiore.run.jahmm._

import scala.collection.JavaConversions._
import scala.collection.mutable.ListBuffer
import scala.concurrent.duration._
import scala.concurrent._


object Enrollment extends Controller {

  def enrollmentForm = Action {
    Logger.info("Enrollment form displayed.")
    Ok(views.html.enrollment())
  }

  def verificationForm = Action {
    Logger.info("Verification form displayed.")
    Ok(views.html.verification())
  }
  
  var signatures = ListBuffer[ListBuffer[Array[Double]]]()

  var username : String = ""
  //var model : SignatureModel = new SignatureModel()

  type Position = (Double, Double, Double)

  implicit val positionReads : Reads[Position] = (
    (__ \ 'x).read[Double] and
    (__ \ 'y).read[Double] and
    (__ \ 't).read[Double]
  ) tupled

  implicit val signatureReads = (
    (__ \ 'name).read[String] and
    (__ \ 'signature).read[ListBuffer[Position]]
  ) tupled

  def addEnrollmentSignature = Action(parse.json) { implicit request =>
    Logger.info("Add enrollment signature")
    request.body.validate[(String, ListBuffer[Position])].map{ 
      case (name, signature) => {
        username = name
        val signArray : ListBuffer[Array[Double]] = signature.map(x => Array(x._1, x._2, x._3))
        signatures += signArray
        Ok(Json.obj("name"->name,
          "sampledPoints"->signArray.size,
          "nbSignature"->signatures.size,
          "signature"->signArray))

      }
    }.recoverTotal{
      e => BadRequest("Detected error:"+ JsError.toFlatJson(e))
    }
  }

  def probaSignature = Action(parse.json) { implicit request =>
    Logger.info("Test probab")
    request.body.validate[(String, ListBuffer[Position])].map{ 
      case (name, signature) => {
        val signArray : ListBuffer[Array[Double]] = signature.map(x => Array(x._1, x._2, x._3))
        val aSign : java.util.List[Array[Double]] = new java.util.LinkedList(signArray.map(y => y.map(z => z)))
        val user : User = User.find.where().eq("name", name).findList().get(0)
        val probab = Future{ user.probability(aSign) }

        val timeoutFuture = play.api.libs.concurrent.Promise.timeout("Oops", 20.seconds)
        Async {
          Future.firstCompletedOf(Seq(probab, timeoutFuture)).map { 
            case i: Boolean => Ok(Json.obj("probability"->i))
            case t: String => InternalServerError(t)
          }  
        }
      }
    }.recoverTotal{
      e => BadRequest("Detected error:"+ JsError.toFlatJson(e))
    }
  }

  def enroll = Action {
    Logger.info("Enrolling ...")
    val aSign : java.util.List[java.util.List[Array[Double]]] = new java.util.LinkedList(signatures.map(x => new java.util.LinkedList(x.map(y => y.map(z => z)))))
    
    signatures = ListBuffer[ListBuffer[Array[Double]]]()
    val user : User = new User(username)
    val train = Future{ user.enroll(aSign) }
    val timeoutFuture = play.api.libs.concurrent.Promise.timeout("Oops", 600.seconds)
    Async {
      Future.firstCompletedOf(Seq(train, timeoutFuture)).map { 
        case i: Boolean => {
          User.create(user)
          Ok("Enrollment successful")
        }
        case t: String => InternalServerError(t)
      }  
    }
  }

}