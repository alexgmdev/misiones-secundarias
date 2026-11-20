package com.groovycow.misionessecundarias.data.repository

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class ProfileRepositoryImplTest {
    @Test
    fun publicColumnsAreExactlyTheSixPublicFields() {
        assertEquals(
            listOf("id", "nickname", "gender", "description", "avatar_id", "photo_url"),
            ProfileRepositoryImpl.PUBLIC_COLUMNS,
        )
    }

    @Test
    fun publicColumnsNeverExposePrivateData() {
        listOf("full_name", "first_name", "last_name", "email", "role", "phone").forEach {
            assertFalse(it in ProfileRepositoryImpl.PUBLIC_COLUMNS)
        }
    }
}
