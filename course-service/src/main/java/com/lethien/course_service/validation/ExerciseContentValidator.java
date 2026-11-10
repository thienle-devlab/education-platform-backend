package com.lethien.course_service.validation;

import com.lethien.course_service.entity.ExerciseType;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * ExerciseContentValidator — kiểm tra options / correctAnswer khớp với exerciseType.
 *
 * Tại sao cần: ExerciseService.autoGrade() đọc correctAnswer theo format cố định
 * ("answer", "answers", "pairs", "order"). Instructor nhập sai format thì bài sẽ
 * bị chấm sai ÂM THẦM — validate ngay lúc tạo/sửa để lỗi lộ ra sớm.
 *
 * Lỗi ném IllegalArgumentException → GlobalExceptionHandler trả 400 INVALID_ARGUMENT.
 */
public final class ExerciseContentValidator {

    private ExerciseContentValidator() {}

    public static void validate(ExerciseType type,
                                Map<String, Object> options,
                                Map<String, Object> correctAnswer) {
        switch (type) {
            case MULTIPLE_CHOICE    -> validateMultipleChoice(options, correctAnswer);
            case TRUE_FALSE         -> validateTrueFalse(correctAnswer);
            case SHORT_ANSWER,
                 DICTATION          -> requireText(correctAnswer, "answer", type);
            case FILL_IN_THE_BLANK  -> validateFillInTheBlank(correctAnswer);
            case MATCHING           -> validateMatching(options, correctAnswer);
            case ORDERING           -> validateOrdering(correctAnswer);
            case ESSAY,
                 SPEAKING_RECORD,
                 SPEAKING_REPEAT    -> { /* AI chấm — correctAnswer không bắt buộc */ }
        }
    }

    // ---------------- MULTIPLE_CHOICE ----------------

    @SuppressWarnings("unchecked")
    private static void validateMultipleChoice(Map<String, Object> options,
                                               Map<String, Object> correctAnswer) {
        Object raw = options == null ? null : options.get("choices");
        if (!(raw instanceof List<?> list) || list.size() < 2) {
            throw invalid("MULTIPLE_CHOICE requires options.choices with at least 2 items");
        }

        Set<String> ids = new HashSet<>();
        for (Object o : list) {
            if (!(o instanceof Map<?, ?> choice)) {
                throw invalid("each choice must be an object with 'id' and 'text'");
            }
            String id = textOf(choice.get("id"));
            String text = textOf(choice.get("text"));
            if (id.isBlank() || text.isBlank()) {
                throw invalid("each choice needs a non-blank 'id' and 'text'");
            }
            if (!ids.add(id.toLowerCase())) {
                throw invalid("duplicate choice id: " + id);
            }
        }

        String answer = requireText(correctAnswer, "answer", ExerciseType.MULTIPLE_CHOICE);
        if (!ids.contains(answer.toLowerCase())) {
            throw invalid("correctAnswer.answer '" + answer + "' is not one of the choice ids");
        }
    }

    // ---------------- TRUE_FALSE ----------------

    private static void validateTrueFalse(Map<String, Object> correctAnswer) {
        String s = textOf(correctAnswer == null ? null : correctAnswer.get("answer"));
        if (!s.equalsIgnoreCase("true") && !s.equalsIgnoreCase("false")) {
            throw invalid("TRUE_FALSE requires correctAnswer.answer to be TRUE or FALSE");
        }
    }

    // ---------------- FILL_IN_THE_BLANK ----------------

    private static void validateFillInTheBlank(Map<String, Object> correctAnswer) {
        Object raw = correctAnswer == null ? null : correctAnswer.get("answers");
        if (!(raw instanceof List<?> list) || list.isEmpty()) {
            throw invalid("FILL_IN_THE_BLANK requires correctAnswer.answers as a non-empty list");
        }
        for (Object o : list) {
            if (!(o instanceof String s) || s.isBlank()) {
                throw invalid("every item in correctAnswer.answers must be a non-blank string");
            }
        }
    }

    // ---------------- MATCHING ----------------

    private static void validateMatching(Map<String, Object> options,
                                         Map<String, Object> correctAnswer) {
        int left  = listSize(options, "left");
        int right = listSize(options, "right");

        Object raw = correctAnswer == null ? null : correctAnswer.get("pairs");
        if (!(raw instanceof List<?> pairs) || pairs.isEmpty()) {
            throw invalid("MATCHING requires correctAnswer.pairs as a non-empty list");
        }
        for (Object p : pairs) {
            if (!(p instanceof List<?> pair) || pair.size() != 2
                    || !isIndex(pair.get(0), left) || !isIndex(pair.get(1), right)) {
                throw invalid("each pair must be [leftIndex, rightIndex] within range");
            }
        }
    }

    private static int listSize(Map<String, Object> options, String key) {
        Object raw = options == null ? null : options.get(key);
        if (!(raw instanceof List<?> list) || list.isEmpty()) {
            throw invalid("MATCHING requires options.left and options.right as non-empty lists");
        }
        return list.size();
    }

    // ---------------- ORDERING ----------------

    private static void validateOrdering(Map<String, Object> correctAnswer) {
        Object raw = correctAnswer == null ? null : correctAnswer.get("order");
        if (!(raw instanceof List<?> order) || order.isEmpty()) {
            throw invalid("ORDERING requires correctAnswer.order as a non-empty list");
        }
        Set<Integer> seen = new HashSet<>();
        for (Object o : order) {
            if (!isIndex(o, order.size()) || !seen.add(((Number) o).intValue())) {
                throw invalid("correctAnswer.order must be a permutation of 0.." + (order.size() - 1));
            }
        }
    }

    // ---------------- helpers ----------------

    private static String requireText(Map<String, Object> correctAnswer, String key, ExerciseType type) {
        Object v = correctAnswer == null ? null : correctAnswer.get(key);
        if (!(v instanceof String s) || s.isBlank()) {
            throw invalid(type + " requires correctAnswer." + key + " as a non-blank string");
        }
        return s.trim();
    }

    private static boolean isIndex(Object o, int size) {
        return o instanceof Number n
                && n.doubleValue() == n.intValue()
                && n.intValue() >= 0
                && n.intValue() < size;
    }

    private static String textOf(Object o) {
        return o == null ? "" : o.toString().trim();
    }

    private static IllegalArgumentException invalid(String message) {
        return new IllegalArgumentException("Invalid exercise content: " + message);
    }
}
