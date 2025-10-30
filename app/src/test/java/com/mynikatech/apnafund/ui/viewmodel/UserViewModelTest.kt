package com.mynikatech.apnafund.ui.viewmodel

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.mynikatech.apnafund.ApnaFundApplication
import com.mynikatech.apnafund.data.model.Users
import com.mynikatech.apnafund.data.repository.UserRoleRepository
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
    fun `fetchUsers should return list of users`() = runBlocking {
        val mockUsers = listOf(
            Users(
                1,
                "John",
                "Doe",
                "john@example.com",
                "1234567890",
                "ACTIVE",
                createdDate = ApnaBankDate.getCurrentDate(),
                userCode = "JODO123456"
            )
        )
        `when`(userRolesRepository.fetchAllUsers()).thenReturn(flowOf(mockUsers))

        val result = viewModel.fetchUsers()
        result.collect {
            assertEquals(1, it.size)
            assertEquals("John", it[0].firstName)
        }
    }

    @Test
    fun `fetchUser should return correct user`() = runBlocking {
        val user = Users(1, "Jane", "Doe", "jane@example.com", "9876543210", "ACTIVE",createdDate = ApnaBankDate.getCurrentDate(), userCode = "JODO123456")
        `when`(userRolesRepository.fetchUser(1)).thenReturn(user)

        val result = viewModel.fetchUser(1)
        assertNotNull(result)
        assertEquals("Jane", result?.firstName)
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

    @Test
    fun `saveOrUpdateUser should call createUser if userId is 0`() = runBlocking {
        val user = Users(0, "New", "User", "new@example.com", "1234567890", "ACTIVE",createdDate = ApnaBankDate.getCurrentDate(), userCode = "JODO123456")

        viewModel.saveOrUpdateUser(user)

        verify(userRolesRepository, timeout(1000)).createUser(user)
    }

    @Test
    fun `saveOrUpdateUser should call updateUser if userId is not 0`() = runBlocking {
        val user =
            Users(2, "Updated", "User", "update@example.com", "9876543210", "ACTIVE", createdDate = ApnaBankDate.getCurrentDate(), userCode = "JODO123456")

        viewModel.saveOrUpdateUser(user)

        verify(userRolesRepository, timeout(1000)).updateUser(user)
    }
}
