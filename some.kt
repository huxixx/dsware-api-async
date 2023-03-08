import org.springframework.transaction.support.TransactionSynchronizationManager
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.ThreadContextElement

class TransactionSynchronizationManagerThreadContextElement :
    ThreadContextElement<Map<String, Any?>> {
    // declare companion object for a key of this element in coroutine context
    companion object Key :
        CoroutineContext.Key<TransactionSynchronizationManagerThreadContextElement>

    // provide the key of the corresponding context element
    override val key: CoroutineContext.Key<TransactionSynchronizationManagerThreadContextElement>
        get() = Key

    // this is invoked before coroutine is resumed on current thread
    override fun updateThreadContext(context: CoroutineContext): Map<String, Any?> {
        val previousTransactionSynchronizationManager =
            TransactionSynchronizationManager.isSynchronizationActive()
        val previousTransactionName = TransactionSynchronizationManager.getCurrentTransactionName()
        val previousTransactionIsolationLevel =
            TransactionSynchronizationManager.getCurrentTransactionIsolationLevel()
        val previousTransactionReadOnly = TransactionSynchronizationManager.isCurrentTransactionReadOnly()
        val previousResourceMap = TransactionSynchronizationManager.getResourceMap()
        val previousSynchronizations = TransactionSynchronizationManager.getSynchronizations()

        // disable transaction synchronization temporarily
        TransactionSynchronizationManager.clearSynchronization()
        try {
            val transactionSynchronizationManagerValues = mapOf(
                "isSynchronizationActive" to previousTransactionSynchronizationManager,
                "currentTransactionName" to previousTransactionName,
                "currentTransactionIsolationLevel" to previousTransactionIsolationLevel,
                "currentTransactionReadOnly" to previousTransactionReadOnly,
                "resourceMap" to previousResourceMap,
                "synchronizations" to previousSynchronizations
            )

            context[TransactionSynchronizationManagerValues] = transactionSynchronizationManagerValues

            return transactionSynchronizationManagerValues
        } finally {
            // enable transaction synchronization again
            TransactionSynchronizationManager.initSynchronization()
        }
    }

    // this is invoked after coroutine has suspended on current thread
    override fun restoreThreadContext(
        context: CoroutineContext,
        oldState: Map<String, Any?>
    ) {
        val previousTransactionSynchronizationManager =
            oldState["isSynchronizationActive"] as Boolean?
        val previousTransactionName = oldState["currentTransactionName"] as String?
        val previousTransactionIsolationLevel =
            oldState["currentTransactionIsolationLevel"] as Int?
        val previousTransactionReadOnly = oldState["currentTransactionReadOnly"] as Boolean?
        val previousResourceMap = oldState["resourceMap"] as Map<*, *>?
        val previousSynchronizations =
            oldState["synchronizations"] as MutableList<*>?

        // restore transaction synchronization manager values
        if (previousTransactionSynchronizationManager != null) {
            if (previousTransactionSynchronizationManager) {
                TransactionSynchronizationManager.initSynchronization()
            } else {
                TransactionSynchronizationManager.clearSynchronization()
            }
        }
        if (previousTransactionName != null) {
            TransactionSynchronizationManager.setCurrentTransactionName(previousTransactionName)
        }
        if (previousTransactionIsolationLevel != null) {
            TransactionSynchronizationManager.setCurrentTransactionIsolationLevel(previousTransactionIsolationLevel)
}
if (previousTransactionReadOnly != null) {
TransactionSynchronizationManager.setCurrentTransactionReadOnly(previousTransactionReadOnly)
}
if (previousResourceMap != null) {
TransactionSynchronizationManager.bindResources(previousResourceMap)
}
if (previousSynchronizations != null) {
for (synchronization in previousSynchronizations) {
TransactionSynchronizationManager.registerSynchronization(synchronization as TransactionSynchronization)
}
}
}private object TransactionSynchronizationManagerValues :
    CoroutineContext.Key<MutableMap<String, Any?>> {
    override val defaultValue: MutableMap<String, Any?> = mutableMapOf()
}}