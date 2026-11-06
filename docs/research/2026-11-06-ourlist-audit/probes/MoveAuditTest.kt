class MoveAuditTest {
    @org.junit.Test fun destinationCreateAfterMoveIsIgnoredWhenSameIdExistsInTrash() {
        val unsafeField = Class.forName("sun.misc.Unsafe").getDeclaredField("theUnsafe").apply { isAccessible = true }
        val unsafe = unsafeField.get(null)
        val engine: Any = unsafe.javaClass.getMethod("allocateInstance", Class::class.java).invoke(unsafe, com.ourlist.app.data.sync.SyncEngine::class.java)
        val oldItem = com.ourlist.app.data.model.BucketItem(id="moving-item", collection=com.ourlist.app.data.model.BucketCollection.SHARED, title="audit move", deletedAt=100L)
        val stored = com.ourlist.app.data.local.entity.BucketItemEntity.fromDomain(oldItem)
        var writes = 0
        val bucket = java.lang.reflect.Proxy.newProxyInstance(javaClass.classLoader, arrayOf(com.ourlist.app.data.local.dao.BucketDao::class.java)) { _, method, _ ->
            when(method.name) { "getItemById" -> stored; "upsert" -> { writes++; kotlin.Unit }; else -> error("Unexpected DAO call: ${method.name}") }
        }
        val syncType = Class.forName("com.ourlist.app.data.local.dao.SyncOperationDao")
        val sync = java.lang.reflect.Proxy.newProxyInstance(javaClass.classLoader, arrayOf(syncType)) { _, method, _ ->
            if(method.name == "getPendingOpsForItem") emptyList<com.ourlist.app.data.local.entity.SyncOperationEntity>() else error(method.name)
        }
        engine.javaClass.getDeclaredField("bucketDao").apply { isAccessible=true }.set(engine,bucket)
        engine.javaClass.getDeclaredField("syncDao").apply { isAccessible=true }.set(engine,sync)
        val moved = oldItem.copy(collection=com.ourlist.app.data.model.BucketCollection.HEART, deletedAt=null)
        val snapshot = kotlinx.serialization.json.Json.encodeToString(com.ourlist.app.data.model.BucketItem.serializer(), moved)
        val operation = com.ourlist.app.data.model.SyncOperation(opId="move-create",collection=moved.collection,deviceId="partner",actorSub="",actorName="audit",itemId=moved.id,kind=com.ourlist.app.data.model.OpKind.CREATE,itemSnapshotJson=snapshot,moveId="move-1")
        val method = engine.javaClass.getDeclaredMethod("applyRemoteOperation",com.ourlist.app.data.model.SyncOperation::class.java,kotlin.coroutines.Continuation::class.java).apply { isAccessible=true }
        val continuation = object:kotlin.coroutines.Continuation<Unit> {
            override val context = kotlin.coroutines.EmptyCoroutineContext
            override fun resumeWith(result:Result<Unit>) { result.getOrThrow() }
        }
        val result = method.invoke(engine,operation,continuation)
        org.junit.Assert.assertNotSame(kotlin.coroutines.intrinsics.COROUTINE_SUSPENDED,result)
        org.junit.Assert.assertEquals(0,writes)
        org.junit.Assert.assertEquals("SHARED",stored.collection)
        org.junit.Assert.assertNotNull(stored.deletedAt)
        println("CONFIRMED: actual SyncEngine ignores HEART CREATE for same-ID trashed SHARED item; moved item stays hidden")
    }
}

