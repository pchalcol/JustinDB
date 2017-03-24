package justin.db

import java.util.UUID

import justin.consistent_hashing.NodeId
import justin.db.StorageNodeActorProtocol.StorageNodeReadingResult
import justin.db.storage.PluggableStorageProtocol.{DataOriginality, StorageGetData}
import justin.vector_clocks.VectorClock
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{FlatSpec, Matchers}
import storage.GetStorageProtocol

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}

class ReplicaLocalReaderTest extends FlatSpec with Matchers with ScalaFutures {

  behavior of "Replica Local Reader"

  it should "found data for existing key" in {
    // given
    val id   = UUID.randomUUID()
    val data = Data(id, "value", VectorClock[NodeId]().increase(NodeId(1)))
    val service = new ReplicaLocalReader(new GetStorageProtocol {
      override def get(id: UUID)(resolveOriginality: (UUID) => DataOriginality)(implicit ec: ExecutionContext): Future[StorageGetData] = {
        Future.successful(StorageGetData.Single(data))
      }
    })

    // when
    val result = service.apply(id, null)

    // then
    whenReady(result) { _ shouldBe StorageNodeReadingResult.Found(data) }
  }

  it should "not found data for non-existing key" in {
    // given
    val id = UUID.randomUUID()
    val service = new ReplicaLocalReader(new GetStorageProtocol {
      override def get(id: UUID)(resolveOriginality: (UUID) => DataOriginality)(implicit ec: ExecutionContext): Future[StorageGetData] = {
        Future.successful(StorageGetData.None)
      }
    })

    // when
    val result = service.apply(id, null)

    // then
    whenReady(result) { _ shouldBe StorageNodeReadingResult.NotFound }
  }

  it should "recover failure reading" in {
    // given
    val id = UUID.randomUUID()
    val service = new ReplicaLocalReader(new GetStorageProtocol {
      override def get(id: UUID)(resolveOriginality: (UUID) => DataOriginality)(implicit ec: ExecutionContext): Future[StorageGetData] = Future.failed(new Exception)
    })

    // when
    val result = service.apply(id, null)

    // then
    whenReady(result) { _ shouldBe StorageNodeReadingResult.FailedRead }
  }
}
