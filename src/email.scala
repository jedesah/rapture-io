package rapture

import javax.mail._
import javax.mail.internet._
import javax.activation._
import scala.xml._

trait Email { this: BaseIo =>

  object Smtp extends Scheme[Smtp] {
    def /(hostname: String) = new Smtp(hostname)
    def schemeName = "smtp"
  }

  class Smtp(val hostname: String) extends Uri {
    def scheme = Smtp
    def schemeSpecificPart = s"//${hostname}"
    def absolute = true

    def email(from: String, to: Seq[String], cc: Seq[String], subject: String,
        bodyText: String, bodyHtml: Option[(String, Seq[(String, String)])],
        attachments: Seq[(String, String, String)]) = {
    
      val props = System.getProperties()
      props.put("mail.smtp.host", hostname)
      val session = Session.getDefaultInstance(props, null)
      val msg = new MimeMessage(session)
      msg.setFrom(new InternetAddress(from))
      for(r <- to) msg.addRecipient(Message.RecipientType.TO, new InternetAddress(r))
      for(r <- cc) msg.addRecipient(Message.RecipientType.CC, new InternetAddress(r))
      msg.setSubject(subject)

      bodyHtml match {
        case Some(Pair(html, inlines)) => {
          var top = new MimeMultipart("alternative")
          val textPart = new MimeBodyPart()
          textPart.setText(bodyText, "UTF-8")
          top.addBodyPart(textPart)

          val htmlPart = new MimeBodyPart()
          htmlPart.setContent(html, "text/html;charset=UTF-8")
          top.addBodyPart(htmlPart)

          if(inlines.length > 0) {
            val body = new MimeBodyPart()
            body.setContent(top)
            top = new MimeMultipart("related")
            top.addBodyPart(body)
            for(i <- inlines) {
              val relPart = new MimeBodyPart()
              relPart.setDisposition(Part.INLINE)
              relPart.setHeader("Content-ID", "<"+i._1+">")
              val ds = new FileDataSource(i._2)
              ds.setFileTypeMap(MimeTypes.mimeTypesMap)
              relPart.setDataHandler(new DataHandler(ds))
              top.addBodyPart(relPart)
            }
          }

          if(attachments.length > 0) {
            val body = new MimeBodyPart()
            body.setContent(top)
            top = new MimeMultipart("mixed")
            top.addBodyPart(body)
            for(a <- attachments) {
              val attPart = new MimeBodyPart()
              attPart.setDisposition(Part.ATTACHMENT)
              attPart.setFileName(a._1)
              val src = new FileDataSource(a._3) {
                override def getContentType() = a._2
              }
              attPart.setDataHandler(new DataHandler(src))
              top.addBodyPart(attPart)
            }
          }
          msg.setContent(top)
        }
        case None => {
          if(attachments.length > 0) {
            val body = new MimeBodyPart()
            body.setText(bodyText, "UTF-8")
            val top = new MimeMultipart("mixed")
            top.addBodyPart(body)
            for(a <- attachments) {
              val attPart = new MimeBodyPart()
              attPart.setDisposition(Part.ATTACHMENT)
              attPart.setFileName(a._1)
              val src = new FileDataSource(a._3) {
                override def getContentType() = a._2
              }
              attPart.setDataHandler(new DataHandler(src))
              top.addBodyPart(attPart)
            }
            msg.setContent(top)
          } else {
            msg.setText(bodyText, "UTF-8")
          }
        }
      }
      Transport.send(msg)
    }
  }
}


