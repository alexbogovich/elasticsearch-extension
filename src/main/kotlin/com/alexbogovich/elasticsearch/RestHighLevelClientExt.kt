package com.alexbogovich.elasticsearch

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.elasticsearch.action.ActionListener
import org.elasticsearch.action.bulk.BulkRequest
import org.elasticsearch.action.bulk.BulkResponse
import org.elasticsearch.action.search.SearchRequest
import org.elasticsearch.action.search.SearchResponse
import org.elasticsearch.client.RequestOptions
import org.elasticsearch.client.RestHighLevelClient
import org.elasticsearch.index.reindex.BulkByScrollResponse
import org.elasticsearch.index.reindex.DeleteByQueryRequest
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

suspend fun RestHighLevelClient.coResponse(requestProvider: () -> SearchRequest): SearchResponse {
    return withContext(Dispatchers.IO) {
        suspendCoroutine { sink: Continuation<SearchResponse> ->
            try {
                val request: SearchRequest = requestProvider()
                val actionListener: ActionListener<SearchResponse> =
                    ActionListener.wrap(sink::resume) { e -> sink.resumeWithException(e) }
                searchAsync(request, RequestOptions.DEFAULT, actionListener)
            } catch (e: Exception) {
                sink.resumeWithException(e)
            }
        }
    }
}

suspend fun RestHighLevelClient.coBulkAsync(
    request: BulkRequest,
    options: RequestOptions = RequestOptions.DEFAULT
): BulkResponse {
    return withContext(Dispatchers.IO) {
        suspendCoroutine { sink: Continuation<BulkResponse> ->
            bulkAsync(request, options, ActionListener.wrap(sink::resume) { e -> sink.resumeWithException(e) })
        }
    }
}

suspend fun RestHighLevelClient.coDeleteByQuery(requestProvider: () -> DeleteByQueryRequest): BulkByScrollResponse {
    return withContext(Dispatchers.IO) {
        suspendCoroutine { sink: Continuation<BulkByScrollResponse> ->
            try {
                val request: DeleteByQueryRequest = requestProvider()
                val actionListener: ActionListener<BulkByScrollResponse> =
                    ActionListener.wrap(sink::resume) { e -> sink.resumeWithException(e) }
                deleteByQueryAsync(request, RequestOptions.DEFAULT, actionListener)
            } catch (e: Exception) {
                sink.resumeWithException(e)
            }
        }
    }
}
