package com.mynikatech.apnafund.server.db.tables

import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.Table


object RolePrivilegeTable : Table(name = "role_privilege") {
    val rolePrivilegeId = integer("rolePrivilegeId").autoIncrement()

    val roleId = integer("roleId")
        .references(RolesTable.roleId, onDelete = ReferenceOption.CASCADE)

    val privilegeId = integer("privilegeId")
        .references(PrivilegeTable.privilegeId, onDelete = ReferenceOption.CASCADE)

    val status = varchar("status", length = 32).default("ACTIVE")

    override val primaryKey = PrimaryKey(rolePrivilegeId)

    init {
        // A role can reference a privilege only once
        uniqueIndex("ux_role_privilege_role_priv", roleId, privilegeId)
        index(false, roleId)
        index(false, privilegeId)
        index(false, status)
    }
}
