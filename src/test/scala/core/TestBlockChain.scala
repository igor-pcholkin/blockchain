package core

import org.scalatest.mockito.MockitoSugar

class TestBlockChain extends BlockChain("Riga") with MockitoSugar {
  override lazy val chainFileOps: ChainFileOps = mock[ChainFileOps]
}

