package com.sis.iids.collab;

import com.sis.iids.common.error.BusinessException;
import com.sis.iids.common.error.ErrorCode;
import com.sis.iids.scenario.ScenarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * R-15 协同编辑服务（FR-04-02）。
 * 评论（@提及解析）+ 变更时间线（版本递增）+ 在线心跳 + SSE 推送（D2 选型 A）。
 */
@Service
public class CollabService {

    private static final Pattern MENTION_PATTERN = Pattern.compile("@([\\w\\u4e00-\\u9fa5]+)");
    private static final int PRESENCE_ONLINE_SECONDS = 120;

    private final ScenarioRepository scenarioRepository;
    private final ScenarioCommentRepository commentRepository;
    private final ScenarioChangeRepository changeRepository;
    private final ScenarioPresenceRepository presenceRepository;
    private final CollabEventBus eventBus;

    public CollabService(ScenarioRepository scenarioRepository,
                         ScenarioCommentRepository commentRepository,
                         ScenarioChangeRepository changeRepository,
                         ScenarioPresenceRepository presenceRepository,
                         CollabEventBus eventBus) {
        this.scenarioRepository = scenarioRepository;
        this.commentRepository = commentRepository;
        this.changeRepository = changeRepository;
        this.presenceRepository = presenceRepository;
        this.eventBus = eventBus;
    }

    // ============================================================
    // SSE 订阅
    // ============================================================
    public SseEmitter subscribe(Long scenarioId) {
        if (!scenarioRepository.existsById(scenarioId)) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "测算方案不存在");
        }
        return eventBus.subscribe(scenarioId);
    }

    // ============================================================
    // 评论
    // ============================================================
    @Transactional(readOnly = true)
    public List<CommentResponse> listComments(Long scenarioId) {
        return commentRepository.findByScenarioIdOrderByCreatedAtAsc(scenarioId).stream()
                .map(this::toCommentResponse).toList();
    }

    @Transactional
    public CommentResponse addComment(Long scenarioId, CommentRequest request, Long authorId, String authorName) {
        if (!scenarioRepository.existsById(scenarioId)) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "测算方案不存在");
        }
        ScenarioComment comment = new ScenarioComment();
        comment.setScenarioId(scenarioId);
        comment.setParentId(request.parentId());
        comment.setContent(request.content().trim());
        comment.setMentions(extractMentions(request.content()));
        comment.setAuthorId(authorId);
        comment.setAuthorName(authorName);
        comment = commentRepository.save(comment);
        recordChange(scenarioId, "COMMENT_ADDED", null, null,
                truncate(request.content(), 200), authorId, authorName);
        CommentResponse response = toCommentResponse(comment);
        eventBus.publish(scenarioId, "comment", response);
        return response;
    }

    // ============================================================
    // 变更时间线
    // ============================================================
    @Transactional(readOnly = true)
    public List<ChangeResponse> listChanges(Long scenarioId) {
        return changeRepository.findByScenarioIdOrderByVersionNoDesc(scenarioId).stream()
                .map(this::toChangeResponse).toList();
    }

    /** 记录一条变更（版本号方案内递增）；供本包与其他服务复用。 */
    @Transactional
    public ChangeResponse recordChange(Long scenarioId, String changeType, String fieldName,
                                       String oldValue, String newValue, Long operatorId, String operatorName) {
        int nextVersion = changeRepository.findMaxVersionNo(scenarioId).orElse(0) + 1;
        ScenarioChange change = new ScenarioChange();
        change.setScenarioId(scenarioId);
        change.setVersionNo(nextVersion);
        change.setChangeType(changeType);
        change.setFieldName(fieldName);
        change.setOldValue(oldValue);
        change.setNewValue(newValue);
        change.setOperatorId(operatorId);
        change.setOperatorName(operatorName);
        change = changeRepository.save(change);
        ChangeResponse response = toChangeResponse(change);
        eventBus.publish(scenarioId, "change", response);
        return response;
    }

    // ============================================================
    // 在线状态
    // ============================================================
    @Transactional
    public List<PresenceResponse> heartbeat(Long scenarioId, PresenceRequest request) {
        if (!scenarioRepository.existsById(scenarioId)) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "测算方案不存在");
        }
        ScenarioPresence presence = presenceRepository.findByScenarioIdAndUserId(scenarioId, request.userId())
                .orElseGet(ScenarioPresence::new);
        presence.setScenarioId(scenarioId);
        presence.setUserId(request.userId());
        presence.setUserName(request.userName().trim());
        presenceRepository.save(presence);
        List<PresenceResponse> online = listPresence(scenarioId);
        eventBus.publish(scenarioId, "presence", online);
        return online;
    }

    @Transactional(readOnly = true)
    public List<PresenceResponse> listPresence(Long scenarioId) {
        LocalDateTime since = LocalDateTime.now().minusSeconds(PRESENCE_ONLINE_SECONDS);
        return presenceRepository.findByScenarioIdAndLastSeenAtAfter(scenarioId, since).stream()
                .map(p -> new PresenceResponse(p.getUserId(), p.getUserName(), p.getLastSeenAt()))
                .toList();
    }

    // ============================================================
    // 内部
    // ============================================================
    private String extractMentions(String content) {
        if (content == null) {
            return null;
        }
        Matcher matcher = MENTION_PATTERN.matcher(content);
        return matcher.results()
                .map(r -> r.group(1))
                .distinct()
                .collect(Collectors.joining(","));
    }

    private String truncate(String s, int max) {
        return s == null ? null : s.length() <= max ? s : s.substring(0, max);
    }

    private CommentResponse toCommentResponse(ScenarioComment c) {
        return new CommentResponse(c.getId(), c.getScenarioId(), c.getParentId(), c.getContent(),
                c.getMentions(), c.getAuthorId(), c.getAuthorName(), c.getCreatedAt());
    }

    private ChangeResponse toChangeResponse(ScenarioChange c) {
        return new ChangeResponse(c.getId(), c.getScenarioId(), c.getVersionNo(), c.getChangeType(),
                c.getFieldName(), c.getOldValue(), c.getNewValue(), c.getOperatorId(), c.getOperatorName(),
                c.getCreatedAt());
    }
}
