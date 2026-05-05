package com.mynikatech.apnafund.ui.viewmodel

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.mynikatech.apnafund.ApnaFundApplication
import com.mynikatech.apnafund.data.model.Users
import com.mynikatech.apnafund.data.repository.UserRoleRepository
import com.mynikatech.apnafund.net.dto.UserSaveSource
import com.mynikatech.apnafund.util.ApnaBankDate
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.timeout
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.junit.MockitoJUnit
import org.mockito.junit.MockitoRule

@OptIn(ExperimentalCoroutinesApi::class)
class UserViewModelTest {

    @get:Rule
    val rule = InstantTaskExecutorRule()

    @get:Rule
    val mockitoRule: MockitoRule = MockitoJUnit.rule()

    private lateinit var userRolesRepository: UserRoleRepository
    private lateinit var viewModel: UserViewModel

    @Before
    fun setup() {
        userRolesRepository = mock(UserRoleRepository::class.java)
        ApnaFundApplication.userRolesRepository = userRolesRepository
        viewModel = UserViewModel()
    }



    @Test
    fun `doesUserExists should return true if user exists`() = runBlocking {
        `when`(userRolesRepository.doesUserExists("test@example.com")).thenReturn(true)

        val result = viewModel.doesUserExists("test@example.com")
        assertTrue(result)
    }

    @Test
    fun `isDuplicate should return true for duplicate user`() = runBlocking {
        `when`(userRolesRepository.isDuplicateUser("a@example.com", "1234567890", 1)).thenReturn(
            true
        )

        val result = viewModel.isDuplicate("a@example.com", "1234567890", 1)
        assertTrue(result)
    }


}
