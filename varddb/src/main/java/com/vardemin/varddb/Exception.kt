package com.vardemin.varddb

import java.lang.Exception


class VardDbNotInitializedException(override val message: String): Exception(message)