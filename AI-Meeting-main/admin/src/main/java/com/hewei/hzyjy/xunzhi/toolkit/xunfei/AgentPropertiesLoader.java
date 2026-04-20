package com.hewei.hzyjy.xunzhi.toolkit.xunfei;

import com.hewei.hzyjy.xunzhi.agent.dao.entity.AgentPropertiesDO;
import com.hewei.hzyjy.xunzhi.agent.api.io.resp.AgentPropertiesRespDTO;
import com.hewei.hzyjy.xunzhi.agent.service.AgentPropertiesService;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class AgentPropertiesLoader implements CommandLineRunner {

    private final AgentPropertiesService agentPropertiesService;

    @Getter
    private final Map<Long, AgentPropertiesDO> agentPropertiesMap = new ConcurrentHashMap<>();

    @Override
    public void run(String... args) {
        refreshActiveAgents();
    }

    public void refreshActiveAgents() {
        List<AgentPropertiesDO> agents = agentPropertiesService.listActiveAgents();
        agentPropertiesMap.clear();
        if (agents != null) {
            for (AgentPropertiesDO agent : agents) {
                agentPropertiesMap.put(agent.getId(), agent);
            }
        }
        log.info("Loaded {} agent properties into startup cache", agentPropertiesMap.size());
    }

    public AgentPropertiesDO getByAgentId(Long agentId) {
        if (agentId == null) {
            return null;
        }
        AgentPropertiesDO cached = agentPropertiesMap.get(agentId);
        if (cached != null && (cached.getDelFlag() == null || cached.getDelFlag() == 0)) {
            return cached;
        }
        AgentPropertiesDO latest = agentPropertiesService.getById(agentId);
        if (latest != null && (latest.getDelFlag() == null || latest.getDelFlag() == 0)) {
            agentPropertiesMap.put(agentId, latest);
            return latest;
        }
        return null;
    }

    public AgentPropertiesDO getByAgentName(String agentName) {
        if (agentName == null || agentName.isBlank()) {
            return null;
        }
        for (AgentPropertiesDO cached : agentPropertiesMap.values()) {
            if (cached != null
                    && agentName.equals(cached.getAgentName())
                    && (cached.getDelFlag() == null || cached.getDelFlag() == 0)) {
                return cached;
            }
        }

        AgentPropertiesRespDTO latest = agentPropertiesService.getByName(agentName);
        if (latest == null || latest.getId() == null) {
            return null;
        }
        return getByAgentId(latest.getId());
    }
}
