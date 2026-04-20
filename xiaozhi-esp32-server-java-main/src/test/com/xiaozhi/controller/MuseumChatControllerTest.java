package com.xiaozhi.controller;

import com.xiaozhi.common.web.ResultMessage;
import com.xiaozhi.dialogue.llm.factory.ChatModelFactory;
import com.xiaozhi.dialogue.rag.config.RagProperties;
import com.xiaozhi.dialogue.rag.service.MuseumRagPromptService;
import com.xiaozhi.dialogue.service.MuseumRagsService;
import com.xiaozhi.service.SysDeviceService;
import com.xiaozhi.service.SysExhibitService;
import com.xiaozhi.service.SysRoleService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class MuseumChatControllerTest {

    @Mock
    private MuseumRagsService museumRagsService;
    @Mock
    private ChatModelFactory chatModelFactory;
    @Mock
    private MuseumRagPromptService ragPromptService;
    @Mock
    private RagProperties ragProperties;
    @Mock
    private SysRoleService roleService;
    @Mock
    private SysDeviceService deviceService;
    @Mock
    private SysExhibitService exhibitService;

    private MuseumChatController controller;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        controller = new MuseumChatController();
        ReflectionTestUtils.setField(controller, "museumRagsService", museumRagsService);
        ReflectionTestUtils.setField(controller, "chatModelFactory", chatModelFactory);
        ReflectionTestUtils.setField(controller, "ragPromptService", ragPromptService);
        ReflectionTestUtils.setField(controller, "ragProperties", ragProperties);
        ReflectionTestUtils.setField(controller, "roleService", roleService);
        ReflectionTestUtils.setField(controller, "deviceService", deviceService);
        ReflectionTestUtils.setField(controller, "exhibitService", exhibitService);
    }

    @Test
    void chatShouldRejectMissingMuseumId() {
        MuseumChatController.MuseumChatParam param = new MuseumChatController.MuseumChatParam();
        param.setQuestion("这件展品是什么时候的？");

        ResultMessage result = controller.chat(param);

        assertEquals(400, result.getCode());
        assertEquals("museumId 不能为空", result.getMessage());
        verifyNoInteractions(exhibitService, roleService, ragPromptService, chatModelFactory);
    }

    @Test
    void chatShouldRejectInvalidExhibitOwnership() {
        MuseumChatController.MuseumChatParam param = new MuseumChatController.MuseumChatParam();
        param.setMuseumId(1L);
        param.setExhibitId(10L);
        param.setQuestion("这件展品是什么时候的？");
        when(exhibitService.exists(10L, 1L)).thenReturn(false);

        ResultMessage result = controller.chat(param);

        assertEquals(400, result.getCode());
        assertEquals("展品不存在或不属于当前场馆", result.getMessage());
        verifyNoInteractions(roleService, ragPromptService, chatModelFactory);
    }

    @Test
    void chatShouldRejectUnsupportedMode() {
        MuseumChatController.MuseumChatParam param = new MuseumChatController.MuseumChatParam();
        param.setMuseumId(1L);
        param.setQuestion("这件展品是什么时候的？");
        param.setMode("expert");

        ResultMessage result = controller.chat(param);

        assertEquals(400, result.getCode());
        assertEquals("mode 仅支持 visitor、edu、kids", result.getMessage());
        verifyNoInteractions(exhibitService, roleService, ragPromptService, chatModelFactory);
    }
}
