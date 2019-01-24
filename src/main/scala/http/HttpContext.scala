package http

import core.{BlockChain, StatementsCache}
import keys.KeysFileOps
import peers.PeerAccess
import util.FileOps

case class HttpContext(nodeName: String, bcHttpServer: BCHttpServer, blockChain: BlockChain,
    statementsCache: StatementsCache, peerAccess: PeerAccess, keysFileOps: KeysFileOps, fileOps: FileOps)
