package com.mynikatech.apnafund.ui.viewmodel

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.mynikatech.apnafund.data.model.*
import com.mynikatech.apnafund.data.repository.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.*
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.junit.MockitoJUnitRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(MockitoJUnitRunner::class)
class UserSummaryViewModelTest {

    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    @Mock
    private lateinit var userSummaryRepository: UserSummaryRepository

    @Mock
    private lateinit var userRolesRepository: UserRoleRepository

    @Mock
    private lateinit var fundRepository: FundRepository

    @Mock
    private lateinit var loanRepository: LoanRepository

    private lateinit var viewModel: UserSummaryViewModel

    @Before
    fun setup() {
        com.mynikatech.apnafund.ApnaFundApplication.userSummaryRepository = userSummaryRepository
        com.mynikatech.apnafund.ApnaFundApplication.userRolesRepository = userRolesRepository
        com.mynikatech.apnafund.ApnaFundApplication.fundRepository = fundRepository
        com.mynikatech.apnafund.ApnaFundApplication.loanRepository = loanRepository

        viewModel = UserSummaryViewModel()
    }

    @Test
    fun `getFund should return fund details`() = runTest {
        val dummyFund = Funds(
            fundId = 1,
            fundName = "Monthly Savings",
            fundStartDate = "2024-01-01",
            fundMaturityDate = "2026-01-01",
            fundPeriod = 24.0,
            depositionFrequency = "Monthly",
            recurringDepositAmount = 1000.0,
            loanInterestRate = 10.5,
            lateFeeRate = 2.0,
            monthlyDepDateBy = 10,
            groupId = 3,
            moderator = 1,
            fundCode = "MO123456"
        )
        `when`(fundRepository.fetchFund(1)).thenReturn(dummyFund)

        val result = viewModel.getFund(1)

        Assert.assertEquals("Monthly Savings", result.fundName)
        Assert.assertEquals("2024-01-01", result.fundStartDate)
        Assert.assertEquals(10.5, result.loanInterestRate, 0.0)
    }

    @Test
    fun `getfundRateOfInterest should return correct rate`() = runTest {
        `when`(userSummaryRepository.getRateOfInterestforFund(1)).thenReturn(8.5)

        val rate = viewModel.getfundRateOfInterest(1)

        Assert.assertEquals(8.5, rate, 0.001)
    }

    @Test
    fun `getTotalAmountAvailableforFund should return amount`() = runTest {
        `when`(fundRepository.getAvailableFundAmount(1)).thenReturn(5000.0)

        val amount = viewModel.getTotalAmountAvailableforFund(1)

        Assert.assertEquals(5000.0, amount!!, 0.0)
    }

    @Test
    fun `getTotalLoanAmountforUserforFund should return loan total`() = runTest {
        `when`(userSummaryRepository.getTotalLoanAmount(2, 3)).thenReturn(10000.0)

        val result = viewModel.getTotalLoanAmountforUserforFund(2, 3)

        Assert.assertEquals(10000.0, result, 0.0)
    }

    @Test
    fun `getTotalPendingAmount should return pending amount`() = runTest {
        `when`(loanRepository.getTotalPendingAmount(2, 3)).thenReturn(800.0)

        val result = viewModel.getTotalPendingAmount(2, 3)

        Assert.assertEquals(800.0, result, 0.0)
    }

    @Test
    fun `getTotalCurrIntPaid should return interest paid`() = runTest {
        `when`(loanRepository.getTotalCurrIntPaid(2, 3)).thenReturn(400.0)

        val result = viewModel.getTotalCurrIntPaid(2, 3)

        Assert.assertEquals(400.0, result, 0.0)
    }

    @Test
    fun `getUserProfile should return user profile list`() = runTest {
        val dummyProfiles = listOf(
            UserProfile(
                userId = 1,
                userName = "Sunil Kumar",
                roleId = 101,
                roleCode = "ADMIN",
                groupId = 12,
                groupName = "Shakti Group",
                token = "dummyToken123",
                isLoggedIn = true,
                firstName = "Sunil",
                lastName = "Kumar",
                emailId = "sunilagl@gmail.com",
                phoneNumber = "9004556784"
            )
        )
        `when`(userRolesRepository.getUserProfile(1)).thenReturn(dummyProfiles)

        val result = viewModel.getUserProfile(1)

        Assert.assertEquals(1, result.size)
        Assert.assertEquals("Sunil Kumar", result[0].userName)
        Assert.assertEquals("ADMIN", result[0].roleCode)
        Assert.assertTrue(result[0].isLoggedIn == true)
    }

    @Test
    fun `getUserLoanDetails should return correct details`() = runTest {
        val details = UserLoanDetails(2000.0, 500.0)
        `when`(loanRepository.getUserLoanDetails(1, 2)).thenReturn(details)

        val result = viewModel.getUserLoanDetails(1, 2)

        Assert.assertEquals(2000.0, result.totalCurrIntPaid, 0.0)
    }
}
