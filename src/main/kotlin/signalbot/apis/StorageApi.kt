package signalbot.apis

import org.dizitart.no2.Document
import org.dizitart.no2.Filter
import org.dizitart.no2.Nitrite
import org.dizitart.no2.UpdateOptions
import org.dizitart.no2.filters.Filters

interface StorageApi{
    fun saveKeyValue(prefix: String, key: String, value: Any)
    fun removeKeyValue(prefix: String, key: String)
    fun <T>loadKeyValue(prefix: String, key: String, default: T): T
    fun saveDocument(prefix: String, document: Document)
    fun loadDocuments(prefix: String, filter: Filter): List<Document>
    fun removeDocument(prefix: String, document: Document)
    fun removeDocuments(prefix: String, filter: Filter)
    fun clearCollection(prefix: String)
}

class NitriteStorage(val db: Nitrite): StorageApi {
    override fun saveKeyValue(prefix: String, key: String, value: Any){
        val collection = db.getCollection(prefix)

        val doc = Document.createDocument("key", key)
            .put("value", value)

        collection.update(Filters.eq("key", key), doc, UpdateOptions.updateOptions(true))
    }

    override fun removeKeyValue(prefix: String, key: String){
        val collection = db.getCollection(prefix)

        collection.remove(Filters.eq("key", key))
    }

    override fun <T>loadKeyValue(prefix: String, key: String, default: T): T{
        val collection = db.getCollection(prefix)

        val data = collection.find(Filters.eq("key", key))

        if (data.size() == 0){
            return default
        }

        return data.first().getValue("value") as T
    }

    override fun saveDocument(prefix: String, document: Document){
        val collection = db.getCollection(prefix)

        collection.insert(document)
    }

    override fun loadDocuments(prefix: String, filter: Filter): List<Document>{
        val collection = db.getCollection(prefix)

        val found = collection.find(filter)

        if(found.size() > 0){
            return found.toList()
        }

        return listOf()
    }

    override fun removeDocument(prefix: String, document: Document) {
        val collection = db.getCollection(prefix)

        collection.remove(document)
    }

    override fun removeDocuments(prefix: String, filter: Filter) {
        val collection = db.getCollection(prefix)

        collection.remove(filter)
    }

    override fun clearCollection(prefix: String){
        val collection = db.getCollection(prefix)

        collection.remove(Filters.ALL)
    }
}