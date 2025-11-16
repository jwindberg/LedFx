package com.marsraver.LedFx.wled

/**
 * Basic information about a WLED device used by the DDP client.
 * This is intentionally minimal – only fields actually needed by [WledDdpClient].
 */
class WledInfo @JvmOverloads constructor(var ip: String?, var name: String? = null)

