package com.cillo.core.email

import play.api.Application
import play.api.PlayException

object PlayConfiguration {
    def apply(key: String, default: Option[String] = None)(implicit app: Application): String =
        app.configuration
            .getString(key)
            .orElse(default)
            .getOrElse(throw new PlayException("Configuration error", s"Could not find $key in settings"))

    def apply(key: String, default: String)(implicit app: Application): String =
        apply(key, Some(default))
}