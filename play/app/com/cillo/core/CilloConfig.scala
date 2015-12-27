package com.cillo.core

object CilloConfig {

    // REDIRECTION CONFIG
    final val RedirectHttp = false
    final val RedirectMobile = false
    // should redirect cillo.co to www.cillo.co
    final val RedirectToWWW = false

    // ETC CONFIG
    // should be responding correctly to elb health checks
    final val HealthCheck = true

}