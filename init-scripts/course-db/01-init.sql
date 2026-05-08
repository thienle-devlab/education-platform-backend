-- ============================================
-- COURSE SERVICE DATABASE SCHEMA
-- ============================================
-- Database: course_db
-- Version: 3.0
-- Updated: 2026-05-03
-- Changes from v2.0:
--   + FIX: ADD UNIQUE(user_id, exercise_id, attempt_number) vào exercise_attempts
--   + FIX: ADD UNIQUE(user_id, exercise_id, attempt_number) vào speaking_submissions
--   + FIX: ADD UNIQUE(user_id, exercise_id, attempt_number) vào writing_submissions
--   + FIX: get_pending_grading_queue → cast tường minh 'PENDING'::submission_status
--   + FIX: get_pending_grading_queue → cast tường minh 'SPEAKING'/'WRITING' sang submission_type VARCHAR
--   + ADD: index idx_exercise_attempts_user_attempt (user_id, exercise_id) cho phổ biến query
--   + ADD: index idx_speaking_user_exercise, idx_writing_user_exercise
--   + IMPROVE: COMPLETION MESSAGE cập nhật đầy đủ
-- ============================================

-- Enable UUID extension
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- ============================================
-- ENUM TYPES
-- ============================================

-- Skill type enum (4 kỹ năng tiếng Anh)
CREATE TYPE skill_type AS ENUM (
    'LISTENING',
    'SPEAKING',
    'READING',
    'WRITING'
);

-- Course level enum
CREATE TYPE course_level AS ENUM (
    'BEGINNER',
    'ELEMENTARY',
    'INTERMEDIATE',
    'UPPER_INTERMEDIATE',
    'ADVANCED'
);

-- Course status enum
CREATE TYPE course_status AS ENUM (
    'DRAFT',
    'PUBLISHED',
    'ARCHIVED'
);

-- Lesson type enum
CREATE TYPE lesson_type AS ENUM (
    'VIDEO',
    'AUDIO',
    'TEXT',
    'EXERCISE'
);

-- Exercise type enum (10 loại bài tập cho 4 kỹ năng)
CREATE TYPE exercise_type AS ENUM (
    'MULTIPLE_CHOICE',   -- Trắc nghiệm (Listening / Reading)
    'FILL_IN_THE_BLANK', -- Điền vào chỗ trống
    'TRUE_FALSE',        -- Đúng / Sai
    'MATCHING',          -- Nối cột
    'ORDERING',          -- Sắp xếp thứ tự
    'SHORT_ANSWER',      -- Trả lời ngắn (Reading / Writing)
    'ESSAY',             -- Viết bài luận → tạo writing_submissions
    'SPEAKING_RECORD',   -- Ghi âm phát âm → tạo speaking_submissions
    'SPEAKING_REPEAT',   -- Nhắc lại câu   → tạo speaking_submissions
    'DICTATION'          -- Nghe và viết lại (Listening)
);

-- Enrollment status enum
CREATE TYPE enrollment_status AS ENUM (
    'ACTIVE',
    'COMPLETED',
    'CANCELLED',
    'EXPIRED'
);

-- Lesson progress status enum
CREATE TYPE progress_status AS ENUM (
    'NOT_STARTED',
    'IN_PROGRESS',
    'COMPLETED'
);

-- Submission status enum
-- Dùng cho speaking_submissions và writing_submissions (AI grading pipeline)
CREATE TYPE submission_status AS ENUM (
    'PENDING',     -- Chờ AI chấm
    'PROCESSING',  -- Đang xử lý
    'GRADED',      -- Đã chấm xong
    'FAILED'       -- Lỗi khi chấm
);

-- ============================================
-- TABLE: courses
-- ============================================
CREATE TABLE courses (
    id                  UUID            PRIMARY KEY DEFAULT uuid_generate_v4(),
    instructor_id       UUID            NOT NULL,               -- Soft ref → User Service (no FK - microservices)
    title               VARCHAR(255)    NOT NULL,
    slug                VARCHAR(255)    NOT NULL,               -- SEO-friendly URL (e.g. "english-listening-beginner")
    description         TEXT,
    skill_type          skill_type      NOT NULL,               -- LISTENING | SPEAKING | READING | WRITING
    level               course_level    NOT NULL DEFAULT 'BEGINNER',
    status              course_status   NOT NULL DEFAULT 'DRAFT',
    thumbnail_url       TEXT,
    trailer_url         TEXT,                                   -- Preview video URL
    price               NUMERIC(12, 2)  NOT NULL DEFAULT 0,     -- 0 = miễn phí; đơn vị VND
    duration_hours      INTEGER,                                -- Tổng giờ học ước tính
    total_lessons       INTEGER         NOT NULL DEFAULT 0,     -- Denormalized; trigger auto-update
    total_enrollments   INTEGER         NOT NULL DEFAULT 0,     -- Denormalized; trigger auto-update
    average_rating      NUMERIC(3, 2),                          -- Denormalized (0.00–5.00); trigger auto-update
    published_at        TIMESTAMP WITH TIME ZONE,
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uk_courses_slug              UNIQUE  (slug),
    CONSTRAINT chk_courses_price            CHECK   (price >= 0),
    CONSTRAINT chk_courses_rating           CHECK   (average_rating IS NULL OR (average_rating >= 0 AND average_rating <= 5)),
    CONSTRAINT chk_courses_total_lessons    CHECK   (total_lessons >= 0),
    CONSTRAINT chk_courses_total_enroll     CHECK   (total_enrollments >= 0)
);

CREATE INDEX idx_courses_instructor_id  ON courses(instructor_id);
CREATE INDEX idx_courses_skill_type     ON courses(skill_type);
CREATE INDEX idx_courses_level          ON courses(level);
CREATE INDEX idx_courses_status         ON courses(status);
CREATE INDEX idx_courses_slug           ON courses(slug);
CREATE INDEX idx_courses_published_at   ON courses(published_at DESC);
CREATE INDEX idx_courses_price          ON courses(price);
CREATE INDEX idx_courses_skill_status   ON courses(skill_type, status);
CREATE INDEX idx_courses_level_status   ON courses(level, status);

-- Full-text search
CREATE INDEX idx_courses_fulltext ON courses
    USING GIN(to_tsvector('english', title || ' ' || COALESCE(description, '')));

-- Partial index: chỉ khóa PUBLISHED (query catalog thường dùng)
CREATE INDEX idx_courses_published ON courses(skill_type, level, average_rating DESC)
    WHERE status = 'PUBLISHED';

COMMENT ON TABLE courses IS 'Khóa học tiếng Anh với 4 kỹ năng: Listening, Speaking, Reading, Writing';
COMMENT ON COLUMN courses.instructor_id     IS 'Soft reference → users.id trong User Service (không có FK do microservices)';
COMMENT ON COLUMN courses.slug              IS 'SEO-friendly URL slug, phải là unique toàn hệ thống';
COMMENT ON COLUMN courses.price             IS 'Giá khóa học (VND). 0 = miễn phí';
COMMENT ON COLUMN courses.total_lessons     IS 'Denormalized — tự động cập nhật bởi trigger trigger_lessons_count';
COMMENT ON COLUMN courses.total_enrollments IS 'Denormalized — tự động cập nhật bởi trigger trigger_enrollments_count';
COMMENT ON COLUMN courses.average_rating    IS 'Denormalized — tự động cập nhật bởi trigger trigger_rating_update';

-- ============================================
-- TABLE: course_tags
-- ============================================
CREATE TABLE course_tags (
    id          UUID            PRIMARY KEY DEFAULT uuid_generate_v4(),
    course_id   UUID            NOT NULL,
    tag_name    VARCHAR(50)     NOT NULL,

    CONSTRAINT fk_course_tags_course   FOREIGN KEY (course_id) REFERENCES courses(id) ON DELETE CASCADE,
    CONSTRAINT uk_course_tag           UNIQUE (course_id, tag_name)
);

CREATE INDEX idx_course_tags_course_id  ON course_tags(course_id);
CREATE INDEX idx_course_tags_tag_name   ON course_tags(tag_name);

COMMENT ON TABLE course_tags IS 'Tags phân loại khóa học (ví dụ: IELTS, TOEIC, Business English)';

-- ============================================
-- TABLE: sections
-- ============================================
CREATE TABLE sections (
    id          UUID            PRIMARY KEY DEFAULT uuid_generate_v4(),
    course_id   UUID            NOT NULL,
    title       VARCHAR(255)    NOT NULL,
    description TEXT,
    order_index INTEGER         NOT NULL DEFAULT 0,   -- Thứ tự trong khóa học (0-based)
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_sections_course      FOREIGN KEY (course_id) REFERENCES courses(id) ON DELETE CASCADE,
    CONSTRAINT chk_sections_order      CHECK (order_index >= 0),
    CONSTRAINT uk_section_order        UNIQUE (course_id, order_index)   -- Không trùng thứ tự trong cùng khóa
);

CREATE INDEX idx_sections_course_id ON sections(course_id);
CREATE INDEX idx_sections_order     ON sections(course_id, order_index);

COMMENT ON TABLE sections IS 'Chương/phần nhóm các bài học trong một khóa học';
COMMENT ON COLUMN sections.order_index IS 'Thứ tự hiển thị trong khóa học (0-based), unique per course';

-- ============================================
-- TABLE: lessons
-- ============================================
CREATE TABLE lessons (
    id               UUID            PRIMARY KEY DEFAULT uuid_generate_v4(),
    section_id       UUID            NOT NULL,
    title            VARCHAR(255)    NOT NULL,
    description      TEXT,
    lesson_type      lesson_type     NOT NULL,           -- VIDEO | AUDIO | TEXT | EXERCISE
    order_index      INTEGER         NOT NULL DEFAULT 0, -- Thứ tự trong chương (0-based)
    duration_seconds INTEGER,                            -- Thời lượng nội dung (VIDEO/AUDIO)
    content_url      TEXT,                               -- URL nội dung chính (S3 / CDN)
    is_preview       BOOLEAN         NOT NULL DEFAULT FALSE,  -- Xem được không cần enroll
    is_free          BOOLEAN         NOT NULL DEFAULT FALSE,  -- Bài miễn phí trong khóa trả phí
    created_at       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_lessons_section      FOREIGN KEY (section_id) REFERENCES sections(id) ON DELETE CASCADE,
    CONSTRAINT chk_lessons_order       CHECK (order_index >= 0),
    CONSTRAINT chk_lessons_duration    CHECK (duration_seconds IS NULL OR duration_seconds > 0),
    CONSTRAINT uk_lesson_order         UNIQUE (section_id, order_index)  -- Không trùng thứ tự trong cùng chương
);

CREATE INDEX idx_lessons_section_id ON lessons(section_id);
CREATE INDEX idx_lessons_order      ON lessons(section_id, order_index);
CREATE INDEX idx_lessons_type       ON lessons(lesson_type);

COMMENT ON TABLE lessons IS 'Bài học đơn vị trong một chương';
COMMENT ON COLUMN lessons.lesson_type   IS 'VIDEO/AUDIO/TEXT = nội dung học; EXERCISE = bài tập thực hành';
COMMENT ON COLUMN lessons.content_url   IS 'URL file nội dung chính (S3 hoặc CDN)';
COMMENT ON COLUMN lessons.is_preview    IS 'TRUE = xem được mà không cần enroll';
COMMENT ON COLUMN lessons.is_free       IS 'TRUE = bài học miễn phí trong khóa có phí';

-- ============================================
-- TABLE: skill_content
-- ============================================
-- Lưu metadata nội dung đặc thù theo kỹ năng (quan hệ 1:1 với lessons).
-- Cấu trúc content_data JSONB theo từng skill_type:
--   LISTENING : { "audio_url": "...", "transcript": "...", "accent": "British|American" }
--   SPEAKING  : { "sample_audio_url": "...", "phoneme_targets": [...], "ipa_text": "..." }
--   READING   : { "article_text": "...", "word_count": 500, "source_url": "..." }
--   WRITING   : { "prompt": "...", "min_words": 200, "writing_type": "essay|email|report" }
-- ============================================
CREATE TABLE skill_content (
    id            UUID        PRIMARY KEY DEFAULT uuid_generate_v4(),
    lesson_id     UUID        NOT NULL,
    skill_type    skill_type  NOT NULL,
    content_data  JSONB       NOT NULL DEFAULT '{}',   -- Dữ liệu cấu trúc đặc thù theo kỹ năng
    media_url     TEXT,                                 -- File media chính (audio/video)
    thumbnail_url TEXT,
    transcript    TEXT,                                 -- Transcript cho LISTENING / SPEAKING
    created_at    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_skill_content_lesson  FOREIGN KEY (lesson_id) REFERENCES lessons(id) ON DELETE CASCADE,
    CONSTRAINT uk_skill_content_lesson  UNIQUE (lesson_id)   -- Enforce 1:1 với lessons
);

CREATE INDEX idx_skill_content_lesson_id  ON skill_content(lesson_id);
CREATE INDEX idx_skill_content_skill_type ON skill_content(skill_type);
CREATE INDEX idx_skill_content_data       ON skill_content USING GIN(content_data);

COMMENT ON TABLE skill_content IS 'Metadata nội dung đặc thù theo kỹ năng — quan hệ 1:1 với lessons (UNIQUE lesson_id)';
COMMENT ON COLUMN skill_content.content_data IS 'JSONB chứa dữ liệu có cấu trúc theo skill_type (xem comment bảng)';
COMMENT ON COLUMN skill_content.transcript   IS 'Transcript đầy đủ cho bài LISTENING và SPEAKING';

-- ============================================
-- TABLE: exercises
-- ============================================
CREATE TABLE exercises (
    id                  UUID            PRIMARY KEY DEFAULT uuid_generate_v4(),
    lesson_id           UUID            NOT NULL,
    exercise_type       exercise_type   NOT NULL,
    question            TEXT            NOT NULL,
    question_audio_url  TEXT,                           -- Audio câu hỏi (Listening / Speaking)
    options             JSONB,                          -- Đáp án lựa chọn (MCQ, MATCHING, v.v.)
    correct_answer      JSONB           NOT NULL,       -- Đáp án đúng ở dạng JSON
    explanation         TEXT,                           -- Giải thích hiển thị sau khi trả lời
    points              INTEGER         NOT NULL DEFAULT 1,
    order_index         INTEGER         NOT NULL DEFAULT 0,
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_exercises_lesson      FOREIGN KEY (lesson_id) REFERENCES lessons(id) ON DELETE CASCADE,
    CONSTRAINT chk_exercises_points     CHECK (points > 0),
    CONSTRAINT chk_exercises_order      CHECK (order_index >= 0)
);

CREATE INDEX idx_exercises_lesson_id  ON exercises(lesson_id);
CREATE INDEX idx_exercises_type       ON exercises(exercise_type);
CREATE INDEX idx_exercises_order      ON exercises(lesson_id, order_index);
CREATE INDEX idx_exercises_options    ON exercises USING GIN(options);

COMMENT ON TABLE exercises IS 'Bài tập thực hành cho 4 kỹ năng (10 loại exercise_type)';
COMMENT ON COLUMN exercises.options         IS 'JSONB: [{"id":"A","text":"..."},...] cho MCQ; {"left":[...],"right":[...]} cho MATCHING';
COMMENT ON COLUMN exercises.correct_answer  IS 'JSONB: "A" cho MCQ; ["A","C"] cho multi-select; {"pairs":[[0,2],...]} cho MATCHING';

-- ============================================
-- TABLE: enrollments
-- ============================================
CREATE TABLE enrollments (
    id               UUID                PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id          UUID                NOT NULL,               -- Soft ref → User Service
    course_id        UUID                NOT NULL,
    status           enrollment_status   NOT NULL DEFAULT 'ACTIVE',
    progress_percent NUMERIC(5, 2)       NOT NULL DEFAULT 0,     -- 0.00–100.00; trigger auto-update
    enrolled_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at     TIMESTAMP WITH TIME ZONE,
    expired_at       TIMESTAMP WITH TIME ZONE,
    last_accessed_at TIMESTAMP WITH TIME ZONE,
    created_at       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_enrollments_course        FOREIGN KEY (course_id) REFERENCES courses(id) ON DELETE CASCADE,
    CONSTRAINT uk_enrollment                UNIQUE (user_id, course_id),   -- Mỗi user chỉ enroll 1 lần
    CONSTRAINT chk_enrollment_progress      CHECK (progress_percent >= 0 AND progress_percent <= 100),
    CONSTRAINT chk_enrollment_completed_at  CHECK (
        (status = 'COMPLETED' AND completed_at IS NOT NULL) OR
        (status != 'COMPLETED')
    )
);

CREATE INDEX idx_enrollments_user_id        ON enrollments(user_id);
CREATE INDEX idx_enrollments_course_id      ON enrollments(course_id);
CREATE INDEX idx_enrollments_status         ON enrollments(status);
CREATE INDEX idx_enrollments_user_course    ON enrollments(user_id, course_id);
CREATE INDEX idx_enrollments_last_accessed  ON enrollments(last_accessed_at DESC);

-- Partial index cho active enrollments (query phổ biến nhất)
CREATE INDEX idx_enrollments_active ON enrollments(user_id, course_id)
    WHERE status = 'ACTIVE';

COMMENT ON TABLE enrollments IS 'Theo dõi việc đăng ký khóa học của user';
COMMENT ON COLUMN enrollments.user_id          IS 'Soft reference → users.id trong User Service (không có FK)';
COMMENT ON COLUMN enrollments.progress_percent IS 'Tỷ lệ hoàn thành khóa học (0–100), tự động cập nhật bởi trigger';
COMMENT ON COLUMN enrollments.last_accessed_at IS 'Lần cuối user truy cập bất kỳ bài học nào của khóa';

-- ============================================
-- TABLE: lesson_progress
-- ============================================
-- Bảng giao giữa enrollments và lessons.
-- UNIQUE(enrollment_id, lesson_id) đảm bảo mỗi bài học chỉ có
-- một bản ghi progress trên mỗi lần enroll.
-- ============================================
CREATE TABLE lesson_progress (
    id                 UUID            PRIMARY KEY DEFAULT uuid_generate_v4(),
    enrollment_id      UUID            NOT NULL,
    lesson_id          UUID            NOT NULL,
    status             progress_status NOT NULL DEFAULT 'NOT_STARTED',
    score              NUMERIC(5, 2),                              -- Điểm bài EXERCISE (0–100)
    time_spent_seconds INTEGER         NOT NULL DEFAULT 0,         -- Tổng thời gian đã học
    completed_at       TIMESTAMP WITH TIME ZONE,
    created_at         TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at         TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_lesson_progress_enrollment  FOREIGN KEY (enrollment_id) REFERENCES enrollments(id) ON DELETE CASCADE,
    CONSTRAINT fk_lesson_progress_lesson      FOREIGN KEY (lesson_id)     REFERENCES lessons(id)     ON DELETE CASCADE,
    CONSTRAINT uk_lesson_progress             UNIQUE (enrollment_id, lesson_id),
    CONSTRAINT chk_lesson_progress_score      CHECK (score IS NULL OR (score >= 0 AND score <= 100)),
    CONSTRAINT chk_lesson_progress_time       CHECK (time_spent_seconds >= 0),
    CONSTRAINT chk_lesson_progress_completed  CHECK (
        (status = 'COMPLETED' AND completed_at IS NOT NULL) OR
        (status != 'COMPLETED')
    )
);

CREATE INDEX idx_lesson_progress_enrollment_id      ON lesson_progress(enrollment_id);
CREATE INDEX idx_lesson_progress_lesson_id          ON lesson_progress(lesson_id);
CREATE INDEX idx_lesson_progress_status             ON lesson_progress(status);
CREATE INDEX idx_lesson_progress_enrollment_status  ON lesson_progress(enrollment_id, status);

COMMENT ON TABLE lesson_progress IS 'Tiến trình từng bài học của mỗi enrollment — bảng giao giữa enrollments và lessons';
COMMENT ON COLUMN lesson_progress.score              IS 'Điểm bài EXERCISE (0–100), NULL cho bài VIDEO/AUDIO/TEXT';
COMMENT ON COLUMN lesson_progress.time_spent_seconds IS 'Tổng thời gian tích lũy user đã ở trên bài học này';

-- ============================================
-- TABLE: exercise_attempts
-- ============================================
-- Ghi nhận MỖI LẦN thử làm bài của user.
--
-- [QUAN TRỌNG] Convention song song với speaking/writing_submissions:
-- Khi exercise_type là ESSAY, SPEAKING_RECORD hoặc SPEAKING_REPEAT,
-- hệ thống tạo ĐỒNG THỜI hai bản ghi:
--   1. exercise_attempts  → ghi nhận "user đã thử", user_answer = {"submission_id": "<uuid>"}
--   2. speaking_submissions / writing_submissions → lưu nội dung thực để AI chấm
-- Sau khi AI chấm xong, score từ submissions được sync ngược vào exercise_attempts.score.
-- Application layer chịu trách nhiệm giữ hai bản ghi nhất quán.
-- ============================================
CREATE TABLE exercise_attempts (
    id                 UUID        PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id            UUID        NOT NULL,          -- Soft ref → User Service
    exercise_id        UUID        NOT NULL,
    attempt_number     INTEGER     NOT NULL DEFAULT 1,
    user_answer        JSONB       NOT NULL,           -- Đáp án user nộp (format theo exercise_type)
    is_correct         BOOLEAN,                        -- NULL cho ESSAY / SPEAKING (chờ AI chấm)
    score              NUMERIC(5, 2),                  -- Điểm đạt được (có thể partial)
    time_taken_seconds INTEGER,
    submitted_at       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_exercise_attempts_exercise  FOREIGN KEY (exercise_id) REFERENCES exercises(id) ON DELETE CASCADE,
    -- [FIX v3.0] Ngăn duplicate attempt_number cho cùng user + exercise
    CONSTRAINT uk_exercise_attempt            UNIQUE (user_id, exercise_id, attempt_number),
    CONSTRAINT chk_attempt_number             CHECK (attempt_number >= 1),
    CONSTRAINT chk_attempt_score              CHECK (score IS NULL OR score >= 0)
);

CREATE INDEX idx_exercise_attempts_user_id       ON exercise_attempts(user_id);
CREATE INDEX idx_exercise_attempts_exercise_id   ON exercise_attempts(exercise_id);
CREATE INDEX idx_exercise_attempts_user_exercise ON exercise_attempts(user_id, exercise_id);
CREATE INDEX idx_exercise_attempts_submitted_at  ON exercise_attempts(submitted_at DESC);
CREATE INDEX idx_exercise_attempts_answer        ON exercise_attempts USING GIN(user_answer);

COMMENT ON TABLE exercise_attempts IS 'Mỗi lần user nộp bài cho một exercise (cho phép nhiều lần thử)';
COMMENT ON COLUMN exercise_attempts.user_answer IS 'JSONB format khớp với exercise_type. Với ESSAY/SPEAKING: {"submission_id":"<uuid>"} trỏ sang bảng submissions tương ứng';
COMMENT ON COLUMN exercise_attempts.is_correct  IS 'NULL cho ESSAY và SPEAKING_* — cần AI hoặc chấm thủ công';
COMMENT ON COLUMN exercise_attempts.score       IS 'Sync từ speaking/writing_submissions.ai_score sau khi AI chấm xong';

-- ============================================
-- TABLE: speaking_submissions
-- ============================================
-- Lưu bản ghi âm và điểm AI cho SPEAKING_RECORD / SPEAKING_REPEAT.
-- Tồn tại song song với exercise_attempts (xem convention ở bảng trên).
-- ============================================
CREATE TABLE speaking_submissions (
    id                   UUID                PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id              UUID                NOT NULL,   -- Soft ref → User Service
    exercise_id          UUID                NOT NULL,   -- exercise_type IN ('SPEAKING_RECORD','SPEAKING_REPEAT')
    attempt_number       INTEGER             NOT NULL DEFAULT 1,
    audio_url            TEXT                NOT NULL,   -- URL file ghi âm (S3)
    duration_seconds     INTEGER,
    ai_score             NUMERIC(5, 2),                  -- Điểm tổng AI (0–100)
    fluency_score        NUMERIC(5, 2),                  -- Điểm trôi chảy (0–100)
    pronunciation_score  NUMERIC(5, 2),                  -- Độ chính xác phát âm (0–100)
    feedback_json        JSONB,                          -- Phản hồi chi tiết AI
    status               submission_status   NOT NULL DEFAULT 'PENDING',
    graded_at            TIMESTAMP WITH TIME ZONE,
    submitted_at         TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_speaking_submissions_exercise  FOREIGN KEY (exercise_id) REFERENCES exercises(id) ON DELETE CASCADE,
    -- [FIX v3.0] Đồng bộ constraint với exercise_attempts
    CONSTRAINT uk_speaking_attempt              UNIQUE (user_id, exercise_id, attempt_number),
    CONSTRAINT chk_speaking_attempt             CHECK (attempt_number >= 1),
    CONSTRAINT chk_speaking_ai_score            CHECK (ai_score IS NULL OR (ai_score >= 0 AND ai_score <= 100)),
    CONSTRAINT chk_speaking_fluency             CHECK (fluency_score IS NULL OR (fluency_score >= 0 AND fluency_score <= 100)),
    CONSTRAINT chk_speaking_pronunciation       CHECK (pronunciation_score IS NULL OR (pronunciation_score >= 0 AND pronunciation_score <= 100))
);

CREATE INDEX idx_speaking_user_id        ON speaking_submissions(user_id);
CREATE INDEX idx_speaking_exercise_id    ON speaking_submissions(exercise_id);
CREATE INDEX idx_speaking_user_exercise  ON speaking_submissions(user_id, exercise_id);  -- [ADD v3.0]
CREATE INDEX idx_speaking_status         ON speaking_submissions(status);
CREATE INDEX idx_speaking_submitted_at   ON speaking_submissions(submitted_at DESC);
CREATE INDEX idx_speaking_feedback       ON speaking_submissions USING GIN(feedback_json);

-- Partial index cho hàng đợi AI chấm
CREATE INDEX idx_speaking_pending ON speaking_submissions(submitted_at)
    WHERE status = 'PENDING'::submission_status;

COMMENT ON TABLE speaking_submissions IS 'Bản ghi âm nộp cho bài SPEAKING với điểm AI (SPEAKING_RECORD / SPEAKING_REPEAT)';
COMMENT ON COLUMN speaking_submissions.audio_url      IS 'URL file ghi âm trên S3/CDN';
COMMENT ON COLUMN speaking_submissions.feedback_json  IS 'Phản hồi AI: {"phonemes":[...], "suggestions":[...], "highlights":[...]}';
COMMENT ON COLUMN speaking_submissions.status         IS 'submission_status ENUM: PENDING→PROCESSING→GRADED|FAILED';

-- ============================================
-- TABLE: writing_submissions
-- ============================================
-- Lưu bài luận và điểm AI cho ESSAY.
-- Tồn tại song song với exercise_attempts (xem convention ở exercise_attempts).
-- ============================================
CREATE TABLE writing_submissions (
    id                      UUID                PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id                 UUID                NOT NULL,   -- Soft ref → User Service
    exercise_id             UUID                NOT NULL,   -- exercise_type = 'ESSAY'
    attempt_number          INTEGER             NOT NULL DEFAULT 1,
    content                 TEXT                NOT NULL,   -- Nội dung bài viết
    word_count              INTEGER             NOT NULL DEFAULT 0,
    ai_score                NUMERIC(5, 2),                  -- Điểm tổng AI (0–100)
    grammar_score           NUMERIC(5, 2),                  -- Điểm ngữ pháp (0–100)
    vocabulary_score        NUMERIC(5, 2),                  -- Điểm từ vựng (0–100)
    coherence_score         NUMERIC(5, 2),                  -- Điểm mạch lạc (0–100)
    task_achievement_score  NUMERIC(5, 2),                  -- Điểm hoàn thành yêu cầu (0–100)
    feedback_json           JSONB,                          -- Phản hồi chi tiết AI
    status                  submission_status   NOT NULL DEFAULT 'PENDING',
    graded_at               TIMESTAMP WITH TIME ZONE,
    submitted_at            TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_writing_submissions_exercise  FOREIGN KEY (exercise_id) REFERENCES exercises(id) ON DELETE CASCADE,
    -- [FIX v3.0] Đồng bộ constraint với exercise_attempts
    CONSTRAINT uk_writing_attempt               UNIQUE (user_id, exercise_id, attempt_number),
    CONSTRAINT chk_writing_attempt              CHECK (attempt_number >= 1),
    CONSTRAINT chk_writing_word_count           CHECK (word_count >= 0),
    CONSTRAINT chk_writing_ai_score             CHECK (ai_score IS NULL OR (ai_score >= 0 AND ai_score <= 100)),
    CONSTRAINT chk_writing_grammar              CHECK (grammar_score IS NULL OR (grammar_score >= 0 AND grammar_score <= 100)),
    CONSTRAINT chk_writing_vocab                CHECK (vocabulary_score IS NULL OR (vocabulary_score >= 0 AND vocabulary_score <= 100)),
    CONSTRAINT chk_writing_coherence            CHECK (coherence_score IS NULL OR (coherence_score >= 0 AND coherence_score <= 100)),
    CONSTRAINT chk_writing_task                 CHECK (task_achievement_score IS NULL OR (task_achievement_score >= 0 AND task_achievement_score <= 100))
);

CREATE INDEX idx_writing_user_id        ON writing_submissions(user_id);
CREATE INDEX idx_writing_exercise_id    ON writing_submissions(exercise_id);
CREATE INDEX idx_writing_user_exercise  ON writing_submissions(user_id, exercise_id);    -- [ADD v3.0]
CREATE INDEX idx_writing_status         ON writing_submissions(status);
CREATE INDEX idx_writing_submitted_at   ON writing_submissions(submitted_at DESC);
CREATE INDEX idx_writing_feedback       ON writing_submissions USING GIN(feedback_json);

-- Partial index cho hàng đợi AI chấm
CREATE INDEX idx_writing_pending ON writing_submissions(submitted_at)
    WHERE status = 'PENDING'::submission_status;

COMMENT ON TABLE writing_submissions IS 'Bài luận nộp cho bài ESSAY với điểm AI (4 thành phần)';
COMMENT ON COLUMN writing_submissions.content       IS 'Nội dung bài viết đầy đủ của user';
COMMENT ON COLUMN writing_submissions.feedback_json IS 'Phản hồi AI: {"corrections":[...], "suggestions":[...], "highlighted_errors":[...]}';
COMMENT ON COLUMN writing_submissions.status        IS 'submission_status ENUM: PENDING→PROCESSING→GRADED|FAILED';

-- ============================================
-- TABLE: course_reviews
-- ============================================
CREATE TABLE course_reviews (
    id                    UUID        PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id               UUID        NOT NULL,   -- Soft ref → User Service
    course_id             UUID        NOT NULL,
    rating                SMALLINT    NOT NULL,   -- 1–5 sao; trigger cập nhật courses.average_rating
    comment               TEXT,
    is_verified_purchase  BOOLEAN     NOT NULL DEFAULT FALSE,  -- TRUE nếu user đang enrolled
    created_at            TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_reviews_course    FOREIGN KEY (course_id) REFERENCES courses(id) ON DELETE CASCADE,
    CONSTRAINT uk_course_review     UNIQUE (user_id, course_id),   -- Mỗi user chỉ review 1 lần
    CONSTRAINT chk_review_rating    CHECK (rating >= 1 AND rating <= 5)
);

CREATE INDEX idx_reviews_course_id  ON course_reviews(course_id);
CREATE INDEX idx_reviews_user_id    ON course_reviews(user_id);
CREATE INDEX idx_reviews_rating     ON course_reviews(course_id, rating);
CREATE INDEX idx_reviews_created_at ON course_reviews(created_at DESC);

COMMENT ON TABLE course_reviews IS 'Đánh giá và nhận xét khóa học của user (tối đa 1 review/user/khóa)';
COMMENT ON COLUMN course_reviews.is_verified_purchase IS 'TRUE nếu reviewer đang actively enrolled — kiểm tra ở application layer';

-- ============================================
-- TRIGGERS: auto-update updated_at
-- ============================================
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_courses_updated_at
    BEFORE UPDATE ON courses
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER trigger_sections_updated_at
    BEFORE UPDATE ON sections
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER trigger_lessons_updated_at
    BEFORE UPDATE ON lessons
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER trigger_skill_content_updated_at
    BEFORE UPDATE ON skill_content
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER trigger_exercises_updated_at
    BEFORE UPDATE ON exercises
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER trigger_enrollments_updated_at
    BEFORE UPDATE ON enrollments
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER trigger_lesson_progress_updated_at
    BEFORE UPDATE ON lesson_progress
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER trigger_reviews_updated_at
    BEFORE UPDATE ON course_reviews
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- ============================================
-- TRIGGER: auto-update courses.total_lessons
-- ============================================
CREATE OR REPLACE FUNCTION update_course_total_lessons()
RETURNS TRIGGER AS $$
DECLARE
    v_course_id UUID;
BEGIN
    IF TG_OP = 'DELETE' THEN
        SELECT s.course_id INTO v_course_id FROM sections s WHERE s.id = OLD.section_id;
    ELSE
        SELECT s.course_id INTO v_course_id FROM sections s WHERE s.id = NEW.section_id;
    END IF;

    UPDATE courses
    SET total_lessons = (
        SELECT COUNT(*)
        FROM lessons l
        JOIN sections s ON l.section_id = s.id
        WHERE s.course_id = v_course_id
    ),
    updated_at = CURRENT_TIMESTAMP
    WHERE id = v_course_id;

    RETURN NULL;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_lessons_count
    AFTER INSERT OR DELETE ON lessons
    FOR EACH ROW EXECUTE FUNCTION update_course_total_lessons();

-- ============================================
-- TRIGGER: auto-update courses.total_enrollments
-- ============================================
CREATE OR REPLACE FUNCTION update_course_total_enrollments()
RETURNS TRIGGER AS $$
BEGIN
    IF TG_OP = 'INSERT' THEN
        UPDATE courses
        SET total_enrollments = total_enrollments + 1,
            updated_at = CURRENT_TIMESTAMP
        WHERE id = NEW.course_id;
    ELSIF TG_OP = 'DELETE' THEN
        UPDATE courses
        SET total_enrollments = GREATEST(total_enrollments - 1, 0),
            updated_at = CURRENT_TIMESTAMP
        WHERE id = OLD.course_id;
    END IF;
    RETURN NULL;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_enrollments_count
    AFTER INSERT OR DELETE ON enrollments
    FOR EACH ROW EXECUTE FUNCTION update_course_total_enrollments();

-- ============================================
-- TRIGGER: auto-update courses.average_rating
-- ============================================
CREATE OR REPLACE FUNCTION update_course_average_rating()
RETURNS TRIGGER AS $$
DECLARE
    v_course_id UUID;
BEGIN
    IF TG_OP = 'DELETE' THEN
        v_course_id := OLD.course_id;
    ELSE
        v_course_id := NEW.course_id;
    END IF;

    UPDATE courses
    SET average_rating = (
        SELECT ROUND(AVG(rating)::NUMERIC, 2)
        FROM course_reviews
        WHERE course_id = v_course_id
    ),
    updated_at = CURRENT_TIMESTAMP
    WHERE id = v_course_id;

    RETURN NULL;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_rating_update
    AFTER INSERT OR UPDATE OR DELETE ON course_reviews
    FOR EACH ROW EXECUTE FUNCTION update_course_average_rating();

-- ============================================
-- TRIGGER: auto-update enrollments.progress_percent
-- ============================================
-- Khi lesson_progress thay đổi, tính lại tỷ lệ hoàn thành và
-- tự chuyển enrollment sang COMPLETED khi đạt 100%.
-- ============================================
CREATE OR REPLACE FUNCTION update_enrollment_progress()
RETURNS TRIGGER AS $$
DECLARE
    v_total     INTEGER;
    v_completed INTEGER;
    v_progress  NUMERIC;
BEGIN
    SELECT
        COUNT(*),
        COUNT(*) FILTER (WHERE lp.status = 'COMPLETED')
    INTO v_total, v_completed
    FROM lesson_progress lp
    WHERE lp.enrollment_id = NEW.enrollment_id;

    IF v_total > 0 THEN
        v_progress := ROUND((v_completed::NUMERIC / v_total) * 100, 2);
    ELSE
        v_progress := 0;
    END IF;

    UPDATE enrollments
    SET
        progress_percent = v_progress,
        status = CASE
            WHEN v_progress = 100 THEN 'COMPLETED'::enrollment_status
            ELSE status
        END,
        completed_at = CASE
            WHEN v_progress = 100 AND completed_at IS NULL THEN CURRENT_TIMESTAMP
            ELSE completed_at
        END,
        updated_at = CURRENT_TIMESTAMP
    WHERE id = NEW.enrollment_id;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_progress_update
    AFTER INSERT OR UPDATE ON lesson_progress
    FOR EACH ROW EXECUTE FUNCTION update_enrollment_progress();

-- ============================================
-- FUNCTIONS
-- ============================================

-- Lấy curriculum đầy đủ của một khóa học
CREATE OR REPLACE FUNCTION get_course_curriculum(p_course_id UUID)
RETURNS TABLE(
    section_id       UUID,
    section_title    VARCHAR,
    section_order    INTEGER,
    lesson_id        UUID,
    lesson_title     VARCHAR,
    lesson_type      lesson_type,
    lesson_order     INTEGER,
    duration_seconds INTEGER,
    is_preview       BOOLEAN
) AS $$
BEGIN
    RETURN QUERY
    SELECT
        s.id,
        s.title,
        s.order_index,
        l.id,
        l.title,
        l.lesson_type,
        l.order_index,
        l.duration_seconds,
        l.is_preview
    FROM sections s
    JOIN lessons l ON l.section_id = s.id
    WHERE s.course_id = p_course_id
    ORDER BY s.order_index, l.order_index;
END;
$$ LANGUAGE plpgsql;

-- Lấy tóm tắt tiến trình học của user trong một khóa
CREATE OR REPLACE FUNCTION get_user_course_progress(p_user_id UUID, p_course_id UUID)
RETURNS TABLE(
    enrollment_id     UUID,
    progress_percent  NUMERIC,
    status            enrollment_status,
    completed_lessons BIGINT,
    total_lessons     BIGINT,
    last_accessed_at  TIMESTAMP WITH TIME ZONE
) AS $$
BEGIN
    RETURN QUERY
    SELECT
        e.id,
        e.progress_percent,
        e.status,
        COUNT(lp.id) FILTER (WHERE lp.status = 'COMPLETED'),
        COUNT(lp.id),
        e.last_accessed_at
    FROM enrollments e
    LEFT JOIN lesson_progress lp ON lp.enrollment_id = e.id
    WHERE e.user_id = p_user_id AND e.course_id = p_course_id
    GROUP BY e.id, e.progress_percent, e.status, e.last_accessed_at;
END;
$$ LANGUAGE plpgsql;

-- Tìm kiếm khóa học (full-text + filter)
CREATE OR REPLACE FUNCTION search_courses(
    p_search_term TEXT          DEFAULT NULL,
    p_skill_type  skill_type    DEFAULT NULL,
    p_level       course_level  DEFAULT NULL,
    p_max_price   NUMERIC       DEFAULT NULL,
    p_limit       INTEGER       DEFAULT 20,
    p_offset      INTEGER       DEFAULT 0
)
RETURNS TABLE(
    id                UUID,
    title             VARCHAR,
    skill_type        skill_type,
    level             course_level,
    price             NUMERIC,
    average_rating    NUMERIC,
    total_enrollments INTEGER,
    thumbnail_url     TEXT
) AS $$
BEGIN
    RETURN QUERY
    SELECT
        c.id, c.title, c.skill_type, c.level,
        c.price, c.average_rating, c.total_enrollments, c.thumbnail_url
    FROM courses c
    WHERE c.status = 'PUBLISHED'::course_status
        AND (p_skill_type IS NULL OR c.skill_type = p_skill_type)
        AND (p_level IS NULL OR c.level = p_level)
        AND (p_max_price IS NULL OR c.price <= p_max_price)
        AND (p_search_term IS NULL OR
             to_tsvector('english', c.title || ' ' || COALESCE(c.description, ''))
             @@ plainto_tsquery('english', p_search_term))
    ORDER BY
        CASE WHEN p_search_term IS NOT NULL THEN
            ts_rank(
                to_tsvector('english', c.title || ' ' || COALESCE(c.description, '')),
                plainto_tsquery('english', p_search_term)
            )
        END DESC NULLS LAST,
        c.average_rating DESC NULLS LAST,
        c.total_enrollments DESC
    LIMIT p_limit OFFSET p_offset;
END;
$$ LANGUAGE plpgsql;

-- [FIX v3.0] Lấy hàng đợi AI chấm bài (PENDING submissions)
-- Cast tường minh submission_status ENUM và kiểu trả về VARCHAR
CREATE OR REPLACE FUNCTION get_pending_grading_queue(p_limit INTEGER DEFAULT 50)
RETURNS TABLE(
    submission_type VARCHAR,
    submission_id   UUID,
    exercise_id     UUID,
    user_id         UUID,
    submitted_at    TIMESTAMP WITH TIME ZONE
) AS $$
BEGIN
    RETURN QUERY
    SELECT
        'SPEAKING'::VARCHAR,
        ss.id,
        ss.exercise_id,
        ss.user_id,
        ss.submitted_at
    FROM speaking_submissions ss
    WHERE ss.status = 'PENDING'::submission_status
    UNION ALL
    SELECT
        'WRITING'::VARCHAR,
        ws.id,
        ws.exercise_id,
        ws.user_id,
        ws.submitted_at
    FROM writing_submissions ws
    WHERE ws.status = 'PENDING'::submission_status
    ORDER BY submitted_at ASC
    LIMIT p_limit;
END;
$$ LANGUAGE plpgsql;

-- ============================================
-- VIEWS
-- ============================================

-- Catalog khóa học đang PUBLISHED
CREATE OR REPLACE VIEW course_catalog AS
SELECT
    c.id,
    c.title,
    c.slug,
    c.skill_type,
    c.level,
    c.price,
    c.duration_hours,
    c.total_lessons,
    c.total_enrollments,
    c.average_rating,
    c.thumbnail_url,
    c.instructor_id,
    c.published_at,
    ARRAY_AGG(DISTINCT ct.tag_name) FILTER (WHERE ct.tag_name IS NOT NULL) AS tags
FROM courses c
LEFT JOIN course_tags ct ON ct.course_id = c.id
WHERE c.status = 'PUBLISHED'::course_status
GROUP BY c.id
ORDER BY c.average_rating DESC NULLS LAST, c.total_enrollments DESC;

-- Dashboard enrollment của user
CREATE OR REPLACE VIEW user_enrollments_view AS
SELECT
    e.id            AS enrollment_id,
    e.user_id,
    e.course_id,
    c.title         AS course_title,
    c.skill_type,
    c.thumbnail_url,
    e.status,
    e.progress_percent,
    e.enrolled_at,
    e.completed_at,
    e.last_accessed_at
FROM enrollments e
JOIN courses c ON c.id = e.course_id
ORDER BY e.last_accessed_at DESC NULLS LAST;

-- Thống kê theo kỹ năng (admin dashboard)
CREATE OR REPLACE VIEW skill_statistics AS
SELECT
    skill_type,
    COUNT(*) FILTER (WHERE status = 'PUBLISHED'::course_status) AS published_courses,
    COUNT(*) FILTER (WHERE status = 'DRAFT'::course_status)     AS draft_courses,
    SUM(total_enrollments)                                        AS total_enrollments,
    ROUND(AVG(average_rating) FILTER (WHERE average_rating IS NOT NULL), 2) AS avg_rating
FROM courses
GROUP BY skill_type
ORDER BY skill_type;

-- ============================================
-- MATERIALIZED VIEW: course_performance_daily
-- ============================================
-- Refresh hàng ngày bằng cron → gọi refresh_course_performance()
-- ============================================
CREATE MATERIALIZED VIEW course_performance_daily AS
SELECT
    c.id AS course_id,
    c.title,
    c.skill_type,
    c.level,
    c.total_enrollments,
    c.average_rating,
    COUNT(DISTINCT e.user_id) FILTER (WHERE e.status = 'COMPLETED'::enrollment_status)                   AS completed_students,
    COUNT(DISTINCT e.user_id) FILTER (WHERE e.enrolled_at > CURRENT_DATE - INTERVAL '7 days')            AS new_enrollments_7d,
    ROUND(AVG(e.progress_percent), 2)                                                                     AS avg_progress
FROM courses c
LEFT JOIN enrollments e ON e.course_id = c.id
WHERE c.status = 'PUBLISHED'::course_status
GROUP BY c.id, c.title, c.skill_type, c.level, c.total_enrollments, c.average_rating
ORDER BY c.total_enrollments DESC;

CREATE UNIQUE INDEX idx_course_performance_daily_id ON course_performance_daily(course_id);

CREATE OR REPLACE FUNCTION refresh_course_performance()
RETURNS VOID AS $$
BEGIN
    REFRESH MATERIALIZED VIEW CONCURRENTLY course_performance_daily;
END;
$$ LANGUAGE plpgsql;

-- ============================================
-- GRANTS (uncomment khi deploy)
-- ============================================
-- GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA public TO course_user;
-- GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO course_user;
-- GRANT EXECUTE ON ALL FUNCTIONS IN SCHEMA public TO course_user;

-- ============================================
-- COMPLETION MESSAGE
-- ============================================
DO $$
BEGIN
    RAISE NOTICE '================================================';
    RAISE NOTICE 'Course DB Schema v3.0 Created Successfully!';
    RAISE NOTICE '================================================';
    RAISE NOTICE 'Tables   : 12';
    RAISE NOTICE '  courses, course_tags, sections, lessons';
    RAISE NOTICE '  skill_content, exercises';
    RAISE NOTICE '  enrollments, lesson_progress';
    RAISE NOTICE '  exercise_attempts';
    RAISE NOTICE '  speaking_submissions, writing_submissions';
    RAISE NOTICE '  course_reviews';
    RAISE NOTICE 'Views    : 3 (+ 1 materialized)';
    RAISE NOTICE 'Functions: 5  (get_course_curriculum,';
    RAISE NOTICE '               get_user_course_progress,';
    RAISE NOTICE '               search_courses,';
    RAISE NOTICE '               get_pending_grading_queue,';
    RAISE NOTICE '               refresh_course_performance)';
    RAISE NOTICE 'Triggers : 12 (8x updated_at + 4x business logic)';
    RAISE NOTICE '------------------------------------------------';
    RAISE NOTICE 'Changes from v2.0:';
    RAISE NOTICE '  + FIX UNIQUE(user_id, exercise_id, attempt_number)';
    RAISE NOTICE '        → exercise_attempts (uk_exercise_attempt)';
    RAISE NOTICE '        → speaking_submissions (uk_speaking_attempt)';
    RAISE NOTICE '        → writing_submissions  (uk_writing_attempt)';
    RAISE NOTICE '  + FIX get_pending_grading_queue: ENUM cast tuong minh';
    RAISE NOTICE '  + FIX search_courses, views: ENUM cast tuong minh';
    RAISE NOTICE '  + ADD idx_speaking_user_exercise';
    RAISE NOTICE '  + ADD idx_writing_user_exercise';
    RAISE NOTICE '================================================';
END $$;
