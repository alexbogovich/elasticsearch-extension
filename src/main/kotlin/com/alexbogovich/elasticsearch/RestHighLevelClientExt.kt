package com.alexbogovich.elasticsearch

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import org.elasticsearch.action.ActionListener
import org.elasticsearch.action.admin.cluster.node.tasks.list.ListTasksResponse
import org.elasticsearch.action.bulk.BulkRequest
import org.elasticsearch.action.bulk.BulkResponse
import org.elasticsearch.action.delete.DeleteRequest
import org.elasticsearch.action.delete.DeleteResponse
import org.elasticsearch.action.get.GetRequest
import org.elasticsearch.action.get.GetResponse
import org.elasticsearch.action.get.MultiGetRequest
import org.elasticsearch.action.get.MultiGetResponse
import org.elasticsearch.action.index.IndexRequest
import org.elasticsearch.action.index.IndexResponse
import org.elasticsearch.action.search.*
import org.elasticsearch.action.update.UpdateRequest
import org.elasticsearch.action.update.UpdateResponse
import org.elasticsearch.client.RequestOptions
import org.elasticsearch.client.RestHighLevelClient
import org.elasticsearch.client.RethrottleRequest
import org.elasticsearch.common.unit.TimeValue
import org.elasticsearch.index.reindex.BulkByScrollResponse
import org.elasticsearch.index.reindex.DeleteByQueryRequest
import org.elasticsearch.index.reindex.ReindexRequest
import org.elasticsearch.index.reindex.UpdateByQueryRequest
import org.elasticsearch.search.Scroll
import org.elasticsearch.search.SearchHit
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine
import kotlin.experimental.ExperimentalTypeInference

@OptIn(ExperimentalTypeInference::class)
suspend fun <T> coListener(@BuilderInference handle: (listener: ActionListener<T>) -> Unit): T {
    return withContext(Dispatchers.IO) {
        suspendCoroutine { sink: Continuation<T> ->
            try {
                handle(ActionListener.wrap(sink::resume) { sink.resumeWithException(it) })
            } catch (e: Exception) {
                sink.resumeWithException(e)
            }
        }
    }
}

suspend fun RestHighLevelClient.coBulk(
    request: BulkRequest,
    options: RequestOptions = RequestOptions.DEFAULT
): BulkResponse = coListener { bulkAsync(request, options, it) }

suspend fun RestHighLevelClient.coReindex(
    request: ReindexRequest,
    options: RequestOptions = RequestOptions.DEFAULT
): BulkByScrollResponse = coListener { reindexAsync(request, options, it) }

suspend fun RestHighLevelClient.coUpdateByQuery(
    request: UpdateByQueryRequest,
    options: RequestOptions = RequestOptions.DEFAULT
): BulkByScrollResponse = coListener { updateByQueryAsync(request, options, it) }

suspend fun RestHighLevelClient.coDeleteByQuery(
    request: DeleteByQueryRequest,
    options: RequestOptions = RequestOptions.DEFAULT
): BulkByScrollResponse = coListener { deleteByQueryAsync(request, options, it) }

suspend fun RestHighLevelClient.coDeleteByQueryRethrottle(
    request: RethrottleRequest,
    options: RequestOptions = RequestOptions.DEFAULT
): ListTasksResponse = coListener { deleteByQueryRethrottleAsync(request, options, it) }

suspend fun RestHighLevelClient.coUpdateByQueryRethrottle(
    request: RethrottleRequest,
    options: RequestOptions = RequestOptions.DEFAULT
): ListTasksResponse = coListener { updateByQueryRethrottleAsync(request, options, it) }

suspend fun RestHighLevelClient.coReindexRethrottle(
    request: RethrottleRequest,
    options: RequestOptions = RequestOptions.DEFAULT
): ListTasksResponse = coListener { reindexRethrottleAsync(request, options, it) }

suspend fun RestHighLevelClient.coSearch(
    request: SearchRequest,
    options: RequestOptions = RequestOptions.DEFAULT
): SearchResponse = coListener { searchAsync(request, options, it) }

suspend fun RestHighLevelClient.coGet(
    request: GetRequest,
    options: RequestOptions = RequestOptions.DEFAULT
): GetResponse = coListener { getAsync(request, options, it) }

suspend fun RestHighLevelClient.coMget(
    request: MultiGetRequest,
    options: RequestOptions = RequestOptions.DEFAULT
): MultiGetResponse = coListener { mgetAsync(request, options, it) }

suspend fun RestHighLevelClient.coIndex(
    request: IndexRequest,
    options: RequestOptions = RequestOptions.DEFAULT
): IndexResponse = coListener { indexAsync(request, options, it) }

suspend fun RestHighLevelClient.coUpdate(
    request: UpdateRequest,
    options: RequestOptions = RequestOptions.DEFAULT
): UpdateResponse = coListener { updateAsync(request, options, it) }

suspend fun RestHighLevelClient.coUpdate(
    request: DeleteRequest,
    options: RequestOptions = RequestOptions.DEFAULT
): DeleteResponse = coListener { deleteAsync(request, options, it) }

suspend fun RestHighLevelClient.coMsearchAsync(
    request: MultiSearchRequest,
    options: RequestOptions = RequestOptions.DEFAULT
): MultiSearchResponse = coListener { msearchAsync(request, options, it) }

suspend fun RestHighLevelClient.coScroll(
    request: SearchScrollRequest,
    options: RequestOptions = RequestOptions.DEFAULT
): SearchResponse = coListener { scrollAsync(request, options, it) }

suspend fun RestHighLevelClient.coClearScroll(
    request: ClearScrollRequest,
    options: RequestOptions = RequestOptions.DEFAULT
): ClearScrollResponse = coListener { clearScrollAsync(request, options, it) }

suspend fun RestHighLevelClient.coScrollAsFlow(searchRequest: SearchRequest): Flow<SearchHit> = flow {
    val scroll = Scroll(TimeValue.timeValueMinutes(1))
    searchRequest.scroll(scroll)

    var searchResponse: SearchResponse = coSearch(searchRequest)
    var scrollId: String = searchResponse.scrollId
    var searchHits: Array<SearchHit> = searchResponse.hits.hits
    try {
        searchHits.forEach {
            emit(it)
        }
        while (!searchHits.isNullOrEmpty()) {
            val scrollRequest = SearchScrollRequest(scrollId)
            scrollRequest.scroll(scroll)
            searchResponse = coScroll(scrollRequest)
            scrollId = searchResponse.scrollId
            searchHits = searchResponse.hits.hits
            searchHits.forEach {
                emit(it)
            }
        }
    } finally {
        val clearScrollRequest = ClearScrollRequest()
        clearScrollRequest.addScrollId(scrollId)
        coClearScroll(clearScrollRequest)
    }
}
