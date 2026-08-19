package magi.aenerv7.ppembytv.server

import magi.aenerv7.ppembytv.data.model.BackupRouteConfig

/** 从远端服务器抓取备用线路的结果。 */
data class RemoteBackupRouteFetchResult(
    val totalCount: Int,
    val routesToImport: List<BackupRouteConfig>,
)
