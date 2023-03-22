package com.raccoongang.auth.presentation.restore

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.raccoongang.auth.domain.interactor.AuthInteractor
import com.raccoongang.auth.presentation.signup.SignUpViewModel
import com.raccoongang.core.R
import com.raccoongang.core.UIMessage
import com.raccoongang.core.system.EdxError
import com.raccoongang.core.system.ResourceManager
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule
import java.net.UnknownHostException

@OptIn(ExperimentalCoroutinesApi::class)
class RestorePasswordViewModelTest {

    @get:Rule
    val testInstantTaskExecutorRule: TestRule = InstantTaskExecutorRule()
    private val dispatcher = StandardTestDispatcher()

    private val resourceManager = mockk<ResourceManager>()
    private val interactor = mockk<AuthInteractor>()

    //region parameters

    private val correctEmail = "acc@test.org"
    private val emptyEmail = ""

    //endregion

    private val noInternet = "Slow or no internet connection"
    private val somethingWrong = "Something went wrong"
    private val invalidEmail = "Invalid email"
    private val invalidPassword = "Password too short"

    @Before
    fun before() {
        Dispatchers.setMain(dispatcher)
        every { resourceManager.getString(R.string.core_error_no_connection) } returns noInternet
        every { resourceManager.getString(R.string.core_error_unknown_error) } returns somethingWrong
        every { resourceManager.getString(com.raccoongang.auth.R.string.auth_invalid_email) } returns invalidEmail
        every { resourceManager.getString(com.raccoongang.auth.R.string.auth_invalid_password) } returns invalidPassword
    }

    @After
    fun after() {
        Dispatchers.resetMain()
    }

    @Test
    fun `passwordReset empty email validation error`() = runTest {
        val viewModel = RestorePasswordViewModel(interactor, resourceManager)
        coEvery { interactor.passwordReset(emptyEmail) } returns true
        viewModel.passwordReset(emptyEmail)
        advanceUntilIdle()
        coVerify(exactly = 0) { interactor.passwordReset(any()) }

        val message = viewModel.uiMessage.value as? UIMessage.SnackBarMessage

        assertEquals(true, viewModel.uiState.value is RestorePasswordUIState.Initial)
        assertEquals(invalidEmail, message?.message)
    }

    @Test
    fun `passwordReset invalid email validation error`() = runTest {
        val viewModel = RestorePasswordViewModel(interactor, resourceManager)
        coEvery { interactor.passwordReset(invalidEmail) } returns true
        viewModel.passwordReset(invalidEmail)
        advanceUntilIdle()
        coVerify(exactly = 0) { interactor.passwordReset(any()) }

        val message = viewModel.uiMessage.value as? UIMessage.SnackBarMessage

        assertEquals(true, viewModel.uiState.value is RestorePasswordUIState.Initial)
        assertEquals(invalidEmail, message?.message)
    }

    @Test
    fun `passwordReset validation error`() = runTest {
        val viewModel = RestorePasswordViewModel(interactor, resourceManager)
        coEvery { interactor.passwordReset(correctEmail) } throws  EdxError.ValidationException("error")
        viewModel.passwordReset(correctEmail)
        advanceUntilIdle()
        coVerify(exactly = 1) { interactor.passwordReset(any()) }

        val message = viewModel.uiMessage.value as? UIMessage.SnackBarMessage

        assertEquals(true, viewModel.uiState.value is RestorePasswordUIState.Initial)
        assertEquals("error", message?.message)
    }

    @Test
    fun `passwordReset no internet error`() = runTest {
        val viewModel = RestorePasswordViewModel(interactor, resourceManager)
        coEvery { interactor.passwordReset(correctEmail) } throws  UnknownHostException()
        viewModel.passwordReset(correctEmail)
        advanceUntilIdle()
        coVerify(exactly = 1) { interactor.passwordReset(any()) }

        val message = viewModel.uiMessage.value as? UIMessage.SnackBarMessage

        assertEquals(true, viewModel.uiState.value is RestorePasswordUIState.Initial)
        assertEquals(noInternet, message?.message)
    }

    @Test
    fun `passwordReset unknown error`() = runTest {
        val viewModel = RestorePasswordViewModel(interactor, resourceManager)
        coEvery { interactor.passwordReset(correctEmail) } throws  Exception()
        viewModel.passwordReset(correctEmail)
        advanceUntilIdle()
        coVerify(exactly = 1) { interactor.passwordReset(any()) }

        val message = viewModel.uiMessage.value as? UIMessage.SnackBarMessage

        assertEquals(true, viewModel.uiState.value is RestorePasswordUIState.Initial)
        assertEquals(somethingWrong, message?.message)
    }

    @Test
    fun `unSuccess restore password`() = runTest {
        val viewModel = RestorePasswordViewModel(interactor, resourceManager)
        coEvery { interactor.passwordReset(correctEmail) } returns false
        viewModel.passwordReset(correctEmail)
        advanceUntilIdle()
        coVerify(exactly = 1) { interactor.passwordReset(any()) }

        val message = viewModel.uiMessage.value as? UIMessage.SnackBarMessage

        assertEquals(true, viewModel.uiState.value is RestorePasswordUIState.Initial)
        assertEquals(somethingWrong, message?.message)
    }


    @Test
    fun `success restore password`() = runTest {
        val viewModel = RestorePasswordViewModel(interactor, resourceManager)
        coEvery { interactor.passwordReset(correctEmail) } returns true
        viewModel.passwordReset(correctEmail)
        advanceUntilIdle()
        coVerify(exactly = 1) { interactor.passwordReset(any()) }

        val state = viewModel.uiState.value as? RestorePasswordUIState.Success
        val message = viewModel.uiMessage.value as? UIMessage.SnackBarMessage

        assertEquals(correctEmail, state?.email)
        assertEquals(true, viewModel.uiState.value is RestorePasswordUIState.Success)
        assertEquals(null, message)
    }


}