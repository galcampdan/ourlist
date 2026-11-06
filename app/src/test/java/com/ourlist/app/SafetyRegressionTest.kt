package com.ourlist.app

import com.ourlist.app.data.backup.*
import com.ourlist.app.data.model.*
import com.ourlist.app.data.sync.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Test

class SafetyRegressionTest {
    private val base = BucketItem(id="item",collection=BucketCollection.SHARED,title="old title",memo="old memo",updatedAt=1)
    private fun update(snapshot:BucketItem,fields:String,before:BucketItem?=base,resolution:Boolean=false) = SyncOperation(
        opId="op",collection=snapshot.collection,deviceId="remote",actorSub="",actorName="partner",itemId=snapshot.id,
        kind=OpKind.UPDATE_FIELD,fieldPath=fields,itemSnapshotJson=Json.encodeToString(snapshot),baseSnapshotJson=before?.let {Json.encodeToString(it)},resolvesConflict=resolution)
    @Test fun unrelatedFieldEditCannotRollBackUploadedTitle() {
        val local=base.copy(title="new local title")
        val result=FieldMerge.merge(local,update(base.copy(memo="new remote memo"),"memo"),emptySet())
        assertEquals("new local title",result.item.title);assertEquals("new remote memo",result.item.memo);assertTrue(result.conflicts.isEmpty())
    }
    @Test fun concurrentSameFieldIsDetectedAfterLocalUpload() {
        val result=FieldMerge.merge(base.copy(title="mine"),update(base.copy(title="theirs"),"title"),emptySet())
        assertEquals("mine",result.item.title);assertEquals(listOf("title"),result.conflicts.map {it.path})
    }
    @Test fun explicitResolutionPropagatesChosenValue() {
        val result=FieldMerge.merge(base.copy(title="mine"),update(base.copy(title="chosen"),"title",resolution=true),setOf("title"))
        assertEquals("chosen",result.item.title);assertTrue(result.conflicts.isEmpty())
    }
    @Test fun checklistDeletionDoesNotReappear() {
        val old=base.copy(checklist=listOf(ChecklistItem("c","remove me")))
        val result=FieldMerge.merge(old,update(old.copy(checklist=emptyList()),"checklist",old),emptySet())
        assertTrue(result.item.checklist.isEmpty())
    }
    @Test fun legacyFieldPathStillRestrictsSnapshot() {
        val result=FieldMerge.merge(base.copy(title="mine"),update(base.copy(memo="remote"),"memo",null),emptySet())
        assertEquals("mine",result.item.title)
    }
    @Test fun moveFromTrashUsesDestinationWithoutLosingLocalEdits() {
        val destination=base.copy(collection=BucketCollection.HEART)
        val op=update(destination,"title").copy(kind=OpKind.CREATE,moveId="move")
        val result=createOrMove(base.copy(title="my edit",deletedAt=10),op)!!
        assertEquals(BucketCollection.HEART,result.collection);assertNull(result.deletedAt);assertEquals("my edit",result.title)
    }
    @Test fun operationReplayKeepsDeviceSequenceDespiteClockSkew() {
        val first=update(base,"title").copy(opId="first",deviceSequence=1,createdAt=200)
        val second=first.copy(opId="second",deviceSequence=2,createdAt=100)
        assertEquals(listOf("first","second"),orderedOperations(listOf(second,first)).map {it.opId})
    }
    @Test fun manualBackupOpensWithPasswordAndRecoveryCode() {
        val encrypted=CryptoEngine.encryptBackup("private data","password","RECOVERY")
        assertEquals("private data",CryptoEngine.decryptBackup(encrypted,"password"))
        assertEquals("private data",CryptoEngine.decryptBackup(encrypted,"RECOVERY"))
        assertTrue(runCatching {CryptoEngine.decryptBackup(encrypted,"wrong")}.isFailure)
    }
    @Test fun oldBackupStillUsesOriginalPassword() {
        assertEquals("old data",CryptoEngine.decryptBackup(CryptoEngine.encrypt("old data","old-password"),"old-password"))
    }
    @Test fun backupEnvelopeTamperFailsAuthentication() {
        val encrypted=CryptoEngine.encryptBackup("private","password","recovery")
        val envelope=Json.decodeFromString<Map<String,String>>(encrypted).toMutableMap()
        envelope["payload"]=CryptoEngine.encrypt("tampered","unknown-key")
        assertTrue(runCatching {CryptoEngine.decryptBackup(Json.encodeToString(envelope),"password")}.isFailure)
    }
    @Test fun exportDefaultScopeExcludesPrivateSecretAndTrash() {
        val items=BucketCollection.entries.map {base.copy(id=it.name,collection=it)}+base.copy(id="trash",deletedAt=10)
        val exported=ExportManager.selectItems(items,setOf(BucketCollection.SHARED,BucketCollection.HEART))
        assertEquals(setOf("SHARED","HEART"),exported.map {it.id}.toSet())
        assertEquals(1,ExportManager.selectItems(items,setOf(BucketCollection.SECRET_LOCAL)).size)
    }
    @Test fun operationBeforeImageSurvivesDatabaseRoundTrip() {
        val op=update(base.copy(title="after"),"title")
        assertEquals(op,com.ourlist.app.data.local.entity.SyncOperationEntity.fromDomain(op).toDomain())
    }
}
