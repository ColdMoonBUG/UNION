package com.scoder.jusic.service.imp;

import com.scoder.jusic.configuration.JusicProperties;
import com.scoder.jusic.model.House;
import com.scoder.jusic.model.User;
import com.scoder.jusic.repository.ConfigRepository;
import com.scoder.jusic.service.HouseService;
import com.scoder.jusic.repository.SessionRepository;
import com.scoder.jusic.service.AuthService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @author H
 */
@Service
@Slf4j
public class AuthServiceImpl implements AuthService {

    @Autowired
    private JusicProperties jusicProperties;
    @Autowired
    private SessionRepository sessionRepository;
    @Autowired
    private ConfigRepository configRepository;
    @Autowired
    private HouseService houseService;

    @Override
    public boolean authRoot(String sessionId, String password) {
        if (null == password) {
            return false;
        }
        String rootPassword = configRepository.getRootPassword();
        if (null == rootPassword) {
            rootPassword = jusicProperties.getRoleRootPassword();
            configRepository.initRootPassword();
        }
        if (password.equals(rootPassword)) {
            // update role
            User user = sessionRepository.getSession(sessionId);
            user.setRole("root");
            sessionRepository.setSession(user);

            return true;
        }
        return false;
    }

    @Override
    public boolean authAdmin(String sessionId, String password) {
        User user = sessionRepository.getSession(sessionId);
        if (user == null) {
            return false;
        }
        String roomId = user.getRoomId();
        House house = houseService.getRaw(roomId);
        if (house == null) {
            return false;
        }
        String ownerSessionId = house.getOwnerSessionId();
        if (ownerSessionId == null || !ownerSessionId.equals(sessionId)) {
            return false;
        }
        user.setRole("admin");
        sessionRepository.setSession(user);
        return true;
    }

}
