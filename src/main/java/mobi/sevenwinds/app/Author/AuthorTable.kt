package mobi.sevenwinds.app.Author

import org.jetbrains.exposed.dao.EntityID
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.IntIdTable

object AuthorTable : IntIdTable("author") {
    val full_name = text("full_name")
    val dateTime = datetime("created_at")

}

class AuthorEntity(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<AuthorEntity>(AuthorTable)

    var full_name by AuthorTable.full_name

    fun toResponse(): AuthorRecord {
        return AuthorRecord(full_name)
    }
}
