package io.scalechain.blockchain.net.message

import io.scalechain.blockchain.proto.InvVector
import io.scalechain.blockchain.proto.GetData

/**
  *  The factory that creates GetData messages.
  */
object GetDataFactory {
  fun create(inventories:List<InvVector>) : GetData {
    return GetData(inventories)
  }
}
