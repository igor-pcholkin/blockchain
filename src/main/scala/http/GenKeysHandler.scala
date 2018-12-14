package http

import java.io.{File, FileOutputStream, IOException, PrintWriter}

import com.sun.net.httpserver.{HttpExchange, HttpHandler}
import keys.{KeysGenerator, KeysSerializator}
import util.Convert

class GenKeysHandler(nodeName: String, keysFileOps: KeysFileOps, bcHttpServer: BCHttpServer) extends HttpHandler with KeysGenerator with KeysSerializator with Convert{
  @throws[IOException]
  def handle(exchange: HttpExchange): Unit = {
    if (exchange.getRequestMethod == "PUT") {
      if (bcHttpServer.nonEmptyKeys && exchange.getRequestURI().getQuery != "overwrite=true") {
        bcHttpServer.sendHttpResponse(exchange, 400, "Public or private key already exists, use overwrite=true to overwrite")
      } else {
        val keyPair = generateKeyPair()
        if (!keysFileOps.isKeysDirExists(nodeName)) {
          keysFileOps.createKeysDir(nodeName)
        }
        keysFileOps.writeKey(s"$nodeName/privateKey", serialize(keyPair.getPrivate))
        keysFileOps.writeKey(s"$nodeName/publicKey", serialize(keyPair.getPublic))
        bcHttpServer.setKeys(keyPair)
        bcHttpServer.sendHttpResponse(exchange, 201, "New keys have been created")
      }
    } else {
      bcHttpServer.sendHttpResponse(exchange, 400, "Invalid method, use PUT")
    }
  }
}

trait KeysFileOps {
  def writeKey(path: String, key: Array[Byte])
  def isKeysDirExists(nodeName: String): Boolean
  def createKeysDir(nodeName: String): Unit
}

class ProdKeysFileOps extends KeysFileOps {
  override def writeKey(path: String, key: Array[Byte]) = {
    val fos = new FileOutputStream(path)
    fos.write(key)
    fos.close()
  }

  override def isKeysDirExists(nodeName: String): Boolean = {
    val keyDir = new File(nodeName)
    keyDir.exists && keyDir.isDirectory
  }

  override def createKeysDir(nodeName: String) = {
    new File(nodeName).mkdirs()
  }
}

