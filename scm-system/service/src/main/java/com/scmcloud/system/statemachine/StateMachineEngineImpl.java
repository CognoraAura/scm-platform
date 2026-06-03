package com.scmcloud.system.statemachine;

import com.scmcloud.system.domain.entity.SysStatusTransition;
import com.scmcloud.system.service.ISysStatusDictService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Implementation of StateMachineEngine.
 * Validates transitions against sys_status_transition rules.
 *
 * <p>Supports SPI extension via preAction/postAction fields
 * (Bean names resolved at runtime via Spring ApplicationContext).</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StateMachineEngineImpl implements StateMachineEngine {

    private final ISysStatusDictService statusDictService;

    @Override
    public TransitionCheckResult canTransition(String bizType, String fromStatus, String toStatus) {
        List<SysStatusTransition> transitions = statusDictService.listTransitionsFrom(bizType, fromStatus);

        boolean allowed = transitions.stream()
                .anyMatch(t -> t.getToStatus().equals(toStatus) && t.getEnabled());

        if (allowed) {
            return TransitionCheckResult.allow(bizType, fromStatus, toStatus);
        }

        return TransitionCheckResult.deny(bizType, fromStatus, toStatus,
                "No enabled transition from " + fromStatus + " to " + toStatus + " for " + bizType);
    }

    @Override
    public TransitionResult transition(String bizType, String fromStatus, String actionCode) {
        List<SysStatusTransition> transitions = statusDictService.listTransitionsFrom(bizType, fromStatus);

        SysStatusTransition matched = transitions.stream()
                .filter(t -> t.getActionCode().equals(actionCode) && t.getEnabled())
                .findFirst()
                .orElse(null);

        if (matched == null) {
            return TransitionResult.failure(bizType, fromStatus, actionCode,
                    "No enabled transition with action '" + actionCode + "' from status " + fromStatus);
        }

        log.info("State transition: {} {} -> {} (action={})", bizType, fromStatus, matched.getToStatus(), actionCode);
        return TransitionResult.success(matched);
    }

    @Override
    public List<AvailableAction> getAvailableActions(String bizType, String currentStatus) {
        List<SysStatusTransition> transitions = statusDictService.listTransitionsFrom(bizType, currentStatus);

        return transitions.stream()
                .filter(SysStatusTransition::getEnabled)
                .map(t -> AvailableAction.builder()
                        .actionCode(t.getActionCode())
                        .actionName(t.getActionName())
                        .actionNameEn(t.getActionNameEn())
                        .targetStatus(t.getToStatus())
                        .needApproval(t.getNeedApproval())
                        .conditionExpression(t.getConditionExpression())
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    public List<String> getValidNextStatuses(String bizType, String currentStatus) {
        List<SysStatusTransition> transitions = statusDictService.listTransitionsFrom(bizType, currentStatus);

        return transitions.stream()
                .filter(SysStatusTransition::getEnabled)
                .map(SysStatusTransition::getToStatus)
                .distinct()
                .collect(Collectors.toList());
    }
}
