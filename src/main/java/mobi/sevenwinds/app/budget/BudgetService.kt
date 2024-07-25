package mobi.sevenwinds.app.budget

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mobi.sevenwinds.app.Author.AuthorEntity
import mobi.sevenwinds.app.Author.AuthorTable
import org.jetbrains.exposed.sql.andWhere
import org.jetbrains.exposed.sql.lowerCase
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
            val query = BudgetTable
                .leftJoin(AuthorTable)
                .select { BudgetTable.year eq param.year }
                .limit(param.limit, param.offset)
            val queryWithName = if (param.authorName != null) {
                query.andWhere { AuthorTable.full_name.lowerCase().like("%${param.authorName.toLowerCase()}%") }
            } else {
                query
            }
            val queryAuthor = BudgetTable
                .leftJoin(AuthorTable)
                .select { BudgetTable.year eq param.year }
                .andWhere { AuthorTable.full_name.isNotNull() }
                .let { query1 ->
                    if (queryWithName.count()==1)
                        query1.andWhere { AuthorTable.full_name.lowerCase().like("%${param.authorName?.toLowerCase()}%")}
                    else
                        query1
                }
                .limit(param.limit, param.offset)
                .map {
                    val authorName = it[AuthorTable.full_name]
                    val authorDateTime = it[AuthorTable.dateTime]
                    Pair(authorName, authorDateTime)
                }
                .toString()

            val total = queryWithName.count()
            val data = BudgetEntity.wrapRows(queryWithName).map { it.toResponse() }
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
