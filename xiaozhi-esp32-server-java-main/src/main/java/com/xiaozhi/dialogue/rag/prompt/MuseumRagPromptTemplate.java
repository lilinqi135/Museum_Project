package com.xiaozhi.dialogue.rag.prompt;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 博物馆问答 Prompt 模板
 */
@Component
public class MuseumRagPromptTemplate {

    public enum AnswerMode {
        VISITOR,
        EDU,
        KIDS
    }

    public AnswerMode normalizeMode(String mode) {
        if (!StringUtils.hasText(mode)) {
            return AnswerMode.VISITOR;
        }
        return switch (mode.trim().toLowerCase()) {
            case "edu", "study", "research" -> AnswerMode.EDU;
            case "kids", "child", "children" -> AnswerMode.KIDS;
            default -> AnswerMode.VISITOR;
        };
    }

    private String styleInstruction(AnswerMode mode) {
        return switch (mode) {
            case EDU -> "你面向研学用户：结构化表达，先结论后依据，可适当补充背景脉络，控制在180-260字。";
            case KIDS -> "你面向儿童用户：语言简单、生动、短句，少术语，可用类比，控制在80-150字。";
            case VISITOR -> "你面向普通游客：自然友好、重点清晰，先答结论再讲亮点，控制在120-180字。";
        };
    }

    public String render(String question, String contextText) {
        return render(question, contextText, null);
    }

    public String render(String question, String contextText, String mode) {
        if (!StringUtils.hasText(question)) {
            return "";
        }
        AnswerMode answerMode = normalizeMode(mode);
        String styleInstruction = styleInstruction(answerMode);
        if (!StringUtils.hasText(contextText)) {
            return """
                    你正在处理一个博物馆导览问题。
                    %s
                    当前未命中馆内知识片段时，请遵循以下回答策略：
                    1) 先给出基于通识的可用回答，不要只回复“查不到”；
                    2) 明确边界：说明“馆内资料暂未覆盖该细节，以下为通识参考”；
                    3) 给出继续探索建议：邀请用户追问2个可选方向。

                    表达要求：自然、友好、像讲解员，不要使用生硬系统话术。

                    【用户问题】
                    %s
                    """.formatted(styleInstruction, question.trim());
        }

        return """
                你正在处理一个与博物馆展品相关的问题。
                %s
                请优先依据“检索到的知识片段”回答，不要编造展品事实。
                如果片段不足以支撑完整结论，请采用“有边界的讲解”：
                1) 先给可用结论；
                2) 明确哪些是知识片段支持、哪些是通识推断；
                3) 提供继续追问方向，而不是生硬拒答。

                输出建议结构：
                - 先用1句话回答核心问题；
                - 再给2-3条关键依据（尽量引用片段细节）；
                - 最后给1句导览式延展建议。

                【检索到的知识片段】
                %s

                【用户问题】
                %s
                """.formatted(styleInstruction, contextText.trim(), question.trim());
    }

    public String renderSystemInstructions(String baseInstructions, String contextText) {
        return renderSystemInstructions(baseInstructions, contextText, null);
    }

    public String renderSystemInstructions(String baseInstructions, String contextText, String mode) {
        String safeBase = StringUtils.hasText(baseInstructions) ? baseInstructions.trim() : "你是一个专业的博物馆导览员。";
        AnswerMode answerMode = normalizeMode(mode);
        String styleInstruction = styleInstruction(answerMode);
        if (!StringUtils.hasText(contextText)) {
            return safeBase + """
                    \n%s
                    \n当前未命中馆内资料时，不要只说“没有信息”。
                    请先给出通识层面的可用回答，再温和说明边界，并引导用户继续提问。
                    语气要像现场讲解员，友好、清晰、不过度学术化。
                    """.formatted(styleInstruction);
        }
        return safeBase + """
                \n%s
                \n请依据以下知识片段讲解，并遵循：
                1) 先答结论；
                2) 用2-3条依据支撑；
                3) 若有不确定处，温和说明边界，不要生硬拒答。
                \n\n【知识片段】\n""".formatted(styleInstruction) + contextText.trim();
    }
}
