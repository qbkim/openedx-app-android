package com.raccoongang.auth.presentation.signup

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.raccoongang.auth.data.model.RegistrationFields
import com.raccoongang.auth.data.model.ValidationFields
import com.raccoongang.auth.domain.interactor.AuthInteractor
import com.raccoongang.core.ApiConstants
import com.raccoongang.core.R
import com.raccoongang.core.UIMessage
import com.raccoongang.core.domain.model.RegistrationField
import com.raccoongang.core.domain.model.RegistrationFieldType
import com.raccoongang.core.system.ResourceManager
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule
import java.net.UnknownHostException


@ExperimentalCoroutinesApi
class SignUpViewModelTest {

    @get:Rule
    val testInstantTaskExecutorRule: TestRule = InstantTaskExecutorRule()
    private val dispatcher = StandardTestDispatcher()

    private val resourceManager = mockk<ResourceManager>()
    private val interactor = mockk<AuthInteractor>()

    //region parameters

    private val parametersMap = mapOf(
        ApiConstants.EMAIL to "user@gmail.com",
        ApiConstants.PASSWORD to "password123"
    )

    private val listOfFields = listOf(
        RegistrationField(
            "",
            "",
            RegistrationFieldType.TEXT,
            "",
            "",
            true,
            true,
            RegistrationField.Restrictions(),
            emptyList()
        ),

        RegistrationField(
            "",
            "",
            RegistrationFieldType.TEXT,
            "",
            "",
            true,
            false,
            RegistrationField.Restrictions(),
            emptyList()
        )
    )

    //endregion

    private val noInternet = "Slow or no internet connection"
    private val somethingWrong = "Something went wrong"

    @Before
    fun before() {
        Dispatchers.setMain(dispatcher)
        every { resourceManager.getString(R.string.core_error_invalid_grant) } returns "Invalid credentials"
        every { resourceManager.getString(R.string.core_error_no_connection) } returns noInternet
        every { resourceManager.getString(R.string.core_error_unknown_error) } returns somethingWrong

    }

    @After
    fun after() {
        Dispatchers.resetMain()
    }

    @Test
    fun `register has validation errors`() = runTest {
        val viewModel = SignUpViewModel(interactor, resourceManager)
        coEvery { interactor.validateRegistrationFields(parametersMap) } returns ValidationFields(
            parametersMap
        )
        coEvery { interactor.register(parametersMap) } returns Unit
        coEvery { interactor.login("", "") } returns Unit
        viewModel.register(parametersMap)
        advanceUntilIdle()
        coVerify(exactly = 1) { interactor.validateRegistrationFields(any()) }
        coVerify(exactly = 0) { interactor.register(any()) }
        coVerify(exactly = 0) { interactor.login(any(), any()) }

        assertEquals(true, viewModel.validationError.value)
        assert(viewModel.successLogin.value != true)
        assert(viewModel.isButtonLoading.value != true)
        assertEquals(null, viewModel.uiMessage.value)
    }

    @Test
    fun `register no internet error`() = runTest {
        val viewModel = SignUpViewModel(interactor, resourceManager)
        coEvery { interactor.validateRegistrationFields(parametersMap) } throws UnknownHostException()
        coEvery { interactor.register(parametersMap) } returns Unit
        coEvery {
            interactor.login(
                parametersMap.getValue(ApiConstants.EMAIL),
                parametersMap.getValue(ApiConstants.PASSWORD)
            )
        } returns Unit
        viewModel.register(parametersMap)
        advanceUntilIdle()
        coVerify(exactly = 1) { interactor.validateRegistrationFields(any()) }
        coVerify(exactly = 0) { interactor.register(any()) }
        coVerify(exactly = 0) { interactor.login(any(), any()) }

        val message = viewModel.uiMessage.value as? UIMessage.SnackBarMessage

        assertEquals(false, viewModel.validationError.value)
        assert(viewModel.successLogin.value != true)
        assert(viewModel.isButtonLoading.value != true)
        assertEquals(noInternet, message?.message)
    }

    @Test
    fun `something went wrong error`() = runTest {
        val viewModel = SignUpViewModel(interactor, resourceManager)
        coEvery { interactor.validateRegistrationFields(parametersMap) } throws Exception()
        coEvery { interactor.register(parametersMap) } returns Unit
        coEvery { interactor.login("", "") } returns Unit
        viewModel.register(parametersMap)
        advanceUntilIdle()
        coVerify(exactly = 1) { interactor.validateRegistrationFields(any()) }
        coVerify(exactly = 0) { interactor.register(any()) }
        coVerify(exactly = 0) { interactor.login(any(), any()) }

        val message = viewModel.uiMessage.value as? UIMessage.SnackBarMessage

        assertEquals(false, viewModel.validationError.value)
        assert(viewModel.successLogin.value != true)
        assert(viewModel.isButtonLoading.value != true)
        assertEquals(somethingWrong, message?.message)
    }


    @Test
    fun `success register`() = runTest {
        val viewModel = SignUpViewModel(interactor, resourceManager)
        coEvery { interactor.validateRegistrationFields(parametersMap) } returns ValidationFields(
            emptyMap()
        )
        coEvery { interactor.register(parametersMap) } returns Unit
        coEvery {
            interactor.login(
                parametersMap.getValue(ApiConstants.EMAIL),
                parametersMap.getValue(ApiConstants.PASSWORD)
            )
        } returns Unit
        viewModel.register(parametersMap)
        advanceUntilIdle()
        coVerify(exactly = 1) { interactor.validateRegistrationFields(any()) }
        coVerify(exactly = 1) { interactor.register(any()) }
        coVerify(exactly = 1) { interactor.login(any(), any()) }

        assertEquals(false, viewModel.validationError.value)
        assertEquals(false, viewModel.isButtonLoading.value)
        assertEquals(null, viewModel.uiMessage.value)
        assertEquals(true, viewModel.successLogin.value)
    }

    @Test
    fun `getRegistrationFields no internet error`() = runTest {
        val viewModel = SignUpViewModel(interactor, resourceManager)
        coEvery { interactor.getRegistrationFields() } throws UnknownHostException()
        viewModel.getRegistrationFields()
        advanceUntilIdle()
        coVerify(exactly = 1) { interactor.getRegistrationFields() }

        val message = viewModel.uiMessage.value as? UIMessage.SnackBarMessage

        assert(viewModel.uiState.value is SignUpUIState.Loading)
        assertEquals(noInternet, message?.message)
    }

    @Test
    fun `getRegistrationFields unknown error`() = runTest {
        val viewModel = SignUpViewModel(interactor, resourceManager)
        coEvery { interactor.getRegistrationFields() } throws Exception()
        viewModel.getRegistrationFields()
        advanceUntilIdle()
        coVerify(exactly = 1) { interactor.getRegistrationFields() }

        val message = viewModel.uiMessage.value as? UIMessage.SnackBarMessage

        assert(viewModel.uiState.value is SignUpUIState.Loading)
        assertEquals(somethingWrong, message?.message)
    }

    @Test
    fun `getRegistrationFields success`() = runTest {
        val viewModel = SignUpViewModel(interactor, resourceManager)
        coEvery { interactor.getRegistrationFields() } returns listOfFields
        viewModel.getRegistrationFields()
        advanceUntilIdle()
        coVerify(exactly = 1) { interactor.getRegistrationFields() }

        //val fields = viewModel.uiState.value as? SignUpUIState.Fields

        assert(viewModel.uiState.value is SignUpUIState.Fields)
        assertEquals(null, viewModel.uiMessage.value)
    }

}