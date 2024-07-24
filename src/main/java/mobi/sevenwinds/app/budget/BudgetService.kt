package mobi.sevenwinds.app.budget

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mobi.sevenwinds.app.Author.AuthorEntity
import mobi.sevenwinds.app.Author.AuthorTable
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction

object BudgetService {
    suspend fun addRecord(body: BudgetRecord): BudgetRecord = withContext(Dispatchers.IO) {
        transaction {
            val entity = BudgetEntity.new {
                this.year = body.year
                this.month = body.month
                this.amount = body.amount
                this.type = body.type
                this.author = body.authorId?.let { AuthorEntity.findById(it) }
            }

            return@transaction entity.toResponse()
        }
    }

    suspend fun getYearStats(param: BudgetYearParam): BudgetYearStatsResponse = withContext(Dispatchers.IO) {
        transaction {
            val queryAuthor = BudgetTable
                .leftJoin(AuthorTable)
                .select { BudgetTable.year eq param.year }
                .limit(param.limit, param.offset)
                .map {
                    val authorName = it[AuthorTable.full_name]
                    val authorDateTime = it[AuthorTable.dateTime]
                    Pair(authorName, authorDateTime)
                }
                .toString()
            val query = BudgetTable
                .select { BudgetTable.year eq param.year }
                .limit(param.limit, param.offset)

            val total = query.count()
            val data = BudgetEntity.wrapRows(query).map { it.toResponse() }
            val sumByType = data.groupBy { it.type.name }.mapValues { it.value.sumOf { v -> v.amount } }

            return@transaction BudgetYearStatsResponse(
                total = total,
                totalByType = sumByType,
                items = data,
                authorName = queryAuthor,
            )
        }
    }
}

private operator fun ResultRow.get(authorTable: AuthorTable): Any {
    return "{0} + {1}"
}
