package com.xiaozhi.utils;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

/**
 * 退出关键词检测器
 * 用于检测用户输入中是否包含明确的退出意图关键词
 * 注意：这个类不再是 Spring 组件，而是作为工具类被 IntentDetector 使用
 */
public class ExitKeywordDetector {

    /**
     * 退出关键词列表
     * 包含各种表达退出、结束对话的词汇
     */
    private static final List<String> EXIT_KEYWORDS = Arrays.asList(
            "拜拜",
            "再见",
            "退下",
            "走了",
            "我走了",
            "我要走了",
            "结束对话",
            "退出",
            "下线",
            "结束",
            "告辞",
            "告退",
            "离开",
            "goodbye",
            "bye",
            "bye bye",
            "byebye",
            "see you",
            "see ya"
    );

    /**
     * 精确匹配的短语模式
     * 这些短语必须完整出现才算匹配
     */
    private static final List<Pattern> EXACT_PATTERNS = Arrays.asList(
            Pattern.compile(".*拜拜.*", Pattern.CASE_INSENSITIVE),
            Pattern.compile(".*再见.*", Pattern.CASE_INSENSITIVE),
            Pattern.compile(".*退下.*", Pattern.CASE_INSENSITIVE),
            Pattern.compile(".*走了.*", Pattern.CASE_INSENSITIVE),
            Pattern.compile(".*我?要?走了.*", Pattern.CASE_INSENSITIVE),
            Pattern.compile(".*结束对话.*", Pattern.CASE_INSENSITIVE),
            Pattern.compile(".*退出.*", Pattern.CASE_INSENSITIVE),
            Pattern.compile(".*告辞.*", Pattern.CASE_INSENSITIVE),
            Pattern.compile(".*告退.*", Pattern.CASE_INSENSITIVE),
            Pattern.compile(".*(?:我|你)?(?:先)?(?:要)?离开.*", Pattern.CASE_INSENSITIVE),
            Pattern.compile(".*(?:我|你)?(?:先)?下线.*", Pattern.CASE_INSENSITIVE),
            Pattern.compile(".*bye\\s*bye.*", Pattern.CASE_INSENSITIVE),
            Pattern.compile(".*goodbye.*", Pattern.CASE_INSENSITIVE),
            Pattern.compile(".*see\\s+you.*", Pattern.CASE_INSENSITIVE),
            Pattern.compile(".*see\\s+ya.*", Pattern.CASE_INSENSITIVE)
    );

    /**
     * 排除的短语模式
     * 包含这些短语时不应该触发退出
     * 例如："不要退出"、"别走"、"不离开" 等
     */
    private static final List<Pattern> EXCLUDE_PATTERNS = Arrays.asList(
            Pattern.compile(".*不.*(?:退出|离开|走|退下|结束).*", Pattern.CASE_INSENSITIVE),
            Pattern.compile(".*别.*(?:退出|离开|走|退下|结束).*", Pattern.CASE_INSENSITIVE),
            Pattern.compile(".*不要.*(?:退出|离开|走|退下|结束).*", Pattern.CASE_INSENSITIVE),
            Pattern.compile(".*为什么.*(?:退出|离开|走|退下|结束).*", Pattern.CASE_INSENSITIVE),
            Pattern.compile(".*怎么.*(?:退出|离开|走|退下|结束).*", Pattern.CASE_INSENSITIVE),
            Pattern.compile(".*如何.*(?:退出|离开|走|退下|结束).*", Pattern.CASE_INSENSITIVE),
            Pattern.compile(".*能否.*(?:退出|离开|走|退下|结束).*", Pattern.CASE_INSENSITIVE),
            Pattern.compile(".*可以.*(?:退出|离开|走|退下|结束).*", Pattern.CASE_INSENSITIVE),
            Pattern.compile(".*会.*(?:退出|离开|走|退下|结束).*", Pattern.CASE_INSENSITIVE),
            Pattern.compile(".*什么.*(?:退出|离开|走|退下|结束).*", Pattern.CASE_INSENSITIVE),
            Pattern.compile(".*don't.*(?:leave|exit|quit|bye).*", Pattern.CASE_INSENSITIVE),
            Pattern.compile(".*not.*(?:leave|exit|quit|bye).*", Pattern.CASE_INSENSITIVE)
    );

    /**
     * 检测输入文本是否包含退出意图
     * 
     * @param input 用户输入的文本
     * @return 如果检测到退出意图返回 true，否则返回 false
     */
    public boolean detectExitIntent(String input) {
        if (input == null || input.trim().isEmpty()) {
            return false;
        }

        // 去除空格和标点符号，统一转为小写
        String normalizedInput = input.trim().toLowerCase();
        
        // 首先检查排除模式，如果匹配到排除模式则不触发退出
        for (Pattern excludePattern : EXCLUDE_PATTERNS) {
            if (excludePattern.matcher(normalizedInput).matches()) {
                return false;
            }
        }

        // 检查精确匹配模式
        for (Pattern pattern : EXACT_PATTERNS) {
            if (pattern.matcher(normalizedInput).matches()) {
                return true;
            }
        }

        // 检查简单关键词（适用于单独的短消息）
        // 只有当输入很短（少于15个字符）时才使用简单关键词匹配
        if (normalizedInput.length() <= 15) {
            for (String keyword : EXIT_KEYWORDS) {
                if (normalizedInput.contains(keyword.toLowerCase())) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * 获取所有退出关键词（用于测试和调试）
     * 
     * @return 退出关键词列表
     */
    public List<String> getExitKeywords() {
        return EXIT_KEYWORDS;
    }
}

