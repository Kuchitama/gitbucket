package service

import java.io.IOException
import java.net.{Proxy, SocketAddress, URI, ProxySelector, InetSocketAddress}

import org.slf4j.LoggerFactory

trait ProxyService {

  protected val logger = LoggerFactory.getLogger(this.getClass)

  private val proxyHost = Option(System.getProperty("http.proxyHost"))
  private val proxyPort = Option(System.getProperty("http.proxyPort").toInt)

  protected val proxy:Proxy = {
    (for(host <- proxyHost; port <- proxyPort) yield {
      val socket = InetSocketAddress.createUnresolved(host, port)
      new Proxy(Proxy.Type.HTTP, socket)
    }).getOrElse(Proxy.NO_PROXY)
  }

  lazy val isUseProxy = proxy != Proxy.NO_PROXY

  lazy val proxyAddress:String = Option(proxy.address).map(_.toString).getOrElse("")

}

object ProxyService extends ProxyService {
  def setProxy() {
    ProxySelector.setDefault(new ProxySelector {

      import scala.collection.JavaConverters._

      override def select(uri: URI): java.util.List[Proxy] = {
        logger.info(s"proxy set: ${proxyAddress}")
        List(proxy).asJava
      }

      override def connectFailed(uri: URI, socketAddress: SocketAddress, e: IOException): Unit = {
        if (uri == null || socketAddress == null || e == null) {
          throw new IllegalArgumentException("Arguments can not be null.")
        }
      }
    })
  }
}
