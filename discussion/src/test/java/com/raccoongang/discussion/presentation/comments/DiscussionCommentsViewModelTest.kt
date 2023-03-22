package com.raccoongang.discussion.presentation.comments

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import com.raccoongang.core.R
import com.raccoongang.core.UIMessage
import com.raccoongang.core.data.storage.PreferencesManager
import com.raccoongang.core.domain.model.Pagination
import com.raccoongang.core.extension.TextConverter
import com.raccoongang.core.system.ResourceManager
import com.raccoongang.discussion.domain.interactor.DiscussionInteractor
import com.raccoongang.discussion.domain.model.CommentsData
import com.raccoongang.discussion.domain.model.DiscussionComment
import com.raccoongang.discussion.domain.model.DiscussionType
import com.raccoongang.discussion.system.notifier.*
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.rules.TestRule
import java.net.UnknownHostException

@OptIn(ExperimentalCoroutinesApi::class)
class DiscussionCommentsViewModelTest {

    @get:Rule
    val testInstantTaskExecutorRule: TestRule = InstantTaskExecutorRule()

    private val dispatcher = UnconfinedTestDispatcher()

    private val resourceManager = mockk<ResourceManager>()
    private val interactor = mockk<DiscussionInteractor>()
    private val preferencesManager = mockk<PreferencesManager>()
    private val notifier = mockk<DiscussionNotifier>(relaxed = true)

    private val noInternet = "Slow or no internet connection"
    private val somethingWrong = "Something went wrong"
    private val commentAddedSuccessfully = "Comment Successfully added"

    //region mockThread

    val mockThread = com.raccoongang.discussion.domain.model.Thread(
        "",
        "",
        "",
        "",
        "",
        "",
        "",
        TextConverter.textToLinkedImageText(""),
        false,
        true,
        20,
        emptyList(),
        false,
        "",
        "",
        "",
        "",
        DiscussionType.DISCUSSION,
        "",
        "",
        "Discussion title long Discussion title long good item",
        true,
        false,
        true,
        21,
        4,
        false,
        false,
        mapOf()
    )

    //endregion

    //region mockComment

    private val mockComment = DiscussionComment(
        "",
        "",
        "",
        "",
        "",
        "",
        "",
        TextConverter.textToLinkedImageText(""),
        false,
        true,
        20,
        emptyList(),
        false,
        "",
        "",
        false,
        "",
        "",
        "",
        21,
        emptyList(),
        emptyMap()
    )

    //endregion


    private val comments = listOf(
        mockComment.copy(id = "0"), mockComment.copy(id = "1")
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        every { resourceManager.getString(R.string.core_error_no_connection) } returns noInternet
        every { resourceManager.getString(R.string.core_error_unknown_error) } returns somethingWrong
        every { resourceManager.getString(com.raccoongang.discussion.R.string.discussion_comment_added) } returns commentAddedSuccessfully
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        clearAllMocks()
    }

    @Test
    fun `getThreadComments no internet connection exception`() = runTest {
        coEvery { interactor.getThreadComments(any(), any()) } throws UnknownHostException()
        val viewModel =
            DiscussionCommentsViewModel(
                interactor,
                resourceManager,
                preferencesManager,
                notifier,
                mockThread
            )

        advanceUntilIdle()

        coVerify(exactly = 0) { interactor.getThreadQuestionComments(any(), any(), any()) }

        val message = viewModel.uiMessage.value as? UIMessage.SnackBarMessage

        assert(noInternet == message?.message)
        assert(viewModel.uiState.value is DiscussionCommentsUIState.Loading)
        assert(viewModel.isUpdating.value == false)
    }

    @Test
    fun `getThreadComments unknown exception`() = runTest {
        coEvery { interactor.getThreadComments(any(), any()) } throws Exception()
        val viewModel =
            DiscussionCommentsViewModel(
                interactor,
                resourceManager,
                preferencesManager,
                notifier,
                mockThread.copy(type = DiscussionType.QUESTION)
            )

        coEvery { interactor.getThreadQuestionComments(any(), any(), any()) } throws Exception()
        advanceUntilIdle()

        coVerify(exactly = 0) { interactor.getThreadComments(any(), any()) }
        coVerify(exactly = 1) { interactor.getThreadQuestionComments(any(), any(), any()) }

        val message = viewModel.uiMessage.value as? UIMessage.SnackBarMessage

        assert(somethingWrong == message?.message)
        assert(viewModel.uiState.value is DiscussionCommentsUIState.Loading)
        assert(viewModel.isUpdating.value == false)
    }

    @Test
    fun `getThreadComments success with next page`() = runTest {
        coEvery { interactor.getThreadQuestionComments(any(), any(), any()) } returns CommentsData(
            comments,
            Pagination(10, "2", 4, "1")
        )
        coEvery { interactor.setThreadRead(any()) } returns mockThread
        val viewModel =
            DiscussionCommentsViewModel(
                interactor,
                resourceManager,
                preferencesManager,
                notifier,
                mockThread.copy(type = DiscussionType.QUESTION)
            )

        advanceUntilIdle()

        coVerify(exactly = 0) { interactor.getThreadComments(any(), any()) }
        coVerify(exactly = 1) { interactor.getThreadQuestionComments(any(), any(), any()) }
        coVerify(exactly = 1) { interactor.setThreadRead(any()) }

        assert(viewModel.uiMessage.value == null)
        assert(viewModel.uiState.value is DiscussionCommentsUIState.Success)
        assert(viewModel.isUpdating.value == false)
        assert(viewModel.canLoadMore.value == true)
    }

    @Test
    fun `getThreadComments success without next page`() = runTest {
        coEvery { interactor.getThreadComments(any(), any()) } returns CommentsData(
            comments,
            Pagination(10, "", 4, "1")
        )
        coEvery { interactor.setThreadRead(any()) } returns mockThread
        val viewModel =
            DiscussionCommentsViewModel(
                interactor,
                resourceManager,
                preferencesManager,
                notifier,
                mockThread
            )

        coEvery { interactor.getThreadQuestionComments(any(), any(), any()) } returns CommentsData(
            comments,
            Pagination(10, "", 4, "1")
        )
        advanceUntilIdle()

        coVerify(exactly = 1) { interactor.getThreadComments(any(), any()) }
        coVerify(exactly = 0) { interactor.getThreadQuestionComments(any(), any(), any()) }
        coVerify(exactly = 1) { interactor.setThreadRead(any()) }

        assert(viewModel.uiMessage.value == null)
        assert(viewModel.uiState.value is DiscussionCommentsUIState.Success)
        assert(viewModel.isUpdating.value == false)
        assert(viewModel.canLoadMore.value == false)
    }

    @Test
    fun `updateThreadComments success`() = runTest {
        coEvery { interactor.getThreadComments(any(), any()) } returns CommentsData(
            comments,
            Pagination(10, "", 4, "1")
        )
        coEvery { interactor.setThreadRead(any()) } returns mockThread
        val viewModel =
            DiscussionCommentsViewModel(
                interactor,
                resourceManager,
                preferencesManager,
                notifier,
                mockThread
            )

        coEvery { interactor.getThreadQuestionComments(any(), any(), any()) } returns CommentsData(
            comments,
            Pagination(10, "", 4, "1")
        )
        viewModel.updateThreadComments()
        advanceUntilIdle()

        coVerify(exactly = 2) { interactor.getThreadComments(any(), any()) }
        coVerify(exactly = 0) { interactor.getThreadQuestionComments(any(), any(), any()) }
        coVerify(exactly = 1) { interactor.setThreadRead(any()) }


        assert(viewModel.uiMessage.value == null)
        assert(viewModel.uiState.value is DiscussionCommentsUIState.Success)
        assert(viewModel.isUpdating.value == false)
        assert(viewModel.canLoadMore.value == false)
    }

    @Test
    fun `fetchMore failure`() = runTest {
        coEvery { interactor.getThreadComments(any(), any()) } returns CommentsData(
            comments,
            Pagination(10, "", 4, "1")
        )
        coEvery { interactor.setThreadRead(any()) } throws UnknownHostException()
        val viewModel =
            DiscussionCommentsViewModel(
                interactor,
                resourceManager,
                preferencesManager,
                notifier,
                mockThread
            )


        coEvery { interactor.getThreadQuestionComments(any(), any(), any()) } returns CommentsData(
            comments,
            Pagination(10, "", 4, "1")
        )
        delay(100)
        viewModel.fetchMore()
        advanceUntilIdle()

        coVerify(exactly = 1) { interactor.getThreadComments(any(), any()) }
        coVerify(exactly = 0) { interactor.getThreadQuestionComments(any(), any(), any()) }
        coVerify(exactly = 1) { interactor.setThreadRead(any()) }

        assert(viewModel.uiMessage.value == null)
        assert(viewModel.uiState.value is DiscussionCommentsUIState.Success)
        assert(viewModel.isUpdating.value == false)
        assert(viewModel.canLoadMore.value == false)
    }

    @Test
    fun `fetchMore success`() = runTest {
        coEvery { interactor.getThreadComments(any(), eq(1)) } returns CommentsData(
            comments,
            Pagination(10, "2", 4, "1")
        )
        val viewModel =
            DiscussionCommentsViewModel(
                interactor,
                resourceManager,
                preferencesManager,
                notifier,
                mockThread
            )

        coEvery { interactor.getThreadComments(any(), eq(2)) } returns CommentsData(
            comments,
            Pagination(10, "", 4, "1")
        )
        delay(100)
        viewModel.fetchMore()
        advanceUntilIdle()

        coVerify(exactly = 2) { interactor.getThreadComments(any(), any()) }
        coVerify(exactly = 0) { interactor.getThreadQuestionComments(any(), any(), any()) }

        assert(viewModel.uiMessage.value == null)
        assert(viewModel.uiState.value is DiscussionCommentsUIState.Success)
        assert(viewModel.isUpdating.value == false)
        assert(viewModel.canLoadMore.value == false)
    }

    @Test
    fun `setThreadUpvoted no internet connection exception`() = runTest {
        coEvery { interactor.getThreadComments(any(), any()) } returns CommentsData(
            comments,
            Pagination(10, "", 4, "1")
        )
        val viewModel =
            DiscussionCommentsViewModel(
                interactor,
                resourceManager,
                preferencesManager,
                notifier,
                mockThread
            )


        coEvery { interactor.setThreadVoted(any(), any()) } throws UnknownHostException()

        viewModel.setThreadUpvoted(true)
        advanceUntilIdle()

        coVerify(exactly = 1) { interactor.setThreadVoted(any(), any()) }

        val message = viewModel.uiMessage.value as? UIMessage.SnackBarMessage
        assert(noInternet == message?.message)
        assert(viewModel.uiState.value is DiscussionCommentsUIState.Success)
    }

    @Test
    fun `setThreadUpvoted unknown exception`() = runTest {
        coEvery { interactor.getThreadComments(any(), any()) } returns CommentsData(
            comments,
            Pagination(10, "", 4, "1")
        )
        val viewModel =
            DiscussionCommentsViewModel(
                interactor,
                resourceManager,
                preferencesManager,
                notifier,
                mockThread
            )

        coEvery { interactor.setThreadVoted(any(), any()) } throws Exception()

        viewModel.setThreadUpvoted(true)
        advanceUntilIdle()

        coVerify(exactly = 1) { interactor.setThreadVoted(any(), any()) }

        val message = viewModel.uiMessage.value as? UIMessage.SnackBarMessage
        assert(somethingWrong == message?.message)
        assert(viewModel.uiState.value is DiscussionCommentsUIState.Success)
    }

    @Test
    fun `setThreadUpvoted success`() = runTest {
        coEvery { interactor.getThreadComments(any(), any()) } returns CommentsData(
            comments,
            Pagination(10, "", 4, "1")
        )
        val viewModel =
            DiscussionCommentsViewModel(
                interactor,
                resourceManager,
                preferencesManager,
                notifier,
                mockThread
            )

        coEvery { interactor.setThreadVoted(any(), any()) } returns mockThread

        viewModel.setThreadUpvoted(true)
        advanceUntilIdle()

        coVerify(exactly = 1) { interactor.setThreadVoted(any(), any()) }

        assert(viewModel.uiMessage.value == null)
        assert(viewModel.uiState.value is DiscussionCommentsUIState.Success)
    }

    @Test
    fun `setCommentReported no internet connection exception`() = runTest {
        coEvery { interactor.getThreadComments(any(), any()) } returns CommentsData(
            comments,
            Pagination(10, "", 4, "1")
        )
        val viewModel =
            DiscussionCommentsViewModel(
                interactor,
                resourceManager,
                preferencesManager,
                notifier,
                mockThread
            )


        coEvery { interactor.setCommentFlagged(any(), any()) } throws UnknownHostException()

        viewModel.setCommentReported("", true)
        advanceUntilIdle()

        coVerify(exactly = 1) { interactor.setCommentFlagged(any(), any()) }

        val message = viewModel.uiMessage.value as? UIMessage.SnackBarMessage
        assert(noInternet == message?.message)
        assert(viewModel.uiState.value is DiscussionCommentsUIState.Success)
    }

    @Test
    fun `setCommentReported unknown exception`() = runTest {
        coEvery { interactor.getThreadComments(any(), any()) } returns CommentsData(
            comments,
            Pagination(10, "", 4, "1")
        )
        val viewModel =
            DiscussionCommentsViewModel(
                interactor,
                resourceManager,
                preferencesManager,
                notifier,
                mockThread
            )


        coEvery { interactor.setCommentFlagged(any(), any()) } throws Exception()

        viewModel.setCommentReported("", true)
        advanceUntilIdle()

        coVerify(exactly = 1) { interactor.setCommentFlagged(any(), any()) }

        val message = viewModel.uiMessage.value as? UIMessage.SnackBarMessage
        assert(somethingWrong == message?.message)
        assert(viewModel.uiState.value is DiscussionCommentsUIState.Success)
    }

    @Test
    fun `setCommentReported success`() = runTest {
        coEvery { interactor.getThreadComments(any(), any()) } returns CommentsData(
            comments,
            Pagination(10, "", 4, "1")
        )
        val viewModel =
            DiscussionCommentsViewModel(
                interactor,
                resourceManager,
                preferencesManager,
                notifier,
                mockThread
            )

        coEvery { interactor.setCommentFlagged(any(), any()) } returns mockComment.copy(id = "0")

        viewModel.setCommentReported("", true)
        advanceUntilIdle()

        coVerify(exactly = 1) { interactor.setCommentFlagged(any(), any()) }

        assert(viewModel.uiMessage.value == null)
        assert(viewModel.uiState.value is DiscussionCommentsUIState.Success)
    }


    @Test
    fun `setCommentUpvoted no internet connection exception`() = runTest {
        coEvery { interactor.getThreadComments(any(), any()) } returns CommentsData(
            comments,
            Pagination(10, "", 4, "1")
        )
        val viewModel =
            DiscussionCommentsViewModel(
                interactor,
                resourceManager,
                preferencesManager,
                notifier,
                mockThread
            )

        coEvery { interactor.setCommentVoted(any(), any()) } throws UnknownHostException()

        viewModel.setCommentUpvoted("", true)
        advanceUntilIdle()

        coVerify(exactly = 1) { interactor.setCommentVoted(any(), any()) }

        val message = viewModel.uiMessage.value as? UIMessage.SnackBarMessage
        assert(noInternet == message?.message)
        assert(viewModel.uiState.value is DiscussionCommentsUIState.Success)
    }

    @Test
    fun `setCommentUpvoted unknown exception`() = runTest {
        coEvery { interactor.getThreadComments(any(), any()) } returns CommentsData(
            comments,
            Pagination(10, "", 4, "1")
        )
        val viewModel =
            DiscussionCommentsViewModel(
                interactor,
                resourceManager,
                preferencesManager,
                notifier,
                mockThread
            )

        coEvery { interactor.setCommentVoted(any(), any()) } throws Exception()

        viewModel.setCommentUpvoted("", true)
        advanceUntilIdle()

        coVerify(exactly = 1) { interactor.setCommentVoted(any(), any()) }

        val message = viewModel.uiMessage.value as? UIMessage.SnackBarMessage
        assert(somethingWrong == message?.message)
        assert(viewModel.uiState.value is DiscussionCommentsUIState.Success)
    }

    @Test
    fun `setCommentUpvoted success`() = runTest {
        coEvery { interactor.getThreadComments(any(), any()) } returns CommentsData(
            comments,
            Pagination(10, "", 4, "1")
        )
        val viewModel =
            DiscussionCommentsViewModel(
                interactor,
                resourceManager,
                preferencesManager,
                notifier,
                mockThread
            )

        coEvery { interactor.setCommentVoted(any(), any()) } returns mockComment.copy(id = "0")

        viewModel.setCommentUpvoted("", true)
        advanceUntilIdle()

        coVerify(exactly = 1) { interactor.setCommentVoted(any(), any()) }

        assert(viewModel.uiMessage.value == null)
        assert(viewModel.uiState.value is DiscussionCommentsUIState.Success)
    }

    @Test
    fun `setThreadReported no internet connection exception`() = runTest {
        coEvery { interactor.getThreadComments(any(), any()) } returns CommentsData(
            comments,
            Pagination(10, "", 4, "1")
        )
        val viewModel =
            DiscussionCommentsViewModel(
                interactor,
                resourceManager,
                preferencesManager,
                notifier,
                mockThread
            )

        coEvery { interactor.setThreadFlagged(any(), any()) } throws UnknownHostException()

        viewModel.setThreadReported(true)
        advanceUntilIdle()

        coVerify(exactly = 1) { interactor.setThreadFlagged(any(), any()) }

        val message = viewModel.uiMessage.value as? UIMessage.SnackBarMessage
        assert(noInternet == message?.message)
        assert(viewModel.uiState.value is DiscussionCommentsUIState.Success)
    }

    @Test
    fun `setThreadReported unknown exception`() = runTest {
        coEvery { interactor.getThreadComments(any(), any()) } returns CommentsData(
            comments,
            Pagination(10, "", 4, "1")
        )
        val viewModel =
            DiscussionCommentsViewModel(
                interactor,
                resourceManager,
                preferencesManager,
                notifier,
                mockThread
            )

        coEvery { interactor.setThreadFlagged(any(), any()) } throws Exception()

        viewModel.setThreadReported(true)
        advanceUntilIdle()

        coVerify(exactly = 1) { interactor.setThreadFlagged(any(), any()) }

        val message = viewModel.uiMessage.value as? UIMessage.SnackBarMessage
        assert(somethingWrong == message?.message)
        assert(viewModel.uiState.value is DiscussionCommentsUIState.Success)
    }

    @Test
    fun `setThreadReported success`() = runTest {
        coEvery { interactor.getThreadComments(any(), any()) } returns CommentsData(
            comments,
            Pagination(10, "", 4, "1")
        )
        val viewModel =
            DiscussionCommentsViewModel(
                interactor,
                resourceManager,
                preferencesManager,
                notifier,
                mockThread
            )

        coEvery { interactor.setThreadFlagged(any(), any()) } returns mockThread

        viewModel.setThreadReported(true)
        advanceUntilIdle()

        coVerify(exactly = 1) { interactor.setThreadFlagged(any(), any()) }

        assert(viewModel.uiMessage.value == null)
        assert(viewModel.uiState.value is DiscussionCommentsUIState.Success)
    }


    @Test
    fun `setThreadFollowed no internet connection exception`() = runTest {
        coEvery { interactor.getThreadComments(any(), any()) } returns CommentsData(
            comments,
            Pagination(10, "", 4, "1")
        )
        val viewModel =
            DiscussionCommentsViewModel(
                interactor,
                resourceManager,
                preferencesManager,
                notifier,
                mockThread
            )

        coEvery { interactor.setThreadFollowed(any(), any()) } throws UnknownHostException()

        viewModel.setThreadFollowed(true)
        advanceUntilIdle()

        coVerify(exactly = 1) { interactor.setThreadFollowed(any(), any()) }

        val message = viewModel.uiMessage.value as? UIMessage.SnackBarMessage
        assert(noInternet == message?.message)
        assert(viewModel.uiState.value is DiscussionCommentsUIState.Success)
    }

    @Test
    fun `setThreadFollowed unknown exception`() = runTest {
        coEvery { interactor.getThreadComments(any(), any()) } returns CommentsData(
            comments,
            Pagination(10, "", 4, "1")
        )
        val viewModel =
            DiscussionCommentsViewModel(
                interactor,
                resourceManager,
                preferencesManager,
                notifier,
                mockThread
            )


        coEvery { interactor.setThreadFollowed(any(), any()) } throws Exception()

        viewModel.setThreadFollowed(true)
        advanceUntilIdle()

        coVerify(exactly = 1) { interactor.setThreadFollowed(any(), any()) }

        val message = viewModel.uiMessage.value as? UIMessage.SnackBarMessage
        assert(somethingWrong == message?.message)
        assert(viewModel.uiState.value is DiscussionCommentsUIState.Success)
    }

    @Test
    fun `setThreadFollowed success`() = runTest {
        coEvery { interactor.getThreadComments(any(), any()) } returns CommentsData(
            comments,
            Pagination(10, "", 4, "1")
        )
        val viewModel =
            DiscussionCommentsViewModel(
                interactor,
                resourceManager,
                preferencesManager,
                notifier,
                mockThread
            )

        coEvery { interactor.setThreadFollowed(any(), any()) } returns mockThread

        viewModel.setThreadFollowed(true)
        advanceUntilIdle()

        coVerify(exactly = 1) { interactor.setThreadFollowed(any(), any()) }

        assert(viewModel.uiMessage.value == null)
        assert(viewModel.uiState.value is DiscussionCommentsUIState.Success)
    }

    @Test
    fun `DiscussionCommentAdded notifier test all comments loaded`() = runTest {
        coEvery { interactor.getThreadComments(any(), any()) } returns CommentsData(
            comments,
            Pagination(10, "", 4, "1")
        )
        val viewModel =
            DiscussionCommentsViewModel(
                interactor,
                resourceManager,
                preferencesManager,
                notifier,
                mockThread
            )

        coEvery { notifier.notifier } returns flow {
            delay(100)
            emit(DiscussionCommentAdded(mockComment))
        }
        coEvery { notifier.send(DiscussionThreadDataChanged(mockThread)) } returns Unit

        val mockLifeCycleOwner: LifecycleOwner = mockk()
        val lifecycleRegistry = LifecycleRegistry(mockLifeCycleOwner)
        lifecycleRegistry.addObserver(viewModel)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)

        advanceUntilIdle()

        assert(viewModel.uiMessage.value == null)
        assert(viewModel.uiState.value is DiscussionCommentsUIState.Success)
        assert(viewModel.scrollToBottom.value == true)
    }


    @Test
    fun `DiscussionCommentAdded notifier test all comments not loaded`() = runTest {
        coEvery { interactor.getThreadComments(any(), any()) } returns CommentsData(
            comments,
            Pagination(10, "2", 4, "1")
        )
        val viewModel =
            DiscussionCommentsViewModel(
                interactor,
                resourceManager,
                preferencesManager,
                notifier,
                mockThread
            )

        coEvery { notifier.notifier } returns flow {
            delay(100)
            emit(DiscussionCommentAdded(mockComment))
        }
        coEvery { notifier.send(DiscussionThreadDataChanged(mockk())) } returns Unit

        val mockLifeCycleOwner: LifecycleOwner = mockk()
        val lifecycleRegistry = LifecycleRegistry(mockLifeCycleOwner)
        lifecycleRegistry.addObserver(viewModel)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)

        advanceUntilIdle()

        val message = viewModel.uiMessage.value as? UIMessage.ToastMessage
        assert(commentAddedSuccessfully == message?.message)
        assert(viewModel.uiState.value is DiscussionCommentsUIState.Success)
        assert(viewModel.scrollToBottom.value == null)
    }

    @Test
    fun `DiscussionCommentDataChanged notifier test `() = runTest {
        coEvery { interactor.getThreadComments(any(), any()) } returns CommentsData(
            comments,
            Pagination(10, "2", 4, "1")
        )
        val viewModel =
            DiscussionCommentsViewModel(
                interactor,
                resourceManager,
                preferencesManager,
                notifier,
                mockThread
            )

        coEvery { notifier.notifier } returns flow {
            delay(100)
            emit(DiscussionCommentDataChanged(mockComment.copy(id = "0")))
        }
        coEvery { notifier.send(DiscussionCommentDataChanged(mockComment)) } returns Unit

        val mockLifeCycleOwner: LifecycleOwner = mockk()
        val lifecycleRegistry = LifecycleRegistry(mockLifeCycleOwner)
        lifecycleRegistry.addObserver(viewModel)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)

        advanceUntilIdle()

        assert(viewModel.uiMessage.value == null)
        assert(viewModel.uiState.value is DiscussionCommentsUIState.Success)
    }

    @Test
    fun `createComment no internet connection exception`() = runTest {
        coEvery { interactor.getThreadComments(any(), any()) } returns CommentsData(
            comments,
            Pagination(10, "2", 4, "1")
        )
        val viewModel =
            DiscussionCommentsViewModel(
                interactor,
                resourceManager,
                preferencesManager,
                notifier,
                mockThread
            )
        coEvery { interactor.createComment(any(), any(), any()) } throws UnknownHostException()

        viewModel.createComment("")
        advanceUntilIdle()

        coVerify(exactly = 1) { interactor.createComment(any(), any(), any()) }

        val message = viewModel.uiMessage.value as? UIMessage.SnackBarMessage
        Assert.assertEquals(noInternet, message?.message)

    }

    @Test
    fun `createComment unknown exception`() = runTest {
        coEvery { interactor.getThreadComments(any(), any()) } returns CommentsData(
            comments,
            Pagination(10, "2", 4, "1")
        )
        val viewModel =
            DiscussionCommentsViewModel(
                interactor,
                resourceManager,
                preferencesManager,
                notifier,
                mockThread
            )
        coEvery { interactor.createComment(any(), any(), any()) } throws Exception()

        viewModel.createComment("")
        advanceUntilIdle()

        coVerify(exactly = 1) { interactor.createComment(any(), any(), any()) }

        val message = viewModel.uiMessage.value as? UIMessage.SnackBarMessage
        Assert.assertEquals(somethingWrong, message?.message)

    }

    @Test
    fun `createComment success`() = runTest {
        coEvery { interactor.getThreadComments(any(), any()) } returns CommentsData(
            comments,
            Pagination(10, "2", 4, "1")
        )
        coEvery { interactor.setThreadRead(any()) } returns mockThread
        val viewModel =
            DiscussionCommentsViewModel(
                interactor,
                resourceManager,
                preferencesManager,
                notifier,
                mockThread
            )
        coEvery { interactor.createComment(any(), any(), any()) } returns mockComment
        every { preferencesManager.profile?.username } returns ""
        every { preferencesManager.profile?.profileImage } returns mockk()

        viewModel.createComment("")
        advanceUntilIdle()

        coVerify(exactly = 1) { interactor.createComment(any(), any(), any()) }
        verify(exactly = 2) { preferencesManager.profile }

        assert(viewModel.uiMessage.value != null)
        assert(viewModel.uiState.value is DiscussionCommentsUIState.Success)
    }

    @Test
    fun `sendCommentAdded DiscussionCommentAdded`() = runTest {
        coEvery { interactor.getThreadComments(any(), any()) } returns CommentsData(
            comments,
            Pagination(10, "2", 4, "1")
        )
        val viewModel =
            DiscussionCommentsViewModel(
                interactor,
                resourceManager,
                preferencesManager,
                notifier,
                mockThread
            )
        coEvery { interactor.createComment(any(), any(), any()) } returns mockComment
        every { preferencesManager.profile?.username } returns ""

        viewModel.createComment("")
        advanceUntilIdle()

    }

    @Test
    fun `sendCommentAdded DiscussionResponseAdded`() = runTest {
        coEvery { interactor.getThreadComments(any(), any()) } returns CommentsData(
            comments,
            Pagination(10, "2", 4, "1")
        )
        val viewModel =
            DiscussionCommentsViewModel(
                interactor,
                resourceManager,
                preferencesManager,
                notifier,
                mockThread
            )
        coEvery { interactor.createComment(any(), any(), any()) } returns mockComment
        every { preferencesManager.profile?.username } returns ""

        viewModel.createComment("")
        advanceUntilIdle()

        assert(viewModel.uiState.value is DiscussionCommentsUIState.Success)
    }

    @Test
    fun `sendCommentAdded DiscussionResponseAdded without next page`() = runTest {
        coEvery { interactor.getThreadComments(any(), any()) } returns CommentsData(
            comments,
            Pagination(10, "", 4, "1")
        )
        val viewModel =
            DiscussionCommentsViewModel(
                interactor,
                resourceManager,
                preferencesManager,
                notifier,
                mockThread
            )
        coEvery { interactor.createComment(any(), any(), any()) } returns mockComment
        every { preferencesManager.profile?.username } returns ""

        viewModel.createComment("")
        advanceUntilIdle()

        assert(viewModel.uiState.value is DiscussionCommentsUIState.Success)

    }

}