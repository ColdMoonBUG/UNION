package com.scoder.jusic.controller;

import com.scoder.jusic.common.message.Response;
import com.scoder.jusic.model.House;
import com.scoder.jusic.model.HouseRequest;
import com.scoder.jusic.model.MessageType;
import com.scoder.jusic.model.User;
import com.scoder.jusic.service.AvService;
import com.scoder.jusic.service.HouseService;
import com.scoder.jusic.service.RoomSnapshotService;
import com.scoder.jusic.service.SessionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.util.List;

/**
 * @author H
 */
@Controller
@Slf4j
public class HouseController {

    @Autowired
    private HouseService houseService;
    @Autowired
    private SessionService sessionService;
    @Autowired
    private RoomSnapshotService roomSnapshotService;
    @Autowired
    private AvService avService;
    private static final List<String> roles = java.util.Arrays.asList("root", "admin");

    @PostMapping("/house/search")
    @ResponseBody
    public Response<List<House>> search(@RequestBody(required = false) HouseRequest request) {
        return Response.success(houseService.listWithPopulation(), "房间列表");
    }

    @PostMapping("/house/add")
    @ResponseBody
    public Response<String> add(@RequestBody HouseRequest request) {
        try {
            String houseId = houseService.create(request);
            return Response.success(houseId, "建房成功");
        } catch (IllegalArgumentException e) {
            return Response.failure((String) null, e.getMessage());
        }
    }

    @PostMapping("/house/enter")
    @ResponseBody
    public Response<House> enter(@RequestBody HouseRequest request) {
        try {
            House house = houseService.enter(request.getId(), request.getPassword());
            return Response.success(house, "进入房间成功");
        } catch (IllegalArgumentException e) {
            return Response.failure((House) null, e.getMessage());
        }
    }

    @PostMapping("/house/get")
    @ResponseBody
    public Response<House> get(@RequestBody HouseRequest request) {
        House house = houseService.get(request.getId());
        if (house == null) {
            return Response.failure((House) null, "房间不存在");
        }
        return Response.success(house, "房间信息");
    }

    @PostMapping("/house/getMiniCode")
    @ResponseBody
    public Response<String> getMiniCode(@RequestBody HouseRequest request) {
        House house = houseService.getRaw(request.getId());
        if (house == null) {
            return Response.failure((String) null, "房间不存在");
        }
        try {
            return Response.success(this.createMiniCode(house), "房间二维码");
        } catch (Exception e) {
            log.error("生成房间二维码失败: {}", house.getId(), e);
            return Response.failure((String) null, "房间二维码生成失败");
        }
    }

    @MessageMapping("/house/search")
    public void searchWs(HouseRequest request, StompHeaderAccessor accessor) {
        String sessionId = accessor.getHeader("simpSessionId").toString();
        sessionService.send(sessionId, MessageType.SEARCH_HOUSE, Response.success(houseService.listWithPopulation(), "房间列表"));
    }

    @MessageMapping("/house/add")
    public void addWs(HouseRequest request, StompHeaderAccessor accessor) {
        String sessionId = accessor.getHeader("simpSessionId").toString();
        try {
            sessionService.send(sessionId, MessageType.ADD_HOUSE_START, Response.success((Object) null, "开始建房"));
            String houseId = houseService.create(request);
            String previousRoomId = houseService.bindSession(sessionId, houseId);
            sessionService.send(sessionId, MessageType.ADD_HOUSE, Response.success(houseId, "建房成功"));
            this.afterRoomSwitch(sessionId, previousRoomId, houseId);
            this.broadcastHouseSearch();
        } catch (IllegalArgumentException e) {
            sessionService.send(sessionId, MessageType.ADD_HOUSE, Response.failure((String) null, e.getMessage()));
        }
    }

    @MessageMapping("/house/enter")
    public void enterWs(HouseRequest request, StompHeaderAccessor accessor) {
        String sessionId = accessor.getHeader("simpSessionId").toString();
        try {
            sessionService.send(sessionId, MessageType.ENTER_HOUSE_START, Response.success((Object) null, "开始进入房间"));
            House house = houseService.enter(request.getId(), request.getPassword());
            String previousRoomId = houseService.bindSession(sessionId, house.getId());
            sessionService.send(sessionId, MessageType.ENTER_HOUSE, Response.success(house, "进入房间成功"));
            this.afterRoomSwitch(sessionId, previousRoomId, house.getId());
            this.broadcastHouseSearch();
        } catch (IllegalArgumentException e) {
            sessionService.send(sessionId, MessageType.ENTER_HOUSE, Response.failure((House) null, e.getMessage()));
        }
    }

    @MessageMapping("/house/houseuser")
    public void houseUser(StompHeaderAccessor accessor) {
        String sessionId = accessor.getHeader("simpSessionId").toString();
        String roomId = sessionService.getRoomId(sessionId);
        List<User> users = houseService.listUsers(roomId);
        sessionService.send(sessionId, MessageType.HOUSE_USER, Response.success(users, "房间成员"));
    }

    @MessageMapping("/house/retain/{enable}")
    public void retain(@org.springframework.messaging.handler.annotation.DestinationVariable boolean enable,
                       StompHeaderAccessor accessor) {
        String sessionId = accessor.getHeader("simpSessionId").toString();
        String role = sessionService.getRole(sessionId);
        if (!roles.contains(role)) {
            sessionService.send(sessionId, MessageType.NOTICE, Response.failure((Object) null, "你没有权限"));
            return;
        }
        String roomId = sessionService.getRoomId(sessionId);
        try {
            House house = houseService.updateRetainStatus(roomId, enable);
            sessionService.send(sessionId, MessageType.NOTICE,
                    Response.success((Object) null, enable ? "房间留存已开启" : "房间留存已关闭"));
            sessionService.sendRoom(roomId, MessageType.ENTER_HOUSE, Response.success(house, "房间信息"));
            this.broadcastHouseSearch();
        } catch (IllegalArgumentException e) {
            sessionService.send(sessionId, MessageType.NOTICE, Response.failure((Object) null, e.getMessage()));
        }
    }

    private void broadcastHouseSearch() {
        sessionService.send(MessageType.SEARCH_HOUSE, Response.success(houseService.listWithPopulation(), "房间列表"));
    }

    private void afterRoomSwitch(String sessionId, String previousRoomId, String currentRoomId) {
        if (previousRoomId != null && !previousRoomId.equals(currentRoomId)) {
            if (sessionService.size(previousRoomId) <= 0) {
                avService.clearPlaybackState(previousRoomId);
            }
            roomSnapshotService.broadcastOnline(previousRoomId);
            sessionService.sendRoom(previousRoomId, MessageType.HOUSE_USER,
                    Response.success(houseService.listUsers(previousRoomId), "房间成员"));
        }
        roomSnapshotService.broadcastOnline(currentRoomId);
        sessionService.sendRoom(currentRoomId, MessageType.HOUSE_USER,
                Response.success(houseService.listUsers(currentRoomId), "房间成员"));
        roomSnapshotService.sendRoomSnapshot(sessionId, currentRoomId, true);
    }

    private String createMiniCode(House house) throws Exception {
        BufferedImage image = new BufferedImage(320, 320, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setColor(Color.WHITE);
        graphics.fillRect(0, 0, 320, 320);
        graphics.setColor(Color.BLACK);
        graphics.setFont(new Font("SansSerif", Font.BOLD, 20));
        graphics.drawString("UNION 房间码", 92, 48);
        graphics.setFont(new Font("SansSerif", Font.PLAIN, 16));
        graphics.drawString("ID: " + house.getId(), 36, 116);
        graphics.drawString("PWD: " + (house.getNeedPwd() ? "已设置" : "无"), 36, 152);
        graphics.drawString("NAME: " + this.limit(house.getName(), 18), 36, 188);
        graphics.drawString("JOIN", 132, 246);
        String password = house.getPassword() != null ? house.getPassword() : "";
        String payload = "houseId=" + house.getId() + "&housePwd=" + password;
        graphics.setFont(new Font("Monospaced", Font.PLAIN, 12));
        graphics.drawString(this.limit(payload, 34), 26, 286);
        graphics.dispose();

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ImageIO.write(image, "jpg", outputStream);
        return Base64.getEncoder().encodeToString(outputStream.toByteArray());
    }

    private String limit(String value, int size) {
        if (value == null) {
            return "";
        }
        return value.length() <= size ? value : value.substring(0, size);
    }
}
