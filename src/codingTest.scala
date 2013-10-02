package rapture.implementation

import org.specs2.mutable._

class CodecsSpecsTest extends Specification with Codecs {
  import CodecsSpecsTestImplicits._

  implicit val enc = Encodings.`UTF-8`

  "Base64 codec" should  {

    "should respect rfc4648 encoding test vector" in {
      Base64.encode("f") must_== "Zg=="
      Base64.encode("fo") must_== "Zm8="
      Base64.encode("foob") must_== "Zm9vYg=="
      Base64.encode("fooba") must_== "Zm9vYmE="
    }.pendingUntilFixed

    "respect rfc4648 encoding test vector" in {
      Base64.encode("") must_== ""
      Base64.encode("foo") must_== "Zm9v"
      Base64.encode("foobar") must_== "Zm9vYmFy"
    }

    "respect rfc4648 decoding test vector" in {
      Base64.decode("") must_==  "".raw
      Base64.decode("Zg==") must_== "f".raw
      Base64.decode("Zm8=") must_== "fo".raw
      Base64.decode("Zm9v") must_== "foo".raw
      Base64.decode("Zm9vYg==") must_== "foob".raw
      Base64.decode("Zm9vYmE=") must_== "fooba".raw
      Base64.decode("Zm9vYmFy") must_== "foobar".raw
    }

    "encode decode Hello World" in {
      Base64.decode(Base64.encode("Hello world")) must_== "Hello world"
      Base64.encode(Base64.decode("SGVsbG8gd29ybGQ=")) must_== "SGVsbG8gd29ybGQ="
    }.pendingUntilFixed

    "encode Hello World" in {
      Base64.encode("Hello world") must_== "SGVsbG8gd29ybGQ="
    }.pendingUntilFixed

    "decode Hello World" in {
      Base64.decode("SGVsbG8gd29ybGQ=") must_==  "Hello world".raw
    }

  }

}

object CodecsSpecsTestImplicits {
  implicit class RichString(val s:String) extends AnyVal {
    def raw:Array[Byte] = s.toCharArray.map(_.asInstanceOf[Byte])
  }
}
