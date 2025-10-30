package com.mynikatech.apnafund.server.types

import com.mynikatech.apnafund.net.dto.TypeDto
import org.jdbi.v3.sqlobject.kotlin.BindKotlin
import org.jdbi.v3.sqlobject.customizer.Bind
import org.jdbi.v3.sqlobject.statement.SqlQuery
import org.jdbi.v3.sqlobject.kotlin.RegisterKotlinMapper

@RegisterKotlinMapper(TypeDto::class)
interface TypesSql {

    @SqlQuery("""SELECT * FROM get_types()""")
    fun getAllTypes(): List<TypeDto>

    @SqlQuery("""SELECT * FROM get_type(:id)""")
    fun getType(@Bind("id") id: Int): List<TypeDto>   // 0..1 row

    @SqlQuery("""SELECT add_type(:typeCode, :typeDescription, :status)""")
    fun addType(@BindKotlin t: TypeDto): Int

    @SqlQuery("""SELECT update_type(:id, :typeCode, :typeDescription, :status)""")
    fun updateType(
        @Bind("id") id: Int,
        @Bind("typeCode") typeCode: String?,
        @Bind("typeDescription") typeDescription: String?,
        @Bind("status") status: String?
    ): Boolean

    @SqlQuery("""SELECT delete_type(:id)""")
    fun deleteType(@Bind("id") id: Int): Boolean
}
