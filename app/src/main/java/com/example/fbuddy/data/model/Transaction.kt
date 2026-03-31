package com.example.fbuddy.data.model

import com.example.fbuddy.data.db.TransactionEntity

/**
 * Public-facing transaction model used by the UI and domain layer.
 *
 * Currently this is a simple typealias to the Room entity to avoid
 * unnecessary mapping boilerplate while keeping the package structure
 * aligned with the PRD.
 */
typealias Transaction = TransactionEntity

