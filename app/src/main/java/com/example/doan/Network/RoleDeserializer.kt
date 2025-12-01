package com.example.doan.Network

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonParseException
import java.lang.reflect.Type

class RoleDeserializer : JsonDeserializer<String> {
    
    @Throws(JsonParseException::class)
    override fun deserialize(
        json: JsonElement,
        typeOfT: Type,
        context: JsonDeserializationContext
    ): String? {
        return when {
            json.isJsonPrimitive -> json.asString
            json.isJsonObject -> {
                // Nếu backend trả về object {"name": "MANAGER"}
                json.asJsonObject.get("name").asString
            }
            else -> null
        }
    }
}
