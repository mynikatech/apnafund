package com.mynikatech.apnafund.server

import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.routing
import io.ktor.server.routing.get

fun Application.installDebugRoutes() {
    routing {
        get("/_debug/echo") {
            call.respondText("Echo -> ${System.currentTimeMillis()}")
        }

        /*get("/_debug/routes") {
            val sb = StringBuilder()

            fun dump(route: Route, indent: String = "") {
                val sel = route..toString()
                if (sel != "(root)") sb.append(indent).append(sel).append('\n')
                for (child in route.children) dump(child, "$indent  ")
            }

            // `this@routing` is the Routing root for this Application
            dump(this@routing)
            call.respondText(sb.toString())
        }*/
    }
}
