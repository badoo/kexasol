package com.badoo.kexasol.api

import com.badoo.kexasol.json.JsonExaData
import com.badoo.kexasol.json.JsonExaMap
import com.squareup.moshi.JsonClass

internal sealed class ApiResponse {
    @JsonClass(generateAdapter = true)
    data class CommonResponse(
        val status: String,
        @JsonExaMap val attributes: Map<String, Any>?,
        val exception: ApiException?,
    ) {
        @JsonClass(generateAdapter = true)
        data class ApiException(
            val text: String,
            val sqlCode: String
        )
    }

    @JsonClass(generateAdapter = true)
    data class CloseResultSetV1(
        val status: String,
        @JsonExaMap val attributes: Map<String, Any>?,
    ) : ApiResponse()

    @JsonClass(generateAdapter = true)
    data class LoginV1(
        val status: String,
        val responseData: ResponseData
    ) : ApiResponse() {
        @JsonClass(generateAdapter = true)
        data class ResponseData(
            val publicKeyPem: String,
            val publicKeyModulus: String,
            val publicKeyExponent: String
        )
    }

    @JsonClass(generateAdapter = true)
    data class LoginAuthV1(
        val status: String,
        @JsonExaMap val responseData: Map<String, Any>
    ) : ApiResponse()

    @JsonClass(generateAdapter = true)
    data class ExecuteV1(
        val status: String,
        @JsonExaMap val attributes: Map<String, Any>?,
        val responseData: ResponseData,
    ) : ApiResponse() {
        @JsonClass(generateAdapter = true)
        data class ResponseData(
            val numResults: Long,
            val results: List<Results>
        ) {
            @JsonClass(generateAdapter = true)
            data class Results(
                val resultType: String,
                val rowCount: Long?,
                val resultSet: ResultSet?
            ) {
                @JsonClass(generateAdapter = true)
                data class ResultSet(
                    val resultSetHandle: Long?,
                    val numColumns: Long,
                    val numRows: Long,
                    val numRowsInMessage: Long,
                    val columns: List<ApiColumn>,
                    @JsonExaData val data: List<List<Any?>>?
                )
            }
        }
    }

    @JsonClass(generateAdapter = true)
    data class FetchV1(
        val status: String,
        @JsonExaMap val attributes: Map<String, Any>?,
        val responseData: ResponseData
    ) : ApiResponse() {
        @JsonClass(generateAdapter = true)
        data class ResponseData(
            val numRows: Long,
            @JsonExaData val data: List<List<Any?>>?
        )
    }

    @JsonClass(generateAdapter = true)
    data class GetAttributesV1(
        val status: String,
        @JsonExaMap val attributes: Map<String, Any>?,
    ) : ApiResponse()

    @JsonClass(generateAdapter = true)
    data class GetHostsV1(
        val status: String,
        @JsonExaMap val attributes: Map<String, Any>?,
        val responseData: ResponseData,
    ) : ApiResponse() {
        @JsonClass(generateAdapter = true)
        data class ResponseData(
            val numNodes: Int,
            val nodes: List<String>
        )
    }

    @JsonClass(generateAdapter = true)
    data class SetAttributesV1(
        val status: String,
        @JsonExaMap val attributes: Map<String, Any>?,
    ) : ApiResponse()

    @JsonClass(generateAdapter = true)
    data class DisconnectV1(
        val status: String,
        @JsonExaMap val attributes: Map<String, Any>?,
    ) : ApiResponse()
}
