package com.cillo.core

object CilloConfig {

    // REDIRECTION CONFIG
    final val RedirectHttp = true
    final val RedirectMobile = true
    // should redirect cillo.co to www.cillo.co
    final val RedirectToWWW = true

    // ETC CONFIG
    // should be responding correctly to elb health checks
    final val HealthCheck = true

}